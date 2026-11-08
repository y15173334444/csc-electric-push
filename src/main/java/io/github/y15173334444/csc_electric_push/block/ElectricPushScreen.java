package io.github.y15173334444.csc_electric_push.block;

import io.github.y15173334444.csc_electric_push.ElectricPush;
import io.github.y15173334444.csc_electric_push.node.ElectricPushNodes;
import io.github.y15173334444.create_schematic_compute.client.editor.AbstractGraphScreen;
import io.github.y15173334444.create_schematic_compute.graph.EvalSnapshot;
import io.github.y15173334444.create_schematic_compute.graph.NodeGraph;
import io.github.y15173334444.create_schematic_compute.network.BlueprintTogglePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;

/**
 * 电推的图编辑器界面（SpeedProxyScreen 同款接线）：右键电推打开，画布里的图驱动
 * 推进比例。运行/停机走蓝图机的切换包（位置寻址，宿主无关）。
 * <p>The electric push's graph editor (the SpeedProxyScreen wiring): opens on
 * right-click, the canvas graph drives the push ratio. Run/stop rides the blueprint
 * computer's toggle packet (position-addressed, host-agnostic).</p>
 */
public class ElectricPushScreen extends AbstractGraphScreen {

    public ElectricPushScreen(BlockPos pos) {
        super(Component.translatable("container." + ElectricPush.MOD_ID + ".electric_push"), pos);
        setNodeAllowance(ElectricPushNodes.ALLOWANCE);
    }

    @Override
    protected ElectricPushBlockEntity getBE() {
        if (minecraft != null && minecraft.level != null) {
            if (minecraft.level.getBlockEntity(blockPos) instanceof ElectricPushBlockEntity be) return be;
        }
        return null;
    }

    @Override
    protected boolean isBlockEntityValid() {
        return minecraft != null && minecraft.level != null
            && minecraft.level.getBlockEntity(blockPos) instanceof ElectricPushBlockEntity;
    }

    @Override public NodeGraph getGraph() { ElectricPushBlockEntity be = getBE(); return be != null ? be.getNodeGraph() : new NodeGraph(); }
    @Override public boolean isRunning() { ElectricPushBlockEntity be = getBE(); return be != null && be.isRunning(); }
    @Override public Map<Integer, Boolean> getFlipflopStates() { ElectricPushBlockEntity be = getBE(); return be != null ? be.getFlipflopStates() : null; }
    @Override public EvalSnapshot getCachedEvalSnapshot() {
        ElectricPushBlockEntity be = getBE();
        return be != null ? be.getCachedEvalSnapshot() : null;
    }

    @Override
    public void toggleRunning(boolean start) {
        ElectricPushBlockEntity be = getBE();
        if (be != null) {
            be.setRunning(start);
            PacketDistributor.sendToServer(new BlueprintTogglePacket(be.getBlockPos(), start));
        }
    }
}
