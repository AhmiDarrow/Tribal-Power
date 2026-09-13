package tk.darrow.tribalpower.lattice;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.blockentity.WirelessRelayBlockEntity;
import tk.darrow.tribalpower.item.MachineRank;

/**
 * Adjacent Tribal machines push into each other when the touching faces are set to extract into insert.
 * Relays stay for distance; a Shatter sitting on a Kiln with output-into-input needs none.
 */
public final class SideIoAdjacency {
    public static final int ITEMS = 16;
    public static final int FLUID = 250;

    private SideIoAdjacency() {}

    public static void beat(Level level, BlockEntity be) {
        if (level.isClientSide || be.isRemoved()) return;
        if ((level.getGameTime() + be.getBlockPos().asLong()) % 20 != 0) return;
        push(level, be);
    }

    public static void push(Level level, BlockEntity be) {
        if (level.isClientSide || be.isRemoved() || !ours(be)) return;
        if (be instanceof WirelessRelayBlockEntity) return;
        BlockPos pos = be.getBlockPos();
        if (level.hasNeighborSignal(pos)) return;
        for (Direction face : Direction.values()) {
            BlockPos destPos = pos.relative(face);
            if (!level.hasChunkAt(destPos)) continue;
            BlockEntity dest = level.getBlockEntity(destPos);
            if (dest == null || dest.isRemoved() || !ours(dest) || dest instanceof WirelessRelayBlockEntity) continue;
            Direction toward = face.getOpposite();
            if (!feeds(mode(be, face), mode(dest, toward))) continue;
            if (level.hasNeighborSignal(destPos)) continue;
            moveItems(level, pos, face, destPos, toward, be);
            moveFluid(level, pos, face, destPos, toward, be);
        }
    }

    static boolean feeds(SideIo.Mode from, SideIo.Mode to) {
        return from.extract() && to.insert() && !(from.insert() && to.extract());
    }

    private static boolean ours(BlockEntity be) {
        var id = BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock());
        return id != null && TribalPower.MOD_ID.equals(id.getNamespace());
    }

    private static SideIo.Mode mode(BlockEntity be, Direction face) {
        return be instanceof HasSideIo io ? io.sideIo().get(face) : SideIo.Mode.BOTH;
    }

    private static IItemHandler handler(Level level, BlockPos pos, Direction face, BlockEntity be) {
        IItemHandler cap = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, face);
        if (cap != null) return cap;
        if (be instanceof WorldlyContainer container)
            return new RedstoneItemHandler(be, new SidedInvWrapper(container, face));
        return null;
    }

    private static void moveItems(Level level, BlockPos sourcePos, Direction sourceFace, BlockPos destPos,
                                  Direction destFace, BlockEntity sourceBe) {
        IItemHandler source = handler(level, sourcePos, sourceFace, sourceBe);
        IItemHandler sink = handler(level, destPos, destFace, level.getBlockEntity(destPos));
        if (source == null || sink == null || source == sink || source.getSlots() == 0) return;
        int burst = MachineRank.itemBurst(sourceBe, ITEMS);
        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack candidate = source.extractItem(slot, burst, true);
            if (candidate.isEmpty()) continue;
            int accepted = candidate.getCount() - ItemHandlerHelper.insertItemStacked(sink, candidate, true).getCount();
            if (accepted <= 0) continue;
            ItemStack actual = source.extractItem(slot, accepted, false);
            if (actual.isEmpty()) continue;
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(sink, actual, false);
            if (!remainder.isEmpty()) {
                remainder = ItemHandlerHelper.insertItemStacked(source, remainder, false);
                if (!remainder.isEmpty()) Block.popResource(level, sourcePos, remainder);
            }
            sourceBe.setChanged();
            BlockEntity dest = level.getBlockEntity(destPos);
            if (dest != null) dest.setChanged();
            return;
        }
    }

    private static void moveFluid(Level level, BlockPos sourcePos, Direction sourceFace, BlockPos destPos,
                                  Direction destFace, BlockEntity sourceBe) {
        IFluidHandler source = level.getCapability(Capabilities.FluidHandler.BLOCK, sourcePos, sourceFace);
        IFluidHandler sink = level.getCapability(Capabilities.FluidHandler.BLOCK, destPos, destFace);
        if (source == null || sink == null || source == sink) return;
        int burst = MachineRank.scalePulse(sourceBe, FLUID);
        var candidate = source.drain(burst, IFluidHandler.FluidAction.SIMULATE);
        if (candidate.isEmpty()) return;
        int accepted = sink.fill(candidate, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return;
        var actual = source.drain(candidate.copyWithAmount(Math.min(burst, accepted)), IFluidHandler.FluidAction.EXECUTE);
        if (actual.isEmpty()) return;
        int filled = sink.fill(actual.copy(), IFluidHandler.FluidAction.EXECUTE);
        if (filled < actual.getAmount()) {
            source.fill(actual.copyWithAmount(actual.getAmount() - filled), IFluidHandler.FluidAction.EXECUTE);
        }
    }
}
