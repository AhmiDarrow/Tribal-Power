package tk.darrow.tribalpower.items;

import net.minecraft.item.Item;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemTool;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemAmazoniteShovel extends ItemSpade {
	
	public ItemAmazoniteShovel(ToolMaterial material) {
		super(material);
		setUnlocalizedName(Reference.TribalPowerItems.AMAZONITESHOVEL.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.AMAZONITESHOVEL.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);
		
	}
}
