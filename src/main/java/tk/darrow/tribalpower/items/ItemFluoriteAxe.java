package tk.darrow.tribalpower.items;

import net.minecraft.item.ItemAxe;
import net.minecraft.item.Item.ToolMaterial;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemFluoriteAxe extends ItemAxe {

	public ItemFluoriteAxe(ToolMaterial material, float damage, float speed) {
		super(material, damage, speed);
		setUnlocalizedName(Reference.TribalPowerItems.FLUORITEAXE.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.FLUORITEAXE.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);

	}
}
