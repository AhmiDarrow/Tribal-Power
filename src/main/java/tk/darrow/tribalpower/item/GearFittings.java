package tk.darrow.tribalpower.item;

import net.minecraft.world.item.ItemStack;

/**
 * What can be taken back off a piece of equipment: its seated Pulse Cell (with whatever charge is left in it),
 * then a hood's Ley Goggles (as the Ley Lens they were made from). Right-clicking a piece in an inventory with an
 * empty cursor takes one fitting per click, the cell first; see {@link GearCell#stackedOn}.
 *
 * <p>Rank and voice are not fittings: they are worked into the piece and stay.
 */
public final class GearFittings {
    private GearFittings() {}

    /** Whether {@link #detach} would take anything off {@code gear}. */
    public static boolean hasFitting(ItemStack gear) {
        return GearCell.cell(gear) != null || SpiritGear.goggles(gear);
    }

    /** Takes the next fitting off {@code gear} and returns it, or empty when there is nothing to take. */
    public static ItemStack detach(ItemStack gear) {
        ItemStack cell = GearCell.asStack(gear);
        if (!cell.isEmpty()) {
            GearCell.unseat(gear);
            return cell;
        }
        if (SpiritGear.goggles(gear)) {
            SpiritGear.setGoggles(gear, false);
            return new ItemStack(tk.darrow.tribalpower.ley.LeyRegistry.LEY_LENS.get());
        }
        return ItemStack.EMPTY;
    }
}
