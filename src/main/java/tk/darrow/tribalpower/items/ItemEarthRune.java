package tk.darrow.tribalpower.items;

import net.minecraft.item.Item;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemEarthRune extends Item {
	
	public ItemEarthRune() {
		setUnlocalizedName(Reference.TribalPowerItems.EARTHRUNE.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.EARTHRUNE.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);
	}
}
