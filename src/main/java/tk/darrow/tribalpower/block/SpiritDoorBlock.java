package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A spirit threshold. You, your camp, your familiars and every gentle creature walk through the middle as if
 * nothing were there; a monster or hostile spirit meets it as a wall. Hang one in a camp's gap and the night
 * stays outside.
 */
public class SpiritDoorBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<SpiritDoorBlock> CODEC = simpleCodec(SpiritDoorBlock::new);
    /** Posts on the east/west edges; opening 0.75 so a player (0.6) fits north-south. */
    private static final VoxelShape NS = Shapes.or(
            Block.box(0.0, 0.0, 5.0, 2.0, 16.0, 11.0),
            Block.box(14.0, 0.0, 5.0, 16.0, 16.0, 11.0)
    );
    /** Posts on the north/south edges; opening east-west. */
    private static final VoxelShape EW = Shapes.or(
            Block.box(5.0, 0.0, 0.0, 11.0, 16.0, 2.0),
            Block.box(5.0, 0.0, 14.0, 11.0, 16.0, 16.0)
    );

    public SpiritDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<SpiritDoorBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                java.util.List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        lines.add(net.minecraft.network.chat.Component.translatable("block.tribalpower.spirit_door.desc").withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    /** The opening closes to anything hostile, and to nothing else. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (tk.darrow.tribalpower.config.TribalConfig.spiritDoorBlocksHostiles()
                && context instanceof net.minecraft.world.phys.shapes.EntityCollisionContext entityContext
                && barred(entityContext.getEntity())) return Shapes.block();
        return getShape(state, level, pos, context);
    }

    /** Monsters and wild hostile spirits; never players, bonded familiars or anything peaceful. */
    public static boolean barred(@org.jetbrains.annotations.Nullable net.minecraft.world.entity.Entity entity) {
        if (!(entity instanceof net.minecraft.world.entity.LivingEntity living) || entity instanceof net.minecraft.world.entity.player.Player)
            return false;
        // A March creature is judged as the familiars judge it: bonded ones and camp-bred young are no threat.
        if (living instanceof tk.darrow.tribalpower.familiar.Familiar) return tk.darrow.tribalpower.familiar.FamiliarRoster.hostile(living);
        return living instanceof net.minecraft.world.entity.monster.Enemy;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? NS : EW;
    }
}
