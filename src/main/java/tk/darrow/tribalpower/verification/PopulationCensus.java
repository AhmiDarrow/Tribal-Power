package tk.darrow.tribalpower.verification;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.entity.CreatureHabitat;
import tk.darrow.tribalpower.entity.CreatureProfile;
import tk.darrow.tribalpower.entity.LatticeMonster;
import tk.darrow.tribalpower.world.ModDimensions;

/**
 * Counts how many creatures the March holds around one standing player, by category, as the server's own
 * spawning, despawning and repopulation leave them.
 *
 * <p>A stand-in player stands at a site in each of a few biomes (no client, so view and simulation distance come
 * from server.properties as on a real server). The wild spirits are cleared and the server runs at night and then
 * by day; a line per snapshot gives what is loaded in the level and what stands within entity-ticking range, with
 * wild spirits split by whether they rise in caves and at lava or in the open. Before that it times, per call,
 * the lookups the tick-time fixes replaced. {@code gradlew runMarchPopulation} runs it and halts; read the
 * "March population" lines in its log.
 */
public final class PopulationCensus {
    private static final String[] SITES = {"march_snow_fields", "march_highlands", "march_steppe"};
    private static final int TICKS_PER_PHASE = 6000, SNAPSHOT_EVERY = 1500, TICKING = 128;
    private static final long NIGHT = 18000, DAY = 6000;

    private static MinecraftServer server;
    private static ServerPlayer watcher;
    private static EmbeddedChannel channel;
    private static int site = -1, phase, ticks, warming;

    private PopulationCensus() {}

    public static void onServerStarted(ServerStartedEvent event) {
        server = event.getServer();
        var march = server.getLevel(ModDimensions.THE_MARCH);
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "census-player"), false);
        watcher = new ServerPlayer(server, march, cookie.gameProfile(), cookie.clientInformation()) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return true; }
        };
        var connection = new Connection(PacketFlow.SERVERBOUND);
        channel = new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, watcher, cookie);
        server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        server.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        NeoForge.EVENT_BUS.addListener(PopulationCensus::tick);
        TribalPower.LOGGER.info("March population: view {} sim {}, difficulty {}",
                server.getPlayerList().getViewDistance(), server.getPlayerList().getSimulationDistance(),
                march.getDifficulty());
        timings(server.overworld());
        nextSite();
    }

    // ---- per-call timings of the hot paths the profile named ----------------------------------------

    /** Old and new cost per call of the lookups the tick-time fixes replaced, timed on this server. */
    private static void timings(ServerLevel level) {
        // Tribal Bench outline: the old code joined seven boxes on every call.
        net.minecraft.world.level.block.Block benchBlock = null;
        for (var block : BuiltInRegistries.BLOCK) if (block instanceof tk.darrow.tribalpower.block.TribalBenchBlock) { benchBlock = block; break; }
        var bench = benchBlock.defaultBlockState().setValue(tk.darrow.tribalpower.block.TribalBenchBlock.FACING, net.minecraft.core.Direction.EAST);
        var empty = net.minecraft.world.phys.shapes.CollisionContext.empty();
        double benchNew = perCall(200_000, () -> bench.getShape(level, BlockPos.ZERO, empty));
        double benchOld = perCall(200_000, () -> {
            var shape = net.minecraft.world.phys.shapes.Shapes.or(net.minecraft.world.level.block.Block.box(0, 6, 0, 16, 10, 16),
                    net.minecraft.world.level.block.Block.box(1, 0, 1, 4, 6, 4), net.minecraft.world.level.block.Block.box(12, 0, 1, 15, 6, 4),
                    net.minecraft.world.level.block.Block.box(1, 0, 12, 4, 6, 15), net.minecraft.world.level.block.Block.box(12, 0, 12, 15, 6, 15));
            return net.minecraft.world.phys.shapes.Shapes.or(shape, net.minecraft.world.phys.shapes.Shapes.or(
                    net.minecraft.world.level.block.Block.box(2, 10, 0, 4, 16, 16), net.minecraft.world.level.block.Block.box(1, 14, 0, 5, 16, 16)));
        });
        TribalPower.LOGGER.info("March population timing: bench outline {} us/call before, {} us/call now", fmt(benchOld), fmt(benchNew));

        // Station lookups: what a hopper asks an Echo Station on every insert attempt.
        var recipes = level.getRecipeManager().getAllRecipesFor(tk.darrow.tribalpower.echo.LatticeRecipe.TYPE.get());
        List<String> stations = new ArrayList<>();
        List<net.minecraft.world.item.ItemStack> inputs = new ArrayList<>();
        for (var holder : recipes) {
            if (!stations.contains(holder.value().station())) stations.add(holder.value().station());
            var items = holder.value().ingredient().getItems();
            if (items.length > 0 && inputs.size() < 64) inputs.add(items[0].copy());
        }
        inputs.add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE));
        int[] k = {0};
        int calls = stations.size() * inputs.size();
        double findOld = perCall(20_000, () -> {
            int i = k[0]++ % calls;
            return tk.darrow.tribalpower.echo.ProcessingRecipes.findWrittenUncached(level, stations.get(i % stations.size()), inputs.get(i / stations.size()));
        });
        double findNew = perCall(200_000, () -> {
            int i = k[0]++ % calls;
            return tk.darrow.tribalpower.echo.ProcessingRecipes.findWritten(level, stations.get(i % stations.size()), inputs.get(i / stations.size()));
        });
        TribalPower.LOGGER.info("March population timing: written-recipe lookup over {} recipes {} us/call before, {} us/call now",
                recipes.size(), fmt(findOld), fmt(findNew));

        // Ley Collector beat: the threads were read twice a beat during a surge; now once, and not again while the totems stand.
        BlockPos spot = level.getSharedSpawnPos();
        double sample = perCall(2_000, () -> tk.darrow.tribalpower.ley.LeyField.sample(level, spot));
        double near = perCall(20_000, () -> tk.darrow.tribalpower.ley.LeyMagnets.near(level, spot));
        double factors = perCall(2_000, () -> tk.darrow.tribalpower.ley.LeyMath.factors(level, spot));
        TribalPower.LOGGER.info("March population timing: ley threads {} us, totem search {} us, full survey {} us; a surge beat was"
                + " survey + threads = {} us, now survey without threads + search = {} us while the totems stand",
                fmt(sample), fmt(near), fmt(factors), fmt(factors + sample), fmt(factors - sample + near));
    }

    private static Object sink;

    private static double perCall(int n, java.util.function.Supplier<Object> work) {
        for (int i = 0; i < n; i++) sink = work.get();          // warm the JIT
        long start = System.nanoTime();
        for (int i = 0; i < n; i++) sink = work.get();
        return (System.nanoTime() - start) / 1000.0 / n;
    }

    private static String fmt(double us) {
        return String.format(java.util.Locale.ROOT, "%.3f", us);
    }

    private static void nextSite() {
        site++;
        if (site >= SITES.length) {
            TribalPower.LOGGER.info("March population done");
            Runtime.getRuntime().halt(0);
        }
        var march = server.getLevel(ModDimensions.THE_MARCH);
        BlockPos at = siteIn(march, SITES[site]);
        watcher.teleportTo(march, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 0, 0);
        march.getChunkSource().move(watcher);
        phase = 0;
        warming = 0;
        TribalPower.LOGGER.info("March population {}: standing at {}", SITES[site], at);
    }

    private static void startPhase(ServerLevel level) {
        ticks = 0;
        if (phase == 0) {
            // each site starts from no wild spirits, so the count is what the spawner builds while watched
            List<Entity> wild = new ArrayList<>();
            for (Entity entity : level.getAllEntities())
                if (entity instanceof LatticeMonster monster && !monster.isPersistenceRequired()) wild.add(entity);
            wild.forEach(Entity::discard);
        }
        server.overworld().setDayTime(phase == 0 ? NIGHT : DAY);   // the March reads the overworld clock
        server.tickRateManager().requestGameToSprint(TICKS_PER_PHASE + 20);
        TribalPower.LOGGER.info("March population {} {}: start", SITES[site], phase == 0 ? "night" : "day");
    }

    private static void tick(ServerTickEvent.Post event) {
        if (site < 0 || site >= SITES.length) return;
        channel.releaseOutbound();
        var level = server.getLevel(ModDimensions.THE_MARCH);
        if (warming >= 0) {
            BlockPos at = watcher.blockPosition();
            boolean ready = watcher.level() == level;
            for (int dx = -96; dx <= 96 && ready; dx += 48) for (int dz = -96; dz <= 96 && ready; dz += 48)
                ready = level.isNaturalSpawningAllowed(at.offset(dx, 0, dz));
            if (!ready && ++warming < 2400) return;
            TribalPower.LOGGER.info("March population {}: loaded after {} ticks (ready {})", SITES[site], warming, ready);
            warming = -1;
            startPhase(level);
            return;
        }
        if (++ticks % SNAPSHOT_EVERY == 0) snapshot(level);
        if (ticks < TICKS_PER_PHASE) return;
        if (++phase > 1) nextSite();
        else startPhase(level);
    }

    private static void snapshot(ServerLevel level) {
        Map<String, int[]> byKind = new TreeMap<>();   // [loaded, ticking]
        Map<String, Integer> monsters = new TreeMap<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity == null || entity == watcher) continue;
            boolean near = entity.distanceTo(watcher) <= TICKING;
            String kind = kind(entity);
            int[] tally = byKind.computeIfAbsent(kind, k -> new int[2]);
            tally[0]++;
            if (near) tally[1]++;
            if (entity instanceof LatticeMonster && kind.startsWith("monster"))
                monsters.merge(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath(), 1, Integer::sum);
        }
        int loaded = 0, ticking = 0;
        StringBuilder line = new StringBuilder();
        for (var entry : byKind.entrySet()) {
            loaded += entry.getValue()[0];
            ticking += entry.getValue()[1];
            line.append(String.format("%s %d/%d  ", entry.getKey(), entry.getValue()[0], entry.getValue()[1]));
        }
        TribalPower.LOGGER.info("March population {} {} @{}: total {}/{} (loaded/within {}) | {}| wild spirits: {}",
                SITES[site], phase == 0 ? "night" : "day", ticks, loaded, ticking, TICKING, line, monsters);
    }

    /** A creature's bucket: wild monsters split by where they rise, then the vanilla categories. */
    private static String kind(Entity entity) {
        if (entity instanceof LatticeMonster monster) {
            if (monster.isPersistenceRequired()) return "monster-kept";
            CreatureHabitat habitat = CreatureHabitat.of(CreatureProfile.of(monster.getType()));
            return habitat == CreatureHabitat.CAVE || habitat == CreatureHabitat.LAVA ? "monster-cave" : "monster-open";
        }
        if (entity instanceof Mob mob && (mob.isPersistenceRequired() || mob.requiresCustomPersistence())) return "kept";
        MobCategory category = entity.getType().getCategory();
        return category == MobCategory.MISC ? "misc" : category.getName();
    }

    /** A spot in the biome near the origin, standing on the surface. */
    private static BlockPos siteIn(ServerLevel march, String biome) {
        var key = ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, biome));
        var generator = march.getChunkSource().getGenerator();
        var state = march.getChunkSource().randomState();
        for (int ring = 0; ring <= 40; ring++) for (int gx = -ring; gx <= ring; gx++) for (int gz = -ring; gz <= ring; gz++) {
            if (Math.max(Math.abs(gx), Math.abs(gz)) != ring) continue;
            // far apart, so one site's population never reaches the next
            int x = 4000 + gx * 256, z = gz * 256 + site * 4000;
            int height = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, march, state);
            if (generator.getBiomeSource().getNoiseBiome(x >> 2, height >> 2, z >> 2, state.sampler()).is(key))
                return new BlockPos(x, height, z);
        }
        throw new IllegalStateException("No " + biome + " found");
    }
}
