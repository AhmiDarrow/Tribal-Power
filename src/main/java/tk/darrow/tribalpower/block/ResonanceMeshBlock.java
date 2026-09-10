package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;
import tk.darrow.tribalpower.blockentity.ModBlockEntities;
import tk.darrow.tribalpower.blockentity.ResonanceMeshBlockEntity;
import tk.darrow.tribalpower.camp.Ownership;

/**
 * "The mesh does not find the ore. The mesh gives the ore somewhere to land." The anchor of the
 * Listening Pit (design 3.1 section 7).
 */
public class ResonanceMeshBlock extends BaseEntityBlock {
    public static final MapCodec<ResonanceMeshBlock> CODEC = simpleCodec(ResonanceMeshBlock::new);
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 10.0, 16.0);

    public ResonanceMeshBlock(BlockBehaviour.Properties properties) {
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
        return new ResonanceMeshBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.RESONANCE_MESH.get(), ResonanceMeshBlockEntity::tick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof ResonanceMeshBlockEntity mesh) mesh.setOwner(Ownership.of(placer));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof ResonanceMeshBlockEntity mesh) {
            if (!Ownership.check(level, mesh.owner(), player)) return ItemInteractionResult.CONSUME;
            // A bucket is the hand-fed version of a Spirit Cistern; both fill the same tank.
            if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection()))
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            if (!level.isClientSide && mesh.canPlaceItem(ResonanceMeshBlockEntity.SAMPLE, stack)
                    && mesh.getItem(ResonanceMeshBlockEntity.SAMPLE).isEmpty()) {
                mesh.setItem(ResonanceMeshBlockEntity.SAMPLE, stack.copyWithCount(1));
                if (!player.isCreative()) stack.shrink(1);
                player.displayClientMessage(mesh.status(), true);
                return ItemInteractionResult.SUCCESS;
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ResonanceMeshBlockEntity mesh) {
            if (!Ownership.check(level, mesh.owner(), player)) return InteractionResult.CONSUME;
            if (player.isShiftKeyDown()) {
                ItemStack sample = mesh.removeItemNoUpdate(ResonanceMeshBlockEntity.SAMPLE);
                if (!sample.isEmpty() && !player.getInventory().add(sample)) player.drop(sample, false);
            }
            player.displayClientMessage(mesh.status(), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbour, BlockPos neighbourPos, boolean movedByPiston) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ResonanceMeshBlockEntity mesh)
            mesh.onNeighbourChanged(neighbourPos);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        return Ownership.canBreak(player.level(), pos, player) && super.canHarvestBlock(state, level, pos, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof ResonanceMeshBlockEntity mesh) {
            Containers.dropContents(level, pos, mesh);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ResonanceMeshBlockEntity mesh ? mesh.progressSignal() : 0;
    }
}
