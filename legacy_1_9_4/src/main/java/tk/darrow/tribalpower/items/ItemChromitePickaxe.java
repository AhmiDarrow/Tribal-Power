package tk.darrow.tribalpower.items;

import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemPickaxe;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemChromitePickaxe extends ItemPickaxe {

	public ItemChromitePickaxe(ToolMaterial material) {
		super(material);
		setUnlocalizedName(Reference.TribalPowerItems.CHROMITEPICKAXE.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.CHROMITEPICKAXE.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);

	}
}
