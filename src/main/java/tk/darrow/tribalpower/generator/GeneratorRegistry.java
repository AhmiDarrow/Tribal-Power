package tk.darrow.tribalpower.generator;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;

/**
 * One generator per voice (design 3.1 section 9). Call {@link #register} from the mod constructor.
 *
 * <p>Each is the craft of the tribe that keeps that voice, which is what finally gives standing a
 * mechanical payoff. The Ley Collector is untouched and keeps its place as the neutral, voice-less
 * starter passive; the Drumheart keeps Earth and only gets its redstone tempo retuned.
 */
public final class GeneratorRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TribalPower.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TribalPower.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TribalPower.MOD_ID);

    private static DeferredBlock<GeneratorBlock> generator(String id, MapColor colour, SoundType sound, float strength) {
        return BLOCKS.register(id, () -> new GeneratorBlock(BlockBehaviour.Properties.of()
                .mapColor(colour)
                .strength(strength)
                .sound(sound)
                .lightLevel(state -> state.getValue(GeneratorBlock.LIT) ? 10 : 0)));
    }

    public static final DeferredBlock<GeneratorBlock> EMBER_HORN = generator("ember_horn", MapColor.COLOR_ORANGE, SoundType.COPPER, 3.0F);
    public static final DeferredBlock<GeneratorBlock> WIND_HARP = generator("wind_harp", MapColor.WOOL, SoundType.WOOD, 2.0F);
    public static final DeferredBlock<GeneratorBlock> WAVE_DRUM = generator("wave_drum", MapColor.WATER, SoundType.WOOD, 2.5F);
    public static final DeferredBlock<GeneratorBlock> WAKE_BELL = generator("wake_bell", MapColor.COLOR_PURPLE, SoundType.COPPER, 3.0F);
    public static final DeferredBlock<GeneratorBlock> LOOM_ANCHOR = generator("loom_anchor", MapColor.DIAMOND, SoundType.AMETHYST, 3.5F);

    public static final DeferredItem<BlockItem> EMBER_HORN_ITEM = ITEMS.registerSimpleBlockItem("ember_horn", EMBER_HORN);
    public static final DeferredItem<BlockItem> WIND_HARP_ITEM = ITEMS.registerSimpleBlockItem("wind_harp", WIND_HARP);
    public static final DeferredItem<BlockItem> WAVE_DRUM_ITEM = ITEMS.registerSimpleBlockItem("wave_drum", WAVE_DRUM);
    public static final DeferredItem<BlockItem> WAKE_BELL_ITEM = ITEMS.registerSimpleBlockItem("wake_bell", WAKE_BELL);
    public static final DeferredItem<BlockItem> LOOM_ANCHOR_ITEM = ITEMS.registerSimpleBlockItem("loom_anchor", LOOM_ANCHOR);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EmberHornBlockEntity>> EMBER_HORN_TYPE =
            BLOCK_ENTITIES.register("ember_horn", () -> BlockEntityType.Builder.of(EmberHornBlockEntity::new, EMBER_HORN.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WindHarpBlockEntity>> WIND_HARP_TYPE =
            BLOCK_ENTITIES.register("wind_harp", () -> BlockEntityType.Builder.of(WindHarpBlockEntity::new, WIND_HARP.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WaveDrumBlockEntity>> WAVE_DRUM_TYPE =
            BLOCK_ENTITIES.register("wave_drum", () -> BlockEntityType.Builder.of(WaveDrumBlockEntity::new, WAVE_DRUM.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WakeBellBlockEntity>> WAKE_BELL_TYPE =
            BLOCK_ENTITIES.register("wake_bell", () -> BlockEntityType.Builder.of(WakeBellBlockEntity::new, WAKE_BELL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LoomAnchorBlockEntity>> LOOM_ANCHOR_TYPE =
            BLOCK_ENTITIES.register("loom_anchor", () -> BlockEntityType.Builder.of(LoomAnchorBlockEntity::new, LOOM_ANCHOR.get()).build(null));

    private GeneratorRegistry() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        // One handler for every death in the world; the bells are a position registry, never a scan.
        NeoForge.EVENT_BUS.addListener(WakeBells::onDeath);
        modBus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) -> {
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                    WAVE_DRUM_TYPE.get(), (be, side) -> be.tank);
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    EMBER_HORN_TYPE.get(), (be, side) -> new tk.darrow.tribalpower.lattice.RedstoneItemHandler(be,
                            side == null ? new net.neoforged.neoforge.items.wrapper.InvWrapper(be)
                                    : new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(be, side)));
        });
    }

    public static void displayItems(CreativeModeTab.Output out) {
        out.accept(EMBER_HORN_ITEM.get());
        out.accept(WIND_HARP_ITEM.get());
        out.accept(WAVE_DRUM_ITEM.get());
        out.accept(WAKE_BELL_ITEM.get());
        out.accept(LOOM_ANCHOR_ITEM.get());
    }
}
