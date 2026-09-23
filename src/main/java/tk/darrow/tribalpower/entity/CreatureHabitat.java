package tk.darrow.tribalpower.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
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
        if (!level.getFluidState(pos).isEmpty() || !level.getBlockState(pos).isAir()) return false;
        if (!level.getBlockState(pos.above()).isAir()) return false;
        for (int drop = 1; drop <= 6; drop++)
            if (!level.getBlockState(pos.below(drop)).isAir())
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
                || below.is(Blocks.TUFF) || below.is(Blocks.GRAVEL);
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
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -4; dx <= 4; dx++)
            for (int dy = -3; dy <= 2; dy++)
                for (int dz = -4; dz <= 4; dz++) {
                    cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (level.getFluidState(cursor).is(FluidTags.LAVA)) return true;
                }
        return false;
    }

    /** Swimming room. */
    public static boolean water(LevelAccessor level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER)
                && level.getFluidState(pos.above()).is(FluidTags.WATER);
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
        boolean march = level.getLevel().dimension().equals(tk.darrow.tribalpower.world.ModDimensions.THE_MARCH);
        if (!Monster.isDarkEnoughToSpawn(level, pos, random)) {
            // The March is the spirits' country: they walk it by day too, a third as often, but never
            // where torchlight or a lantern falls, so a lit camp stays safe.
            if (!march || level.getBrightness(LightLayer.BLOCK, pos) > 0 || random.nextInt(3) != 0) return false;
        }
        if (this == AIR) return air(level, pos);
        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        return MarchSpawns.turf(below) || MarchSpawns.wispFooting(below)
                || below.isValidSpawn(level, belowPos, type);
    }

    /** A creature that lives in lava has no business burning in it. */
    public boolean needsFireImmunity() {
        return this == LAVA;
    }

    public static MobSpawnType naturalOnly() {
        return MobSpawnType.NATURAL;
    }
}
