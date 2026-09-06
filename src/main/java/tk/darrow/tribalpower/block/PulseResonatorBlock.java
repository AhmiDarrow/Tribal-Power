package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
 * Coal-fueled Spirit Pulse generator. Feed coal/charcoal; lattice reads it like a Ley Collector.
 */
public class PulseResonatorBlock extends BaseEntityBlock {
    public static final MapCodec<PulseResonatorBlock> CODEC = simpleCodec(PulseResonatorBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public PulseResonatorBlock(BlockBehaviour.Properties properties) {
        super(properties);
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PulseResonatorBlockEntity resonator)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.getItem() instanceof PulseCellItem) {
            if (!level.isClientSide) {
                int want = Math.min(25, PulseCellItem.CAPACITY - PulseCellItem.getPulse(stack));
                int taken = resonator.extractPulse(want, false);
                int filled = PulseCellItem.insertPulse(stack, taken, false);
                if (filled < taken) {
                    resonator.insertPulse(taken - filled, false);
                }
                player.displayClientMessage(Component.translatable(
                        "message.tribalpower.pulse_cell.charge_resonator",
                        filled,
                        PulseCellItem.getPulse(stack),
                        PulseCellItem.CAPACITY,
                        resonator.getPulseStored()
                ), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (PulseResonatorBlockEntity.isFuel(stack)) {
            if (!level.isClientSide) {
                if (resonator.acceptFuel(stack)) {
                    player.displayClientMessage(Component.translatable(
                            "message.tribalpower.pulse_resonator.fueled",
                            resonator.fuelCount(),
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
                ItemStack taken = resonator.takeFuel();
                if (!taken.isEmpty()) {
                    if (!player.addItem(taken)) {
                        player.drop(taken, false);
                    }
                    player.displayClientMessage(Component.translatable(
                            "message.tribalpower.pulse_resonator.removed_fuel"
                    ), true);
                } else {
                    showStatus(player, resonator);
                }
            } else {
                showStatus(player, resonator);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void showStatus(Player player, PulseResonatorBlockEntity resonator) {
        if (resonator.isBurning() || resonator.fuelCount() > 0) {
            player.displayClientMessage(Component.translatable(
                    "message.tribalpower.pulse_resonator.status",
                    resonator.getPulseStored(),
                    resonator.getPulseCapacity(),
                    resonator.getBurnTime(),
                    resonator.fuelCount()
            ), true);
        } else {
            player.displayClientMessage(Component.translatable(
                    "message.tribalpower.pulse_resonator.need_fuel",
                    resonator.getPulseStored(),
                    resonator.getPulseCapacity()
            ), true);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof PulseResonatorBlockEntity resonator) {
            Containers.dropContents(serverLevel, pos, resonator);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
