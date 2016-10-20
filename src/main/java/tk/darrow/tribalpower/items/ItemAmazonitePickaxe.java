package tk.darrow.tribalpower.items;

import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemPickaxe;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemAmazonitePickaxe extends ItemPickaxe {

	public ItemAmazonitePickaxe(ToolMaterial material) {
		super(material);
		setUnlocalizedName(Reference.TribalPowerItems.AMAZONITEPICKAXE.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.AMAZONITEPICKAXE.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);

	}

}
