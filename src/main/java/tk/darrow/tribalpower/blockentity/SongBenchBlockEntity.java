package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SongBenchBlockEntity extends BlockEntity {
    private boolean singing;
    private int linkedTotems;
    private int songTicks;

    public SongBenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SONG_BENCH.get(), pos, state);
    }

    public void startSong(int linked) {
        this.linkedTotems = linked;
        this.singing = linked > 0;
        this.songTicks = singing ? 100 : 0;
        setChanged();
    }

    public boolean isSinging() {
        return singing;
    }

    public int getLinkedTotems() {
        return linkedTotems;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Singing", singing);
        tag.putInt("LinkedTotems", linkedTotems);
        tag.putInt("SongTicks", songTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        singing = tag.getBoolean("Singing");
        linkedTotems = tag.getInt("LinkedTotems");
        songTicks = tag.getInt("SongTicks");
    }
}
