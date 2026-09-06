package tk.darrow.tribalpower.items;

import net.minecraft.item.ItemHoe;
import net.minecraft.item.Item.ToolMaterial;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemFluoriteHoe extends ItemHoe {
	public ItemFluoriteHoe(ToolMaterial material) {
		super(material);
		setUnlocalizedName(Reference.TribalPowerItems.FLUORITEHOE.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.FLUORITEHOE.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);

	}
}
