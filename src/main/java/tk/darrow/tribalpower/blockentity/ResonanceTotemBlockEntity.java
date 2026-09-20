package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.api.pulse.PulseHandler;
import tk.darrow.tribalpower.api.pulse.PulseStorage;
import tk.darrow.tribalpower.block.ResonanceTotemBlock;
import tk.darrow.tribalpower.lattice.Keeping;
import tk.darrow.tribalpower.ley.LeyMath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResonanceTotemBlockEntity extends BlockEntity implements PulseHandler, tk.darrow.tribalpower.api.Diagnosable {
    private Attunement attunement = Attunement.SPIRIT;
    private final PulseStorage resonance = new PulseStorage(250);
    private final List<BlockPos> links = new ArrayList<>();
    private int attention = Keeping.TOTAL_TICKS;

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

    /** Cached ley reading: the land around a totem changes on the scale of weather, not of ticks. */
    private boolean lush;
    private long lushReadAt = Long.MIN_VALUE;

    public static void serverTick(Level level, BlockPos pos, BlockState state, ResonanceTotemBlockEntity be) {
        if (be.attention <= 0) return;
        long now = level.getGameTime();
        if (now - be.lushReadAt >= 100L || be.lushReadAt == Long.MIN_VALUE) {
            be.lush = LeyMath.glimpse(level, pos).strength() >= LeyMath.PAD;
            be.lushReadAt = now;
        }
        if (be.lush && (now & 1L) != 0L) return;
        be.attention--;
        if (be.attention % 200 == 0) be.setChanged();
    }

    public Keeping.State keeping() {
        if (attention > Keeping.ANSWERED_TICKS) return Keeping.State.ANSWERED;
        if (attention > 0) return Keeping.State.DIM;
        return Keeping.State.QUIET;
    }

    public int attention() {
        return attention;
    }

    public void feed() {
        attention = Keeping.TOTAL_TICKS;
        setChanged();
    }

    /** Test hook. */
    public void setAttention(int ticks) {
        attention = Math.max(0, ticks);
        setChanged();
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
        tag.putInt("Attention", attention);
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
        attention = tag.contains("Attention") ? Math.max(0, tag.getInt("Attention")) : Keeping.TOTAL_TICKS;
    }

    @Override
    public List<Component> diagnose(net.minecraft.server.level.ServerLevel server, BlockPos pos) {
        List<Component> lines = new ArrayList<>();
        Keeping.State state = keeping();
        lines.add(Component.translatable("diag.tribalpower.totem.keeping." + state.name().toLowerCase(java.util.Locale.ROOT)));
        if (state != Keeping.State.ANSWERED)
            lines.add(Component.translatable("diag.tribalpower.totem.wake").withStyle(net.minecraft.ChatFormatting.YELLOW));
        return lines;
    }
}
