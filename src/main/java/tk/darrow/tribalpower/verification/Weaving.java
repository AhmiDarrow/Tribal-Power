package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.blockentity.LatticeConductorBlockEntity;
import tk.darrow.tribalpower.item.MachineRank;

/**
 * Pulse travels only along the lattice now (lattice/Weave): a machine beside a generator starves until a Lattice
 * Conductor stands within 8 blocks of both. Tests that power a machine from a generator weave one in with this.
 *
 * <p>The conductor is Manifested by default (4,096 Pulse a second), so a test about a machine is never capped by
 * the lattice's throughput; tests about throughput choose the rank. It goes in the upper middle of the 16x8x16
 * "empty" template, where it reaches the whole floor of the test and stays out of reach of the tests beside it.
 */
final class Weaving {
    /** The spot {@link #weave(GameTestHelper)} uses: within 8 of every block of the test's lower layers. */
    static final BlockPos CENTRE = new BlockPos(8, 6, 8);

    private Weaving() {}

    /** A Manifested conductor at the template's upper middle. */
    static LatticeConductorBlockEntity weave(GameTestHelper h) {
        return conductor(h, CENTRE, MachineRank.MAX);
    }

    /** A Manifested conductor at {@code pos}. */
    static LatticeConductorBlockEntity conductor(GameTestHelper h, BlockPos pos) {
        return conductor(h, pos, MachineRank.MAX);
    }

    /** A conductor of {@code rank} (0 Woven to 3 Manifested) at {@code pos}. */
    static LatticeConductorBlockEntity conductor(GameTestHelper h, BlockPos pos, int rank) {
        h.setBlock(pos, ModBlocks.LATTICE_CONDUCTOR.get());
        var conductor = (LatticeConductorBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
        MachineRank.apply(conductor, rank);
        return conductor;
    }
}
