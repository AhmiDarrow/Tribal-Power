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
     */
    public static boolean travelThroughGate(ServerPlayer player) {
        ServerLevel current = player.serverLevel();
        ResourceKey<Level> targetKey = current.dimension().equals(THE_MARCH) ? Level.OVERWORLD : THE_MARCH;
        ServerLevel target = player.server.getLevel(targetKey);
        if (target == null) {
            TribalPower.LOGGER.warn("Dimension {} is not loaded", targetKey.location());
            return false;
        }

        int x = player.blockPosition().getX();
        int z = player.blockPosition().getZ();
        int surfaceY = findSafeSurfaceY(target, x, z);
        BlockPos feet = new BlockPos(x, surfaceY, z);
        ensureLandingPad(target, feet);

        player.changeDimension(new DimensionTransition(
                target,
                new Vec3(x + 0.5, feet.getY() + 1.0, z + 0.5),
                Vec3.ZERO,
                player.getYRot(),
                player.getXRot(),
                DimensionTransition.DO_NOTHING
        ));
        if (targetKey.equals(THE_MARCH)) {
            DeepCacheManager.markVisited(player);
        }
        return true;
    }

    private static int findSafeSurfaceY(ServerLevel level, int x, int z) {
        int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int min = level.getMinBuildHeight() + 1;
        int max = level.getMaxBuildHeight() - 3;
        if (height < min) {
            height = 64;
        }
        height = Math.min(height, max);

        // Prefer standing on solid ground with two air blocks of headroom.
        for (int y = height; y >= min; y--) {
            BlockPos ground = new BlockPos(x, y, z);
            BlockPos feet = ground.above();
            BlockPos head = feet.above();
            BlockState groundState = level.getBlockState(ground);
            if (groundState.blocksMotion()
                    && !level.getBlockState(feet).blocksMotion()
                    && !level.getBlockState(head).blocksMotion()) {
                return y;
            }
        }
        return Math.max(64, min);
    }

    private static void ensureLandingPad(ServerLevel level, BlockPos groundCenter) {
        boolean inMarch = level.dimension().equals(THE_MARCH);
        BlockState pad = inMarch
                ? ModBlocks.MARCH_GRASS.get().defaultBlockState()
                : Blocks.STONE.defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos ground = groundCenter.offset(dx, 0, dz);
                BlockPos feet = ground.above();
                BlockPos head = feet.above();
                if (!level.getBlockState(ground).blocksMotion()) {
                    level.setBlockAndUpdate(ground, pad);
                }
                clearIfBlocking(level, feet);
                clearIfBlocking(level, head);
            }
        }
    }

    private static void clearIfBlocking(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.isAir() && (state.canBeReplaced() || state.blocksMotion() || !state.getCollisionShape(level, pos).isEmpty())) {
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
