package tk.darrow.tribalpower.device;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.item.MachineRank;
import tk.darrow.tribalpower.lattice.LatticeNetwork;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 3x3 grid, one Recipe Seal, nine output slots. Sneak-use imprints a matching recipe onto a blank seal. */
public class SealLoomBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, tk.darrow.tribalpower.lattice.HasSideIo, tk.darrow.tribalpower.api.pulse.PulseSpend {
    public static final int GRID = 9;
    public static final int SEAL = 9;
    public static final int OUTPUT = 10;
    public static final int SIZE = 19;
    public static final int COST = 6;

    @Override
    public int spendPerSecond() {
        return "need_pulse".equals(reason) || "wove".equals(reason) ? COST : 0;
    }
    private static final int[] INPUT_SLOTS = java.util.stream.IntStream.range(0, GRID).toArray();
    private static final int[] OUTPUT_SLOTS = java.util.stream.IntStream.range(OUTPUT, SIZE).toArray();
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private String reason = "seat";
    private int reasonN;
    private String reasonName = "";
    private final tk.darrow.tribalpower.lattice.SideIo sides = tk.darrow.tribalpower.lattice.SideIo.station();
    @Override public tk.darrow.tribalpower.lattice.SideIo sideIo() { return sides; }
    @Override public int[] inputSlots(Direction face) { return INPUT_SLOTS; }
    @Override public int[] outputSlots(Direction face) { return OUTPUT_SLOTS; }

    public SealLoomBlockEntity(BlockPos pos, BlockState state) {
        super(DeviceRegistry.SEAL_LOOM_TYPE.get(), pos, state);
    }

    public Component status() {
        return switch (reason) {
            case "imprint" -> Component.translatable("message.tribalpower.seal_loom.imprint", reasonName);
            case "need_pulse" -> Component.translatable("message.tribalpower.seal_loom.need_pulse", reasonN);
            case "wove" -> Component.translatable("message.tribalpower.seal_loom.wove", reasonName);
            case "seat", "paused", "need_blank", "arrange", "mismatch", "output_full"
                    -> Component.translatable("message.tribalpower.seal_loom." + reason);
            default -> Component.literal(reason);
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SealLoomBlockEntity be) {
        tk.darrow.tribalpower.lattice.SideIoAdjacency.beat(level, be);
        if (level.isClientSide || !MachineRank.due(level, pos, be)) return;
        if (!(level instanceof ServerLevel server)) return;
        be.beat(server);
    }

    public void beat(ServerLevel server) {
        if (server.hasNeighborSignal(worldPosition)) { setReason("paused"); return; }
        craft(server);
    }

    public void imprint(Player player) {
        if (level == null || level.isClientSide) return;
        ItemStack seal = items.get(SEAL);
        if (!(seal.getItem() instanceof RecipeSealItem) || !RecipeSealItem.isBlank(seal)) {
            setReason("need_blank");
            player.displayClientMessage(status(), true);
            return;
        }
        Optional<RecipeHolder<CraftingRecipe>> found = match((ServerLevel) level);
        if (found.isEmpty()) {
            setReason("arrange");
            player.displayClientMessage(status(), true);
            return;
        }
        RecipeSealItem.imprint(seal, found.get().id());
        setReason("imprint", found.get().id().toString());
        player.displayClientMessage(status(), true);
        setChanged();
    }

    private void craft(ServerLevel server) {
        ResourceLocation id = RecipeSealItem.recipeId(items.get(SEAL));
        if (id == null) { setReason("seat"); return; }
        Optional<RecipeHolder<CraftingRecipe>> found = match(server);
        if (found.isEmpty() || !found.get().id().equals(id)) { setReason("mismatch"); return; }
        int cost = COST;
        if (LatticeNetwork.extractPulseNearby(server, worldPosition, 8, cost, true) < cost) {
            setReason("need_pulse", cost);
            return;
        }
        CraftingInput.Positioned positioned = CraftingInput.ofPositioned(3, 3, List.copyOf(items.subList(0, GRID)));
        CraftingInput input = positioned.input();
        ItemStack result = found.get().value().assemble(input, server.registryAccess());
        if (result.isEmpty()) { setReason("output_full"); return; }
        NonNullList<ItemStack> preview = snapshot();
        if (!storeInto(preview, result.copy())) { setReason("output_full"); return; }
        List<ItemStack> remain = found.get().value().getRemainingItems(input);
        List<ItemStack> overflow = new ArrayList<>();
        for (int i = 0; i < GRID; i++) {
            ItemStack slot = preview.get(i);
            if (!slot.isEmpty()) {
                slot.shrink(1);
                if (slot.isEmpty()) preview.set(i, ItemStack.EMPTY);
            }
            int x = i % 3 - positioned.left(), y = i / 3 - positioned.top();
            int remainderIndex = y * input.width() + x;
            if (x >= 0 && x < input.width() && y >= 0 && y < input.height()
                    && remainderIndex < remain.size() && !remain.get(remainderIndex).isEmpty()) {
                ItemStack leftover = remain.get(remainderIndex).copy();
                if (preview.get(i).isEmpty()) preview.set(i, leftover);
                else if (!storeInto(preview, leftover)) overflow.add(leftover);
            }
        }
        LatticeNetwork.extractPulseNearby(server, worldPosition, 8, cost, false);
        items = preview;
        for (ItemStack extra : overflow) Block.popResource(server, worldPosition, extra);
        setReason("wove", result.getHoverName().getString());
        setChanged();
    }

    private Optional<RecipeHolder<CraftingRecipe>> match(ServerLevel server) {
        return server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid(), server);
    }

    private CraftingInput grid() {
        return CraftingInput.of(3, 3, List.copyOf(items.subList(0, GRID)));
    }

    private NonNullList<ItemStack> snapshot() {
        NonNullList<ItemStack> copy = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        for (int i = 0; i < SIZE; i++) copy.set(i, items.get(i).copy());
        return copy;
    }

    private static boolean storeInto(NonNullList<ItemStack> dest, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = OUTPUT; i < SIZE && !remaining.isEmpty(); i++) {
            ItemStack in = dest.get(i);
            if (in.isEmpty()) { dest.set(i, remaining); return true; }
            if (ItemStack.isSameItemSameComponents(in, remaining)) {
                int n = Math.min(remaining.getCount(), in.getMaxStackSize() - in.getCount());
                in.grow(n); remaining.shrink(n);
            }
        }
        return remaining.isEmpty();
    }

    private void setReason(String key) { reason = key; reasonN = 0; reasonName = ""; }
    private void setReason(String key, int n) { reason = key; reasonN = n; reasonName = ""; }
    private void setReason(String key, String name) { reason = key; reasonN = 0; reasonName = name; }

    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> value) { items = value; }
    @Override public int getContainerSize() { return SIZE; }
    @Override protected Component getDefaultName() { return Component.translatable("block.tribalpower.seal_loom"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new SealLoomMenu(id, inventory, this); }
    @Override public int[] getSlotsForFace(Direction side) {
        return tk.darrow.tribalpower.lattice.SideIo.slots(this, side);
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SEAL) return stack.getItem() instanceof RecipeSealItem;
        if (slot >= OUTPUT) return false;
        return true;
    }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot < GRID && !level.hasNeighborSignal(worldPosition) && sides.get(side).insert();
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= OUTPUT && !level.hasNeighborSignal(worldPosition) && sides.get(side).extract();
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putString("Reason", reason);
        tag.putInt("ReasonN", reasonN);
        tag.putString("ReasonName", reasonName);
        sides.save(tag);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        reason = tag.getString("Reason");
        if (reason.isEmpty()) reason = "seat";
        reasonN = tag.getInt("ReasonN");
        reasonName = tag.getString("ReasonName");
        sides.load(tag);
    }
}
