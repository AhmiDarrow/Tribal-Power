package tk.darrow.tribalpower.block;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/** Placeables always return themselves. Inventories are dumped from onRemove. */
public final class MachineDrops {
    private MachineDrops() {}

    public static List<ItemStack> withSelf(Block block, List<ItemStack> drops) {
        List<ItemStack> out = new ArrayList<>(drops);
        Item item = block.asItem();
        if (item != Items.AIR && out.stream().noneMatch(stack -> stack.is(item))) {
            out.add(new ItemStack(item));
        }
        return out;
    }
}
