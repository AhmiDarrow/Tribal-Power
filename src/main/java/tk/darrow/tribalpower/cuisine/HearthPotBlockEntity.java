package tk.darrow.tribalpower.cuisine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.healing.SpiritKettleBlockEntity;

/**
 * The Hearth Pot: four ingredients and a bowl over the fire, and a meal comes out. It cooks by heat alone -- a lit
 * campfire, fire, magma or an Ember Bowl beneath it -- and asks no Pulse, so it is a camp's first machine.
 */
public class HearthPotBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider, tk.darrow.tribalpower.api.Diagnosable {
    public static final int CONTAINER = 4, OUTPUT = 5, SIZE = 6;
    public static final int IDLE = 0, COOKING = 1, NO_HEAT = 2, BLOCKED = 3;
    private static final int[] INPUTS = {0, 1, 2, 3, CONTAINER}, OUTPUTS = {OUTPUT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int progress, total = 1, state;

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) { case 0 -> progress; case 1 -> total; default -> state; };
        }
        @Override public void set(int index, int value) {}
        @Override public int getCount() { return 3; }
    };

    public HearthPotBlockEntity(BlockPos pos, BlockState state) {
        super(CuisineRegistry.HEARTH_POT_ENTITY.get(), pos, state);
    }

    private HearthRecipe.Input input() {
        List<ItemStack> seated = new ArrayList<>();
        for (int i = 0; i < CONTAINER; i++) seated.add(items.get(i));
        return new HearthRecipe.Input(seated, items.get(CONTAINER));
    }

    public Optional<RecipeHolder<HearthRecipe>> recipe(Level level) {
        return level.getRecipeManager().getRecipeFor(CuisineRegistry.HEARTH_TYPE.get(), input(), level);
    }

    public static void tick(Level level, BlockPos pos, BlockState blockState, HearthPotBlockEntity pot) {
        if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
        int was = pot.state;
        pot.state = pot.step((ServerLevel) level);
        boolean cooking = pot.state == COOKING;
        if (blockState.getValue(HearthPotBlock.COOKING) != cooking) level.setBlock(pos, blockState.setValue(HearthPotBlock.COOKING, cooking), 3);
        if (was != pot.state) pot.setChanged();
    }

    private int step(ServerLevel level) {
        var found = recipe(level);
        if (found.isEmpty()) {
            progress = 0;
            return IDLE;
        }
        HearthRecipe recipe = found.get().value();
        total = Math.max(1, (int) Math.round(recipe.seconds() * TribalConfig.hearthCookScale()));
        if (TribalConfig.hearthNeedsHeat() && !SpiritKettleBlockEntity.heated(level, worldPosition)) return NO_HEAT;
        ItemStack made = recipe.assemble(input(), level.registryAccess());
        ItemStack output = items.get(OUTPUT);
        if (!output.isEmpty() && (!ItemStack.isSameItemSameComponents(output, made) || output.getCount() + made.getCount() > output.getMaxStackSize()))
            return BLOCKED;
        if (++progress < total) {
            setChanged();
            return COOKING;
        }
        for (int i = 0; i <= CONTAINER; i++) {
            ItemStack seat = items.get(i);
            if (i == CONTAINER && recipe.container().isEmpty()) continue;
            if (seat.isEmpty()) continue;
            ItemStack remainder = seat.getCraftingRemainingItem();
            seat.shrink(1);
            if (seat.isEmpty()) items.set(i, remainder);
            else if (!remainder.isEmpty()) net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, remainder);
        }
        if (output.isEmpty()) items.set(OUTPUT, made);
        else output.grow(made.getCount());
        progress = 0;
        setChanged();
        level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.GENERIC_EAT, net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 0.9F);
        return COOKING;
    }

    /** Cooks a whole meal at once, for tests: true when it made something. */
    public boolean cookNow(ServerLevel level) {
        var found = recipe(level);
        if (found.isEmpty()) return false;
        progress = (int) Math.round(found.get().value().seconds() * TribalConfig.hearthCookScale()) - 1;
        ItemStack before = items.get(OUTPUT).copy();
        state = step(level);
        return !ItemStack.matches(before, items.get(OUTPUT));
    }

    public int state() { return state; }

    @Override public int[] getSlotsForFace(Direction side) { return side == Direction.DOWN ? OUTPUTS : INPUTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == OUTPUT; }
    @Override public int getContainerSize() { return SIZE; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot < OUTPUT; }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { items.clear(); }
    public NonNullList<ItemStack> items() { return items; }
    @Override public Component getDisplayName() { return Component.translatable("block.tribalpower.hearth_pot"); }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new HearthPotMenu(id, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        progress = tag.getInt("Progress");
    }

    @Override
    public List<Component> diagnose(ServerLevel server, BlockPos pos) {
        return List.of(Component.translatable("gui.tribalpower.hearth_pot.state." + state),
                Component.translatable(SpiritKettleBlockEntity.heated(server, pos) ? "gui.tribalpower.kettle.heat.on" : "gui.tribalpower.kettle.heat.off"));
    }
}
