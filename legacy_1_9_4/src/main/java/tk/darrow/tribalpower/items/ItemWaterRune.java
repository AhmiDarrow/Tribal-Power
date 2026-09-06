package tk.darrow.tribalpower.items;

import net.minecraft.item.Item;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemWaterRune extends Item {
	public ItemWaterRune() {
		setUnlocalizedName(Reference.TribalPowerItems.WATERRUNE.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.WATERRUNE.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);
		setMaxStackSize(16);
	}
}
