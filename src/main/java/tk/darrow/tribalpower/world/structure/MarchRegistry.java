package tk.darrow.tribalpower.world.structure;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.boss.TheUnsungEntity;

/** The March structures' blocks (Silent Drum, Lore Tablet) and The Unsung (design §3). */
public final class MarchRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("tribalpower");
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("tribalpower");
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "tribalpower");
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, "tribalpower");

    public static final DeferredBlock<SilentDrumBlock> SILENT_DRUM = BLOCKS.register("silent_drum", () -> new SilentDrumBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(4F, 1200F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<LoreTabletBlock> LORE_TABLET = BLOCKS.register("lore_tablet", () -> new LoreTabletBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.5F, 6F).sound(SoundType.DEEPSLATE_TILES).noOcclusion()));

    public static final DeferredItem<net.minecraft.world.item.BlockItem> SILENT_DRUM_ITEM = ITEMS.registerSimpleBlockItem("silent_drum", SILENT_DRUM);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> LORE_TABLET_ITEM = ITEMS.registerSimpleBlockItem("lore_tablet", LORE_TABLET);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SilentDrumBlockEntity>> SILENT_DRUM_BE =
            BLOCK_ENTITIES.register("silent_drum", () -> BlockEntityType.Builder.of(SilentDrumBlockEntity::new, SILENT_DRUM.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LoreTabletBlockEntity>> LORE_TABLET_BE =
            BLOCK_ENTITIES.register("lore_tablet", () -> BlockEntityType.Builder.of(LoreTabletBlockEntity::new, LORE_TABLET.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<TheUnsungEntity>> THE_UNSUNG = ENTITIES.register("the_unsung", () ->
            EntityType.Builder.of(TheUnsungEntity::new, MobCategory.MONSTER).sized(3.2F, 4.2F).fireImmune().clientTrackingRange(12).updateInterval(2).build("tribalpower:the_unsung"));
    public static final DeferredItem<Item> THE_UNSUNG_EGG = ITEMS.register("the_unsung_spawn_egg", () ->
            new net.neoforged.neoforge.common.DeferredSpawnEggItem(THE_UNSUNG, 0x3a2430, 0x9b6cff, new Item.Properties()));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ENTITIES.register(modBus);
        modBus.addListener(MarchRegistry::attributes);
        modBus.addListener(MarchRegistry::placements);
    }

    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(THE_UNSUNG.get(), TheUnsungEntity.createAttributes().build());
    }

    public static void placements(RegisterSpawnPlacementsEvent event) {
        // Never spawns naturally: only the Silent Drum wakes it. Placement rule rejects every natural spawn.
        event.register(THE_UNSUNG.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> reason != net.minecraft.world.entity.MobSpawnType.NATURAL && reason != net.minecraft.world.entity.MobSpawnType.CHUNK_GENERATION,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    public static void displayItems(CreativeModeTab.Output out) {
        out.accept(SILENT_DRUM_ITEM.get());
        out.accept(LORE_TABLET_ITEM.get());
        out.accept(THE_UNSUNG_EGG.get());
    }

    private MarchRegistry() {}
}
