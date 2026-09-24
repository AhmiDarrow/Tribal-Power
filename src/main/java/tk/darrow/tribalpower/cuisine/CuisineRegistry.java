package tk.darrow.tribalpower.cuisine;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.effect.ModEffects;

/** Tribal cuisine: the March's crops, the Hearth Pot, the nine tribe dishes and the six voice feasts. */
public final class CuisineRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TribalPower.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TribalPower.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TribalPower.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, TribalPower.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, TribalPower.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, TribalPower.MOD_ID);

    public static final Map<MarchCrop, DeferredBlock<MarchCropBlock>> CROPS = new EnumMap<>(MarchCrop.class);
    public static final Map<MarchCrop, DeferredItem<Item>> CROP_ITEMS = new EnumMap<>(MarchCrop.class);
    public static final Map<Dish, DeferredItem<Item>> DISHES = new EnumMap<>(Dish.class);
    public static final Map<Attunement, DeferredBlock<FeastBlock>> FEASTS = new EnumMap<>(Attunement.class);
    public static final Map<Attunement, DeferredItem<BlockItem>> FEAST_ITEMS = new EnumMap<>(Attunement.class);

    public static final DeferredBlock<HearthPotBlock> HEARTH_POT = BLOCKS.register("hearth_pot", () -> new HearthPotBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(2.0F).sound(SoundType.DECORATED_POT).noOcclusion()));
    public static final DeferredItem<BlockItem> HEARTH_POT_ITEM = ITEMS.registerSimpleBlockItem("hearth_pot", HEARTH_POT);

    static {
        for (MarchCrop crop : MarchCrop.values()) {
            DeferredBlock<MarchCropBlock> block = BLOCKS.register(crop.id(), () -> new MarchCropBlock(crop,
                    BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.DESTROY)));
            CROPS.put(crop, block);
            CROP_ITEMS.put(crop, ITEMS.register(crop.id(), () -> new ItemNameBlockItem(block.get(), new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(crop.nutrition).saturationModifier(crop.saturation).build()))));
        }
        for (Dish dish : Dish.values())
            DISHES.put(dish, ITEMS.register(dish.id(), () -> {
                FoodProperties.Builder food = new FoodProperties.Builder().nutrition(dish.nutrition).saturationModifier(dish.saturation)
                        .effect(() -> new MobEffectInstance(ModEffects.boon(dish.tribe), TribalConfig.dishBoonMinutes() * 60 * 20, 0), 1.0F);
                if (dish.bowl) food.usingConvertsTo(net.minecraft.world.item.Items.BOWL);
                return new Item(new Item.Properties().stacksTo(16).food(food.build()));
            }));
        for (Attunement voice : Attunement.values()) {
            DeferredBlock<FeastBlock> block = BLOCKS.register(voice.getSerializedName() + "_feast", () -> new FeastBlock(voice,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.5F).sound(SoundType.WOOL).noOcclusion().pushReaction(PushReaction.DESTROY)));
            FEASTS.put(voice, block);
            FEAST_ITEMS.put(voice, ITEMS.register(voice.getSerializedName() + "_feast", () -> new BlockItem(block.get(), new Item.Properties().stacksTo(1))));
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HearthPotBlockEntity>> HEARTH_POT_ENTITY =
            BLOCK_ENTITIES.register("hearth_pot", () -> BlockEntityType.Builder.of(HearthPotBlockEntity::new, HEARTH_POT.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<HearthPotMenu>> HEARTH_POT_MENU =
            MENUS.register("hearth_pot", () -> new MenuType<>(HearthPotMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<RecipeType<?>, RecipeType<HearthRecipe>> HEARTH_TYPE = RECIPE_TYPES.register("hearth", () -> new RecipeType<>() {});
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<HearthRecipe>> HEARTH_SERIALIZER = SERIALIZERS.register("hearth", HearthRecipe.Serializer::new);

    private CuisineRegistry() {}

    public static Item dish(tk.darrow.tribalpower.tribe.TribeDefinition tribe) {
        return DISHES.get(Dish.of(tribe)).get();
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        RECIPE_TYPES.register(modBus);
        SERIALIZERS.register(modBus);
    }

    public static void displayItems(CreativeModeTab.Output out) {
        out.accept(HEARTH_POT_ITEM.get());
        CROP_ITEMS.values().forEach(item -> out.accept(item.get()));
        DISHES.values().forEach(item -> out.accept(item.get()));
        FEAST_ITEMS.values().forEach(item -> out.accept(item.get()));
    }
}
