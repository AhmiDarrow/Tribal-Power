package tk.darrow.tribalpower.items;

import net.minecraft.item.Item;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemFireRune extends Item {

	public ItemFireRune() {
		setUnlocalizedName(Reference.TribalPowerItems.FIRERUNE.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.FIRERUNE.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);
		setMaxStackSize(16);
	}
}
