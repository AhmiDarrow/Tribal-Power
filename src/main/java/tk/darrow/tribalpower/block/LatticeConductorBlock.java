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
import tk.darrow.tribalpower.blockentity.LatticeConductorBlockEntity;
import tk.darrow.tribalpower.blockentity.ModBlockEntities;

/**
 * Lattice hub that pushes Spirit Pulse along chalk-linked totems and assists Song Benches on the network.
 */
public class LatticeConductorBlock extends BaseEntityBlock {
    public static final MapCodec<LatticeConductorBlock> CODEC = simpleCodec(LatticeConductorBlock::new);

    public LatticeConductorBlock(BlockBehaviour.Properties properties) {
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
        return new LatticeConductorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? null
                : createTickerHelper(type, ModBlockEntities.LATTICE_CONDUCTOR.get(), LatticeConductorBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof LatticeConductorBlockEntity conductor)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            player.displayClientMessage(conductor.conductOnce(), true);
            if (conductor.getNetworkSize() >= 2 && conductor.getLastPulsePushed() == 0) {
                player.displayClientMessage(Component.translatable("message.tribalpower.conductor.no_pulse"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
