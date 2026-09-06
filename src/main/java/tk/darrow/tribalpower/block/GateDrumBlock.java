package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import tk.darrow.tribalpower.world.ModDimensions;

/**
 * Portal drum that opens a passage into The March.
 */
public class GateDrumBlock extends Block {
    public static final MapCodec<GateDrumBlock> CODEC = simpleCodec(GateDrumBlock::new);

    public GateDrumBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            boolean ok = ModDimensions.travelThroughGate(serverPlayer);
            serverPlayer.displayClientMessage(Component.translatable(
                    ok ? "message.tribalpower.gate.travel" : "message.tribalpower.gate.fail"
            ), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
