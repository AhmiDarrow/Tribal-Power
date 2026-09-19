package tk.darrow.tribalpower.wildlife;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;

/**
 * The March's wild life beyond its land creatures: fish, jellies, rays and eels in its waters, swifts in its
 * sky, and (client side only, see the client's MarchInsects) the insects that fill the air near the ground.
 *
 * <p>Budget: every creature here is in a vanilla category with its own cap (water ambient, water creature,
 * ambient) and despawns when no one is near; insects are particles and never touch the server.
 */
public final class Wildlife {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, TribalPower.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TribalPower.MOD_ID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, TribalPower.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<GlimmerfinEntity>> GLIMMERFIN = ENTITIES.register("glimmerfin",
            () -> EntityType.Builder.of(GlimmerfinEntity::new, MobCategory.WATER_AMBIENT).sized(0.5F, 0.3F).clientTrackingRange(4).build("tribalpower:glimmerfin"));
    public static final DeferredHolder<EntityType<?>, EntityType<MarchSwimmerEntity>> DRIFT_BELL = swimmer("drift_bell", 0.8F, 0.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<MarchSwimmerEntity>> VEIL_RAY = swimmer("veil_ray", 1.4F, 0.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<MarchSwimmerEntity>> SILT_EEL = swimmer("silt_eel", 0.6F, 0.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<LoomSwiftEntity>> LOOM_SWIFT = ENTITIES.register("loom_swift",
            () -> EntityType.Builder.of(LoomSwiftEntity::new, MobCategory.AMBIENT).sized(0.4F, 0.3F).clientTrackingRange(8).build("tribalpower:loom_swift"));

    public static final DeferredItem<Item> RAW_GLIMMERFIN = food("raw_glimmerfin", new FoodProperties.Builder().nutrition(2).saturationModifier(0.1F).build());
    public static final DeferredItem<Item> COOKED_GLIMMERFIN = food("cooked_glimmerfin", new FoodProperties.Builder().nutrition(5).saturationModifier(0.6F).build());
    public static final DeferredItem<Item> RAW_SILT_EEL = food("raw_silt_eel", new FoodProperties.Builder().nutrition(3).saturationModifier(0.2F).build());
    public static final DeferredItem<Item> SMOKED_SILT_EEL = food("smoked_silt_eel", new FoodProperties.Builder().nutrition(7).saturationModifier(0.8F).build());
    public static final DeferredItem<Item> DRIFT_JELLY = food("drift_jelly", new FoodProperties.Builder().nutrition(2).saturationModifier(0.2F).alwaysEdible()
            .effect(() -> new MobEffectInstance(MobEffects.WATER_BREATHING, 900), 1.0F)
            .effect(() -> new MobEffectInstance(MobEffects.NIGHT_VISION, 900), 1.0F).build());
    public static final DeferredItem<MobBucketItem> GLIMMERFIN_BUCKET = ITEMS.register("glimmerfin_bucket",
            () -> new MobBucketItem(GLIMMERFIN.get(), Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, new Item.Properties().stacksTo(1)));

    public static final List<DeferredItem<DeferredSpawnEggItem>> EGGS = List.of(
            egg("glimmerfin", GLIMMERFIN, 0x1E5C66, 0x7CF6E0),
            egg("drift_bell", DRIFT_BELL, 0x5A4A8C, 0xB9F4FF),
            egg("veil_ray", VEIL_RAY, 0x2C3F55, 0x8FD8FF),
            egg("silt_eel", SILT_EEL, 0x3A3524, 0xC8E070),
            egg("loom_swift", LOOM_SWIFT, 0x33303E, 0xF2C46A));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FIREFLY = particle("firefly");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> REED_DARTER = particle("reed_darter");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GLASSWING = particle("glasswing");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CINDER_GNAT = particle("cinder_gnat");

    private Wildlife() {}

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        PARTICLES.register(modBus);
        modBus.addListener(Wildlife::attributes);
        modBus.addListener(Wildlife::placements);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(Wildlife::flocks);
    }

    /**
     * Swifts arrive as flocks. Vanilla ambient spawning would almost never pick a spot in open sky and shares
     * its small cap with bats and wisps, so this places them directly: by day, in the March, every twelve
     * seconds or so per player, a flock of three to six in open sky nearby, never more than twelve around one
     * player. They despawn like any ambient creature once nobody is near.
     */
    private static void flocks(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) || player.isSpectator()) return;
        var level = player.serverLevel();
        if ((level.getGameTime() + player.getId()) % 240 != 0 || !level.dimension().equals(tk.darrow.tribalpower.world.ModDimensions.THE_MARCH)
                || !level.isDay() || level.isRaining()) return;
        if (level.getEntitiesOfClass(LoomSwiftEntity.class, player.getBoundingBox().inflate(64)).size() >= 12) return;
        var random = level.getRandom();
        double angle = random.nextDouble() * net.minecraft.util.Mth.TWO_PI, distance = 32 + random.nextInt(24);
        int x = (int) (player.getX() + Math.cos(angle) * distance), z = (int) (player.getZ() + Math.sin(angle) * distance);
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) return;
        var biome = level.getBiome(new BlockPos(x, 64, z));
        if (biome.is(net.minecraft.resources.ResourceKey.create(Registries.BIOME,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "march_ember_wastes")))) return;
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        int count = 3 + random.nextInt(4);
        for (int i = 0; i < count; i++) {
            BlockPos at = new BlockPos(x + random.nextInt(5) - 2, ground + 8 + random.nextInt(8), z + random.nextInt(5) - 2);
            if (!level.getBlockState(at).isAir() || !level.canSeeSky(at)) continue;
            LOOM_SWIFT.get().spawn(level, at, MobSpawnType.EVENT);
        }
    }

    /** Items in creative-tab order. */
    public static List<DeferredItem<? extends Item>> tabItems() {
        var items = new java.util.ArrayList<DeferredItem<? extends Item>>(List.of(RAW_GLIMMERFIN, COOKED_GLIMMERFIN,
                RAW_SILT_EEL, SMOKED_SILT_EEL, DRIFT_JELLY, GLIMMERFIN_BUCKET));
        items.addAll(EGGS);
        return items;
    }

    private static DeferredHolder<EntityType<?>, EntityType<MarchSwimmerEntity>> swimmer(String id, float width, float height) {
        return ENTITIES.register(id, () -> EntityType.Builder.of(MarchSwimmerEntity::new, MobCategory.WATER_CREATURE)
                .sized(width, height).clientTrackingRange(6).build("tribalpower:" + id));
    }

    private static DeferredItem<Item> food(String id, FoodProperties food) {
        return ITEMS.registerSimpleItem(id, new Item.Properties().food(food));
    }

    private static DeferredItem<DeferredSpawnEggItem> egg(String id, DeferredHolder<EntityType<?>, ? extends EntityType<? extends net.minecraft.world.entity.Mob>> type,
                                                        int base, int spots) {
        return ITEMS.register(id + "_spawn_egg", () -> new DeferredSpawnEggItem(type, base, spots, new Item.Properties()));
    }

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> particle(String id) {
        return PARTICLES.register(id, () -> new SimpleParticleType(false));
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(GLIMMERFIN.get(), AbstractFish.createAttributes().build());
        event.put(DRIFT_BELL.get(), AbstractFish.createAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.25).build());
        event.put(VEIL_RAY.get(), AbstractFish.createAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.9).build());
        event.put(SILT_EEL.get(), AbstractFish.createAttributes().add(Attributes.MAX_HEALTH, 12.0).add(Attributes.MOVEMENT_SPEED, 1.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.FOLLOW_RANGE, 12.0).build());
        event.put(LOOM_SWIFT.get(), LoomSwiftEntity.createAttributes().build());
    }

    private static void placements(RegisterSpawnPlacementsEvent event) {
        for (var type : List.of(GLIMMERFIN.get(), DRIFT_BELL.get(), VEIL_RAY.get(), SILT_EEL.get()))
            event.register(type, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Wildlife::inWater,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(LOOM_SWIFT.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING, Wildlife::inOpenSky,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /**
     * Any water with water or air above it, however shallow: March meres and pools sit well above sea level and
     * are often only a block or two deep, so vanilla's sea-level rule would refuse nearly all of them.
     */
    private static <T extends net.minecraft.world.entity.Entity> boolean inWater(EntityType<T> type, ServerLevelAccessor level,
                                                                                 MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getFluidState(pos).is(FluidTags.WATER)
                && (level.getFluidState(pos.above()).is(FluidTags.WATER) || level.getBlockState(pos.above()).isAir());
    }

    private static <T extends net.minecraft.world.entity.Entity> boolean inOpenSky(EntityType<T> type, ServerLevelAccessor level,
                                                                                   MobSpawnType reason, BlockPos pos, RandomSource random) {
        return reason != MobSpawnType.NATURAL || (level.canSeeSky(pos) && level.getBlockState(pos).isAir()
                && pos.getY() >= level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()));
    }
}
