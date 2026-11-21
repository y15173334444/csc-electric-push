package io.github.y15173334444.csc_electric_push.node;

import io.github.y15173334444.csc_electric_push.block.ElectricPushBlockEntity;
import io.github.y15173334444.create_schematic_compute.api.CustomNodeBehavior;
import io.github.y15173334444.create_schematic_compute.api.CustomNodeResult;
import io.github.y15173334444.create_schematic_compute.api.CustomNodeType;
import io.github.y15173334444.create_schematic_compute.api.HostAdmission;
import io.github.y15173334444.create_schematic_compute.api.HostCategory;
import io.github.y15173334444.create_schematic_compute.api.HostGraphType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

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

    /** 宿主键（波 2 套件）：自声明键，命名空间 = 本模组 id —— {@code HostGraphs.bind}
     * 即声明，{@code CustomNodes.admitToHost} 归属两路放行的「自声明键同命名空间」路。
     * The host key (wave-2 suite): self-declared, namespace = this mod's id -
     * {@code HostGraphs.bind} declares it, and it rides the "self-declared key, same
     * namespace" path of {@code CustomNodes.admitToHost} ownership. */
    public static final String HOST_KEY = "csc_electric_push:electric_push";

    /** 电推自持图的声明面（波 2 套件）：准入偏好沿用原名单类目集（api
     * {@link HostCategory} 与内部类目一一对应）；tick 编排偏差 = SpeedProxy 同款
     * L 档 + 停机检查介于图检查后。
     * The self-graph declaration face (wave-2 suite): the admission preference keeps
     * the original category set (api {@link HostCategory} mirrors the internals
     * one-to-one); tick deviations = the SpeedProxy-style Light tier + the stop check
     * after the graph check. */
    public static final HostGraphType HOST_SPEC = HostGraphType.of(
        HOST_KEY,
        HostAdmission.of(HostCategory.VALUES, HostCategory.MATH_BASIC, HostCategory.MATH_ADVANCED,
            HostCategory.TRIG, HostCategory.LOGIC, HostCategory.CONTROL,
            HostCategory.CUSTOM, HostCategory.DEBUG),
        HostGraphType.EvalVariant.BASIC)
        .withLightRecompile()
        .withStopSlot(HostGraphType.StopSlot.AFTER_GRAPH_CHECK);

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
