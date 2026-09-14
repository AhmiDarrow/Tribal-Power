package tk.darrow.tribalpower.device;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import tk.darrow.tribalpower.item.MachineRank;
import tk.darrow.tribalpower.lattice.LatticeNetwork;

public class WorkshopBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer, tk.darrow.tribalpower.lattice.HasSideIo {
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    private boolean extract = true;
    private String reason = "Waiting";
    private FluidStack pending = FluidStack.EMPTY;
    private final tk.darrow.tribalpower.lattice.SideIo sides = tk.darrow.tribalpower.lattice.SideIo.mesh();
    @Override public tk.darrow.tribalpower.lattice.SideIo sideIo() { return sides; }
    @Override public int[] inputSlots(Direction face) { return java.util.stream.IntStream.range(0, 27).toArray(); }
    @Override public int[] outputSlots(Direction face) { return java.util.stream.IntStream.range(0, 27).toArray(); }

    public WorkshopBlockEntity(BlockPos pos, BlockState state) {
        super(DeviceRegistry.WORKSHOP.get(), pos, state);
    }

    public String kind() { return BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath(); }

    public Component status() { return Component.literal(reason); }

    public int signal() {
        return switch (kind()) {
            case "tide_pump" -> extract ? 15 : 1;
            case "wind_snare" -> AbstractContainerMenu.getRedstoneSignalFromContainer(this);
            case "ward_drum" -> reason.startsWith("Struck") ? 15 : 0;
            default -> 0;
        };
    }

    public void cycle(Player player) {
        if ("tide_pump".equals(kind())) {
            extract = !extract;
            reason = extract ? "Drawing from the face" : "Pushing into the face";
            player.displayClientMessage(status(), true);
            setChanged();
        } else player.displayClientMessage(status(), true);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WorkshopBlockEntity be) {
        tk.darrow.tribalpower.lattice.SideIoAdjacency.beat(level, be);
        if (level.isClientSide || !MachineRank.due(level, pos, be)) return;
        if (!(level instanceof ServerLevel server)) return;
        be.beat(server);
    }

    public void beat(ServerLevel server) {
        if (server.hasNeighborSignal(worldPosition)) { reason = "Paused by redstone"; return; }
        switch (kind()) {
            case "tide_pump" -> pump(server);
            case "wind_snare" -> vacuum(server);
            case "ward_drum" -> fight(server);
        }
        boolean lit = !reason.startsWith("Paused") && !reason.startsWith("Need") && !reason.startsWith("Waiting");
        if (getBlockState().getValue(WorkshopBlock.LIT) != lit)
            server.setBlock(worldPosition, getBlockState().setValue(WorkshopBlock.LIT, lit), 3);
        server.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    private void pump(ServerLevel server) {
        Direction face = getBlockState().getValue(WorkshopBlock.FACING);
        BlockPos front = worldPosition.relative(face);
        BlockPos back = worldPosition.relative(face.getOpposite());
        IFluidHandler from = server.getCapability(Capabilities.FluidHandler.BLOCK, extract ? front : back, extract ? face.getOpposite() : face);
        IFluidHandler to = server.getCapability(Capabilities.FluidHandler.BLOCK, extract ? back : front, extract ? face : face.getOpposite());
        if (from == null || to == null) { reason = "Need a tank on both faces"; return; }
        int cost = 4;
        if (LatticeNetwork.extractPulseNearby(server, worldPosition, 8, cost, true) < cost) { reason = "Need " + cost + " Pulse"; return; }
        if (!pending.isEmpty()) {
            int flushed = to.fill(pending.copy(), IFluidHandler.FluidAction.EXECUTE);
            if (flushed > 0) { pending.shrink(flushed); setChanged(); }
            if (!pending.isEmpty()) { reason = "Destination full"; return; }
        }
        FluidStack drained = from.drain(250, IFluidHandler.FluidAction.SIMULATE);
        if (drained.isEmpty()) { reason = "Nothing to move"; return; }
        int filled = to.fill(drained, IFluidHandler.FluidAction.SIMULATE);
        if (filled <= 0) { reason = "Destination full"; return; }
        FluidStack moved = from.drain(filled, IFluidHandler.FluidAction.EXECUTE);
        if (moved.isEmpty()) { reason = "Nothing to move"; return; }
        int accepted = to.fill(moved, IFluidHandler.FluidAction.EXECUTE);
        if (accepted < moved.getAmount()) {
            pending = moved.copy();
            pending.setAmount(moved.getAmount() - accepted);
        }
        LatticeNetwork.extractPulseNearby(server, worldPosition, 8, cost, false);
        reason = extract ? "Drawing " + accepted + " mB" : "Pushing " + accepted + " mB";
        setChanged();
    }

    private void vacuum(ServerLevel server) {
        int cost = 2;
        if (LatticeNetwork.extractPulseNearby(server, worldPosition, 8, cost, true) < cost) { reason = "Need " + cost + " Pulse per item"; return; }
        var box = new AABB(worldPosition).inflate(8);
        int pulled = 0;
        for (ItemEntity entity : server.getEntitiesOfClass(ItemEntity.class, box, e -> !e.isRemoved() && !e.getItem().isEmpty())) {
            ItemStack stack = entity.getItem();
            if (insertLeftover(stack, true).getCount() == stack.getCount()) continue;
            if (LatticeNetwork.extractPulseNearby(server, worldPosition, 8, cost, false) < cost) break;
            ItemStack leftover = insertLeftover(stack, false);
            if (leftover.isEmpty()) entity.discard();
            else entity.setItem(leftover);
            pulled++;
            if (pulled >= 8) break;
        }
        reason = pulled > 0 ? "Caught " + pulled + " stacks" : "Listening for dropped items";
    }

    private ItemStack insertLeftover(ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < 27 && !remaining.isEmpty(); i++) {
            ItemStack in = simulate ? items.get(i).copy() : items.get(i);
            if (in.isEmpty()) {
                if (!simulate) items.set(i, remaining.copy());
                remaining = ItemStack.EMPTY;
                break;
            }
            if (ItemStack.isSameItemSameComponents(in, remaining)) {
                int n = Math.min(remaining.getCount(), in.getMaxStackSize() - in.getCount());
                if (!simulate) in.grow(n);
                remaining.shrink(n);
            }
        }
        if (!simulate) setChanged();
        return remaining;
    }

    private void fight(ServerLevel server) {
        int cost = 8;
        if (LatticeNetwork.extractPulseNearby(server, worldPosition, 8, cost, true) < cost) { reason = "Need " + cost + " Pulse"; return; }
        var box = new AABB(worldPosition).inflate(8);
        for (var living : server.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box,
                e -> tk.darrow.tribalpower.familiar.FamiliarRoster.hostile(e) && e.isAlive())) {
            if (LatticeNetwork.extractPulseNearby(server, worldPosition, 8, cost, false) < cost) break;
            living.hurt(server.damageSources().magic(), 4);
            var push = living.position().subtract(worldPosition.getCenter()).normalize().scale(0.35);
            living.setDeltaMovement(living.getDeltaMovement().add(push.x, 0.15, push.z));
            living.hurtMarked = true;
            reason = "Struck " + living.getName().getString();
            setChanged();
            return;
        }
        reason = "No hostiles within 8";
    }

    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> value) { items = value; }
    @Override public int getContainerSize() { return 27; }
    @Override protected Component getDefaultName() { return Component.translatable("block.tribalpower." + kind()); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return ChestMenu.threeRows(id, inventory, this); }
    @Override public int[] getSlotsForFace(Direction side) {
        return tk.darrow.tribalpower.lattice.SideIo.slots(this, side);
    }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return "wind_snare".equals(kind()) && !level.hasNeighborSignal(worldPosition) && sides.get(side).insert();
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return "wind_snare".equals(kind()) && !level.hasNeighborSignal(worldPosition) && sides.get(side).extract();
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putBoolean("Extract", extract);
        tag.putString("Reason", reason);
        if (!pending.isEmpty()) tag.put("Pending", pending.save(registries));
        sides.save(tag);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(27, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        extract = !tag.contains("Extract") || tag.getBoolean("Extract");
        reason = tag.getString("Reason");
        pending = FluidStack.parseOptional(registries, tag.getCompound("Pending"));
        sides.load(tag);
    }
}
