package tk.darrow.tribalpower.rite.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.blockentity.SpiritCisternBlockEntity;

/**
 * Spring Calling (design 3.1 section 10): Rain Calling's small sibling, and the answer to "water has to
 * come from somewhere".
 *
 * <p>It marks an adjacent Spirit Cistern a spring: {@link #RATE} mB a second for ten minutes. Cheap and
 * repeatable, and it is what makes the cistern and both fluid relays load-bearing for the Wave Drum, for
 * Washing, and for anything else that drinks.
 *
 * <p>March-side lava stays a manual haul on purpose: the Ember Horn's best fuel should cost a trip.
 */
public final class Springs {
    /** Millibuckets a second a called spring gives back. */
    public static final int RATE = 100;

    private Springs() {}

    /** The cistern this brazier can bless, or null when there is none beside it. */
    @Nullable
    public static SpiritCisternBlockEntity adjacentCistern(ServerLevel level, BlockPos pos) {
        for (Direction face : Direction.values()) {
            BlockPos side = pos.relative(face);
            if (level.hasChunkAt(side) && level.getBlockEntity(side) instanceof SpiritCisternBlockEntity cistern)
                return cistern;
        }
        return null;
    }

    /** Calls a spring into the cistern beside {@code pos}. Returns false when there is nothing to call it into. */
    public static boolean call(ServerLevel level, BlockPos pos, int durationTicks) {
        SpiritCisternBlockEntity cistern = adjacentCistern(level, pos);
        if (cistern == null) return false;
        RiteSavedData.get(level.getServer()).spring(level, cistern.getBlockPos(), level.getGameTime() + durationTicks);
        return true;
    }

    /** One second of every live spring in this dimension. Called from the world rite tick. */
    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 20 != 0) return;
        RiteSavedData data = RiteSavedData.get(level.getServer());
        for (BlockPos pos : data.springs(level)) {
            if (!level.hasChunkAt(pos)) continue;
            if (!(level.getBlockEntity(pos) instanceof SpiritCisternBlockEntity cistern)) continue;
            if (level.hasNeighborSignal(pos)) continue;
            cistern.tank.fill(new FluidStack(Fluids.WATER, RATE), IFluidHandler.FluidAction.EXECUTE);
        }
    }
}
