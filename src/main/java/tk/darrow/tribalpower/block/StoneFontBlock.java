package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;
import tk.darrow.tribalpower.blockentity.ModBlockEntities;
import tk.darrow.tribalpower.blockentity.StoneFontBlockEntity;
import tk.darrow.tribalpower.camp.Ownership;

/**
 * The Stone Font (design 3.1 section 6): the first pattern, and the block that teaches chalk, shape and
 * Pulse draw before a player owns a single generator.
 */
public class StoneFontBlock extends BaseEntityBlock {
    public static final MapCodec<StoneFontBlock> CODEC = simpleCodec(StoneFontBlock::new);
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);

    public StoneFontBlock(BlockBehaviour.Properties properties) {
        super(properties.noOcclusion());
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StoneFontBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.STONE_FONT.get(), StoneFontBlockEntity::tick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof StoneFontBlockEntity font) font.setOwner(Ownership.of(placer));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof StoneFontBlockEntity font) {
            if (!Ownership.check(level, font.owner(), player)) return ItemInteractionResult.CONSUME;
            // Buckets and cisterns feed the grounded obsidian path: 250 mB water and 250 mB lava.
            if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection()))
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof StoneFontBlockEntity font) {
            if (!Ownership.check(level, font.owner(), player)) return InteractionResult.CONSUME;
            if (player.isShiftKeyDown()) {
                // Sneak empties the font by hand; the pattern is the automation, this is the courtesy.
                for (int slot = 0; slot < font.getContainerSize(); slot++) {
                    ItemStack stack = font.removeItemNoUpdate(slot);
                    if (!stack.isEmpty() && !player.getInventory().add(stack)) player.drop(stack, false);
                }
                level.updateNeighbourForOutputSignal(pos, this);
            }
            player.displayClientMessage(font.status(), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbour, BlockPos neighbourPos, boolean movedByPiston) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof StoneFontBlockEntity font)
            font.onNeighbourChanged(neighbourPos);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        return Ownership.canBreak(player.level(), pos, player) && super.canHarvestBlock(state, level, pos, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof StoneFontBlockEntity font) {
            Containers.dropContents(level, pos, font);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof StoneFontBlockEntity font ? font.progressSignal() : 0;
    }
}
