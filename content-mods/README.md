# content-mods/ — 内容侧模组 / Content-Side Mods

本目录放**内容侧**模组 jar：只进 dev 运行时（`runClient`/`runServer` 里能玩到），
**不是依赖**——不参与编译、不写进 `neoforge.mods.toml` 的依赖声明、也不入库。
`build.gradle` 把本目录的 `*.jar` 以 `runtimeOnly` 挂进运行时；目录为空时自动跳过。

This directory holds **content-side** mod jars: they enter the dev runtime only
(available in `runClient`/`runServer`) and are **not dependencies** — never compiled
against, never declared in `neoforge.mods.toml`, never committed. `build.gradle` wires
every `*.jar` here in as `runtimeOnly`; an empty folder contributes nothing.

**jar 一律不入库**：`../.gitignore` 挡住 `content-mods/*.jar`（本 README 入库）。

**The jars are never committed**: `../.gitignore` blocks `content-mods/*.jar`
(this README is tracked).

## 当前内容 / Current content

| jar | 模组 / Mod | 版本 / Version | 说明 / Notes |
| --- | --- | --- | --- |
| `create-aeronautics-bundled-1.21.1-1.2.1.jar` | **Create Aeronautics**（捆绑 Simulated + Aeronautics + Offroad 三件） | 1.2.1 | 2026-11-08 从主仓 git 历史恢复（2026-09-12 移出 `libs/` 前的那份）；供便携物理化等玩法操作 |

捆绑件的运行时依赖已核齐：`create [6.0.10,)`、`sable [1.2.1,)`、`flywheel [1.0.6,)`
由本工程 `libs/` 与 Modrinth 接线提供，`simulated` 内嵌于捆绑 jar 自带。本工程与
CSC 均不声明、不依赖它——它只是内容。

The bundled runtime requirements are all covered: `create [6.0.10,)`,
`sable [1.2.1,)` and `flywheel [1.0.6,)` come from this project's `libs/` and the
Modrinth wiring, and `simulated` ships inside the bundle itself. Neither this project
nor CSC declares or depends on it — it is content only.

## 许可 / License

`create-aeronautics-bundled` 的 LICENSE.md：**代码 MIT**（The Simulated Team / The
Creators of Aeronautics）；**`assets/` 资产 All Rights Reserved**。正因 ARR 资产，
jar 不入任何公开仓库（与主仓 2026-09-12 把它移出 `libs/` 的理由相同）。

The bundle's LICENSE.md: **code is MIT** (The Simulated Team / The Creators of
Aeronautics); **`assets/` content is All Rights Reserved**. That ARR is exactly why
the jar never enters a public repository (the same reason the main repo moved it out
of `libs/` on 2026-09-12).

## 更换 / Replacing

把新版本 jar 丢进本目录即可（同名覆盖或换名都行，旧的删掉）；构建无需改动。
需要哪一版就用哪一版——本目录不设基线、不校验哈希，内容形态由使用者自管。

Drop a newer jar in (overwrite or rename — just remove the old one); the build needs
no change. This directory keeps no baseline and checks no hashes; the content shape
is the user's to manage.
