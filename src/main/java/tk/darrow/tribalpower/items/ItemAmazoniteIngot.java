package tk.darrow.tribalpower.items;

import net.minecraft.item.Item;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemAmazoniteIngot extends Item {
	
	public ItemAmazoniteIngot() {
		setUnlocalizedName(Reference.TribalPowerItems.AMAZONITEINGOT.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.AMAZONITEINGOT.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);
	}
}

