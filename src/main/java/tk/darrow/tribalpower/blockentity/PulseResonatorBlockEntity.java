package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.api.pulse.PulseHandler;
import tk.darrow.tribalpower.api.pulse.PulseStorage;
import tk.darrow.tribalpower.block.PulseResonatorBlock;

/**
 * Fueled Spirit Pulse generator — burns coal/charcoal as a Ley Collector alternative.
 */
public class PulseResonatorBlockEntity extends BlockEntity implements PulseHandler, Container {
    public static final int CAPACITY = 2500;
    public static final int GAIN_INTERVAL = 20;
    public static final int GAIN_AMOUNT = 4;
    public static final int COAL_BURN_TICKS = 1600;
    public static final int SLOT = 0;

    private final PulseStorage pulse = new PulseStorage(CAPACITY);
    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private int burnTime;
    private int burnDuration;
    private int tickCounter;

    public PulseResonatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PULSE_RESONATOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PulseResonatorBlockEntity be) {
        boolean wasLit = be.isBurning();

        if (be.burnTime > 0) {
            be.burnTime--;
            be.tickCounter++;
            if (be.tickCounter >= GAIN_INTERVAL) {
                be.tickCounter = 0;
                if (be.insertPulse(GAIN_AMOUNT, false) > 0) {
                    be.setChanged();
                }
            }
        } else {
            be.tickCounter = 0;
        }

        if (be.burnTime <= 0 && be.canStartBurn()) {
            be.consumeFuel();
        }

        boolean lit = be.isBurning();
        if (wasLit != lit) {
            level.setBlock(pos, state.setValue(PulseResonatorBlock.LIT, lit), 3);
            be.setChanged();
        } else if (lit) {
            be.setChanged();
        }
    }

    public boolean isBurning() {
        return burnTime > 0;
    }

    public int getBurnTime() {
        return burnTime;
    }

    public int getBurnDuration() {
        return burnDuration;
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
    }

    public static int burnTicksFor(ItemStack stack) {
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
            return COAL_BURN_TICKS;
        }
        return 0;
    }

    private boolean canStartBurn() {
        if (!isFuel(items.get(SLOT))) {
            return false;
        }
        return pulse.getPulseStored() < pulse.getPulseCapacity();
    }

    private void consumeFuel() {
        ItemStack fuel = items.get(SLOT);
        int ticks = burnTicksFor(fuel);
        if (ticks <= 0) {
            return;
        }
        fuel.shrink(1);
        if (fuel.isEmpty()) {
            items.set(SLOT, ItemStack.EMPTY);
        }
        burnTime = ticks;
        burnDuration = ticks;
        setChanged();
    }

    /** Insert one fuel item from the held stack; returns true if accepted. */
    public boolean acceptFuel(ItemStack stack) {
        if (!isFuel(stack)) {
            return false;
        }
        ItemStack slot = items.get(SLOT);
        if (slot.isEmpty()) {
            items.set(SLOT, stack.split(1));
            setChanged();
            return true;
        }
        if (ItemStack.isSameItemSameComponents(slot, stack) && slot.getCount() < slot.getMaxStackSize()) {
            slot.grow(1);
            stack.shrink(1);
            setChanged();
            return true;
        }
        return false;
    }

    public ItemStack takeFuel() {
        ItemStack taken = items.get(SLOT);
        items.set(SLOT, ItemStack.EMPTY);
        if (!taken.isEmpty()) {
            setChanged();
        }
        return taken;
    }

    public int fuelCount() {
        return items.get(SLOT).getCount();
    }

    @Override
    public int getPulseStored() {
        return pulse.getPulseStored();
    }

    @Override
    public int getPulseCapacity() {
        return pulse.getPulseCapacity();
    }

    @Override
    public int insertPulse(int amount, boolean simulate) {
        int n = pulse.insertPulse(amount, simulate);
        if (!simulate && n > 0) {
            setChanged();
        }
        return n;
    }

    @Override
    public int extractPulse(int amount, boolean simulate) {
        int n = pulse.extractPulse(amount, simulate);
        if (!simulate && n > 0) {
            setChanged();
        }
        return n;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return items.get(SLOT).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isFuel(stack);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        pulse.save(tag);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnDuration", burnDuration);
        tag.putInt("TickCounter", tickCounter);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pulse.load(tag);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        burnTime = tag.getInt("BurnTime");
        burnDuration = tag.getInt("BurnDuration");
        tickCounter = tag.getInt("TickCounter");
    }
}
