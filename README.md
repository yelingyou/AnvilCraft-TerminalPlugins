# AnvilCraft-TerminalPlugins（铁砧工艺：终端插件）

给 **铁砧工艺（AnvilCraft 1.6 / MC 1.21.1 / NeoForge）** 的 **本地终端 / 潜影终端 / 超维终端**
做一套「插件（升级模块）」体系的附属模组 —— 形态参照精妙背包的升级插件。

**An English summary is available at the bottom of this page.**
（英文说明见页面末尾。）

- 模组 ID：`anvilcraft_terminal_plugins`
- 许可：**LGPL-3.0-or-later**（与铁砧工艺一致，见 `LICENSE`；第三方声明见 `NOTICE`）
- 依赖：AnvilCraft `1.6.0+`（Cjsah Maven）、AnvilLib（随 AnvilCraft 一起提供）
- 开源：欢迎 Issue / PR

---

## 一、玩法

### 1. 插件安装台

用铁砧工艺的方式把插件"装"到终端上：

1. 合成 **终端插件安装台**（8 铁锭 + 1 工作台）；
2. 右键打开界面：**左边 1 格放终端，右边 3×3 放插件**；
3. 放入插件后会自动安装到终端上：**终端右侧一排绿色槽位会实时显示已安装的插件**（共 6 格）；
4. 点 **「全部拆卸」** 把终端上已装的插件全部拆回暂存槽；点 **「调节插件」** 直接打开插件调节面板；
5. 把终端拿走即可携带插件，插件的配置保存在插件物品自身（拆下时配置一起带走）。

### 1.1 插件调节面板（终端内）

界面形态对齐精妙背包的升级标签页 —— **在铁砧工艺的终端界面里**操作：

- 打开方式：终端界面（存储界面）里的 **「≡ 插件 N」按钮**，或按 **K**；安装台界面点「调节插件」；
- 面板是叠加层，**不会关闭你已经打开的终端界面**；
- **位置默认贴屏幕左侧**（JEI 的素材列表默认在右侧，避免重叠）；**按住「≡」按钮可以把整个面板拖到任意位置**，
  拖动后的位置会记在 `config/anvilcraft_terminal_plugins_panel.txt`，下次进游戏仍然生效；
- 交互方式**对齐精妙背包的升级设置**：每个设置就是一个按钮，**按钮上直接写着当前值**（如「半径 5」「配方 熔炼」「阈值 6」「模式 磁吸 + 拾取」），
  点一下循环切换，鼠标悬停会显示说明 tooltip；
- 面板里可以：
  - 查看终端上已安装的插件列表（图标 + 名称 + 开关状态；最多显示 5 个，更多会显示「+N」）；
  - **设置按钮**：直接显示并切换该插件的两个主设置（不同插件的含义见下表）；
  - **↑ / ↓**：调整插件顺序；
  - **启用 / 停用**：单独关掉某个插件而不拆下它；
  - **拆下**：把插件退回玩家背包；
  - 选中炼金插件时，下方出现 **炼金条目配置**：4 行，每行是「药水槽（左键放入光标上的药水 / 右键清空）＋ 触发条件按钮（点击循环）＋ 阈值 −/+」，
    下方还有作用半径按钮。

| 插件 | 设置按钮 1（主） | 设置按钮 2（副） |
|---|---|---|
| 过滤 | 白名单 / 黑名单 | 比对组件 开 / 关 |
| 磁吸 · 拾取 | 半径 3 / 5 / 8 / 12 / 16 | 模式：磁吸 + 拾取 / 仅磁吸 / 仅拾取 |
| 自动熔炼 · 烹饪 | 配方：熔炼 / 高炉 / 烟熏 / 营火 | 燃料 开 / 关 |
| 自动喂食 | 阈值 2 / 4 / 6 / 8 / 12 / 16 | 负面食物 开 / 关 |
| 炼金 | 作用范围：仅自己 / 玩家 / 生物 / 全部 | 等级匹配 开 / 关 |

### 1.2 手持快速配置

也可以手持插件 **右键切换主档位**，**潜行 + 右键切换副档位**（聊天栏即时提示当前值）：

| 插件 | 右键（主） | 潜行右键（副） |
|---|---|---|
| 过滤 | 白名单 ↔ 黑名单 | 是否比对组件 |
| 磁吸 / 拾取 | 半径 3 → 5 → 8 → 12 → 16 | 磁吸/拾取 两者 → 仅磁吸 → 仅拾取 |
| 自动熔炼 · 烹饪 | 熔炼 → 高炉 → 烟熏 → 营火 | 是否需要燃料 |
| 自动喂食 | 饥饿阈值 2 → 4 → 6 → 8 → 12 → 16 | 是否允许负面效果食物 |
| 炼金 | 作用范围 仅自己 → 玩家 → 生物 → 全部 | 是否要求匹配效果等级 |

### 2. 五个插件（第一版）

| 插件 | 作用 | 默认行为 |
|---|---|---|
| **过滤插件** | 决定哪些物品允许进入终端连接的存储 | 复用铁砧工艺自身的过滤组件：支持白名单/黑名单、是否比对组件、命名牌写 `#tag` 做标签过滤 |
| **磁吸 / 拾取插件** | 把附近的掉落物收进存储 | 半径 5 格内自动拾取；可配置半径与是否启用磁吸（拉向玩家） |
| **自动熔炼 · 烹饪插件** | 在存储里自动加工原料 | 默认熔炼（可切高炉/烟熏/营火），每 40 tick 处理一批，可选消耗燃料 |
| **自动喂食插件** | 饥饿时自动进食 | 饥饿值 ≤ 6 时从存储取食物；默认拒绝负面效果食物；碗一类容器会放回存储 |
| **炼金插件** | **按条件自动使用药水**（语义对齐精妙背包炼金升级） | 4 个条目，每条 = 过滤药水 + 触发条件（从不/总是/水下/着火/下落/疾跑/受伤低于 X/有负面效果）+ 阈值；满足条件就从存储取一瓶匹配药水：普通药水直接饮用、喷溅/滞留药水就地投掷、不祥之瓶照常使用；空瓶放回存储 |

所有插件都作用于**终端当前连接的存储**（本地终端=32 格内最近的大型板条箱；潜影终端=身上的潜影集装箱；超维终端=绑定的全局存储）。

---

## 二、设计要点

```
终端物品
 └── DataComponent: anvilcraft_terminal_plugins:installed_plugins  ← 已安装插件（每项是一枚插件物品堆栈）
       └── 插件物品自己的组件：magnet_settings / cooking_settings / feeding_settings / alchemy_settings
                                （过滤插件复用 AnvilCraft 的 filter_content 组件）
```

- **行为派发**：`TerminalPluginEvents` 监听 `PlayerTickEvent.Post`，每 `pluginTickInterval`（默认 10）tick
  扫描玩家身上的终端，按每个插件的 `intervalTicks()` 调用 `onPlayerTick`。
- **存储接入**：`TerminalStorageResolver` 复用铁砧工艺的 `TerminalBlockRegistry` / `Storages` /
  `StorageServerStub.terminalTargetId` 解析目标存储，得到 `BaseStorage#getItems()`
  （`UnlimitedItemStacksResourceHandler`，按类型计数、数量无限），再通过它的 `insertItem` / `extractUnlimited` 读写。
- **过滤统一入口**：所有自动入库都走 `PluginContext#insertIntoStorage`，在那里先过过滤插件。
- **无 mixin**：完全不碰铁砧工艺内部实现。终端内的插件面板通过 NeoForge 的 `ScreenEvent`（Render.Post / MouseButtonPressed.Pre / KeyPressed.Pre）
  以叠加层形式画在 `StorageScreen` 上，所以不会关闭玩家已打开的终端界面，也不需要改铁砧工艺的源码。

---

### 1.3 插件不生效怎么排查

把配置 `config/anvilcraft_terminal_plugins-common.toml` 里的 `debug_logging` 改成 `true`，
日志（`logs/latest.log`）里会打印每次派发：

```
plugin-dispatch tick=120 terminal=本地终端 plugins=2 storage=reachable(1)
plugin-run 终端自动喂食插件 tick=120
```

- 没有 `plugin-dispatch` ⇒ 终端不在物品栏/双手中，或没装插件；
- `storage=UNREACHABLE` ⇒ 存储连不上（本地终端要 32 格内有大型板条箱；潜影终端要有潜影集装箱；超维终端要绑定存储）；
- 有 `plugin-run` 但没反应 ⇒ 多半是插件自身条件没满足（存储里没有对应物品、饥饿值还够、药水不匹配等）。

---

## 三、已知限制

1. **过滤插件只约束"本附属的自动入库"**（磁吸拾取、烹饪产物、炼金产物）。
   玩家在终端界面里手动存入的物品由铁砧工艺本体处理，不受本插件限制 —— 要覆盖它需要 mixin。
2. 自动烹饪使用**原版烹饪配方**（熔炉/高炉/烟熏/营火）。
   铁砧工艺的"超级加热 / 烹饪"是 InWorld（世界里多方块）配方，不适合在存储内直接套用。
3. 炼金插件按条件使用**存储里现成的药水**（与精妙背包一致）；喷溅药水是就地投掷生效，不做弹道瞄准。
4. 终端界面本身没有插件页签（那需要 mixin `StorageScreen`），插件配置目前通过物品组件/JSON 调整。

---

## 四、开发

```bash
# 需要 JDK 21
set JAVA_HOME=C:\Program Files\Java\jdk-21.0.11
gradlew.bat compileJava       # 编译（已验证通过）
gradlew.bat runData           # 重新生成 blockstate/item model/lang/loot_table
gradlew.bat build             # 打包 build/libs/*.jar（已验证通过）
gradlew.bat runServer         # 专用服务器实测（已验证通过）
gradlew.bat runClient         # 起客户端
```

> 物品/方块的模型、方块状态、掉落表、英文语言文件由 **数据生成器**产出（`src/generated/resources`），
> 不要手写同名文件；中文语言、配方、贴图是手写资源（`src/main/resources`）。
> 详细验证记录与沙箱绕行见 `VERIFICATION.md`。

依赖（`gradle/libs.versions.toml`）：

- `dev.dubhe:anvilcraft-neoforge-1.21.1:1.6.0+snapshot.2270`（Cjsah Maven `https://server.cjsah.net:1002/maven/`）
- `dev.anvilcraft.lib:anvillib-neoforge-1.21.1:2.0.0+snapshot.521`

主要目录：

```
src/main/java/dev/anvilcraft/addon/terminalplugins/
├── plugin/            插件 API：TerminalPlugin / PluginContext / TerminalPluginRegistry / TerminalStorage(Resolver)
│   └── impl/          五个插件的实现
├── component/         数据组件（已安装插件列表 + 各插件配置）
├── item/              插件物品
├── block/(entity)/    插件安装台方块与方块实体
├── inventory/         安装台 Menu
├── client/            插件调节面板（叠加在终端界面上）、按键、客户端事件
├── client/screen/     安装台界面
├── event/             服务端 tick 派发
├── network/           拆卸插件的网络包
└── init/              注册入口（物品/方块/方块实体/菜单/数据组件/网络/创造标签页）
```

---

## 五、Roadmap

- [x] 插件调节面板（终端界面内打开，可开关 / 排序 / 改配置 / 拆下）
- [ ] 过滤表的图形化编辑（当前过滤表仍借助铁砧工艺自身的过滤组件）
- [ ] 更多插件：销毁(Void)、容量/类型扩容、流体罐、电池、经验泵、压缩
- [ ] 可选的 mixin 方案：让过滤插件约束"玩家在终端界面手动存入"的物品

---

## 六、致谢 / Acknowledgements

### 中文

这个附属能写出来，靠的是别人的肩膀，在此郑重致谢：

- **铁砧工艺（AnvilCraft）开发团队 / Anvil-Dev（Gugle 及各位贡献者）** —— 本体模组、附属开发文档、官方附属模板与构建脚手架。
  终端的存储网络、`IStorageType`、`StoredItem` 系列 API 都由它提供；没有它就没有这个附属。
- **AnvilLib** —— 提供了 Registrum 注册框架、配置系统与数据生成能力。
- **精妙背包 / Sophisticated Backpacks 与 Sophisticated Core（作者 P3pp3rF1y）** —— 「插件（升级）」这一交互形态，
  以及**炼金插件「按条件自动用药」的语义设计**，都是参考它而来的。本项目**只参考行为设计，未复制任何代码、资源或数据文件**。
- **NeoForge / MinecraftForge 团队** —— 模组加载器与 API。
- **Mojang Studios / Microsoft** —— Minecraft 本身（本项目不含任何官方资源）。
- **所有提交问题、反馈与测试的玩家** —— 你们的 bug 报告（比如创造模式页签那个崩溃）让这个模组真的能用起来。

### English

This addon stands on the shoulders of others. Sincere thanks to:

- **The AnvilCraft team / Anvil-Dev (Gugle and contributors)** — the base mod, the official addon documentation,
  and the addon template whose build scaffolding this repository is based on. The terminal storage network,
  `IStorageType` and the `StoredItem` APIs all come from AnvilCraft; without it this addon would not exist.
- **AnvilLib** — for the Registrum registration framework, the configuration system and data generation.
- **Sophisticated Backpacks / Sophisticated Core by P3pp3rF1y** — the "upgrade module" interaction model and the
  semantics of the alchemy upgrade (condition-based automatic potion use) were used as **design references only**.
  No source code, assets or data files from those projects are copied into or redistributed by this project.
- **The NeoForge / MinecraftForge teams** — for the mod loader and APIs.
- **Mojang Studios / Microsoft** — for Minecraft itself (this project ships no official assets).
- **Everyone who reported issues, gave feedback and tested** — your crash reports (such as the creative-tab crash)
  are what made this mod actually usable.

Also thanks to the authors of the many open-source projects whose tools made development possible.

---

## 七、许可 / License

- 本项目以 **GNU Lesser General Public License v3.0 or later（LGPL-3.0-or-later）** 发布，与铁砧工艺保持一致。
  完整文本见 [`LICENSE`](LICENSE)，第三方组件与设计参考的声明见 [`NOTICE`](NOTICE)。
- 你可以自由地使用、修改、再分发本模组，**包括放进整合包**；再分发修改版时请保留同样的许可与版权声明。
- 源码文件均带 `SPDX-License-Identifier: LGPL-3.0-or-later` 头。
- 本模组不打包 AnvilCraft、AnvilLib、NeoForge 或 Minecraft 的任何代码与资源，它们各自遵循自己的许可。
- 本项目与 AnvilCraft、Sophisticated Backpacks、NeoForge、Mojang 均无隶属或赞助关系。

- Released under the **GNU Lesser General Public License v3.0 or later (LGPL-3.0-or-later)**, matching AnvilCraft.
  See [`LICENSE`](LICENSE) for the full text and [`NOTICE`](NOTICE) for third-party notices.
- You are free to use, modify and redistribute this mod, **including in modpacks**; redistributed forks must keep
  the same license and copyright notices.
- Every source file carries an `SPDX-License-Identifier: LGPL-3.0-or-later` header.
- No code or assets from AnvilCraft, AnvilLib, NeoForge or Minecraft are bundled; they keep their own licenses.
- This project is not affiliated with, endorsed by or sponsored by AnvilCraft, Sophisticated Backpacks,
  NeoForge or Mojang.

---

## English Summary

**AnvilCraft-TerminalPlugins** is an addon for **AnvilCraft** (Minecraft 1.21.1 / NeoForge) that adds an
**upgrade-module ("plugin") system for the terminals** — Local Terminal, Shulker Terminal and Hyperdimension Terminal —
inspired by the upgrade model of Sophisticated Backpacks.

**What it adds**

- **Terminal Plugin Station** — put a terminal on the left and plugins on the right; plugins are installed automatically,
  the installed list is shown live, and one button takes them all back out again.
- **In-terminal plugin panel** — open it from the "Plugins" button in the terminal screen (or press **K**).
  It is drawn as an overlay, so your open terminal GUI is not closed and no mixins are required.
  From there you can inspect, enable/disable, reorder, reconfigure or remove installed plugins.
- **Five plugins (first release)**
  - **Filter** — decides which items may enter the bound storage (reuses AnvilCraft's own filter component:
    whitelist/blacklist, component matching, `#tag` filters).
  - **Magnet / Pickup** — pulls nearby drops in and stores them directly (configurable radius and mode).
  - **Auto Cooking** — smelts/cooks ingredients inside the storage (furnace, blast furnace, smoker, campfire;
    optional fuel).
  - **Feeding** — feeds you from the storage when you get hungry (harmful food blocked by default).
  - **Alchemy** — uses stored potions automatically when a condition is met (underwater, on fire, falling,
    sprinting, hurt below X%, having a debuff, ...), matching the semantics of the Sophisticated Backpacks
    alchemy upgrade: drink normal potions, throw splash/lingering ones, and put the empty bottles back.

All plugins act on the storage the terminal is currently connected to (nearest large crate within 32 blocks,
the shulker container you carry, or the hyperdimension storage the terminal is bound to).

**Building** — JDK 21, then `gradlew.bat build` (or `./gradlew build`); `gradlew.bat runData` regenerates models,
blockstates and language files; `gradlew.bat runServer` / `runClient` start a dev server / client.

**License** — LGPL-3.0-or-later. See [`LICENSE`](LICENSE) and [`NOTICE`](NOTICE).
**Credits** — see the bilingual acknowledgements above.
