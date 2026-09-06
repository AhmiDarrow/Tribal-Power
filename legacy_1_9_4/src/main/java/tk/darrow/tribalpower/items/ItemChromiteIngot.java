package tk.darrow.tribalpower.items;

import net.minecraft.item.Item;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemChromiteIngot extends Item {

	public ItemChromiteIngot() {
		setUnlocalizedName(Reference.TribalPowerItems.CHROMITEINGOT.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.CHROMITEINGOT.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);
	}

}
