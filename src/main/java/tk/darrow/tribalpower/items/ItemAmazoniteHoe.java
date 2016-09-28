package tk.darrow.tribalpower.items;

import net.minecraft.item.Item;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemHoe;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemAmazoniteHoe extends ItemHoe {
	public ItemAmazoniteHoe(ToolMaterial material) {
		super(material);
		setUnlocalizedName(Reference.TribalPowerItems.AMAZONITEHOE.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.AMAZONITEHOE.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);
		
	}
}
