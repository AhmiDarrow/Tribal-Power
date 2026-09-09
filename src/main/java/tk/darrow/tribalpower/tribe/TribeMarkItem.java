package tk.darrow.tribalpower.tribe;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** One item; the tribe lives in CUSTOM_DATA {@code Tribe}. Earned once per tribe at Voice rank. */
public class TribeMarkItem extends Item {
    public TribeMarkItem(Properties properties) { super(properties); }

    @Override
    public Component getName(ItemStack stack) {
        TribeDefinition tribe = TribeDefinition.of(stack);
        return tribe == null ? super.getName(stack) : Component.translatable("item.tribalpower.tribe_mark.named", tribe.displayNameComponent());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        TribeDefinition tribe = TribeDefinition.of(stack);
        if (tribe == null) { tooltip.add(Component.translatable("item.tribalpower.tribe_mark.blank").withStyle(ChatFormatting.GRAY)); return; }
        tooltip.add(tribe.displayNameComponent().withStyle(TribeStanding.colour(tribe)));
        tooltip.add(tribe.marginComponent().withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
    }

    @Override public boolean isFoil(ItemStack stack) { return TribeDefinition.of(stack) != null; }
}
