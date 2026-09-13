package tk.darrow.tribalpower.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Extra grey how-to lines for items that declare {@code <descriptionId>.hint} in en_us.
 */
final class ItemHints {
    private ItemHints() {}

    static void tooltip(ItemTooltipEvent event) {
        String key = event.getItemStack().getItem().getDescriptionId() + ".hint";
        if (I18n.exists(key)) {
            event.getToolTip().add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        }
        tk.darrow.tribalpower.item.MachineRank.appendTooltip(event.getItemStack(), event.getToolTip());
    }
}
