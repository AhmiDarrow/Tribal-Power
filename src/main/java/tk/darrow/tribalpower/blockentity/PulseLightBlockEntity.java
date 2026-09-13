package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.block.PulseLightBlock;
import tk.darrow.tribalpower.lattice.LatticeNetwork;

/** Draws a trickle of Pulse each second. No buffer: nearby lattice or it goes dark. */
public class PulseLightBlockEntity extends BlockEntity {
    public PulseLightBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PULSE_LIGHT.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PulseLightBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
        int cost = state.getBlock() instanceof PulseLightBlock light ? light.kind().cost : 1;
        boolean want = !level.hasNeighborSignal(pos)
                && LatticeNetwork.extractPulseNearby(level, pos, LatticeNetwork.DEFAULT_RADIUS, cost, true) >= cost;
        if (want) LatticeNetwork.extractPulseNearby(level, pos, LatticeNetwork.DEFAULT_RADIUS, cost, false);
        if (state.getValue(PulseLightBlock.LIT) != want) {
            level.setBlock(pos, state.setValue(PulseLightBlock.LIT, want), 3);
        }
    }
}
