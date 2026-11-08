package io.github.y15173334444.csc_electric_push.node;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 节点输入卫生的纯函数面：寻址解析 fail-closed、推进比例钳制。
 * The pure-function face of node input hygiene: fail-closed address parsing,
 * push-ratio clamping.
 */
class NodeInputsTest {

    @Test
    void parsePos_accepts_space_and_comma_separators() {
        assertEquals(new BlockPos(1, 2, 3), NodeInputs.parsePos("1 2 3"));
        assertEquals(new BlockPos(-4, 5, 6), NodeInputs.parsePos("-4,5,6"));
        assertEquals(new BlockPos(7, -8, 9), NodeInputs.parsePos(" 7 , -8 , 9 "));
    }

    @Test
    void parsePos_fails_closed_on_malformed_input() {
        assertNull(NodeInputs.parsePos(null));
        assertNull(NodeInputs.parsePos(""));
        assertNull(NodeInputs.parsePos("   "));
        assertNull(NodeInputs.parsePos("1 2"));
        assertNull(NodeInputs.parsePos("1 2 3 4"));
        assertNull(NodeInputs.parsePos("1.5 2 3"));
        assertNull(NodeInputs.parsePos("a b c"));
        assertNull(NodeInputs.parsePos("1,2,x"));
    }

    @Test
    void clampRatio_clamps_range_and_sanitizes_non_finite() {
        assertEquals(0.5f, NodeInputs.clampRatio(0.5f));
        assertEquals(1f, NodeInputs.clampRatio(1f));
        assertEquals(0f, NodeInputs.clampRatio(0f));
        assertEquals(0f, NodeInputs.clampRatio(-0.3f));
        assertEquals(1f, NodeInputs.clampRatio(2f));
        assertEquals(0f, NodeInputs.clampRatio(Float.NaN));
        assertEquals(0f, NodeInputs.clampRatio(Float.POSITIVE_INFINITY));
        assertEquals(0f, NodeInputs.clampRatio(Float.NEGATIVE_INFINITY));
    }
}
