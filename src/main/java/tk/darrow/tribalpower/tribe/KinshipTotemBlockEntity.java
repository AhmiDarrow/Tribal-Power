package tk.darrow.tribalpower.tribe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import tk.darrow.tribalpower.api.pulse.Attunement;

/** Stores the totem's tribe ({@code Tribe} int NBT); the block state property mirrors it for rendering. */
public class KinshipTotemBlockEntity extends BlockEntity {
    private TribeDefinition tribe = TribeDefinition.SOIL;

    public KinshipTotemBlockEntity(BlockPos pos, BlockState state) {
        super(TribeRegistry.KINSHIP_TYPE.get(), pos, state);
        if (state.hasProperty(KinshipTotemBlock.TRIBE)) tribe = TribeDefinition.byOrdinal(state.getValue(KinshipTotemBlock.TRIBE));
    }

    public TribeDefinition tribe() { return tribe; }
    public Attunement attunement() { return tribe.attunement(); }

    public void setTribe(TribeDefinition tribe) {
        this.tribe = tribe;
        setChanged();
        if (level != null && getBlockState().hasProperty(KinshipTotemBlock.TRIBE) && getBlockState().getValue(KinshipTotemBlock.TRIBE) != tribe.ordinal())
            level.setBlock(worldPosition, getBlockState().setValue(KinshipTotemBlock.TRIBE, tribe.ordinal()), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TribeDefinition.NBT_KEY, tribe.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TribeDefinition.NBT_KEY)) tribe = TribeDefinition.byOrdinal(tag.getInt(TribeDefinition.NBT_KEY));
        else if (getBlockState().hasProperty(KinshipTotemBlock.TRIBE)) tribe = TribeDefinition.byOrdinal(getBlockState().getValue(KinshipTotemBlock.TRIBE));
    }
}
