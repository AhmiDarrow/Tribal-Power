package tk.darrow.tribalpower.event;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.blockentity.LeyCollectorBlockEntity;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.effect.AfflictionEffect;
import tk.darrow.tribalpower.effect.ModEffects;
import tk.darrow.tribalpower.ley.LeyField;

/**
 * A ley surge: for a while one voice's threads run bright. Collectors that hear that voice yield more; anyone
 * standing on a bare crossing (a cell's nexus with no collector to drink it) takes Ley Sickness.
 */
public final class LeySurges {
    /** How close to a nexus counts as standing on it. */
    public static final int CROSSING_REACH = 3;
    private static final int BARE_REACH = 4;

    private LeySurges() {}

    /** The surging voice, on either side, or null. */
    public static Attunement voice(Level level) {
        // the client only ever shows the surge in the March; the server's collectors feel it wherever ley runs
        if (level.isClientSide) return level.dimension().equals(tk.darrow.tribalpower.world.ModDimensions.THE_MARCH) ? MarchStatePayload.latest.surge() : null;
        return level instanceof ServerLevel server ? MarchEventsSavedData.get(server.getServer()).surgeVoice(server.getGameTime()) : null;
    }

    /** What a collector's beat is worth under the surge: more when one of its threads carries the surging voice. */
    public static double multiplier(Level level, LeyField.Reading reading) {
        Attunement surge = voice(level);
        return surge != null && reading.heard(surge) ? TribalConfig.surgeYieldMultiplier() : 1.0;
    }

    /** The multiplier for a collector at this position (samples the threads only while a surge runs). */
    public static double multiplier(Level level, BlockPos pos) {
        if (voice(level) == null || !(level instanceof ServerLevel server)) return 1.0;
        return multiplier(level, LeyField.sample(server, pos));
    }

    /** The nexus of the cell this position lies in. */
    public static BlockPos nexusOf(ServerLevel level, BlockPos pos) {
        return LeyField.nexusPos(level, Math.floorDiv(pos.getX(), LeyField.CELL), Math.floorDiv(pos.getZ(), LeyField.CELL));
    }

    /** Whether the player stands on a bare crossing: at its nexus, with no collector near to drink it. */
    public static boolean onBareCrossing(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos nexus = nexusOf(level, player.blockPosition());
        BlockPos at = player.blockPosition();
        if (Math.abs(at.getX() - nexus.getX()) > CROSSING_REACH || Math.abs(at.getZ() - nexus.getZ()) > CROSSING_REACH
                || Math.abs(at.getY() - nexus.getY()) > CROSSING_REACH + 2) return false;
        for (BlockPos near : BlockPos.betweenClosed(nexus.offset(-BARE_REACH, -BARE_REACH, -BARE_REACH), nexus.offset(BARE_REACH, BARE_REACH, BARE_REACH)))
            if (level.getBlockEntity(near) instanceof LeyCollectorBlockEntity) return false;
        return true;
    }

    /** Every couple of seconds while a surge runs: the sickness for anyone on a bare crossing. */
    public static void sicknessTick(ServerPlayer player) {
        if (player.getAbilities().invulnerable || player.isSpectator() || voice(player.level()) == null) return;
        if (!onBareCrossing(player)) return;
        boolean had = ModEffects.afflicted(player, AfflictionEffect.Kind.LEY_SICKNESS);
        ModEffects.afflict(player, AfflictionEffect.Kind.LEY_SICKNESS, TribalConfig.surgeSicknessSeconds() * 20, 0);
        if (!had) player.displayClientMessage(Component.translatable("message.tribalpower.surge.sickness").withStyle(ChatFormatting.DARK_GREEN), true);
    }
}
