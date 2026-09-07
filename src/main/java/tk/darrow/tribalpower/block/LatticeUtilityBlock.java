package tk.darrow.tribalpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import tk.darrow.tribalpower.blockentity.*;

public class LatticeUtilityBlock extends BaseEntityBlock {
    public static final MapCodec<LatticeUtilityBlock> CODEC = simpleCodec(LatticeUtilityBlock::new);
    public LatticeUtilityBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.is(ModBlocks.SPIRIT_CISTERN.get())) return new SpiritCisternBlockEntity(pos, state);
        return state.is(ModBlocks.PULSE_ADAPTER.get()) ? new PulseAdapterBlockEntity(pos, state) : new WirelessRelayBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return state.is(ModBlocks.PULSE_ADAPTER.get())
                ? createTickerHelper(type, ModBlockEntities.PULSE_ADAPTER.get(), PulseAdapterBlockEntity::tick)
                : createTickerHelper(type, ModBlockEntities.WIRELESS_RELAY.get(), WirelessRelayBlockEntity::tick);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            var be = level.getBlockEntity(pos);
            if (be instanceof WirelessRelayBlockEntity relay) player.displayClientMessage(relay.status(), true);
            if (be instanceof PulseAdapterBlockEntity adapter) player.displayClientMessage(adapter.status(), true);
            if (be instanceof SpiritCisternBlockEntity cistern) player.displayClientMessage(cistern.status(), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (state.is(ModBlocks.SPIRIT_CISTERN.get()) && net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection()))
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        var drops = super.getDrops(state, builder);
        var be = builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if (be != null && be.getLevel() != null) {
            var data = be.saveWithFullMetadata(be.getLevel().registryAccess());
            for (var stack : drops) if (stack.is(asItem()))
                stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.CustomData.of(data));
        }
        return drops;
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        var be = level.getBlockEntity(pos);
        if (be instanceof SpiritCisternBlockEntity tank) return tank.tank.getFluidAmount() == 0 ? 0 : 1 + 14 * tank.tank.getFluidAmount() / tank.tank.getCapacity();
        if (be instanceof PulseAdapterBlockEntity adapter) return adapter.handler.getEnergyStored() == 0 ? 0 : 1 + 14 * adapter.handler.getEnergyStored() / adapter.handler.getMaxEnergyStored();
        if (be instanceof WirelessRelayBlockEntity relay) return relay.signal();
        return 0;
    }
}
