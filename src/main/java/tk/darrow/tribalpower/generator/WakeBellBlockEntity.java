package tk.darrow.tribalpower.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * The Seal-carvers' craft (design 3.1 section 9.1): mob farms become power plants.
 *
 * <p>Twenty Pulse for a hostile death within {@link #RANGE}, five for a passive one, gathered into a
 * {@link #RESERVOIR} that lets out at most {@link #MAX_RATE} a second. A big farm fills the reservoir and
 * then idles rather than scaling to absurdity (design 3.1 section 9.5).
 */
public class WakeBellBlockEntity extends GeneratorBlockEntity {
    /** The buffer is the generator here: deaths land in it and the bell tolls them out steadily. */
    public static final int CAPACITY = 2000;
    public static final int RESERVOIR = 2000;
    public static final int MAX_RATE = 8;
    public static final int RANGE = 16;
    public static final int HOSTILE = 20;
    public static final int PASSIVE = 5;

    private int reservoir;
    private int lastToll;

    public WakeBellBlockEntity(BlockPos pos, BlockState state) {
        super(GeneratorRegistry.WAKE_BELL_TYPE.get(), pos, state, CAPACITY);
    }

    @Override public tk.darrow.tribalpower.api.pulse.Attunement voice() { return tk.darrow.tribalpower.api.pulse.Attunement.SPIRIT; }

    public int reservoir() { return reservoir; }

    /** A death nearby. Anything past the reservoir's brim is simply lost, which is the cap working. */
    public void mourn(int pulse) {
        if (pulse <= 0) return;
        reservoir = Math.min(RESERVOIR, reservoir + pulse);
        setChanged();
        if (level != null) {
            level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.BELL_RESONATE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.35F, 1.2F);
        }
    }

    @Override
    protected int rawOutput(Level level, BlockPos pos) {
        return Math.min(MAX_RATE, reservoir);
    }

    @Override
    protected void afterProduce(Level level, BlockPos pos, int produced) {
        reservoir = Math.max(0, reservoir - produced);
        lastToll = produced;
    }

    @Override
    public List<Component> breakdown() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("wake.tribalpower.reservoir", reservoir, RESERVOIR));
        lines.add(Component.translatable("wake.tribalpower.rates", HOSTILE, PASSIVE, RANGE));
        lines.add(Component.translatable("wake.tribalpower.total", lastToll, MAX_RATE));
        return lines;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof net.minecraft.server.level.ServerLevel server) WakeBells.add(server, worldPosition);
    }

    @Override
    public void setRemoved() {
        if (level instanceof net.minecraft.server.level.ServerLevel server) WakeBells.remove(server, worldPosition);
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Reservoir", reservoir);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        reservoir = Math.max(0, Math.min(RESERVOIR, tag.getInt("Reservoir")));
    }
}
