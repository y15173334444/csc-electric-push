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
import io.github.y15173334444.create_schematic_compute.api.CustomNodes;
import io.github.y15173334444.create_schematic_compute.api.HostGraphBehavior;
import io.github.y15173334444.create_schematic_compute.api.HostGraphHandle;
import io.github.y15173334444.create_schematic_compute.api.HostGraphs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
 * 图宿主走公开宿主套件（波 2，已迁移）：构造期 {@code HostGraphs.bind} 拿引擎柄，
 * tick/生命周期/NBT 各一行转发 —— 图域对象不经本类之手；右键开图走
 * {@code HostGraphs.openEditor} 泛用屏。
 * <p>The electric push BE: a CSC graph host and a Sable sub-level thruster in one.
 * Control plane: its own node graph (right-click to open, the SpeedProxy-style L-variant
 * host skeleton), driven by the "push ratio" custom node ({@code pos} blank = write the
 * host itself); the same node in a blueprint graph still writes remotely by {@code pos}.
 * Execution plane: {@link BlockEntitySubLevelActor} queues a point force per physics
 * substep - the actuator's own "electric push thrust" force group, applied at the block
 * centre (plot-local), {@code facing.opposite × MAX_THRUST × ratio × timeStep} (FACING
 * is the nozzle/exhaust direction, thrust runs opposite - rocket-style; frame-rate
 * independent, the cadence of Sable's own propeller). The host rides the public host
 * suite (wave 2, migrated): the engine handle comes from {@code HostGraphs.bind} at
 * construction, tick/lifecycle/NBT forward one line each, and the editor opens through
 * {@code HostGraphs.openEditor}'s generic screen.</p>
 */
public class ElectricPushBlockEntity extends BlockEntity
    implements BlockEntitySubLevelActor {

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

    /** 宿主套件引擎柄：bind 即键声明（幂等，键属宿主类型）；实例专属状态住 CSC 侧
     * 适配器登记表（按 BE 实例分键）。tick/生命周期/NBT 各一行转发进柄。
     * The suite engine handle: bind declares the key (idempotent - keys belong to the
     * host type); per-instance state lives in CSC's adapter registry keyed by BE
     * instance. tick/lifecycle/NBT forward one line each into the handle. */
    private final HostGraphHandle graph;

    @SuppressWarnings("unchecked")
    public ElectricPushBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType<? extends BlockEntity>) ElectricPush.ELECTRIC_PUSH_BE.get(), pos, state);
        // bind 即键声明（幂等）；缝面空实现——偏差全部化进 HOST_SPEC 声明（L 档 +
        // 停机位），本宿主不占任何缝。
        // bind means declare (idempotent); an empty seam face - the deviations all ride
        // HOST_SPEC's declarations (the Light tier + the stop slot), this host uses no
        // seam.
        this.graph = HostGraphs.bind(this, ElectricPushNodes.HOST_SPEC, new HostGraphBehavior() {});
        // 自持图名单：自声明键 + 同命名空间节点（归属两路放行的第②路）。
        // The self-graph roster: the self-declared key + same-namespace node (path 2 of
        // the two-path ownership).
        CustomNodes.admitToHost(ElectricPushNodes.NODE_ID, ElectricPushNodes.HOST_KEY);
    }

    // ── 图宿主（套件柄转发，SpeedProxy 同款 L 档）/ graph host (suite-handle forwards) ──

    /** tick 体经柄进引擎（K23 骨架）。 The tick body rides the handle into the engine
     *  (the K23 skeleton). */
    public void tick() {
        graph.tick();
    }

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

        // FACING = 喷口（排气面，正面贴图环形喷口）朝向——推力沿反向（火箭式：喷口向后、
        // 机体向前）。实机判定 2026-11-21：沿 FACING 直推把机体往喷口面推，方向反了。
        // FACING is the nozzle (exhaust) direction - the front texture's ring port -
        // so thrust runs opposite (rocket-style: nozzle aft, body forward). Live verdict
        // 2026-11-21: pushing along FACING shoved the craft toward the nozzle face.
        Direction facing = getBlockState().getValue(ElectricPushBlock.FACING);
        Vec3 dir = Vec3.atLowerCornerOf(facing.getOpposite().getNormal());
        Vector3d impulse = new Vector3d(dir.x, dir.y, dir.z).mul(MAX_THRUST * pushRatio * timeStep);
        QueuedForceGroup forceGroup = subLevel.getOrCreateQueuedForceGroup(THRUST_GROUP);
        forceGroup.applyAndRecordPointForce(JOMLConversion.atCenterOf(worldPosition), impulse);
    }

    // ── 图 NBT/同步管线（柄转发；SyncedGraphBlockEntity 迁出后本类自管原版链）──
    //     Graph NBT/sync pipeline (handle forwards; after leaving SyncedGraphBlockEntity
    //     this class owns its vanilla chain).

    @Override
    public void onLoad() {
        super.onLoad();
        graph.onHostLoad();
    }

    @Override
    public void onChunkUnloaded() {
        graph.onHostChunkUnloaded();
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        graph.onHostRemoved();
        super.setRemoved();
    }

    /** getUpdateTag = 全图随包（SyncedGraphBlockEntity 口径；柄内门控按 clientPacket
     *  收口，本路径恒 false）。/ The update tag ships the full graph (the
     *  SyncedGraphBlockEntity stance; the handle's gate keys on clientPacket, always
     *  false on this path). */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        graph.save(tag, registries, false);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        graph.load(tag, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        graph.save(tag, registries, false);
        tag.putFloat("PushRatio", pushRatio);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        graph.load(tag, registries);
        pushRatio = NodeInputs.clampRatio(tag.getFloat("PushRatio"));
    }
}
