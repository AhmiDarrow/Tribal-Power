package tk.darrow.tribalpower.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.config.TribalConfig;

/**
 * The March's own weather: one state per biome that has one, beyond rain. Each dims the world, shifts what
 * spawns, and favours one voice's generators while it hampers another's. The server keeps which are running in
 * {@link MarchEventsSavedData}; the client hears it in {@link MarchStatePayload}.
 */
public enum MarchWeather {
    ASHFALL("march_ember_wastes", Attunement.FIRE, Attunement.AIR, ParticleTypes.ASH, 0.36F, 0.30F, 0.28F, 40),
    GLIMMER_STORM("march_crystal_fields", Attunement.SPIRIT, Attunement.EARTH, ParticleTypes.END_ROD, 0.72F, 0.66F, 0.90F, 56),
    WHITEOUT("march_snow_fields", Attunement.AIR, Attunement.WATER, ParticleTypes.SNOWFLAKE, 0.90F, 0.92F, 0.96F, 22),
    FEN_MIST("march_reed_fen", Attunement.WATER, Attunement.FIRE, ParticleTypes.CLOUD, 0.58F, 0.66F, 0.60F, 30);

    public final ResourceKey<Biome> biome;
    public final Attunement favours, hampers;
    public final ParticleOptions particle;
    public final float red, green, blue;
    /** How far you can see in it, in blocks. */
    public final int sight;

    MarchWeather(String biome, Attunement favours, Attunement hampers, ParticleOptions particle, float red, float green, float blue, int sight) {
        this.biome = ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("tribalpower", biome));
        this.favours = favours; this.hampers = hampers; this.particle = particle;
        this.red = red; this.green = green; this.blue = blue; this.sight = sight;
    }

    public String key() { return "weather.tribalpower." + name().toLowerCase(java.util.Locale.ROOT); }

    /** The weather this biome can have, or null. */
    public static MarchWeather forBiome(ResourceKey<Biome> biome) {
        for (MarchWeather weather : values()) if (weather.biome.equals(biome)) return weather;
        return null;
    }

    /** Whether this weather is running now, on either side. */
    public boolean active(Level level) {
        if (level.isClientSide) return MarchStatePayload.latest.weather(this);
        return level instanceof ServerLevel server && MarchEventsSavedData.get(server.getServer()).weatherActive(this, server.getGameTime());
    }

    /** The weather running over this position, or null when the sky is plain. */
    public static MarchWeather at(Level level, BlockPos pos) {
        if (!level.dimension().equals(tk.darrow.tribalpower.world.ModDimensions.THE_MARCH)) return null;
        var biome = level.getBiome(pos).unwrapKey().orElse(null);
        if (biome == null) return null;
        MarchWeather weather = forBiome(biome);
        return weather != null && weather.active(level) ? weather : null;
    }

    /** What this weather does to a generator of the given voice: favoured voices make more, hampered ones less. */
    public double generatorScale(Attunement voice) {
        if (voice == favours) return TribalConfig.weatherGeneratorBonus();
        if (voice == hampers) return TribalConfig.weatherGeneratorPenalty();
        return 1.0;
    }

    /** The scale for a generator standing here, 1 under a plain sky. */
    public static double generatorScale(Level level, BlockPos pos, Attunement voice) {
        MarchWeather weather = at(level, pos);
        return weather == null ? 1.0 : weather.generatorScale(voice);
    }

    /** Whether the weather here is thick enough that spirits rise as if it were night. */
    public static boolean darkens(Level level, BlockPos pos) {
        return TribalConfig.weatherSpawnShift() && at(level, pos) != null;
    }
}
