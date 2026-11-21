package io.github.y15173334444.csc_electric_push.block;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 电推原型方块：Sable 子级推进器的机械面。喷口（排气面）朝向 = {@link #FACING}（六向，
 * 放置朝向为点击面），推力沿其反向（火箭式：喷口向后、机体向前）；{@link #ACTIVE} 亮灭
 * 是「图上正给推进比例」的视觉回执。比例本身没有方块 GUI——唯一控制面是 CSC 节点图
 * （单回路口径），红石邻接信号只是启停门。扳手旋转走 {@code IWrenchable} 默认转序；
 * 潜行扳手收回保留 NBT。
 * <p>The prototype actuator block - the mechanical face of the Sable sub-level thruster.
 * The nozzle (exhaust face) runs along {@link #FACING} (six-way; placement faces the
 * clicked face) and thrust runs opposite (rocket-style: nozzle aft, body forward);
 * {@link #ACTIVE} is the visual receipt that a graph is currently commanding the ratio.
 * The ratio has no block GUI - the CSC node graph is its only control surface
 * (single-loop design); a neighbouring redstone signal is just the enable gate.
 * Wrench rotation rides {@code IWrenchable}'s default rotation order; sneak-wrench
 * pickup keeps the NBT.</p>
 */
public class ElectricPushBlock extends BaseEntityBlock implements IWrenchable {

    public static final MapCodec<ElectricPushBlock> CODEC = simpleCodec(ElectricPushBlock::new);
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.values());
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public ElectricPushBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getClickedFace());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(FACING, ACTIVE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricPushBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        if (l.isClientSide()) return null;
        return createTickerHelper(t, io.github.y15173334444.csc_electric_push.ElectricPush.ELECTRIC_PUSH_BE.get(),
            (lv, p, st, be) -> be.tick());
    }

    /** 右键开图（CSC 方块惯例）：仅客户端直接打开图编辑器（无 Menu 网络 round-trip）。
     *  Right-click opens the graph (the CSC block convention): client-side only, the
     *  editor opens directly (no menu round-trip). */
    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level l, BlockPos p, Player pl, BlockHitResult h) {
        if (l.isClientSide()) {
            if (l.getBlockEntity(p) instanceof ElectricPushBlockEntity) openScreen(p);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }

    /** 客户端打开图编辑器（波 2 套件一行口：泛用屏经 client-only 钩子打开；
     *  @OnlyIn 保留原形态，dist cleaner 在专用服务端剥离）。
     *  Client-side open (the wave-2 suite one-liner: the generic screen opens through
     *  the client-only hook; @OnlyIn keeps the original shape, stripped by the dist
     *  cleaner on servers). */
    @net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    private static void openScreen(BlockPos p) {
        io.github.y15173334444.create_schematic_compute.api.HostGraphs.openEditor(p);
    }

    @Override
    protected RenderShape getRenderShape(BlockState s) {
        return RenderShape.MODEL;
    }

    /** 潜行扳手收回：掉落物保留 BE NBT（推进比例随物品走）。镐挖/爆炸掉纯物品是设计意图。
     *  Sneak-wrench pickup: the drops keep the BE NBT (the ratio rides the item).
     *  Pickaxe/explosion drops being plain items is by design. */
    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, be, player, context.getItemInHand());
        if (be != null) {
            for (ItemStack stack : drops) {
                if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                    be.saveToItem(stack, level.registryAccess());
                    break;
                }
            }
        }
        for (ItemStack stack : drops) {
            if (!stack.isEmpty()) {
                if (player != null) player.getInventory().placeItemBackInInventory(stack);
                else Block.popResource(level, pos, stack);
            }
        }
        state.spawnAfterBreak(serverLevel, pos, ItemStack.EMPTY, true);
        level.destroyBlock(pos, false);
        IWrenchable.playRemoveSound(level, pos);
        return InteractionResult.SUCCESS;
    }
}
