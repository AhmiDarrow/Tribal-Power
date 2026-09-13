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

/** Pulse Gauge, Pulse Threshold, Song Thread, Song Vine, Verse Call/Answer, and the thirteen song plates. */
public final class LogicRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TribalPower.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TribalPower.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TribalPower.MOD_ID);

    public static final DeferredBlock<PulseGaugeBlock> PULSE_GAUGE = BLOCKS.register("pulse_gauge",
            () -> new PulseGaugeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.5F).sound(SoundType.STONE)));
    public static final DeferredBlock<PulseThresholdBlock> PULSE_THRESHOLD = BLOCKS.register("pulse_threshold",
            () -> new PulseThresholdBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.5F).sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(PulseThresholdBlock.POWERED) ? 6 : 0)));
    public static final DeferredBlock<SongThreadBlock> SONG_THREAD = BLOCKS.register("song_thread",
            () -> new SongThreadBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(0.4F).sound(SoundType.WOOL).noOcclusion().noCollission()));
    public static final DeferredBlock<SongVineBlock> SONG_VINE = BLOCKS.register("song_vine",
            () -> new SongVineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.2F).sound(SoundType.VINE).noOcclusion().noCollission()));
    public static final DeferredBlock<VerseLinkBlock> VERSE_CALL = BLOCKS.register("verse_call",
            () -> new VerseLinkBlock(true, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(1.5F).sound(SoundType.STONE).noOcclusion()));
    public static final DeferredBlock<VerseLinkBlock> VERSE_ANSWER = BLOCKS.register("verse_answer",
            () -> new VerseLinkBlock(false, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(1.5F).sound(SoundType.STONE).noOcclusion()
                    .lightLevel(state -> state.getValue(VerseLinkBlock.POWER) > 0 ? 6 : 0)));

    public static final DeferredBlock<LogicPlateBlock> CHORUS_PLATE = plate("chorus_plate", LogicKind.CHORUS);
    public static final DeferredBlock<LogicPlateBlock> GATHERING_PLATE = plate("gathering_plate", LogicKind.GATHERING);
    public static final DeferredBlock<LogicPlateBlock> DISCORD_PLATE = plate("discord_plate", LogicKind.DISCORD);
    public static final DeferredBlock<LogicPlateBlock> HUSH_PLATE = plate("hush_plate", LogicKind.HUSH);
    public static final DeferredBlock<LogicPlateBlock> INVERSE_PLATE = plate("inverse_plate", LogicKind.INVERSE);
    public static final DeferredBlock<LogicPlateBlock> ECHO_PLATE = plate("echo_plate", LogicKind.ECHO);
    public static final DeferredBlock<LogicPlateBlock> MEMORY_PLATE = plate("memory_plate", LogicKind.MEMORY);
    public static final DeferredBlock<LogicPlateBlock> HEARTBEAT_PLATE = plate("heartbeat_plate", LogicKind.HEARTBEAT);
    public static final DeferredBlock<LogicPlateBlock> DRIFT_PLATE = plate("drift_plate", LogicKind.DRIFT);
    public static final DeferredBlock<LogicPlateBlock> TALLY_PLATE = plate("tally_plate", LogicKind.TALLY);
    public static final DeferredBlock<LogicPlateBlock> STRIKE_PLATE = plate("strike_plate", LogicKind.STRIKE);
    public static final DeferredBlock<LogicPlateBlock> CHANCE_PLATE = plate("chance_plate", LogicKind.CHANCE);
    public static final DeferredBlock<LogicPlateBlock> VERSE_PLATE = plate("verse_plate", LogicKind.VERSE);

    public static final DeferredItem<BlockItem> PULSE_GAUGE_ITEM = ITEMS.registerSimpleBlockItem("pulse_gauge", PULSE_GAUGE);
    public static final DeferredItem<BlockItem> PULSE_THRESHOLD_ITEM = ITEMS.registerSimpleBlockItem("pulse_threshold", PULSE_THRESHOLD);
    public static final DeferredItem<BlockItem> SONG_THREAD_ITEM = ITEMS.registerSimpleBlockItem("song_thread", SONG_THREAD);
    public static final DeferredItem<SongVineItem> SONG_VINE_ITEM = ITEMS.register("song_vine",
            () -> new SongVineItem(SONG_VINE.get(), new net.minecraft.world.item.Item.Properties()));
    public static final DeferredItem<BlockItem> VERSE_CALL_ITEM = ITEMS.registerSimpleBlockItem("verse_call", VERSE_CALL);
    public static final DeferredItem<BlockItem> VERSE_ANSWER_ITEM = ITEMS.registerSimpleBlockItem("verse_answer", VERSE_ANSWER);
    public static final DeferredItem<BlockItem> CHORUS_PLATE_ITEM = ITEMS.registerSimpleBlockItem("chorus_plate", CHORUS_PLATE);
    public static final DeferredItem<BlockItem> GATHERING_PLATE_ITEM = ITEMS.registerSimpleBlockItem("gathering_plate", GATHERING_PLATE);
    public static final DeferredItem<BlockItem> DISCORD_PLATE_ITEM = ITEMS.registerSimpleBlockItem("discord_plate", DISCORD_PLATE);
    public static final DeferredItem<BlockItem> HUSH_PLATE_ITEM = ITEMS.registerSimpleBlockItem("hush_plate", HUSH_PLATE);
    public static final DeferredItem<BlockItem> INVERSE_PLATE_ITEM = ITEMS.registerSimpleBlockItem("inverse_plate", INVERSE_PLATE);
    public static final DeferredItem<BlockItem> ECHO_PLATE_ITEM = ITEMS.registerSimpleBlockItem("echo_plate", ECHO_PLATE);
    public static final DeferredItem<BlockItem> MEMORY_PLATE_ITEM = ITEMS.registerSimpleBlockItem("memory_plate", MEMORY_PLATE);
    public static final DeferredItem<BlockItem> HEARTBEAT_PLATE_ITEM = ITEMS.registerSimpleBlockItem("heartbeat_plate", HEARTBEAT_PLATE);
    public static final DeferredItem<BlockItem> DRIFT_PLATE_ITEM = ITEMS.registerSimpleBlockItem("drift_plate", DRIFT_PLATE);
    public static final DeferredItem<BlockItem> TALLY_PLATE_ITEM = ITEMS.registerSimpleBlockItem("tally_plate", TALLY_PLATE);
    public static final DeferredItem<BlockItem> STRIKE_PLATE_ITEM = ITEMS.registerSimpleBlockItem("strike_plate", STRIKE_PLATE);
    public static final DeferredItem<BlockItem> CHANCE_PLATE_ITEM = ITEMS.registerSimpleBlockItem("chance_plate", CHANCE_PLATE);
    public static final DeferredItem<BlockItem> VERSE_PLATE_ITEM = ITEMS.registerSimpleBlockItem("verse_plate", VERSE_PLATE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PulseGaugeBlockEntity>> PULSE_GAUGE_TYPE =
            BLOCK_ENTITIES.register("pulse_gauge", () -> BlockEntityType.Builder.of(PulseGaugeBlockEntity::new, PULSE_GAUGE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PulseThresholdBlockEntity>> PULSE_THRESHOLD_TYPE =
            BLOCK_ENTITIES.register("pulse_threshold", () -> BlockEntityType.Builder.of(PulseThresholdBlockEntity::new, PULSE_THRESHOLD.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogicPlateBlockEntity>> PLATE_TYPE =
            BLOCK_ENTITIES.register("logic_plate", () -> BlockEntityType.Builder.of(LogicPlateBlockEntity::new,
                    CHORUS_PLATE.get(), GATHERING_PLATE.get(), DISCORD_PLATE.get(), HUSH_PLATE.get(), INVERSE_PLATE.get(),
                    ECHO_PLATE.get(), MEMORY_PLATE.get(), HEARTBEAT_PLATE.get(), DRIFT_PLATE.get(), TALLY_PLATE.get(),
                    STRIKE_PLATE.get(), CHANCE_PLATE.get(), VERSE_PLATE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SongVineBlockEntity>> SONG_VINE_TYPE =
            BLOCK_ENTITIES.register("song_vine", () -> BlockEntityType.Builder.of(SongVineBlockEntity::new, SONG_VINE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VerseLinkBlockEntity>> VERSE_LINK_TYPE =
            BLOCK_ENTITIES.register("verse_link", () -> BlockEntityType.Builder.of(VerseLinkBlockEntity::new,
                    VERSE_CALL.get(), VERSE_ANSWER.get()).build(null));

    private LogicRegistry() {}

    private static DeferredBlock<LogicPlateBlock> plate(String id, LogicKind kind) {
        return BLOCKS.register(id, () -> new LogicPlateBlock(kind, BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE).strength(1.5F).sound(SoundType.STONE).noOcclusion()));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

    public static void displayItems(CreativeModeTab.Output out) {
        out.accept(PULSE_GAUGE_ITEM.get());
        out.accept(PULSE_THRESHOLD_ITEM.get());
        out.accept(SONG_THREAD_ITEM.get());
        out.accept(SONG_VINE_ITEM.get());
        out.accept(VERSE_CALL_ITEM.get());
        out.accept(VERSE_ANSWER_ITEM.get());
        out.accept(CHORUS_PLATE_ITEM.get());
        out.accept(GATHERING_PLATE_ITEM.get());
        out.accept(DISCORD_PLATE_ITEM.get());
        out.accept(HUSH_PLATE_ITEM.get());
        out.accept(INVERSE_PLATE_ITEM.get());
        out.accept(ECHO_PLATE_ITEM.get());
        out.accept(MEMORY_PLATE_ITEM.get());
        out.accept(HEARTBEAT_PLATE_ITEM.get());
        out.accept(DRIFT_PLATE_ITEM.get());
        out.accept(TALLY_PLATE_ITEM.get());
        out.accept(STRIKE_PLATE_ITEM.get());
        out.accept(CHANCE_PLATE_ITEM.get());
        out.accept(VERSE_PLATE_ITEM.get());
    }
}
