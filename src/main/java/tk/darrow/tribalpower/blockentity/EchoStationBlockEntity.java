package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.echo.ProcessingRecipes;
import tk.darrow.tribalpower.lattice.Keeping;
import tk.darrow.tribalpower.lattice.LatticeNetwork;
import tk.darrow.tribalpower.effect.SpiritEffects;

/**
 * Slot 0 input, slots 1-8 output, slots 9-10 catalysts (for work that consumes more than its input, such as
 * gear ranks). One work beat per second; no per-tick lattice volume scans.
 */
public class EchoStationBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, tk.darrow.tribalpower.api.Diagnosable, tk.darrow.tribalpower.lattice.HasSideIo, tk.darrow.tribalpower.api.pulse.PulseSpend {
    public static final int CATALYST_A = 9, CATALYST_B = 10, SIZE = 11;
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int work;
    private String recipeId = "";
    private String state = "idle";
    private boolean arrayed;
    private int shownSeconds = 1, shownPulse;
    private final tk.darrow.tribalpower.pattern.PatternState array =
            new tk.darrow.tribalpower.pattern.PatternState(tk.darrow.tribalpower.pattern.ModPatterns.SHATTER_ARRAY);
    private final tk.darrow.tribalpower.lattice.SideIo sides = tk.darrow.tribalpower.lattice.SideIo.station();
    private static final int[] INPUT_SLOTS = {0, CATALYST_A, CATALYST_B};
    private static final int[] OUTPUT_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] CATALYST_SLOTS = {CATALYST_A, CATALYST_B};
    /** The menu's state index, in the order StationMenu reads it back. */
    private static final java.util.List<String> STATES = java.util.List.of("idle", "working", "paused", "full", "attunement", "pulse", "quiet", "catalyst", "silent", "lattice");

    @Override public tk.darrow.tribalpower.lattice.SideIo sideIo() { return sides; }
    @Override public int[] inputSlots(Direction face) { return INPUT_SLOTS; }
    @Override public int[] outputSlots(Direction face) { return OUTPUT_SLOTS; }

    /** The Shatter Array pays a quarter of the Pulse back and empties the station into the cache. */
    public static final double ARRAY_DISCOUNT = 0.75;
    /** Where the array seats its totems, as offsets from the station. Rotation-invariant as a set. */
    private static final int[][] CORNERS = {{2, 2}, {2, -2}, {-2, 2}, {-2, -2}};
    public EchoStationBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.ECHO_STATION.get(), pos, state); }
    /** The block id, read from the registry once: every hopper probe asks which station this is. */
    private String station;
    public String station() {
        if (station == null) station = BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath();
        return station;
    }
    /** A hopper asks about each slot in turn; the redstone hold is read once a tick, not once a slot. */
    private final HeldSignal held = new HeldSignal();
    /** The block saw a neighbour change: read the redstone hold afresh. */
    public void neighbourChanged() { held.forget(); }
    /** Redstone held high closes it to hoppers and pipes; read at most once a tick. */
    public boolean stilled() { return level != null && held.get(level, worldPosition); }
    public int work() { return work; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> value) { items = value; }
    @Override public int getContainerSize() { return SIZE; }
    @Override protected Component getDefaultName() { return Component.translatable("block.tribalpower." + station()); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new tk.darrow.tribalpower.echo.StationMenu(id, inv, this, new ContainerData() {
            public int get(int index) {
                // The beat stores these; recomputing here rescanned the lattice per index, per viewer, per tick.
                return switch(index) {
                    case 0 -> work;
                    case 1 -> shownSeconds;
                    case 3 -> sides.pack();
                    case 7 -> shownPulse;
                    case 4 -> worldPosition.getX();
                    case 5 -> worldPosition.getY();
                    case 6 -> worldPosition.getZ();
                    case 8 -> worldPosition.getX() >> 16;
                    case 9 -> worldPosition.getY() >> 16;
                    case 10 -> worldPosition.getZ() >> 16;
                    default -> Math.max(0, STATES.indexOf(state));
                };
            }
            public void set(int index, int value) {}
            public int getCount() { return 11; }
        });
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == CATALYST_A || slot == CATALYST_B) return catalystSlotAccepts(stack);
        return slot == 0 && ProcessingRecipes.find(level, station(), stack) != null;
    }

    /**
     * Gear crystals, or whatever the seated job consumes. Echo Attune also takes glowstone dust before the
     * glass is in, because that dust is how quartz glass becomes lit glass.
     */
    private boolean catalystSlotAccepts(ItemStack stack) {
        if (tk.darrow.tribalpower.item.SpiritGear.isCatalyst(stack)) return true;
        if ("echo_attune".equals(station()) && stack.is(net.minecraft.world.item.Items.GLOWSTONE_DUST)) return true;
        var formula = ProcessingRecipes.find(level, station(), items.get(0));
        return formula != null && asksFor(formula, stack);
    }

    /** Whether the seated job lists {@code stack} among its catalysts. */
    private static boolean asksFor(ProcessingRecipes.Formula formula, ItemStack stack) {
        for (ItemStack want : formula.catalysts()) if (ItemStack.isSameItem(want, stack)) return true;
        return false;
    }
    @Override public int[] getSlotsForFace(Direction face) { return tk.darrow.tribalpower.lattice.SideIo.slots(this, face); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) {
        if (slot == CATALYST_A || slot == CATALYST_B) {
            // Automation only feeds a catalyst the seated input actually asks for, so ordinary inputs never pile up there.
            var formula = ProcessingRecipes.find(level, station(), items.get(0));
            if (formula == null || !asksFor(formula, stack)) return false;
        }
        return !held.get(level, worldPosition) && (face == null || sides.get(face).insert()) && canPlaceItem(slot, stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        return !held.get(level, worldPosition) && (face == null || sides.get(face).extract()) && slot > 0 && slot < CATALYST_A;
    }
    public Component status() { return Component.translatable("message.tribalpower.station." + state, work); }

    /** True when this station stands in a Shatter Array whose totems match {@code attunement}. */
    public boolean arrayed(Level level, tk.darrow.tribalpower.api.pulse.Attunement attunement) {
        if (!array.satisfied(level, worldPosition, 1)) return false;
        for (int[] offset : CORNERS) {
            BlockPos corner = worldPosition.offset(offset[0], 0, offset[1]);
            if (!level.hasChunkAt(corner)) return false;
            if (!(level.getBlockEntity(corner) instanceof ResonanceTotemBlockEntity totem)) return false;
            if (totem.getAttunement() != attunement) return false;
        }
        return true;
    }

    /** The Ancestral Cache beneath an arrayed station, or null. */
    private AncestralCacheBlockEntity cacheBelow(Level level) {
        BlockPos below = worldPosition.below();
        return level.hasChunkAt(below) && level.getBlockEntity(below) instanceof AncestralCacheBlockEntity cache
                && !level.hasNeighborSignal(below) ? cache : null;
    }

    /**
     * Empties the output slots into the cache beneath. Only an arrayed station does this.
     *
     * <p>The station has to mark itself changed here, not only the cache: the tick can return early for
     * want of Pulse or attunement straight after a drain, and an unload at that moment would restore the
     * stacks this just gave away -- the same items in two places.
     */
    private void drainToCache(Level level) {
        AncestralCacheBlockEntity cache = cacheBelow(level);
        if (cache == null) return;
        boolean drained = false;
        for (int slot = 1; slot < 9; slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) continue;
            for (int target = 0; target < cache.getContainerSize() && !stack.isEmpty(); target++) {
                ItemStack held = cache.getItem(target);
                if (held.isEmpty()) {
                    cache.setItem(target, stack.copy());
                    items.set(slot, ItemStack.EMPTY);
                    stack = ItemStack.EMPTY;
                    drained = true;
                } else if (ItemStack.isSameItemSameComponents(held, stack)) {
                    int moved = Math.min(stack.getCount(), held.getMaxStackSize() - held.getCount());
                    if (moved <= 0) continue;
                    held.grow(moved);
                    stack.shrink(moved);
                    if (stack.isEmpty()) items.set(slot, ItemStack.EMPTY);
                    drained = true;
                }
            }
        }
        if (drained) { cache.setChanged(); setChanged(); }
    }

    /** Whether the result and a freed cell both fit, tried together on a copy of the output slots. */
    private boolean fits(ItemStack result, ItemStack extra) {
        var copy = net.minecraft.core.NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 1; i < 9; i++) copy.set(i, items.get(i).copy());
        return place(copy, result, false) && (extra.isEmpty() || place(copy, extra, false));
    }

    private boolean placeOutput(ItemStack result, boolean simulate) {
        return place(items, result, simulate);
    }

    private boolean place(java.util.List<ItemStack> items, ItemStack result, boolean simulate) {
        int remaining = result.getCount();
        for (int i = 1; i < 9; i++) {
            ItemStack current = items.get(i);
            if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, result)) continue;
            int capacity = Math.min(getMaxStackSize(), result.getMaxStackSize()) - current.getCount();
            int count = Math.min(remaining, Math.max(0, capacity));
            if (!simulate && count > 0) {
                if (current.isEmpty()) items.set(i, result.copyWithCount(count));
                else current.grow(count);
            }
            remaining -= count;
            if (remaining == 0) return true;
        }
        return false;
    }
    public static void tick(Level level, BlockPos pos, BlockState blockState, EchoStationBlockEntity be) {
        tk.darrow.tribalpower.lattice.SideIoAdjacency.beat(level, be);
        if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
        var recipe = ProcessingRecipes.find(level, be.station(), be.items.get(0));
        if (recipe == null) {
            // Only a real change is worth saving the chunk and waking comparators for.
            boolean changed = be.work != 0 || !be.recipeId.isEmpty() || !"idle".equals(be.state);
            be.work = 0; be.recipeId = ""; be.state = "idle"; be.shownSeconds = 1; be.shownPulse = 0;
            if (changed) be.setChanged();
            return;
        }
        if (!recipe.id().toString().equals(be.recipeId)) { be.work = 0; be.recipeId = recipe.id().toString(); be.setChanged(); }
        if (level.hasNeighborSignal(pos)) { be.state = "paused"; return; }
        ItemStack result = recipe.result();
        // Voice, keeping (twice) and feeding all ask about the same totems: found once for the beat.
        var totems = LatticeNetwork.TotemsNear.of(level, pos, LatticeNetwork.DEFAULT_RADIUS);
        be.arrayed = be.arrayed(level, recipe.attunement());
        if (be.arrayed) be.drainToCache(level);
        be.shownSeconds = be.workSeconds(recipe, totems);
        be.shownPulse = be.pulsePerSecond(recipe, be.arrayed);
        // Taking a piece apart hands back the Pulse Cell seated in it.
        ItemStack freedCell = tk.darrow.tribalpower.item.GearCell.accepts(result) ? ItemStack.EMPTY
                : tk.darrow.tribalpower.item.GearCell.asStack(be.items.get(0));
        if (!be.fits(result, freedCell)) { be.state = "full"; return; }
        if (!be.hasCatalysts(recipe)) { be.state = "catalyst"; return; }
        // A totem of the voice that stands here but has run its buffer dry is silent, not missing: say which.
        if (!totems.has(recipe.attunement())) { be.state = totems.silent(recipe.attunement()) ? "silent" : "attunement"; return; }
        var keeping = Keeping.voice(totems, recipe.attunement());
        if (keeping == Keeping.State.QUIET && be.work == 0) { be.state = "quiet"; return; }
        int seconds = be.shownSeconds;
        if (be.work < 0) be.work = 0;
        int cost = be.shownPulse;
        // Pulse comes only through the lattice: a station with no conductor in reach says so rather than "no Pulse".
        if (!LatticeNetwork.tryExtractPulseNearby(level, pos, 8, cost)) {
            be.state = tk.darrow.tribalpower.lattice.Weave.onLattice(level, pos) ? "pulse" : "lattice";
            return;
        }
        be.state = "working";
        be.work += tk.darrow.tribalpower.effect.EffectHooks.clockNear((ServerLevel) level, pos) ? 2 : 1;
        Keeping.feedWork(totems, recipe.attunement());
        SpiritEffects.ring((ServerLevel)level, pos.getCenter().add(0, 0.55, 0), recipe.attunement(), 0.45, 8);
        if (be.work >= seconds) {
            ItemStack input = be.items.get(0);
            input.shrink(1);
            if (input.isEmpty()) be.items.set(0, ItemStack.EMPTY);
            be.takeCatalysts(recipe);
            be.placeOutput(result, false);
            if (!freedCell.isEmpty()) be.placeOutput(freedCell, false);
            be.work = 0;
        }
        be.setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Work", work); tag.putString("Recipe", recipeId);
        sides.save(tag);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        work = Math.max(0, tag.getInt("Work")); recipeId = tag.getString("Recipe");
        sides.load(tag);
        array.invalidate();
    }

    @Override public java.util.List<net.minecraft.network.chat.Component> diagnose(ServerLevel server, BlockPos pos) {
        java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
        var recipe = ProcessingRecipes.find(server, station(), items.get(0));
        lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.station.state", status()));
        if (recipe == null) lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.station.no_recipe"));
        else {
            lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.station.recipe", recipe.result().getHoverName(), work, workSeconds(recipe), pulsePerSecond(recipe),
                    net.minecraft.network.chat.Component.translatable("attunement.tribalpower." + recipe.attunement().getSerializedName())));
            if (!LatticeNetwork.hasAttunement(server, pos, 8, recipe.attunement()))
                lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.station.missing_attunement",
                        net.minecraft.network.chat.Component.translatable("attunement.tribalpower." + recipe.attunement().getSerializedName())).withStyle(net.minecraft.ChatFormatting.YELLOW));
            else {
                var keeping = Keeping.voice(server, pos, recipe.attunement());
                if (keeping != Keeping.State.ANSWERED)
                    lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.station.keeping." + keeping.name().toLowerCase(java.util.Locale.ROOT))
                            .withStyle(net.minecraft.ChatFormatting.YELLOW));
            }
            for (ItemStack want : recipe.catalysts()) lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.station.catalyst", want.getCount(), want.getHoverName())
                    .withStyle(hasCatalysts(recipe) ? net.minecraft.ChatFormatting.GRAY : net.minecraft.ChatFormatting.YELLOW));
            if (!placeOutput(recipe.result(), true)) lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.station.output_full").withStyle(net.minecraft.ChatFormatting.YELLOW));
            lines.add(net.minecraft.network.chat.Component.translatable(arrayed(server, recipe.attunement())
                    ? "diag.tribalpower.station.arrayed" : "diag.tribalpower.station.no_array"));
        }
        return lines;
    }

    /** True when the catalyst slots hold everything this work consumes. */
    public boolean hasCatalysts(ProcessingRecipes.Formula recipe) {
        for (ItemStack want : recipe.catalysts()) {
            int held = 0;
            for (int slot : CATALYST_SLOTS)
                if (ItemStack.isSameItem(items.get(slot), want)) held += items.get(slot).getCount();
            if (held < want.getCount()) return false;
        }
        return true;
    }

    private void takeCatalysts(ProcessingRecipes.Formula recipe) {
        for (ItemStack want : recipe.catalysts()) {
            int remaining = want.getCount();
            for (int slot : CATALYST_SLOTS) {
                if (remaining <= 0) break;
                ItemStack held = items.get(slot);
                if (!ItemStack.isSameItem(held, want)) continue;
                int taken = Math.min(remaining, held.getCount());
                held.shrink(taken);
                if (held.isEmpty()) items.set(slot, ItemStack.EMPTY);
                remaining -= taken;
            }
        }
    }

    /** Rank, dim stretch and the live totem — the same seconds the tick waits. */
    private int workSeconds(ProcessingRecipes.Formula recipe) {
        return workSeconds(recipe, level == null ? null
                : LatticeNetwork.TotemsNear.of(level, worldPosition, LatticeNetwork.DEFAULT_RADIUS));
    }

    private int workSeconds(ProcessingRecipes.Formula recipe, LatticeNetwork.TotemsNear totems) {
        int seconds = tk.darrow.tribalpower.item.MachineRank.scaleTime(this, recipe.seconds());
        if (totems == null) return seconds;
        return Keeping.stretch(Keeping.voice(totems, recipe.attunement()), seconds);
    }

    @Override
    public int spendPerSecond() {
        // "pulse" is the starved beat: it still wants this much, which is the deficit the lens is for.
        if (!"working".equals(state) && !"pulse".equals(state) && !"lattice".equals(state)) return 0;
        return Math.max(0, shownPulse);
    }

    /** Array discount, rank and pack consumption — the same Pulse the tick draws. */
    public int pulsePerSecond(ProcessingRecipes.Formula recipe) {
        return pulsePerSecond(recipe, level != null && arrayed(level, recipe.attunement()));
    }

    /** As {@link #pulsePerSecond(ProcessingRecipes.Formula)}, with the array check the beat already made. */
    private int pulsePerSecond(ProcessingRecipes.Formula recipe, boolean discounted) {
        int cost = discounted ? Math.max(1, (int) Math.round(recipe.pulse() * ARRAY_DISCOUNT)) : recipe.pulse();
        cost = tk.darrow.tribalpower.item.MachineRank.scalePulse(this, cost);
        return tk.darrow.tribalpower.config.TribalConfig.scaleConsumption(cost);
    }
}
