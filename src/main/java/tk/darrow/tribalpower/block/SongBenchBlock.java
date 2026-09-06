package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
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
import tk.darrow.tribalpower.blockentity.ModBlockEntities;
import tk.darrow.tribalpower.blockentity.SongBenchBlockEntity;
import tk.darrow.tribalpower.echo.EchoStage;
import tk.darrow.tribalpower.lattice.LatticeNetwork;

/**
 * Starts a lattice song that advances Echo-stage materials using Pulse and nearby attunements.
 */
public class SongBenchBlock extends BaseEntityBlock {
    public static final MapCodec<SongBenchBlock> CODEC = simpleCodec(SongBenchBlock::new);

    public SongBenchBlock(BlockBehaviour.Properties properties) {
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
        return new SongBenchBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.SONG_BENCH.get(), SongBenchBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SongBenchBlockEntity bench)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.isEmpty() || !EchoStage.isProcessable(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            if (bench.insertItem(stack)) {
                player.displayClientMessage(Component.translatable("message.tribalpower.song_bench.inserted"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.tribalpower.song_bench.full"), true);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SongBenchBlockEntity bench)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                ItemStack taken = bench.takeItem();
                if (!taken.isEmpty()) {
                    if (!player.addItem(taken)) {
                        player.drop(taken, false);
                    }
                    player.displayClientMessage(Component.translatable("message.tribalpower.song_bench.removed"), true);
                } else {
                    player.displayClientMessage(bench.statusMessage(), true);
                }
            } else if (bench.isSinging()) {
                player.displayClientMessage(bench.statusMessage(), true);
            } else {
                int linked = LatticeNetwork.countNearbyTotems(level, pos, SongBenchBlockEntity.RADIUS);
                bench.startSong();
                player.displayClientMessage(Component.translatable("message.tribalpower.song_bench.start", linked), true);
                if (bench.isSinging()) {
                    player.displayClientMessage(bench.statusMessage(), true);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof SongBenchBlockEntity bench) {
            Containers.dropContents(serverLevel, pos, bench);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
