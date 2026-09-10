package tk.darrow.tribalpower.gate;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import tk.darrow.tribalpower.camp.Ownership;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.item.PulseCellItem;
import tk.darrow.tribalpower.item.WaystoneCompassItem;

/**
 * The keystone: the stone in the bottom course that makes a frame into a gate (design 3.1 section 8).
 *
 * <p>Charged from a Pulse Cell like the Gate Drum, lit by a struck redstone signal or by hand, linked with
 * a Waystone Compass or Ritual Chalk locally and with a Gate Sigil across dimensions.
 */
public class GateKeystoneBlock extends BaseEntityBlock {
    public static final MapCodec<GateKeystoneBlock> CODEC = simpleCodec(GateKeystoneBlock::new);

    public GateKeystoneBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GateKeystoneBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, GateRegistry.KEYSTONE_TYPE.get(), GateKeystoneBlockEntity::tick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof GateKeystoneBlockEntity keystone) {
            keystone.setOwner(Ownership.of(placer));
            if (level instanceof ServerLevel server) keystone.record(server);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof GateKeystoneBlockEntity keystone))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!Ownership.check(level, keystone.owner(), player)) return ItemInteractionResult.CONSUME;

        if (stack.getItem() instanceof PulseCellItem) {
            if (!level.isClientSide) {
                int available = PulseCellItem.getPulse(stack);
                int transfer = Math.min(available, Math.min(GateKeystoneBlockEntity.CELL_CHARGE,
                        keystone.getPulseCapacity() - keystone.getPulseStored()));
                if (transfer <= 0) {
                    player.displayClientMessage(Component.translatable(
                            available <= 0 ? "message.tribalpower.gate.cell_empty" : "message.tribalpower.gate.full"), true);
                } else {
                    PulseCellItem.extractPulse(stack, transfer, false);
                    int gained = keystone.insertPulse(transfer, false);
                    player.displayClientMessage(Component.translatable("message.tribalpower.gate.charge",
                            gained, keystone.getPulseStored(), keystone.getPulseCapacity()), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (stack.getItem() instanceof GateSigilItem) {
            return GateSigilItem.useOnKeystone(stack, level, pos, player)
                    ? ItemInteractionResult.sidedSuccess(level.isClientSide) : ItemInteractionResult.CONSUME;
        }

        if (stack.getItem() instanceof WaystoneCompassItem && player.isShiftKeyDown()) {
            return GateLinking.linkWithCompass(stack, level, pos, player)
                    ? ItemInteractionResult.sidedSuccess(level.isClientSide) : ItemInteractionResult.CONSUME;
        }

        if (stack.is(ModItems.RITUAL_CHALK.get())) {
            return GateLinking.linkWithChalk(stack, level, pos, player)
                    ? ItemInteractionResult.sidedSuccess(level.isClientSide) : ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GateKeystoneBlockEntity keystone) {
            if (!Ownership.check(level, keystone.owner(), player)) return InteractionResult.CONSUME;
            ServerLevel server = (ServerLevel) level;
            if (keystone.lit()) {
                keystone.extinguish(server);
                player.displayClientMessage(Component.translatable("message.tribalpower.gate.dark"), true);
            } else {
                Component failure = keystone.light(server);
                player.displayClientMessage(failure != null ? failure.copy().withStyle(net.minecraft.ChatFormatting.RED)
                        : Component.translatable("message.tribalpower.gate.lit"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbour, BlockPos neighbourPos, boolean movedByPiston) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof GateKeystoneBlockEntity keystone)) return;
        keystone.onNeighbourChanged(neighbourPos);
        keystone.onRedstoneChanged((ServerLevel) level);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        return Ownership.canBreak(player.level(), pos, player) && super.canHarvestBlock(state, level, pos, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && level instanceof ServerLevel server
                && level.getBlockEntity(pos) instanceof GateKeystoneBlockEntity keystone) {
            keystone.extinguish(server);
            GateLinking.onKeystoneBroken(server, keystone);
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof GateKeystoneBlockEntity keystone ? keystone.signal(level) : 0;
    }
}
