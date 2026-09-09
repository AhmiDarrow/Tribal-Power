package tk.darrow.tribalpower.tribe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.item.PulseCellItem;

import java.util.List;

/**
 * Tribe Hearth: accepts offerings for standing. Favoured +3, reagent +8, any food +1; a charged Pulse Cell drains up
 * to 40 Pulse for +2 per 10. Comparator reads the last visitor's rank.
 */
public class TribeHearthBlock extends BaseEntityBlock {
    public static final MapCodec<TribeHearthBlock> CODEC = simpleCodec(TribeHearthBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 5, 15), Block.box(3, 5, 3, 13, 9, 13));

    public TribeHearthBlock(Properties properties) { super(properties); }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TribeHearthBlockEntity(pos, state); }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, TribeRegistry.HEARTH_TYPE.get(), TribeHearthBlockEntity::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        TribeDefinition tribe = TribeDefinition.of(stack);
        if (tribe != null && level.getBlockEntity(pos) instanceof TribeHearthBlockEntity hearth) hearth.setTribe(tribe);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TribeHearthBlockEntity hearth)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        TribeDefinition tribe = hearth.tribe();
        if (stack.getItem() instanceof PulseCellItem) {
            if (PulseCellItem.getPulse(stack) < 10) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (player instanceof ServerPlayer sp) {
                int drained = PulseCellItem.extractPulse(stack, TribeStanding.MAX_CELL_DRAIN, false);
                int gain = drained / 10 * TribeStanding.GAIN_PULSE_PER_10;
                offered(sp, hearth, gain, pos);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        int value = tribe.offeringValue(stack);
        if (value <= 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (player instanceof ServerPlayer sp) {
            if (!sp.isCreative()) stack.shrink(1);
            offered(sp, hearth, value, pos);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void offered(ServerPlayer player, TribeHearthBlockEntity hearth, int gain, BlockPos pos) {
        TribeDefinition tribe = hearth.tribe();
        int total = TribeStanding.add(player, tribe, gain);
        hearth.touched(player.getUUID(), TribeRank.of(total));
        var server = player.serverLevel();
        server.playSound(null, pos, net.minecraft.sounds.SoundEvents.FIRECHARGE_USE, net.minecraft.sounds.SoundSource.BLOCKS, 0.4F, 1.4F);
        DustParticleOptions dust = new DustParticleOptions(tribe.particleColour(), 1.0F);
        server.sendParticles(dust, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 12, 0.25, 0.2, 0.25, 0.01);
        player.displayClientMessage(Component.translatable("message.tribalpower.hearth.offered", tribe.displayNameComponent(), gain, total,
                Component.translatable(TribeRank.of(total).translationKey())), true);
        tk.darrow.tribalpower.camp.CampHooks.award(server, player.getUUID(), "tribes/offering");
        if (TribeRank.of(total).ordinal() >= TribeRank.FRIEND.ordinal())
            tk.darrow.tribalpower.camp.CampHooks.award(server, player.getUUID(), "tribes/friend");
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof TribeHearthBlockEntity hearth) {
            TribeDefinition tribe = hearth.tribe();
            int standing = TribeStanding.get(sp.server, sp.getUUID(), tribe);
            hearth.touched(sp.getUUID(), TribeRank.of(standing));
            sp.displayClientMessage(Component.translatable("message.tribalpower.hearth.status", tribe.displayNameComponent(), standing,
                    Component.translatable(TribeRank.of(standing).translationKey())), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof TribeHearthBlockEntity hearth)) return;
        if (random.nextInt(3) == 0) {
            level.addParticle(new DustParticleOptions(hearth.tribe().particleColour(), 0.8F),
                    pos.getX() + 0.35 + random.nextDouble() * 0.3, pos.getY() + 0.6 + random.nextDouble() * 0.3, pos.getZ() + 0.35 + random.nextDouble() * 0.3,
                    0, 0.02, 0);
        }
        if (random.nextInt(6) == 0)
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 0, 0.03, 0);
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TribeHearthBlockEntity hearth ? hearth.signal() : 0;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        TribeDefinition tribe = be instanceof TribeHearthBlockEntity hearth ? hearth.tribe() : TribeDefinition.SOIL;
        return List.of(tribe.stamped(this));
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target, net.minecraft.world.level.LevelReader level, BlockPos pos, Player player) {
        TribeDefinition tribe = level.getBlockEntity(pos) instanceof TribeHearthBlockEntity hearth ? hearth.tribe() : TribeDefinition.SOIL;
        return tribe.stamped(this);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock())) level.updateNeighbourForOutputSignal(pos, this);
        super.onRemove(state, level, pos, next, moved);
    }
}
