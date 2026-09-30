package tk.darrow.tribalpower.lattice;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.blockentity.ResonanceTotemBlockEntity;

/**
 * A totem you use stays answered. A totem you ignore goes dim, then quiet.
 * Timers only run while the chunk ticks (a Wayanchor is the legal "I meant to leave this on").
 */
public final class Keeping {
    public static final int ANSWERED_TICKS = 48000;
    public static final int TOTAL_TICKS = 96000;
    public static final float DIM_STRETCH = 1.3F;

    public enum State { ANSWERED, DIM, QUIET }

    private Keeping() {}

    /** Best state among totems of this voice (nearby or chalk-linked). Kinship Totems are always answered. */
    public static State voice(Level level, BlockPos origin, Attunement voice) {
        return voice(LatticeNetwork.TotemsNear.of(level, origin, LatticeNetwork.DEFAULT_RADIUS), voice);
    }

    /** As {@link #voice(Level, BlockPos, Attunement)}, from totems a machine beat already found. */
    public static State voice(LatticeNetwork.TotemsNear near, Attunement voice) {
        State best = State.QUIET;
        for (ResonanceTotemBlockEntity totem : near.voice(voice)) {
            State state = totem.keeping();
            if (state == State.ANSWERED) return State.ANSWERED;
            if (state == State.DIM) best = State.DIM;
        }
        // A Kinship Totem never goes quiet, so it answers even beside a dim or quiet Resonance Totem of its voice.
        for (var kinship : near.kinship())
            if (kinship.attunement() == voice) return State.ANSWERED;
        return best;
    }

    public static boolean quiet(Level level, BlockPos origin, Attunement voice) {
        return voice(level, origin, voice) == State.QUIET
                && LatticeNetwork.hasAttunement(level, origin, LatticeNetwork.DEFAULT_RADIUS, voice);
    }

    public static int stretch(State state, int seconds) {
        if (state != State.DIM) return seconds;
        return Math.max(seconds + 1, Math.round(seconds * DIM_STRETCH));
    }

    public static void feedWork(Level level, BlockPos origin, Attunement voice) {
        if (level.isClientSide) return;
        for (ResonanceTotemBlockEntity totem : LatticeNetwork.findVoiceTotems(level, origin, LatticeNetwork.DEFAULT_RADIUS, voice)) {
            if (totem.keeping() != State.QUIET) totem.feed();
        }
    }

    /** As {@link #feedWork(Level, BlockPos, Attunement)}, from totems a machine beat already found. */
    public static void feedWork(LatticeNetwork.TotemsNear near, Attunement voice) {
        if (near.level().isClientSide) return;
        for (ResonanceTotemBlockEntity totem : near.voice(voice)) {
            if (totem.keeping() != State.QUIET) totem.feed();
        }
    }

    /** Player strike, Bone Chime, Kin drummer. Wakes Quiet. */
    public static void livingBeat(Level level, BlockPos origin) {
        if (level.isClientSide) return;
        for (ResonanceTotemBlockEntity totem : LatticeNetwork.findNearbyTotems(level, origin, LatticeNetwork.DEFAULT_RADIUS))
            totem.feed();
    }

    public static void relight(ResonanceTotemBlockEntity totem, ServerLevel server) {
        if (totem == null) return;
        totem.feed();
        tk.darrow.tribalpower.effect.SpiritEffects.ring(server, totem.getBlockPos().getCenter().add(0, 0.6, 0),
                totem.getAttunement(), 0.7, 12);
    }
}
