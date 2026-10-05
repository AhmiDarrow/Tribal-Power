package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.api.Diagnosable;
import tk.darrow.tribalpower.api.pulse.PulseHandler;
import tk.darrow.tribalpower.api.pulse.PulseStorage;
import tk.darrow.tribalpower.block.ModBlocks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A cairn of stones that holds a beat (design 3.1 section 9.4).
 *
 * <p>Cairns that touch face to face are one pile, and a pile is one store: each stone keeps its own share up to
 * its rank's capacity ({@link #capacityFor}: 4,000 / 16,000 / 64,000 / 256,000 for Woven / Attuned / Bound /
 * Manifested), so a stone's NBT is still just its own share, but insert, extract, the stored and capacity
 * readings and the comparator all speak for the whole pile, from any stone of it. A pile stops growing at
 * {@link #MAX_GROUP} stones; a stone past that stands alone.
 *
 * <p>Because every stone answers for the pile, anything that walks many positions and adds them up must
 * count a pile once: see {@link #repeatsPile}. The lattice does.
 *
 * <p>It is the lattice's storage (see {@link tk.darrow.tribalpower.lattice.Weave}): a pile within reach of a
 * Lattice Conductor drinks the surplus of the network's generators and lends it back to the network's machines.
 * It moves Pulse in and out at its stones' summed rate, {@link tk.darrow.tribalpower.lattice.Weave#rate} by each
 * stone's rank (64 / 256 / 1,024 / 4,096 a second, the same as a conductor), which is what turns a mob farm's
 * bursty Wake Bell into the steady draw a Listening Pit wants. Unranked stones from before ranks count as Woven.
 */
public class PulseCairnBlockEntity extends BlockEntity implements PulseHandler, Diagnosable {
    /** A Woven (unranked) stone's share. */
    public static final int CAPACITY = 4000;
    private static final int[] CAPACITY_BY_RANK = {4000, 16000, 64000, 256000};
    /** Most stones one pile joins: 256,000 Pulse. Keeps the flood fill cheap. */
    public static final int MAX_GROUP = 64;
    /** Safety stop for the flood fill that finds a pile's leader; far above any pile that can form. */
    private static final int FLOOD_LIMIT = 4096;

    /** Sized for a Manifested stone; {@link #ownCapacity} holds a lower rank to its own share. */
    private final PulseStorage pulse = new PulseStorage(CAPACITY_BY_RANK[3]);
    private Pile pile;

    public PulseCairnBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PULSE_CAIRN.get(), pos, state);
    }

    /** One stone's capacity at a machine rank: 4,000 / 16,000 / 64,000 / 256,000. */
    public static int capacityFor(int rank) {
        return CAPACITY_BY_RANK[Math.clamp(rank, 0, CAPACITY_BY_RANK.length - 1)];
    }

    /** This stone's capacity at its rank. */
    public int ownCapacity() {
        return capacityFor(tk.darrow.tribalpower.item.MachineRank.rank(this));
    }

    /** Pulse a second this stone moves in, and again out: its rank's lattice rate. */
    public int ownRate() {
        return tk.darrow.tribalpower.lattice.Weave.rate(tk.darrow.tribalpower.item.MachineRank.rank(this));
    }

    /**
     * The stones one pile joins. Every member holds the same instance, so invalidating it once
     * invalidates it for all of them.
     */
    public static final class Pile {
        private final List<PulseCairnBlockEntity> members;
        /** True for a stone that stands alone only because the pile it touches is already full. */
        private final boolean overflow;
        private boolean valid = true;
        private int lastSignal = -1;
        /** The second the in and out budgets count, and what each has moved in it. */
        private long window = Long.MIN_VALUE;
        private int inUsed, outUsed;
        private final Object inKey = new Object(), outKey = new Object();

        private Pile(List<PulseCairnBlockEntity> members, boolean overflow) {
            this.members = members;
            this.overflow = overflow;
        }

        public PulseCairnBlockEntity leader() { return members.get(0); }
        public List<PulseCairnBlockEntity> members() { return members; }
        public boolean valid() { return valid; }
        public int size() { return members.size(); }
        public boolean overflow() { return overflow; }
        public long key() { return leader().worldPosition.asLong(); }

        boolean contains(BlockPos pos) {
            for (PulseCairnBlockEntity member : members) if (member.worldPosition.equals(pos)) return true;
            return false;
        }

        public int stored() {
            long total = 0;
            for (PulseCairnBlockEntity member : members) total += member.pulse.getPulseStored();
            return (int) Math.min(total, Integer.MAX_VALUE);
        }

        public int capacity() {
            long total = 0;
            for (PulseCairnBlockEntity member : members) total += member.ownCapacity();
            return (int) Math.min(total, Integer.MAX_VALUE);
        }

        /** Pulse a second the pile moves in, and again out: its stones' rates summed. */
        public int rate() {
            long total = 0;
            for (PulseCairnBlockEntity member : members) total += member.ownRate();
            return (int) Math.min(total, Integer.MAX_VALUE);
        }

        private void roll(long now) {
            long second = Math.floorDiv(now, 20);
            if (second != window) {
                window = second;
                inUsed = 0;
                outUsed = 0;
            }
        }

        /** What the pile may still take in this second. */
        public int inLeft(long now) { roll(now); return Math.max(0, rate() - inUsed); }
        /** What the pile may still hand out this second. */
        public int outLeft(long now) { roll(now); return Math.max(0, rate() - outUsed); }
        public void spendIn(int n, long now) { roll(now); inUsed += Math.max(0, n); }
        public void spendOut(int n, long now) { roll(now); outUsed += Math.max(0, n); }
        /** Ledger keys for a simulated draw's pending budget. */
        public Object inKey() { return inKey; }
        public Object outKey() { return outKey; }

        public int insert(int amount, boolean simulate) {
            if (amount <= 0) return 0;
            int accepted = Math.min(amount, capacity() - stored());
            if (accepted <= 0) return 0;
            if (simulate) return accepted;
            // Spread it, so every stone carries a fair share and a broken stone takes only its own.
            int remaining = accepted;
            while (remaining > 0) {
                int open = 0;
                for (PulseCairnBlockEntity member : members) if (member.pulse.getPulseStored() < member.ownCapacity()) open++;
                if (open == 0) break;
                int share = Math.max(1, remaining / open);
                for (PulseCairnBlockEntity member : members) {
                    if (remaining <= 0) break;
                    int roomHere = member.ownCapacity() - member.pulse.getPulseStored();
                    if (roomHere <= 0) continue;
                    int n = member.pulse.insertPulse(Math.min(Math.min(share, remaining), roomHere), false);
                    if (n > 0) {
                        remaining -= n;
                        member.markDirty();
                    }
                }
            }
            changed();
            return accepted - remaining;
        }

        public int extract(int amount, boolean simulate) {
            if (amount <= 0) return 0;
            int taken = Math.min(amount, stored());
            if (taken <= 0) return 0;
            if (simulate) return taken;
            int remaining = taken;
            while (remaining > 0) {
                int holding = 0;
                for (PulseCairnBlockEntity member : members) if (member.pulse.getPulseStored() > 0) holding++;
                if (holding == 0) break;
                int share = Math.max(1, remaining / holding);
                for (PulseCairnBlockEntity member : members) {
                    if (remaining <= 0) break;
                    int n = member.pulse.extractPulse(Math.min(share, remaining), false);
                    if (n > 0) {
                        remaining -= n;
                        member.markDirty();
                    }
                }
            }
            changed();
            return taken - remaining;
        }

        public int signal() {
            int stored = stored();
            return stored == 0 ? 0 : 1 + 14 * stored / Math.max(1, capacity());
        }

        /** 0 for an empty pile, then 1 to 4 as it fills: what every stone's veins show. */
        public int charge() {
            int stored = stored();
            return stored <= 0 ? 0 : Math.min(4, 1 + 3 * stored / Math.max(1, capacity()));
        }

        /** Tell every stone's comparators when the pile's reading moves. */
        void changed() {
            int now = signal();
            if (now != lastSignal) {
                lastSignal = now;
                for (PulseCairnBlockEntity member : members) {
                    Level level = member.level;
                    if (level != null && !member.isRemoved())
                        level.updateNeighbourForOutputSignal(member.worldPosition, member.getBlockState().getBlock());
                }
            }
            showCharge();
        }

        /**
         * Light every stone to the pile's charge. Checked against each stone's own state rather than remembered, so a
         * stone whose state was set from outside (a command, a structure) comes back to the pile's light by itself.
         */
        void showCharge() {
            int charge = charge();
            for (PulseCairnBlockEntity member : members) {
                Level level = member.level;
                if (level == null || member.isRemoved()) continue;
                BlockState state = member.getBlockState();
                if (state.hasProperty(tk.darrow.tribalpower.block.PulseCairnBlock.CHARGE)
                        && state.getValue(tk.darrow.tribalpower.block.PulseCairnBlock.CHARGE) != charge)
                    // Same block, new look: the stone keeps its block entity, and neighbours are not woken for it.
                    level.setBlock(member.worldPosition, state.setValue(tk.darrow.tribalpower.block.PulseCairnBlock.CHARGE, charge),
                            net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            }
        }
    }

    // ---- the pile ----------------------------------------------------------------------------------

    /** This stone's pile, recomputed only after a stone beside it came or went. */
    public Pile pile() {
        Pile current = pile;
        if (current != null && current.valid) return current;
        return regroup();
    }

    /** What this one stone holds of its pile's store. */
    public int ownPulse() { return pulse.getPulseStored(); }

    /** Drop the cached pile; the next reading floods it again. */
    public void invalidatePile() {
        if (pile != null) pile.valid = false;
        pile = null;
    }

    /**
     * True when {@code be} is a cairn whose pile was already counted in {@code seen}; otherwise records it.
     * Anything that adds up handlers over many positions calls this so a pile of ten is counted once, not ten times.
     */
    public static boolean repeatsPile(BlockEntity be, Set<Long> seen) {
        return be instanceof PulseCairnBlockEntity cairn && !seen.add(cairn.pile().key());
    }

    private static PulseCairnBlockEntity cairnAt(Level level, BlockPos pos) {
        if (!level.isLoaded(pos) || !level.getBlockState(pos).is(ModBlocks.PULSE_CAIRN.get())) return null;
        return level.getBlockEntity(pos) instanceof PulseCairnBlockEntity cairn && !cairn.isRemoved() ? cairn : null;
    }

    /**
     * Flood the whole touching cluster, choose its leader (lowest {@link BlockPos#asLong}), then walk out
     * from the leader and take the first {@link #MAX_GROUP} stones. Every stone in the cluster is assigned
     * here, so the answer is the same whichever stone asked, and no stone recomputes until something changes.
     */
    private Pile regroup() {
        if (level == null || isRemoved()) {
            pile = new Pile(List.of(this), false);
            return pile;
        }
        HashMap<Long, PulseCairnBlockEntity> cluster = new HashMap<>();
        HashSet<Long> queued = new HashSet<>();
        ArrayDeque<PulseCairnBlockEntity> queue = new ArrayDeque<>();
        queue.add(this);
        queued.add(worldPosition.asLong());
        PulseCairnBlockEntity leader = this;
        while (!queue.isEmpty() && cluster.size() < FLOOD_LIMIT) {
            PulseCairnBlockEntity current = queue.removeFirst();
            cluster.put(current.worldPosition.asLong(), current);
            if (current.worldPosition.asLong() < leader.worldPosition.asLong()) leader = current;
            for (Direction side : Direction.values()) {
                BlockPos next = current.worldPosition.relative(side);
                if (!queued.add(next.asLong())) continue;
                PulseCairnBlockEntity found = cairnAt(level, next);
                if (found != null) queue.add(found);
            }
        }

        List<PulseCairnBlockEntity> members = new ArrayList<>();
        HashSet<Long> joined = new HashSet<>();
        ArrayDeque<PulseCairnBlockEntity> walk = new ArrayDeque<>();
        walk.add(leader);
        joined.add(leader.worldPosition.asLong());
        while (!walk.isEmpty() && members.size() < MAX_GROUP) {
            PulseCairnBlockEntity current = walk.removeFirst();
            members.add(current);
            for (Direction side : Direction.values()) {
                long next = current.worldPosition.relative(side).asLong();
                PulseCairnBlockEntity found = cluster.get(next);
                if (found != null && joined.add(next)) walk.add(found);
            }
        }

        for (PulseCairnBlockEntity stone : cluster.values()) stone.invalidatePile();
        Pile shared = new Pile(List.copyOf(members), false);
        HashSet<PulseCairnBlockEntity> inShared = new HashSet<>(members);
        for (PulseCairnBlockEntity stone : cluster.values())
            stone.pile = inShared.contains(stone) ? shared : new Pile(List.of(stone), true);
        return pile;
    }

    /** A stone beside this one came or went: forget the pile so the next reading floods it afresh. */
    public void onNeighbourChanged(BlockPos neighbour) {
        if (level == null) return;
        boolean isCairn = level.getBlockState(neighbour).is(ModBlocks.PULSE_CAIRN.get());
        Pile current = pile;
        if (current == null || !current.valid) return;
        if (isCairn != current.contains(neighbour)) {
            invalidatePile();
            tk.darrow.tribalpower.lattice.Weave.memberChanged(level, worldPosition);
        }
    }

    /** Forget the piles of every cairn touching {@code pos}. */
    public static void invalidateAround(Level level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            BlockPos next = pos.relative(side);
            if (level.isLoaded(next) && level.getBlockEntity(next) instanceof PulseCairnBlockEntity cairn) cairn.invalidatePile();
        }
        tk.darrow.tribalpower.lattice.Weave.memberChanged(level, pos);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // A chunk loading beside a pile brings stones the pile has not met yet.
        if (level != null) invalidateAround(level, worldPosition);
    }

    // The lattice keeps the cairns it found on each network; a stone arriving or leaving must tell it.
    @Override
    public void clearRemoved() {
        super.clearRemoved();
        tk.darrow.tribalpower.lattice.Weave.memberChanged(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        invalidatePile();
        tk.darrow.tribalpower.lattice.Weave.memberChanged(level, worldPosition);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        tk.darrow.tribalpower.lattice.Weave.memberChanged(level, worldPosition);
    }

    private void markDirty() {
        if (level != null) level.blockEntityChanged(worldPosition);
    }

    // ---- PulseHandler, for the whole pile ------------------------------------------------------------

    @Override public int getPulseStored() { return pile().stored(); }
    @Override public int getPulseCapacity() { return pile().capacity(); }
    @Override public int insertPulse(int amount, boolean simulate) { return pile().insert(amount, simulate); }
    @Override public int extractPulse(int amount, boolean simulate) { return pile().extract(amount, simulate); }

    public int signal() { return pile().signal(); }

    public static void tick(Level level, BlockPos pos, BlockState state, PulseCairnBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
        Pile pile = be.pile();
        // The pile drinks once, through its leader, so a pile of twenty is not twenty straws in the same generators.
        if (pile.leader() != be) return;
        pile.changed();
        for (PulseCairnBlockEntity member : pile.members)
            if (level.hasNeighborSignal(member.worldPosition)) return;
        // Only ever pull generator surplus, and only through the lattice: a cairn hoarding another cairn's stock
        // would be a shell game, and one emptying a generator its machines draw on would starve them. Every stone
        // in reach of a conductor taps it, so a long pile drinks along its length.
        tk.darrow.tribalpower.lattice.Weave.fillPile(level, pile);
    }

    @Override
    public List<Component> diagnose(ServerLevel server, BlockPos pos) {
        List<Component> lines = new ArrayList<>();
        Pile pile = pile();
        if (pile.overflow()) {
            lines.add(Component.translatable("diag.tribalpower.cairn.overflow", MAX_GROUP)
                    .withStyle(net.minecraft.ChatFormatting.YELLOW));
        }
        lines.add(Component.translatable("diag.tribalpower.cairn.pile", pile.size(), MAX_GROUP));
        lines.add(Component.translatable("diag.tribalpower.cairn.share", ownPulse(), ownCapacity()));
        lines.add(Component.translatable("diag.tribalpower.cairn.rank",
                Component.translatable("item.tribalpower.spiritgear.rank." + tk.darrow.tribalpower.item.MachineRank.rank(this)),
                ownCapacity(), ownRate()));
        lines.add(Component.translatable("diag.tribalpower.cairn.rate", pile.capacity(), pile.rate()));
        if (!tk.darrow.tribalpower.lattice.Weave.onLattice(server, pos))
            lines.add(Component.translatable("diag.tribalpower.cairn.off_lattice").withStyle(net.minecraft.ChatFormatting.YELLOW));
        return lines;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        pulse.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pulse.load(tag);
    }
}
