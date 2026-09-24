package tk.darrow.tribalpower.cuisine;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A wide clay pot in a copper cradle; set it over the fire and it cooks. */
public class HearthPotBlock extends BaseEntityBlock {
    public static final MapCodec<HearthPotBlock> CODEC = simpleCodec(HearthPotBlock::new);
    public static final BooleanProperty COOKING = BooleanProperty.create("cooking");
    private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 3, 2, 14, 12, 14), Block.box(1, 0, 1, 15, 3, 15));

    public HearthPotBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(COOKING, false));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(COOKING); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new HearthPotBlockEntity(pos, state); }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, CuisineRegistry.HEARTH_POT_ENTITY.get(), HearthPotBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof HearthPotBlockEntity pot) player.openMenu(pot);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof HearthPotBlockEntity pot) Containers.dropContents(level, pos, pot.items());
        super.onRemove(state, level, pos, next, moved);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(COOKING)) return;
        double x = pos.getX() + 0.3 + random.nextDouble() * 0.4, z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
        level.addParticle(ParticleTypes.BUBBLE_POP, x, pos.getY() + 0.78, z, 0, 0.02, 0);
        if (random.nextInt(2) == 0) level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, pos.getY() + 0.85, z, 0, 0.04, 0);
    }
}
