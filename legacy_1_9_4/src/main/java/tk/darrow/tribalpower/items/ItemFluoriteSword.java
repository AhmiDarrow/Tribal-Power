package tk.darrow.tribalpower.items;

import net.minecraft.item.ItemSword;
import net.minecraft.item.Item.ToolMaterial;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemFluoriteSword extends ItemSword {

	public ItemFluoriteSword(ToolMaterial material) {
		super(material);
		setUnlocalizedName(Reference.TribalPowerItems.FLUORITESWORD.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.FLUORITESWORD.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);

	}
}
