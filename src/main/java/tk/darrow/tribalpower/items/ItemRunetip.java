package tk.darrow.tribalpower.items;

import net.minecraft.item.Item;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemRunetip extends Item {
	public ItemRunetip() {
		setUnlocalizedName(Reference.TribalPowerItems.RUNETIP.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.RUNETIP.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);
	}
}
