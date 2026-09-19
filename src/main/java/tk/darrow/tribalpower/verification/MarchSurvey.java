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
    private static final double MAX_PLANTS_PER_CHUNK = 70, MAX_LIGHTS_PER_CHUNK = 8, MIN_PLANTS_PER_CHUNK = 4;
    private static final String[] SITES = {"march_steppe", "march_crystal_fields", "march_highlands", "march_reed_fen",
            "march_snow_fields", "march_ember_wastes"};

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
            // The Ember Wastes are meant to be nearly bare.
            boolean pass = plants <= MAX_PLANTS_PER_CHUNK && lights <= MAX_LIGHTS_PER_CHUNK
                    && (plants >= MIN_PLANTS_PER_CHUNK || SITES[site].equals("march_ember_wastes"));
            ok &= pass;
            TribalPower.LOGGER.info("March survey {} {}: {} plants/chunk, {} lights/chunk, {} ms/chunk, surface {}",
                    pass ? "PASS" : "FAIL", SITES[site], String.format("%.1f", plants), String.format("%.1f", lights),
                    (System.nanoTime() - started) / 1_000_000 / chunks, tops);
        }
        ImageIO.write(image, "png", new File("march_survey.png"));
        TribalPower.LOGGER.info("March survey plants: {}", PLANTS);
        return ok & underground(march, origins);
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
                for (int y = march.getMinBuildHeight() + 6; y < surface - 6; y++) {
                    BlockState state = chunk.getBlockState(cursor.set(wx, y, wz));
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
            if (height <= march.getSeaLevel() + 2) continue;
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
