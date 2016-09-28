package tk.darrow.tribalpower.items;

import net.minecraft.item.Item;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemFluorite extends Item {
	
	public ItemFluorite() {
		setUnlocalizedName(Reference.TribalPowerItems.FLUORITE.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.FLUORITE.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);
	}
}
