package tk.darrow.tribalpower.device;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
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

public final class DeviceRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TribalPower.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TribalPower.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TribalPower.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, TribalPower.MOD_ID);

    public static final DeferredBlock<WorkshopBlock> TIDE_PUMP = block("tide_pump", MapColor.WATER);
    public static final DeferredBlock<WorkshopBlock> WIND_SNARE = block("wind_snare", MapColor.WOOL);
    public static final DeferredBlock<WorkshopBlock> WARD_DRUM = block("ward_drum", MapColor.COLOR_BROWN);
    public static final DeferredBlock<SealLoomBlock> SEAL_LOOM = BLOCKS.register("seal_loom",
            () -> new SealLoomBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion()));

    public static final DeferredItem<BlockItem> TIDE_PUMP_ITEM = ITEMS.registerSimpleBlockItem("tide_pump", TIDE_PUMP);
    public static final DeferredItem<BlockItem> WIND_SNARE_ITEM = ITEMS.registerSimpleBlockItem("wind_snare", WIND_SNARE);
    public static final DeferredItem<BlockItem> WARD_DRUM_ITEM = ITEMS.registerSimpleBlockItem("ward_drum", WARD_DRUM);
    public static final DeferredItem<BlockItem> SEAL_LOOM_ITEM = ITEMS.registerSimpleBlockItem("seal_loom", SEAL_LOOM);
    public static final DeferredItem<RecipeSealItem> RECIPE_SEAL = ITEMS.register("recipe_seal",
            () -> new RecipeSealItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorkshopBlockEntity>> WORKSHOP =
            BLOCK_ENTITIES.register("workshop", () -> BlockEntityType.Builder.of(WorkshopBlockEntity::new,
                    TIDE_PUMP.get(), WIND_SNARE.get(), WARD_DRUM.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SealLoomBlockEntity>> SEAL_LOOM_TYPE =
            BLOCK_ENTITIES.register("seal_loom", () -> BlockEntityType.Builder.of(SealLoomBlockEntity::new, SEAL_LOOM.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<SealLoomMenu>> SEAL_LOOM_MENU =
            MENUS.register("seal_loom", () -> new MenuType<>(SealLoomMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    private DeviceRegistry() {}

    private static DeferredBlock<WorkshopBlock> block(String id, MapColor color) {
        return BLOCKS.register(id, () -> new WorkshopBlock(BlockBehaviour.Properties.of()
                .mapColor(color).strength(2.5F).sound(SoundType.STONE).noOcclusion()));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
    }

    public static void displayItems(CreativeModeTab.Output out) {
        out.accept(TIDE_PUMP_ITEM.get());
        out.accept(WIND_SNARE_ITEM.get());
        out.accept(WARD_DRUM_ITEM.get());
        out.accept(SEAL_LOOM_ITEM.get());
        out.accept(RECIPE_SEAL.get());
    }
}
