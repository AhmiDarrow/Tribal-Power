package tk.darrow.tribalpower.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.blockentity.ResonanceTotemBlockEntity;
import tk.darrow.tribalpower.lattice.LatticeNetwork;
import tk.darrow.tribalpower.rite.world.LeyLines;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The Loom-stitchers' craft (design 3.1 section 9.1): the endgame generator, which pays for completeness.
 *
 * <p>One Pulse a second per distinct voice within eight blocks, to a maximum of six, doubled while a Ley
 * Binding is live nearby. It generates from the system itself, so it is naturally capped at the number of
 * voices there are and needs no density rule of its own.
 */
public class LoomAnchorBlockEntity extends GeneratorBlockEntity {
    public static final int CAPACITY = 1000;
    public static final int MAX_VOICES = 6;

    public LoomAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(GeneratorRegistry.LOOM_ANCHOR_TYPE.get(), pos, state, CAPACITY);
    }

    @Override public tk.darrow.tribalpower.api.pulse.Attunement voice() { return tk.darrow.tribalpower.api.pulse.Attunement.LOOM; }

    public int voices(Level level, BlockPos pos) {
        Set<Attunement> voices = LatticeNetwork.collectAttunements(level, pos, LatticeNetwork.DEFAULT_RADIUS);
        return Math.min(MAX_VOICES, voices.size());
    }

    /** True while a live ley line touches any totem near this anchor. */
    public boolean bound(Level level, BlockPos pos) {
        for (ResonanceTotemBlockEntity totem : LatticeNetwork.findNearbyTotems(level, pos, LatticeNetwork.DEFAULT_RADIUS))
            if (!LeyLines.linked(level, totem.getBlockPos()).isEmpty()) return true;
        return false;
    }

    @Override
    protected int rawOutput(Level level, BlockPos pos) {
        int voices = voices(level, pos);
        return voices <= 0 ? 0 : voices * (bound(level, pos) ? 2 : 1);
    }

    @Override
    protected void afterProduce(Level level, BlockPos pos, int produced) {
        // The endgame generator is the natural place to notice a full elemental plant.
        if (level instanceof net.minecraft.server.level.ServerLevel server) SixVoices.check(server, pos);
    }

    @Override
    public List<Component> breakdown() {
        List<Component> lines = new ArrayList<>();
        if (level == null) return lines;
        lines.add(Component.translatable("loom.tribalpower.voices", voices(level, worldPosition), MAX_VOICES));
        lines.add(Component.translatable(bound(level, worldPosition)
                ? "loom.tribalpower.bound" : "loom.tribalpower.unbound"));
        lines.add(Component.translatable("loom.tribalpower.total", currentOutput()));
        return lines;
    }
}
