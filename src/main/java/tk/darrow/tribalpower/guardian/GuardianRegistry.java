package tk.darrow.tribalpower.guardian;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;

/** The guardians, their altar, their cores and their eggs. */
public final class GuardianRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, TribalPower.MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TribalPower.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TribalPower.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TribalPower.MOD_ID);

    public static final Map<Guardian, DeferredHolder<EntityType<?>, EntityType<GuardianEntity>>> ENTITIES = new EnumMap<>(Guardian.class);
    public static final Map<Guardian, DeferredItem<Item>> CORES = new EnumMap<>(Guardian.class);
    public static final Map<Guardian, DeferredItem<Item>> EGGS = new EnumMap<>(Guardian.class);
    private static final Map<EntityType<?>, Guardian> BY_TYPE = new java.util.HashMap<>();

    public static final DeferredBlock<GuardianAltarBlock> ALTAR = BLOCKS.registerBlock("guardian_altar", GuardianAltarBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(6F, 1200F).sound(SoundType.DEEPSLATE_BRICKS).noOcclusion());
    public static final DeferredItem<net.minecraft.world.item.BlockItem> ALTAR_ITEM = ITEMS.registerSimpleBlockItem("guardian_altar", ALTAR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GuardianAltarBlockEntity>> ALTAR_ENTITY =
            BLOCK_ENTITIES.register("guardian_altar", () -> BlockEntityType.Builder.of(GuardianAltarBlockEntity::new, ALTAR.get()).build(null));

    static {
        for (Guardian guardian : Guardian.values()) {
            ENTITIES.put(guardian, ENTITY_TYPES.register(guardian.id, () -> {
                EntityType<GuardianEntity> type = EntityType.Builder.of(GuardianEntity::new, MobCategory.MONSTER)
                        .sized(guardian.width, guardian.height).fireImmune().clientTrackingRange(12).updateInterval(2)
                        .build("tribalpower:" + guardian.id);
                BY_TYPE.put(type, guardian);
                return type;
            }));
            CORES.put(guardian, ITEMS.register(guardian.coreId, () -> new Item(new Item.Properties().rarity(Rarity.RARE).fireResistant())));
            EGGS.put(guardian, ITEMS.register(guardian.id + "_spawn_egg", () ->
                    new DeferredSpawnEggItem(ENTITIES.get(guardian), 0x1a1a22, guardian.colour, new Item.Properties())));
        }
    }

    private GuardianRegistry() {}

    /** Which guardian an entity type is; the type is registered with its guardian, so this never misses. */
    public static Guardian guardianOf(EntityType<?> type) {
        Guardian guardian = BY_TYPE.get(type);
        if (guardian != null) return guardian;
        for (Guardian g : Guardian.values()) if (ENTITIES.get(g).get() == type) return g;
        throw new IllegalArgumentException("Not a guardian: " + type);
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(GuardianRegistry::attributes);
        modBus.addListener(GuardianRegistry::placements);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        for (Guardian guardian : Guardian.values()) event.put(ENTITIES.get(guardian).get(), GuardianEntity.createAttributes(guardian).build());
    }

    /** Guardians never spawn on their own: only an altar calls them. */
    private static void placements(RegisterSpawnPlacementsEvent event) {
        for (Guardian guardian : Guardian.values())
            event.register(ENTITIES.get(guardian).get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (type, level, reason, pos, random) -> reason != MobSpawnType.NATURAL && reason != MobSpawnType.CHUNK_GENERATION,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    public static void displayItems(CreativeModeTab.Output out) {
        out.accept(ALTAR_ITEM.get());
        CORES.values().forEach(core -> out.accept(core.get()));
        EGGS.values().forEach(egg -> out.accept(egg.get()));
    }
}
