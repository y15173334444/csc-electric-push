# CSC: Electric Push「电推」

Create: Schematic Compute 的第一个附属模组：**Sable 子级推进器原型**。推进比例由 CSC
节点图控制——图里一个数，变成子级上的一股定向推力。

The first addon mod for Create: Schematic Compute: **a Sable sub-level thruster prototype**.
Its push ratio is driven by a CSC node graph — one number in the graph becomes directed
thrust on a sub-level.

## 机制 / Mechanism

- **控制面**：右键电推打开自持图（CSC 方块惯例），图里放「推进比例」节点（CSC 1.3.0
  公开 API 自定义节点）——`ratio` 输入给比例（0–1）、`applied` 输出钳制回读、TEXT 参数
  `pos` **留空 = 写自己**；蓝图机图里的同款节点填 `pos`（`"x y z"`）即远程写。
- **执行面**：电推 BE 实现 Sable `BlockEntitySubLevelActor`，每个物理子步在专属
  「电推推力」力组排队点力（方块中心、朝向 × 满推力 × 比例 × timeStep，帧率无关）。
- **方块**：`FACING` 六向 = 推力方向；Create 扳手转向；潜行扳手收回保留 NBT；
  `ACTIVE` 亮 = 图上正给比例；红石邻接信号 = 启停门。

Design note (mechanism level): [docs/electric-push-design.md](docs/electric-push-design.md).

## 姊妹仓 / Sibling repos

| 仓库 / Repo | 路径 / Path | 关系 / Relation |
|---|---|---|
| 主仓 Create: Schematic Compute | `../create-schematic-compute` | 本体 + 公开 API（本仓的编译契约面）/ the body plus the public API (this repo's compile contract) |
| CSC Example Addon | `../csc-example-addon` | 公开 API 示例模组的独立仓（原主仓 `tools/example-addon/`）；主仓第一方附属的动力类（舵机 / 周期变距）形态在其 README 的路线节登记、尚未动工 / the standalone repo of the public-API example addon (formerly the main repo's `tools/example-addon/`); the main repo's power-class first-party addon (servo / cyclic pitch) is noted in its roadmap, not yet in progress |

## 依赖 / Dependencies

全部强制 / all mandatory: `create_schematic_compute` `[1.3.0, 1.4)`、`create` `[6.0.10,)`、
`sable` `[1.2.2, 2.1)`。编译接线复用主仓形态：Create 走 Modrinth Maven，Sable 用
`libs/` 四件套，CSC 按兄弟目录 `build/libs` 产物编译。

## 构建与运行 / Build & run

```bash
# 1. 先构建主仓 CSC jar（兄弟目录 build/libs 产物）
../create-schematic-compute/gradlew jar

# 2. 构建本模组（编译 + 单测）
./gradlew build

# 3. 开发客户端（--no-daemon 必带：daemon 会在字体初始化崩）
./gradlew runClient --no-daemon
```

## 手动验收 / Manual acceptance

1. 创造栏拿「电推」放置，Create 扳手转向到推力方向。
2. 蓝图计算机开图 → CUSTOM「推进比例」节点 → `pos` 填电推坐标 → `CONST` 比例连入
   `ratio` → 电推 `ACTIVE` 亮起。
3. 用 Sable 装配把电推装进子级，图内给比例，观察子级受推。

## 内容侧模组 / Content-side mods

`content-mods/` 放只进 dev 运行时的内容模组（现为 Create Aeronautics 捆绑包——内容
素材；物理化本体是 Sable）——不编译、不进 mods.toml 依赖、jar 不入库。详见
[content-mods/README.md](content-mods/README.md)。

`content-mods/` holds content-only mods for the dev runtime (currently the Create
Aeronautics bundle, for portable physicalization play) — never compiled against,
never declared as dependencies, jars never committed. See
[content-mods/README.md](content-mods/README.md).

## 原型边界 / Prototype boundaries

只对 Sable 子级生效；无供电模型；占位贴图；控制面仅蓝图机宿主（自定义节点现行
准入名单）。推进比例 ∈ [0,1] 线性缩放推力（满比例 25.0 N，常量可调）。

Acts inside Sable sub-levels only; no power model; placeholder textures; the blueprint host
is the only control plane (the custom-node roster's current enrolment). The push ratio lives
in [0, 1] and scales thrust linearly (25.0 N at full ratio, a tunable constant).
