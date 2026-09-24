package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/** A door that swings its twin with it: two doors hinged apart and facing the same way open as one. Sneak to move just one. */
public class PairedDoorBlock extends DoorBlock {
    public static final MapCodec<PairedDoorBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockSetType.CODEC.fieldOf("block_set_type").forGetter(DoorBlock::type),
            propertiesCodec()).apply(i, PairedDoorBlock::new));

    public PairedDoorBlock(BlockSetType type, Properties properties) {
        super(type, properties);
    }

    @Override
    public MapCodec<? extends DoorBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        InteractionResult result = super.useWithoutItem(state, level, pos, player, hit);
        if (!result.consumesAction() || player.isSecondaryUseActive()) return result;

        BlockState now = level.getBlockState(pos);
        if (!(now.getBlock() instanceof DoorBlock)) return result;
        boolean open = now.getValue(OPEN);
        Direction facing = now.getValue(FACING);
        // A right-hinged door's twin stands to its left (counter-clockwise), and the reverse.
        Direction side = now.getValue(HINGE) == DoorHingeSide.RIGHT ? facing.getCounterClockWise() : facing.getClockWise();
        BlockPos twinPos = pos.relative(side);
        BlockState twin = level.getBlockState(twinPos);
        if (twin.getBlock() instanceof DoorBlock door && door.type().canOpenByHand()
                && twin.getValue(FACING) == facing
                && twin.getValue(HINGE) != now.getValue(HINGE)
                && twin.getValue(HALF) == now.getValue(HALF)
                && twin.getValue(OPEN) != open) {
            level.setBlock(twinPos, twin.setValue(OPEN, open), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
            level.gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, twinPos);
        }
        return result;
    }
}
