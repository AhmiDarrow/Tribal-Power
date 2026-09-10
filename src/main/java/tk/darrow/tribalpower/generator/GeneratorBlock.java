package tk.darrow.tribalpower.generator;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import tk.darrow.tribalpower.item.PulseCellItem;

/**
 * One block class for the six voices (design 3.1 section 9), in the same shape the mod already uses for
 * its utility blocks: the block entity is chosen from which block this is.
 */
public class GeneratorBlock extends BaseEntityBlock {
    public static final MapCodec<GeneratorBlock> CODEC = simpleCodec(GeneratorBlock::new);
    /** Whether the voice is producing. Only the Ember Horn actually changes it. */
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public GeneratorBlock(Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(getStateDefinition().any().setValue(LIT, false));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.is(GeneratorRegistry.EMBER_HORN.get())) return new EmberHornBlockEntity(pos, state);
        if (state.is(GeneratorRegistry.WIND_HARP.get())) return new WindHarpBlockEntity(pos, state);
        if (state.is(GeneratorRegistry.WAVE_DRUM.get())) return new WaveDrumBlockEntity(pos, state);
        if (state.is(GeneratorRegistry.WAKE_BELL.get())) return new WakeBellBlockEntity(pos, state);
        return new LoomAnchorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        if (state.is(GeneratorRegistry.EMBER_HORN.get()))
            return createTickerHelper(type, GeneratorRegistry.EMBER_HORN_TYPE.get(), GeneratorBlockEntity::tick);
        if (state.is(GeneratorRegistry.WIND_HARP.get()))
            return createTickerHelper(type, GeneratorRegistry.WIND_HARP_TYPE.get(), GeneratorBlockEntity::tick);
        if (state.is(GeneratorRegistry.WAVE_DRUM.get()))
            return createTickerHelper(type, GeneratorRegistry.WAVE_DRUM_TYPE.get(), GeneratorBlockEntity::tick);
        if (state.is(GeneratorRegistry.WAKE_BELL.get()))
            return createTickerHelper(type, GeneratorRegistry.WAKE_BELL_TYPE.get(), GeneratorBlockEntity::tick);
        return createTickerHelper(type, GeneratorRegistry.LOOM_ANCHOR_TYPE.get(), GeneratorBlockEntity::tick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.is(GeneratorRegistry.WAVE_DRUM.get())
                && FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection()))
            return ItemInteractionResult.sidedSuccess(level.isClientSide);

        if (level.getBlockEntity(pos) instanceof GeneratorBlockEntity generator) {
            if (stack.getItem() instanceof PulseCellItem) {
                if (!level.isClientSide) {
                    int want = Math.min(100, PulseCellItem.capacity(stack) - PulseCellItem.getPulse(stack));
                    int taken = generator.extractPulse(want, false);
                    int filled = PulseCellItem.insertPulse(stack, taken, false);
                    if (filled < taken) generator.insertPulse(taken - filled, false);
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                            "message.tribalpower.pulse_cell.charge", filled, PulseCellItem.getPulse(stack),
                            PulseCellItem.capacity(stack), generator.getPulseStored()), true);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (generator instanceof EmberHornBlockEntity horn && horn.canPlaceItem(EmberHornBlockEntity.SLOT, stack)) {
                if (!level.isClientSide) {
                    ItemStack held = horn.getItem(EmberHornBlockEntity.SLOT);
                    int moved = 0;
                    if (held.isEmpty()) {
                        moved = Math.min(stack.getCount(), stack.getMaxStackSize());
                        horn.setItem(EmberHornBlockEntity.SLOT, stack.copyWithCount(moved));
                    } else if (ItemStack.isSameItemSameComponents(held, stack)) {
                        moved = Math.min(stack.getCount(), held.getMaxStackSize() - held.getCount());
                        held.grow(moved);
                        horn.setChanged();
                    }
                    if (moved > 0 && !player.isCreative()) stack.shrink(moved);
                    player.displayClientMessage(horn.status(), true);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GeneratorBlockEntity generator)
            player.displayClientMessage(generator.status(), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof EmberHornBlockEntity horn) {
            Containers.dropContents(level, pos, horn);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof GeneratorBlockEntity generator ? generator.signal() : 0;
    }
}
