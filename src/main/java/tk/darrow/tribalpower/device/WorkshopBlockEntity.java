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
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import tk.darrow.tribalpower.item.MachineRank;
import tk.darrow.tribalpower.lattice.LatticeNetwork;

public class WorkshopBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer, tk.darrow.tribalpower.lattice.HasSideIo, tk.darrow.tribalpower.api.pulse.PulseSpend {
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    private boolean extract = true;
    private String reason = "waiting";
    private int reasonN;
    private String reasonName = "";
    private FluidStack pending = FluidStack.EMPTY;
    /** The Tide Pump's own well: what it draws from below and what fluid plates bring, until it is passed on. */
    public static final int WELL = 4000;
    public final FluidTank well = new FluidTank(WELL) {
        @Override protected void onContentsChanged() { setChanged(); }
    };
    // A pump's faces are open both ways, so fluid plates can draw from it and fill it; the other hands keep the mesh's.
    private final tk.darrow.tribalpower.lattice.SideIo sides;
    @Override public tk.darrow.tribalpower.lattice.SideIo sideIo() { return sides; }
    /** Hoppers ask for these every tick; build the slot list once. */
    private static final int[] ALL_SLOTS = java.util.stream.IntStream.range(0, 27).toArray();
    @Override public int[] inputSlots(Direction face) { return ALL_SLOTS; }
    @Override public int[] outputSlots(Direction face) { return ALL_SLOTS; }

    public WorkshopBlockEntity(BlockPos pos, BlockState state) {
        super(DeviceRegistry.WORKSHOP.get(), pos, state);
        sides = isPump(state) ? new tk.darrow.tribalpower.lattice.SideIo(tk.darrow.tribalpower.lattice.SideIo.Mode.BOTH)
                : tk.darrow.tribalpower.lattice.SideIo.mesh();
    }

    private static boolean isPump(BlockState state) { return state.is(DeviceRegistry.TIDE_PUMP.get()); }
    public boolean isPump() { return isPump(getBlockState()); }

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
            case "need_voice" -> Component.translatable("message.tribalpower.workshop.need_voice", tk.darrow.tribalpower.lattice.Voices.name(reasonName));
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
            idle(server, "paused", "");
            return;
        }
        var voice = tk.darrow.tribalpower.lattice.Voices.required(kind());
        if (voice != null && !tk.darrow.tribalpower.lattice.Voices.kept(server, worldPosition, voice)) {
            idle(server, "need_voice", voice.getSerializedName());
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
        BlockPos below = worldPosition.below();
        // A tank underneath that is already one of the pump's ends is pumped as that end, not drawn from twice.
        IFluidHandler under = below.equals(front) || below.equals(back) ? null
                : server.getCapability(Capabilities.FluidHandler.BLOCK, below, Direction.UP);
        boolean water = waterBelow(server, below);
        if ((from == null || to == null) && under == null && !water && (to == null || well.isEmpty())) {
            setReason("need_tanks");
            return;
        }
        int cost = 4;
        if (LatticeNetwork.extractPulseNearby(server, worldPosition, 8, cost, true) < cost) { setReason("need_pulse", cost); return; }
        full = false;
        int drew = water ? drawWater(server, below) : under != null ? move(under, well, 250) : 0;
        int moved = 0;
        if (to != null) {
            if (!pending.isEmpty()) {
                int flushed = to.fill(pending.copy(), IFluidHandler.FluidAction.EXECUTE);
                if (flushed > 0) { pending.shrink(flushed); setChanged(); }
                if (!pending.isEmpty()) full = true;
            }
            if (pending.isEmpty()) {
                // What the well holds goes on first; the tank-to-tank pumping fills the rest of the beat.
                moved = move(well, to, 250);
                if (from != null && moved < 250) moved += move(from, to, 250 - moved);
            }
        }
        if (drew + moved == 0) { setReason(full ? "dest_full" : "nothing"); return; }
        LatticeNetwork.extractPulseNearby(server, worldPosition, 8, cost, false);
        if (moved > 0) setReason(extract ? "drawing" : "pushing", moved);
        else setReason("drawing", drew);
        setChanged();
    }

    /** Set by {@link #move} when a receiving tank had no room this beat. */
    private boolean full;

    /** Up to {@code max} of one fluid from {@code src} into {@code dst}; what {@code dst} turns back waits as pending. */
    private int move(IFluidHandler src, IFluidHandler dst, int max) {
        FluidStack offer = src.drain(max, IFluidHandler.FluidAction.SIMULATE);
        if (offer.isEmpty()) return 0;
        int room = dst.fill(offer, IFluidHandler.FluidAction.SIMULATE);
        if (room <= 0) { full = true; return 0; }
        FluidStack taken = src.drain(room, IFluidHandler.FluidAction.EXECUTE);
        if (taken.isEmpty()) return 0;
        int accepted = dst.fill(taken.copy(), IFluidHandler.FluidAction.EXECUTE);
        if (accepted < taken.getAmount()) {
            FluidStack rest = taken.copyWithAmount(taken.getAmount() - accepted);
            if (dst != well) rest.shrink(well.fill(rest.copy(), IFluidHandler.FluidAction.EXECUTE));
            if (!rest.isEmpty()) {
                if (pending.isEmpty()) pending = rest;
                else if (FluidStack.isSameFluidSameComponents(pending, rest)) pending.grow(rest.getAmount());
            }
            setChanged();
        }
        return accepted;
    }

    /** A water source the pump can take a bucket from: still water, or a waterlogged block that gives it up. */
    private static boolean waterBelow(ServerLevel server, BlockPos below) {
        var fluid = server.getFluidState(below);
        return fluid.isSource() && fluid.getType().isSame(net.minecraft.world.level.material.Fluids.WATER)
                && server.getBlockState(below).getBlock() instanceof net.minecraft.world.level.block.BucketPickup;
    }

    /**
     * A bucket's worth from the source below, by the game's own rule for infinite water: a source with two source
     * neighbours on its level and solid ground (or more source) under it refills at once, so it stays; any other
     * source is taken up the way a bucket takes it.
     */
    private int drawWater(ServerLevel server, BlockPos below) {
        var bucket = new FluidStack(net.minecraft.world.level.material.Fluids.WATER, net.neoforged.neoforge.fluids.FluidType.BUCKET_VOLUME);
        if (well.fill(bucket.copy(), IFluidHandler.FluidAction.SIMULATE) < bucket.getAmount()) { full = true; return 0; }
        if (!infinite(server, below)) {
            BlockState state = server.getBlockState(below);
            if (!(state.getBlock() instanceof net.minecraft.world.level.block.BucketPickup pickup)
                    || pickup.pickupBlock(null, server, below, state).isEmpty()) return 0;
        }
        return well.fill(bucket, IFluidHandler.FluidAction.EXECUTE);
    }

    static boolean infinite(ServerLevel server, BlockPos pos) {
        BlockState state = server.getBlockState(pos);
        var fluid = state.getFluidState();
        if (!(state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) || !fluid.isSource()) return false;
        if (!fluid.canConvertToSource(server, pos)) return false;
        int sources = 0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            var next = server.getFluidState(pos.relative(side));
            if (next.isSource() && next.getType().isSame(fluid.getType())) sources++;
        }
        if (sources < 2) return false;
        BlockState ground = server.getBlockState(pos.below());
        return ground.isSolid() || (ground.getFluidState().isSource() && ground.getFluidState().getType().isSame(fluid.getType()));
    }

    private void vacuum(ServerLevel server) {
        int cost = 2;
        if (LatticeNetwork.extractPulseNearby(server, worldPosition, 8, cost, true) < cost) { setReason("need_pulse_stack", cost); return; }
        var box = new AABB(worldPosition).inflate(8);
        int pulled = 0;
        for (ItemEntity entity : server.getEntitiesOfClass(ItemEntity.class, box, e -> !e.isRemoved() && !e.getItem().isEmpty())) {
            ItemStack stack = entity.getItem();
            if (insertLeftover(stack, true).getCount() == stack.getCount()) continue;
            // Probe first: a real draw that comes up short would burn what it did find.
            if (!LatticeNetwork.tryExtractPulseNearby(server, worldPosition, 8, cost)) break;
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
                e -> tk.darrow.tribalpower.familiar.FamiliarRoster.hostile(e) && e.isAlive()
                        && !e.getType().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES))) {
            if (!LatticeNetwork.tryExtractPulseNearby(server, worldPosition, 8, cost)) break;
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

    /** Stopped for a reason: dark, and a comparator on it learns so at once. */
    private void idle(ServerLevel server, String key, String name) {
        setReason(key, name);
        if (getBlockState().getValue(WorkshopBlock.LIT)) server.setBlock(worldPosition, getBlockState().setValue(WorkshopBlock.LIT, false), 3);
        setChanged();
        server.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
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
        if (isPump()) {
            tag.put("Well", well.writeToNBT(registries, new CompoundTag()));
            tag.putBoolean("OpenFaces", true);
        }
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
        if (isPump()) {
            well.readFromNBT(registries, tag.getCompound("Well"));
            // Pumps saved before they had a well carried the mesh's face layout, which nothing used: open them up.
            if (!tag.getBoolean("OpenFaces"))
                sides.unpack(new tk.darrow.tribalpower.lattice.SideIo(tk.darrow.tribalpower.lattice.SideIo.Mode.BOTH).pack());
        }
    }
}
