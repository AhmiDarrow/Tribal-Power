package tk.darrow.tribalpower.items;

import net.minecraft.item.ItemSpade;
import net.minecraft.item.Item.ToolMaterial;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemFluoriteShovel extends ItemSpade {

	public ItemFluoriteShovel(ToolMaterial material) {
		super(material);
		setUnlocalizedName(Reference.TribalPowerItems.FLUORITESHOVEL.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.FLUORITESHOVEL.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);

	}
}
