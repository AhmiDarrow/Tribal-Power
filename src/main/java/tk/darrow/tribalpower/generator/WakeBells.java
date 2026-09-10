package tk.darrow.tribalpower.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Where the bells are (design 3.1 section 9.6).
 *
 * <p>One global death handler and a position registry per level, so a bell never polls and a death never
 * searches. A death credits only the nearest bell in range: crediting every bell would make stacking them
 * a multiplier, and no voice in this release is a multiplier.
 */
public final class WakeBells {
    private static final Map<ServerLevel, Set<BlockPos>> BELLS = new WeakHashMap<>();

    private WakeBells() {}

    public static void add(ServerLevel level, BlockPos pos) {
        BELLS.computeIfAbsent(level, l -> new HashSet<>()).add(pos.immutable());
    }

    public static void remove(ServerLevel level, BlockPos pos) {
        Set<BlockPos> bells = BELLS.get(level);
        if (bells != null) bells.remove(pos);
    }

    public static int count(ServerLevel level) {
        Set<BlockPos> bells = BELLS.get(level);
        return bells == null ? 0 : bells.size();
    }

    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead instanceof Player || !(dead.level() instanceof ServerLevel level)) return;
        Set<BlockPos> bells = BELLS.get(level);
        if (bells == null || bells.isEmpty()) return;

        BlockPos where = dead.blockPosition();
        BlockPos nearest = null;
        double best = Double.MAX_VALUE;
        for (BlockPos pos : bells) {
            double distance = pos.distSqr(where);
            if (distance > (double) WakeBellBlockEntity.RANGE * WakeBellBlockEntity.RANGE || distance >= best) continue;
            nearest = pos;
            best = distance;
        }
        if (nearest == null) return;
        if (!(level.getBlockEntity(nearest) instanceof WakeBellBlockEntity bell)) {
            // The registry outlived the block: forget it rather than looking again next death.
            bells.remove(nearest);
            return;
        }
        if (bell.stilled()) return;
        bell.mourn(dead instanceof Enemy ? WakeBellBlockEntity.HOSTILE : WakeBellBlockEntity.PASSIVE);
    }
}
