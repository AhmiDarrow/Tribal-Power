package tk.darrow.tribalpower.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.api.pulse.PulseGenerator;
import tk.darrow.tribalpower.camp.CampHooks;

import java.util.EnumSet;
import java.util.Set;

/**
 * "Six voices singing" (design 3.1 section 14): all six generators producing at once, one per voice,
 * inside {@link #RANGE} blocks of each other.
 *
 * <p>Checked from the Loom Anchor rather than from every generator, because the Loom Anchor is the one
 * that only exists in a base that has already got this far -- and it is already walking its neighbours.
 */
public final class SixVoices {
    public static final int RANGE = 32;
    /** How often the Loom Anchor bothers to look. Rare: this is a one-time award, not a mechanic. */
    public static final int CHECK_TICKS = 200;

    private SixVoices() {}

    /** Voices actually producing Pulse within {@link #RANGE} of {@code origin}. */
    public static Set<Attunement> producing(ServerLevel level, BlockPos origin) {
        EnumSet<Attunement> voices = EnumSet.noneOf(Attunement.class);
        int min = RANGE;
        for (int cx = (origin.getX() - min) >> 4; cx <= (origin.getX() + min) >> 4; cx++) {
            for (int cz = (origin.getZ() - min) >> 4; cz <= (origin.getZ() + min) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof PulseGenerator generator) || be.isRemoved()) continue;
                    if (!be.getBlockPos().closerThan(origin, RANGE)) continue;
                    if (generator.currentOutput() > 0) voices.add(generator.voice());
                }
            }
        }
        return voices;
    }

    /** Awards anyone standing in the middle of a full elemental plant. */
    public static void check(ServerLevel level, BlockPos origin) {
        if (level.getGameTime() % CHECK_TICKS != 0) return;
        if (producing(level, origin).size() < Attunement.values().length) return;
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                new AABB(origin).inflate(RANGE), p -> !p.isSpectator()))
            CampHooks.award(level, player.getUUID(), "journey/six_voices");
    }
}
