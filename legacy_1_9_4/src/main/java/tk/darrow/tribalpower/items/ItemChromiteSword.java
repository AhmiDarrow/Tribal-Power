package tk.darrow.tribalpower.items;

import net.minecraft.item.ItemSword;
import net.minecraft.item.Item.ToolMaterial;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemChromiteSword extends ItemSword {

	public ItemChromiteSword(ToolMaterial material) {
		super(material);
		setUnlocalizedName(Reference.TribalPowerItems.CHROMITESWORD.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.CHROMITESWORD.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);

	}
}
