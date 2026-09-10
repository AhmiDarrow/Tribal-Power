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
import tk.darrow.tribalpower.lattice.LatticeNetwork;
import tk.darrow.tribalpower.effect.SpiritEffects;

/** Slot 0 input, slots 1-8 output. One work beat per second; no per-tick lattice volume scans. */
public class EchoStationBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, tk.darrow.tribalpower.api.Diagnosable {
    private NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
    private int work;
    private String recipeId = "";
    private String state = "idle";
    private boolean arrayed;
    private final tk.darrow.tribalpower.pattern.PatternState array =
            new tk.darrow.tribalpower.pattern.PatternState(tk.darrow.tribalpower.pattern.ModPatterns.SHATTER_ARRAY);

    /** The Shatter Array pays a quarter of the Pulse back and empties the station into the cache. */
    public static final double ARRAY_DISCOUNT = 0.75;
    /** Where the array seats its totems, as offsets from the station. Rotation-invariant as a set. */
    private static final int[][] CORNERS = {{2, 2}, {2, -2}, {-2, 2}, {-2, -2}};
    public EchoStationBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.ECHO_STATION.get(), pos, state); }
    public String station() { return BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath(); }
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
                    case 1 -> recipe == null ? 1 : recipe.seconds();
                    default -> Math.max(0, java.util.List.of("idle", "working", "paused", "full", "attunement", "pulse").indexOf(state));
                };
            }
            public void set(int index, int value) {}
            public int getCount() { return 3; }
        });
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && ProcessingRecipes.find(level, station(), stack) != null; }
    @Override public int[] getSlotsForFace(Direction face) { return face == Direction.DOWN ? new int[]{1,2,3,4,5,6,7,8} : new int[]{0}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) { return !level.hasNeighborSignal(worldPosition) && canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) { return !level.hasNeighborSignal(worldPosition) && slot > 0; }
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

    /** Empties the output slots into the cache beneath. Only an arrayed station does this. */
    private void drainToCache(Level level) {
        AncestralCacheBlockEntity cache = cacheBelow(level);
        if (cache == null) return;
        for (int slot = 1; slot < 9; slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) continue;
            for (int target = 0; target < cache.getContainerSize() && !stack.isEmpty(); target++) {
                ItemStack held = cache.getItem(target);
                if (held.isEmpty()) { cache.setItem(target, stack.copy()); items.set(slot, ItemStack.EMPTY); stack = ItemStack.EMPTY; }
                else if (ItemStack.isSameItemSameComponents(held, stack)) {
                    int moved = Math.min(stack.getCount(), held.getMaxStackSize() - held.getCount());
                    if (moved <= 0) continue;
                    held.grow(moved);
                    stack.shrink(moved);
                }
            }
        }
        cache.setChanged();
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
        if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
        var recipe = ProcessingRecipes.find(level, be.station(), be.items.get(0));
        if (recipe == null) { be.work = 0; be.recipeId = ""; be.state = "idle"; be.setChanged(); return; }
        if (!recipe.id().toString().equals(be.recipeId)) { be.work = 0; be.recipeId = recipe.id().toString(); }
        if (be.work < 0 || be.work >= recipe.seconds()) be.work = 0;
        if (level.hasNeighborSignal(pos)) { be.state = "paused"; return; }
        ItemStack result = recipe.result();
        be.arrayed = be.arrayed(level, recipe.attunement());
        if (be.arrayed) be.drainToCache(level);
        if (!be.placeOutput(result, true)) { be.state = "full"; return; }
        if (!LatticeNetwork.hasAttunement(level, pos, 8, recipe.attunement())) { be.state = "attunement"; return; }
        int cost = be.arrayed ? Math.max(1, (int) Math.round(recipe.pulse() * ARRAY_DISCOUNT)) : recipe.pulse();
        if (LatticeNetwork.extractPulseNearby(level, pos, 8, cost, true) < cost) { be.state = "pulse"; return; }
        LatticeNetwork.extractPulseNearby(level, pos, 8, cost, false);
        be.state = "working";
        be.work++;
        SpiritEffects.ring((ServerLevel)level, pos.getCenter().add(0, 0.55, 0), recipe.attunement(), 0.45, 8);
        if (be.work >= recipe.seconds()) {
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
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(9, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        work = Math.max(0, tag.getInt("Work")); recipeId = tag.getString("Recipe");
        array.invalidate();
    }

    @Override public java.util.List<net.minecraft.network.chat.Component> diagnose(ServerLevel server, BlockPos pos) {
        java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
        var recipe = ProcessingRecipes.find(server, station(), items.get(0));
        lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.station.state", status()));
        if (recipe == null) lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.station.no_recipe"));
        else {
            lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.station.recipe", recipe.result().getHoverName(), work, recipe.seconds(), recipe.pulse(),
                    net.minecraft.network.chat.Component.translatable("attunement.tribalpower." + recipe.attunement().getSerializedName())));
            if (!LatticeNetwork.hasAttunement(server, pos, 8, recipe.attunement()))
                lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.station.missing_attunement",
                        net.minecraft.network.chat.Component.translatable("attunement.tribalpower." + recipe.attunement().getSerializedName())).withStyle(net.minecraft.ChatFormatting.YELLOW));
            if (!placeOutput(recipe.result(), true)) lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.station.output_full").withStyle(net.minecraft.ChatFormatting.YELLOW));
            lines.add(net.minecraft.network.chat.Component.translatable(arrayed(server, recipe.attunement())
                    ? "diag.tribalpower.station.arrayed" : "diag.tribalpower.station.no_array"));
        }
        return lines;
    }
}
