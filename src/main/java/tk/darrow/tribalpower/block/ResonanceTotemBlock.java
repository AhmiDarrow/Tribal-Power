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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.blockentity.ResonanceTotemBlockEntity;

public class ResonanceTotemBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);

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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable(
                    "message.tribalpower.totem.attunement",
                    Component.translatable("attunement.tribalpower." + attunement.getSerializedName())
            ), true);
            if (level.getBlockEntity(pos) instanceof ResonanceTotemBlockEntity totem) {
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
    protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof tk.darrow.tribalpower.api.pulse.PulseHandler pulse)
            return pulse.getPulseStored() == 0 ? 0 : 1 + 14 * pulse.getPulseStored() / Math.max(1, pulse.getPulseCapacity());
        return 0;
    }
}
