package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.block.RelayBlock;
import tk.darrow.tribalpower.echo.RelayMenu;
import tk.darrow.tribalpower.effect.SpiritEffects;
import tk.darrow.tribalpower.item.MachineRank;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.lattice.LatticeNetwork;
import tk.darrow.tribalpower.lattice.RelayLinks;

/**
 * Face-mounted plate. Pulls from the host machine and sends to the faces a Lattice Tuner aimed it at, through a rune
 * (items or fluid). A plain plate has one channel; each rank (Echo Attune, Bind, Manifest) opens one more, up to four.
 * Every channel has its own destination and its own eight-slot whitelist or blacklist. The plate routes by priority
 * (the first channel that takes the goods) or round-robin (channels in turn); either way they share the plate's one
 * transfer a beat, so ranking scales speed and Pulse exactly as it did for one channel.
 *
 * <p>The old Bond slot is gone: a bonded pair found loaded becomes the same tuner link on channel 1 and its bond
 * items are handed back.
 */
public class WirelessRelayBlockEntity extends BlockEntity implements tk.darrow.tribalpower.api.Diagnosable, WorldlyContainer, MenuProvider, tk.darrow.tribalpower.api.pulse.PulseSpend, tk.darrow.tribalpower.camp.Ownership.Owned {
    /** LINK is the retired Bond slot, kept only so old saves load and hand their bond back. */
    public static final int LINK = 0, RUNE = 1;
    public static final int FILTERS = 8;
    public static final int MAX_CHANNELS = MachineRank.MAX + 1;
    private static final int[] NO_HOPPER = new int[0];
    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);

    /** One destination and its filter. Channel 0 saves under the plate's original tags, so old plates load as channel 1. */
    private static final class Channel {
        BlockPos target;
        String dimension = "";
        Direction face = Direction.UP;
        /** Ghost copies: never real items, never dropped, never seen by a hopper. */
        final NonNullList<ItemStack> filters = NonNullList.withSize(FILTERS, ItemStack.EMPTY);
        /** true: only listed goods pass. false (the default): listed goods are held back, so an empty list passes all. */
        boolean allow;

        boolean passes(ItemStack stack) {
            boolean listed = false;
            for (ItemStack entry : filters) {
                if (!entry.isEmpty() && ItemStack.isSameItem(entry, stack)) { listed = true; break; }
            }
            return allow == listed;
        }

        boolean passes(FluidStack fluid) {
            boolean listed = false;
            for (ItemStack entry : filters) {
                if (entry.isEmpty()) continue;
                var held = net.neoforged.neoforge.fluids.FluidUtil.getFluidContained(entry);
                if (held.isPresent() && FluidStack.isSameFluid(held.get(), fluid)) { listed = true; break; }
            }
            return allow == listed;
        }

        void save(CompoundTag tag, HolderLookup.Provider registries) {
            if (target != null) { tag.putLong("Target", target.asLong()); tag.putInt("Face", face.ordinal()); }
            tag.putString("Dimension", dimension);
            tag.putBoolean("Allow", allow);
            CompoundTag filterTag = new CompoundTag();
            ContainerHelper.saveAllItems(filterTag, filters, true, registries);
            tag.put("Filter", filterTag);
        }

        void load(CompoundTag tag, HolderLookup.Provider registries) {
            target = tag.contains("Target") ? BlockPos.of(tag.getLong("Target")) : null;
            dimension = tag.getString("Dimension");
            face = Direction.from3DDataValue(tag.getInt("Face"));
            allow = tag.getBoolean("Allow");
            for (int i = 0; i < FILTERS; i++) filters.set(i, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag.getCompound("Filter"), filters, registries);
        }
    }

    /** A channel that can deliver this beat. */
    private record Route(int channel, Level level, BlockPos pos, Direction face) {}

    private final Channel[] channels = new Channel[MAX_CHANNELS];
    /** The channel the screen shows and the tuner aims. */
    private int selected;
    private boolean roundRobin;
    /** Round-robin: the channel to offer goods to first next beat. */
    private int nextChannel;
    private int cursor;
    private String status = "unlinked";
    private boolean extract = true;
    private FluidStack pending = FluidStack.EMPTY;
    /** The channel a pending fluid remainder was drained for. */
    private int pendingChannel;
    private Route lastRoute;
    private java.util.UUID owner;

    public WirelessRelayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRELESS_RELAY.get(), pos, state);
        for (int i = 0; i < MAX_CHANNELS; i++) channels[i] = new Channel();
    }

    public Direction facing() {
        return getBlockState().hasProperty(RelayBlock.FACING) ? getBlockState().getValue(RelayBlock.FACING) : Direction.UP;
    }

    /** The machine this plate is snapped onto. Default FACING=UP keeps the host below. */
    public BlockPos host() { return worldPosition.relative(facing().getOpposite()); }

    /** Channels this plate has: one, plus one for each rank. */
    public int channelCount() { return Math.max(1, Math.min(MAX_CHANNELS, 1 + MachineRank.rank(this))); }

    public int selected() { return Math.min(selected, channelCount() - 1); }
    public void select(int channel) {
        if (channel < 0 || channel >= channelCount()) return;
        selected = channel;
        setChanged();
    }
    private Channel current() { return channels[selected()]; }

    /** Where a tuner aimed channel 1, or null. */
    public BlockPos target() { return channels[0].target; }
    /** Where a tuner aimed this channel, or null. */
    public BlockPos target(int channel) { return channels[channel].target; }

    public boolean roundRobin() { return roundRobin; }
    public void toggleRoundRobin() { roundRobin = !roundRobin; setChanged(); }

    @Override public java.util.UUID owner() { return owner; }
    @Override public void setOwner(java.util.UUID owner) { this.owner = owner; setChanged(); }

    public ItemStack link() { return items.get(LINK); }
    public boolean extracting() { return extract; }
    public void extract(boolean value) { extract = value; setChanged(); }
    public boolean allowing() { return current().allow; }
    public void toggleAllow(Player player) {
        Channel ch = current();
        ch.allow = !ch.allow;
        setChanged();
        if (player != null) player.displayClientMessage(Component.translatable(ch.allow
                ? "message.tribalpower.relay.allow" : "message.tribalpower.relay.block"), true);
    }

    /** Filter entries of the selected channel. */
    public ItemStack filter(int slot) { return current().filters.get(slot); }
    public void setFilter(int slot, ItemStack stack) {
        current().filters.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        setChanged();
    }

    /** Whether an item may cross the selected channel: same item as a filter entry, components ignored. */
    public boolean passes(ItemStack stack) { return current().passes(stack); }

    /** Whether a fluid may cross the selected channel: a bucket of water lists water. */
    public boolean passes(FluidStack fluid) { return current().passes(fluid); }

    /** The selected channel's filter row, for the menu. It follows the selection, so switching tabs swaps the row. */
    public net.minecraft.world.Container filterContainer() {
        return new net.minecraft.world.Container() {
            public int getContainerSize() { return FILTERS; }
            public boolean isEmpty() { return current().filters.stream().allMatch(ItemStack::isEmpty); }
            public ItemStack getItem(int slot) { return current().filters.get(slot); }
            public ItemStack removeItem(int slot, int count) { ItemStack was = getItem(slot); setFilter(slot, ItemStack.EMPTY); return was; }
            public ItemStack removeItemNoUpdate(int slot) { return removeItem(slot, 1); }
            public void setItem(int slot, ItemStack stack) { setFilter(slot, stack); }
            public void setChanged() { WirelessRelayBlockEntity.this.setChanged(); }
            public boolean stillValid(Player player) { return WirelessRelayBlockEntity.this.stillValid(player); }
            public void clearContent() { for (int i = 0; i < FILTERS; i++) current().filters.set(i, ItemStack.EMPTY); WirelessRelayBlockEntity.this.setChanged(); }
        };
    }

    /**
     * An old bonded pair becomes a tuner binding on channel 1 of the plate that used to push (the extracting one, or
     * the lower one when both did), aimed at the face its partner sat on. Then both bonds come back out.
     */
    private void retireBond(WirelessRelayBlockEntity partner) {
        WirelessRelayBlockEntity from = null, to = null;
        if (extract && !(partner.extract && worldPosition.asLong() > partner.worldPosition.asLong())) { from = this; to = partner; }
        else if (partner.extract) { from = partner; to = this; }
        if (from != null && to.getLevel() != null)
            from.bindChannel(0, to.host(), to.facing(), to.getLevel().dimension().location().toString());
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

    /**
     * A Fire Seal in the rune slot burns what the plate pulls instead of sending it on. It only ever burns what the
     * first channel's filter names: an empty filter burns nothing, so seating the seal can never wipe a chest.
     */
    public boolean voiding() { return items.get(RUNE).is(ModItems.FIRE_SEAL.get()); }

    public boolean fluid() {
        ItemStack rune = items.get(RUNE);
        if (rune.is(ModItems.WATER_SEAL.get())) return true;
        if (rune.is(ModItems.EARTH_SEAL.get())) return false;
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath().contains("fluid");
    }

    /** Aims the selected channel; what the tuner does. */
    public boolean bind(BlockPos target, Direction face, String dimension) { return bindChannel(selected(), target, face, dimension); }

    public boolean bindChannel(int channel, BlockPos target, Direction face, String dimension) {
        if (channel < 0 || channel >= channelCount()) return false;
        if (!reaches(target, dimension)) return false;
        Channel ch = channels[channel];
        ch.target = target.immutable(); ch.face = face; ch.dimension = dimension;
        updateStatus("waiting"); setChanged(); return true;
    }

    public void unlink(int channel) {
        if (channel < 0 || channel >= MAX_CHANNELS) return;
        channels[channel].target = null;
        if (pendingChannel == channel) pending = FluidStack.EMPTY;
        setChanged();
    }

    private boolean reaches(BlockPos target, String dimension) {
        boolean same = level != null && dimension.equals(level.dimension().location().toString());
        int range = tier() == 1 ? 32 : 128;
        return !((!same && tier() < 3) || (same && (target.equals(worldPosition) || target.equals(host())))
                || (tier() < 3 && target.distSqr(worldPosition) > (long) range * range));
    }

    /** Bit n set when channel n has a destination; the screen shows which tabs are linked. */
    public int linkedMask() {
        int mask = 0;
        for (int i = 0; i < MAX_CHANNELS; i++) if (channels[i].target != null) mask |= 1 << i;
        return mask;
    }

    public Component status() { return Component.translatable("message.tribalpower.relay." + status); }
    public int signal() { return status.equals("working") ? 15 : linkedMask() == 0 && RelayLinks.key(link()).isEmpty() ? 0 : 1; }
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
        if (be.voiding()) be.tickVoid(level);
        else be.tickChannels(level);
    }

    private void tickVoid(Level level) {
        Channel filter = channels[0];
        if (filter.filters.stream().allMatch(ItemStack::isEmpty)) { updateStatus("void_empty"); return; }
        if (!level.hasChunkAt(host())) { updateStatus("unloaded"); return; }
        int cost = pulseCost();
        if (LatticeNetwork.extractPulseNearby(level, worldPosition, 8, cost, true) < cost) { updateStatus("pulse"); return; }
        boolean burned = fluid() ? voidFluid(level, filter) : voidItem(level, filter);
        updateStatus(burned ? "working" : "waiting");
        if (!burned) return;
        LatticeNetwork.extractPulseNearby(level, worldPosition, 8, cost, false);
        setChanged();
        if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), 60) == 0)
            ((ServerLevel) level).sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.3, worldPosition.getZ() + 0.5, 6, 0.2, 0.1, 0.2, 0.01);
    }

    private boolean voidItem(Level level, Channel filter) {
        var source = level.getCapability(Capabilities.ItemHandler.BLOCK, host(), facing());
        if (source == null || source.getSlots() == 0) return false;
        int burst = MachineRank.itemBurst(this, 16);
        for (int n = 0; n < source.getSlots(); n++) {
            int slot = Math.floorMod(cursor + n, source.getSlots());
            ItemStack candidate = source.extractItem(slot, burst, true);
            if (candidate.isEmpty() || !filter.passes(candidate)) continue;
            ItemStack burned = source.extractItem(slot, candidate.getCount(), false);
            cursor = (slot + 1) % source.getSlots();
            return !burned.isEmpty();
        }
        return false;
    }

    private boolean voidFluid(Level level, Channel filter) {
        var source = level.getCapability(Capabilities.FluidHandler.BLOCK, host(), facing());
        if (source == null) return false;
        int burst = MachineRank.scalePulse(this, 250);
        for (int tank = 0; tank < source.getTanks(); tank++) {
            var inTank = source.getFluidInTank(tank);
            if (inTank.isEmpty() || !filter.passes(inTank)) continue;
            if (!source.drain(inTank.copyWithAmount(burst), IFluidHandler.FluidAction.EXECUTE).isEmpty()) return true;
        }
        return false;
    }

    private void tickChannels(Level level) {
        java.util.List<Route> routes = new java.util.ArrayList<>();
        String why = "unlinked";
        for (int i = 0; i < channelCount(); i++) {
            Channel ch = channels[i];
            if (ch.target == null) continue;
            var dim = net.minecraft.resources.ResourceLocation.tryParse(ch.dimension);
            var destination = dim == null ? null : ((ServerLevel) level).getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dim));
            if (!reaches(ch.target, ch.dimension)) continue;
            if (destination == null || !destination.hasChunkAt(ch.target)) { if (!"paused".equals(why)) why = "unloaded"; continue; }
            if (destination.hasNeighborSignal(ch.target)) { why = "paused"; continue; }
            routes.add(new Route(i, destination, ch.target, ch.face));
        }
        if (routes.isEmpty()) { updateStatus(why); return; }
        if (!level.hasChunkAt(host())) { updateStatus("unloaded"); return; }
        if (roundRobin) {
            // Start at the channel whose turn it is; the rest follow in order.
            int start = 0;
            while (start < routes.size() && routes.get(start).channel() < nextChannel) start++;
            if (start == routes.size()) start = 0;
            java.util.Collections.rotate(routes, -start);
        }
        transfer(level, routes);
    }

    @Override
    public int spendPerSecond() {
        // "pulse" means a transfer is waiting on power. "waiting" has nothing to move, so it spends nothing.
        if (!"working".equals(status) && !"pulse".equals(status)) return 0;
        return pulseCost();
    }

    private int pulseCost() {
        int cost = tk.darrow.tribalpower.config.TribalConfig.scaleConsumption(tier() == 3 ? 16 : tier() == 2 ? 8 : 4);
        return MachineRank.scalePulse(this, cost);
    }

    private void transfer(Level sourceLevel, java.util.List<Route> routes) {
        int cost = pulseCost();
        if (LatticeNetwork.extractPulseNearby(sourceLevel, worldPosition, 8, cost, true) < cost) { updateStatus("pulse"); return; }
        lastRoute = null;
        boolean moved = fluid() ? moveFluid(sourceLevel, routes) : moveItem(sourceLevel, routes);
        updateStatus(moved ? "working" : "waiting");
        if (!moved) return;
        if (roundRobin && lastRoute != null) nextChannel = (lastRoute.channel() + 1) % channelCount();
        LatticeNetwork.extractPulseNearby(sourceLevel, worldPosition, 8, cost, false);
        setChanged();
        if (lastRoute == null || Math.floorMod(sourceLevel.getGameTime() + worldPosition.asLong(), 80) != 0) return;
        Level destLevel = lastRoute.level();
        BlockPos destPos = lastRoute.pos();
        if (destLevel == sourceLevel && worldPosition.distSqr(destPos) <= 32 * 32)
            SpiritEffects.beam((ServerLevel) sourceLevel, worldPosition.getCenter(), destPos.getCenter(), fluid() ? Attunement.WATER : Attunement.AIR);
        else {
            SpiritEffects.ring((ServerLevel) sourceLevel, worldPosition.getCenter(), Attunement.SPIRIT, 0.6, 8);
            SpiritEffects.ring((ServerLevel) destLevel, destPos.getCenter(), Attunement.SPIRIT, 0.6, 8);
        }
    }

    /** One stack a beat: the first source stack some channel takes, sent to the first channel (in route order) that takes it. */
    private boolean moveItem(Level sourceLevel, java.util.List<Route> routes) {
        var source = sourceLevel.getCapability(Capabilities.ItemHandler.BLOCK, host(), facing());
        if (source == null || source.getSlots() == 0) return false;
        int burst = MachineRank.itemBurst(this, 16);
        for (int n = 0; n < source.getSlots(); n++) {
            int slot = Math.floorMod(cursor + n, source.getSlots());
            ItemStack candidate = source.extractItem(slot, burst, true);
            if (candidate.isEmpty()) continue;
            for (Route route : routes) {
                if (!channels[route.channel()].passes(candidate)) continue;
                var sink = route.level().getCapability(Capabilities.ItemHandler.BLOCK, route.pos(), route.face());
                if (sink == null || sink == source) continue;
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
                lastRoute = route;
                return transferred > 0;
            }
        }
        return false;
    }

    private boolean moveFluid(Level sourceLevel, java.util.List<Route> routes) {
        if (!pending.isEmpty()) {
            // A remainder belongs to the channel it was drained for; it waits for that one, whatever the order.
            for (Route route : routes) {
                if (route.channel() != pendingChannel) continue;
                var sink = route.level().getCapability(Capabilities.FluidHandler.BLOCK, route.pos(), route.face());
                if (sink == null) return false;
                int filled = sink.fill(pending.copy(), IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) { pending.shrink(filled); setChanged(); lastRoute = route; }
                return filled > 0;
            }
            return false;
        }
        var source = sourceLevel.getCapability(Capabilities.FluidHandler.BLOCK, host(), facing());
        if (source == null) return false;
        int burst = MachineRank.scalePulse(this, 250);
        for (Route route : routes) {
            Channel ch = channels[route.channel()];
            var sink = route.level().getCapability(Capabilities.FluidHandler.BLOCK, route.pos(), route.face());
            if (sink == null || sink == source) continue;
            var candidate = source.drain(burst, IFluidHandler.FluidAction.SIMULATE);
            if (candidate.isEmpty()) return false;
            if (!ch.passes(candidate)) {
                // A tank offers one fluid first; when that one is held back, try each listed tank's own fluid instead.
                candidate = FluidStack.EMPTY;
                for (int tank = 0; tank < source.getTanks() && candidate.isEmpty(); tank++) {
                    var inTank = source.getFluidInTank(tank);
                    if (inTank.isEmpty() || !ch.passes(inTank)) continue;
                    candidate = source.drain(inTank.copyWithAmount(burst), IFluidHandler.FluidAction.SIMULATE);
                }
                if (candidate.isEmpty()) continue;
            }
            int accepted = sink.fill(candidate, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) continue;
            var actual = source.drain(candidate.copyWithAmount(Math.min(burst, accepted)), IFluidHandler.FluidAction.EXECUTE);
            if (actual.isEmpty()) continue;
            int filled = sink.fill(actual.copy(), IFluidHandler.FluidAction.EXECUTE);
            if (filled < actual.getAmount()) {
                pending = actual.copyWithAmount(actual.getAmount() - filled);
                pendingChannel = route.channel();
                setChanged();
            }
            lastRoute = route;
            return filled > 0;
        }
        return false;
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        channels[0].save(tag, registries);
        ListTag more = new ListTag();
        for (int i = 1; i < MAX_CHANNELS; i++) {
            CompoundTag one = new CompoundTag();
            channels[i].save(one, registries);
            more.add(one);
        }
        tag.put("Channels", more);
        tag.putInt("Selected", selected);
        tag.putBoolean("RoundRobin", roundRobin);
        tag.putInt("NextChannel", nextChannel);
        tag.putInt("Cursor", cursor);
        tag.putBoolean("Extract", extract);
        if (!pending.isEmpty()) { tag.put("Pending", pending.save(registries)); tag.putInt("PendingChannel", pendingChannel); }
        tk.darrow.tribalpower.camp.Ownership.save(tag, owner);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        channels[0].load(tag, registries);
        ListTag more = tag.getList("Channels", Tag.TAG_COMPOUND);
        for (int i = 1; i < MAX_CHANNELS; i++) channels[i].load(i - 1 < more.size() ? more.getCompound(i - 1) : new CompoundTag(), registries);
        selected = tag.getInt("Selected");
        roundRobin = tag.getBoolean("RoundRobin");
        nextChannel = tag.getInt("NextChannel");
        cursor = tag.getInt("Cursor");
        extract = !tag.contains("Extract") || tag.getBoolean("Extract");
        pending = FluidStack.parseOptional(registries, tag.getCompound("Pending"));
        pendingChannel = tag.getInt("PendingChannel");
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
            public int get(int index) {
                return switch (index) {
                    case RelayMenu.DATA_SELECTED -> selected();
                    case RelayMenu.DATA_ALLOW -> allowing() ? 1 : 0;
                    case RelayMenu.DATA_CHANNELS -> channelCount();
                    case RelayMenu.DATA_ROUND_ROBIN -> roundRobin ? 1 : 0;
                    default -> linkedMask();
                };
            }
            public void set(int index, int value) {}
            public int getCount() { return RelayMenu.DATA_COUNT; }
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
    public static boolean isRune(ItemStack stack) {
        return stack.is(ModItems.WATER_SEAL.get()) || stack.is(ModItems.EARTH_SEAL.get()) || stack.is(ModItems.FIRE_SEAL.get());
    }
    @Override public int[] getSlotsForFace(Direction side) { return NO_HOPPER; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) { return false; }
    @Override public int getMaxStackSize() { return 1; }

    @Override public java.util.List<net.minecraft.network.chat.Component> diagnose(ServerLevel server, BlockPos pos) {
        java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
        lines.add(Component.translatable("diag.tribalpower.relay.state", status(), tier(), fluid() ? "fluid" : "item"));
        if (voiding()) {
            lines.add(Component.translatable("diag.tribalpower.relay.void").withStyle(net.minecraft.ChatFormatting.GOLD));
            lines.add(Component.translatable("diag.tribalpower.relay.cost", pulseCost()));
            return lines;
        }
        WirelessRelayBlockEntity partner = RelayLinks.partner(this);
        if (partner != null) {
            BlockPos p = partner.getBlockPos();
            lines.add(Component.translatable("diag.tribalpower.relay.target", p.getX(), p.getY(), p.getZ(),
                    partner.getLevel() == null ? "" : partner.getLevel().dimension().location().toString(), partner.facing().getSerializedName()));
        } else if (linkedMask() == 0) {
            lines.add(Component.translatable("diag.tribalpower.relay.unlinked").withStyle(net.minecraft.ChatFormatting.YELLOW));
            return lines;
        } else {
            if (channelCount() > 1)
                lines.add(Component.translatable(roundRobin ? "diag.tribalpower.relay.round_robin" : "diag.tribalpower.relay.priority", channelCount()));
            for (int i = 0; i < channelCount(); i++) {
                Channel ch = channels[i];
                if (ch.target == null) continue;
                Component where = Component.translatable("diag.tribalpower.relay.target", ch.target.getX(), ch.target.getY(), ch.target.getZ(), ch.dimension, ch.face.getSerializedName());
                lines.add(channelCount() > 1 ? Component.translatable("diag.tribalpower.relay.channel", i + 1, where) : where);
                var dim = net.minecraft.resources.ResourceLocation.tryParse(ch.dimension);
                var destination = dim == null ? null : server.getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dim));
                if (destination == null || !destination.hasChunkAt(ch.target)) lines.add(Component.translatable("diag.tribalpower.relay.target_unloaded").withStyle(net.minecraft.ChatFormatting.RED));
                else if (destination.hasNeighborSignal(ch.target)) lines.add(Component.translatable("diag.tribalpower.relay.target_paused").withStyle(net.minecraft.ChatFormatting.YELLOW));
            }
        }
        if (!server.hasChunkAt(host())) lines.add(Component.translatable("diag.tribalpower.relay.source_unloaded").withStyle(net.minecraft.ChatFormatting.RED));
        lines.add(Component.translatable("diag.tribalpower.relay.cost", pulseCost()));
        if (!pending.isEmpty()) lines.add(Component.translatable("diag.tribalpower.relay.pending", pending.getAmount()));
        return lines;
    }
}
