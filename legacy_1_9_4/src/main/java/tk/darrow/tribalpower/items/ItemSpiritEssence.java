package tk.darrow.tribalpower.items;

import net.minecraft.item.Item;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemSpiritEssence extends Item {

	public ItemSpiritEssence() {
		setUnlocalizedName(Reference.TribalPowerItems.SPIRITESSENCE.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.SPIRITESSENCE.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);
	}
}
