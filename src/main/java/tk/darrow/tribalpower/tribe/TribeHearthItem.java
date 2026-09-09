package tk.darrow.tribalpower.tribe;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Block item for hearth / banner / kinship totem carrying {@code Tribe} in CUSTOM_DATA. */
public class TribeHearthItem extends BlockItem {
    public TribeHearthItem(Block block, Properties properties) { super(block, properties); }

    @Override
    public Component getName(ItemStack stack) {
        TribeDefinition tribe = TribeDefinition.of(stack);
        return tribe == null ? super.getName(stack) : Component.translatable(getDescriptionId(stack) + ".named", tribe.displayNameComponent());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        TribeDefinition tribe = TribeDefinition.of(stack);
        if (tribe != null) tooltip.add(Component.translatable("tribe.tribalpower.strand", tribe.strand(),
                Component.translatable("attunement.tribalpower." + tribe.attunement().getSerializedName())).withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
