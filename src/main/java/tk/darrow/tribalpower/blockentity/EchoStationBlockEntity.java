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

/** Slot 0 input, slots 1-8 output. One work beat per second; no per-tick lattice volume scans. */
public class EchoStationBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, tk.darrow.tribalpower.api.Diagnosable, tk.darrow.tribalpower.lattice.HasSideIo {
    private NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
    private int work;
    private String recipeId = "";
    private String state = "idle";
    private boolean arrayed;
    private final tk.darrow.tribalpower.pattern.PatternState array =
            new tk.darrow.tribalpower.pattern.PatternState(tk.darrow.tribalpower.pattern.ModPatterns.SHATTER_ARRAY);
    private final tk.darrow.tribalpower.lattice.SideIo sides = tk.darrow.tribalpower.lattice.SideIo.station();
    private static final int[] INPUT_SLOTS = {0};
    private static final int[] OUTPUT_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8};

    @Override public tk.darrow.tribalpower.lattice.SideIo sideIo() { return sides; }
    @Override public int[] inputSlots(Direction face) { return INPUT_SLOTS; }
    @Override public int[] outputSlots(Direction face) { return OUTPUT_SLOTS; }

    /** The Shatter Array pays a quarter of the Pulse back and empties the station into the cache. */
    public static final double ARRAY_DISCOUNT = 0.75;
    /** Where the array seats its totems, as offsets from the station. Rotation-invariant as a set. */
    private static final int[][] CORNERS = {{2, 2}, {2, -2}, {-2, 2}, {-2, -2}};
    public EchoStationBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.ECHO_STATION.get(), pos, state); }
    public String station() { return BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath(); }
    public int work() { return work; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> value) { items = value; }
    @Override public int getContainerSize() { return 9; }
    @Override protected Component getDefaultName() { return Component.translatable("block.tribalpower." + station()); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new tk.darrow.tribalpower.echo.StationMenu(id, inv, this, new ContainerData() {
            public int get(int index) {
                var recipe = ProcessingRecipes.find(level, station(), items.get(0));
                return switch(index) {
                    case 0 -> work;
                    case 1 -> recipe == null ? 1 : EchoStationBlockEntity.this.workSeconds(recipe);
                    case 3 -> sides.pack();
                    case 4 -> worldPosition.getX();
                    case 5 -> worldPosition.getY();
                    case 6 -> worldPosition.getZ();
                    default -> Math.max(0, java.util.List.of("idle", "working", "paused", "full", "attunement", "pulse", "quiet").indexOf(state));
                };
            }
            public void set(int index, int value) {}
            public int getCount() { return 7; }
        });
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && ProcessingRecipes.find(level, station(), stack) != null; }
    @Override public int[] getSlotsForFace(Direction face) { return tk.darrow.tribalpower.lattice.SideIo.slots(this, face); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) {
        return !level.hasNeighborSignal(worldPosition) && sides.get(face).insert() && canPlaceItem(slot, stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        return !level.hasNeighborSignal(worldPosition) && sides.get(face).extract() && slot > 0;
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

    public boolean isArrayed() { return arrayed; }

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
                    drained = true;
                }
            }
        }
        if (drained) { cache.setChanged(); setChanged(); }
    }

    private boolean placeOutput(ItemStack result, boolean simulate) {
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
        if (recipe == null) { be.work = 0; be.recipeId = ""; be.state = "idle"; be.setChanged(); return; }
        if (!recipe.id().toString().equals(be.recipeId)) { be.work = 0; be.recipeId = recipe.id().toString(); }
        if (level.hasNeighborSignal(pos)) { be.state = "paused"; return; }
        ItemStack result = recipe.result();
        be.arrayed = be.arrayed(level, recipe.attunement());
        if (be.arrayed) be.drainToCache(level);
        if (!be.placeOutput(result, true)) { be.state = "full"; return; }
        if (!LatticeNetwork.hasAttunement(level, pos, 8, recipe.attunement())) { be.state = "attunement"; return; }
        var keeping = Keeping.voice(level, pos, recipe.attunement());
        if (keeping == Keeping.State.QUIET && be.work == 0) { be.state = "quiet"; return; }
        int seconds = be.workSeconds(recipe);
        if (be.work < 0) be.work = 0;
        int cost = be.pulsePerSecond(recipe);
        if (LatticeNetwork.extractPulseNearby(level, pos, 8, cost, true) < cost) { be.state = "pulse"; return; }
        LatticeNetwork.extractPulseNearby(level, pos, 8, cost, false);
        be.state = "working";
        be.work++;
        Keeping.feedWork(level, pos, recipe.attunement());
        SpiritEffects.ring((ServerLevel)level, pos.getCenter().add(0, 0.55, 0), recipe.attunement(), 0.45, 8);
        if (be.work >= seconds) {
            be.items.get(0).shrink(1);
            be.placeOutput(result, false);
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
        items = NonNullList.withSize(9, ItemStack.EMPTY);
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
            if (!placeOutput(recipe.result(), true)) lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.station.output_full").withStyle(net.minecraft.ChatFormatting.YELLOW));
            lines.add(net.minecraft.network.chat.Component.translatable(arrayed(server, recipe.attunement())
                    ? "diag.tribalpower.station.arrayed" : "diag.tribalpower.station.no_array"));
        }
        return lines;
    }

    /** Rank, dim stretch and the live totem — the same seconds the tick waits. */
    private int workSeconds(ProcessingRecipes.Formula recipe) {
        int seconds = tk.darrow.tribalpower.item.MachineRank.scaleTime(this, recipe.seconds());
        if (level == null) return seconds;
        return Keeping.stretch(Keeping.voice(level, worldPosition, recipe.attunement()), seconds);
    }

    /** Array discount, rank and pack consumption — the same Pulse the tick draws. */
    private int pulsePerSecond(ProcessingRecipes.Formula recipe) {
        boolean discounted = level != null && arrayed(level, recipe.attunement());
        int cost = discounted ? Math.max(1, (int) Math.round(recipe.pulse() * ARRAY_DISCOUNT)) : recipe.pulse();
        cost = tk.darrow.tribalpower.item.MachineRank.scalePulse(this, cost);
        return tk.darrow.tribalpower.config.TribalConfig.scaleConsumption(cost);
    }
}
