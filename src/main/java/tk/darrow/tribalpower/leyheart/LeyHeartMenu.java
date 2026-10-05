package tk.darrow.tribalpower.leyheart;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import tk.darrow.tribalpower.blockentity.PulseResonatorBlockEntity;
import tk.darrow.tribalpower.song.Reagents;

/** The crystal and the reagent on the left, the water beside them, the heart's readout on the right. */
public class LeyHeartMenu extends AbstractContainerMenu {
    public static final int CRYSTAL_X = 17, CRYSTAL_Y = 19, REAGENT_X = 17, REAGENT_Y = 45;
    private final Container container;
    private final ContainerData data;

    public LeyHeartMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(LeyHeartBlockEntity.SIZE), new SimpleContainerData(LeyHeartBlockEntity.DATA_COUNT));
    }

    public LeyHeartMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(LeyHeartRegistry.LEY_HEART_MENU.get(), id);
        this.container = container;
        this.data = data;
        checkContainerSize(container, LeyHeartBlockEntity.SIZE);
        checkContainerDataCount(data, LeyHeartBlockEntity.DATA_COUNT);
        addSlot(new Slot(container, LeyHeartBlockEntity.CRYSTAL, CRYSTAL_X, CRYSTAL_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return PulseResonatorBlockEntity.isCatalyst(stack); }
        });
        addSlot(new Slot(container, LeyHeartBlockEntity.REAGENT, REAGENT_X, REAGENT_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return Reagents.isReagent(stack); }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        addDataSlots(data);
    }

    public int data(int index) { return data.get(index); }

    /** A reading sent as two 15-bit halves, {@code lo} and {@code hi}: ContainerData travels as shorts. */
    public int wide(int lo, int hi) { return (data.get(lo) & 0x7FFF) | (data.get(hi) & 0xFFFF) << 15; }

    @Override public boolean stillValid(Player player) { return container.stillValid(player); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int size = LeyHeartBlockEntity.SIZE;
        if (index < size) {
            if (!moveItemStackTo(stack, size, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int target = 0; target < size && !moved; target++)
                if (slots.get(target).mayPlace(stack)) moved = moveItemStackTo(stack, target, target + 1, false);
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }
}
