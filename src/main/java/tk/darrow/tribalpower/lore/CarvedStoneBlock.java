package tk.darrow.tribalpower.lore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A carved stone set into a ruin's wall, carrying one fragment of the Chronicle. {@code FACING} points away from
 * the wall; {@code FRAGMENT} says which. Right-click reads it; in creative, sneak-click cycles it.
 */
public class CarvedStoneBlock extends HorizontalDirectionalBlock {
    public static final com.mojang.serialization.MapCodec<CarvedStoneBlock> CODEC = simpleCodec(CarvedStoneBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty FRAGMENT = IntegerProperty.create("fragment", 0, Chronicle.FRAGMENTS - 1);
    private static final VoxelShape NORTH = Block.box(1, 1, 13, 15, 15, 16), SOUTH = Block.box(1, 1, 0, 15, 15, 3),
            WEST = Block.box(13, 1, 1, 16, 15, 15), EAST = Block.box(0, 1, 1, 3, 15, 15);

    public CarvedStoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FRAGMENT, 0));
    }

    @Override protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, FRAGMENT); }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) { case SOUTH -> SOUTH; case WEST -> WEST; case EAST -> EAST; default -> NORTH; };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (!face.getAxis().isHorizontal()) face = context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, face);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos behind = pos.relative(facing.getOpposite());
        return level.getBlockState(behind).isFaceSturdy(level, behind, facing);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isCreative() && player.isShiftKeyDown()) {
            level.setBlock(pos, state.setValue(FRAGMENT, (state.getValue(FRAGMENT) + 1) % Chronicle.FRAGMENTS), 3);
            return InteractionResult.CONSUME;
        }
        Chronicle.read(player, state.getValue(FRAGMENT));
        return InteractionResult.CONSUME;
    }
}
