package tk.darrow.tribalpower.song;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import tk.darrow.tribalpower.echo.ModMenus;
import tk.darrow.tribalpower.entity.CreatureProfile;

/** Look through the pouch and take raw reagents back out. Empowering happens at the bench. */
public class PouchMenu extends AbstractContainerMenu {
    public static final int WIDTH = 196;
    public static final int HEIGHT = 196;
    private final Inventory inventory;
    private final int pouchSlot;

    public PouchMenu(int id, Inventory inventory, int pouchSlot) {
        super(ModMenus.REAGENT_POUCH.get(), id);
        this.inventory = inventory;
        this.pouchSlot = pouchSlot;
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 18 + col * 18, 114 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, 18 + col * 18, 172));
    }

    public PouchMenu(int id, Inventory inventory) {
        this(id, inventory, inventory.selected);
    }

    public ItemStack pouch() {
        if (pouchSlot == 40) return inventory.player.getOffhandItem();
        return inventory.getItem(pouchSlot);
    }

    @Override
    public boolean stillValid(Player player) {
        return pouch().getItem() instanceof ReagentPouchItem;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide || id < SongBenchMenu.WITHDRAW) return false;
        CreatureProfile[] values = CreatureProfile.values();
        int ordinal = id - SongBenchMenu.WITHDRAW;
        if (ordinal < 0 || ordinal >= values.length) return false;
        CreatureProfile profile = values[ordinal];
        ItemStack pouch = pouch();
        int taken = ReagentPouch.takeRaw(pouch, profile, 64);
        if (taken <= 0) return true;
        ItemStack stack = new ItemStack(Reagents.item(profile), taken);
        if (!player.getInventory().add(stack)) player.drop(stack, false);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
