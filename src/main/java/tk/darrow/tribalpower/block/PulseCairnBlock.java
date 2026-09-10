package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import tk.darrow.tribalpower.blockentity.ModBlockEntities;
import tk.darrow.tribalpower.blockentity.PulseCairnBlockEntity;

/** Stacked stones that hold a beat (design 3.1 section 9.4). */
public class PulseCairnBlock extends BaseEntityBlock {
    public static final MapCodec<PulseCairnBlock> CODEC = simpleCodec(PulseCairnBlock::new);
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    public PulseCairnBlock(BlockBehaviour.Properties properties) {
        super(properties.noOcclusion());
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PulseCairnBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.PULSE_CAIRN.get(), PulseCairnBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PulseCairnBlockEntity cairn) {
            player.displayClientMessage(cairn.counted()
                    ? net.minecraft.network.chat.Component.translatable("message.tribalpower.cairn.status",
                            cairn.getPulseStored(), cairn.getPulseCapacity(), cairn.columnIndex() + 1)
                    : net.minecraft.network.chat.Component.translatable("message.tribalpower.cairn.too_tall",
                            PulseCairnBlockEntity.MAX_COLUMN), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbour, BlockPos neighbourPos, boolean movedByPiston) {
        // A stone removed from under the pile changes what every stone above it is worth.
        if (level.isClientSide || neighbourPos.getX() != pos.getX() || neighbourPos.getZ() != pos.getZ()) return;
        // Only the stones that could change worth need telling, and a column is capped anyway.
        BlockPos cursor = pos;
        for (int i = 0; i <= PulseCairnBlockEntity.MAX_COLUMN; i++, cursor = cursor.above()) {
            if (!(level.getBlockEntity(cursor) instanceof PulseCairnBlockEntity cairn)) break;
            cairn.onColumnChanged();
        }
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof PulseCairnBlockEntity cairn ? cairn.signal() : 0;
    }
}
