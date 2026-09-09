package tk.darrow.tribalpower.camp.identity;

import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import tk.darrow.tribalpower.storage.DeepCacheContainer;
import tk.darrow.tribalpower.storage.DeepCacheSavedData;

/**
 * Live view of a camp's shared 54-slot vault, stored on {@link CampSavedData}. Extends {@link DeepCacheContainer}
 * so the Deep Cache block and Wayfarer Satchel open it unchanged; every container method is redirected to the
 * camp's list (the super view is a detached, empty scratch record).
 */
public class CampVaultContainer extends DeepCacheContainer {
    private final CampSavedData data;
    private final CampSavedData.Camp camp;
    private final NonNullList<ItemStack> items;
    public CampVaultContainer(CampSavedData data,CampSavedData.Camp camp) {
        super(new DeepCacheSavedData(),camp.id);
        this.data=data;this.camp=camp;this.items=camp.vault;
    }
    public CampSavedData.Camp camp() { return camp; }
    @Override public int getContainerSize() { return CampSavedData.VAULT_SLOTS; }
    @Override public boolean isEmpty() { for(var stack:items)if(!stack.isEmpty())return false;return true; }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot,int amount) { var result=ContainerHelper.removeItem(items,slot,amount);if(!result.isEmpty())setChanged();return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) { var stack=items.get(slot);if(stack.isEmpty())return ItemStack.EMPTY;items.set(slot,ItemStack.EMPTY);setChanged();return stack; }
    @Override public void setItem(int slot,ItemStack stack) { items.set(slot,stack);if(stack.getCount()>getMaxStackSize())stack.setCount(getMaxStackSize());setChanged(); }
    @Override public void setChanged() { data.setDirty(); }
    @Override public boolean stillValid(Player player) { return camp.isMember(player.getUUID()) && data.camp(camp.id)==camp; }
    @Override public void clearContent() { items.clear();setChanged(); }
}
