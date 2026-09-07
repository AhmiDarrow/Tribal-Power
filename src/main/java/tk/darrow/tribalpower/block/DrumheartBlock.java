package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.blockentity.DrumheartBlockEntity;
import tk.darrow.tribalpower.blockentity.ModBlockEntities;
import tk.darrow.tribalpower.item.PulseCellItem;

/**
 * Early Spirit Pulse generator. Right-click drums a beat; redstone tempo also accumulates pulse.
 */
public class DrumheartBlock extends BaseEntityBlock {
    public static final MapCodec<DrumheartBlock> CODEC = simpleCodec(DrumheartBlock::new);

    public DrumheartBlock(BlockBehaviour.Properties properties) {
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
        return new DrumheartBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.DRUMHEART.get(), DrumheartBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof PulseCellItem
                && level.getBlockEntity(pos) instanceof DrumheartBlockEntity drum) {
            if (!level.isClientSide) {
                int want = Math.min(25, PulseCellItem.capacity(stack) - PulseCellItem.getPulse(stack));
                int taken = drum.extractPulse(want, false);
                int filled = PulseCellItem.insertPulse(stack, taken, false);
                if (filled < taken) {
                    drum.insertPulse(taken - filled, false);
                }
                player.displayClientMessage(Component.translatable(
                        "message.tribalpower.pulse_cell.charge",
                        filled,
                        PulseCellItem.getPulse(stack),
                        PulseCellItem.capacity(stack),
                        drum.getPulseStored()
                ), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof DrumheartBlockEntity drum) {
            if (!level.isClientSide) {
                int gained = drum.drumBeat();
                player.displayClientMessage(Component.translatable(
                        "message.tribalpower.drumheart.beat", gained, drum.getPulseStored(), drum.getPulseCapacity()
                ), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide && level.hasNeighborSignal(pos) && level.getBlockEntity(pos) instanceof DrumheartBlockEntity drum) {
            drum.onRedstonePulse();
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Ambient trickle for mid-game feel once placed near activity; tiny idle gain.
        if (level.getBlockEntity(pos) instanceof DrumheartBlockEntity drum && random.nextInt(4) == 0) {
            drum.insertPulse(1, false);
        }
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
