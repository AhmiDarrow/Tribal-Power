package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.api.pulse.PulseHandler;
import tk.darrow.tribalpower.api.pulse.PulseStorage;
import tk.darrow.tribalpower.block.ResonanceTotemBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResonanceTotemBlockEntity extends BlockEntity implements PulseHandler {
    private Attunement attunement = Attunement.SPIRIT;
    private final PulseStorage resonance = new PulseStorage(250);
    private final List<BlockPos> links = new ArrayList<>();

    public ResonanceTotemBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESONANCE_TOTEM.get(), pos, state);
        if (state.getBlock() instanceof ResonanceTotemBlock totem) {
            this.attunement = totem.getAttunement();
        }
    }

    public ResonanceTotemBlockEntity(BlockPos pos, BlockState state, Attunement attunement) {
        this(pos, state);
        this.attunement = attunement;
    }

    public static ResonanceTotemBlockEntity create(BlockPos pos, BlockState state) {
        return new ResonanceTotemBlockEntity(pos, state);
    }

    public Attunement getAttunement() {
        return attunement;
    }

    public List<BlockPos> getLinks() {
        return Collections.unmodifiableList(links);
    }

    public boolean isLinkedTo(BlockPos other) {
        return links.contains(other);
    }

    public void addLink(BlockPos other) {
        if (!links.contains(other) && !other.equals(worldPosition)) {
            links.add(other.immutable());
            setChanged();
        }
    }

    public void removeLink(BlockPos other) {
        if (links.remove(other)) {
            setChanged();
        }
    }

    @Override
    public int getPulseStored() {
        return resonance.getPulseStored();
    }

    @Override
    public int getPulseCapacity() {
        return resonance.getPulseCapacity();
    }

    @Override
    public int insertPulse(int amount, boolean simulate) {
        int n = resonance.insertPulse(amount, simulate);
        if (!simulate && n > 0) {
            setChanged();
        }
        return n;
    }

    @Override
    public int extractPulse(int amount, boolean simulate) {
        int n = resonance.extractPulse(amount, simulate);
        if (!simulate && n > 0) {
            setChanged();
        }
        return n;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Attunement", attunement.getSerializedName());
        resonance.save(tag);
        ListTag list = new ListTag();
        for (BlockPos link : links) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("X", link.getX());
            entry.putInt("Y", link.getY());
            entry.putInt("Z", link.getZ());
            list.add(entry);
        }
        tag.put("Links", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        attunement = getBlockState().getBlock() instanceof ResonanceTotemBlock totem
                ? totem.getAttunement() : Attunement.byName(tag.getString("Attunement"));
        resonance.load(tag);
        links.clear();
        ListTag list = tag.getList("Links", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            links.add(new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z")));
        }
    }
}
