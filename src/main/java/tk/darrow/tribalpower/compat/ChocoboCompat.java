package tk.darrow.tribalpower.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.world.ModDimensions;

/**
 * Chocobos Reborn in The March. Spawns, farms and wild greens are data (conditional biome modifiers and
 * tags); the one thing data cannot say is plumage, which that mod picks by dimension. The Ember Wastes
 * are Spark-tribe country, so wild birds hatched there wear Flame.
 *
 * <p>There is no compile dependency: plumage is set through the bird's own save format.
 */
public final class ChocoboCompat {
    public static final String MOD_ID = "chocobosreborn";
    private static final ResourceLocation CHOCOBO = ResourceLocation.fromNamespaceAndPath(MOD_ID, "chocobo");
    private static final ResourceKey<Biome> EMBER_WASTES = ResourceKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "march_ember_wastes"));
    private static final String PENDING = "TribalMarchFlame";
    private static final int FLAME = 7;
    private static final float FLAME_HEALTH = 50.0F;

    private ChocoboCompat() {}

    public static void register() {
        if (!ModList.get().isLoaded(MOD_ID)) return;
        NeoForge.EVENT_BUS.addListener(ChocoboCompat::markWildSpawn);
        NeoForge.EVENT_BUS.addListener(ChocoboCompat::applyPlumage);
    }

    /** Only birds the world itself spawned: eggs, breeding and travelling mounts keep their colour. */
    private static void markWildSpawn(FinalizeSpawnEvent event) {
        var type = event.getSpawnType();
        if (type != MobSpawnType.NATURAL && type != MobSpawnType.CHUNK_GENERATION) return;
        var mob = event.getEntity();
        if (!BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).equals(CHOCOBO)) return;
        // Chunk-generation spawns run on a worldgen thread: read the biome from the region, not the ServerLevel.
        var region = event.getLevel();
        if (!region.getLevel().dimension().equals(ModDimensions.THE_MARCH)) return;
        if (region.getBiome(mob.blockPosition()).is(EMBER_WASTES)) mob.getPersistentData().putBoolean(PENDING, true);
    }

    /** The bird settles its own colour while finalising, so ours is applied once it joins the level. */
    private static void applyPlumage(EntityJoinLevelEvent event) {
        var entity = event.getEntity();
        if (event.getLevel().isClientSide() || !entity.getPersistentData().getBoolean(PENDING)) return;
        entity.getPersistentData().remove(PENDING);
        CompoundTag tag = entity.saveWithoutId(new CompoundTag());
        tag.putInt("Plumage", FLAME);
        tag.putFloat("Health", FLAME_HEALTH);
        entity.load(tag);
    }
}
