package tk.darrow.tribalpower.client;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import tk.darrow.tribalpower.familiar.Familiar;
import tk.darrow.tribalpower.familiar.FamiliarData;

/**
 * A creature's stats at a glance: look at your own companion, or sneak and look at any March creature, and a
 * small panel beside the crosshair shows its five threads, its Marks, its generation and its bloodline total.
 */
public final class FamiliarInspectHud {
    private static final int[] TIER = {0xFF5A5F63, 0xFFB8C4BC, 0xFF7CE0A0, 0xFF5CC8F0, 0xFFC08CFF, 0xFFFFC857};

    private FamiliarInspectHud() {}

    public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;
        if (!(mc.crosshairPickEntity instanceof Familiar familiar)) return;
        boolean own = familiar.isOwnedBy(mc.player);
        if (!own && !mc.player.isShiftKeyDown()) return;
        int packed = familiar.syncedStats();
        if (packed == 0) return;
        var g = event.getGuiGraphics();
        var font = mc.font;
        int x = mc.getWindow().getGuiScaledWidth() / 2 + 14, y = mc.getWindow().getGuiScaledHeight() / 2 - 44;
        int width = 132, room = width - 10;
        int bloodline = 0;
        for (FamiliarData.Thread thread : FamiliarData.Thread.values()) bloodline += FamiliarData.unpackThread(packed, thread);
        var marks = Component.empty();
        for (int slot = 0; slot < 2; slot++) {
            var mark = FamiliarData.unpackMark(packed, slot);
            if (mark == FamiliarData.Mark.NONE) continue;
            if (!marks.getSiblings().isEmpty()) marks.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
            marks.append(Component.translatable("mark.tribalpower." + mark.id).withStyle(ChatFormatting.GOLD));
        }
        if (marks.getSiblings().isEmpty()) marks = Component.translatable("gui.tribalpower.lattice.marks_none").withStyle(ChatFormatting.DARK_GRAY);
        // Two long Marks, or the summary, wrap onto another row and the panel grows to hold them.
        List<FormattedCharSequence> markLines = font.split(marks, room);
        List<FormattedCharSequence> summaryLines = font.split(Component.translatable("gui.tribalpower.lattice.summary",
                FamiliarData.unpackGeneration(packed), bloodline), room);
        // its voice once attuned, or whether and how a wild one can be bonded
        var status = tk.darrow.tribalpower.familiar.FamiliarBoost.status(familiar, familiar.syncedVoice());
        List<FormattedCharSequence> statusLines = status == null ? List.of() : font.split(status, room);
        int height = 86 + 10 * (Math.max(1, markLines.size()) - 1 + Math.max(1, summaryLines.size()) - 1 + statusLines.size());
        g.fill(x, y, x + width, y + height, 0xD9101B22);
        g.fill(x, y, x + 2, y + height, own ? 0xFF65D7C0 : 0xFFB58A58);
        g.drawString(font, fit(font, familiar.asMob().getDisplayName(), room), x + 6, y + 4, 0xFFE7DCC1, false);
        int row = y + 16;
        for (FamiliarData.Thread thread : FamiliarData.Thread.values()) {
            int value = FamiliarData.unpackThread(packed, thread);
            g.drawString(font, Component.translatable("gui.tribalpower.lattice." + thread.name().toLowerCase(java.util.Locale.ROOT)), x + 6, row, 0xFF99C9BD, false);
            for (int i = 0; i < FamiliarData.MAX; i++) {
                int px = x + 62 + i * 9;
                g.fill(px, row + 1, px + 7, row + 7, i < value ? TIER[Mth.clamp(value, 0, 5)] : 0xFF2A3438);
            }
            row += 10;
        }
        for (FormattedCharSequence line : markLines) {
            g.drawString(font, line, x + 6, row + 1, 0xFFE7DCC1, false);
            row += 10;
        }
        for (FormattedCharSequence line : statusLines) {
            g.drawString(font, line, x + 6, row + 1, 0xFFE7DCC1, false);
            row += 10;
        }
        int summary = FamiliarData.unpackSparked(packed) ? 0xFFFFE08A : 0xFF8FA8A0;
        for (FormattedCharSequence line : summaryLines) {
            g.drawString(font, line, x + 6, row + 2, summary, false);
            row += 10;
        }
    }

    /** The text whole if it fits the room, else cut short with an ellipsis; its styling survives the cut. */
    private static FormattedCharSequence fit(Font font, Component text, int room) {
        if (font.width(text) <= room) return text.getVisualOrderText();
        FormattedText cut = font.substrByWidth(text, room - font.width("…"));
        return Language.getInstance().getVisualOrder(FormattedText.composite(cut, Component.literal("…")));
    }
}
