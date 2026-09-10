package tk.darrow.tribalpower.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.api.Diagnosable;
import tk.darrow.tribalpower.api.pulse.PulseGenerator;
import tk.darrow.tribalpower.api.pulse.PulseStorage;
import tk.darrow.tribalpower.config.TribalConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared shape for the six voices (design 3.1 section 9): one beat a second, staggered by position, a
 * held redstone signal stills it, and every one of them is a {@link PulseGenerator} so gauges, thresholds,
 * cairns and conductors need no special case.
 *
 * <p>Each voice generates from its own kind of input. None of them is a flat multiplier on another.
 */
public abstract class GeneratorBlockEntity extends BlockEntity implements PulseGenerator, Diagnosable {
    protected final PulseStorage pulse;

    protected GeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state);
        this.pulse = new PulseStorage(capacity);
    }

    /** Redstone held high stills the work. Struck signals mean nothing to a generator. */
    public boolean stilled() {
        return level != null && level.hasNeighborSignal(worldPosition);
    }

    /** What this generator would make this second, before the config multiplier and before stilling. */
    protected abstract int rawOutput(Level level, BlockPos pos);

    /** Called once a second when output was actually produced, for input the voice consumes. */
    protected void afterProduce(Level level, BlockPos pos, int produced) {}

    @Override
    public int currentOutput() {
        if (level == null || stilled()) return 0;
        return TribalConfig.scaleGeneration(rawOutput(level, worldPosition));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GeneratorBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
        if (be.stilled()) return;
        int output = TribalConfig.scaleGeneration(be.rawOutput(level, pos));
        if (output <= 0) return;
        int accepted = be.pulse.insertPulse(output, false);
        be.afterProduce(level, pos, accepted);
        be.setChanged();
        level.updateNeighbourForOutputSignal(pos, state.getBlock());
    }

    // ---- PulseHandler ----------------------------------------------------------------------

    @Override public int getPulseStored() { return pulse.getPulseStored(); }
    @Override public int getPulseCapacity() { return pulse.getPulseCapacity(); }

    @Override
    public int insertPulse(int amount, boolean simulate) {
        int n = pulse.insertPulse(amount, simulate);
        if (!simulate && n > 0) changed();
        return n;
    }

    @Override
    public int extractPulse(int amount, boolean simulate) {
        int n = pulse.extractPulse(amount, simulate);
        if (!simulate && n > 0) changed();
        return n;
    }

    protected void changed() {
        setChanged();
        if (level != null) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    /** Comparator: how full the generator's own buffer is. */
    public int signal() {
        int stored = getPulseStored();
        return stored == 0 ? 0 : 1 + 14 * stored / Math.max(1, getPulseCapacity());
    }

    public Component status() {
        return Component.translatable("message.tribalpower.generator.status",
                getBlockState().getBlock().getName(), currentOutput(), getPulseStored(), getPulseCapacity());
    }

    @Override
    public List<Component> diagnose(ServerLevel server, BlockPos pos) {
        List<Component> lines = new ArrayList<>(breakdown());
        if (stilled()) lines.add(Component.translatable("diag.tribalpower.paused").withStyle(net.minecraft.ChatFormatting.RED));
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
