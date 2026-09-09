package tk.darrow.tribalpower.logic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Pulse Gauge: a comparator for Spirit Pulse. The arrow face points at a Pulse handler; every other
 * face emits redstone 0–15 proportional to that handler's stored/capacity.
 */
public class PulseGaugeBlock extends BaseEntityBlock {
    public static final MapCodec<PulseGaugeBlock> CODEC = simpleCodec(PulseGaugeBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public PulseGaugeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // The arrow points into the block the gauge is placed against.
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PulseGaugeBlockEntity(pos, state); }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, LogicRegistry.PULSE_GAUGE_TYPE.get(), PulseGaugeBlockEntity::tick);
    }

    @Override protected boolean isSignalSource(BlockState state) { return true; }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        // `side` points from the receiver toward this block; the arrow face (opposite of FACING) never emits.
        if (side == state.getValue(FACING).getOpposite()) return 0;
        return level.getBlockEntity(pos) instanceof PulseGaugeBlockEntity gauge ? gauge.signal() : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return side == state.getValue(FACING) ? getSignal(state, level, pos, side) : 0;
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof PulseGaugeBlockEntity gauge ? gauge.signal() : 0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PulseGaugeBlockEntity gauge) {
            var handler = PulseGaugeBlockEntity.faced(level, pos, state);
            player.displayClientMessage(handler == null
                    ? Component.translatable("message.tribalpower.logic.no_target")
                    : Component.translatable("message.tribalpower.pulse_gauge.status", handler.getPulseStored(), handler.getPulseCapacity(), gauge.signal()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
