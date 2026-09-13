package tk.darrow.tribalpower.device;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import tk.darrow.tribalpower.block.MachineDrops;

public class SealLoomBlock extends BaseEntityBlock {
    public static final MapCodec<SealLoomBlock> CODEC = simpleCodec(SealLoomBlock::new);

    public SealLoomBlock(Properties properties) { super(properties); }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SealLoomBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, DeviceRegistry.SEAL_LOOM_TYPE.get(), SealLoomBlockEntity::tick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown() && !stack.isEmpty() && level.getBlockEntity(pos) instanceof SealLoomBlockEntity loom) {
            if (!level.isClientSide) tk.darrow.tribalpower.lattice.HasSideIo.cycle(player, loom, hit.getDirection());
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SealLoomBlockEntity loom) {
            if (player.isShiftKeyDown()) loom.imprint(player);
            else player.openMenu(loom);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof SealLoomBlockEntity loom) {
            Containers.dropContents(level, pos, loom);
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Override
    protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        return MachineDrops.withSelf(this, super.getDrops(state, builder));
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }
}
