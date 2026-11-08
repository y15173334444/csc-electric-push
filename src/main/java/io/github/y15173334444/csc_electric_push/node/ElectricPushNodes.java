package io.github.y15173334444.csc_electric_push.node;

import io.github.y15173334444.csc_electric_push.block.ElectricPushBlockEntity;
import io.github.y15173334444.create_schematic_compute.api.CustomNodeBehavior;
import io.github.y15173334444.create_schematic_compute.api.CustomNodeResult;
import io.github.y15173334444.create_schematic_compute.api.CustomNodeType;
import io.github.y15173334444.create_schematic_compute.graph.BlockNodeAllowances;
import io.github.y15173334444.create_schematic_compute.graph.NodeAllowance;
import io.github.y15173334444.create_schematic_compute.graph.NodeCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.EnumSet;
import java.util.List;

/**
 * 自定义节点「电推·推进比例」（{@code csc_electric_push:push_ratio}）：CSC 节点图对
 * 电推的唯一控制面。schema：1 入 {@code ratio}、1 出 {@code applied}（钳制后回读，
 * 供图内闭环）、TEXT 参数 {@code pos}。「参数即寻址」约定：{@code pos} 填目标电推的
 * 坐标 {@code "x y z"}（宿主维度内绝对坐标），求值回调自行解析定位——同 tick 双向
 * 写入（公开 API 契约 D6 提供方直连）；寻址失败（未加载/不是电推）→ 输出 0，不抛异常。
 * <p>The custom node "electric push / push ratio" ({@code csc_electric_push:push_ratio}):
 * the CSC node graph's only control surface over the actuator. Schema: one {@code ratio}
 * input, one {@code applied} output (the clamped read-back, closing the loop inside the
 * graph), and a TEXT {@code pos} param. Per the "params are addressing" convention
 * {@code pos} carries the target actuator's {@code "x y z"} absolute coordinates in the
 * host's dimension and the callback resolves them itself - a same-tick provider-direct
 * write (public API contract D6); a failed address outputs 0 instead of throwing.</p>
 */
public final class ElectricPushNodes {

    /** 节点 id：持久化在图 NBT 里（cid），改名=断档，勿动。
     *  The node id: persisted in the graph NBT (cid) - renaming breaks saves, keep it. */
    public static final String NODE_ID = "csc_electric_push:push_ratio";

    /** 电推自持图的节点名单。过渡口径（可否决）：名单键借用蓝图机的
     * {@code hostKey}——键在 CSC 里只是名单命名空间（客户端菜单过滤 + 服务端准入门
     * 两处消费，不进存档 NBT），而第三方宿主键今天还登记不进名单（登记口只放行蓝图机，
     * 波 2 宿主套件才让宿主自声明可登记键）。借用后本节点（已登记进蓝图机名单）在
     * 电推图里同名单可见；波 2 声明口落地后换回正式键即可。
     * The electric push's own-graph node roster. Transitional stance (vetoable): the
     * roster key borrows the blueprint computer's {@code hostKey} - the key is only a
     * roster namespace in CSC (client menu filter + server admission gate; never saved
     * to NBT), and third-party host keys cannot yet enrol into any roster (the enrolment
     * point only admits the blueprint key; the wave-2 host suite lets hosts declare
     * enrolable keys). The borrowed key makes this node (already on the blueprint
     * roster) visible in the electric push's graph from the same roster; swap to a
     * proper key once the wave-2 declaration point lands. */
    public static final NodeAllowance ALLOWANCE = NodeAllowance.of(
        BlockNodeAllowances.BLUEPRINT.hostKey(),
        EnumSet.of(NodeCategory.VALUES, NodeCategory.MATH_BASIC, NodeCategory.MATH_ADVANCED,
            NodeCategory.TRIG, NodeCategory.LOGIC, NodeCategory.CONTROL,
            NodeCategory.CUSTOM, NodeCategory.DEBUG));

    private ElectricPushNodes() {}

    public static CustomNodeType pushRatioSchema() {
        return new CustomNodeType(NODE_ID, "电推·推进比例 / Push Ratio",
            List.of("ratio"), List.of("applied"),
            List.of(new CustomNodeType.CustomParam("pos", CustomNodeType.CustomParam.ParamKind.TEXT)));
    }

    public static CustomNodeBehavior pushRatioBehavior() {
        return ctx -> {
            ServerLevel level = ctx.level();
            BlockPos targetPos = NodeInputs.resolveTarget(ctx.textParam("pos"), ctx.hostPos());
            float applied = 0f;
            if (level != null && targetPos != null
                && level.getBlockEntity(targetPos) instanceof ElectricPushBlockEntity be) {
                float raw = ctx.inputs().length > 0 ? ctx.inputs()[0] : 0f;
                be.setPushRatio(raw);
                applied = be.getPushRatio();
            }
            return CustomNodeResult.done(applied);
        };
    }
}
