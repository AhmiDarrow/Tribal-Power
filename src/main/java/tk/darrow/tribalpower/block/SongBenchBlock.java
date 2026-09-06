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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.blockentity.SongBenchBlockEntity;
import tk.darrow.tribalpower.lattice.LatticeNetwork;

/**
 * Starts a lattice "song" that routes materials/spirit along connected Resonance Totems.
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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            int linked = LatticeNetwork.countNearbyTotems(level, pos, 8);
            if (level.getBlockEntity(pos) instanceof SongBenchBlockEntity bench) {
                bench.startSong(linked);
            }
            player.displayClientMessage(Component.translatable("message.tribalpower.song_bench.start", linked), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
