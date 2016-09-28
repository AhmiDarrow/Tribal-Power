package tk.darrow.tribalpower.items;

import net.minecraft.item.Item;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemSpiritDiamond extends Item {
	
	public ItemSpiritDiamond() {
		setUnlocalizedName(Reference.TribalPowerItems.SPIRITDIAMOND.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.SPIRITDIAMOND.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);
	}
}
