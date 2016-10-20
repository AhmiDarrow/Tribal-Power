package tk.darrow.tribalpower.items;

import net.minecraft.item.Item;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemAirEssence extends Item {
	
	public ItemAirEssence() {
		setUnlocalizedName(Reference.TribalPowerItems.AIRESSENCE.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.AIRESSENCE.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);
		
	}
}
