package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.blockentity.CampDisplayBlockEntity;
import tk.darrow.tribalpower.entity.SeatEntity;

/**
 * Camp furniture: stool, table, urn. Optional facing for the table's grain. The table and the urn
 * hold what you set on them ({@link CampDisplay}); the stool is for sitting on and holds nothing.
 */
public class CampDecorBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public enum Kind {
        STOOL(Shapes.or(Block.box(4, 0, 4, 6, 8, 6), Block.box(10, 0, 4, 12, 8, 6),
                Block.box(4, 0, 10, 6, 8, 12), Block.box(10, 0, 10, 12, 8, 12),
                Block.box(3, 8, 3, 13, 10, 13))),
        TABLE(Shapes.or(Block.box(1, 0, 1, 3, 13, 3), Block.box(13, 0, 1, 15, 13, 3),
                Block.box(1, 0, 13, 3, 13, 15), Block.box(13, 0, 13, 15, 13, 15),
                Block.box(0, 13, 0, 16, 16, 16))),
        URN(Shapes.or(Block.box(4, 0, 4, 12, 10, 12), Block.box(5, 10, 5, 11, 12, 11),
                Block.box(6, 12, 6, 10, 14, 10)));

        public final VoxelShape shape;

        Kind(VoxelShape shape) {
            this.shape = shape;
        }
    }

    private final Kind kind;
    private final MapCodec<CampDecorBlock> codec;

    public CampDecorBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        this.codec = simpleCodec(p -> new CampDecorBlock(kind, p));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return kind.shape;
    }

    /** The stool's seat is 10/16 up; sit a little into it so the player is not perched on air. */
    private static final double SEAT_HEIGHT = 0.4;

    /** A stool is for sitting on; only the table and the urn have somewhere to put something. */
    private boolean holds() {
        return kind != Kind.STOOL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return holds() ? new CampDisplayBlockEntity(pos, state) : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        return holds()
                ? CampDisplay.place(stack, state, level, pos, player, hand, hit)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (holds()) return CampDisplay.take(state, level, pos, player, hit);
        if (player.isPassenger() || player.isCrouching()) return InteractionResult.PASS;
        return SeatEntity.sit(level, pos, player, SEAT_HEIGHT)
                ? InteractionResult.sidedSuccess(level.isClientSide)
                : InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (holds()) CampDisplay.dropContents(state, level, pos, newState, this);
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return holds();
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return holds() ? CampDisplay.signal(level, pos) : 0;
    }
}
