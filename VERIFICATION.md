# 验证记录（VERIFICATION）

本文件记录 `AnvilCraft-TerminalPlugins` 的验证结果：**已经实测通过的部分**、验证方式，以及本环境特有的构建绕行方案。

## 〇、第三轮需求（版本 1.7.0）：执行子页 + 输入输出槽

### 1. 「执行」从设置里独立出来

面板从「列表 / 设置」两级变成「列表 / 设置 / 执行」：插件列表行上，「设置」旁边多一个**执行**按钮，
只有会执行配方的插件才有这一页（`PluginExecViews.of(kind) == null` 的插件不显示按钮）。

| 插件 | 设置页（只放配置） | 执行页（只管加工） |
|---|---|---|
| 铁砧加工 | 过滤表 | 加工方式 / 批次 / 输入槽 / 输出槽 / **开始加工** |
| 锻造 | 过滤表 | 批次 / 输入槽 / 输出槽 / **开始锻造** |
| 压缩 | 批次 / 过滤表 | 输入槽 / 输出槽 / 「自动执行」提示 |
| 充能 | 申请功率 / 充能配方 / FE 充电开关 | 输入槽 / 输出槽 / 当前充能进度 |

面板状态多了 `executeOpen`，高度自适应按 `PluginExecViews.of(kind).height(plugin)` 计算，与设置页同一套逻辑。

### 2. 输入输出槽（`PluginSample`）

- **输入槽**：左键放入光标物品、右键清空，写进 `PLUGIN_SAMPLE.sample`；留空 = 不指定。
- **输出槽**：只读，显示 `PLUGIN_SAMPLE.lastOutput`，由服务端在每次成功加工后写回。
- 服务端行为：四个插件都会读输入槽 —— 铁砧加工要求配方能吃下这个物品、锻造只锻造这一种基底、
  压缩只压这一种、充能只挑能吃下它的充能配方；成功后把产物写进输出槽。
- 新网络动作：`SMITHING_NOW`（开始锻造），与 `ANVIL_PROCESS_NOW` 共用同一条即时动作分发。

### 3. 验证

| 项 | 结果 |
|---|---|
| `compileJava` | BUILD SUCCESSFUL |
| `runData build` | BUILD SUCCESSFUL，产物 `anvilcraft_terminal_plugins-neoforge-1.21.1-1.7.0.jar` |
| `runServer` | `Done (15.408s)! For help, type "help"`，无报错（新 payload 与执行页未影响专用服务器加载） |
## 〇、第二轮需求（版本 1.6.0）：充能 / 铁砧加工 / 删修复 / 安装台重做 / 铁砧工艺配方

设计依据（全部来自实际核对，不是推测）见 `插件扩展计划2-充能与铁砧加工.md`。

### 1. 充能插件

| 项 | 实现 |
|---|---|
| 接电网 | `IDynamicPowerComponentHolder.of(player).anvilcraft$getPowerComponent()`，往 `getPowerConsumptions()` 里加 `new PowerConsumption(kW)`；`getPowerGrid() != null && isWorking()` 才算拿到电（与本体 `IonocraftBackpackItem` 同款做法） |
| KW→FE | 直接读 `AnvilCraft.CONFIG.powerConverter.powerConverterEfficiency` / `powerConverterCountdown`；这条路径不乘 `powerConverterLoss`，因为反编译 `ChargerBlockEntity#tick` 确认本体充电器分支也没乘 |
| 充能配方 | `ModRecipeTypes.CHARGER_CHARGING_TYPE` + `ChargerChargingRecipe#getPower/getTime/getIngredient/getResult`；进度（配方 id + tick 数）存在插件物品的 `CHARGING_SETTINGS` 组件里 |
| 时钟 | 派发器受 `pluginTickInterval`（默认 10）限制，一次派发按 10 tick 结算 FE 与配方进度，数值从配置读 |
| 电源回收 | `ChargingPower.sweep(server)` 每 20 tick 跑一次：插件被拆下 / 关闭 / 玩家下线的用电登记会被删除，不会一直占着电网功率 |

### 2. 铁砧加工插件（只手动、批量）

- `intervalTicks()` 返回 **0**：派发器直接跳过，这个插件没有任何周期行为，只有面板上的「开始加工」会触发 `onAction`。
- 支持 8 种纯物品进出的加工：冲压 / 粉碎 / 压缩 / 分解 / 过筛 / 超加热 / 时移 / 中子辐照；
  配方判定与产出走本体 `AbstractProcessRecipe#getInputItems / getResultItems`，概率产出用 `ChanceItemStack#getResult(ServerLevel)` 掷。
- 需要方块（`getInputBlocks()` 非空，如质量注入要指定方块）或需要炼药锅流水的配方被跳过 —— 存储物品的语境下没法诚实还原。
- 取出前先校验数量，取出过程中任一步失败都把已取出的原样插回。

### 3. 删除铁砧修复插件

`ANVIL_REPAIR` 的物品、组件、视图、贴图、配方、语言键、行为实现全部移除（`src/main` 内已无 `anvil_repair` 引用）。

### 4. 安装台修复

- **真实 bug**：`PluginStationBlockEntity#setRemoved()` 里掉落内容物，而 `LevelChunk#clearAllBlockEntities()`（反编译确认）在区块卸载时也会调用它 → 走远一点终端和插件就掉一地。改成 `PluginStationBlock#onRemove(...)` 里掉落。
- **逻辑理顺**：暂存槽不再自动安装；新增 `StationActionPacket`（安装全部 / 全部取下），替换掉原来只能卸载的 `UninstallPluginsPacket`；界面按钮从 2 个变成 3 个，并写清「台面上的终端不工作」。

### 5. 物品的铁砧工艺化配方 + 配置开关

- 新增 15 个数据包配方：13 条**冲压**（各插件与安装台）、1 条**时移**（铁砧加工插件）、1 条**充电器充能配方**（过滤插件 → 充能插件）。
- 自定义配方条件 `anvilcraft_terminal_plugins:recipes_enabled`（NeoForge 1.21.1 的条件是 `CONDITION_CODECS` 编解码器注册表），读配置项 `enableAnvilCraftRecipes`（默认 true）。

### 6. 验证

| 项 | 结果 |
|---|---|
| `compileJava` | BUILD SUCCESSFUL（新增 5 个类：`ChargingSettings` / `ChargingPower` / `ChargingPlugin` / `AnvilProcessSettings` / `AnvilProcessPlugin` / `ConfigCondition` / `StationActionPacket`） |
| `runData build` | BUILD SUCCESSFUL，产物 `anvilcraft_terminal_plugins-neoforge-1.21.1-1.6.0.jar` |
| 配方打包 | 已确认 jar 内含 `recipe/stamping/*.json` 13 个、`recipe/time_warp/anvil_process_plugin.json`、`recipe/charger_charging/charging_plugin.json` |
| `runServer` | `Done (19.826s)! For help, type "help"`，无配方解析错误、无条件注册错误；配置文件 `anvilcraft_terminal_plugins-common.toml` 正常生成 |
| 贴图 | `charging_plugin.png`、`anvil_process_plugin.png` 生成，物品贴图共 14 张 |
## 〇、P3 收尾（版本 1.5.1）：锻造插件（合成扩展）

### 1. 为什么是锻造台

先核对本体：`ageratum/004_block/003_crate.md#合成窗口` 明确写「在仓库中放入工作台和切石机可以启用自带的合成窗口」，
也就是说**存储内的合成只覆盖工作台 + 切石机**，锻造台（`RecipeType.SMITHING`）是空白 —— 这不是重复实现，才动手。

### 2. 实现要点

| 项 | 内容 |
|---|---|
| 配方来源 | `level.getRecipeManager().getAllRecipesFor(RecipeType.SMITHING)` |
| 三件材料 | `SmithingRecipe#isBaseIngredient / isTemplateIngredient / isAdditionIngredient` 三个**本体自带的谓词**筛选，不自己解析配方 |
| 校验 | 取出前后各跑一次 `recipe.matches(SmithingRecipeInput, Level)`；只有成品 `assemble(...)` 非空才真正消耗 |
| 回滚 | 三件材料是分三次 `extractFirst` 的，任一步失败（或校验失败、成品为空）都会把**已取出的部分原样插回**存储 |
| 过滤表 | 列出**允许当基底的物品**；留空 = 什么都不做（消耗型插件的统一语义），避免悄悄消耗下界合金锭 / 模板 |

### 3. 验证

| 项 | 结果 |
|---|---|
| `compileJava` | BUILD SUCCESSFUL（`SmithingSettings` / `SmithingPlugin`） |
| `runData build` | BUILD SUCCESSFUL，产物 `anvilcraft_terminal_plugins-neoforge-1.21.1-1.5.1.jar` |
| `runServer` | `Done (9.842s)! For help, type "help"`，无报错 |
| 贴图 / 配方 | `textures/item/smithing_plugin.png`（锻造台 + 模板 + 锤子）、`recipe/smithing_plugin.json`（铁锭 + 红石 + 锻造台） |
## 〇、P3（版本 1.5.0）：生物捕捉插件 + 经验泵插件

计划里 P3 的三项是「生物捕捉 / 经验泵 / 合成扩展」。前两项本轮落地，第三项排到下一轮（原因见末尾）。

### 1. 生物捕捉插件

**关键取舍：规则不在我们这边实现，而是直接调用本体的公开 API。**
本体的树脂块（`anvilcraft:resin_block`）已经支持手动捕捉：手持右键生物，敌对 / 中立需要先施加虚弱，体积过大抓不了。
`HasMobBlockItem` 把这两件事都公开了：

| 本体 API | 用途 |
|---|---|
| `canMobBeSaved(Mob, Player, ItemStack)` | 判定某个生物能不能被这个树脂块抓（体积、虚弱、创造模式等规则全在里面） |
| `saveMobInItem(Level, Mob, Player, ItemStack)` | 实际捕捉：`SavedEntity.fromMob` → `stack.split(1)` → 设置 `SAVED_ENTITY` 组件 → 移除生物 |

因此插件只做「自动化」：从存储取一个**未装生物**的树脂块 → 找半径内最近的候选生物 → 逐个调用上面两个方法 → 把装着生物的树脂块放回存储。
插件的设置（半径 4/8/12/16、是否连敌对 / 中立一起抓）只影响**候选范围**，不影响能否成功。

**两个容易踩的坑（都已核对字节码确认）**：

1. `saveMobInItem` 在 `player != null` 时会调用 `player.getInventory().placeItemBackInInventory(...)`，把抓到的树脂块直接塞进**玩家背包**；
   我们要的是收进**存储**，所以传 `player = null`——此时它只返回结果给我们，不做任何背包写入（服务端分支不受影响）。
2. `saveMobInItem` 自己会 `mob.remove(Entity.RemovalReason.DISCARDED)`，插件**不能**再删一次，否则会有重复生物的隐患（重复删除虽然无害，但语义上必须清楚归属）。

失败路径：没有空树脂块 → 直接返回；所有候选都抓不了 → 把树脂块原样插回存储，不消耗物品。

### 2. 经验泵插件

**为什么用经验宝石而不是经验流体**：终端存储是「按类型计数」的物品模型，没有流体槽位。
本体文档写明 `1000mB 经验流体 = 1 经验宝石 = 50 玩家经验值`，所以经验宝石正好能装进现有存储，不需要引入第二套存储模型。

| 模式 | 行为 |
|---|---|
| 存入 | 等级 ≥ 阈值（默认 30）且 `totalExperience ≥ 50` 时：`giveExperiencePoints(-50)` → 一颗经验宝石进存储，每周期最多 1/2/4/8 颗 |
| 取出 | 等级 ≤ 阈值（默认 5）时：从存储取经验宝石 → `giveExperiencePoints(+50 × 数量)` |

**防丢经验**：写入走 `PluginContext#insertIntoStorage`，它会先过终端上已安装的**过滤插件**；
如果宝石被过滤掉（返回 0），插件会立刻把扣掉的 50 点经验退回，避免「经验没了、宝石也没进存储」。

### 3. 验证

| 项 | 结果 |
|---|---|
| `compileJava` | BUILD SUCCESSFUL（新增 4 个类：`MobCatcherSettings` / `XpPumpSettings` / `MobCatcherPlugin` / `XpPumpPlugin`） |
| `runData build` | BUILD SUCCESSFUL，产物 `anvilcraft_terminal_plugins-neoforge-1.21.1-1.5.0.jar`，语言键与物品模型都已生成 |
| `runServer` | `Done (9.490s)! For help, type "help"`，无报错 |
| 贴图 / 配方 | `textures/item/mob_catcher_plugin.png`（琥珀块 + 生物剪影）、`textures/item/xp_pump_plugin.png`（经验宝石 + 双向箭头）；配方分别用 `anvilcraft:resin_block` 与 `anvilcraft:exp_gem` |

### 4. 「合成扩展」为什么留到下一轮

核对本体文档后确认：存储界面的**合成窗口只支持工作台与切石机**（`ageratum/004_block/003_crate.md#合成窗口`），
所以「锻造台」确实是空白，值得做；但它需要走 `RecipeType.SMITHING` + `SmithingRecipeInput`（模板 / 基底 / 附加三件套），
消耗与回滚路径比这两个插件复杂，**单独一轮实现 + 单独验证**更稳。
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
