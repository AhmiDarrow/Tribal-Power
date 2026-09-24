package tk.darrow.tribalpower.kit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.config.TribalConfig;

/**
 * Vine Lifts: living vine that knows its kin in the same column. Stand on one and jump to rise to the next lift
 * above; sneak to sink to the next one below. Any number can be strung up a shaft, one per floor, and they find
 * each other with no linking.
 */
public final class VineLifts {
    private static final Map<UUID, Boolean> SNEAKING = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> RESTED = new ConcurrentHashMap<>();

    private VineLifts() {}

    /** The next lift up (+1) or down (-1) from a lift, with room to stand on it, or null. */
    public static @Nullable BlockPos next(BlockGetter level, BlockPos from, int direction) {
        int range = TribalConfig.liftRange();
        for (int step = 1; step <= range; step++) {
            BlockPos at = from.above(step * direction);
            if (!(level.getBlockState(at).getBlock() instanceof VineLiftBlock)) continue;
            if (level.getBlockState(at.above()).isSuffocating(level, at.above())
                    || level.getBlockState(at.above(2)).isSuffocating(level, at.above(2))) continue;
            return at;
        }
        return null;
    }

    /** The lift under a player's feet, or null. */
    private static @Nullable BlockPos standingOn(ServerPlayer player) {
        if (!player.onGround()) return null;
        BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
        return player.level().getBlockState(below).getBlock() instanceof VineLiftBlock ? below : null;
    }

    /** Moves the player up or down the column. Returns whether it found somewhere to go. */
    public static boolean ride(ServerPlayer player, int direction) {
        BlockPos from = standingOn(player);
        if (from == null) return false;
        long now = player.level().getGameTime();
        if (now - RESTED.getOrDefault(player.getUUID(), -100L) < 6) return false;
        BlockPos to = next(player.level(), from, direction);
        if (to == null) return false;
        RESTED.put(player.getUUID(), now);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, player.getX(), player.getY() + 0.5, player.getZ(), 10, 0.3, 0.5, 0.3, 0);
        player.teleportTo(to.getX() + 0.5, to.getY() + 1, to.getZ() + 0.5);
        player.setDeltaMovement(0, 0, 0);
        player.hurtMarked = true;
        player.resetFallDistance();
        level.playSound(null, to, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 0.8F, direction > 0 ? 1.3F : 0.8F);
        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, to.getX() + 0.5, to.getY() + 1.5, to.getZ() + 0.5, 10, 0.3, 0.5, 0.3, 0);
        return true;
    }

    public static void loggedOut(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        SNEAKING.remove(event.getEntity().getUUID());
        RESTED.remove(event.getEntity().getUUID());
    }

    /** A jump on a lift carries you up. */
    public static void jump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof ServerPlayer player && !player.isShiftKeyDown()) ride(player, 1);
    }

    /** A fresh sneak on a lift lets you down. */
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        boolean sneaking = player.isShiftKeyDown();
        Boolean was = SNEAKING.put(player.getUUID(), sneaking);
        if (sneaking && !Boolean.TRUE.equals(was)) ride(player, -1);
    }
}
