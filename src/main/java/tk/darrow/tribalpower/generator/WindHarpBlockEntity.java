package tk.darrow.tribalpower.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * The Pattern-weavers' craft (design 3.1 section 9.1): free Pulse, and therefore capped Pulse.
 *
 * <p>Two a second at y 80 under open sky, six a second high in a storm, nothing at all under a roof.
 * Harps within {@link WindMath#CROWD_RANGE} divide the wind between them, so a wall of harps is a wall of
 * decorations (design 3.1 section 9.5).
 */
public class WindHarpBlockEntity extends GeneratorBlockEntity {
    public static final int CAPACITY = 1000;

    public WindHarpBlockEntity(BlockPos pos, BlockState state) {
        super(GeneratorRegistry.WIND_HARP_TYPE.get(), pos, state, CAPACITY);
    }

    @Override public tk.darrow.tribalpower.api.pulse.Attunement voice() { return tk.darrow.tribalpower.api.pulse.Attunement.AIR; }

    @Override
    protected int rawOutput(Level level, BlockPos pos) {
        return WindMath.gain(level, pos);
    }

    @Override
    protected void afterProduce(Level level, BlockPos pos, int produced) {
        if (produced <= 0) return;
        float pitch = 0.85F + Math.min(0.5F, pos.getY() / 256.0F);
        tk.darrow.tribalpower.sound.ModSounds.play(level, pos,
                tk.darrow.tribalpower.sound.ModSounds.WIND_HARP_STRING, 0.28F, pitch);
    }

    @Override
    public List<Component> breakdown() {
        return level == null ? List.of() : WindMath.breakdown(level, worldPosition);
    }
}
