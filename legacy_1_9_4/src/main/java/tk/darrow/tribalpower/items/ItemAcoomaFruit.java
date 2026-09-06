package tk.darrow.tribalpower.items;

import net.minecraft.item.ItemFood;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemAcoomaFruit extends ItemFood {

	public ItemAcoomaFruit() {
		super(6, 0.3F, false);

		setUnlocalizedName(Reference.TribalPowerItems.ACOOMAFRUIT.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.ACOOMAFRUIT.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);
	}

}
