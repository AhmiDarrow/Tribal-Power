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

/**
 * A Resonance Totem: the voice a station of its attunement answers to.
 *
 * <p>Two separate conditions decide whether it speaks (see lattice/Weave and lattice/Keeping):
 * <ul>
 *   <li><b>Its buffer</b> ({@link #BUFFER} Pulse). While the totem stands on an active lattice network (a Lattice
 *   Conductor within reach whose network's generators or cairns hold Pulse) it tops the buffer up from the network
 *   and then sits full: once full it costs nothing. Off the lattice, or while its network is dry, the buffer drains
 *   {@link #DRAIN_PER_SECOND} a second, and a totem with an empty buffer is <i>silent</i>: it lends no voice at all,
 *   whatever its keeping clock says. A newly carved totem starts full, so it speaks for about two minutes before
 *   it needs a lattice.</li>
 *   <li><b>Its keeping clock</b> ({@link Keeping}): answered, dim, then quiet when nobody works or wakes it. That
 *   clock runs on its own and is untouched by the buffer.</li>
 * </ul>
 * Totems are not Pulse sources: no machine draws on a totem's buffer, it only keeps the voice alive.
 */
public class ResonanceTotemBlockEntity extends BlockEntity implements PulseHandler, tk.darrow.tribalpower.api.Diagnosable {
    /** Pulse a totem holds to keep its voice: the same 250 it always held. */
    public static final int BUFFER = 250;
    /** What an off-lattice (or dry-lattice) totem's buffer loses a second: a full one goes silent in about two minutes. */
    public static final int DRAIN_PER_SECOND = 2;
    private Attunement attunement = Attunement.SPIRIT;
    private final PulseStorage resonance = new PulseStorage(BUFFER);
    private final List<BlockPos> links = new ArrayList<>();
    private int attention = Keeping.TOTAL_TICKS;

    public ResonanceTotemBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESONANCE_TOTEM.get(), pos, state);
        if (state.getBlock() instanceof ResonanceTotemBlock totem) {
            this.attunement = totem.getAttunement();
        }
        // A newly carved totem holds a first breath; a saved one loads what it held.
        resonance.setStored(BUFFER);
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
        // A totem from before it stood as two blocks grows its clickable top once the space above is clear.
        if ((level.getGameTime() + pos.asLong()) % 100 == 0) ResonanceTotemBlock.growTop(level, pos, state);
        if ((level.getGameTime() + pos.asLong()) % 20 == 0) be.breathe(level, pos);
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

    /**
     * Once a second: on an active lattice network, top the buffer up (and sit full once it is); off one, or on a
     * dry one, let it drain toward silence.
     */
    private void breathe(Level level, BlockPos pos) {
        int stored = resonance.getPulseStored();
        if (tk.darrow.tribalpower.lattice.Weave.active(level, pos)) {
            int room = BUFFER - stored;
            if (room > 0) {
                int drawn = tk.darrow.tribalpower.lattice.Weave.draw(level, pos, room, false);
                if (drawn > 0) insertPulse(drawn, false);
            }
        } else if (stored > 0) {
            extractPulse(DRAIN_PER_SECOND, false);
        }
    }

    /** True while the buffer holds Pulse: an empty totem is silent and lends no voice. */
    public boolean voiced() {
        return resonance.getPulseStored() > 0;
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

    // Conductors keep the chalk network they walked; a totem arriving or leaving must drop it.
    // The lattice lists the totems on each network too, for its readings.
    // An unloading chunk calls setRemoved, so that covers it without a second notice.
    @Override
    public void clearRemoved() {
        super.clearRemoved();
        tk.darrow.tribalpower.lattice.Weave.memberChanged(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        tk.darrow.tribalpower.lattice.Weave.memberChanged(level, worldPosition);
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
        if (tag.contains("Pulse")) resonance.load(tag);
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
        boolean active = tk.darrow.tribalpower.lattice.Weave.active(server, pos);
        if (!voiced()) {
            lines.add(Component.translatable("diag.tribalpower.totem.silent").withStyle(net.minecraft.ChatFormatting.RED));
        } else {
            lines.add(Component.translatable(active ? "diag.tribalpower.totem.fed" : "diag.tribalpower.totem.draining",
                    getPulseStored(), BUFFER, DRAIN_PER_SECOND).withStyle(active ? net.minecraft.ChatFormatting.GRAY : net.minecraft.ChatFormatting.YELLOW));
        }
        if (!active) lines.add(Component.translatable(tk.darrow.tribalpower.lattice.Weave.onLattice(server, pos)
                ? "diag.tribalpower.totem.dry_lattice" : "diag.tribalpower.totem.no_lattice").withStyle(net.minecraft.ChatFormatting.YELLOW));
        return lines;
    }
}
