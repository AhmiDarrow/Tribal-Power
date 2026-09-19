package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Gate Drum holds no Pulse: the Gate Rite beaten on it is what opens the way (see DrumRite). The block entity
 * stays so drums placed by earlier versions still load; any Pulse they held is simply no longer needed.
 */
public class GateDrumBlockEntity extends BlockEntity {
    public GateDrumBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GATE_DRUM.get(), pos, state);
    }
}
