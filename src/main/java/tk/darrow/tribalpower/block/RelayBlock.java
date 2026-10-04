package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.Containers;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import tk.darrow.tribalpower.blockentity.ModBlockEntities;
import tk.darrow.tribalpower.blockentity.WirelessRelayBlockEntity;

/** Face-mounted wireless plate: sits on a machine face, not a full block. */
public class RelayBlock extends BaseEntityBlock {
    public static final MapCodec<RelayBlock> CODEC = simpleCodec(RelayBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    // FACING is the host face the plate was put on, so the plate lies against the opposite side of its own
    // block space, the way the blockstate rotates the model (as a wall button does). The four wall boxes used
    // to sit on the far side, a hitbox floating a block-width away from the thin plate drawn on the machine.
    private static final VoxelShape[] SHAPES = {
            Block.box(3, 13, 3, 13, 16, 13), // down: hangs under the host above
            Block.box(3, 0, 3, 13, 3, 13),   // up: lies on the host below
            Block.box(3, 3, 13, 13, 13, 16), // north: host is to the south (+Z)
            Block.box(3, 3, 0, 13, 13, 3),   // south: host is to the north (-Z)
            Block.box(13, 3, 3, 16, 13, 13), // west: host is to the east (+X)
            Block.box(0, 3, 3, 3, 13, 13)    // east: host is to the west (-X)
    };

    public RelayBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP)); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction mount = state.getValue(FACING).getOpposite();
        BlockPos support = pos.relative(mount);
        BlockState host = level.getBlockState(support);
        return !host.isAir() && !host.canBeReplaced();
    }
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).ordinal()];
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new WirelessRelayBlockEntity(pos, state); }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        tk.darrow.tribalpower.item.MachineRank.onPlacedBy(level, pos, stack);
        if (level.getBlockEntity(pos) instanceof WirelessRelayBlockEntity relay) relay.setOwner(tk.darrow.tribalpower.camp.Ownership.of(placer));
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.WIRELESS_RELAY.get(), WirelessRelayBlockEntity::tick);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof WirelessRelayBlockEntity relay) {
            // the rune and the filter decide what the relay carries, so only its owner's camp may touch them
            if (!tk.darrow.tribalpower.camp.Ownership.check(level, relay.owner(), player)) return InteractionResult.CONSUME;
            if (player.isShiftKeyDown()) relay.toggleAllow(player);
            else player.openMenu(relay);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof WirelessRelayBlockEntity relay)
            Containers.dropContents(level, pos, relay);
        super.onRemove(state, level, pos, next, moved);
    }
    @Override
    protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        return MachineDrops.withSelfAndBlockEntity(this, super.getDrops(state, builder), builder);
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof WirelessRelayBlockEntity relay ? relay.signal() : 0;
    }
}
