# 电推原型设计说明 / Electric Push Prototype Design Note

状态 / Status: 原型（prototype），0.1.0
日期 / Date: 2026-11-08

## 1. 定位 / Positioning

电推是 Create: Schematic Compute（CSC）的第一个附属模组「CSC: Electric Push」的原型方块：
一个 Sable 子级推进器。一句话机制：**把 CSC 节点图里的一个数值（推进比例）变成 Sable
子级上的定向推力**。

The electric push is the prototype block of "CSC: Electric Push", the first addon mod for
Create: Schematic Compute (CSC): a Sable sub-level thruster. The mechanism in one line:
**turn one number from a CSC node graph (the push ratio) into directed thrust on a Sable
sub-level**.

## 2. 机制 / Mechanism

### 2.1 控制面：图中控制推进比例 / Control plane: the graph drives the push ratio

- 推进比例的唯一控制面是 CSC 节点图（单回路口径），有两条等价入口：
  - **自持图**（主入口）：电推自己就是图宿主（SpeedProxy 同款），右键打开图编辑器；
  - **远程图**：蓝图机等宿主的图里放同款节点，按 `pos` 寻址远程写入。
- 两条入口都走 CSC 1.3.0 公开 API 的自定义节点 `csc_electric_push:push_ratio`（「推进比例」）：
  - schema：1 入 `ratio`、1 出 `applied`（钳制后回读，供图内闭环）、TEXT 参数 `pos`；
  - 「参数即寻址」约定：`pos` 填目标电推的坐标 `"x y z"`（宿主维度内绝对坐标）；
    **`pos` 留空 = 写宿主自己**（自持图用法）；畸形输入 fail-closed（输出 0，不落到宿主）；
  - 求值（服务端主线程，同 tick 双向）：决议目标 → 定位电推 BE → 写入比例 → 输出
    实际生效值；寻址失败（未加载/不是电推）→ 输出 0，不抛异常。
- 注册即准入：构造期 `CustomNodes.register(...)` + `admitToHost(..., HOST_BLUEPRINT)`，
  节点进入蓝图机名单（注册 ≠ 准入，名单制默认拒绝）；电推自持图的名单键借用蓝图机
  的 `hostKey`（过渡口径，见 §4）。

The only control surface for the push ratio is the CSC node graph (single-loop design), with
two equivalent entries: **the self-hosted graph** (primary) — the actuator is itself a graph
host (the SpeedProxy pattern) and right-click opens its editor; and **remote graphs** — the
same node addressed by `pos` from a blueprint computer or any other host. Both ride the CSC
1.3.0 public API custom node `csc_electric_push:push_ratio`: one `ratio` input, one `applied`
output (the clamped read-back), and a TEXT `pos` param carrying the target's `"x y z"`
absolute coordinates; **a blank `pos` targets the host itself** (the self-hosted usage), and
malformed text fails closed (output 0, never the host). Evaluation is server-main-thread and
same-tick both ways. Registration is not admission: the id is enrolled into the blueprint
roster at construction; the self-hosted graph borrows the blueprint's `hostKey` for its
roster (transitional, see §4).

### 2.2 执行面：推 Sable 子级 / Execution plane: pushing a Sable sub-level

- 电推 BE 实现 Sable 的 `BlockEntitySubLevelActor`：放进子级即被 Sable 自动收纳，
  无需显式注册。
- 每个物理子步在 `sable$physicsTick` 排队一个点力：专属力组「电推推力」（自建
  `ForceGroup` 实例——Sable 内置力组常量是 Veil 的 `RegistryObject` 类型，不在 javac
  可见面，而 `ForceGroup` 本体是纯 record）、作用点为方块中心（绘图本地坐标）、
  力矢量为 `朝向 × MAX_THRUST × 推进比例 × timeStep`。乘 `timeStep` 使每游戏刻的
  总冲量与子步数无关（与 Sable 自带螺旋桨同款节拍）。
- 子步开始时力组清零、子步末统一结算冲量，休眠体会被力唤醒。

The BE implements Sable's `BlockEntitySubLevelActor` — Sable collects it automatically once
the block lives inside a sub-level, with no registration call. Each physics substep queues a
point force in `sable$physicsTick`: the actuator's own "electric push thrust" force group
(a local `ForceGroup` instance — Sable's built-in group constants are Veil's
`RegistryObject` type, outside javac's reach, while the `ForceGroup` record itself is
pure), applied at the block's centre (plot-local coordinates), with the vector
`facing × MAX_THRUST × ratio × timeStep`. Multiplying by `timeStep` keeps the
per-game-tick impulse independent of the substep count (the exact cadence of Sable's own
propeller). Force groups reset at substep start and settle
into one impulse at substep end; sleeping bodies wake when the total changes.

### 2.3 方块行为 / Block behaviour

- `FACING` 六向即推力方向；放置朝向为点击面；Create 扳手按点击面轴向旋转（
  `IWrenchable` 默认转序）；潜行扳手收回保留 NBT（比例随物品走）。
- 右键打开自持图编辑器（CSC 方块惯例；SpeedProxy 同款 Screen 接线）。
- `ACTIVE` 亮灭反映「比例 > 0」——图上正给出比例时的视觉回执。
- 红石邻接信号是启停门（红石=兜底控制通道的口径）。
- 镐挖/爆炸掉落纯物品是设计意图（NBT 只随扳手收回保留）。

`FACING` (six-way) is the thrust direction; placement faces the clicked face; the Create
wrench rotates it around the clicked face's axis (`IWrenchable`'s default rotation order);
sneak-wrench pickup keeps the NBT (the ratio rides the item). Right-click opens the
self-hosted graph editor (the CSC block convention; the SpeedProxy screen wiring).
`ACTIVE` mirrors "ratio > 0" — the visual receipt that a graph is currently commanding. A neighbouring redstone signal is
the enable gate (redstone as the fallback control channel). Pickaxe/explosion drops being
plain items is by design — NBT survives only the wrench pickup.

## 3. 参数语义 / Parameter semantics

- 推进比例 ∈ [0, 1]，线性缩放推力：0 = 停，1 = `MAX_THRUST`（25.0 N，原型常量，可调）。
  越界与 NaN 一律钳制。
- 反推负值、速度伺服等语义扩展不进原型（预留后续拍板）。

The push ratio lives in [0, 1] and scales the thrust linearly: 0 = idle, 1 = `MAX_THRUST`
(25.0 N, a tunable prototype constant). Out-of-range values and NaN clamp. Negative reverse
thrust and speed-servo semantics are explicitly out of the prototype.

## 4. 依赖边界 / Dependency boundary

- 三强制（mods.toml 全部 mandatory）：`create_schematic_compute` `[1.3.0, 1.4)`（API
  实验期至 1.4.0 冻结，上限卡 1.4）、`create` `[6.0.10,)`、`sable` `[1.2.2, 2.1)`。
- 编译接线复用 CSC 现成验证过的形态：Create 走 Modrinth Maven；Sable 用 `libs/` 四件套
  （javac 看不见 jarjar 嵌套 jar，故 Flywheel/Ponder 同袋）；CSC 本体按兄弟目录
  `build/libs` 产物编译（缺 jar 时构建期报错并提示先构建主仓）。
- Create 的原型期用途是扳手（`IWrenchable`）；动力/供电门控留待后续版本。
- **图宿主已迁公开宿主套件**（2026-11-21，CSC 波 2 落地）：BE 构造期 `HostGraphs.bind`
  拿引擎柄（声明面 `HostGraphType`，缝面空——偏差全化声明），tick/生命周期/NBT 各一行
  转发；右键开图走 `HostGraphs.openEditor` 泛用屏（自屏删除）。**名单键自声明**（过渡口径
  已销）：自持图键 = `csc_electric_push:electric_push`，bind 即声明，自持图名单走归属
  两路放行的「自声明键同命名空间」路；蓝图机名单登记保留（CSC 白名单键，第①路）。

All three mods are mandatory in mods.toml; the compile wiring reuses the shape the main repo
already validates (Create from Modrinth Maven, Sable via the `libs/` quartet because javac
cannot see jarjar-nested jars, CSC from the sibling build output with a build-time existence
check). Create's prototype-stage role is the wrench; power/energy gating is deferred.
The graph host rides the public host suite (migrated 2026-11-21, CSC wave 2): the BE
binds at construction (`HostGraphs.bind`, declarations in `HostGraphType`, an empty seam
face - the deviations all became declarations) and forwards tick/lifecycle/NBT one line
each; right-click opens the editor via `HostGraphs.openEditor`'s generic screen (the own
screen is deleted). **Roster key self-declared** (the transitional borrow is retired):
the self-graph key is `csc_electric_push:electric_push`, declared by bind, and the
self-graph roster rides the "self-declared key, same namespace" path of the two-path
ownership; the blueprint-roster enrolment stays (a CSC allowlist key, path 1).

## 5. 原型边界 / Prototype boundaries

- 只对 Sable 子级生效：电推不在任何子级上时静默不动作。
- 无供电模型：推力不计能耗。
- 贴图为占位（程序生成），正式美术后续替换。
- 控制面 = 自持图 + 蓝图机远程图（名单键借用过渡）。两个图同时写同一电推时后写覆盖
  （跨宿主写顺序未定义），勿双写。

Only acts inside a Sable sub-level; no power model (thrust is free); textures are
programmatic placeholders; the control plane is the self-hosted graph plus remote
blueprint graphs (the transitional roster-key borrow). Two graphs writing the same actuator
resolve last-writer-wins (cross-host write order is undefined) — do not double-drive.

## 6. 验收路径 / Acceptance path

1. `./gradlew build`：编译 + 单测（寻址解析、比例钳制）。
2. `./gradlew runClient --no-daemon`：放置电推 → 扳手转向；蓝图计算机图内放
   「推进比例」节点（`pos` 填电推坐标）→ `CONST` 比例连入 `ratio` → `ACTIVE` 亮起。
3. 手动推进演示：用 Sable 装配把电推装进子级，图内给比例，观察子级受推。

Build runs the unit tests (address parsing, ratio clamping); the client smoke places the
block, rotates it with the wrench, then commands a ratio from a blueprint graph and watches
`ACTIVE` light up; the full demo assembles the actuator into a Sable sub-level and observes
the push.
