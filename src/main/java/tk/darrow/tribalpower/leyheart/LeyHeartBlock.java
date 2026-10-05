package tk.darrow.tribalpower.leyheart;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.block.MachineDrops;
import tk.darrow.tribalpower.item.PulseCellItem;

/**
 * The Ley Heart's block: right-click to open it, a bucket or a Pulse Cell to use it, sneak with an empty hand to
 * set a face's intake. Breaking it drops its fuel and lowers its six threads.
 */
public class LeyHeartBlock extends BaseEntityBlock {
    public static final MapCodec<LeyHeartBlock> CODEC = simpleCodec(LeyHeartBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public LeyHeartBlock(Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LeyHeartBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, LeyHeartRegistry.LEY_HEART_TYPE.get(), LeyHeartBlockEntity::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        tk.darrow.tribalpower.item.MachineRank.onPlacedBy(level, pos, stack);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof LeyHeartBlockEntity heart))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection()))
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        if (stack.getItem() instanceof PulseCellItem) {
            if (!level.isClientSide) {
                int filled = PulseCellItem.fillFrom(stack, heart);
                player.displayClientMessage(Component.translatable("message.tribalpower.pulse_cell.charge", filled,
                        PulseCellItem.getPulse(stack), PulseCellItem.capacity(stack), state.getBlock().getName(),
                        heart.getPulseStored()), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LeyHeartBlockEntity heart) {
            if (player.isShiftKeyDown() && tk.darrow.tribalpower.lattice.HasSideIo.cycle(player, heart, hit.getDirection()))
                return InteractionResult.SUCCESS;
            player.openMenu(heart);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, block, from, moving);
        // The dais is right underneath: a lifted anchor stone should not wait for the next look.
        if (level.getBlockEntity(pos) instanceof LeyHeartBlockEntity heart) heart.invalidatePattern();
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && level instanceof ServerLevel server) {
            if (level.getBlockEntity(pos) instanceof LeyHeartBlockEntity heart) {
                Containers.dropContents(level, pos, heart);
                level.updateNeighbourForOutputSignal(pos, this);
            }
            // The six threads leave with the heart.
            tk.darrow.tribalpower.rite.world.LeyLines.lower(server, pos);
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Override
    protected java.util.List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        return MachineDrops.withSelfAndBlockEntity(this, super.getDrops(state, builder), builder);
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof LeyHeartBlockEntity heart ? heart.signal() : 0;
    }
}
