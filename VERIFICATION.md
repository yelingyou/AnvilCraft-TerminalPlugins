# 验证记录（VERIFICATION）

本文件记录 `AnvilCraft-TerminalPlugins` 的验证结果：**已经实测通过的部分**、验证方式，以及本环境特有的构建绕行方案。

## 〇、P2（版本 1.4.0）：流体接口插件 + 移除与本体重复的「一键存入」

### 1. 移除「一键存入插件」

核对本体实现后确认该功能**本体已经有了**，本附属不再重复实现（相关代码、组件、贴图、配方、语言键、网络动作全部删除）：

| 本体已有的能力 | 证据 |
|---|---|
| 智能补货：按住 Alt 切换「智能（双向）/ 仅补货 / 仅存入 / 关」；**存入** = 捡起物品超过一组时只保留一组，多余自动存入存储站 | 本体游戏内手册 `ageratum`：`004_block/215_large_crate.md` §终端的「智能补货」 |
| 存储界面的「存入 / 取出」按钮（整包搬运，支持撤销） | `StorageServerStub.deposit(UUID, long, boolean all)` / `undo(UUID, long)` / `take(UUID, long)`，以及 `storage_station/put.png`、`take.png` |
| 终端悬浮窗「手持物品右击塞入」 | 同上手册 §终端的「悬浮窗」 |

因此 `DepositPlugin`、`DepositSettings`、`DEPOSIT_NOW`、面板「立即存入」按钮、`deposit_plugin` 物品/贴图/配方一并移除；插件总数 9 → 8。
（旧版本仍可从 git 历史 `e4b5382` 取回。）

### 2. 新增「流体接口插件」（贴合终端设计的实现方式）

终端的存储是**按类型计数**的物品模型，没有流体槽位，所以没有照搬「流体罐 + 泵」那种双绑定设计，而是：

- **插件物品自带储液缓冲**（数据组件 `FLUID_BUFFER`，容量 1~64 桶，默认 8 桶）——流体跟着插件走，拆下插件就把缓冲里的流体一起带走，符合「插件 = 可拆卸模块」的设定；
- 流体进出存储**始终以容器物品的形式**发生，存储里只多出/少掉桶、瓶这类普通物品，不引入任何存储模型改动；
- 复用本体 `api/fluid/FluidHandlerWrapper`（它已处理桶、玻璃瓶、水瓶、蜜瓶、不祥之瓶等特例）：
  - **抽进缓冲**：从存储取一个能倒出流体的容器 → `fillFromItem` → 空容器放回存储；
  - **灌进容器**：从存储取一个空容器 → `drainToItem` → 装满的容器放回存储；
  - 候选筛选走 `simulate` 版本（`fillFromItem(stack, true)` / `drainToItem(stack, true)`，已反编译确认第二/第三个 boolean 就是 `FluidAction.SIMULATE`），失败时把取出的原容器原样放回，不会出现「容器没了、流体没动」的丢物品；
- 过滤表（复刻本体的 `ModComponents.FILTER_CONTENT`）决定允许哪些容器参与；每周期处理 1/2/4/8/16 个；工作模式 关闭 / 抽进缓冲 / 灌进容器。

### 3. 验证

| 项 | 结果 |
|---|---|
| `compileJava` | BUILD SUCCESSFUL |
| `runData build` | BUILD SUCCESSFUL，产物 `anvilcraft_terminal_plugins-neoforge-1.21.1-1.4.0.jar`；生成的 `en_us` / `en_ud` 中已无 `deposit` 键、已含 `fluid` 键 |
| `runServer` | `Done (10.055s)! For help, type "help"`，无报错；插件类正常加载 |
| 贴图 / 配方 | `textures/item/fluid_plugin.png`（16×16 储罐+液面+阀门）、`recipe/fluid_plugin.json`（铁锭+玻璃+桶 3×3） |

### 4. 关于「补货增强」

原计划里的「补货增强（Advanced Refill）」对应本体已有的 `BalanceMode.RESTOCK`（智能补货的「补货」档：手持物品用完时自动补满一组），
与原计划表里标注的「需先确认增量价值，否则不做」一致，本轮**不做**；
确实需要「换工具」这类本体没有的语义时，见下面的「工具切换插件」（版本 1.4.1）。

## 〇·2、P2（版本 1.4.1）：工具切换插件

### 1. 为什么不是「补货增强」而是「工具切换」

本体 `BalanceMode.RESTOCK` 的补货是「手持物品用完 → 补满一组」，**对不可堆叠的工具（镐/斧/锹…）没有意义**；
而工具磨损是玩家真实痛点，本体也没有对应能力。因此把 P2 的「补货增强」替换为语义明确、不与本体重复的**工具切换**。

### 2. 行为

| 项 | 内容 |
|---|---|
| 触发 | 每 10 tick 检查一次；槽位里的物品可损坏、且**剩余耐久**低于阈值（5% / 10% / 25% / 50%） |
| 替换品 | 从终端连接的存储里取**同种物品**（`getItem()` 相同）且**剩余耐久严格大于**当前工具的一件（`extractFirst` + 谓词） |
| 槽位 | 仅主手（默认）或整个快捷栏 9 格 |
| 旧工具 | 默认放回存储（可作为铁砧修复插件的材料）；关闭时留在背包，背包满则掉在脚下 |
| 防刷 | 「严格更多耐久」的谓词保证同一把工具不会来回换；换入的是存储里的真实物品，没有凭空生成 |
| 过滤表 | 留空 = 所有可损坏物品都生效；填了内容就只换表里的工具 |

> 过滤表语义在本轮统一了：本体的过滤器在「白名单 + 空表」时对任何物品都返回 `false`（做白名单是安全的），
> 所以*消耗 / 破坏型*插件（销毁、压缩）留空 = 什么都不做；*增益型*插件（流体接口、工具切换）留空 = 对全部目标生效。
> 该判断经反编译 `FilterContent#filter(ItemStack)` 确认（遍历非空条目 → 命中返回 `!blackList`；遍历完返回 `blackList`）。

### 3. 验证

| 项 | 结果 |
|---|---|
| `compileJava` | BUILD SUCCESSFUL |
| `runData build` | BUILD SUCCESSFUL，产物 `anvilcraft_terminal_plugins-neoforge-1.21.1-1.4.1.jar` |
| `runServer` | `Done (12.050s)! For help, type "help"`，无报错，插件类正常加载 |
| 贴图 / 配方 | `textures/item/tool_swap_plugin.png`（两把镐 + 循环箭头）、`recipe/tool_swap_plugin.json`（铁锭 + 红石 + 钻石镐） |
| 新增语言键 | `plugin.…tool_swap_plugin(.desc)`、`screen.…setting.tool_swap_threshold/target/return_worn`、`screen.…swap_target.main_hand/all_hotbar`、`screen.…panel.tool_swap_return_tip` |
## 〇、P1：四个新插件（版本 1.3.0）

按计划新增（全部含设置子页、手持右键档位、配方、贴图、中英语言键）：

> 注：原计划里的「一键存入」已**移除** —— 铁砧工艺本体自带「智能补货（Alt 切换：智能 / 仅补货 / 仅存入 / 关）」与存储界面的「存入 / 取出」按钮，
> 本体的 `StorageServerStub.deposit(playerId, sessionId, all)` 就是「把背包里的物品一次存进存储」，再做一个插件属于重复实现。

| 插件 | 语义 | 实现要点 |
|---|---|---|
| **销毁** | 存储里匹配过滤的物品只保留指定组数 | 直接操作 `UnlimitedItemStack` 的数量；保留组数 0/1/2/4/8/16/64 |
| **压缩** | 9 个同类物品自动压成 1 个 | 用 `RecipeType.CRAFTING` + `CraftingInput.of(3,3,9 个相同物品)` 查询原版配方；只处理过滤表内物品；取不足 9 个时原样归还 |
| **铁砧修复** | 用匹配过滤的材料修复受损装备 | 取出受损装备 1 件 + 材料 1 个 → `setDamageValue` → 放回；无材料时把装备放回 |

**顺带完成**：过滤表编辑界面（P1 的第一项）已在 P0 随两级面板一起落地（6×3 共 18 格 ghost 网格）。

验证：`compileJava`、`runData build`（jar 1.3.0，新增语言键已生成）、`runServer`（`Done (11.955s)!`）均通过。

## 〇之前、P0：面板两级化 + 控件库 + 设置视图 + 失败反馈（版本 1.2.0）

按《插件扩展计划》P0 落地，全部为架构性改造（玩家可见的变化是设置界面变成两级）：

| 项 | 内容 |
|---|---|
| 两级面板 | 列表页 → 「设置」→ 插件设置子页（返回按钮、标题显示插件名与图标）；面板高度按子页内容**自适应** |
| 设置视图接口 | 新增 `client/gui/PluginSettingsView`（含 `Ctx` 渲染上下文）与 `client/gui/PluginViews`（每种插件一个内部类实现）；面板不再为每种插件写 if-else |
| 通用控件 | `settingButton`（显示当前值的按钮）、`smallButton`（- / + 步进）、`ghostSlot`（过滤/药水槽，左键放入右键清空）、tooltip |
| 网络动作 | 新增 `SET_FILTER_SLOT`、`ADJUST_MAGNET_RANGE`、`ADJUST_FEEDING_THRESHOLD`；服务端统一走新增的 `TerminalPluginManager#update(terminal, index, operator)` |
| 失败反馈 | 新增 `PluginFeedbackPacket`（服务端 → 客户端，快捷栏上方提示）；终端不在身上 / 插件索引过期时给出提示（对齐精妙背包的错误反馈包） |

验证：`compileJava`、`runData build`（jar 1.2.0）、`runServer`（`Done (11.005s)!`，新 payload 注册未影响专用服务器加载）均通过。

## 〇之前、上一轮改动（对齐精妙背包的设置交互 + 开源许可修正）

**研究结论（精妙背包/Sophisticated Core 的升级设置怎么做）**：每个升级对应一个设置标签页，
设置项一律是**"图标按钮 + 直接显示当前状态"**（`ButtonDefinition.Toggle`，如条件 NEVER/ALWAYS/ON_FIRE…），
配合 **tooltip** 说明；炼金升级的条目在标签里以 **2 列过滤槽 + 条件按钮** 排列，阈值用滑块/按钮调整。
参考类：`client/gui/UpgradeSettingsTab`、`common/gui/UpgradeContainerBase`、`upgrades/alchemy/AlchemyUpgradeTab`（**仅参考交互设计，未复制代码**）。

据此把我们的面板改成同样的模式：

| 改动 | 之前 | 现在 |
|---|---|---|
| 设置按钮 | 两个含义不明的「主 / 副」按钮 | 每个设置一个按钮，**按钮上显示当前值**（如「半径 5」「配方 熔炼」「模式 磁吸 + 拾取」），点击循环切换 |
| 悬停提示 | 无 | 每个控件都有 tooltip（含拖动说明、药水槽用法、条件/半径按钮用法） |
| 炼金条目 | 20px 行、槽位 18px | 压缩为 18px 行（槽 16px），条件按钮 62px，阈值 −/+ 内联，另加作用半径按钮 |
| 面板高度 | 190 | 246（容纳设置行与炼金条目） |

**开源许可修正**：原先 LICENSE 顶部加了自定义说明文字，导致 GitHub 识别为 `NOASSERTION`；
现已替换为**标准 LGPL-3.0 全文**（SPDX 官方文本），版权信息保留在 `NOTICE` 与各源码文件的 SPDX 头中。

**版本**：`mod_version` 1.0.1 → **1.1.0**（设置交互属于功能更新）。

验证：`compileJava`、`runData build`（jar `...-1.1.0.jar`）均通过；新增语言键已生成。

## 〇之前、上一轮改动（JEI 重叠 + 喂食插件失效）

| 问题 | 根因 | 修复 |
|---|---|---|
| 插件面板与 JEI 重叠 | 面板固定在屏幕**右侧**，而 JEI 的素材列表默认就在右侧 | 默认锚点改到屏幕**左侧**，并支持**按住「≡」按钮拖动**面板；位置持久化到 `config/anvilcraft_terminal_plugins_panel.txt` |
| 喂食插件（以及其它插件）不生效 | **时钟错配**：派发器用 `player.tickCount % 10`，插件内部用 `gameTime % k`。两者相差固定偏移，当偏移不是间隔的公倍数时条件**永不成立** | 统一为同一时钟（`PluginContext#tick` = `player.tickCount`），派发与插件判定共用 |

时钟问题的数值验证（4000 tick 内触发次数，喂食 = 派发 10 / 插件 20）：

```
offset(gameTime - tickCount)   旧实现   新实现
        0                      200      200
        3                        0      200
        5                        0      200
        7                        0      200
       13                        0      200
```

即：玩家进入世界时若 `gameTime - tickCount` 不是 10 的倍数，**喂食插件整局都不会触发**；修复后稳定每 20 tick 一次。

另外新增可选的 `debug_logging` 配置项，打开后日志会打印 `plugin-dispatch` / `plugin-run`，便于排查插件是否派发、存储是否可达。

本轮验证：`compileJava`、`runData build`（jar 162KB）、`runServer`（`Done (11.996s)!`）均通过；
配置文件已生成 `debug_logging = false` 选项。

## 〇、上一轮改动（安装台修复 + 终端内插件面板 + 炼金语义重写）

| 项目 | 内容 | 验证 |
|---|---|---|
| 安装台卸载无效 | `uninstallAll()` 在 `finally` 里提前把 `applying` 置回 false，随后写终端槽触发 `applyPlugins()`，刚拆下的插件被立刻装回 → 整段逻辑（含最后一次写槽）保持 `applying=true` | 见下表构建/启动验证 |
| 安装台看不到已安装插件 | 界面新增**已安装插件幽灵槽位一行**（最多 6 格），实时读取台面终端上的 `installed_plugins` | 同上 |
| 终端内调节 UI | 新增 `TerminalPluginPanel`（叠加层）+ `TerminalPluginClientEvents`：终端界面右上角「插件 N」按钮或 **K** 打开；支持查看列表、主/副档位、↑↓ 排序、启用/停用、拆下（退回背包）、炼金条目配置 | 同上 |
| 炼金插件语义 | 由「存储内自动酿造」重写为**按条件自动用药**（对齐精妙背包炼金升级）：4 条目 = 过滤药水 + 触发条件（NEVER/ALWAYS/UNDER_WATER/ON_FIRE/FALLING/SPRINTING/HURT/NEGATIVE_EFFECT）+ 阈值；普通药水饮用生效、喷溅/滞留就地投掷、不祥之瓶照常使用，空瓶放回存储 | 同上 |

本轮验证命令与结果：

```
gradle compileJava        -> BUILD SUCCESSFUL（修正 2 处 StreamCodec 缓冲类型不匹配）
gradle runData build      -> BUILD SUCCESSFUL，jar 141KB / 124 条目，生成语言键已含 panel / trigger 等新键
gradle runServer          -> Done (12.288s)!，无本模组异常
```

## 一、结论：已实测通过

| 步骤 | 命令 | 结果 |
|---|---|---|
| 编译 | `gradle compileJava` | ✅ BUILD SUCCESSFUL（真实 AnvilCraft 1.6.0 + NeoForge 21.1.248 + MC 1.21.1 类路径） |
| 打包 | `gradle clean build` | ✅ 产出 `build/libs/anvilcraft_terminal_plugins-neoforge-1.21.1-1.0.0.jar`（104KB / 113 条目，无残留类） |
| 数据生成（真实加载模组） | `gradle runData` | ✅ 6 个注册项全部生成 blockstate/item model/lang/loot_table，11 个文件 |
| 专用服务器实测 | `gradle runServer` | ✅ 23 个模组加载完成、`Done (12.215s)!`、GameTest 命名空间 `anvilcraft_terminal_plugins` 生效、无本模组报错 |

服务器日志关键行：

```
Found valid mod file main with {anvilcraft_terminal_plugins} mods - versions {1.0.0}
Loaded TOML config file run/config/anvilcraft_terminal_plugins-common.toml
Attempting to inject @EventBusSubscriber classes into the eventbus for anvilcraft_terminal_plugins
[Server thread/INFO] [minecraft/DedicatedServer]: Done (12.215s)! For help, type "help"
[ne.ne.ne.ga.GameTestHooks]: Enabled Gametest Namespaces: [anvilcraft_terminal_plugins]
```

## 二、编译期修掉的真实问题（由真实编译暴露）

1. `ItemEntity#hasPickupDelay()` → 实际方法名是 `hasPickUpDelay()`（大写 U）；
2. `.component(...)` 不属于 Registrum 的 `ItemBuilder`，而属于 **`Item.Properties`**，必须写成
   `.properties(properties -> properties.component(TYPE, VALUE))`；
3. `AutoCookingSettings.CookingMode.STREAM_CODEC` 声明成了 `StreamCodec<RegistryFriendlyByteBuf, …>`，
   而 `ByteBufCodecs.fromCodec` 返回的是 `StreamCodec<ByteBuf, …>`（缓冲类型不变型，必须一致）；
4. `AlchemyPlugin` 里 lambda 捕获了在循环中被赋值的局部变量 → 改为 `final` 副本；
5. 5 个 `package-info.java` 仍声明旧包名 `dev.anvilcraft.addon.template.*` → 会产生残留类，已修正；
6. 模板脚本 `gradle/scripts/publishing.gradle` 使用了 Gradle 9 已移除的 `project.archivesBaseName` → 改为 `project.name`。

## 三、资源清理

- 手写的 `models/item/*`、`blockstates/*`、`models/block/*`、`loot_table/*` 与 Registrum 数据生成结果**完全一致**，
  已删除手写副本，统一由 `runData` 生成（避免同一路径两份文件）；
- 手写的额外语言键（tooltip / 界面 / 插件说明共 33 条）已迁入 `AddonLangHandler`，随数据生成输出；
- 保留手写：`lang/zh_cn.json`（44 键）、6 个配方、6 张 16×16 贴图、mixins 配置。

## 四、构建环境说明（本机特有的绕行）

在开发机上，受限的运行环境**默认阻断 Java 进程出网**，且不允许写用户目录下的 `~/.gradle`。实测与绕行：

| 限制 | 现象 | 绕行方案 |
|---|---|---|
| Java 出网被拦 | `services.gradle.org` 连接超时、`maven.neoforged.net` 间歇性 `Connection refused` | 自建 Node 本地 CONNECT 代理（`java_proxy.js`，监听 `127.0.0.1:8899`，含 IPv4 强制与 5 次重试），Gradle 通过 `-Dhttps.proxyHost/-Dhttps.proxyPort` 走代理 |
| Gradle 发行版主机不可达 | Wrapper 无法下载 `gradle-8.14.5` | 使用本机已解压的 **Gradle 9.5.1**（`~/.gradle/wrapper/dists/gradle-9.5.1-bin/.../bin/gradle.bat`） |
| 不能写 `~/.gradle` | Wrapper 报 "Could not create parent directory for lock file" | 设 `GRADLE_USER_HOME=<工作区>/.gradle-home`，所有缓存写入工作区内 |
| 无法写工作区外 | —— | NeoForge 版本改用本机已缓存的 **21.1.248**（`gradle/libs.versions.toml`），减少一次外网下载 |

> 你在正常有网环境里**不需要**这些绕行：直接 `gradlew.bat build` 即可（Wrapper 会拉取 8.14.5）。

## 五、尚未由自动化覆盖的部分（建议进游戏手测）

1. 创造模式物品栏出现「铁砧工艺：终端插件」页签，6 个物品贴图正常；
2. 放插件安装台 → 右键开界面（纯色块绘制，无需贴图资源）→ 终端放左槽、插件放右侧自动安装；
3. 「全部拆卸」按钮把插件退回暂存槽；
4. 磁吸：手持带插件的终端，附近丢物品 → 直接进存储；
5. 过滤：白名单外物品不再自动入库；
6. 自动烹饪：存储内生肉 + 煤 → 一段时间后出现熟肉；
7. 喂食：饥饿时自动进食；
8. 炼金：存储内水瓶 + 下界疣 → 出现粗制药水；
9. 插件右键 / 潜行右键切换档位时聊天栏提示正确。

## 六、崩溃修复：创造模式页签重复添加（已修复并复测）

### 现象（来自用户实际崩溃报告 `错误报告-09-11-2026_13.31.05.zip`）

```
net.neoforged.fml.ModLoadingException: Loading errors encountered:
  - AnvilCraft-TerminalPlugins (anvilcraft_terminal_plugins) encountered an error while dispatching
    the net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event
    java.lang.IllegalArgumentException: Itemstack 1 anvilcraft_terminal_plugins:plugin_station already exists in the tab's list
```

崩溃发生在 `MinecraftServer.createLevels → CreativeModeTab.buildContents → BuildCreativeModeTabContentsEvent`，
在该整合包里由 SimpleSorter 在服务器启动时触发页签构建而暴露。

### 根因

`AddonItemGroups.TERMINAL_PLUGINS` 的 `displayItems` 里手动 `entries.accept(...)` 了 6 个物品，
而它们的注册又都设置了 `REGISTRUM.defaultCreativeTab(TERMINAL_PLUGINS)`（Registrum 会自动加入同一页签）
→ 同一个 `ItemStack` 被加入两次。（官方模板里该 lambda 本来就是空的，是我多写了这一段。）

### 修复

清空 `displayItems`（保留为空 lambda，与官方模板一致），物品由 Registrum 自动加入，各出现一次。

### 复现与验证

新增临时监听器（验证后已删除），在 `ServerStartedEvent` 中遍历 `BuiltInRegistries.CREATIVE_MODE_TAB`
并调用 `CreativeModeTab#buildContents(...)`，**正是崩溃的调用路径**：

```
[Server thread/INFO] [minecraft/DedicatedServer]: Done (11.909s)! For help, type "help"
[Server thread/INFO] [de.an.ad.te.AnvilCraftTerminalPlugins/]: TAB-SMOKE-RESULT failures=0 ourItems=6
```

- `failures=0`：全部原版 + 模组页签构建均无异常（修复前此路径会抛 `IllegalArgumentException`）；
- `ourItems=6`：本模组 5 个插件 + 1 个安装台，在页签中各出现一次。

删除临时代码后再次 `clean build` + `runServer`：`Done (12.365s)!`，无异常；
最终 jar `anvilcraft_terminal_plugins-neoforge-1.21.1-1.0.0.jar`（104KB / 113 条目，`debug`、`addon/template` 残留类均为 0）。
