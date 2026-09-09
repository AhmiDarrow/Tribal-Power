package tk.darrow.tribalpower.world.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A wall-hung stone tablet carrying one of twelve lore fragments. {@code FACING} points away from the wall.
 * Right-click records the tablet in the player's persistent data ({@code TribalTabletsRead} bitmask) and the
 * client opens {@code LoreTabletScreen}.
 */
public class LoreTabletBlock extends BaseEntityBlock {
    public static final MapCodec<LoreTabletBlock> CODEC = simpleCodec(LoreTabletBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final int TABLETS = 12;
    public static final String READ_KEY = "TribalTabletsRead";
    private static final VoxelShape NORTH = Block.box(1, 2, 14, 15, 14, 16);
    private static final VoxelShape SOUTH = Block.box(1, 2, 0, 15, 14, 2);
    private static final VoxelShape WEST = Block.box(14, 2, 1, 16, 14, 15);
    private static final VoxelShape EAST = Block.box(0, 2, 1, 2, 14, 15);

    public LoreTabletBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LoreTabletBlockEntity(pos, state); }

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
        Direction wall = state.getValue(FACING).getOpposite();
        return level.getBlockState(pos.relative(wall)).isFaceSturdy(level, pos.relative(wall), state.getValue(FACING));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)
                ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LoreTabletBlockEntity tablet) {
            if (player.isShiftKeyDown() && player.isCreative()) { // builders cycle the fragment
                tablet.setTablet(tablet.tablet() + 1);
                player.displayClientMessage(Component.translatable("message.tribalpower.lore_tablet.set", tablet.tablet() + 1,
                        Component.translatable("lore.tribalpower.tablet." + tablet.tablet() + ".title")), true);
                return InteractionResult.CONSUME;
            }
            boolean first = markRead(player, tablet.tablet());
            if (first) player.displayClientMessage(Component.translatable("message.tribalpower.lore_tablet.read",
                    Component.translatable("lore.tribalpower.tablet." + tablet.tablet() + ".title"), readCount(player), TABLETS), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Records a tablet as read; returns true the first time. */
    public static boolean markRead(Player player, int tablet) {
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompound(Player.PERSISTED_NBT_TAG);
        int mask = persisted.getInt(READ_KEY);
        int bit = 1 << Math.floorMod(tablet, TABLETS);
        persisted.putInt(READ_KEY, mask | bit);
        data.put(Player.PERSISTED_NBT_TAG, persisted);
        return (mask & bit) == 0;
    }

    public static int readMask(Player player) { return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getInt(READ_KEY); }
    public static int readCount(Player player) { return Integer.bitCount(readMask(player) & ((1 << TABLETS) - 1)); }
    public static boolean hasRead(Player player, int tablet) { return (readMask(player) & (1 << Math.floorMod(tablet, TABLETS))) != 0; }
}
