package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.blockentity.GateDrumBlockEntity;

/**
 * The door to The March. It needs no Pulse: beat out the Gate Rite on it (see DrumRite) and the rhythm itself
 * opens the way and carries you through. A redstone signal locks it.
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.hasNeighborSignal(pos)) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.tribalpower.redstone.locked"), true);
            return InteractionResult.CONSUME;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) tk.darrow.tribalpower.gate.DrumRite.begin(serverPlayer, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
