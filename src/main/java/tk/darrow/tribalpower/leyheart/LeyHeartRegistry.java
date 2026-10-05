package tk.darrow.tribalpower.leyheart;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;

/** The Ley Heart: its block, item, block entity, menu and intakes. Call {@link #register} from the mod constructor. */
public final class LeyHeartRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TribalPower.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TribalPower.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TribalPower.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, TribalPower.MOD_ID);

    public static final DeferredBlock<LeyHeartBlock> LEY_HEART = BLOCKS.register("ley_heart", () -> new LeyHeartBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(3.5F)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> state.getValue(LeyHeartBlock.LIT) ? 11 : 4)
                    .noOcclusion()));
    public static final DeferredItem<BlockItem> LEY_HEART_ITEM = ITEMS.registerSimpleBlockItem("ley_heart", LEY_HEART);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LeyHeartBlockEntity>> LEY_HEART_TYPE =
            BLOCK_ENTITIES.register("ley_heart", () -> BlockEntityType.Builder.of(LeyHeartBlockEntity::new, LEY_HEART.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<LeyHeartMenu>> LEY_HEART_MENU =
            MENUS.register("ley_heart", () -> new MenuType<>(LeyHeartMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private LeyHeartRegistry() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        modBus.addListener((RegisterCapabilitiesEvent event) -> {
            // Hoppers, relays and pipes feed the crystal and reagent slots; nothing is taken back out.
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LEY_HEART_TYPE.get(), (be, side) ->
                    new tk.darrow.tribalpower.lattice.RedstoneItemHandler(be,
                            side == null ? new net.neoforged.neoforge.items.wrapper.InvWrapper(be)
                                    : new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(be, side)));
            // Buckets, pipes and fluid relay plates fill the water; the tank only feeds the heart.
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, LEY_HEART_TYPE.get(), (be, side) ->
                    tk.darrow.tribalpower.lattice.SidedFluidHandler.wrapInput(be, side, be.tank));
        });
    }
}
