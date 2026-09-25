package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import tk.darrow.tribalpower.blockentity.ModBlockEntities;
import tk.darrow.tribalpower.blockentity.PulseCairnBlockEntity;

/** Stones that hold a beat (design 3.1 section 9.4). Touching stones are one pile and one store. */
public class PulseCairnBlock extends BaseEntityBlock {
    public static final MapCodec<PulseCairnBlock> CODEC = simpleCodec(PulseCairnBlock::new);
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    public PulseCairnBlock(BlockBehaviour.Properties properties) {
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
        return new PulseCairnBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.PULSE_CAIRN.get(), PulseCairnBlockEntity::tick);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state,
            Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof tk.darrow.tribalpower.item.PulseCellItem
                && level.getBlockEntity(pos) instanceof PulseCairnBlockEntity cairn) {
            if (!level.isClientSide) {
                // The cell drinks from the whole pile, as far as it has room and the pile holds.
                int filled = tk.darrow.tribalpower.item.PulseCellItem.fillFrom(stack, cairn);
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.tribalpower.pulse_cell.charge", filled,
                        tk.darrow.tribalpower.item.PulseCellItem.getPulse(stack),
                        tk.darrow.tribalpower.item.PulseCellItem.capacity(stack),
                        state.getBlock().getName(), cairn.getPulseStored()), true);
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PulseCairnBlockEntity cairn) {
            PulseCairnBlockEntity.Pile pile = cairn.pile();
            player.displayClientMessage(pile.overflow()
                    ? net.minecraft.network.chat.Component.translatable("message.tribalpower.cairn.overflow",
                            pile.stored(), pile.capacity(), PulseCairnBlockEntity.MAX_GROUP)
                    : net.minecraft.network.chat.Component.translatable("message.tribalpower.cairn.status",
                            pile.stored(), pile.capacity(), pile.size()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override
    protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        return MachineDrops.withSelf(this, super.getDrops(state, builder));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer,
                            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        // A new stone joins every pile it touches; they flood again on their next reading.
        if (!level.isClientSide) PulseCairnBlockEntity.invalidateAround(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        // The stone's own share leaves with it; the piles it touched just shrink.
        if (!level.isClientSide && !state.is(newState.getBlock())) PulseCairnBlockEntity.invalidateAround(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbour, BlockPos neighbourPos, boolean movedByPiston) {
        // Only a stone coming or going beside this one changes the pile; anything else keeps the cache.
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PulseCairnBlockEntity cairn)
            cairn.onNeighbourChanged(neighbourPos);
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof PulseCairnBlockEntity cairn ? cairn.signal() : 0;
    }
}
