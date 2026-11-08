package io.github.y15173334444.csc_electric_push.block;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.y15173334444.csc_electric_push.ElectricPush;
import io.github.y15173334444.csc_electric_push.node.NodeInputs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

/**
 * 电推 BE：持一个「推进比例」（0..1），并把它变成 Sable 子级上的定向推力。
 * 执行面走 Sable 的 {@link BlockEntitySubLevelActor}（放进子级即被自动收纳，无需注册）：
 * 每个物理子步在 {@code PROPULSION} 力组排队一个点力，矢量为
 * {@code 朝向 × MAX_THRUST × 比例 × timeStep}——乘 timeStep 使每游戏刻总冲量与
 * 子步数无关（与 Sable 自带螺旋桨同款节拍），作用点为方块中心（绘图本地坐标）。
 * <p>The actuator BE: holds one push ratio (0..1) and turns it into directed thrust on a
 * Sable sub-level. Execution rides Sable's {@link BlockEntitySubLevelActor} (collected
 * automatically once the block lives in a sub-level): each physics substep queues a point
 * force in the {@code PROPULSION} group with the vector
 * {@code facing × MAX_THRUST × ratio × timeStep} - multiplying by timeStep keeps the
 * per-game-tick impulse independent of the substep count (the cadence of Sable's own
 * propeller), applied at the block's centre (plot-local coordinates).</p>
 */
public class ElectricPushBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {

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
