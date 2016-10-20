package tk.darrow.tribalpower.items;

import net.minecraft.item.ItemAxe;
import net.minecraft.item.Item.ToolMaterial;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemChromiteAxe extends ItemAxe {

	public ItemChromiteAxe(ToolMaterial material, float damage, float speed) {
		super(material, damage, speed);
		setUnlocalizedName(Reference.TribalPowerItems.CHROMITEAXE.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.CHROMITEAXE.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);

	}
}
