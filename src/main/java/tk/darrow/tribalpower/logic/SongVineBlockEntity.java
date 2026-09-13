package tk.darrow.tribalpower.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Per-face 0–15 for a {@link SongVineBlock}. Packed six nybbles. */
public class SongVineBlockEntity extends BlockEntity {
    private int packed;

    public SongVineBlockEntity(BlockPos pos, BlockState state) {
        super(LogicRegistry.SONG_VINE_TYPE.get(), pos, state);
    }

    public int power(Direction face) {
        return (packed >> (face.ordinal() * 4)) & 15;
    }

    public void setPower(Direction face, int value) {
        int shift = face.ordinal() * 4;
        packed = (packed & ~(15 << shift)) | ((value & 15) << shift);
    }

    public int maxPower() {
        int best = 0;
        for (Direction face : Direction.values()) best = Math.max(best, power(face));
        return best;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SongVineBlockEntity vine) {
        if (level.isClientSide) return;
        if (vine.recompute(level, pos, state)) {
            level.updateNeighborsAt(pos, state.getBlock());
            for (Direction face : Direction.values()) level.updateNeighborsAt(pos.relative(face), state.getBlock());
        }
    }

    boolean recompute(Level level, BlockPos pos, BlockState state) {
        boolean changed = false;
        for (Direction face : Direction.values()) {
            if (!SongVineBlock.has(state, face)) {
                if (power(face) != 0) { setPower(face, 0); changed = true; }
                continue;
            }
            int best = 0;
            for (Direction along : Direction.values()) {
                if (along.getAxis() == face.getAxis()) continue;
                if (SongVineBlock.has(state, along)) best = Math.max(best, power(along) - 1);
                best = Math.max(best, SongVineBlock.incoming(level, pos, face, along));
            }
            best = Math.clamp(best, 0, 15);
            if (best != power(face)) { setPower(face, best); changed = true; }
        }
        if (changed) setChanged();
        return changed;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Power", packed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        packed = tag.getInt("Power");
    }
}
