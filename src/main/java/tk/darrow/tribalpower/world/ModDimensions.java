package tk.darrow.tribalpower.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.block.ModBlocks;

public final class ModDimensions {
    public static final ResourceKey<Level> THE_MARCH = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "the_march")
    );

    private ModDimensions() {}

    /**
     * Gate Drum travel: Overworld ↔ The March. Crude safe teleport with a small landing pad.
     */
    public static boolean travelThroughGate(ServerPlayer player) {
        ServerLevel current = player.serverLevel();
        ResourceKey<Level> targetKey = current.dimension().equals(THE_MARCH) ? Level.OVERWORLD : THE_MARCH;
        ServerLevel target = player.server.getLevel(targetKey);
        if (target == null) {
            TribalPower.LOGGER.warn("Dimension {} is not loaded", targetKey.location());
            return false;
        }

        double x = player.getX();
        double z = player.getZ();
        int surfaceY = target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z);
        if (surfaceY < target.getMinBuildHeight() + 1) {
            surfaceY = 80;
        }

        BlockPos feet = BlockPos.containing(x, surfaceY, z);
        ensureLandingPad(target, feet);

        player.teleportTo(target, x + 0.5, feet.getY() + 1.0, z + 0.5, player.getYRot(), player.getXRot());
        return true;
    }

    private static void ensureLandingPad(ServerLevel level, BlockPos feet) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos ground = feet.offset(dx, 0, dz);
                BlockPos above = ground.above();
                if (level.getBlockState(ground).isAir() || !level.getBlockState(ground).blocksMotion()) {
                    level.setBlockAndUpdate(ground, ModBlocks.MARCH_STONE.get().defaultBlockState());
                }
                if (!level.getBlockState(above).isAir() && level.getBlockState(above).canBeReplaced()) {
                    level.setBlockAndUpdate(above, Blocks.AIR.defaultBlockState());
                } else if (!level.getBlockState(above).isAir() && level.getBlockState(above).blocksMotion()) {
                    level.setBlockAndUpdate(above, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        // Hook reserved for March ambient effects / spirit link maintenance.
    }
}
