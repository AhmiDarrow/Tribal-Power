package tk.darrow.tribalpower.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

/**
 * Where a creature is allowed to appear.
 *
 * <p>Every March creature used to share one rule — turf underfoot, daylight, no fluid — which is the
 * right rule for a grazer and wrong for everything else. A cave dweller could never spawn because
 * caves are dark, and a lava dweller could never spawn because lava is a fluid and magma is not turf.
 * Both were registered, spawned in no biome, and nothing reported it.
 *
 * <p>Each habitat carries its own footing and light test, and {@link CreatureEntities#placements} hands
 * the matching one to Minecraft per creature.
 */
public enum CreatureHabitat {
    /** Open ground in daylight: the grazers. */
    GROUND,
    /** Anywhere with room to fly and sky above it. */
    AIR,
    /** Underground and in the dark, on stone rather than turf. */
    CAVE,
    /** Beside lava or on the hot stone around it. Only fire-immune creatures belong here. */
    LAVA,
    /** In water deep enough to swim. */
    WATER;

    /** Which creatures live where. Anything unlisted grazes. */
    private static final Map<String, CreatureHabitat> BY_ID = Map.ofEntries(
            Map.entry("glimmer_moth", AIR), Map.entry("dust_flitter", AIR),
            Map.entry("veil_drifter", AIR), Map.entry("storm_moth", AIR),
            Map.entry("mourning_bell", AIR), Map.entry("stone_grub", CAVE),
            Map.entry("gloom_crawler", CAVE), Map.entry("deep_lurker", CAVE),
            Map.entry("pale_stalker", CAVE), Map.entry("ember_drifter", LAVA),
            Map.entry("magma_creeper", LAVA), Map.entry("palewing", AIR),
            Map.entry("burrow_gnasher", CAVE), Map.entry("updrifter", AIR), Map.entry("shardmoth", AIR),
            Map.entry("glowgrub", CAVE), Map.entry("facet_stalker", CAVE), Map.entry("snowveil", AIR),
            Map.entry("cinderhide", LAVA), Map.entry("ashmoth", AIR), Map.entry("slaglump", LAVA),
            Map.entry("bog_floater", AIR), Map.entry("glass_flitter", AIR),
            Map.entry("geode_grub", CAVE), Map.entry("silt_glider", WATER),
            Map.entry("pale_drifter", WATER), Map.entry("shoal_darter", WATER),
            Map.entry("brine_lurker", WATER), Map.entry("slag_troll", LAVA),
            Map.entry("tunnel_goblin", CAVE), Map.entry("ridge_kobold", CAVE),
            Map.entry("geode_kobold", CAVE), Map.entry("glimmer_fay", AIR), Map.entry("prism_fay", AIR),
            Map.entry("marsh_fay", AIR), Map.entry("barrow_shade", CAVE), Map.entry("pale_wraith", AIR),
            Map.entry("cinder_shade", AIR), Map.entry("drowned_shade", WATER));

    public static CreatureHabitat of(CreatureProfile profile) {
        return BY_ID.getOrDefault(profile.id, GROUND);
    }

    public static CreatureHabitat of(String id) {
        return BY_ID.getOrDefault(id, GROUND);
    }

    // ---- the rules ---------------------------------------------------------------------------------

    /** Grazing footing: turf, and light to see by. */
    public static boolean ground(LevelAccessor level, BlockPos pos) {
        return MarchSpawns.turf(level.getBlockState(pos.below()))
                && level.getRawBrightness(pos, 0) > 8
                && level.getFluidState(pos).isEmpty();
    }

    /** Room to fly: open air with something solid a little way below, and sky overhead. */
    public static boolean air(LevelAccessor level, BlockPos pos) {
        // grass and flowers are open air to a moth: a meadow would otherwise have nowhere for one to rise
        BlockState here = level.getBlockState(pos), over = level.getBlockState(pos.above());
        if (!level.getFluidState(pos).isEmpty() || !(here.isAir() || here.canBeReplaced())) return false;
        if (!(over.isAir() || over.canBeReplaced())) return false;
        BlockPos.MutableBlockPos under = pos.mutable();
        for (int drop = 1; drop <= 6; drop++)
            if (!level.getBlockState(under.move(Direction.DOWN)).isAir())
                return level.getRawBrightness(pos, 0) > 6;
        return false;
    }

    /** Dark, dry, and standing on stone rather than grass. */
    public static boolean cave(LevelAccessor level, BlockPos pos) {
        if (!level.getFluidState(pos).isEmpty()) return false;
        if (level.getBrightness(LightLayer.SKY, pos) > 2) return false;      // not under open sky
        if (level.getMaxLocalRawBrightness(pos) > 7) return false;           // and not near a torch
        BlockState below = level.getBlockState(pos.below());
        return below.is(BlockTags.BASE_STONE_OVERWORLD) || below.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                || below.is(tk.darrow.tribalpower.block.ModBlocks.MARCH_STONE.get())
                || below.is(tk.darrow.tribalpower.block.ModBlocks.MARCH_COBBLE.get())
                || below.is(tk.darrow.tribalpower.block.ModBlocks.MOONSTONE.get())
                || below.is(tk.darrow.tribalpower.block.ModBlocks.MOSS_AGATE.get())
                || below.is(Blocks.TUFF) || below.is(Blocks.GRAVEL)
                || below.is(tk.darrow.tribalpower.block.MarchTags.CAVE_FOOTING);
    }

    /** Hot stone, with lava close enough to matter. */
    public static boolean lava(LevelAccessor level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) return false;
        BlockState below = level.getBlockState(pos.below());
        boolean footing = below.is(Blocks.MAGMA_BLOCK) || below.is(Blocks.BASALT) || below.is(Blocks.SMOOTH_BASALT)
                || below.is(Blocks.BLACKSTONE) || below.is(Blocks.OBSIDIAN) || below.is(BlockTags.BASE_STONE_OVERWORLD)
                || below.is(tk.darrow.tribalpower.block.ModBlocks.MARCH_STONE.get());
        if (!footing) return false;
        // Lava within a few blocks, so these gather at the edges rather than anywhere underground.
        if (noLavaInSections(level, pos.getX() - 4, pos.getY() - 3, pos.getZ() - 4, pos.getX() + 4, pos.getY() + 2, pos.getZ() + 4)) return false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -4; dx <= 4; dx++)
            for (int dy = -3; dy <= 2; dy++)
                for (int dz = -4; dz <= 4; dz++) {
                    cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (level.getFluidState(cursor).is(FluidTags.LAVA)) return true;
                }
        return false;
    }

    private static final java.util.function.Predicate<BlockState> HOLDS_LAVA = state -> state.getFluidState().is(FluidTags.LAVA);

    /**
     * Whether the chunk sections around a box hold no lava at all, read from their palettes. Base stone counts as hot
     * footing, so nearly every cave floor asks for the 486-block lava search above on every spawn try, and nearly
     * all of them have none in reach. False (search the blocks) whenever it cannot tell: off the server level (a
     * world-generation region), or with a chunk around the box not loaded. A palette can list a state no block uses
     * any more, so this may send a lava-free spot to the search, but never turns away one with lava in reach.
     */
    private static boolean noLavaInSections(LevelAccessor level, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)) return false;
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++)
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                // never loads or waits: a chunk not already loaded and full is left to the block search, as before
                var chunk = server.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) return false;
                for (int sy = minY >> 4; sy <= maxY >> 4; sy++) {
                    int index = chunk.getSectionIndexFromSectionY(sy);
                    if (index < 0 || index >= chunk.getSectionsCount()) continue;   // outside the world: no fluid there
                    if (chunk.getSection(index).maybeHas(HOLDS_LAVA)) return false;
                }
            }
        return true;
    }

    /** Three deep or more above this block: a bed the sky does not reach into, where the drowned rise by day. */
    public static boolean deep(LevelAccessor level, BlockPos pos) {
        return level.getFluidState(pos.above()).is(FluidTags.WATER) && level.getFluidState(pos.above(2)).is(FluidTags.WATER);
    }

    /** Water the sky does not look straight into: iced over, lidded by mud or a lily, or under a canopy of leaves and limbs. */
    public static boolean shaded(LevelAccessor level, BlockPos pos) {
        BlockPos.MutableBlockPos lidPos = pos.mutable().move(Direction.UP);
        while (level.getFluidState(lidPos).is(FluidTags.WATER)) lidPos.move(Direction.UP);
        BlockState lid = level.getBlockState(lidPos);
        if (!lid.isAir() && !(lid.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock)) return true;
        return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) > lidPos.getY();
    }

    /** Swimming room. */
    public static boolean water(LevelAccessor level, BlockPos pos) {
        // two deep, counted either way: the spawner's heightmap lands on the top block of a lake or a shore
        if (!level.getFluidState(pos).is(FluidTags.WATER)) return false;
        if (level.getFluidState(pos.above()).is(FluidTags.WATER) || level.getFluidState(pos.below()).is(FluidTags.WATER)
                || level.getBlockState(pos.above()).is(net.minecraft.tags.BlockTags.ICE)) return true;   // a mere frozen over kept its depth under the lid
        // a shallow shelf along a shore is a body of water too; a lone puddle is not
        return (level.getFluidState(pos.north()).is(FluidTags.WATER) && level.getFluidState(pos.south()).is(FluidTags.WATER))
                || (level.getFluidState(pos.east()).is(FluidTags.WATER) && level.getFluidState(pos.west()).is(FluidTags.WATER));
    }

    /** The check Minecraft is handed for a passive creature of this habitat. */
    public boolean allowsAnimal(LevelAccessor level, BlockPos pos) {
        return switch (this) {
            case GROUND -> ground(level, pos);
            case AIR -> air(level, pos);
            case CAVE -> cave(level, pos);
            case LAVA -> lava(level, pos);
            case WATER -> water(level, pos);
        };
    }

    /** The same, for a hostile: it must also be dark enough for a monster to walk. */
    public boolean allowsMonster(ServerLevelAccessor level, BlockPos pos, RandomSource random,
                                 EntityType<? extends Mob> type) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
        if (this == CAVE) return cave(level, pos);          // caves are dark by definition
        if (this == LAVA) return lava(level, pos);
        // deep water is dark on its own: a few blocks down the sky no longer reaches, and the drowned rise by day
        if (this == WATER) return water(level, pos)
                && (level.getBrightness(LightLayer.SKY, pos) < 12 || deep(level, pos) || shaded(level, pos) || MarchSpawns.spiritsRise(level, pos, random));
        if (!MarchSpawns.spiritsRise(level, pos, random)) return false;
        if (this == AIR) return air(level, pos);
        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        return MarchSpawns.wispFooting(below) || below.isValidSpawn(level, belowPos, type);
    }
}
