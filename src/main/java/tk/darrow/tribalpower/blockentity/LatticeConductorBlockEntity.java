package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.item.MachineRank;
import tk.darrow.tribalpower.lattice.LatticeNetwork;
import tk.darrow.tribalpower.lattice.Weave;

/**
 * A thread of the Pulse lattice: the only way Pulse travels from a generator to a machine.
 *
 * <p>Everything that makes, holds or spends Pulse within {@link #RADIUS} of a conductor is on the lattice, and
 * conductors within that reach of each other link into one network (see {@link Weave} for the full rules). Machines
 * draw only from their network's generators and Pulse Cairns, never straight from a generator beside them.
 *
 * <p>Each conductor carries at most its rank's rate a second ({@link #rate}: Woven 64, Attuned 256, Bound 1,024,
 * Manifested 4,096), counting Pulse it lifts from a source and Pulse it hands to a consumer; Pulse that enters and
 * leaves through the same conductor counts once. Ranked like any machine (Echo Attune, Bind, Manifest); an unranked
 * conductor from an older world is Woven. A redstone signal lifts it out of the weave.
 *
 * <p>The conductor does no work of its own each tick: the budget below is counted lazily from the game time, and
 * totems fill themselves from their network. Chalk links between totems still share voices, but no longer carry
 * Pulse.
 */
public class LatticeConductorBlockEntity extends BlockEntity implements tk.darrow.tribalpower.api.Diagnosable {
    public static final int RADIUS = LatticeNetwork.DEFAULT_RADIUS;

    /** The second the budget counts ({@code gameTime / 20}), what has passed through in it, and in the one before. */
    private long window = Long.MIN_VALUE;
    private int carried;
    private int carriedBefore;
    /** The redstone lock as last read, or null before the first neighbour change. */
    private Boolean lockedWhenRead;

    public LatticeConductorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LATTICE_CONDUCTOR.get(), pos, state);
    }

    // The lattice keeps the networks it wove; a conductor arriving or leaving (placed, broken, its chunk loaded or
    // unloaded) must have them woven again.
    @Override
    public void clearRemoved() {
        super.clearRemoved();
        Weave.conductorChanged(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        Weave.conductorChanged(level, worldPosition);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        Weave.conductorChanged(level, worldPosition);
    }

    /** A neighbour changed: a redstone lock that came or went re-weaves the networks. */
    public void neighbourChanged() {
        if (level == null || level.isClientSide) return;
        boolean locked = level.hasNeighborSignal(worldPosition);
        if (lockedWhenRead == null || locked != lockedWhenRead) {
            lockedWhenRead = locked;
            Weave.conductorChanged(level, worldPosition);
        }
    }

    /** The rank's rate, read from the rank NBT once and kept until the block changes or the second turns. */
    private int rate = -1;

    /** Pulse a second this conductor carries at its rank. */
    public int rate() {
        if (rate < 0) rate = Weave.rate(MachineRank.rank(this));
        return rate;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        rate = -1; // ranking marks the block changed
    }

    private void roll(long now) {
        long second = Math.floorDiv(now, 20);
        if (second != window) {
            rate = -1;
            carriedBefore = second == window + 1 ? carried : 0;
            window = second;
            carried = 0;
        }
    }

    /** What this conductor may still carry in the current second. */
    public int budgetLeft(long now) {
        roll(now);
        return Math.max(0, rate() - carried);
    }

    /** Counts {@code amount} through this conductor in the current second. */
    public void carry(int amount, long now) {
        roll(now);
        carried += Math.max(0, amount);
    }

    /** What passed through in the last whole second: what a reading shows. */
    public int carriedLastSecond(long now) {
        roll(now);
        return carriedBefore;
    }

    /** What has passed through so far this second. */
    public int carriedThisSecond(long now) {
        roll(now);
        return carried;
    }

    /** Manual strike: say which network this conductor weaves and what it carries. */
    public Component conductOnce() {
        if (level == null) return Component.translatable("message.tribalpower.conductor.no_network");
        if (level.hasNeighborSignal(worldPosition)) return Component.translatable("message.tribalpower.redstone.locked");
        Weave.Reading reading = Weave.read(level, worldPosition);
        return Component.translatable("message.tribalpower.conductor.weave", reading.conductors(), reading.generators(),
                reading.cairns(), rate(), reading.rate(), reading.stored());
    }

    @Override
    public java.util.List<Component> diagnose(net.minecraft.server.level.ServerLevel server, BlockPos pos) {
        java.util.List<Component> lines = new java.util.ArrayList<>();
        if (server.hasNeighborSignal(pos)) {
            lines.add(Component.translatable("diag.tribalpower.conductor.locked").withStyle(net.minecraft.ChatFormatting.YELLOW));
            return lines;
        }
        long now = server.getGameTime();
        lines.add(Component.translatable("diag.tribalpower.conductor.rate",
                Component.translatable("item.tribalpower.spiritgear.rank." + MachineRank.rank(this)), rate(),
                carriedLastSecond(now)));
        Weave.Reading reading = Weave.read(server, pos);
        lines.add(Component.translatable("diag.tribalpower.conductor.weave", reading.conductors(), reading.rate()));
        lines.add(Component.translatable("diag.tribalpower.conductor.members", reading.generators(), reading.cairns(),
                reading.totems()));
        if (reading.generators() == 0 && reading.cairns() == 0)
            lines.add(Component.translatable("diag.tribalpower.conductor.no_sources").withStyle(net.minecraft.ChatFormatting.YELLOW));
        else if (!reading.active())
            lines.add(Component.translatable("diag.tribalpower.conductor.dry").withStyle(net.minecraft.ChatFormatting.YELLOW));
        if (carriedLastSecond(now) >= rate())
            lines.add(Component.translatable("diag.tribalpower.conductor.saturated").withStyle(net.minecraft.ChatFormatting.YELLOW));
        return lines;
    }
}
