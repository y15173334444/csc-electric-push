package io.github.y15173334444.csc_electric_push.block;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.y15173334444.csc_electric_push.ElectricPush;
import io.github.y15173334444.csc_electric_push.node.ElectricPushNodes;
import io.github.y15173334444.csc_electric_push.node.NodeInputs;
import io.github.y15173334444.create_schematic_compute.blocks.GraphHost;
import io.github.y15173334444.create_schematic_compute.blocks.SyncedGraphBlockEntity;
import io.github.y15173334444.create_schematic_compute.graph.NodeAllowance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

/**
 * 电推 BE：CSC 图宿主 + Sable 子级推进器，二合一。
 * <ul>
 *   <li><b>控制面</b>：自持节点图（右键开图，SpeedProxy 同款 L 档宿主骨架），图里
 *       「推进比例」自定义节点（{@code pos} 留空=写自己）驱动比例；蓝图机图里的同款
 *       节点带 {@code pos} 寻址仍可远程写入。</li>
 *   <li><b>执行面</b>：{@link BlockEntitySubLevelActor} 物理子步排队点力——专属力组
 *       「电推推力」、作用点=方块中心（绘图本地坐标）、矢量=
 *       {@code 朝向 × MAX_THRUST × 比例 × timeStep}（帧率无关，Sable 螺旋桨同款节拍）。</li>
 * </ul>
 * 图宿主基类走 CSC 内部面（{@code SyncedGraphBlockEntity}/{@code GraphHost}，公开
 * 宿主套件波 2 落地后迁移）。
 * <p>The electric push BE: a CSC graph host and a Sable sub-level thruster in one.
 * Control plane: its own node graph (right-click to open, the SpeedProxy-style L-variant
 * host skeleton), driven by the "push ratio" custom node ({@code pos} blank = write the
 * host itself); the same node in a blueprint graph still writes remotely by {@code pos}.
 * Execution plane: {@link BlockEntitySubLevelActor} queues a point force per physics
 * substep - the actuator's own "electric push thrust" force group, applied at the block
 * centre (plot-local), {@code facing × MAX_THRUST × ratio × timeStep} (frame-rate
 * independent, the cadence of Sable's own propeller). The host base classes are CSC
 * internal surface ({@code SyncedGraphBlockEntity}/{@code GraphHost}); migrate once the
 * public host suite (wave 2) lands.</p>
 */
public class ElectricPushBlockEntity extends SyncedGraphBlockEntity
    implements GraphHost.TickHooks, BlockEntitySubLevelActor {

    /** 满比例推力（牛顿）：原型常量，可调。Full-ratio thrust in newtons: a tunable prototype constant. */
    public static final double MAX_THRUST = 25.0;

    /** 专属力组「电推推力」：Sable 力 overlay 里独立归因。自建实例而非
     * {@code ForceGroups.PROPULSION}——后者是 Veil 的 {@code RegistryObject} 类型，
     * javac 看不见 jarjar 嵌套的 Veil；{@link ForceGroup} 本体是纯 record，可直接构造。
     * The actuator's own force group: separate attribution in Sable's force overlay.
     * A local instance instead of {@code ForceGroups.PROPULSION} - the latter is Veil's
     * {@code RegistryObject} type and javac cannot see jarjar-nested Veil; the
     * {@link ForceGroup} record itself is pure and constructs directly. */
    private static final ForceGroup THRUST_GROUP = new ForceGroup(
        Component.translatable("force.csc_electric_push.thrust"), null, 0x2FD8D8, true);

    private float pushRatio = 0f;

    public ElectricPushBlockEntity(BlockPos pos, BlockState state) {
        super(ElectricPush.ELECTRIC_PUSH_BE.get(), pos, state);
    }

    // ── 图宿主（SpeedProxy 同款 L 档）/ graph host (the SpeedProxy L variant) ──

    /** tick 体收敛进 GraphHost.tickBody（K23 骨架）。
     *  The tick body converges into GraphHost.tickBody (the K23 skeleton). */
    public void tick() {
        tickBody(this);
    }

    /** ④ L 档轻量重编译（Monitor/KineticGauge/SpeedProxy 同款）。 / Step ④ light recompile. */
    @Override public boolean lightRecompile() { return true; }

    /** ⑤ 停机检查介于 ③ 后——停机也让链路注册跟随图 generation（P2-18 同款口径）。
     *  Step ⑤ stop check after ③ - link registration tracks graph generation even
     *  while stopped (the P2-18 stance). */
    @Override public GraphHost.StopSlot stopSlot() { return GraphHost.StopSlot.AFTER_GRAPH_CHECK; }

    /** ⑨ 基础档（3 参）。 / Step ⑨ basic (3-arg) form. */
    @Override public GraphHost.EvalVariant evalVariant() { return GraphHost.EvalVariant.BASIC; }

    /** P2-7 服务端准入门：本宿主答自己的名单（与 ElectricPushScreen 的客户端过滤同源）。
     *  P2-7 server admission gate: this host answers its own allowlist (the same source
     *  as ElectricPushScreen's client-side filtering). */
    @Override public NodeAllowance nodeAllowance() { return ElectricPushNodes.ALLOWANCE; }

    // 比例不走缝 8：图里的「推进比例」自定义节点在求值回调内提供方直连写入
    // setPushRatio（自持图 pos 留空=写本宿主，远程图按 pos 寻址），宿主无需读图输出。
    // The ratio does not ride seam 8: the "push ratio" custom node writes setPushRatio
    // provider-direct inside its evaluation callback (blank pos = this host in the
    // self-hosted graph; addressed pos in remote graphs) - the host reads no output.

    // ── 推进执行面（Sable actor）/ thrust execution plane (the Sable actor) ──

    public float getPushRatio() {
        return pushRatio;
    }

    /** 图侧写入口（提供方直连，同 tick 双向）：钳制到 [0,1] 并同步 ACTIVE 亮灭。
     *  The graph-side write (provider-direct, same tick both ways): clamps to [0,1]
     *  and syncs the ACTIVE receipt. */
    public void setPushRatio(float raw) {
        float next = NodeInputs.clampRatio(raw);
        if (next == pushRatio) return;
        pushRatio = next;
        setChanged();
        syncActive();
    }

    private void syncActive() {
        Level level = getLevel();
        if (level == null) return;
        BlockState state = getBlockState();
        boolean active = pushRatio > 0f;
        if (state.hasProperty(ElectricPushBlock.ACTIVE) && state.getValue(ElectricPushBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(ElectricPushBlock.ACTIVE, active), 3);
        }
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        if (pushRatio <= 0f) return;
        // 红石=启停门（兜底控制通道）/ redstone is the enable gate (fallback control channel)
        Level level = getLevel();
        if (level != null && level.hasNeighborSignal(worldPosition)) return;

        Direction facing = getBlockState().getValue(ElectricPushBlock.FACING);
        Vec3 dir = Vec3.atLowerCornerOf(facing.getNormal());
        Vector3d impulse = new Vector3d(dir.x, dir.y, dir.z).mul(MAX_THRUST * pushRatio * timeStep);
        QueuedForceGroup forceGroup = subLevel.getOrCreateQueuedForceGroup(THRUST_GROUP);
        forceGroup.applyAndRecordPointForce(JOMLConversion.atCenterOf(worldPosition), impulse);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("PushRatio", pushRatio);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pushRatio = NodeInputs.clampRatio(tag.getFloat("PushRatio"));
    }
}
