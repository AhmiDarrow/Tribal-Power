package tk.darrow.tribalpower.world.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds the tablet id (0–11), synced to the client so the screen can show the right fragment. */
public class LoreTabletBlockEntity extends BlockEntity {
    private int tablet;

    public LoreTabletBlockEntity(BlockPos pos, BlockState state) { super(MarchRegistry.LORE_TABLET_BE.get(), pos, state); }

    public int tablet() { return tablet; }
    public void setTablet(int tablet) {
        this.tablet = Math.floorMod(tablet, LoreTabletBlock.TABLETS);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Tablet", tablet);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tablet = Math.floorMod(tag.getInt("Tablet"), LoreTabletBlock.TABLETS);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
