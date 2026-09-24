package tk.darrow.tribalpower.cuisine;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.effect.ModEffects;

/**
 * A voice's feast: a great dish set down for the camp, six servings, and everyone who eats from it is blessed
 * with that voice. It is the table the whole camp sits at before a night in the March.
 */
public class FeastBlock extends Block {
    public static final MapCodec<FeastBlock> CODEC = simpleCodec(properties -> new FeastBlock(Attunement.EARTH, properties));
    public static final int SERVINGS = 6;
    public static final IntegerProperty EATEN = IntegerProperty.create("eaten", 0, SERVINGS - 1);
    private static final VoxelShape[] SHAPES = new VoxelShape[SERVINGS];

    static {
        for (int eaten = 0; eaten < SERVINGS; eaten++) SHAPES[eaten] = Block.box(1 + eaten * 2, 0, 1, 15, 8, 15);
    }

    public final Attunement voice;

    public FeastBlock(Attunement voice, Properties properties) {
        super(properties);
        this.voice = voice;
        registerDefaultState(stateDefinition.any().setValue(EATEN, 0));
    }

    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(EATEN); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPES[state.getValue(EATEN)]; }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isSolid();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return player.canEat(false) ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        return eat(level, pos, state, player);
    }

    /** One serving: food, the voice's blessing, and one slice less on the table. */
    public InteractionResult eat(Level level, BlockPos pos, BlockState state, Player player) {
        if (!player.canEat(false)) return InteractionResult.PASS;
        player.getFoodData().eat(TribalConfig.feastNutrition(), (float) TribalConfig.feastSaturation());
        int minutes = TribalConfig.feastBlessingMinutes();
        if (minutes > 0) ModEffects.bless(player, voice, minutes * 60 * 20, 0, false);
        level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.8F, 1.0F);
        int eaten = state.getValue(EATEN);
        if (eaten < SERVINGS - 1) level.setBlock(pos, state.setValue(EATEN, eaten + 1), 3);
        else level.removeBlock(pos, false);
        level.gameEvent(player, net.minecraft.world.level.gameevent.GameEvent.EAT, pos);
        return InteractionResult.SUCCESS;
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) { return (SERVINGS - state.getValue(EATEN)) * 15 / SERVINGS; }

    /** A feast with a serving gone is not picked back up. */
    @Override
    protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        return state.getValue(EATEN) == 0 ? super.getDrops(state, builder) : java.util.List.of();
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.core.Direction direction, BlockState neighbour, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return direction == net.minecraft.core.Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
}
