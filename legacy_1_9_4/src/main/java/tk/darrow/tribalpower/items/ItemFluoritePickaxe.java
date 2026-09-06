package tk.darrow.tribalpower.items;

import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.Item.ToolMaterial;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemFluoritePickaxe extends ItemPickaxe {

	public ItemFluoritePickaxe(ToolMaterial material) {
		super(material);
		setUnlocalizedName(Reference.TribalPowerItems.FLUORITEPICKAXE.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.FLUORITEPICKAXE.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);

	}
}
