package tk.darrow.tribalpower.block;

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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.blockentity.ModBlockEntities;
import tk.darrow.tribalpower.blockentity.PulseResonatorBlockEntity;
import tk.darrow.tribalpower.item.PulseCellItem;

/**
 * Harmonic Spirit Pulse generator. Seat a reusable Echo catalyst and place two distinct totem voices nearby.
 */
public class PulseResonatorBlock extends BaseEntityBlock {
    public static final MapCodec<PulseResonatorBlock> CODEC = simpleCodec(PulseResonatorBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public PulseResonatorBlock(BlockBehaviour.Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(LIT, false);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PulseResonatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.PULSE_RESONATOR.get(), PulseResonatorBlockEntity::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        tk.darrow.tribalpower.item.MachineRank.onPlacedBy(level, pos, stack);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PulseResonatorBlockEntity resonator)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.getItem() instanceof PulseCellItem) {
            if (!level.isClientSide) {
                int want = Math.min(100, PulseCellItem.capacity(stack) - PulseCellItem.getPulse(stack));
                int taken = resonator.extractPulse(want, false);
                int filled = PulseCellItem.insertPulse(stack, taken, false);
                if (filled < taken) {
                    resonator.insertPulse(taken - filled, false);
                }
                player.displayClientMessage(Component.translatable(
                        "message.tribalpower.pulse_cell.charge_resonator",
                        filled,
                        PulseCellItem.getPulse(stack),
                        PulseCellItem.capacity(stack),
                        state.getBlock().getName(),
                        resonator.getPulseStored()
                ), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (PulseResonatorBlockEntity.isCatalyst(stack)) {
            if (!level.isClientSide) {
                if (resonator.acceptCatalyst(player.isCreative() ? stack.copy() : stack)) {
                    var seated = resonator.getItem(PulseResonatorBlockEntity.SLOT);
                    player.displayClientMessage(Component.translatable(
                            "message.tribalpower.pulse_resonator.fueled",
                            seated.getHoverName(),
                            PulseResonatorBlockEntity.catalystRank(seated),
                            resonator.getPulseStored(),
                            resonator.getPulseCapacity()
                    ), true);
                } else {
                    player.displayClientMessage(Component.translatable(
                            "message.tribalpower.pulse_resonator.full_fuel"
                    ), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PulseResonatorBlockEntity resonator)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                ItemStack taken = resonator.takeCatalyst();
                if (!taken.isEmpty()) {
                    tk.darrow.tribalpower.item.SpiritgearHelper.give(player, taken);
                    player.displayClientMessage(Component.translatable(
                            "message.tribalpower.pulse_resonator.removed_fuel"
                    ), true);
                } else {
                    showStatus(player, resonator, level, pos);
                }
            } else {
                showStatus(player, resonator, level, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void showStatus(Player player, PulseResonatorBlockEntity resonator, Level level, BlockPos pos) {
        if (resonator.catalystCount() == 0) {
            player.displayClientMessage(Component.translatable(
                    "message.tribalpower.pulse_resonator.need_fuel",
                    resonator.getPulseStored(),
                    resonator.getPulseCapacity()
            ), true);
            return;
        }
        if (level.hasNeighborSignal(pos)) {
            player.displayClientMessage(Component.translatable("message.tribalpower.pulse_resonator.paused"), true);
            return;
        }
        if (resonator.getHarmonics() < 2) {
            player.displayClientMessage(Component.translatable(
                    "message.tribalpower.pulse_resonator.need_voices",
                    resonator.getHarmonics()
            ), true);
            return;
        }
        player.displayClientMessage(Component.translatable(
                "message.tribalpower.pulse_resonator.status",
                resonator.getPulseStored(),
                resonator.getPulseCapacity(),
                resonator.getGain(),
                resonator.getHarmonics()
        ), true);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof PulseResonatorBlockEntity resonator) {
            resonator.stampWear();
            Containers.dropContents(serverLevel, pos, resonator);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
    @Override
    protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        return MachineDrops.withSelf(this, super.getDrops(state, builder));
    }
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof tk.darrow.tribalpower.api.pulse.PulseHandler pulse)
            return pulse.getPulseStored() == 0 ? 0 : 1 + 14 * pulse.getPulseStored() / Math.max(1, pulse.getPulseCapacity());
        return 0;
    }
}
