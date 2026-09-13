package tk.darrow.tribalpower.charm;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

/** Three Spirit Charm slots owned by Tribal Power — no Curios required. */
public final class CharmInventory implements Container {
    public static final int SIZE = 3;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    public CharmInventory() {}

    public static final IAttachmentSerializer<CompoundTag, CharmInventory> SERIALIZER =
            new IAttachmentSerializer<>() {
                @Override
                public CharmInventory read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider lookup) {
                    CharmInventory inv = new CharmInventory();
                    ContainerHelper.loadAllItems(tag, inv.items, lookup);
                    return inv;
                }

                @Override
                public CompoundTag write(CharmInventory inv, HolderLookup.Provider lookup) {
                    CompoundTag tag = new CompoundTag();
                    ContainerHelper.saveAllItems(tag, inv.items, lookup);
                    return tag;
                }
            };

    @Override public int getContainerSize() { return SIZE; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack taken = ContainerHelper.removeItem(items, slot, amount);
        setChanged();
        return taken;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); setChanged(); }
    @Override public void setChanged() {}
    @Override public boolean stillValid(Player player) { return true; }
    @Override public void clearContent() { items.clear(); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return stack.getItem() instanceof SpiritCharmItem; }

    public boolean equip(ItemStack stack) {
        if (!(stack.getItem() instanceof SpiritCharmItem)) return false;
        for (int i = 0; i < SIZE; i++) {
            if (items.get(i).isEmpty()) {
                setItem(i, stack.split(1));
                return true;
            }
        }
        return false;
    }
}
