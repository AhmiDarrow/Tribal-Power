package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import tk.darrow.tribalpower.blockentity.*;

/** Four real stations, retaining the original block IDs and existing world placements. */
public class EchoStationBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final MapCodec<EchoStationBlock> CODEC = simpleCodec(EchoStationBlock::new);

    public EchoStationBlock(Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static final VoxelShape ATTUNE = Block.box(0, 0, 0, 16, 12, 16);
    private static final VoxelShape BIND = Block.box(0, 0, 0, 16, 14, 16);

    /** Hearth models leave the corners open, so a full cube would hide the neighbour's face there. */
    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.is(ModBlocks.ECHO_ATTUNE.get())) return ATTUNE;
        if (state.is(ModBlocks.ECHO_BIND.get())) return BIND;
        return Shapes.block();
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new EchoStationBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.ECHO_STATION.get(), EchoStationBlockEntity::tick);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        tk.darrow.tribalpower.item.MachineRank.onPlacedBy(level, pos, stack);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof EchoStationBlockEntity station) {
            if (player.isShiftKeyDown()) {
                tk.darrow.tribalpower.lattice.HasSideIo.cycle(player, station, hit.getDirection());
            } else player.openMenu(station);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof EchoStationBlockEntity station) {
            Containers.dropContents(level, pos, station);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, next, moved);
    }
    @Override
    protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        return MachineDrops.withSelf(this, super.getDrops(state, builder));
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }
}
