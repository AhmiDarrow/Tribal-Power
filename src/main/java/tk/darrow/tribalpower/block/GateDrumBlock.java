package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.blockentity.GateDrumBlockEntity;
import tk.darrow.tribalpower.item.PulseCellItem;
import tk.darrow.tribalpower.storage.DeepCacheManager;
import tk.darrow.tribalpower.world.ModDimensions;

/**
 * Portal drum that opens a passage into The March. Stores Pulse to fuel travel.
 */
public class GateDrumBlock extends BaseEntityBlock {
    public static final MapCodec<GateDrumBlock> CODEC = simpleCodec(GateDrumBlock::new);

    public GateDrumBlock(BlockBehaviour.Properties properties) {
        super(properties.noOcclusion());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GateDrumBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit
    ) {
        if (stack.getItem() instanceof PulseCellItem && level.getBlockEntity(pos) instanceof GateDrumBlockEntity drum) {
            if (!level.isClientSide) {
                int available = PulseCellItem.getPulse(stack);
                int room = drum.getPulseCapacity() - drum.getPulseStored();
                int transfer = Math.min(available, Math.min(room, GateDrumBlockEntity.CELL_CHARGE));
                if (transfer <= 0) {
                    player.displayClientMessage(Component.translatable(
                            available <= 0 ? "message.tribalpower.gate.cell_empty" : "message.tribalpower.gate.full"
                    ), true);
                } else {
                    PulseCellItem.extractPulse(stack, transfer, false);
                    int gained = drum.insertPulse(transfer, false);
                    player.displayClientMessage(Component.translatable(
                            "message.tribalpower.gate.charge", gained, drum.getPulseStored(), drum.getPulseCapacity()
                    ), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.hasNeighborSignal(pos)) {
            if (!level.isClientSide) player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.tribalpower.redstone.locked"), true);
            return InteractionResult.CONSUME;
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof GateDrumBlockEntity drum) {
            if (player.isShiftKeyDown()) {
                int gained = drum.manualCharge();
                serverPlayer.displayClientMessage(Component.translatable(
                        "message.tribalpower.gate.charge", gained, drum.getPulseStored(), drum.getPulseCapacity()
                ), true);
                return InteractionResult.CONSUME;
            }

            if (!drum.tryConsumeTravelPulse()) {
                serverPlayer.displayClientMessage(Component.translatable(
                        "message.tribalpower.gate.need_pulse", GateDrumBlockEntity.TRAVEL_COST, drum.getPulseStored()
                ), true);
                return InteractionResult.CONSUME;
            }

            boolean ok = ModDimensions.travelThroughGate(serverPlayer);
            if (ok) {
                DeepCacheManager.markVisited(serverPlayer);
                serverPlayer.displayClientMessage(Component.translatable("message.tribalpower.gate.travel"), true);
            } else {
                drum.insertPulse(GateDrumBlockEntity.TRAVEL_COST, false);
                serverPlayer.displayClientMessage(Component.translatable("message.tribalpower.gate.fail"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
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
