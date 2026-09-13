package tk.darrow.tribalpower.block;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

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

    /**
     * Keep tanks, rank and pending fluid on the dropped machine. Strip {@code Items} so
     * {@code onRemove} dumps the inventory once.
     */
    public static List<ItemStack> withSelfAndBlockEntity(Block block, List<ItemStack> drops, LootParams.Builder builder) {
        List<ItemStack> out = withSelf(block, drops);
        var be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be == null || be.getLevel() == null) return out;
        CompoundTag data = be.saveWithFullMetadata(be.getLevel().registryAccess());
        data.remove("Items");
        for (ItemStack stack : out) {
            if (stack.is(block.asItem())) {
                stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(data));
            }
        }
        return out;
    }
}
