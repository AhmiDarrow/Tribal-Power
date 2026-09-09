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

/** Reads the faced PulseHandler every 4 ticks and holds a 0–15 redstone level proportional to stored/capacity. */
public class PulseGaugeBlockEntity extends BlockEntity implements Diagnosable {
    public static final int INTERVAL = 4;
    private int signal;

    public PulseGaugeBlockEntity(BlockPos pos, BlockState state) {
        super(LogicRegistry.PULSE_GAUGE_TYPE.get(), pos, state);
    }

    public int signal() { return signal; }

    @Nullable
    public static PulseHandler faced(Level level, BlockPos pos, BlockState state) {
        BlockPos target = pos.relative(state.getValue(PulseGaugeBlock.FACING));
        return level.hasChunkAt(target) && level.getBlockEntity(target) instanceof PulseHandler handler ? handler : null;
    }

    /** Proportional level: 0 when empty, otherwise 1..15 (same curve as the comparator outputs elsewhere in the mod). */
    public static int level(@Nullable PulseHandler handler) {
        if (handler == null || handler.getPulseStored() <= 0) return 0;
        return 1 + 14 * handler.getPulseStored() / Math.max(1, handler.getPulseCapacity());
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PulseGaugeBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % INTERVAL != 0) return;
        int next = level(faced(level, pos, state));
        if (next == be.signal) return;
        be.signal = next;
        be.setChanged();
        level.updateNeighborsAt(pos, state.getBlock());
        level.updateNeighborsAt(pos.relative(state.getValue(PulseGaugeBlock.FACING).getOpposite()), state.getBlock());
    }

    @Override
    public List<Component> diagnose(ServerLevel level, BlockPos pos) {
        PulseHandler handler = faced(level, pos, getBlockState());
        return List.of(handler == null
                ? Component.translatable("diag.tribalpower.logic.no_target", getBlockState().getValue(PulseGaugeBlock.FACING).getSerializedName())
                : Component.translatable("diag.tribalpower.logic.gauge", handler.getPulseStored(), handler.getPulseCapacity(), signal));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Signal", signal);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        signal = Math.max(0, Math.min(15, tag.getInt("Signal")));
    }
}
