package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import tk.darrow.tribalpower.blockentity.ModBlockEntities;
import tk.darrow.tribalpower.blockentity.PulseLightBlockEntity;

/** Camp light that draws a trickle of Pulse from the lattice. Redstone dims it. */
public class PulseLightBlock extends BaseEntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public enum Kind {
        GLOW_REED(10, 1, Shapes.or(Block.box(6, 0, 6, 10, 12, 10), Block.box(5, 12, 5, 11, 16, 11))),
        SHARD_LAMP(12, 1, Shapes.or(Block.box(4, 0, 4, 12, 2, 12), Block.box(7, 2, 7, 9, 8, 9), Block.box(5, 8, 5, 11, 15, 11))),
        ECHO_SCONCE(11, 1, Block.box(5, 3, 11, 11, 13, 16)),
        EMBER_BOWL(14, 2, Block.box(3, 0, 3, 13, 5, 13));

        public final int light;
        public final int cost;
        public final VoxelShape shape;

        Kind(int light, int cost, VoxelShape shape) {
            this.light = light;
            this.cost = cost;
            this.shape = shape;
        }
    }

    private final Kind kind;
    private final MapCodec<PulseLightBlock> codec;

    public PulseLightBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        this.codec = simpleCodec(p -> new PulseLightBlock(kind, p));
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return kind.shape;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PulseLightBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.PULSE_LIGHT.get(), PulseLightBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            player.displayClientMessage(state.getValue(LIT)
                    ? Component.translatable("message.tribalpower.pulse_light.lit", kind.cost)
                    : Component.translatable("message.tribalpower.pulse_light.dark", kind.cost), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(LIT) ? 15 : 0;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide && level.hasNeighborSignal(pos) && state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, false), 3);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        if (kind == Kind.EMBER_BOWL) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                    x + (random.nextDouble() - 0.5) * 0.3, pos.getY() + 0.28, z + (random.nextDouble() - 0.5) * 0.3,
                    0.0, 0.01, 0.0);
        } else if (random.nextInt(4) == 0) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    x + (random.nextDouble() - 0.5) * 0.2, pos.getY() + 0.7, z + (random.nextDouble() - 0.5) * 0.2,
                    0.0, 0.01, 0.0);
        }
    }
}
