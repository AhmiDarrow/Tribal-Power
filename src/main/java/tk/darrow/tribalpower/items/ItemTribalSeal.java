package tk.darrow.tribalpower.items;




import net.minecraft.item.Item;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemTribalSeal extends Item {
	
	public ItemTribalSeal() {
		setUnlocalizedName(Reference.TribalPowerItems.TRIBALSEAL.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.TRIBALSEAL.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);
	}
}
