package tk.darrow.tribalpower.tribe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

/** Registrations for the Nine Tribes (design 3.0 §2). Call {@link #register} from TribalPower's constructor. */
public final class TribeRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("tribalpower");
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("tribalpower");
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "tribalpower");
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, "tribalpower");
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_SERIALIZER, "tribalpower");
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>, net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<KinshipTotemRecipe>> KINSHIP_RECIPE =
            RECIPES.register("kinship_totem", () -> new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(KinshipTotemRecipe::new));

    public static final DeferredBlock<TribeHearthBlock> TRIBE_HEARTH = BLOCKS.register("tribe_hearth", () -> new TribeHearthBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5F).sound(SoundType.STONE).noOcclusion().lightLevel(s -> 9)));
    public static final DeferredBlock<TribeBannerBlock> TRIBE_BANNER = BLOCKS.register("tribe_banner", () -> new TribeBannerBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(1.0F).sound(SoundType.WOOL).noOcclusion().noCollission()));
    public static final DeferredBlock<KinshipTotemBlock> KINSHIP_TOTEM = BLOCKS.register("kinship_totem", () -> new KinshipTotemBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion()));

    public static final DeferredItem<TribeHearthItem> TRIBE_HEARTH_ITEM = ITEMS.register("tribe_hearth", () -> new TribeHearthItem(TRIBE_HEARTH.get(), new Item.Properties()));
    public static final DeferredItem<TribeHearthItem> TRIBE_BANNER_ITEM = ITEMS.register("tribe_banner", () -> new TribeHearthItem(TRIBE_BANNER.get(), new Item.Properties()));
    public static final DeferredItem<TribeHearthItem> KINSHIP_TOTEM_ITEM = ITEMS.register("kinship_totem", () -> new TribeHearthItem(KINSHIP_TOTEM.get(), new Item.Properties()));
    public static final DeferredItem<TribeMarkItem> TRIBE_MARK = ITEMS.register("tribe_mark", () -> new TribeMarkItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TribeHearthBlockEntity>> HEARTH_TYPE = BLOCK_ENTITIES.register("tribe_hearth",
            () -> BlockEntityType.Builder.of(TribeHearthBlockEntity::new, TRIBE_HEARTH.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KinshipTotemBlockEntity>> KINSHIP_TYPE = BLOCK_ENTITIES.register("kinship_totem",
            () -> BlockEntityType.Builder.of(KinshipTotemBlockEntity::new, KINSHIP_TOTEM.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<TribalKinEntity>> TRIBAL_KIN = ENTITIES.register("tribal_kin",
            () -> EntityType.Builder.of(TribalKinEntity::new, MobCategory.MISC).sized(0.6F, 1.9F).clientTrackingRange(10).build("tribalpower:tribal_kin"));

    /** One spawn egg per role; the egg places a Kin of that role with tribe 0 unless the egg carries {@code Tribe} custom data. */
    public static final Map<KinRole, DeferredItem<DeferredSpawnEggItem>> EGGS = new EnumMap<>(KinRole.class);
    static {
        for (KinRole role : KinRole.values()) {
            int secondary = switch (role) { case ELDER -> 0xe8d9a8; case DRUMMER -> 0xe0a32d; case HUNTER -> 0xb8512f; case WEAVER -> 0x62d1c9; };
            EGGS.put(role, ITEMS.register("tribal_kin_" + role.id() + "_spawn_egg",
                    () -> new KinSpawnEggItem(role, 0x3a2a22, secondary, new Item.Properties())));
        }
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ENTITIES.register(modBus);
        RECIPES.register(modBus);
        modBus.addListener(TribeRegistry::attributes);
        // no natural spawning: Kin come from structures and eggs
    }

    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(TRIBAL_KIN.get(), TribalKinEntity.createAttributes().build());
    }

    /** Creative tab: nine hearths, nine banners, nine kinship totems, nine marks, four eggs. */
    public static void displayItems(CreativeModeTab.Output out) {
        for (TribeDefinition tribe : TribeDefinition.values()) out.accept(tribe.stamped(TRIBE_HEARTH_ITEM.get()));
        for (TribeDefinition tribe : TribeDefinition.values()) out.accept(tribe.stamped(TRIBE_BANNER_ITEM.get()));
        for (TribeDefinition tribe : TribeDefinition.values()) out.accept(tribe.stamped(KINSHIP_TOTEM_ITEM.get()));
        for (TribeDefinition tribe : TribeDefinition.values()) out.accept(tribe.stamped(TRIBE_MARK.get()));
        EGGS.values().forEach(egg -> out.accept(egg.get()));
    }

    private TribeRegistry() {}
}
