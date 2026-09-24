package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.blockentity.TribalBenchBlockEntity;

/**
 * The Tribal Bench: a crafting table two blocks wide that keeps what is left on it.
 *
 * <p>A vanilla table throws the grid on the floor the moment you close it, which is fine for a
 * workbench you visit and wrong for one you work at. The nine places here are part of the block and
 * stay exactly as they were left.
 *
 * <p>The back carries a shelf that holds items the way a Wall Shelf does, so the tools you keep
 * reaching for live on the bench rather than in a chest behind you. Where you click decides which
 * you get: the top opens the grid, the shelf takes and gives back items.
 */
public class TribalBenchBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<TribalBenchBlock> CODEC = simpleCodec(TribalBenchBlock::new);

    /** Which half of the bench this block is, looking along {@link #FACING}. */
    public enum Part implements StringRepresentable {
        LEFT, RIGHT;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    /**
     * Above this the block is shelf, below it is table.
     *
     * <p>The tabletop is 10 high and the shelf board 14 to 16, so anything clicked above 11 is the
     * board and anything at or below it is the working surface.
     */
    public static final double SHELF_Y = 11.0 / 16.0;

    private static final VoxelShape TOP = Block.box(0, 6, 0, 16, 10, 16);
    private static final VoxelShape LEG_NW = Block.box(1, 0, 1, 4, 6, 4);
    private static final VoxelShape LEG_NE = Block.box(12, 0, 1, 15, 6, 4);
    private static final VoxelShape LEG_SW = Block.box(1, 0, 12, 4, 6, 15);
    private static final VoxelShape LEG_SE = Block.box(12, 0, 12, 15, 6, 15);

    public TribalBenchBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, Part.LEFT));
    }

    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    /** A mirrored bench swaps its halves too, or each would look for its partner on the wrong side and fall apart. */
    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        BlockState turned = super.mirror(state, mirror);
        if (mirror == net.minecraft.world.level.block.Mirror.NONE) return turned;
        return turned.setValue(PART, state.getValue(PART) == Part.LEFT ? Part.RIGHT : Part.LEFT);
    }

    /** The other half of this bench. */
    public static BlockPos otherHalf(BlockState state, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        Direction along = state.getValue(PART) == Part.LEFT
                ? facing.getClockWise() : facing.getCounterClockWise();
        return pos.relative(along);
    }

    /** The half that owns the inventory, so both halves open the same bench. */
    public static BlockPos head(BlockState state, BlockPos pos) {
        return state.getValue(PART) == Part.LEFT ? pos : otherHalf(state, pos);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = defaultBlockState().setValue(FACING, facing).setValue(PART, Part.LEFT);
        BlockPos other = otherHalf(state, context.getClickedPos());
        return context.getLevel().getBlockState(other).canBeReplaced(context) ? state : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide)
            level.setBlock(otherHalf(state, pos), state.setValue(PART, Part.RIGHT), 3);
    }

    /** Break one half and the other goes with it, without dropping a second bench. */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour,
                                     LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (neighbourPos.equals(otherHalf(state, pos))
                && (!neighbour.is(this) || neighbour.getValue(PART) == state.getValue(PART)))
            return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Only the half holding the inventory drops it, and only once.
        if (!level.isClientSide && player.isCreative()) {
            BlockPos other = otherHalf(state, pos);
            BlockState there = level.getBlockState(other);
            if (there.is(this) && there.getValue(PART) != state.getValue(PART))
                level.setBlock(other, Blocks.AIR.defaultBlockState(), 35);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = Shapes.or(TOP, LEG_NW, LEG_NE, LEG_SW, LEG_SE);
        return Shapes.or(shape, shelfShape(state.getValue(FACING)));
    }

    /** The shelf board and its posts, standing clear above the back edge of the top. */
    private static VoxelShape shelfShape(Direction facing) {
        return switch (facing) {
            case SOUTH -> Shapes.or(Block.box(0, 10, 2, 16, 16, 4), Block.box(0, 14, 1, 16, 16, 5));
            case WEST -> Shapes.or(Block.box(12, 10, 0, 14, 16, 16), Block.box(11, 14, 0, 15, 16, 16));
            case EAST -> Shapes.or(Block.box(2, 10, 0, 4, 16, 16), Block.box(1, 14, 0, 5, 16, 16));
            default -> Shapes.or(Block.box(0, 10, 12, 16, 16, 14), Block.box(0, 14, 11, 16, 16, 15));
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TribalBenchBlockEntity(pos, state);
    }

    /**
     * Click the top and the grid opens; click the shelf and it behaves like any other shelf.
     *
     * <p>The height of the hit decides, which is the same thing the player sees: the board is above
     * the tabletop and nothing else is up there.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (onShelf(pos, hit)) return CampDisplay.place(stack, state, level, pos, player, hand, hit);
        return openGrid(state, level, pos, player)
                ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (onShelf(pos, hit)) {
            InteractionResult taken = CampDisplay.take(state, level, pos, player, hit);
            if (taken != InteractionResult.PASS) return taken;
        }
        return openGrid(state, level, pos, player)
                ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }

    private static boolean onShelf(BlockPos pos, BlockHitResult hit) {
        return hit.getLocation().y - pos.getY() > SHELF_Y;
    }

    private static boolean openGrid(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return true;
        BlockEntity be = level.getBlockEntity(head(state, pos));
        if (be instanceof MenuProvider provider) {
            player.openMenu(provider);
            return true;
        }
        return false;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TribalBenchBlockEntity bench) bench.dropEverything(level, pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return CampDisplay.signal(level, pos);
    }
}
