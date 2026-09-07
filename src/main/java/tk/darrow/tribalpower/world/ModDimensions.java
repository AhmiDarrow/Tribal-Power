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

    /**
     * Gate Drum travel: Overworld ↔ The March with a cleared landing pad.
     * Landing resolves the motion-blocking surface so noise hills/valleys stay safe.
     */
    public static boolean travelThroughGate(ServerPlayer player) {
        if (player.isPassenger()) return false;
        ServerLevel current = player.serverLevel();
        boolean returning = current.dimension().equals(THE_MARCH);
        var memory = player.getPersistentData();
        var returnId = ResourceLocation.tryParse(memory.getString("TribalGateReturnDimension"));
        ResourceKey<Level> targetKey = returning
                ? (returnId == null ? Level.OVERWORLD : ResourceKey.create(Registries.DIMENSION, returnId)) : THE_MARCH;
        ServerLevel target = player.server.getLevel(targetKey);
        if (target == null) {
            TribalPower.LOGGER.warn("Dimension {} is not loaded", targetKey.location());
            return false;
        }

        int x = player.blockPosition().getX();
        int z = player.blockPosition().getZ();
        BlockPos remembered = returning && memory.contains("TribalGateReturn") ? BlockPos.of(memory.getLong("TribalGateReturn")) : null;
        if (remembered != null) { x = remembered.getX(); z = remembered.getZ(); }
        if (!target.getWorldBorder().isWithinBounds(new BlockPos(x, target.getMinBuildHeight(), z))
                || (remembered != null && !TravelSafety.withinBounds(target, remembered))) return false;
        // Force destination chunk generation before heightmap / pad work.
        target.getChunk(x >> 4, z >> 4);

        int surfaceY = remembered == null ? findSafeSurfaceY(target, x, z) : remembered.getY()-1;
        BlockPos ground = new BlockPos(x, surfaceY, z);
        if (!TravelSafety.withinBounds(target, ground.above()) || TravelSafety.hasHazard(target, ground.above()) || target.hasNeighborSignal(ground)
                || !target.getBlockState(ground.above()).getCollisionShape(target, ground.above()).isEmpty()
                || !target.getBlockState(ground.above(2)).getCollisionShape(target, ground.above(2)).isEmpty()) return false;
        ensureLandingPad(target, ground);
        if (!target.getBlockState(ground).isFaceSturdy(target, ground, net.minecraft.core.Direction.UP)
                || !target.getFluidState(ground.above()).isEmpty() || !target.getFluidState(ground.above(2)).isEmpty()) return false;
        BlockPos origin = player.blockPosition();

        if (player.changeDimension(new DimensionTransition(
                target,
                new Vec3(x + 0.5, ground.getY() + 1.0, z + 0.5),
                Vec3.ZERO,
                player.getYRot(),
                player.getXRot(),
                DimensionTransition.DO_NOTHING
        )) == null) return false;
        player.fallDistance = 0;
        if (!returning) {
            memory.putString("TribalGateReturnDimension", current.dimension().location().toString());
            memory.putLong("TribalGateReturn", origin.asLong());
        }
        if (targetKey.equals(THE_MARCH)) {
            DeepCacheManager.markVisited(player);
        }
        return true;
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
