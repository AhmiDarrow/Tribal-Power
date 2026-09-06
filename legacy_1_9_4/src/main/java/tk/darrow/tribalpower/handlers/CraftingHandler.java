package tk.darrow.tribalpower.handlers;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import tk.darrow.tribalpower.init.ModItems;

public class CraftingHandler {

	@SubscribeEvent
	public void onCrafting(ItemCraftedEvent event) {

		final IInventory craftMatrix = null;
		for (int i = 0; i < event.craftMatrix.getSizeInventory(); i++) {
			if (event.craftMatrix.getStackInSlot(i) != null) {

				ItemStack item0 = event.craftMatrix.getStackInSlot(i);
				if (item0 != null && item0.getItem() == ModItems.runecarver) {
					ItemStack k = new ItemStack(ModItems.runecarver, 2, item0.getItemDamage() + 1);

					if (k.getItemDamage() >= k.getMaxDamage()) {
						k.stackSize--;
					}
					event.craftMatrix.setInventorySlotContents(i, k);
				}

			}

		}

	}
}
