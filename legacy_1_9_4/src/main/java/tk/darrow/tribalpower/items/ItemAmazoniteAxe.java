package tk.darrow.tribalpower.items;

import net.minecraft.item.Item;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemAxe;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.init.TribalToolMaterials;

public class ItemAmazoniteAxe extends ItemAxe {

	public ItemAmazoniteAxe(ToolMaterial material, float damage, float speed) {
		super(material, damage, speed);
		setUnlocalizedName(Reference.TribalPowerItems.AMAZONITEAXE.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.AMAZONITEAXE.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);

	}

}
