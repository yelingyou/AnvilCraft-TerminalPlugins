# 贡献指南 / Contributing

感谢你愿意为 **AnvilCraft-TerminalPlugins** 出一份力！
Thanks for taking the time to contribute to **AnvilCraft-TerminalPlugins**!

## 中文

### 环境

- JDK 21
- IntelliJ IDEA（推荐）+ Minecraft Development 插件
- `gradlew.bat build` 构建；`gradlew.bat runData` 重新生成模型/方块状态/语言文件；`gradlew.bat runServer` / `runClient` 起开发环境

### 提交前

1. 跑一遍 `gradlew.bat build`，确保编译与打包通过；
2. 如果改了物品 / 方块 / 语言键，跑 `gradlew.bat runData` 并**把 `src/generated/resources` 的变更一起提交**；
3. 遵守代码风格：所有源码文件带 `SPDX-License-Identifier: LGPL-3.0-or-later` 头；
4. 新插件请实现 `TerminalPlugin` 接口并在 `TerminalPlugins#register()` 里登记，配置以数据组件形式挂在插件物品上。

### 提交 Issue

报告崩溃时请附上 `crash-reports/` 里的崩溃报告 + `logs/latest.log`，以及模组列表与 AnvilCraft 版本。

## English

### Environment

- JDK 21
- IntelliJ IDEA (recommended) with the Minecraft Development plugin
- `gradlew.bat build` to build, `gradlew.bat runData` to regenerate models/blockstates/lang,
  `gradlew.bat runServer` / `runClient` for a dev environment

### Before you open a PR

1. Run `gradlew.bat build` and make sure it succeeds;
2. If you touched items, blocks or language keys, run `gradlew.bat runData` and **commit the changes under
   `src/generated/resources`** together with your code;
3. Keep the code style: every source file carries an `SPDX-License-Identifier: LGPL-3.0-or-later` header;
4. New plugins implement the `TerminalPlugin` interface and are registered in `TerminalPlugins#register()`;
   their settings live in data components attached to the plugin item.

### Reporting issues

Please attach the crash report from `crash-reports/`, `logs/latest.log`, your mod list and the AnvilCraft version.

## License

By contributing you agree that your contributions are licensed under **LGPL-3.0-or-later** (see `LICENSE`).
贡献即表示你同意以 **LGPL-3.0-or-later** 授权你的贡献（见 `LICENSE`）。
