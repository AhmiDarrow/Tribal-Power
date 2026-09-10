package tk.darrow.tribalpower.gate;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;

/**
 * Registrations for the gates (design 3.1 section 8). Call {@link #register} from the mod constructor.
 *
 * <p>The Gate Drum is untouched: it stays the portable, personal way into The March. These are the built
 * ones, and item entities pass through them, which is what makes a gate pair bulk logistics where an
 * astral relay is a trickle.
 */
public final class GateRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TribalPower.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TribalPower.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TribalPower.MOD_ID);

    public static final DeferredBlock<Block> GATE_FRAME = BLOCKS.registerSimpleBlock("gate_frame",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_CYAN)
                    .strength(4.0F, 12.0F)
                    .sound(SoundType.COPPER)
                    .requiresCorrectToolForDrops());

    public static final DeferredBlock<GateKeystoneBlock> GATE_KEYSTONE = BLOCKS.register("gate_keystone",
            () -> new GateKeystoneBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_CYAN)
                    .strength(5.0F, 20.0F)
                    .sound(SoundType.COPPER)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 6)));

    public static final DeferredBlock<GatePortalBlock> GATE_PORTAL = BLOCKS.register("gate_portal",
            () -> new GatePortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .noCollission()
                    .noOcclusion()
                    .noLootTable()
                    .strength(-1.0F, 3600000.0F)
                    .lightLevel(state -> 11)
                    .pushReaction(PushReaction.BLOCK)
                    .sound(SoundType.AMETHYST)));

    public static final DeferredItem<net.minecraft.world.item.BlockItem> GATE_FRAME_ITEM =
            ITEMS.registerSimpleBlockItem("gate_frame", GATE_FRAME);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> GATE_KEYSTONE_ITEM =
            ITEMS.registerSimpleBlockItem("gate_keystone", GATE_KEYSTONE);

    public static final DeferredItem<GateSigilItem> GATE_SIGIL =
            ITEMS.register("gate_sigil", () -> new GateSigilItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GateKeystoneBlockEntity>> KEYSTONE_TYPE =
            BLOCK_ENTITIES.register("gate_keystone", () -> BlockEntityType.Builder.of(
                    GateKeystoneBlockEntity::new, GATE_KEYSTONE.get()).build(null));

    private GateRegistry() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        NeoForge.EVENT_BUS.addListener(GateCommands::register);
    }

    public static void displayItems(CreativeModeTab.Output out) {
        out.accept(GATE_FRAME_ITEM.get());
        out.accept(GATE_KEYSTONE_ITEM.get());
        out.accept(GATE_SIGIL.get());
    }
}
