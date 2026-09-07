package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import tk.darrow.tribalpower.storage.DeepCacheContainer;
import tk.darrow.tribalpower.storage.DeepCacheManager;

/**
 * Overworld/March terminal into per-player spirit-linked storage.
 */
public class DeepCacheBlock extends Block {
    public static final MapCodec<DeepCacheBlock> CODEC = simpleCodec(DeepCacheBlock::new);

    public DeepCacheBlock(BlockBehaviour.Properties properties) {
        super(properties.noOcclusion());
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.hasNeighborSignal(pos)) {
            if (!level.isClientSide) player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.tribalpower.redstone.locked"), true);
            return InteractionResult.CONSUME;
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            boolean linked = DeepCacheManager.hasSpiritLink(serverPlayer);
            if (!DeepCacheManager.tryAuthorize(serverPlayer)) {
                serverPlayer.displayClientMessage(Component.translatable("message.tribalpower.deep_cache.need_pulse"), true);
                return InteractionResult.CONSUME;
            }
            if (!linked) {
                serverPlayer.displayClientMessage(Component.translatable("message.tribalpower.deep_cache.pulse_link"), true);
            }
            DeepCacheContainer container = DeepCacheManager.openContainer(serverPlayer);
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x6, id, inv, container, 6) {
                        @Override
                        public boolean stillValid(Player viewer) {
                            return super.stillValid(viewer) && !level.hasNeighborSignal(pos)
                                    && level.getBlockState(pos).is(DeepCacheBlock.this)
                                    && viewer.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
                        }
                    },
                    Component.translatable("block.tribalpower.deep_cache")
            ));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
