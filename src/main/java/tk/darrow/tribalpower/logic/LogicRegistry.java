package tk.darrow.tribalpower.logic;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;

/** Pulse logic registry: Pulse Gauge and Pulse Threshold. Call {@link #register(IEventBus)} from the mod constructor. */
public final class LogicRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TribalPower.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TribalPower.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TribalPower.MOD_ID);

    public static final DeferredBlock<PulseGaugeBlock> PULSE_GAUGE = BLOCKS.register("pulse_gauge",
            () -> new PulseGaugeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.5F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<PulseThresholdBlock> PULSE_THRESHOLD = BLOCKS.register("pulse_threshold",
            () -> new PulseThresholdBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.5F).sound(SoundType.STONE).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(PulseThresholdBlock.POWERED) ? 6 : 0)));

    public static final DeferredItem<BlockItem> PULSE_GAUGE_ITEM = ITEMS.registerSimpleBlockItem("pulse_gauge", PULSE_GAUGE);
    public static final DeferredItem<BlockItem> PULSE_THRESHOLD_ITEM = ITEMS.registerSimpleBlockItem("pulse_threshold", PULSE_THRESHOLD);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PulseGaugeBlockEntity>> PULSE_GAUGE_TYPE =
            BLOCK_ENTITIES.register("pulse_gauge", () -> BlockEntityType.Builder.of(PulseGaugeBlockEntity::new, PULSE_GAUGE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PulseThresholdBlockEntity>> PULSE_THRESHOLD_TYPE =
            BLOCK_ENTITIES.register("pulse_threshold", () -> BlockEntityType.Builder.of(PulseThresholdBlockEntity::new, PULSE_THRESHOLD.get()).build(null));

    private LogicRegistry() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

    public static void displayItems(CreativeModeTab.Output out) {
        out.accept(PULSE_GAUGE_ITEM.get());
        out.accept(PULSE_THRESHOLD_ITEM.get());
    }
}
