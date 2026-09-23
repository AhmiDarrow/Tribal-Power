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

public class ResonanceTotemBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 32.0, 16.0);

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
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResonanceTotemBlockEntity(pos, state, attunement);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, tk.darrow.tribalpower.blockentity.ModBlockEntities.RESONANCE_TOTEM.get(), ResonanceTotemBlockEntity::serverTick);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state,
            Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
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
        boolean gear = tk.darrow.tribalpower.item.SpiritGear.isGear(stack);
        boolean charm = stack.getItem() instanceof tk.darrow.tribalpower.charm.SpiritCharmItem;
        if (!player.isShiftKeyDown() || (!gear && !charm)) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            boolean linked = gear
                    ? tk.darrow.tribalpower.item.SpiritGear.tryLink(player, stack, attunement)
                    : tk.darrow.tribalpower.charm.SpiritCharmItem.addVoice(player, stack, attunement);
            if (linked) {
                tk.darrow.tribalpower.effect.SpiritEffects.ring(
                        (net.minecraft.server.level.ServerLevel) level, pos.getCenter().add(0, 0.6, 0), attunement, 0.9, 16);
            }
        }
        return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
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
        if (!state.is(newState.getBlock()) && level instanceof net.minecraft.server.level.ServerLevel server)
            tk.darrow.tribalpower.rite.world.LeyLines.unbind(server, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
    @Override
    protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        return MachineDrops.withSelf(this, super.getDrops(state, builder));
    }
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof tk.darrow.tribalpower.api.pulse.PulseHandler pulse)
            return pulse.getPulseStored() == 0 ? 0 : 1 + 14 * pulse.getPulseStored() / Math.max(1, pulse.getPulseCapacity());
        return 0;
    }
}
