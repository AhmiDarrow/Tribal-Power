package tk.darrow.tribalpower.items;

import net.minecraft.item.Item;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemAirRune extends Item {

	public ItemAirRune() {
		setUnlocalizedName(Reference.TribalPowerItems.AIRRUNE.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.AIRRUNE.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);
		setMaxStackSize(16);
	}
}
