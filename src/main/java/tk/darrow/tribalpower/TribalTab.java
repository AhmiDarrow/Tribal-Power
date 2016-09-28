package tk.darrow.tribalpower;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import tk.darrow.tribalpower.init.ModItems;

public class TribalTab extends CreativeTabs{

	public TribalTab() {
		super("tabTribal");
		
	}
	public Item getTabIconItem() {
		return ModItems.tribalseal;
	}
}
