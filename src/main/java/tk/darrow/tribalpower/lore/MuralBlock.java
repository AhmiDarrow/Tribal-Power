package tk.darrow.tribalpower.lore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
 * A carved mural: a two-by-two panel on a wall, one fragment of the Chronicle painted across it. {@code PART}
 * says which quarter a block is (0 bottom-left, 1 bottom-right, 2 top-left, 3 top-right, seen from the front);
 * placing one lays all four, breaking any takes them all, and reading any reads the mural.
 */
public class MuralBlock extends HorizontalDirectionalBlock {
    public static final com.mojang.serialization.MapCodec<MuralBlock> CODEC = simpleCodec(MuralBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty FRAGMENT = CarvedStoneBlock.FRAGMENT;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 3);
    private static final VoxelShape NORTH = Block.box(0, 0, 13, 16, 16, 16), SOUTH = Block.box(0, 0, 0, 16, 16, 3),
            WEST = Block.box(13, 0, 0, 16, 16, 16), EAST = Block.box(0, 0, 0, 3, 16, 16);

    public MuralBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FRAGMENT, 0).setValue(PART, 0));
    }

    @Override protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, FRAGMENT, PART); }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) { case SOUTH -> SOUTH; case WEST -> WEST; case EAST -> EAST; default -> NORTH; };
    }

    /** The mural's right, seen from the front: the direction from part 0 to part 1. */
    public static Direction right(Direction facing) { return facing.getCounterClockWise(); }

    /** Where each part of a mural sits, from the position of part 0. */
    public static BlockPos partPos(BlockPos origin, Direction facing, int part) {
        BlockPos pos = origin;
        if ((part & 1) != 0) pos = pos.relative(right(facing));
        if ((part & 2) != 0) pos = pos.above();
        return pos;
    }

    /** Position of part 0 for a block that is some part of a mural. */
    public static BlockPos origin(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        int part = state.getValue(PART);
        BlockPos origin = pos;
        if ((part & 1) != 0) origin = origin.relative(right(facing).getOpposite());
        if ((part & 2) != 0) origin = origin.below();
        return origin;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (!face.getAxis().isHorizontal()) face = context.getHorizontalDirection().getOpposite();
        BlockPos origin = context.getClickedPos();
        for (int part = 1; part < 4; part++) {
            BlockPos at = partPos(origin, face, part);
            if (!context.getLevel().getBlockState(at).canBeReplaced() || !supported(context.getLevel(), at, face)) return null;
        }
        return supported(context.getLevel(), origin, face) ? defaultBlockState().setValue(FACING, face) : null;
    }

    private static boolean supported(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos behind = pos.relative(facing.getOpposite());
        return level.getBlockState(behind).isFaceSturdy(level, behind, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        for (int part = 1; part < 4; part++) level.setBlock(partPos(pos, state.getValue(FACING), part), state.setValue(PART, part), 3);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) { return supported(level, pos, state.getValue(FACING)); }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockPos origin = origin(pos, state);
            for (int part = 0; part < 4; part++) {
                BlockPos at = partPos(origin, state.getValue(FACING), part);
                if (!at.equals(pos) && level.getBlockState(at).is(this)) level.setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 35);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isCreative() && player.isShiftKeyDown()) {
            int next = (state.getValue(FRAGMENT) + 1) % Chronicle.FRAGMENTS;
            BlockPos origin = origin(pos, state);
            for (int part = 0; part < 4; part++) {
                BlockPos at = partPos(origin, state.getValue(FACING), part);
                BlockState there = level.getBlockState(at);
                if (there.is(this)) level.setBlock(at, there.setValue(FRAGMENT, next), 3);
            }
            return InteractionResult.CONSUME;
        }
        Chronicle.read(player, state.getValue(FRAGMENT));
        return InteractionResult.CONSUME;
    }
}
