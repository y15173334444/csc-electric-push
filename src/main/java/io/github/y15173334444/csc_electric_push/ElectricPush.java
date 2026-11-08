package io.github.y15173334444.csc_electric_push;

import io.github.y15173334444.csc_electric_push.block.ElectricPushBlock;
import io.github.y15173334444.csc_electric_push.block.ElectricPushBlockEntity;
import io.github.y15173334444.csc_electric_push.node.ElectricPushNodes;
import io.github.y15173334444.create_schematic_compute.api.CustomNodes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * CSC: Electric Push——Create: Schematic Compute 的第一个附属模组。一个方块：
 * 「电推」（{@link ElectricPushBlock}），Sable 子级推进器原型，推进比例由 CSC
 * 节点图控制（自定义节点 {@code csc_electric_push:push_ratio} 提供方直连写入）。
 * <p>CSC: Electric Push - the first addon mod for Create: Schematic Compute. One block:
 * the electric push ({@link ElectricPushBlock}), a Sable sub-level thruster prototype
 * whose push ratio is driven by a CSC node graph (the custom node
 * {@code csc_electric_push:push_ratio} writes it provider-direct).</p>
 */
@Mod(ElectricPush.MOD_ID)
public final class ElectricPush {

    public static final String MOD_ID = "csc_electric_push";

    public static final DeferredRegister.Blocks BLOCKS =
        DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredBlock<ElectricPushBlock> ELECTRIC_PUSH =
        BLOCKS.registerBlock("electric_push", ElectricPushBlock::new);

    public static final DeferredItem<BlockItem> ELECTRIC_PUSH_ITEM =
        ITEMS.registerSimpleBlockItem("electric_push", ELECTRIC_PUSH);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricPushBlockEntity>> ELECTRIC_PUSH_BE =
        BLOCK_ENTITIES.register("electric_push", () -> BlockEntityType.Builder
            .of(ElectricPushBlockEntity::new, ELECTRIC_PUSH.get()).build(null));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
        TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + MOD_ID))
            .icon(() -> new ItemStack(ELECTRIC_PUSH_ITEM.get()))
            .displayItems((params, output) -> output.accept(ELECTRIC_PUSH_ITEM.get()))
            .build());

    public ElectricPush(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        TABS.register(modEventBus);

        // ① 注册自定义节点「推进比例」：schema + 求值行为一次注册。
        // ① Register the custom node "push ratio": schema + behavior in one call.
        CustomNodes.register(ElectricPushNodes.pushRatioSchema(), ElectricPushNodes.pushRatioBehavior());

        // ② 名单制登记：注册 ≠ 准入 —— id 显式登记进蓝图机的准入名单。
        // ② Roster enrolment: registering ≠ admitting - enrol the id into the
        // blueprint host's roster explicitly.
        CustomNodes.admitToHost(ElectricPushNodes.NODE_ID, CustomNodes.HOST_BLUEPRINT);
    }
}
