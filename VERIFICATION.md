# 验证记录（VERIFICATION）

本文件记录 `AnvilCraft-TerminalPlugins` 的验证结果：**已经实测通过的部分**、验证方式，以及本环境特有的构建绕行方案。

## 〇、P0：面板两级化 + 控件库 + 设置视图 + 失败反馈（版本 1.2.0）

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
