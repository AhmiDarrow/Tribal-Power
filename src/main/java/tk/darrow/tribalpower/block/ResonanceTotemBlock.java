package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.blockentity.ResonanceTotemBlockEntity;

/**
 * A Resonance Totem stands two blocks tall. The lower half holds everything; the upper half is an invisible
 * stand-in that only gives the top a hitbox of its own, because a click is only ever tested against the block space
 * it lands in, so a tall shape on the lower block alone left the top half unclickable. Every use of the upper half
 * is handed to the lower one. Totems placed before this have their upper half filled in once there is air above.
 */
public class ResonanceTotemBlock extends BaseEntityBlock {
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<net.minecraft.world.level.block.state.properties.DoubleBlockHalf> HALF =
            net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF;
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 32.0, 16.0);
    private static final VoxelShape UPPER_SHAPE = Block.box(0.0, -16.0, 0.0, 16.0, 16.0, 16.0);

    public static final MapCodec<ResonanceTotemBlock> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Attunement.CODEC.fieldOf("attunement").forGetter(b -> b.attunement),
                    propertiesCodec()
            ).apply(instance, ResonanceTotemBlock::new)
    );

    private final Attunement attunement;

    public ResonanceTotemBlock(Attunement attunement, BlockBehaviour.Properties properties) {
        super(properties);
        this.attunement = attunement;
        registerDefaultState(stateDefinition.any().setValue(HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    public static boolean isUpper(BlockState state) {
        return state.getBlock() instanceof ResonanceTotemBlock && state.getValue(HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER;
    }

    /** The block holding the totem: the one below for an upper half, otherwise {@code pos} itself. */
    public static BlockPos base(BlockGetter level, BlockPos pos) {
        return isUpper(level.getBlockState(pos)) ? pos.below() : pos;
    }

    /** Puts the upper half on a totem that lacks one, only into air. */
    public static void growTop(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof ResonanceTotemBlock) || isUpper(state)) return;
        BlockPos above = pos.above();
        if (level.getBlockState(above).isAir())
            level.setBlock(above, state.setValue(HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        BlockPos above = context.getClickedPos().above();
        if (above.getY() >= context.getLevel().getMaxBuildHeight() || !context.getLevel().getBlockState(above).canBeReplaced(context))
            return null;   // a totem needs its two blocks
        return defaultBlockState();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.core.Direction direction, BlockState neighbour,
            net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        // An upper half without its totem below goes; a totem that lost its top keeps working and regrows it.
        if (isUpper(state) && direction == net.minecraft.core.Direction.DOWN
                && !(neighbour.is(this) && !isUpper(neighbour)))
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Breaking the top breaks the totem: it drops once, from the lower half, with everything it holds.
        if (!level.isClientSide && isUpper(state)) {
            BlockPos below = pos.below();
            if (level.getBlockState(below).is(this)) {
                if (player.isCreative()) level.setBlock(below, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                else level.destroyBlock(below, true, player);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    public Attunement getAttunement() {
        return attunement;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return isUpper(state) ? UPPER_SHAPE : SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isUpper(state) ? null : new ResonanceTotemBlockEntity(pos, state, attunement);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || isUpper(state) ? null : createTickerHelper(type, tk.darrow.tribalpower.blockentity.ModBlockEntities.RESONANCE_TOTEM.get(), ResonanceTotemBlockEntity::serverTick);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state,
            Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (isUpper(state)) {
            BlockPos below = pos.below();
            BlockState base = level.getBlockState(below);
            return base.is(this) ? useItemOn(stack, base, level, below, player, hand, hit.withPosition(below))
                    : net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(tk.darrow.tribalpower.item.ModItems.BONE_CHIME.get())) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof ResonanceTotemBlockEntity totem) {
                tk.darrow.tribalpower.lattice.Keeping.relight(totem, (net.minecraft.server.level.ServerLevel) level);
                player.displayClientMessage(Component.translatable(
                        "message.tribalpower.totem.keeping." + totem.keeping().name().toLowerCase(java.util.Locale.ROOT),
                        Component.translatable("attunement.tribalpower." + attunement.getSerializedName())
                ), true);
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!player.isShiftKeyDown() || !linkable(stack)) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        link(level, pos, player, stack);
        return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Spiritgear and Spirit Charms take a totem's voice with a sneak-click. */
    public static boolean linkable(net.minecraft.world.item.ItemStack stack) {
        return tk.darrow.tribalpower.item.SpiritGear.isGear(stack) || stack.getItem() instanceof tk.darrow.tribalpower.charm.SpiritCharmItem;
    }

    /** Gives {@code stack} this totem's voice (the server decides; the client only answers the click). */
    private void link(Level level, BlockPos pos, Player player, net.minecraft.world.item.ItemStack stack) {
        if (level.isClientSide) return;
        boolean linked = tk.darrow.tribalpower.item.SpiritGear.isGear(stack)
                ? tk.darrow.tribalpower.item.SpiritGear.tryLink(player, stack, attunement)
                : tk.darrow.tribalpower.charm.SpiritCharmItem.addVoice(player, stack, attunement);
        if (linked) {
            tk.darrow.tribalpower.effect.SpiritEffects.ring(
                    (net.minecraft.server.level.ServerLevel) level, pos.getCenter().add(0, 0.6, 0), attunement, 0.9, 16);
        }
    }

    /**
     * A sneak-click never reaches {@link #useItemOn} while the player holds anything: vanilla hands a sneaking,
     * item-holding click to the item alone. So the link is made here, before vanilla decides, and the click ends.
     */
    public static void sneakLink(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!player.isShiftKeyDown() || !linkable(event.getItemStack())) return;
        Level level = event.getLevel();
        if (!(level.getBlockState(event.getPos()).getBlock() instanceof ResonanceTotemBlock totem)) return;
        totem.link(level, base(level, event.getPos()), player, event.getItemStack());
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (isUpper(state)) {
            BlockPos below = pos.below();
            BlockState base = level.getBlockState(below);
            return base.is(this) ? useWithoutItem(base, level, below, player, hit.withPosition(below)) : InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable(
                    "message.tribalpower.totem.attunement",
                    Component.translatable("attunement.tribalpower." + attunement.getSerializedName())
            ), true);
            if (level.getBlockEntity(pos) instanceof ResonanceTotemBlockEntity totem) {
                if (totem.keeping() != tk.darrow.tribalpower.lattice.Keeping.State.ANSWERED)
                    tk.darrow.tribalpower.lattice.Keeping.relight(totem, (net.minecraft.server.level.ServerLevel) level);
                player.displayClientMessage(Component.translatable(
                        "message.tribalpower.totem.keeping." + totem.keeping().name().toLowerCase(java.util.Locale.ROOT),
                        Component.translatable("attunement.tribalpower." + attunement.getSerializedName())
                ), true);
                player.displayClientMessage(Component.translatable(
                        "message.tribalpower.totem.links",
                        totem.getLinks().size()
                ), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // A broken totem drops its temporary ley lines (rite/world/LeyLines) instead of leaving them dangling.
        if (!state.is(newState.getBlock()) && !isUpper(state) && level instanceof net.minecraft.server.level.ServerLevel server)
            tk.darrow.tribalpower.rite.world.LeyLines.unbind(server, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
    @Override
    protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        if (isUpper(state)) return java.util.List.of();   // the totem drops once, from its lower half
        return MachineDrops.withSelf(this, super.getDrops(state, builder));
    }
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(base(level, pos)) instanceof tk.darrow.tribalpower.api.pulse.PulseHandler pulse)
            return pulse.getPulseStored() == 0 ? 0 : 1 + 14 * pulse.getPulseStored() / Math.max(1, pulse.getPulseCapacity());
        return 0;
    }
}
