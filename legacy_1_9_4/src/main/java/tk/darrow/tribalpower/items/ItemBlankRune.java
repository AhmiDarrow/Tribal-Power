package tk.darrow.tribalpower.items;

import net.minecraft.item.Item;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemBlankRune extends Item {

	public ItemBlankRune() {
		setUnlocalizedName(Reference.TribalPowerItems.BLANKRUNE.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.BLANKRUNE.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);
		setMaxStackSize(16);
	}
}
