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

    /**
     * The wrapped lines each item adds, which depend on the item alone. A tooltip is rebuilt every frame it shows,
     * and this looked up the hint key, resolved up to six translations and wrapped them each time; the text is kept
     * until the language is loaded again (any resource reload makes a new one).
     */
    private static final java.util.Map<net.minecraft.world.item.Item, List<String>> WRAPPED = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile net.minecraft.locale.Language wrappedFor;

    private ItemHints() {}

    static void tooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> lines = event.getToolTip();
        net.minecraft.locale.Language language = net.minecraft.locale.Language.getInstance();
        if (language != wrappedFor) {
            WRAPPED.clear();
            wrappedFor = language;
        }
        for (String line : WRAPPED.computeIfAbsent(stack.getItem(), ItemHints::hints))
            lines.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        tk.darrow.tribalpower.item.MachineRank.appendTooltip(stack, lines);
    }

    /** The item's how-to hint, then its reagent lines, wrapped. */
    private static List<String> hints(net.minecraft.world.item.Item item) {
        List<String> out = new java.util.ArrayList<>();
        if (TribalPower.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())) {
            String key = item.getDescriptionId() + ".hint";
            if (I18n.exists(key)) addWrapped(out, Component.translatable(key));
        }
        CreatureProfile profile = Reagents.of(item);
        if (profile != null) {
            Note note = Note.of(profile);
            String noteId = note.name().toLowerCase(java.util.Locale.ROOT);
            addWrapped(out, Component.translatable("item.tribalpower.reagent.from",
                    Component.translatable("song.tribalpower.note." + noteId),
                    Component.translatable("entity.tribalpower." + profile.id)));
            addWrapped(out, Component.translatable("item.tribalpower.reagent.uses",
                    Component.translatable("item.tribalpower.reagent.song." + noteId),
                    Component.translatable("anointment.tribalpower." + Anointment.of(note).id()),
                    Component.translatable("remedy.tribalpower." + Remedy.of(note).id())));
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static void addWrapped(List<String> lines, Component text) {
        for (FormattedText part : Minecraft.getInstance().font.getSplitter().splitLines(text, WRAP_WIDTH, Style.EMPTY)) {
            lines.add(part.getString());
        }
    }
}
