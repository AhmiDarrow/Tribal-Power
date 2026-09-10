package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.BlockPos;
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
import tk.darrow.tribalpower.lattice.LatticeNetwork;

import java.util.ArrayList;
import java.util.List;

/**
 * A cairn of stones that holds a beat (design 3.1 section 9.4).
 *
 * <p>Stack them: each cairn in a vertical column carries its own {@link #CAPACITY}, up to
 * {@link #MAX_COLUMN}. The sixth stone on a pile is just a stone -- a cairn is a shape, not a warehouse.
 *
 * <p>It is both sink and source. It draws surplus out of nearby generators and holds it, which is what
 * turns a mob farm's bursty Wake Bell into the steady draw a Listening Pit wants.
 */
public class PulseCairnBlockEntity extends BlockEntity implements PulseHandler, Diagnosable {
    public static final int CAPACITY = 4000;
    public static final int MAX_COLUMN = 5;
    /** How fast one cairn can swallow a burst. Generous, because catching bursts is the entire point. */
    public static final int FILL_RATE = 200;

    private final PulseStorage pulse = new PulseStorage(CAPACITY);
    private int columnIndex = -1;

    public PulseCairnBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PULSE_CAIRN.get(), pos, state);
    }

    /** Position in its own column, counting from the bottom stone. */
    public int columnIndex() {
        if (columnIndex < 0) columnIndex = computeColumnIndex();
        return columnIndex;
    }

    private int computeColumnIndex() {
        if (level == null) return 0;
        int index = 0;
        BlockPos cursor = worldPosition.below();
        while (level.hasChunkAt(cursor) && level.getBlockState(cursor).is(ModBlocks.PULSE_CAIRN.get())) {
            index++;
            if (index > MAX_COLUMN) break;
            cursor = cursor.below();
        }
        return index;
    }

    /** A cairn past the fifth in its column holds nothing at all. */
    public boolean counted() { return columnIndex() < MAX_COLUMN; }

    public void onColumnChanged() { columnIndex = -1; }

    @Override public int getPulseStored() { return counted() ? pulse.getPulseStored() : 0; }
    @Override public int getPulseCapacity() { return counted() ? CAPACITY : 0; }

    @Override
    public int insertPulse(int amount, boolean simulate) {
        if (!counted()) return 0;
        int n = pulse.insertPulse(amount, simulate);
        if (!simulate && n > 0) changed();
        return n;
    }

    @Override
    public int extractPulse(int amount, boolean simulate) {
        if (!counted()) return 0;
        int n = pulse.extractPulse(amount, simulate);
        if (!simulate && n > 0) changed();
        return n;
    }

    private void changed() {
        setChanged();
        if (level != null) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    public int signal() {
        int stored = getPulseStored();
        return stored == 0 ? 0 : 1 + 14 * stored / Math.max(1, getPulseCapacity());
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PulseCairnBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
        if (level.hasNeighborSignal(pos) || !be.counted()) return;
        int room = Math.min(FILL_RATE, be.getPulseCapacity() - be.pulse.getPulseStored());
        if (room <= 0) return;
        // Only ever pull from generators: a cairn hoarding another cairn's stock would be a shell game.
        int taken = LatticeNetwork.extractPulseFromGenerators(level, pos, LatticeNetwork.DEFAULT_RADIUS, room, false);
        if (taken > 0) {
            be.pulse.insertPulse(taken, false);
            be.changed();
        }
    }

    @Override
    public List<Component> diagnose(ServerLevel server, BlockPos pos) {
        List<Component> lines = new ArrayList<>();
        if (!counted()) {
            lines.add(Component.translatable("diag.tribalpower.cairn.too_tall", MAX_COLUMN)
                    .withStyle(net.minecraft.ChatFormatting.YELLOW));
            return lines;
        }
        lines.add(Component.translatable("diag.tribalpower.cairn.column", columnIndex() + 1, MAX_COLUMN));
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
        columnIndex = -1;
    }
}
