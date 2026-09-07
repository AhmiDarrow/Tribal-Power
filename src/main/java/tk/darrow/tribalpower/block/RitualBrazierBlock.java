package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import tk.darrow.tribalpower.blockentity.*;

/** A sustained camp ritual: seat one seal, provide its totem and Pulse, silence with redstone. */
public class RitualBrazierBlock extends BaseEntityBlock {
    public static final MapCodec<RitualBrazierBlock> CODEC = simpleCodec(RitualBrazierBlock::new);
    public RitualBrazierBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RitualBrazierBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.RITUAL_BRAZIER.get(), RitualBrazierBlockEntity::tick);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if(stack.getItem() instanceof tk.darrow.tribalpower.camp.BoundEffigyItem effigy){
            effigy.useOn(new net.minecraft.world.item.context.UseOnContext(player,hand,hit));
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (RitualBrazierBlockEntity.element(stack) == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RitualBrazierBlockEntity be) {
            if (!be.seal().isEmpty()) {
                player.displayClientMessage(Component.translatable("message.tribalpower.brazier.occupied"), true);
            } else {
                be.setSeal(stack.copyWithCount(1));
                if (!player.isCreative()) stack.shrink(1);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RitualBrazierBlockEntity be) {
            if (player.isShiftKeyDown()) {
                ItemStack seal = be.seal(); be.setSeal(ItemStack.EMPTY);
                if (!player.getInventory().add(seal)) player.drop(seal, false);
            } else player.displayClientMessage(be.status(), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof RitualBrazierBlockEntity be)
            Containers.dropItemStack(level, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, be.seal());
        super.onRemove(state, level, pos, next, moved);
    }
}
