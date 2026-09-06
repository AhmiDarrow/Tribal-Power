package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import tk.darrow.tribalpower.blockentity.LeyCollectorBlockEntity;
import tk.darrow.tribalpower.blockentity.ModBlockEntities;

/**
 * Ambient Pulse collector. Right-click reports stored Spirit Pulse.
 */
public class LeyCollectorBlock extends BaseEntityBlock {
    public static final MapCodec<LeyCollectorBlock> CODEC = simpleCodec(LeyCollectorBlock::new);

    public LeyCollectorBlock(BlockBehaviour.Properties properties) {
        super(properties);
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
        return new LeyCollectorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.LEY_COLLECTOR.get(), LeyCollectorBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LeyCollectorBlockEntity ley) {
            player.displayClientMessage(Component.translatable(
                    "message.tribalpower.ley_collector.status",
                    ley.getPulseStored(),
                    ley.getPulseCapacity()
            ), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
