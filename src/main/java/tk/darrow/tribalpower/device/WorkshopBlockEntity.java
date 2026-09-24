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

public class WorkshopBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer, tk.darrow.tribalpower.lattice.HasSideIo, tk.darrow.tribalpower.api.pulse.PulseSpend {
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    private boolean extract = true;
    private String reason = "waiting";
    private int reasonN;
    private String reasonName = "";
    private FluidStack pending = FluidStack.EMPTY;
    private final tk.darrow.tribalpower.lattice.SideIo sides = tk.darrow.tribalpower.lattice.SideIo.mesh();
    @Override public tk.darrow.tribalpower.lattice.SideIo sideIo() { return sides; }
    @Override public int[] inputSlots(Direction face) { return java.util.stream.IntStream.range(0, 27).toArray(); }
    @Override public int[] outputSlots(Direction face) { return java.util.stream.IntStream.range(0, 27).toArray(); }

    public WorkshopBlockEntity(BlockPos pos, BlockState state) {
        super(DeviceRegistry.WORKSHOP.get(), pos, state);
    }

    public String kind() { return BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath(); }

    @Override
    public int spendPerSecond() {
        int unit = switch (kind()) {
            case "tide_pump" -> 4;
            case "wind_snare" -> 2;
            case "ward_drum" -> 8;
            default -> 0;
        };
        if (unit == 0) return 0;
        return switch (reason) {
            case "drawing", "pushing", "need_pulse", "struck" -> unit;
            case "caught" -> unit * Math.max(1, reasonN);
            case "need_pulse_stack" -> unit;
            default -> 0;
        };
    }

    public Component status() {
        return switch (reason) {
            case "need_pulse" -> Component.translatable("message.tribalpower.workshop.need_pulse", reasonN);
            case "need_pulse_stack" -> Component.translatable("message.tribalpower.workshop.need_pulse_stack", reasonN);
            case "drawing" -> Component.translatable("message.tribalpower.workshop.drawing", reasonN);
            case "pushing" -> Component.translatable("message.tribalpower.workshop.pushing", reasonN);
            case "caught" -> Component.translatable("message.tribalpower.workshop.caught", reasonN);
            case "struck" -> Component.translatable("message.tribalpower.workshop.struck", reasonName);
            case "need_voice" -> Component.translatable("message.tribalpower.workshop.need_voice", reasonName);
            case "waiting", "paused", "need_tanks", "dest_full", "nothing", "listening", "idle_ward", "draw_face", "push_face"
                    -> Component.translatable("message.tribalpower.workshop." + reason);
            default -> Component.literal(reason);
        };
    }

    public int signal() {
        return switch (kind()) {
            case "tide_pump" -> extract ? 15 : 1;
            case "wind_snare" -> AbstractContainerMenu.getRedstoneSignalFromContainer(this);
            case "ward_drum" -> "struck".equals(reason) || reason.startsWith("Struck") ? 15 : 0;
            default -> 0;
        };
    }

    public void cycle(Player player) {
        if ("tide_pump".equals(kind())) {
            extract = !extract;
            setReason(extract ? "draw_face" : "push_face");
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
        if (server.hasNeighborSignal(worldPosition)) {
            setReason("paused");
            if (getBlockState().getValue(WorkshopBlock.LIT)) server.setBlock(worldPosition, getBlockState().setValue(WorkshopBlock.LIT, false), 3);
            return;
        }
        var voice = tk.darrow.tribalpower.lattice.Voices.required(kind());
        if (voice != null && !tk.darrow.tribalpower.lattice.Voices.kept(server, worldPosition, voice)) {
            setReason("need_voice"); reasonName = tk.darrow.tribalpower.lattice.Voices.name(voice).getString();
            if (getBlockState().getValue(WorkshopBlock.LIT)) server.setBlock(worldPosition, getBlockState().setValue(WorkshopBlock.LIT, false), 3);
            return;
        }
        switch (kind()) {
            case "tide_pump" -> pump(server);
            case "wind_snare" -> vacuum(server);
            case "ward_drum" -> fight(server);
        }
        boolean idle = switch (reason) {
            case "waiting", "paused", "need_pulse", "need_pulse_stack", "need_tanks" -> true;
            default -> reason.startsWith("Paused") || reason.startsWith("Need") || reason.startsWith("Waiting");
        };
        if (getBlockState().getValue(WorkshopBlock.LIT) != !idle)
            server.setBlock(worldPosition, getBlockState().setValue(WorkshopBlock.LIT, !idle), 3);
        server.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    private void pump(ServerLevel server) {
        Direction face = getBlockState().getValue(WorkshopBlock.FACING);
        BlockPos front = worldPosition.relative(face);
        BlockPos back = worldPosition.relative(face.getOpposite());
        IFluidHandler from = server.getCapability(Capabilities.FluidHandler.BLOCK, extract ? front : back, extract ? face.getOpposite() : face);
        IFluidHandler to = server.getCapability(Capabilities.FluidHandler.BLOCK, extract ? back : front, extract ? face : face.getOpposite());
        if (from == null || to == null) { setReason("need_tanks"); return; }
        int cost = 4;
        if (LatticeNetwork.extractPulseNearby(server, worldPosition, 8, cost, true) < cost) { setReason("need_pulse", cost); return; }
        if (!pending.isEmpty()) {
            int flushed = to.fill(pending.copy(), IFluidHandler.FluidAction.EXECUTE);
            if (flushed > 0) { pending.shrink(flushed); setChanged(); }
            if (!pending.isEmpty()) { setReason("dest_full"); return; }
        }
        FluidStack drained = from.drain(250, IFluidHandler.FluidAction.SIMULATE);
        if (drained.isEmpty()) { setReason("nothing"); return; }
        int filled = to.fill(drained, IFluidHandler.FluidAction.SIMULATE);
        if (filled <= 0) { setReason("dest_full"); return; }
        FluidStack moved = from.drain(filled, IFluidHandler.FluidAction.EXECUTE);
        if (moved.isEmpty()) { setReason("nothing"); return; }
        int accepted = to.fill(moved, IFluidHandler.FluidAction.EXECUTE);
        if (accepted < moved.getAmount()) {
            pending = moved.copy();
            pending.setAmount(moved.getAmount() - accepted);
        }
        LatticeNetwork.extractPulseNearby(server, worldPosition, 8, cost, false);
        setReason(extract ? "drawing" : "pushing", accepted);
        setChanged();
    }

    private void vacuum(ServerLevel server) {
        int cost = 2;
        if (LatticeNetwork.extractPulseNearby(server, worldPosition, 8, cost, true) < cost) { setReason("need_pulse_stack", cost); return; }
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
        if (pulled > 0) setReason("caught", pulled);
        else setReason("listening");
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
        if (LatticeNetwork.extractPulseNearby(server, worldPosition, 8, cost, true) < cost) { setReason("need_pulse", cost); return; }
        var box = new AABB(worldPosition).inflate(8);
        for (var living : server.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box,
                e -> tk.darrow.tribalpower.familiar.FamiliarRoster.hostile(e) && e.isAlive())) {
            if (LatticeNetwork.extractPulseNearby(server, worldPosition, 8, cost, false) < cost) break;
            living.hurt(server.damageSources().magic(), 4);
            var push = living.position().subtract(worldPosition.getCenter()).normalize().scale(0.35);
            living.setDeltaMovement(living.getDeltaMovement().add(push.x, 0.15, push.z));
            living.hurtMarked = true;
            setReason("struck", living.getName().getString());
            setChanged();
            return;
        }
        setReason("idle_ward");
    }

    private void setReason(String key) { reason = key; reasonN = 0; reasonName = ""; }
    private void setReason(String key, int n) { reason = key; reasonN = n; reasonName = ""; }
    private void setReason(String key, String name) { reason = key; reasonN = 0; reasonName = name; }

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
        tag.putInt("ReasonN", reasonN);
        tag.putString("ReasonName", reasonName);
        if (!pending.isEmpty()) tag.put("Pending", pending.save(registries));
        sides.save(tag);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(27, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        extract = !tag.contains("Extract") || tag.getBoolean("Extract");
        reason = tag.getString("Reason");
        if (reason.isEmpty()) reason = "waiting";
        reasonN = tag.getInt("ReasonN");
        reasonName = tag.getString("ReasonName");
        pending = FluidStack.parseOptional(registries, tag.getCompound("Pending"));
        sides.load(tag);
    }
}
