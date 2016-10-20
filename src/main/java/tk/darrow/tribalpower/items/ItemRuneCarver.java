package tk.darrow.tribalpower.items;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.init.ModItems;

public class ItemRuneCarver extends Item {

	private Item containerItem;


	public ItemRuneCarver() {
		super();
		this.maxStackSize = 1;
		this.setMaxDamage(64);
		setNoRepair();
		setUnlocalizedName(Reference.TribalPowerItems.RUNECARVER.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerItems.RUNECARVER.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);
		
	}

	@Override
	 public Item setContainerItem(Item containerItem)
    {
        this.containerItem = containerItem;
        return this;
    }
	
	
	public ItemStack getContainerItemStack(ItemStack itemStack) {

		ItemStack returnItem = new ItemStack(itemStack.getItem(), 1, itemStack.getItemDamage() + 1);

		if (itemStack.isItemEnchanted()) {
			NBTTagCompound nbtcompound = itemStack.getTagCompound();
			returnItem.setTagCompound(nbtcompound);
		}

		return returnItem;
	}
	
}
