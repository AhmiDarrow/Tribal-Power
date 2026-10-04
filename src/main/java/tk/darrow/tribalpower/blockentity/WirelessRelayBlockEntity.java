package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.block.RelayBlock;
import tk.darrow.tribalpower.echo.RelayMenu;
import tk.darrow.tribalpower.effect.SpiritEffects;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.lattice.LatticeNetwork;
import tk.darrow.tribalpower.lattice.RelayLinks;

/**
 * Face-mounted plate. Pulls from the host machine into the face a Lattice Tuner bound it to, through a rune
 * (items or fluid) and an eight-slot whitelist or blacklist. The old Bond slot is gone: a bonded pair found on
 * load is turned into the same tuner binding and its bond items are handed back.
 */
public class WirelessRelayBlockEntity extends BlockEntity implements tk.darrow.tribalpower.api.Diagnosable, WorldlyContainer, MenuProvider, tk.darrow.tribalpower.api.pulse.PulseSpend, tk.darrow.tribalpower.camp.Ownership.Owned {
    /** LINK is the retired Bond slot, kept only so old saves load and hand their bond back. */
    public static final int LINK = 0, RUNE = 1;
    public static final int FILTERS = 8;
    private static final int[] NO_HOPPER = new int[0];
    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    /** Ghost copies: never real items, never dropped, never seen by a hopper. */
    private final NonNullList<ItemStack> filters = NonNullList.withSize(FILTERS, ItemStack.EMPTY);
    /** true: only listed goods pass. false (the default): listed goods are held back, so an empty list passes all. */
    private boolean allow;
    private BlockPos target;
    private String dimension = "";
    private Direction face = Direction.UP;
    private int cursor;
    private String status = "unlinked";
    private boolean extract = true;
    private net.neoforged.neoforge.fluids.FluidStack pending = net.neoforged.neoforge.fluids.FluidStack.EMPTY;
    private java.util.UUID owner;

    public WirelessRelayBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.WIRELESS_RELAY.get(), pos, state); }

    public Direction facing() {
        return getBlockState().hasProperty(RelayBlock.FACING) ? getBlockState().getValue(RelayBlock.FACING) : Direction.UP;
    }

    /** The machine this plate is snapped onto. Default FACING=UP keeps the host below. */
    public BlockPos host() { return worldPosition.relative(facing().getOpposite()); }

    @Override public java.util.UUID owner() { return owner; }
    @Override public void setOwner(java.util.UUID owner) { this.owner = owner; setChanged(); }

    public ItemStack link() { return items.get(LINK); }
    public boolean extracting() { return extract; }
    public void extract(boolean value) { extract = value; setChanged(); }
    public boolean allowing() { return allow; }
    public void toggleAllow(Player player) {
        allow = !allow;
        setChanged();
        if (player != null) player.displayClientMessage(Component.translatable(allow
                ? "message.tribalpower.relay.allow" : "message.tribalpower.relay.block"), true);
    }

    public ItemStack filter(int slot) { return filters.get(slot); }
    public void setFilter(int slot, ItemStack stack) {
        filters.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        setChanged();
    }

    /** Whether an item may cross: same item as a filter entry, components ignored. */
    public boolean passes(ItemStack stack) {
        boolean listed = false;
        for (ItemStack entry : filters) {
            if (!entry.isEmpty() && ItemStack.isSameItem(entry, stack)) { listed = true; break; }
        }
        return allow == listed;
    }

    /** Whether a fluid may cross: a filter entry lists the fluid it holds, so a bucket of water lists water. */
    public boolean passes(net.neoforged.neoforge.fluids.FluidStack fluid) {
        boolean listed = false;
        for (ItemStack entry : filters) {
            if (entry.isEmpty()) continue;
            var held = net.neoforged.neoforge.fluids.FluidUtil.getFluidContained(entry);
            if (held.isPresent() && net.neoforged.neoforge.fluids.FluidStack.isSameFluid(held.get(), fluid)) { listed = true; break; }
        }
        return allow == listed;
    }

    /** The filter row as a container for the menu. Writes go straight to the ghost list. */
    public net.minecraft.world.Container filterContainer() {
        return new net.minecraft.world.Container() {
            public int getContainerSize() { return FILTERS; }
            public boolean isEmpty() { return filters.stream().allMatch(ItemStack::isEmpty); }
            public ItemStack getItem(int slot) { return filters.get(slot); }
            public ItemStack removeItem(int slot, int count) { ItemStack was = filters.get(slot); setFilter(slot, ItemStack.EMPTY); return was; }
            public ItemStack removeItemNoUpdate(int slot) { return removeItem(slot, 1); }
            public void setItem(int slot, ItemStack stack) { setFilter(slot, stack); }
            public void setChanged() { WirelessRelayBlockEntity.this.setChanged(); }
            public boolean stillValid(Player player) { return WirelessRelayBlockEntity.this.stillValid(player); }
            public void clearContent() { for (int i = 0; i < FILTERS; i++) filters.set(i, ItemStack.EMPTY); WirelessRelayBlockEntity.this.setChanged(); }
        };
    }

    /**
     * An old bonded pair becomes a tuner binding on the plate that used to push (the extracting one, or the
     * lower one when both did), aimed at the face its partner sat on. Then both bonds come back out.
     */
    private void retireBond(WirelessRelayBlockEntity partner) {
        WirelessRelayBlockEntity from = null, to = null;
        if (extract && !(partner.extract && worldPosition.asLong() > partner.worldPosition.asLong())) { from = this; to = partner; }
        else if (partner.extract) { from = partner; to = this; }
        if (from != null && to.getLevel() != null)
            from.bind(to.host(), to.facing(), to.getLevel().dimension().location().toString());
        popBond();
        partner.popBond();
    }

    private void popBond() {
        ItemStack bond = items.get(LINK);
        if (bond.isEmpty() || level == null) return;
        items.set(LINK, ItemStack.EMPTY);
        RelayLinks.drop(this);
        net.minecraft.world.level.block.Block.popResource(level, worldPosition, bond);
        setChanged();
    }

    public int tier() {
        String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath();
        return id.startsWith("astral_") ? 3 : id.startsWith("longreach_") ? 2 : 1;
    }

    public boolean fluid() {
        ItemStack rune = items.get(RUNE);
        if (rune.is(ModItems.WATER_SEAL.get())) return true;
        if (rune.is(ModItems.EARTH_SEAL.get())) return false;
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath().contains("fluid");
    }

    public boolean bind(BlockPos target, Direction face, String dimension) {
        boolean same = level != null && dimension.equals(level.dimension().location().toString());
        int range = tier() == 1 ? 32 : 128;
        if ((!same && tier() < 3) || (same && (target.equals(worldPosition) || target.equals(host())))
                || (tier() < 3 && target.distSqr(worldPosition) > range * range)) return false;
        this.target = target.immutable(); this.face = face; this.dimension = dimension; updateStatus("waiting"); setChanged(); return true;
    }

    public Component status() { return Component.translatable("message.tribalpower.relay." + status); }
    public int signal() { return status.equals("working") ? 15 : target == null && RelayLinks.key(link()).isEmpty() ? 0 : 1; }
    private void updateStatus(String value) {
        if (status.equals(value)) return;
        status = value;
        if (level != null) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    @Override public void onLoad() { super.onLoad(); RelayLinks.index(this); }
    @Override public void setRemoved() { RelayLinks.drop(this); super.setRemoved(); }

    public static void tick(Level level, BlockPos pos, BlockState state, WirelessRelayBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
        if (level.hasNeighborSignal(pos)) { be.updateStatus("paused"); return; }
        // a relay carries through the air, an Astral one along the Loom's threads: it wants that voice kept nearby
        if (!tk.darrow.tribalpower.lattice.Voices.kept(level, pos, tk.darrow.tribalpower.lattice.Voices.relay(be.tier()))) { be.updateStatus("voice"); return; }
        WirelessRelayBlockEntity partner = RelayLinks.partner(be);
        if (partner != null) {
            be.retireBond(partner);
            return;
        }
        if (!tk.darrow.tribalpower.lattice.RelayLinks.key(be.link()).isEmpty()) {
            // An old bond still waits for its partner rather than sending into a leftover tuner mark. Opening the
            // plate hands the bond back, so one whose partner is gone does not stay stuck in a slot nobody can see.
            be.updateStatus("unlinked");
            return;
        }
        if (be.target == null) { be.updateStatus("unlinked"); return; }
        be.tickBound(level, pos);
    }

    private void tickBound(Level level, BlockPos pos) {
        var dim = net.minecraft.resources.ResourceLocation.tryParse(dimension);
        var destination = dim == null ? null : ((ServerLevel) level).getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dim));
        boolean same = destination == level;
        int range = tier() == 1 ? 32 : 128;
        if ((!same && tier() < 3) || (same && (target.equals(pos) || target.equals(host())))
                || (tier() < 3 && target.distSqr(pos) > range * range)) { updateStatus("unlinked"); return; }
        if (destination == null || !destination.hasChunkAt(target) || !level.hasChunkAt(host())) { updateStatus("unloaded"); return; }
        if (destination.hasNeighborSignal(target)) { updateStatus("paused"); return; }
        transfer(level, host(), facing(), destination, target, face);
    }

    @Override
    public int spendPerSecond() {
        // "pulse" means a transfer is waiting on power. "waiting" has nothing to move, so it spends nothing.
        if (!"working".equals(status) && !"pulse".equals(status)) return 0;
        return pulseCost();
    }

    private int pulseCost() {
        int cost = tk.darrow.tribalpower.config.TribalConfig.scaleConsumption(tier() == 3 ? 16 : tier() == 2 ? 8 : 4);
        return tk.darrow.tribalpower.item.MachineRank.scalePulse(this, cost);
    }

    private void transfer(Level sourceLevel, BlockPos sourcePos, Direction sourceFace, Level destLevel, BlockPos destPos, Direction destFace) {
        int cost = pulseCost();
        if (LatticeNetwork.extractPulseNearby(sourceLevel, worldPosition, 8, cost, true) < cost) { updateStatus("pulse"); return; }
        boolean moved = fluid() ? moveFluid(sourceLevel, sourcePos, sourceFace, destLevel, destPos, destFace)
                : moveItem(sourceLevel, sourcePos, sourceFace, destLevel, destPos, destFace);
        updateStatus(moved ? "working" : "waiting");
        if (!moved) return;
        LatticeNetwork.extractPulseNearby(sourceLevel, worldPosition, 8, cost, false);
        setChanged();
        if (Math.floorMod(sourceLevel.getGameTime() + worldPosition.asLong(), 80) != 0) return;
        if (destLevel == sourceLevel && worldPosition.distSqr(destPos) <= 32 * 32)
            SpiritEffects.beam((ServerLevel) sourceLevel, worldPosition.getCenter(), destPos.getCenter(), fluid() ? Attunement.WATER : Attunement.AIR);
        else {
            SpiritEffects.ring((ServerLevel) sourceLevel, worldPosition.getCenter(), Attunement.SPIRIT, 0.6, 8);
            SpiritEffects.ring((ServerLevel) destLevel, destPos.getCenter(), Attunement.SPIRIT, 0.6, 8);
        }
    }

    private boolean moveItem(Level sourceLevel, BlockPos sourcePos, Direction sourceFace, Level destLevel, BlockPos destPos, Direction destFace) {
        var source = sourceLevel.getCapability(Capabilities.ItemHandler.BLOCK, sourcePos, sourceFace);
        var sink = destLevel.getCapability(Capabilities.ItemHandler.BLOCK, destPos, destFace);
        if (source == null || sink == null || source == sink || source.getSlots() == 0) return false;
        for (int n = 0; n < source.getSlots(); n++) {
            int slot = Math.floorMod(cursor + n, source.getSlots());
            ItemStack candidate = source.extractItem(slot, tk.darrow.tribalpower.item.MachineRank.itemBurst(this, 16), true);
            if (candidate.isEmpty() || !passes(candidate)) continue;
            int accepted = candidate.getCount() - ItemHandlerHelper.insertItemStacked(sink, candidate, true).getCount();
            if (accepted <= 0) continue;
            ItemStack actual = source.extractItem(slot, accepted, false);
            if (actual.isEmpty()) continue;
            int count = actual.getCount();
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(sink, actual, false);
            int transferred = count - remainder.getCount();
            if (!remainder.isEmpty()) {
                remainder = ItemHandlerHelper.insertItemStacked(source, remainder, false);
                if (!remainder.isEmpty()) Containers.dropItemStack(sourceLevel, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, remainder);
            }
            cursor = (slot + 1) % source.getSlots();
            return transferred > 0;
        }
        return false;
    }

    private boolean moveFluid(Level sourceLevel, BlockPos sourcePos, Direction sourceFace, Level destLevel, BlockPos destPos, Direction destFace) {
        var sink = destLevel.getCapability(Capabilities.FluidHandler.BLOCK, destPos, destFace);
        if (sink == null) return false;
        if (!pending.isEmpty()) {
            int filled = sink.fill(pending.copy(), IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) { pending.shrink(filled); setChanged(); }
            return filled > 0;
        }
        var source = sourceLevel.getCapability(Capabilities.FluidHandler.BLOCK, sourcePos, sourceFace);
        if (source == null || source == sink) return false;
        int burst = tk.darrow.tribalpower.item.MachineRank.scalePulse(this, 250);
        var candidate = source.drain(burst, IFluidHandler.FluidAction.SIMULATE);
        if (candidate.isEmpty()) return false;
        if (!passes(candidate)) {
            // A tank offers one fluid first; when that one is held back, try each listed tank's own fluid instead.
            candidate = net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            for (int tank = 0; tank < source.getTanks() && candidate.isEmpty(); tank++) {
                var inTank = source.getFluidInTank(tank);
                if (inTank.isEmpty() || !passes(inTank)) continue;
                candidate = source.drain(inTank.copyWithAmount(burst), IFluidHandler.FluidAction.SIMULATE);
            }
            if (candidate.isEmpty()) return false;
        }
        int accepted = sink.fill(candidate, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return false;
        var actual = source.drain(candidate.copyWithAmount(Math.min(burst, accepted)), IFluidHandler.FluidAction.EXECUTE);
        if (actual.isEmpty()) return false;
        int filled = sink.fill(actual.copy(), IFluidHandler.FluidAction.EXECUTE);
        if (filled < actual.getAmount()) {
            pending = actual.copyWithAmount(actual.getAmount() - filled);
            setChanged();
        }
        return filled > 0;
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        if (target != null) { tag.putLong("Target", target.asLong()); tag.putInt("Face", face.ordinal()); }
        tag.putString("Dimension", dimension);
        tag.putInt("Cursor", cursor);
        tag.putBoolean("Extract", extract);
        tag.putBoolean("Allow", allow);
        CompoundTag filterTag = new CompoundTag();
        ContainerHelper.saveAllItems(filterTag, filters, true, registries);
        tag.put("Filter", filterTag);
        if (!pending.isEmpty()) tag.put("Pending", pending.save(registries));
        tk.darrow.tribalpower.camp.Ownership.save(tag, owner);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        target = tag.contains("Target") ? BlockPos.of(tag.getLong("Target")) : null;
        dimension = tag.getString("Dimension");
        face = Direction.from3DDataValue(tag.getInt("Face")); cursor = tag.getInt("Cursor");
        extract = !tag.contains("Extract") || tag.getBoolean("Extract");
        allow = tag.getBoolean("Allow");
        for (int i = 0; i < FILTERS; i++) filters.set(i, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag.getCompound("Filter"), filters, registries);
        pending = net.neoforged.neoforge.fluids.FluidStack.parseOptional(registries, tag.getCompound("Pending"));
        owner = tk.darrow.tribalpower.camp.Ownership.load(tag);
    }

    @Override public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        ItemStack bond = items.get(LINK);
        if (!bond.isEmpty() && level != null && !level.isClientSide) {
            items.set(LINK, ItemStack.EMPTY);
            RelayLinks.drop(this);
            inv.placeItemBackInInventory(bond);
            setChanged();
        }
        return new RelayMenu(id, inv, this, filterContainer(), new ContainerData() {
            public int get(int index) { return index == 1 ? (allow ? 1 : 0) : (extract ? 1 : 0); }
            public void set(int index, int value) { if (index == 1) allow = value != 0; }
            public int getCount() { return 2; }
        });
    }

    @Override public int getContainerSize() { return 2; }
    @Override public boolean isEmpty() { return items.get(LINK).isEmpty() && items.get(RUNE).isEmpty(); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int count) {
        ItemStack taken = ContainerHelper.removeItem(items, slot, count);
        if (!taken.isEmpty()) { if (slot == LINK) RelayLinks.index(this); setChanged(); }
        return taken;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack taken = ContainerHelper.takeItem(items, slot);
        if (!taken.isEmpty()) { if (slot == LINK) RelayLinks.index(this); setChanged(); }
        return taken;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(1);
        if (slot == LINK) RelayLinks.index(this);
        setChanged();
    }
    @Override public void clearContent() { items.clear(); RelayLinks.drop(this); setChanged(); }
    @Override public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this && player.distanceToSqr(worldPosition.getCenter()) <= 64.0;
    }
    /** No new bonds; the rune slot takes the two seals that choose items or fluid. */
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == RUNE && isRune(stack); }
    public static boolean isRune(ItemStack stack) { return stack.is(ModItems.WATER_SEAL.get()) || stack.is(ModItems.EARTH_SEAL.get()); }
    @Override public int[] getSlotsForFace(Direction side) { return NO_HOPPER; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) { return false; }
    @Override public int getMaxStackSize() { return 1; }

    @Override public java.util.List<net.minecraft.network.chat.Component> diagnose(ServerLevel server, BlockPos pos) {
        java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
        lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.relay.state", status(), tier(), fluid() ? "fluid" : "item"));
        WirelessRelayBlockEntity partner = RelayLinks.partner(this);
        if (partner != null) {
            BlockPos p = partner.getBlockPos();
            lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.relay.target", p.getX(), p.getY(), p.getZ(),
                    partner.getLevel() == null ? dimension : partner.getLevel().dimension().location().toString(), partner.facing().getSerializedName()));
        } else if (target == null) {
            lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.relay.unlinked").withStyle(net.minecraft.ChatFormatting.YELLOW));
            return lines;
        } else {
            lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.relay.target", target.getX(), target.getY(), target.getZ(), dimension, face.getSerializedName()));
            var dim = net.minecraft.resources.ResourceLocation.tryParse(dimension);
            var destination = dim == null ? null : server.getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dim));
            if (destination == null || !destination.hasChunkAt(target)) lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.relay.target_unloaded").withStyle(net.minecraft.ChatFormatting.RED));
            else if (destination.hasNeighborSignal(target)) lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.relay.target_paused").withStyle(net.minecraft.ChatFormatting.YELLOW));
        }
        if (!server.hasChunkAt(host())) lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.relay.source_unloaded").withStyle(net.minecraft.ChatFormatting.RED));
        lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.relay.cost", pulseCost()));
        if (!pending.isEmpty()) lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.relay.pending", pending.getAmount()));
        return lines;
    }
}
