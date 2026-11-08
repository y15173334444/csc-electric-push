package io.github.y15173334444.csc_electric_push.node;

import net.minecraft.core.BlockPos;

/**
 * 节点输入卫生（纯函数）：寻址文本解析 + 推进比例钳制。畸形输入一律 fail-closed——
 * 解析失败返回 null、数值越界/NaN 钳到安全值，不抛异常（与公开 API 示例同款口径：
 * 输入卫生是提供方自己的责任）。
 * <p>Node input hygiene (pure functions): addressing-text parsing plus push-ratio
 * clamping. Malformed input is fail-closed - a bad address returns null and an
 * out-of-range / NaN value clamps to a safe number, never throwing (the same stance
 * as the public API example: input hygiene is the provider's own duty).</p>
 */
public final class NodeInputs {

    private NodeInputs() {}

    /** 解析 {@code "x y z"}（分隔符为空白或逗号）；任何畸形输入 → null。
     *  Parses {@code "x y z"} (whitespace or comma separators); any malformed input → null. */
    public static BlockPos parsePos(String text) {
        if (text == null || text.isBlank()) return null;
        String[] parts = text.trim().split("[ ,]+");
        if (parts.length != 3) return null;
        try {
            return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 寻址决议：{@code pos} 为空 → 宿主自己（自持图用法）；填了就解析（远程直连用法）；
     *  畸形输入仍然 fail-closed → null（不落到宿主——垃圾文本不是「留空」）。
     *  Addressing ruling: a blank {@code pos} targets the host itself (self-hosted-graph
     *  usage); a filled one is parsed (remote-direct usage); malformed input still fails
     *  closed to null (garbage text is not "left blank"). */
    public static BlockPos resolveTarget(String posText, BlockPos hostPos) {
        if (posText == null || posText.isBlank()) return hostPos;
        return parsePos(posText);
    }

    /** 推进比例钳制到 [0,1]；NaN/无穷 → 0。Clamps the push ratio to [0,1]; NaN/infinity → 0. */
    public static float clampRatio(float raw) {
        if (!Float.isFinite(raw)) return 0f;
        return Math.min(1f, Math.max(0f, raw));
    }
}
