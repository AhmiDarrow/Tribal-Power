package tk.darrow.tribalpower.items;

import net.minecraft.item.Item;
import net.minecraft.item.ItemSword;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemAmazoniteSword extends ItemSword {

	public ItemAmazoniteSword(ToolMaterial material) {
		super(material);
		setUnlocalizedName(Reference.TribalPowerItems.AMAZONITESWORD.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.AMAZONITESWORD.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);
		
	}

}
