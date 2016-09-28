package tk.darrow.tribalpower.items;

import net.minecraft.item.Item;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemAmazoniteNugget extends Item {
	
	public ItemAmazoniteNugget() {
		setUnlocalizedName(Reference.TribalPowerItems.AMAZONITENUGGET.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.AMAZONITENUGGET.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);
	}
}
