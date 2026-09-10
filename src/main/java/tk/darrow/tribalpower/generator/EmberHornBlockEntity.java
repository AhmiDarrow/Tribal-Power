package tk.darrow.tribalpower.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * The Drumhearts' craft (design 3.1 section 9.1): fire fed by hand or by hopper.
 *
 * <p>Burn time divided by twenty is Pulse, delivered at up to {@link #MAX_RATE} a second. Coal is 80,
 * a coal block 800, a lava bucket 1,000, a blaze rod 120. The rate ceiling is the real cap: scaling means
 * more horns, and horns are a tribe craft.
 *
 * <p>This is the generator most likely to be pointed at a loop, so the arithmetic is stated plainly. The
 * pit's common band spends 140 Pulse a cycle and returns one coal ore block; shattering it costs 40 more
 * and yields 2 coal worth 160, against 180 spent. Fortune closes that to break-even and cannot be
 * automated, because the mod has no fake players. No automatable loop is net positive.
 */
public class EmberHornBlockEntity extends GeneratorBlockEntity implements WorldlyContainer {
    public static final int CAPACITY = 1000;
    public static final int MAX_RATE = 20;
    public static final int SLOT = 0;
    private static final int[] SLOTS = {SLOT};

    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    /** Pulse still owed by the fuel already burning. */
    private int burning;

    public EmberHornBlockEntity(BlockPos pos, BlockState state) {
        super(GeneratorRegistry.EMBER_HORN_TYPE.get(), pos, state, CAPACITY);
    }

    @Override public tk.darrow.tribalpower.api.pulse.Attunement voice() { return tk.darrow.tribalpower.api.pulse.Attunement.FIRE; }

    public boolean lit() { return burning > 0; }
    public int burning() { return burning; }

    /** Pulse a stack of fuel is worth: burn time over twenty. */
    public static int pulseOf(ItemStack stack) {
        int burnTime = stack.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING);
        return burnTime <= 0 ? 0 : burnTime / 20;
    }

    @Override
    protected int rawOutput(Level level, BlockPos pos) {
        if (burning <= 0 && pulseOf(items.get(SLOT)) <= 0) return 0;
        return Math.min(MAX_RATE, burning > 0 ? burning : pulseOf(items.get(SLOT)));
    }

    @Override
    protected void afterProduce(Level level, BlockPos pos, int produced) {
        // Charge the fire for exactly what left it, so a full buffer does not eat fuel.
        if (produced <= 0) return;
        if (burning <= 0) consumeFuel(level, pos);
        burning = Math.max(0, burning - produced);
        if (burning <= 0) updateLit(level, pos, false);
    }

    private void consumeFuel(Level level, BlockPos pos) {
        ItemStack fuel = items.get(SLOT);
        int worth = pulseOf(fuel);
        if (worth <= 0) return;
        burning = worth;
        ItemStack remainder = fuel.getCraftingRemainingItem();
        fuel.shrink(1);
        if (!remainder.isEmpty() && fuel.isEmpty()) items.set(SLOT, remainder);
        updateLit(level, pos, true);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.FIRECHARGE_USE,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.4F, 1.4F);
    }

    private void updateLit(Level level, BlockPos pos, boolean lit) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(GeneratorBlock.LIT) && state.getValue(GeneratorBlock.LIT) != lit)
            level.setBlock(pos, state.setValue(GeneratorBlock.LIT, lit), 3);
    }

    @Override
    public List<Component> breakdown() {
        List<Component> lines = new ArrayList<>();
        ItemStack fuel = items.get(SLOT);
        lines.add(fuel.isEmpty()
                ? Component.translatable("ember.tribalpower.no_fuel")
                : Component.translatable("ember.tribalpower.fuel", fuel.getHoverName(), pulseOf(fuel)));
        lines.add(Component.translatable("ember.tribalpower.burning", burning));
        lines.add(Component.translatable("ember.tribalpower.total", currentOutput(), MAX_RATE));
        return lines;
    }

    // ---- fuel inventory --------------------------------------------------------------------

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return items.get(SLOT).isEmpty(); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int count) { setChanged(); return ContainerHelper.removeItem(items, slot, count); }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); setChanged(); }
    @Override public void clearContent() { items.clear(); setChanged(); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return pulseOf(stack) > 0; }
    @Override public int[] getSlotsForFace(Direction face) { return SLOTS; }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) {
        return !stilled() && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        // Only a spent bucket comes back out; nobody siphons the fuel out of a lit fire.
        return !stilled() && pulseOf(stack) <= 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return !stilled() && level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getCenter()) <= 64.0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Burning", burning);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        burning = Math.max(0, tag.getInt("Burning"));
    }
}
