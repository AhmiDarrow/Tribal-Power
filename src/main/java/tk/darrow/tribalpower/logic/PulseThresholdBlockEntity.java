package tk.darrow.tribalpower.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.api.Diagnosable;
import tk.darrow.tribalpower.api.pulse.PulseHandler;

import java.util.List;

/** Emits redstone while the faced PulseHandler holds at least the chosen fraction of its capacity. */
public class PulseThresholdBlockEntity extends BlockEntity implements Diagnosable {
    public static final int INTERVAL = 4;
    public static final int[] THRESHOLDS = {25, 50, 75, 100};
    private int index = 1;
    private boolean powered;

    public PulseThresholdBlockEntity(BlockPos pos, BlockState state) {
        super(LogicRegistry.PULSE_THRESHOLD_TYPE.get(), pos, state);
    }

    public int index() { return index; }
    public int threshold() { return THRESHOLDS[index]; }
    public boolean powered() { return powered; }

    /** Cycle 25 → 50 → 75 → 100 → 25. */
    public int cycle() {
        index = (index + 1) % THRESHOLDS.length;
        setChanged();
        if (level != null) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            refresh(level, worldPosition, getBlockState(), this);
        }
        return threshold();
    }

    @Nullable
    public static PulseHandler faced(Level level, BlockPos pos, BlockState state) {
        BlockPos target = pos.relative(state.getValue(PulseThresholdBlock.FACING));
        return level.hasChunkAt(target) && level.getBlockEntity(target) instanceof PulseHandler handler ? handler : null;
    }

    public static boolean meets(@Nullable PulseHandler handler, int percent) {
        if (handler == null) return false;
        long stored = handler.getPulseStored(), capacity = Math.max(1, handler.getPulseCapacity());
        return stored * 100L >= capacity * percent;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PulseThresholdBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % INTERVAL != 0) return;
        refresh(level, pos, state, be);
    }

    private static void refresh(Level level, BlockPos pos, BlockState state, PulseThresholdBlockEntity be) {
        boolean next = meets(faced(level, pos, state), be.threshold());
        if (next == be.powered) return;
        be.powered = next;
        be.setChanged();
        if (state.getValue(PulseThresholdBlock.POWERED) != next)
            level.setBlock(pos, state.setValue(PulseThresholdBlock.POWERED, next), 3);
        level.updateNeighborsAt(pos, state.getBlock());
        level.updateNeighborsAt(pos.relative(state.getValue(PulseThresholdBlock.FACING).getOpposite()), state.getBlock());
    }

    @Override
    public List<Component> diagnose(ServerLevel level, BlockPos pos) {
        PulseHandler handler = faced(level, pos, getBlockState());
        if (handler == null)
            return List.of(Component.translatable("diag.tribalpower.logic.no_target", getBlockState().getValue(PulseThresholdBlock.FACING).getSerializedName()));
        return List.of(Component.translatable("diag.tribalpower.logic.threshold", threshold(),
                handler.getPulseStored() * 100L / Math.max(1, handler.getPulseCapacity()),
                Component.translatable(powered ? "diag.tribalpower.logic.on" : "diag.tribalpower.logic.off")));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Threshold", index);
        tag.putBoolean("Powered", powered);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        index = Math.max(0, Math.min(THRESHOLDS.length - 1, tag.getInt("Threshold")));
        powered = tag.getBoolean("Powered");
    }
}
