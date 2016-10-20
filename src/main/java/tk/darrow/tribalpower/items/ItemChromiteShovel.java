package tk.darrow.tribalpower.items;

import net.minecraft.item.ItemSpade;
import net.minecraft.item.Item.ToolMaterial;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemChromiteShovel extends ItemSpade {

	public ItemChromiteShovel(ToolMaterial material) {
		super(material);
		setUnlocalizedName(Reference.TribalPowerItems.CHROMITESHOVEL.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.CHROMITESHOVEL.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);

	}
}
