package tk.darrow.tribalpower.healing;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.song.Reagents;

/** Reagent, herb and base on the left, the brew on the right. */
public class KettleMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public KettleMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(SpiritKettleBlockEntity.SIZE), new SimpleContainerData(4));
    }

    public KettleMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(HealingRegistry.KETTLE_MENU.get(), id);
        this.container = container;
        this.data = data;
        checkContainerDataCount(data, 4);
        addSlot(new Slot(container, SpiritKettleBlockEntity.REAGENT, 44, 17) {
            @Override public boolean mayPlace(ItemStack stack) { return Reagents.of(stack.getItem()) != null; }
        });
        addSlot(new Slot(container, SpiritKettleBlockEntity.HERB, 44, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(SpiritKettleBlockEntity.HERBS); }
        });
        addSlot(new Slot(container, SpiritKettleBlockEntity.BASE, 44, 53) {
            @Override public boolean mayPlace(ItemStack stack) { return SpiritKettleBlockEntity.form(stack) != null; }
        });
        addSlot(new Slot(container, SpiritKettleBlockEntity.OUTPUT, 116, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        addDataSlots(data);
    }

    public int progress() { return data.get(0); }
    public int total() { return Math.max(1, data.get(1)); }
    public int state() { return data.get(2); }

    public @Nullable Attunement voice() {
        int ordinal = data.get(3) - 1;
        return ordinal < 0 || ordinal >= Attunement.values().length ? null : Attunement.values()[ordinal];
    }

    @Override public boolean stillValid(Player player) { return container.stillValid(player); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int size = SpiritKettleBlockEntity.SIZE;
        if (index < size) {
            if (!moveItemStackTo(stack, size, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int target = 0; target < SpiritKettleBlockEntity.OUTPUT && !moved; target++)
                if (slots.get(target).mayPlace(stack)) moved = moveItemStackTo(stack, target, target + 1, false);
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }
}
