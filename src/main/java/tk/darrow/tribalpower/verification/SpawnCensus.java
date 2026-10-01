package tk.darrow.tribalpower.verification;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.wildlife.Wildlife;
import tk.darrow.tribalpower.world.ModDimensions;

/**
 * Counts the March's water life as the natural spawner really produces it.
 *
 * <p>The survey asks each creature's placement rules about one spot per column; this puts a stand-in player
 * in a biome, walks it round a 48-block ring as an explorer would, and lets the server run, so the category
 * caps, the spawner's choice of height and despawning all have their say. Each arrival is logged by biome as
 * in open water (nothing solid between it and the sky) or buried (aquifers and flooded caves under the land,
 * which a player never sees), with how many are alive near the player every few minutes.
 * {@code gradlew runMarchCensus} starts a server on the survey's world ({@code runMarchSurvey} makes it) with
 * {@code -Dtribalpower.spawnCensus=true} and halts when done; read the "March census" lines in its log.
 */
public final class SpawnCensus {
    private static final String[] SITES = {"march_reed_fen", "march_shallows", "march_steppe", "march_ember_wastes"};
    private static final int TICKS_PER_SITE = 12000, SNAPSHOT_EVERY = 3000, WANDER_EVERY = 600, RADIUS = 128;

    private static MinecraftServer server;
    private static ServerPlayer watcher;
    private static BlockPos centre = BlockPos.ZERO;
    private static EmbeddedChannel channel;
    private static int site = -1, ticks, warming;
    /** Per type and biome: arrivals in open water, then buried 1-4, 5-16 and over 16 blocks under the ground above them. */
    private static final Map<String, int[]> ARRIVALS = new TreeMap<>();

    private SpawnCensus() {}

    private static List<EntityType<?>> counted() {
        return List.of(Wildlife.GLIMMERFIN.get(), Wildlife.DRIFT_BELL.get(), Wildlife.VEIL_RAY.get(), Wildlife.SILT_EEL.get());
    }

    public static void onServerStarted(ServerStartedEvent event) {
        server = event.getServer();
        var march = server.getLevel(ModDimensions.THE_MARCH);
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "census-watcher"), false);
        watcher = new ServerPlayer(server, march, cookie.gameProfile(), cookie.clientInformation()) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return true; }
        };
        var connection = new Connection(PacketFlow.SERVERBOUND);
        channel = new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, watcher, cookie);
        NeoForge.EVENT_BUS.addListener(SpawnCensus::tick);
        NeoForge.EVENT_BUS.addListener(SpawnCensus::arrived);
        nextSite();
    }

    private static void nextSite() {
        site++;
        ticks = 0;
        ARRIVALS.clear();
        if (site >= SITES.length) {
            TribalPower.LOGGER.info("March census done");
            Runtime.getRuntime().halt(0);
        }
        var march = server.getLevel(ModDimensions.THE_MARCH);
        clear(march);
        BlockPos at = wetSite(march, SITES[site]);
        centre = at;
        watcher.teleportTo(march, at.getX() + 0.5, at.getY() + 2, at.getZ() + 0.5, 0, 0);
        march.getChunkSource().move(watcher);   // a real client's next move packet would do this
        warming = 0;
        TribalPower.LOGGER.info("March census {}: watching from {}", SITES[site], at);
    }

    private static void arrived(EntityJoinLevelEvent event) {
        if (site < 0 || site >= SITES.length || event.loadedFromDisk() || !(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(ModDimensions.THE_MARCH) || !counted().contains(event.getEntity().getType())) return;
        Entity entity = event.getEntity();
        BlockPos pos = entity.blockPosition();
        String biome = level.getBiome(pos).unwrapKey().map(k -> k.location().getPath().replace("march_", "")).orElse("?");
        int[] tally = ARRIVALS.computeIfAbsent(name(entity.getType()) + "@" + biome, k -> new int[4]);
        int depth = level.getHeight(Heightmap.Types.OCEAN_FLOOR, pos.getX(), pos.getZ()) - 1 - pos.getY();
        tally[open(level, pos) ? 0 : depth <= 4 ? 1 : depth <= 16 ? 2 : 3]++;
    }

    private static void tick(ServerTickEvent.Post event) {
        if (site < 0 || site >= SITES.length) return;
        channel.releaseOutbound();
        var level = server.getLevel(ModDimensions.THE_MARCH);
        if (warming >= 0) {
            // the stand-in's chunks load like a player's, over a few seconds; counting starts once the ring spawns
            BlockPos at = watcher.blockPosition();
            boolean ready = watcher.level() == level;
            for (int dx = -96; dx <= 96 && ready; dx += 48) for (int dz = -96; dz <= 96 && ready; dz += 48)
                ready = level.isNaturalSpawningAllowed(at.offset(dx, 0, dz));
            if (!ready && ++warming < 2400) return;
            TribalPower.LOGGER.info("March census {}: counting after {} ticks of loading (ready {}), in {}", SITES[site], warming, ready,
                    watcher.level().dimension().location());
            warming = -1;
            clear(level);
            ARRIVALS.clear();
            server.tickRateManager().requestGameToSprint(TICKS_PER_SITE + 20);
        }
        if (++ticks % WANDER_EVERY == 0) {
            // a player exploring, not standing still: fish within 32 blocks of a still player never despawn
            double angle = ticks / WANDER_EVERY * Math.PI / 4;
            watcher.teleportTo(level, centre.getX() + 0.5 + Math.cos(angle) * 48, centre.getY() + 2, centre.getZ() + 0.5 + Math.sin(angle) * 48, 0, 0);
            level.getChunkSource().move(watcher);
        }
        if (ticks % SNAPSHOT_EVERY != 0) return;
        var march = server.getLevel(ModDimensions.THE_MARCH);
        Map<String, int[]> living = new TreeMap<>();
        for (var entity : march.getAllEntities()) {
            if (entity == null || !counted().contains(entity.getType()) || entity.distanceTo(watcher) > RADIUS) continue;
            living.computeIfAbsent(name(entity.getType()), k -> new int[2])[open(march, entity.blockPosition()) ? 0 : 1]++;
        }
        StringBuilder alive = new StringBuilder(), came = new StringBuilder();
        living.forEach((k, v) -> alive.append(String.format("%s %d open/%d buried  ", k, v[0], v[1])));
        ARRIVALS.forEach((k, v) -> came.append(String.format("%s %d open/%d buried (%d shallow, %d mid, %d deep)  ", k, v[0],
                v[1] + v[2] + v[3], v[1], v[2], v[3])));
        TribalPower.LOGGER.info("March census {} @{} ticks: living within {}: {}| arrived so far: {}", SITES[site], ticks, RADIUS,
                alive.toString(), came.toString());
        if (ticks >= TICKS_PER_SITE) nextSite();
    }

    /** Removes the counted kinds, so each site starts from empty water. Collected first: discarding mid-walk breaks the walk. */
    private static void clear(ServerLevel level) {
        List<Entity> found = new java.util.ArrayList<>();
        for (var entity : level.getAllEntities()) if (entity != null && counted().contains(entity.getType())) found.add(entity);
        found.forEach(Entity::discard);
    }

    /** Water a player can see into: nothing solid between it and the sky. */
    private static boolean open(ServerLevel level, BlockPos pos) {
        return pos.getY() >= level.getHeight(Heightmap.Types.OCEAN_FLOOR, pos.getX(), pos.getZ()) - 1;
    }

    private static String name(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
    }

    /** The spot in the biome, within a few thousand blocks, with the most open surface water around it. */
    private static BlockPos wetSite(ServerLevel march, String biome) {
        var key = ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, biome));
        var generator = march.getChunkSource().getGenerator();
        var state = march.getChunkSource().randomState();
        BlockPos best = null;
        int bestWet = -1, checked = 0;
        for (int ring = 0; ring <= 30 && checked < 12; ring++) for (int gx = -ring; gx <= ring; gx++) for (int gz = -ring; gz <= ring; gz++) {
            if (Math.max(Math.abs(gx), Math.abs(gz)) != ring || checked >= 12) continue;
            int x = gx * 192, z = gz * 192;
            int height = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, march, state);
            if (!generator.getBiomeSource().getNoiseBiome(x >> 2, height >> 2, z >> 2, state.sampler()).is(key)) continue;
            checked++;
            for (int cx = (x - 48) >> 4; cx <= (x + 48) >> 4; cx++) for (int cz = (z - 48) >> 4; cz <= (z + 48) >> 4; cz++)
                march.getChunk(cx, cz);   // getHeight reads an unloaded chunk as the bottom of the world
            int wet = 0;
            for (int dx = -48; dx <= 48; dx += 4) for (int dz = -48; dz <= 48; dz += 4) {
                int top = march.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz);
                if (march.getFluidState(new BlockPos(x + dx, top - 1, z + dz)).is(FluidTags.WATER)) wet++;
            }
            if (wet > bestWet) { bestWet = wet; best = new BlockPos(x, height, z); }
        }
        if (best == null) throw new IllegalStateException("No " + biome + " found");
        TribalPower.LOGGER.info("March census {}: {} of 625 sampled columns are open water", biome, bestWet);
        return best;
    }
}
