package tk.darrow.tribalpower.items;

import net.minecraft.item.Item;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class ItemAcoomaStick extends Item {
	
	public ItemAcoomaStick() {
		setUnlocalizedName(Reference.TribalPowerItems.ACOOMASTICK.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.ACOOMASTICK.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);
	
	}
	
	@SideOnly(Side.CLIENT)
    public boolean isFull3D()
    {
        return true;
    }
}
