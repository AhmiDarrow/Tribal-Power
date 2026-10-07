package tk.darrow.tribalpower.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.entity.CreatureProfile;
import tk.darrow.tribalpower.healing.Remedy;
import tk.darrow.tribalpower.song.Anointment;
import tk.darrow.tribalpower.song.Note;
import tk.darrow.tribalpower.song.Reagents;

import java.util.List;

/**
 * Extra grey how-to lines for items that declare {@code <descriptionId>.hint} in en_us, and a
 * generated pair of lines for every reagent: which creature it comes from, what its note does when
 * sung, and which anointment and remedy it makes. Long lines wrap to a tooltip's width.
 *
 * <p>Only Tribal Power's own items are considered: the companion mods each run the same hook, and a hint
 * key looked up for every item would print once per installed mod.
 */
final class ItemHints {
    /** About vanilla's own tooltip wrap: wide enough for a sentence, narrow enough to read. */
    private static final int WRAP_WIDTH = 220;

    private ItemHints() {}

    static void tooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> lines = event.getToolTip();
        if (TribalPower.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())) {
            String key = stack.getItem().getDescriptionId() + ".hint";
            if (I18n.exists(key)) addWrapped(lines, Component.translatable(key));
        }
        CreatureProfile profile = Reagents.of(stack.getItem());
        if (profile != null) {
            Note note = Note.of(profile);
            String noteId = note.name().toLowerCase(java.util.Locale.ROOT);
            addWrapped(lines, Component.translatable("item.tribalpower.reagent.from",
                    Component.translatable("song.tribalpower.note." + noteId),
                    Component.translatable("entity.tribalpower." + profile.id)));
            addWrapped(lines, Component.translatable("item.tribalpower.reagent.uses",
                    Component.translatable("item.tribalpower.reagent.song." + noteId),
                    Component.translatable("anointment.tribalpower." + Anointment.of(note).id()),
                    Component.translatable("remedy.tribalpower." + Remedy.of(note).id())));
        }
        tk.darrow.tribalpower.item.MachineRank.appendTooltip(stack, lines);
    }

    private static void addWrapped(List<Component> lines, Component text) {
        for (FormattedText part : Minecraft.getInstance().font.getSplitter().splitLines(text, WRAP_WIDTH, Style.EMPTY)) {
            lines.add(Component.literal(part.getString()).withStyle(ChatFormatting.GRAY));
        }
    }
}
