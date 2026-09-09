package tk.darrow.tribalpower.rite.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.blockentity.ResonanceTotemBlockEntity;
import tk.darrow.tribalpower.effect.SpiritEffects;

import java.util.ArrayList;
import java.util.List;

/**
 * Ley Binding: temporary ley lines that make two Resonance Totems adjacent for Pulse routing.
 * {@code LatticeNetwork} consults {@link #linked(Level, BlockPos)} while walking chalk links.
 */
public final class LeyLines {
    public static final int RANGE = 64;
    public static final int PARTICLE_INTERVAL = 10;

    private LeyLines() {}

    /** Other ends of live ley lines touching {@code pos}. Empty on the client or when nothing is bound. */
    public static List<BlockPos> linked(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server) || server.getServer() == null) return List.of();
        List<BlockPos> out = null;
        for (RiteSavedData.LeyLine line : RiteSavedData.get(server.getServer()).leyLines(server)) {
            if (!line.touches(pos)) continue;
            if (out == null) out = new ArrayList<>(2);
            out.add(line.other(pos));
        }
        return out == null ? List.of() : out;
    }

    public static boolean isLinked(Level level, BlockPos a, BlockPos b) {
        return linked(level, a).contains(b);
    }

    public static void bind(ServerLevel level, BlockPos a, BlockPos b, int durationTicks) {
        RiteSavedData.get(level.getServer()).bind(level, a, b, level.getGameTime() + durationTicks);
    }

    /** Nearest Resonance Totem to {@code origin} within the cube of {@code radius}, or null. */
    @Nullable
    public static ResonanceTotemBlockEntity nearestTotem(ServerLevel level, BlockPos origin, int radius, @Nullable BlockPos exclude) {
        ResonanceTotemBlockEntity best = null;
        double bestDist = Double.MAX_VALUE;
        ChunkPos center = new ChunkPos(origin);
        int chunkRadius = (radius >> 4) + 1;
        for (int cx = center.x - chunkRadius; cx <= center.x + chunkRadius; cx++) {
            for (int cz = center.z - chunkRadius; cz <= center.z + chunkRadius; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof ResonanceTotemBlockEntity totem) || be.isRemoved()) continue;
                    BlockPos pos = be.getBlockPos();
                    if (pos.equals(exclude) || pos.equals(origin)) continue;
                    if (Math.abs(pos.getX() - origin.getX()) > radius || Math.abs(pos.getY() - origin.getY()) > radius
                            || Math.abs(pos.getZ() - origin.getZ()) > radius) continue;
                    double d = pos.distSqr(origin);
                    if (d < bestDist) { bestDist = d; best = totem; }
                }
            }
        }
        return best;
    }

    /** Server tick: a thread of Loom-coloured particles runs along every live line every 10 ticks. */
    public static void tick(ServerLevel level) {
        if (level.getGameTime() % PARTICLE_INTERVAL != 0) return;
        List<RiteSavedData.LeyLine> lines = RiteSavedData.get(level.getServer()).leyLines(level);
        if (lines.isEmpty()) return;
        DustParticleOptions dust = new DustParticleOptions(SpiritEffects.color(Attunement.LOOM), 0.8F);
        double phase = (level.getGameTime() / (double) PARTICLE_INTERVAL) % 1.0;
        for (RiteSavedData.LeyLine line : lines) {
            if (!level.hasChunkAt(line.a()) || !level.hasChunkAt(line.b())) continue;
            Vec3 from = line.a().getCenter().add(0, 0.6, 0);
            Vec3 to = line.b().getCenter().add(0, 0.6, 0);
            int count = Math.max(4, (int) (from.distanceTo(to) / 1.5));
            for (int i = 0; i < count; i++) {
                double t = (i + phase * 0.5) / count;
                Vec3 p = from.lerp(to, t);
                double lift = Math.sin(t * Math.PI) * 0.6;
                level.sendParticles(dust, p.x, p.y + lift, p.z, 1, 0, 0, 0, 0);
            }
        }
    }
}
