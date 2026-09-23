package tk.darrow.tribalpower.verification;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.world.ModDimensions;

/**
 * Generates real March chunks and holds them to a budget. Flora density is what made the dimension slow
 * (meshing, light, particles), so a regression there should fail a run rather than a player's frame rate.
 * Also writes {@code march_survey.png}: a top-down colour map over a height map for each surveyed site.
 *
 * <p>The GameTest server only has an Overworld, so this rides a real server: {@code gradlew runMarchSurvey}
 * starts one with {@code -Dtribalpower.marchSurvey=true}, surveys, and halts with the verdict as exit code.
 */
public final class MarchSurvey {
    private static final int SITE_CHUNKS = 8, SIZE = SITE_CHUNKS * 16;
    private static final double MAX_PLANTS_PER_CHUNK = 70, MAX_LIGHTS_PER_CHUNK = 8, MIN_PLANTS_PER_CHUNK = 2;
    /** Below this many sampled spots a spawn result is noise, not evidence. */
    private static final int MIN_SPAWN_SAMPLE = 12;
    /** How much of a land biome may lie under water before it reads as ocean to a player. */
    private static final double MAX_BIOME_UNDER_WATER = 0.35;
    /** How much dry land the sea biome may stand on before the March reads as ocean. */
    private static final double MAX_SEA_BIOME_ON_LAND = 0.10;
    private static final String[] SITES = {"march_steppe", "march_crystal_fields", "march_highlands", "march_reed_fen",
            "march_snow_fields", "march_ember_wastes", "march_glimmer_ridge"};

    /** Water and cave decoration that must turn up across the surveyed sites. */
    private static final java.util.Set<String> DECOR = java.util.Set.of("march_lily_pad", "moon_lily", "ribbon_weed", "march_silt",
            "veil_lichen", "echo_roots", "lantern_cap");

    /** Every plant seen, by id, so compat flora shows up by name in the log. */
    private static final Map<String, Integer> PLANTS = new TreeMap<>();

    private MarchSurvey() {}

    public static void onServerStarted(ServerStartedEvent event) {
        int verdict = 1;
        try {
            verdict = run(event.getServer().getLevel(ModDimensions.THE_MARCH)) ? 0 : 1;
            // Leaves a real save behind, so the retrogen can be exercised against it.
            event.getServer().saveAllChunks(true, true, true);
        } catch (Throwable error) {
            TribalPower.LOGGER.error("March survey crashed", error);
        }
        Runtime.getRuntime().halt(verdict);
    }

    private static boolean run(ServerLevel march) throws java.io.IOException {
        if (march == null) throw new IllegalStateException("The March is not loaded");
        List<BlockPos> origins = new ArrayList<>();
        for (String site : SITES) origins.add(landSite(march, site));
        var image = new BufferedImage(SIZE * SITES.length, SIZE * 2, BufferedImage.TYPE_INT_RGB);
        boolean ok = true;
        for (int site = 0; site < SITES.length; site++) {
            long[] totals = new long[2]; // plants, lights
            Map<String, Integer> tops = new TreeMap<>();
            long started = System.nanoTime();
            for (int cz = 0; cz < SITE_CHUNKS; cz++) for (int cx = 0; cx < SITE_CHUNKS; cx++)
                survey(march, origins.get(site), site, cx, cz, image, totals, tops);
            int chunks = SITE_CHUNKS * SITE_CHUNKS;
            double plants = totals[0] / (double) chunks, lights = totals[1] / (double) chunks;
            // The Ember Wastes and the Glimmer Ridge are meant to be nearly bare: one is ash, the
            // other is a mineral peak. Holding them to a plant count would be asking them to stop
            // being what they are.
            boolean bare = SITES[site].equals("march_ember_wastes") || SITES[site].equals("march_glimmer_ridge");
            boolean pass = plants <= MAX_PLANTS_PER_CHUNK && lights <= MAX_LIGHTS_PER_CHUNK
                    && (plants >= MIN_PLANTS_PER_CHUNK || bare);
            ok &= pass;
            TribalPower.LOGGER.info("March survey {} {}: {} plants/chunk, {} lights/chunk, {} ms/chunk, surface {}",
                    pass ? "PASS" : "FAIL", SITES[site], String.format("%.1f", plants), String.format("%.1f", lights),
                    (System.nanoTime() - started) / 1_000_000 / chunks, tops);
        }
        ImageIO.write(image, "png", new File("march_survey.png"));
        TribalPower.LOGGER.info("March survey plants: {}", PLANTS);
        return ok & spread(march) & underground(march, origins) & structures(march, origins) & spawns(march, origins) & trees(march);
    }

    /**
     * Every creature a March biome lists must be able to spawn somewhere in that biome under its own rules.
     * Samples real ground and water in each surveyed site and runs each listed type's placement and spawn checks,
     * by day and by night; a type that never passes in its own land is a dead spawn entry.
     */
    /**
     * How the March actually divides between its biomes.
     *
     * <p>Parameter boxes in the dimension file say what a biome asks for, not how much land it wins:
     * multi-noise picks the nearest point, so a narrow box can end up almost unreachable while a wide
     * one swallows the map. A player who has to walk four thousand blocks to stand in a biome will
     * never see it. So this samples the biome source over a wide grid and holds every biome to a share
     * of the whole.
     */
    private static final double MIN_SHARE = 0.05;   // one biome in twenty, at worst
    private static final double MAX_SHARE = 0.34;

    /**
     * The March's own waterline.
     *
     * <p>{@code Level.getSeaLevel()} is a flat 63 for every dimension and says nothing about this one,
     * whose noise settings put the sea at 48. Measuring against 63 counted fifteen blocks of dry
     * hillside as ocean, which is how the Shallows came to be sized to swallow the map.
     */
    private static int drownedTotal(java.util.Map<String, Integer> counts) {
        int wet = 0;
        for (var e : counts.entrySet()) if (e.getKey().endsWith("(SEA)")) wet += e.getValue();
        return wet;
    }

    private static int seaLevel(ServerLevel march) {
        return march.getChunkSource().getGenerator().getSeaLevel();
    }

    private static boolean spread(ServerLevel march) {
        var source = march.getChunkSource().getGenerator().getBiomeSource();
        var sampler = march.getChunkSource().randomState().sampler();
        var counts = new TreeMap<String, Integer>();
        // Only dry land counts. Sampling the biome source alone says which biome *claims* a spot, not
        // whether there is any ground there: a biome can hold a fifth of the parameter space and still
        // be sea everywhere it lands, which is exactly what happened when these were first rebalanced.
        var generator = march.getChunkSource().getGenerator();
        var state = march.getChunkSource().randomState();
        int step = 48, span = 110, total = 0, wet = 0;
        for (int gx = -span; gx <= span; gx++) for (int gz = -span; gz <= span; gz++) {
            int x = gx * step, z = gz * step;
            int height = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, march, state);
            var biome = source.getNoiseBiome(x >> 2, 64 >> 2, z >> 2, sampler);
            String name = biome.unwrapKey().map(k -> k.location().getPath()).orElse("?");
            if (height <= seaLevel(march)) { wet++; counts.merge(name + " (SEA)", 1, Integer::sum); continue; }
            counts.merge(name, 1, Integer::sum);
            total++;
        }
        // Where the ground actually sits relative to the waterline: if half the March is sea, that is
        // the terrain, not the biome list, and no amount of reshuffling parameter boxes will fix it.
        var heights = new ArrayList<Integer>();
        for (int gx = -span; gx <= span; gx += 2) for (int gz = -span; gz <= span; gz += 2)
            heights.add(generator.getBaseHeight(gx * step, gz * step, Heightmap.Types.OCEAN_FLOOR_WG, march, state));
        java.util.Collections.sort(heights);
        TribalPower.LOGGER.info("March terrain height p05/p25/p50/p75/p95: {} {} {} {} {}  (sea level {})",
                heights.get(heights.size() / 20), heights.get(heights.size() / 4),
                heights.get(heights.size() / 2), heights.get(heights.size() * 3 / 4),
                heights.get(heights.size() * 19 / 20), seaLevel(march));
        TribalPower.LOGGER.info("March survey spread: {} of {} sampled columns were below sea level",
                wet, wet + total);
        // What the March's noise actually produces, so the boxes can be aimed at real values
        // rather than at the whole -1..1 range, most of which is never sampled.
        var spread = new java.util.TreeMap<String, java.util.List<Double>>();
        for (String k : new String[]{"T", "H", "C", "E", "W"}) spread.put(k, new ArrayList<>());
        for (int gx = -60; gx <= 60; gx += 3) for (int gz = -60; gz <= 60; gz += 3) {
            var pt = sampler.sample((gx * step) >> 2, 64 >> 2, (gz * step) >> 2);
            spread.get("T").add(pt.temperature() / 10000.0);
            spread.get("H").add(pt.humidity() / 10000.0);
            spread.get("C").add(pt.continentalness() / 10000.0);
            spread.get("E").add(pt.erosion() / 10000.0);
            spread.get("W").add(pt.weirdness() / 10000.0);
        }
        var climate = new StringBuilder();
        for (var e : spread.entrySet()) {
            var v = new ArrayList<>(e.getValue()); java.util.Collections.sort(v);
            climate.append(String.format("%s[%.2f %.2f %.2f %.2f %.2f] ", e.getKey(),
                    v.get(0), v.get(v.size() / 4), v.get(v.size() / 2), v.get(v.size() * 3 / 4), v.get(v.size() - 1)));
        }
        TribalPower.LOGGER.info("March climate min/q1/med/q3/max: {}", climate.toString().trim());
        boolean ok = true;
        var report = new StringBuilder();
        for (var e : counts.entrySet()) {
            double share = e.getValue() / (double) total;
            report.append(String.format("%s %.1f%%  ", e.getKey(), share * 100));
            // The Shallows are the March's sea, so they are judged on the water they hold, not land.
            if (!e.getKey().startsWith("march_") || e.getKey().endsWith("(SEA)")
                    || e.getKey().equals("march_shallows")) continue;
            if (share < MIN_SHARE || share > MAX_SHARE) ok = false;
        }
        // What matters is how much of a biome is under water, not how much of the map. Measuring
        // against the whole map could never fire: the March is only a fifth sea, so seven biomes
        // splitting all of it still read as a couple of percent each. A biome that is mostly ocean
        // is the thing a player actually walks into.
        for (var e : counts.entrySet()) {
            if (!e.getKey().endsWith("(SEA)")) continue;
            String name = e.getKey().substring(0, e.getKey().length() - 6);
            if (name.equals("march_shallows")) continue;          // the Shallows are meant to be wet
            int wetHere = e.getValue(), dryHere = counts.getOrDefault(name, 0);
            if (wetHere + dryHere < 200) continue;                // too small a sample to judge
            double wetFraction = wetHere / (double) (wetHere + dryHere);
            if (wetFraction > MAX_BIOME_UNDER_WATER) {
                ok = false;
                TribalPower.LOGGER.error("March survey spread FAIL: {} is {}% under water; a land biome"
                        + " should meet the sea only at its coast", name, (int) (wetFraction * 100));
            }
        }
        // And the fault that actually shipped: the sea biome standing on dry land. Ground wearing the
        // Shallows' fog, water colours and empty feature list reads as ocean whether or not it is wet,
        // and it covered a quarter of the March's land before this was measured properly.
        double shallowsOnLand = counts.getOrDefault("march_shallows", 0) / (double) total;
        if (shallowsOnLand > MAX_SEA_BIOME_ON_LAND) {
            ok = false;
            TribalPower.LOGGER.error("March survey spread FAIL: the Shallows cover {}% of the dry land;"
                    + " a sea biome belongs on the sea", (int) (shallowsOnLand * 100));
        } else {
            TribalPower.LOGGER.info("March survey spread: the Shallows stand on {}% of the dry land",
                    (int) (shallowsOnLand * 100));
        }

        for (String site : SITES) {                    // a biome that never appeared at all
            if (!counts.containsKey(site)) { counts.put(site, 0); ok = false; }
        }
        TribalPower.LOGGER.info("March survey spread {}: {}", ok ? "PASS" : "FAIL", report.toString().trim());
        if (!ok)
            TribalPower.LOGGER.error("March survey spread FAIL: every March biome must hold between {}% and {}% of the map",
                    (int) (MIN_SHARE * 100), (int) (MAX_SHARE * 100));
        return ok;
    }

    private static boolean spawns(ServerLevel march, List<BlockPos> origins) {
        var categories = new net.minecraft.world.entity.MobCategory[]{net.minecraft.world.entity.MobCategory.MONSTER,
                net.minecraft.world.entity.MobCategory.CREATURE, net.minecraft.world.entity.MobCategory.WATER_CREATURE,
                net.minecraft.world.entity.MobCategory.WATER_AMBIENT};
        Map<String, int[]> tally = new TreeMap<>();
        var random = march.getRandom();
        long dayTime = march.getDayTime();
        for (long time : new long[]{6000, 18000}) {
            march.setDayTime(time);
            for (int site = 0; site < SITES.length; site++) {
                BlockPos origin = origins.get(site);
                for (int i = 0; i < 600; i++) {
                    int x = origin.getX() * 16 + random.nextInt(SIZE), z = origin.getZ() * 16 + random.nextInt(SIZE);
                    int surface = march.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos land = new BlockPos(x, surface, z);
                    boolean wet = march.getFluidState(land.below()).is(net.minecraft.tags.FluidTags.WATER);
                    BlockPos water = wet ? land.below() : null;
                    var biome = march.getBiome(land);
                    String biomeName = biome.unwrapKey().map(k -> k.location().getPath()).orElse("?");
                    if (!biomeName.equals(SITES[site])) continue;
                    for (var category : categories) {
                        boolean aquatic = category == net.minecraft.world.entity.MobCategory.WATER_CREATURE
                                || category == net.minecraft.world.entity.MobCategory.WATER_AMBIENT;
                        BlockPos pos = aquatic ? water : land;
                        for (var entry : biome.value().getMobSettings().getMobs(category).unwrap()) {
                            String key = biomeName + " " + net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entry.type);
                            int[] counts = tally.computeIfAbsent(key, k -> new int[2]);
                            if (pos == null) continue;
                            counts[1]++;
                            if (net.minecraft.world.entity.SpawnPlacements.isSpawnPositionOk(entry.type, march, pos)
                                    && net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(entry.type, march,
                                    net.minecraft.world.entity.MobSpawnType.NATURAL, pos, random)) counts[0]++;
                        }
                    }
                }
            }
        }
        march.setDayTime(dayTime);
        boolean ok = true;
        for (var entry : tally.entrySet()) {
            int spawnable = entry.getValue()[0], sampled = entry.getValue()[1];
            // Now that no biome swallows the map, a site window can hold only a handful of its own
            // biome. One sampled spot says nothing about whether a mob can live there, so too small a
            // sample is reported and passed over rather than failed on.
            if (sampled < MIN_SPAWN_SAMPLE) {
                TribalPower.LOGGER.info("March survey spawn THIN {}: only {} spots sampled, not judged",
                        entry.getKey(), sampled);
                continue;
            }
            boolean pass = spawnable > 0;
            ok &= pass;
            TribalPower.LOGGER.info("March survey spawn {} {}: {} of {} spots", pass ? "PASS" : "FAIL", entry.getKey(),
                    spawnable, sampled);
        }
        return ok;
    }

    /**
     * Grows each of the March's own trees on open ground far from the surveyed sites and holds it to its
     * promise: real size, no leaf left too far from wood (it would only decay), and strands on the willow.
     */
    /** Takes down whatever tree stands on a planting site, leaving the ground under it untouched. */
    private static void clearGrowth(ServerLevel march, int x, int z, int span) {
        int standing = march.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        for (int dx = -span; dx <= span; dx++) for (int dz = -span; dz <= span; dz++) {
            for (int dy = standing + 4; dy > march.getMinBuildHeight(); dy--) {
                BlockPos at = new BlockPos(x + dx, dy, z + dz);
                var state = march.getBlockState(at);
                if (state.isAir()) continue;
                if (!isGrowth(state)) break;          // the first solid thing that is not a tree is the ground
                march.setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    private static boolean isGrowth(net.minecraft.world.level.block.state.BlockState state) {
        if (state.is(net.minecraft.tags.BlockTags.LOGS) || state.is(net.minecraft.tags.BlockTags.LEAVES)) return true;
        var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null && (id.getPath().contains("strand") || id.getPath().contains("vine"));
    }

    private static boolean trees(ServerLevel march) {
        var features = march.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        boolean ok = true;
        int slot = 0;
        for (var shape : tk.darrow.tribalpower.world.MarchTreeFeature.Shape.values()) {
            var id = ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "march_" + shape.getSerializedName());
            var feature = features.get(id);
            if (feature == null) { TribalPower.LOGGER.error("March survey tree FAIL {}: not registered", id); ok = false; continue; }
            int x = 30000 + slot++ * 128, z = 30000;
            int span = shape == tk.darrow.tribalpower.world.MarchTreeFeature.Shape.WEEPING_COLOSSUS ? 56 : 24;
            march.getChunk(x >> 4, z >> 4);
            // The survey world is kept between runs, so last run's tree is still standing here.
            // Measuring the ground first put it on top of that trunk, so every run planted higher than
            // the last until the tree had no room and reported nothing at all. Clear the old growth,
            // and only the growth: blanking a column to sea level would quarry the hillside away.
            clearGrowth(march, x, z, span);
            int y = march.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            var ground = march.getBlockState(new BlockPos(x, y - 1, z));
            // Give every tree the same footing: a pad of March grass with open air above.
            for (int dx = -span; dx <= span; dx++) for (int dz = -span; dz <= span; dz++) {
                march.setBlock(new BlockPos(x + dx, y - 1, z + dz), tk.darrow.tribalpower.block.ModBlocks.MARCH_GRASS.get().defaultBlockState(), 2);
                for (int dy = 0; dy < 200; dy++) march.setBlock(new BlockPos(x + dx, y + dy, z + dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            }
            long started = System.nanoTime();
            boolean placed = false;
            for (int attempt = 0; attempt < 4 && !placed; attempt++)
                placed = feature.place(march, march.getChunkSource().getGenerator(), march.getRandom(), new BlockPos(x, y, z));
            long ms = (System.nanoTime() - started) / 1_000_000;
            int logs = 0, leaves = 0, strands = 0, decaying = 0, top = y;
            var cursor = new BlockPos.MutableBlockPos();
            for (int dx = -span; dx <= span; dx++) for (int dz = -span; dz <= span; dz++) for (int dy = -14; dy < 200; dy++) {
                BlockState at = march.getBlockState(cursor.set(x + dx, y + dy, z + dz));
                if (at.is(net.minecraft.tags.BlockTags.LOGS)) { logs++; top = Math.max(top, y + dy); }
                else if (at.getBlock() instanceof LeavesBlock) {
                    leaves++;
                    top = Math.max(top, y + dy);
                    if (at.getValue(LeavesBlock.DISTANCE) >= 7 && !at.getValue(LeavesBlock.PERSISTENT)) decaying++;
                } else if (at.getBlock() instanceof tk.darrow.tribalpower.block.WillowStrandBlock) strands++;
            }
            int tall = top - y;
            int minTall = switch (shape) { case WEEPING_COLOSSUS -> 80; case YOUNG_WILLOW -> 8; case FROSTPINE -> 14; case BELLCAP -> 10; case HEARTHOAK, STRIDER -> 8; case CINDER_SNAG -> 5; };
            boolean pass = placed && decaying == 0 && tall >= minTall && logs > 0
                    && (shape == tk.darrow.tribalpower.world.MarchTreeFeature.Shape.CINDER_SNAG || leaves > 20)
                    && (shape != tk.darrow.tribalpower.world.MarchTreeFeature.Shape.WEEPING_COLOSSUS || strands > 100);
            ok &= pass;
            TribalPower.LOGGER.info("March survey tree {} {}: {} tall, {} logs, {} leaves, {} strands, {} decaying, {} ms (ground was {})",
                    pass ? "PASS" : "FAIL", shape.getSerializedName(), tall, logs, leaves, strands, decaying, ms,
                    ground.getBlock().builtInRegistryHolder().key().location().getPath());
        }
        return ok;
    }

    /**
     * Every structure a manifest lists must be findable in the March within a few thousand blocks, and its
     * start chunk (with the ring around it) must generate cleanly.
     */
    private static boolean structures(ServerLevel march, List<BlockPos> origins) throws java.io.IOException {
        var resources = march.getServer().getResourceManager();
        var registry = march.registryAccess().registryOrThrow(Registries.STRUCTURE);
        boolean ok = true;
        for (String group : new String[]{"snow_ember", "steppe_highlands", "crystal_fen", "underground", "trees"}) {
            var file = ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "structure/manifest_" + group + ".json");
            var json = group.equals("trees") ? com.google.gson.JsonParser.parseString("[\"willow_grove\"]").getAsJsonArray()
                    : com.google.gson.JsonParser.parseReader(resources.openAsReader(file)).getAsJsonArray();
            for (var element : json) {
                var id = ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, element.getAsString());
                var holder = registry.getHolder(ResourceKey.create(Registries.STRUCTURE, id)).orElse(null);
                if (holder == null) { TribalPower.LOGGER.error("March survey structure FAIL {}: not registered", id); ok = false; continue; }
                long started = System.nanoTime();
                // The search radius counts placement cells, not chunks: size it from the set's spacing so it means
                // "within 2560 blocks" for every structure.
                int spacing = march.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).stream()
                        .filter(set -> set.structures().stream().anyMatch(entry -> entry.structure().equals(holder)))
                        .map(set -> set.placement() instanceof net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement spread
                                ? spread.spacing() : 16)
                        .findFirst().orElse(16);
                // Search from the surveyed site of the structure's own land: a rare biome may lie far from spawn.
                BlockPos from = BlockPos.ZERO;
                var biomes = march.registryAccess().registryOrThrow(Registries.BIOME).getTag(net.minecraft.tags.TagKey.create(Registries.BIOME,
                        ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "has_structure/" + id.getPath())));
                if (biomes.isPresent() && biomes.get().size() > 0) {
                    String home = biomes.get().get(0).unwrapKey().orElseThrow().location().getPath();
                    int site = java.util.Arrays.asList(SITES).indexOf(home);
                    if (site >= 0) from = new BlockPos(origins.get(site).getX() * 16 + 64, 0, origins.get(site).getZ() * 16 + 64);
                }
                var found = march.getChunkSource().getGenerator().findNearestMapStructure(march,
                        net.minecraft.core.HolderSet.direct(holder), from, Math.max(2, 2560 / (spacing * 16) + 1), false);
                if (found == null) { TribalPower.LOGGER.error("March survey structure FAIL {}: none within 2560 blocks of its land", id); ok = false; continue; }
                BlockPos at = found.getFirst();
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) march.getChunk((at.getX() >> 4) + dx, (at.getZ() >> 4) + dz);
                var start = march.structureManager().getStartForStructure(net.minecraft.core.SectionPos.of(at), holder.value(), march.getChunk(at));
                boolean placed = start.isValid();
                ok &= placed;
                TribalPower.LOGGER.info("March survey structure {} {} at {} ({} blocks out, {} ms)", placed ? "PASS" : "FAIL", id.getPath(),
                        at.toShortString(), (int) Math.sqrt(at.distSqr(from)), (System.nanoTime() - started) / 1_000_000);
            }
        }
        return ok;
    }

    /** Ores, amethyst and cave volume across every surveyed site, below the surface. */
    private static boolean underground(ServerLevel march, List<BlockPos> origins) {
        Map<String, Integer> finds = new TreeMap<>();
        long cave = 0, rock = 0;
        var cursor = new BlockPos.MutableBlockPos();
        for (BlockPos origin : origins) for (int cz = 0; cz < SITE_CHUNKS; cz++) for (int cx = 0; cx < SITE_CHUNKS; cx++) {
            var chunk = march.getChunk(origin.getX() + cx, origin.getZ() + cz);
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                int wx = chunk.getPos().getMinBlockX() + x, wz = chunk.getPos().getMinBlockZ() + z;
                int surface = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
                int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                for (int y = march.getMinBuildHeight() + 6; y <= top + 1; y++) {
                    BlockState state = chunk.getBlockState(cursor.set(wx, y, wz));
                    String decor = state.getBlock().builtInRegistryHolder().key().location().getPath();
                    if (DECOR.contains(decor)) finds.merge(decor, 1, Integer::sum);
                    if (y >= surface - 6) continue;
                    if (state.isAir()) cave++;
                    else rock++;
                    String id = state.getBlock().builtInRegistryHolder().key().location().getPath();
                    if (id.endsWith("_ore") && !id.equals("march_ore")) finds.merge(id.replace("deepslate_", ""), 1, Integer::sum);
                    else if (id.contains("amethyst")) finds.merge(id, 1, Integer::sum);
                }
            }
        }
        double open = 100.0 * cave / Math.max(1, cave + rock);
        TribalPower.LOGGER.info("March survey underground: {}% open cave, finds {}", String.format("%.1f", open), finds);
        boolean ok = open >= 6 && finds.getOrDefault("amethyst_block", 0) > 0;
        for (String decor : DECOR) {
            if (finds.getOrDefault(decor, 0) == 0) TribalPower.LOGGER.error("March survey decoration missing: {}", decor);
            ok &= finds.getOrDefault(decor, 0) > 0;
        }
        for (String ore : new String[]{"coal", "iron", "copper", "gold", "redstone", "lapis", "diamond", "emerald"})
            ok &= finds.getOrDefault("march_" + ore + "_ore", 0) > 0;
        TribalPower.LOGGER.info("March survey underground {}", ok ? "PASS" : "FAIL");
        return ok;
    }

    /** Nearest spot to the origin that is dry land and sits in the wanted biome at the surface. */
    private static BlockPos landSite(ServerLevel march, String site) {
        var key = ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, site));
        var generator = march.getChunkSource().getGenerator();
        var random = march.getChunkSource().randomState();
        for (int ring = 0; ring <= 40; ring++) for (int gx = -ring; gx <= ring; gx++) for (int gz = -ring; gz <= ring; gz++) {
            if (Math.max(Math.abs(gx), Math.abs(gz)) != ring) continue;
            int x = gx * 160, z = gz * 160;
            int height = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, march, random);
            if (height <= seaLevel(march) + 2) continue;
            var source = generator.getBiomeSource();
            if (source.getNoiseBiome(x >> 2, height >> 2, z >> 2, random.sampler()).is(key))
                return new BlockPos((x >> 4) - SITE_CHUNKS / 2, 0, (z >> 4) - SITE_CHUNKS / 2);
        }
        throw new IllegalStateException("The March has no dry " + site + " within 6400 blocks");
    }

    private static void survey(ServerLevel march, BlockPos origin, int site, int cx, int cz, BufferedImage image,
                               long[] totals, Map<String, Integer> tops) {
        var chunk = march.getChunk(origin.getX() + cx, origin.getZ() + cz);
        var cursor = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int wx = chunk.getPos().getMinBlockX() + x, wz = chunk.getPos().getMinBlockZ() + z;
            int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            int ground = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z); // the floor block itself
            boolean canopy = false;
            for (int y = top + 1; y >= Math.max(ground - 1, march.getMinBuildHeight()); y--) {
                BlockState state = chunk.getBlockState(cursor.set(wx, y, wz));
                if (state.getBlock() instanceof BushBlock) {
                    totals[0]++;
                    PLANTS.merge(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), 1, Integer::sum);
                }
                if (state.getLightEmission(march, cursor) > 0) totals[1]++;
                canopy |= state.getBlock() instanceof LeavesBlock;
            }
            BlockState surface = chunk.getBlockState(cursor.set(wx, ground, wz));
            boolean water = !chunk.getFluidState(wx, ground + 1, wz).isEmpty();
            tops.merge(water ? "water" : surface.getBlock().builtInRegistryHolder().key().location().getPath(), 1, Integer::sum);
            int colour = water ? 0x2C5A8C : canopy ? 0x1F6B5C : surface.getMapColor(march, cursor).col;
            float shade = Math.clamp(0.55F + (ground - 60) / 110F, 0.35F, 1.25F);
            int px = site * SIZE + cx * 16 + x, pz = cz * 16 + z;
            image.setRGB(px, pz, scale(colour, shade));
            // Lower panel: height in grey, terrace floors (top block at 55 + 12k) picked out in red.
            int grey = Math.clamp((ground - 30) * 2, 0, 255);
            image.setRGB(px, SIZE + pz, ground >= 55 && ground % 12 == 7 ? 0xFF0000 | grey << 8 | grey : grey << 16 | grey << 8 | grey);
        }
    }

    private static int scale(int rgb, float by) {
        int r = Math.min(255, (int) ((rgb >> 16 & 255) * by)), g = Math.min(255, (int) ((rgb >> 8 & 255) * by));
        return r << 16 | g << 8 | Math.min(255, (int) ((rgb & 255) * by));
    }
}
