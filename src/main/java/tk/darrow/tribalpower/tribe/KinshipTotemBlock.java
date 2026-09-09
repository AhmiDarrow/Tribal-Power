package tk.darrow.tribalpower.tribe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Kinship Totem: an extra distinct voice for the Pulse Resonator (counted per tribe, separately from Attunement) that
 * also provides its tribe's Attunement to stations. Property {@code tribe} 0-8 mirrors the block entity's tribe.
 */
public class KinshipTotemBlock extends BaseEntityBlock {
    public static final MapCodec<KinshipTotemBlock> CODEC = simpleCodec(KinshipTotemBlock::new);
    public static final IntegerProperty TRIBE = TribeBannerBlock.TRIBE;
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);

    public KinshipTotemBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TRIBE, 0));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(TRIBE); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new KinshipTotemBlockEntity(pos, state); }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(TRIBE, TribeDefinition.ofOrDefault(ctx.getItemInHand()).ordinal());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof KinshipTotemBlockEntity totem) totem.setTribe(tribe(state));
    }

    public static TribeDefinition tribe(BlockState state) { return TribeDefinition.byOrdinal(state.getValue(TRIBE)); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            TribeDefinition tribe = tribe(state);
            player.displayClientMessage(Component.translatable("message.tribalpower.kinship.status", tribe.displayNameComponent(),
                    Component.translatable("attunement.tribalpower." + tribe.attunement().getSerializedName())), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0)
            level.addParticle(new DustParticleOptions(tribe(state).particleColour(), 0.7F),
                    pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 1.05, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.01, 0);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) { return List.of(tribe(state).stamped(this)); }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return tribe(state).stamped(this);
    }
}
