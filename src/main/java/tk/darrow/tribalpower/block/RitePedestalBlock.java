package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import tk.darrow.tribalpower.blockentity.RitePedestalBlockEntity;
import tk.darrow.tribalpower.camp.Ownership;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.rite.RiteHelper;

/**
 * Pedestal for Seals and Rites. Applying a seal by hand still enacts its working, exactly as it did in
 * 3.0; in 3.1 the pedestal also holds and shows one item, which is what lets a Rite Circle be restocked
 * by a relay and fired by a clock (design 3.1 sections 2 and 5).
 */
public class RitePedestalBlock extends BaseEntityBlock {
    public static final MapCodec<RitePedestalBlock> CODEC = simpleCodec(RitePedestalBlock::new);
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);

    public RitePedestalBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RitePedestalBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof RitePedestalBlockEntity pedestal) pedestal.setOwner(Ownership.of(placer));
    }

    /** The six seals a pedestal enacts by hand rather than holds. Unchanged from 3.0. */
    private static boolean isRiteSeal(ItemStack stack) {
        return stack.is(ModItems.BLANK_SEAL.get()) || stack.is(ModItems.SPIRIT_SEAL.get())
                || stack.is(ModItems.EARTH_SEAL.get()) || stack.is(ModItems.FIRE_SEAL.get())
                || stack.is(ModItems.WATER_SEAL.get()) || stack.is(ModItems.AIR_SEAL.get());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.hasNeighborSignal(pos)) return ItemInteractionResult.CONSUME;

        if (isRiteSeal(stack)) {
            if (!level.isClientSide) {
                boolean ok = RiteHelper.performSealRite(level, pos, player, stack);
                player.displayClientMessage(Component.translatable(
                        ok ? "message.tribalpower.rite.success" : "message.tribalpower.rite.fail"
                ), true);
                if (ok && !player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!stack.isEmpty() && level.getBlockEntity(pos) instanceof RitePedestalBlockEntity pedestal
                && pedestal.held().isEmpty()) {
            if (!Ownership.check(level, pedestal.owner(), player)) return ItemInteractionResult.CONSUME;
            if (!level.isClientSide) {
                pedestal.setItem(RitePedestalBlockEntity.SLOT, stack.copyWithCount(1));
                if (!player.isCreative()) stack.shrink(1);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.hasNeighborSignal(pos)) return InteractionResult.CONSUME;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RitePedestalBlockEntity pedestal
                && !pedestal.held().isEmpty()) {
            if (!Ownership.check(level, pedestal.owner(), player)) return InteractionResult.CONSUME;
            ItemStack taken = pedestal.removeItemNoUpdate(RitePedestalBlockEntity.SLOT);
            if (!player.getInventory().add(taken)) player.drop(taken, false);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof RitePedestalBlockEntity pedestal) {
            Containers.dropContents(level, pos, pedestal);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof RitePedestalBlockEntity pedestal ? pedestal.signal() : 0;
    }
}
