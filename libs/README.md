# libs/ — 编译期与运行期依赖 / Compile- and Runtime Dependencies

本目录的 jar 由 `.gitignore` 的 `!libs/*.jar` 显式保留在版本控制中，用于本地编译、测试与
dev 运行。它们与主仓 CSC 的 `libs/` 基线逐字节同源（同一次拷贝），Sable 兼容性以主仓
`tools/sable-compat` 矩阵为准。

The jars in this directory are deliberately kept in version control (`.gitignore`,
`!libs/*.jar`) for local compilation, testing and dev runtime. They are byte-identical to
the main CSC repo's `libs/` baseline (copied in one pass); Sable compatibility follows the
main repo's `tools/sable-compat` matrix.

完整的许可与义务分析见主仓 [`docs/third-party-licenses.md`](../../create-schematic-compute/docs/third-party-licenses.md)。

For the full license and obligation analysis see the main repo's
[`docs/third-party-licenses.md`](../../create-schematic-compute/docs/third-party-licenses.md).

## 清单 / Inventory

| jar | 许可 / License | 版权 / Copyright | 编译是否需要 / Needed to compile |
| --- | --- | --- | --- |
| `sable-neoforge-1.21.1-1.2.2.jar` | **PolyForm Shield 1.0.0** + natives Apache-2.0 + 内嵌 **Veil (LGPLv3)** / embedded **Veil (LGPLv3)** | RyanHCode / dimforge (Rapier) / amo, Cappin, Ocelot (Veil) | ✅ 必需 / required |
| `sable-companion-common-1.21.1-1.6.0.jar` | MIT | (c) 2026 RyanHCode | ✅ 必需：javac 看不到嵌套 jar，且调用的 Sable API 签名里带 companion 类型 / required: javac cannot see nested jars and the Sable API called here returns companion types |
| `flywheel-neoforge-1.21.1-1.0.6.jar` | MIT | (c) 2021-2024 Jozufozu | ✅ 必需 / required（javac 看不到 Create 内嵌的那份 / javac cannot see Create's nested copy） |
| `ponder-neoforge-1.0.85+mc1.21.1.jar` | MIT | (c) 2022 The Create Team | ✅ 必需 / required |

Create 不在这里——见 `build.gradle` 的 `maven.modrinth:create`（版本在
`gradle.properties` 的 `create_maven_version`）。CSC 本体按兄弟目录
`../create-schematic-compute/build/libs/` 产物编译，同样不入本目录。

Create is not here — see `maven.modrinth:create` in `build.gradle` (version in
`gradle.properties` → `create_maven_version`). The CSC body compiles against the sibling
`../create-schematic-compute/build/libs/` output and likewise never lands here.

## 两条硬规则 / Two Hard Rules

**1. 不要重打包任何 jar。 / Do not repackage any jar.**

`sable-neoforge` 内嵌 Veil（LGPLv3）与 Rapier natives（Apache-2.0）；LGPL §4 要求接收方
能替换/修改该库，裁剪嵌套 jar 即实质违反 LGPL。同理 Apache-2.0 §4(a)(b) 的通知文件必须
原样传递。

`sable-neoforge` embeds Veil (LGPLv3) and the Rapier natives (Apache-2.0); LGPL §4 requires
that recipients stay able to replace or modify that library, so pruning the nested jar is a
substantive LGPL violation. Likewise the Apache-2.0 §4(a)(b) notices must travel unmodified.

**2. 不要把这些 jar 并入以 MIT 发布的产物。 / Do not merge these jars into anything released under MIT.**

PolyForm Shield 的 "No Other Rights" 禁止 sublicense / transfer，`libs/` 只能作为构建依赖
（`compileOnly` / `runtimeOnly`）。需要新第三方依赖时优先用版权方自己的 Maven 渠道
（如 Modrinth Maven），不要把带 ARR 资产的 jar 提交进来。

PolyForm Shield's "No Other Rights" forbids sublicensing and transfer, so `libs/` may only
serve as build dependencies (`compileOnly` / `runtimeOnly`). For new third-party
dependencies prefer the authors' own Maven channel (such as Modrinth Maven) over vendoring
an ARR-bearing jar.

## 通知 / Notices

```text
Third-Party Notices / 第三方声明

Sable is licensed under the PolyForm Shield License 1.0.0:
https://polyformproject.org/licenses/shield/1.0.0
Its bundled Rapier natives are a modified version of
https://github.com/ryanhcode/rapier, under Apache License 2.0
(see natives/sable_rapier/LICENSE-RAPIER inside the jar).
Sable also bundles Veil (modId "veil", authors amo, Cappin, Ocelot),
licensed under the GNU Lesser General Public License v3
(see META-INF/jarjar/veil-neoforge-1.21.1-4.0.0.jar inside the jar).

Flywheel, Ponder and Catnip code is MIT licensed. Create is consumed
from Modrinth Maven and is not redistributed by this project; the All Rights
Reserved assets inside its jar are not redistributed either.

--- 中文版 ---

Sable 以 PolyForm Shield License 1.0.0 授权：
https://polyformproject.org/licenses/shield/1.0.0
它内嵌的 Rapier natives 是 https://github.com/ryanhcode/rapier 的修改版，
以 Apache License 2.0 授权（见 jar 内 natives/sable_rapier/LICENSE-RAPIER）。
Sable 还内嵌了 Veil（modId "veil"，作者 amo、Cappin、Ocelot），
以 GNU Lesser General Public License v3 授权
（见 jar 内 META-INF/jarjar/veil-neoforge-1.21.1-4.0.0.jar）。

Flywheel、Ponder、Catnip 的代码以 MIT 授权。Create 从 Modrinth Maven 获取，
本项目不分发它，也不分发其 jar 内 All Rights Reserved 的资产。
```
