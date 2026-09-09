package tk.darrow.tribalpower.rite.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/**
 * Green Blessing: crops, saplings and stems inside blessed chunks receive extra random ticks.
 * Each server tick every non-empty section of a blessed chunk is sampled {@link #EXTRA_TICKS}
 * times the way vanilla {@code randomTickSpeed} samples it; only growing plants are ticked.
 * Green sparkles mark the blessed ground once a second.
 */
public final class GreenBlessing {
    public static final int EXTRA_TICKS = 3;
    public static final int CHUNK_RADIUS = 1;

    private GreenBlessing() {}

    public static void bless(ServerLevel level, BlockPos center, int durationTicks) {
        RiteSavedData data = RiteSavedData.get(level.getServer());
        ChunkPos origin = new ChunkPos(center);
        long expiry = level.getGameTime() + durationTicks;
        for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++)
            for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++)
                data.bless(level, new ChunkPos(origin.x + dx, origin.z + dz), expiry);
    }

    public static boolean isBlessed(ServerLevel level, BlockPos pos) {
        return RiteSavedData.get(level.getServer()).isBlessed(level, new ChunkPos(pos));
    }

    public static boolean growable(BlockState state) {
        return state.getBlock() instanceof CropBlock || state.getBlock() instanceof SaplingBlock
                || state.getBlock() instanceof StemBlock || state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS);
    }

    public static void tick(ServerLevel level) {
        List<ChunkPos> chunks = RiteSavedData.get(level.getServer()).blessedChunks(level);
        if (chunks.isEmpty()) return;
        RandomSource random = level.random;
        boolean sparkle = level.getGameTime() % 20 == 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (ChunkPos chunkPos : chunks) {
            if (!level.hasChunk(chunkPos.x, chunkPos.z)) continue;
            LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
            int minX = chunkPos.getMinBlockX(), minZ = chunkPos.getMinBlockZ();
            LevelChunkSection[] sections = chunk.getSections();
            for (int s = 0; s < sections.length; s++) {
                LevelChunkSection section = sections[s];
                if (section == null || !section.isRandomlyTicking()) continue;
                int minY = chunk.getSectionYFromSectionIndex(s) << 4;
                for (int i = 0; i < EXTRA_TICKS; i++) {
                    int r = random.nextInt();
                    int x = r & 15, y = (r >> 8) & 15, z = (r >> 16) & 15;
                    BlockState state = section.getBlockState(x, y, z);
                    if (!state.isRandomlyTicking() || !growable(state)) continue;
                    cursor.set(minX + x, minY + y, minZ + z);
                    state.randomTick(level, cursor.immutable(), random);
                }
            }
            if (sparkle) {
                for (int i = 0; i < 4; i++) {
                    int x = minX + random.nextInt(16), z = minZ + random.nextInt(16);
                    int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x & 15, z & 15) + 1;
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x + 0.5, y + 0.3, z + 0.5, 2, 0.4, 0.3, 0.4, 0.0);
                }
            }
        }
    }
}
