# 上架 CurseForge 方案（铁砧工艺：终端插件）

本文是从零把本模组发布到 CurseForge 的操作方案，包含网页流程、自动化接线、审核规则与已知坑。

## 一、需要准备的东西

| 素材 | 现状 | 要求 |
|---|---|---|
| CurseForge 账号 | 需要你操作 | 登录作者门户即可建项目，不需要单独申请资格 |
| 项目头像 | `docs/curseforge-avatar-512.png`（本次新生成，512×512） | 官方要求 ≥400×400、1:1、PNG；`docs/cover.png` 是 1280×640 非 1:1，不能当头像；jar 内的 `icon.png` 保持 256×256 不变 |
| 项目截图 3~5 张 | `docs/ui-preview/*.png` 可先用 | 建议进游戏截真机图：安装台、插件面板列表页、执行页、物品与配方 |
| 项目描述 | `docs/WIKI.md`（中英对照） | 描述里英文在前、中文在后；明确写出「非官方附属」与「需要 AnvilCraft 1.6.0+」 |
| 许可 | LGPL-3.0-or-later | 下拉选 GNU LGPL v3；若下拉无精确项则用 Custom License 填 `LGPL-3.0-or-later` + https://www.gnu.org/licenses/lgpl-3.0.html |
| 前置声明 | 需要你在项目里加 | AnvilCraft：CurseForge 项目 id 986251，slug `anvilcraft` |
| 构建产物 | `build/libs/anvilcraft_terminal_plugins-neoforge-1.21.1-<版本>.jar` | 只传主 jar，不要传 sources，也不要把本体打进去 |

## 二、创建项目（网页）

1. 登录 https://authors.curseforge.com/ ，Create a Project，Game 选 Minecraft；
2. Project name：AnvilCraft-TerminalPlugins（不要带版本号或 Mod 字样）；
3. Summary：一句话英文简介；
4. Class 选 Mods；Main category 建议 Addons，Additional categories 可选 Technology、Storage；
5. Main version 1.21.1，Mod loader NeoForge；
6. License 选 LGPL-3.0，并建议开启 Distribution 开关；
7. Description 粘贴 `docs/WIKI.md` 内容（把英文摘要放前面）；
8. 上传头像与截图，保存。

建好之后在项目页补齐：Source = GitHub 仓库，Issues Tracker = GitHub Issues，Images = 截图，并把 AnvilCraft 设为默认 Required Dependency。

## 三、上传第一个文件

| 字段 | 填法 |
|---|---|
| Choose file | `anvilcraft_terminal_plugins-neoforge-1.21.1-1.7.2.jar` |
| Display Name | `1.7.2` |
| Release Type | Release（调试阶段可用 Beta；Alpha 不会同步到启动器） |
| Changelog | 必填，内容与 GitHub Release 正文一致 |
| Supported Version | 1.21.1 |
| Modloader | NeoForge |
| Java version | Java 21 |
| Environment | Client 与 Server 都要勾（本模组有服务端 tick 与网络包） |
| Related Projects | AnvilCraft = Required Dependency |

## 四、自动化（推荐：先修好仓库里已有的 mc-publish）

说明：**Minotaur 只能发 Modrinth，不能发 CurseForge**，所以本仓库走的是 `Kir-Antipov/mc-publish`。
`.github/workflows/release.yml` 里已经接好了 mc-publish（同时发 Modrinth 与 CurseForge），但有四处需要修改：

1. `if: ${{ steps.properties.outputs.upload }}` 在 GitHub Actions 里恒为真（`"false"` 是非空字符串），要改成 `== 'true'`；
2. 依赖行 `anvilcraft@1.21.1-1.5.0` 与本体要求不符，要改成 `anvilcraft(required){curseforge:986251}`；
3. 要补上 CurseForge 必需的 `environment: client | server` 与 `loaders: neoforge`；
4. `continue-on-error: true` 会让发布失败被静默吞掉，建议改为 false。

修改后的发布步骤应为（其余部分不变）：

```yaml
      - name: publish neoforge mc mod
        uses: Kir-Antipov/mc-publish@v3.3.1
        if: ${{ steps.properties.outputs.upload == 'true' }}
        continue-on-error: false
        with:
          name: "${{ steps.properties.outputs.mod_name }} For NeoForge v${{ steps.version.outputs.version }}"
          version: ${{ steps.version.outputs.version }}
          game-versions: 1.21.1
          loaders: neoforge
          version-type: ${{ env.VERSION_TYPE }}
          java: 21
          environment: client | server
          fail-mode: skip
          changelog: ${{ github.event.release.body }}

          modrinth-id: ${{ steps.properties.outputs.modrinth_id }}
          modrinth-token: ${{ secrets.MODRINTH_TOKEN }}
          modrinth-featured: true

          curseforge-id: ${{ steps.properties.outputs.curseforge_id }}
          curseforge-token: ${{ secrets.CURSEFORGE_TOKEN }}

          dependencies: |
            anvilcraft(required){curseforge:986251}

          files: |
            build/libs/${{ steps.properties.outputs.mod_id }}-neoforge-${{ steps.version.outputs.version }}.jar
```

注意：修改 `.github/workflows/` 下的文件需要带 `workflow` 权限的 token（或在 GitHub 网页上直接用编辑器改）。
本次推送所用的 token 没有该权限，所以这一步需要你在网页上应用上面的补丁。

你还需要做三步（都需要你账号上的信息）：

1. 在 GitHub 仓库 Settings → Secrets and variables → Actions 添加 `CURSEFORGE_TOKEN` 与 `MODRINTH_TOKEN`；
2. 把 `gradle.properties` 的 `upload=false` 改成 `true`，`curseforge_id=xxxxx` 换成你的项目 id（数字），`modrinth_id=xxxxx` 换成 Modrinth 项目 id；
3. 之后每发一次 GitHub Release，就会自动同时发布到两个平台（Changelog 取 Release 正文）。

CurseForge 的上传 token 在作者站点 API Tokens 页面生成：https://legacy.curseforge.com/account/api-tokens
注意它和第三方只读 API Key（`api.curseforge.com` 用的 `$2a$10$...`）不是一回事，后者需要另行申请。

## 五、备用方案：CurseForgeGradle（本地补发/应急）

插件 id 为 `net.darkhax.curseforgegradle`（接入时以插件页当前版本为准），在 `build.gradle` 里注册任务后：

```groovy
tasks.register('publishCurseForge', net.darkhax.curseforgegradle.TaskPublishCurseForge) {
    apiToken = System.getenv('CURSEFORGE_TOKEN')
    debugMode = false                 // true = 只打印不发送，用于试跑
    def mainFile = upload(project.findProperty('curseforge_id'), tasks.named('jar').get())
    mainFile.releaseType = 'release'  // 插件默认是 alpha，必须显式改
    mainFile.changelogType = 'markdown'
    mainFile.changelog = file('CHANGELOG.md').text
    mainFile.addGameVersion('1.21.1')
    mainFile.addModLoader('NeoForge')
    mainFile.addJavaVersion('Java 21')
    mainFile.addEnvironment('Client', 'Server')
    mainFile.addRequirement('anvilcraft')
}
```

命令：`gradlew.bat build publishCurseForge`（token 放环境变量，不要提交进仓库）。

## 六、也可以直接用上传 API（脚本兜底）

`POST /api/projects/{projectId}/upload-file`，multipart 两个字段：`metadata`（JSON 字符串）与 `file`，认证用 `X-Api-Token` 头。
metadata 支持 `changelog`、`changelogType`、`displayName`、`releaseType`、`gameVersionNames`（如 `["1.21.1","NeoForge","Java 21","Client","Server"]`）与 `relations.projects`（`{"slug":"anvilcraft","type":"requiredDependency"}`）。

## 七、审核规则里最容易踩的几条

1. 描述里不能出现「去某某网站下载」这类站外下载引导；仓库链接请放 Source 字段与描述最底部；
2. 必须至少有一个 Release 类型的文件才会同步到启动器；
3. Changelog 必填；
4. 文件名不能含表情、非拉丁字符或特殊符号，不能 zip 套 zip，不能带 `__MACOSX`、`.DS_Store`；
5. 与已有文件哈希相同的重复文件会被拒；
6. 必须声明 Environment（Client / Server）；
7. 不要靠重传微小改动刷更新；
8. AI 生成的宣传图需要可见免责声明。

## 八、硬依赖附属特有的注意事项

1. 不声明 Required Dependency，启动器不会自动装 AnvilCraft，玩家启动必崩；
2. CurseForge 的项目关系不带版本范围，「需要 AnvilCraft 1.6.0 及以上」只能在描述与 Changelog 里用文字写清；
3. `neoforge.mods.toml` 已声明 `anvilcraft [1.6.0,)`，平台关系要与它保持一致；
4. 不要把 AnvilCraft 或 AnvilLib 打进 jar；
5. 项目名含 AnvilCraft 时，描述里要写明这是非官方附属、与本体团队无隶属关系。

## 九、README 徽章（拿到项目 id 后再加）

```markdown
[![CurseForge](https://img.shields.io/curseforge/dt/<PROJECT_ID>?logo=curseforge&label=CurseForge)](https://www.curseforge.com/minecraft/mc-mods/<你的 slug>)
[![GitHub Release](https://img.shields.io/github/v/release/yelingyou/AnvilCraft-TerminalPlugins)](https://github.com/yelingyou/AnvilCraft-TerminalPlugins/releases)
```

## 十、需要你提供的信息

1. CurseForge 项目 id（建完项目后从项目页 URL 取得）；
2. `CURSEFORGE_TOKEN`（放进 GitHub Secrets，不要贴到公开场合）；
3. Modrinth 项目 id（若同时发 Modrinth）。

> 说明：网页字段名与审核规则来自 CurseForge 官方支持文档与官方博客；Gradle 插件的具体版本号、以及「Environment 何时成为强制项」在官方站点的表述不完全一致，接入时会以插件页与门户实际校验为准。
