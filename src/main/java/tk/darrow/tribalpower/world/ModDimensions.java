package tk.darrow.tribalpower.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.storage.DeepCacheManager;

public final class ModDimensions {
    public static final ResourceKey<Level> THE_MARCH = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "the_march")
    );

    private ModDimensions() {}

    private static final String RETURN_DIMENSION = "TribalGateReturnDimension";
    private static final String RETURN_POS = "TribalGateReturn";

    /**
     * Gate Drum travel: Overworld ↔ The March with a cleared landing pad.
     * The outbound leg is conservative; the return leg always resolves somewhere safe, because a
     * drum struck in the March must never strand the player there.
     */
    public static boolean travelThroughGate(ServerPlayer player) {
        if (player.isPassenger()) return false;
        ServerLevel current = player.serverLevel();
        boolean returning = current.dimension().equals(THE_MARCH);
        ServerLevel target = returning ? returnLevel(player) : player.server.getLevel(THE_MARCH);
        if (target == null) {
            TribalPower.LOGGER.warn("Gate Drum target dimension is not loaded");
            return false;
        }

        BlockPos feet = returning ? resolveReturn(target, player) : resolveArrival(target, player.blockPosition());
        if (feet == null) return false;
        BlockPos origin = player.blockPosition();

        if (player.changeDimension(new DimensionTransition(
                target,
                new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5),
                Vec3.ZERO,
                player.getYRot(),
                player.getXRot(),
                DimensionTransition.DO_NOTHING
        )) == null) return false;
        player.fallDistance = 0;
        if (!returning) {
            var memory = gateMemory(player);
            memory.putString(RETURN_DIMENSION, current.dimension().location().toString());
            memory.putLong(RETURN_POS, origin.asLong());
            DeepCacheManager.markVisited(player);
        }
        return true;
    }

    /** Return memory survives death and respawn, unlike the root of the persistent data. */
    private static net.minecraft.nbt.CompoundTag gateMemory(ServerPlayer player) {
        var root = player.getPersistentData();
        var persisted = root.getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        root.put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, persisted);
        // Adopt a return point written by 3.4.3 and earlier.
        if (!persisted.contains(RETURN_POS) && root.contains(RETURN_POS)) {
            persisted.putLong(RETURN_POS, root.getLong(RETURN_POS));
            persisted.putString(RETURN_DIMENSION, root.getString(RETURN_DIMENSION));
        }
        return persisted;
    }

    private static ServerLevel returnLevel(ServerPlayer player) {
        String stored = gateMemory(player).getString(RETURN_DIMENSION);
        var id = stored.isEmpty() ? null : ResourceLocation.tryParse(stored);
        ServerLevel level = id == null ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        // A missing or unloaded home (no memory, removed dimension mod) still leads out of the March.
        return level == null || level.dimension().equals(THE_MARCH) ? player.server.overworld() : level;
    }

    /** Surface landing on a cleared pad in the column of {@code from}; null when that column is unsafe. */
    static BlockPos resolveArrival(ServerLevel target, BlockPos from) {
        int x = from.getX();
        int z = from.getZ();
        if (!target.getWorldBorder().isWithinBounds(new BlockPos(x, target.getMinBuildHeight(), z))) return null;
        // Force destination chunk generation before heightmap / pad work.
        target.getChunk(x >> 4, z >> 4);
        return padAt(target, x, z);
    }

    private static BlockPos resolveReturn(ServerLevel target, ServerPlayer player) {
        var memory = gateMemory(player);
        boolean remembers = memory.contains(RETURN_POS)
                && target.dimension().location().toString().equals(memory.getString(RETURN_DIMENSION));
        BlockPos remembered = remembers ? BlockPos.of(memory.getLong(RETURN_POS)) : null;
        if (remembered != null && TravelSafety.withinBounds(target, remembered)) {
            target.getChunk(remembered.getX() >> 4, remembered.getZ() >> 4);
            // Beside the home drum first, touching nothing: bases have campfires, slabs and wiring.
            BlockPos stand = TravelSafety.nearestStand(target, remembered, 6, 4);
            if (stand != null) return stand;
            BlockPos pad = padAt(target, remembered.getX(), remembered.getZ());
            if (pad != null) return pad;
        }
        BlockPos spawn = player.getRespawnDimension().equals(target.dimension()) && player.getRespawnPosition() != null
                ? player.getRespawnPosition() : target.getSharedSpawnPos();
        target.getChunk(spawn.getX() >> 4, spawn.getZ() >> 4);
        BlockPos stand = TravelSafety.nearestStand(target, spawn, 8, 6);
        if (stand != null) return stand;
        BlockPos pad = padAt(target, spawn.getX(), spawn.getZ());
        if (pad != null) return pad;
        // Last resort: the spawn column itself, on a fabricated pad.
        BlockPos ground = new BlockPos(spawn.getX(), findSafeSurfaceY(target, spawn.getX(), spawn.getZ()), spawn.getZ());
        ensureLandingPad(target, ground);
        return ground.above();
    }

    /** Surface landing with a cleared pad; null when the column is unsafe. Returns the feet position. */
    private static BlockPos padAt(ServerLevel target, int x, int z) {
        BlockPos ground = new BlockPos(x, findSafeSurfaceY(target, x, z), z);
        if (!TravelSafety.withinBounds(target, ground.above()) || TravelSafety.hasHazard(target, ground.above())
                || !target.getBlockState(ground.above()).getCollisionShape(target, ground.above()).isEmpty()
                || !target.getBlockState(ground.above(2)).getCollisionShape(target, ground.above(2)).isEmpty()) return null;
        ensureLandingPad(target, ground);
        if (!target.getBlockState(ground).isFaceSturdy(target, ground, net.minecraft.core.Direction.UP)
                || !target.getFluidState(ground.above()).isEmpty() || !target.getFluidState(ground.above(2)).isEmpty()) return null;
        return ground.above();
    }
    private static int findSafeSurfaceY(ServerLevel level, int x, int z) {
        int min = level.getMinBuildHeight() + 1;
        int max = level.getMaxBuildHeight() - 3;
        int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (height < min) {
            height = Math.max(64, level.getSeaLevel());
        }
        height = Math.min(Math.max(height, min), max);

        Integer dry = scanForStand(level, x, z, height, min, true);
        if (dry != null) {
            return dry;
        }
        Integer any = scanForStand(level, x, z, height, min, false);
        if (any != null) {
            return any;
        }
        // Fabricate a pad at the heightmap / sea level — ensureLandingPad fills solids.
        return Math.min(Math.max(height - 1, level.getSeaLevel()), max);
    }

    private static Integer scanForStand(ServerLevel level, int x, int z, int fromY, int minY, boolean requireDry) {
        for (int y = fromY; y >= minY; y--) {
            BlockPos ground = new BlockPos(x, y, z);
            BlockPos feet = ground.above();
            BlockPos head = feet.above();
            BlockState groundState = level.getBlockState(ground);
            BlockState feetState = level.getBlockState(feet);
            BlockState headState = level.getBlockState(head);
            if (!groundState.blocksMotion()) {
                continue;
            }
            if (feetState.blocksMotion() || headState.blocksMotion()) {
                continue;
            }
            if (requireDry && (!feetState.getFluidState().isEmpty() || !headState.getFluidState().isEmpty())) {
                continue;
            }
            return y;
        }
        return null;
    }

    private static void ensureLandingPad(ServerLevel level, BlockPos groundCenter) {
        boolean inMarch = level.dimension().equals(THE_MARCH);
        BlockState pad = inMarch
                ? ModBlocks.MARCH_GRASS.get().defaultBlockState()
                : Blocks.STONE.defaultBlockState();
        BlockState fill = inMarch
                ? ModBlocks.MARCH_SOIL.get().defaultBlockState()
                : Blocks.DIRT.defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos ground = groundCenter.offset(dx, 0, dz);
                BlockPos under = ground.below();
                BlockPos feet = ground.above();
                BlockPos head = feet.above();
                if (level.getBlockState(under).isAir()) {
                    level.setBlockAndUpdate(under, fill);
                }
                if (level.getBlockState(ground).isAir()) {
                    level.setBlockAndUpdate(ground, pad);
                }
                clearIfBlocking(level, feet);
                clearIfBlocking(level, head);
            }
        }
    }

    private static void clearIfBlocking(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.isAir() && state.canBeReplaced() && state.getFluidState().isEmpty() && level.getBlockEntity(pos) == null) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getTo().equals(THE_MARCH)) {
            DeepCacheManager.markVisited(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        // Hook reserved for March ambient effects / spirit link maintenance.
    }
}
