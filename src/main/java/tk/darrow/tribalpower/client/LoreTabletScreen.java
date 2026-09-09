package tk.darrow.tribalpower.client;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import tk.darrow.tribalpower.world.structure.LoreTabletBlock;

/** A small parchment panel showing one Lore Tablet fragment (design §3). Client only. */
public final class LoreTabletScreen extends Screen {
    private static final int INK = 0xFF1B1A22, PAPER = 0xFFE9DFC4, PAPER_DARK = 0xFFCDBF9B, TEAL = 0xFF62D1C9, GOLD = 0xFFB88A3C;
    private final int tablet;

    public LoreTabletScreen(int tablet) {
        super(Component.translatable("lore.tribalpower.tablet." + Math.floorMod(tablet, LoreTabletBlock.TABLETS) + ".title"));
        this.tablet = Math.floorMod(tablet, LoreTabletBlock.TABLETS);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        int w = Math.min(width - 24, 260), h = Math.min(height - 24, 176);
        int x = (width - w) / 2, y = (height - h) / 2;
        // parchment with a darker frame and a teal thread along the top edge
        g.fill(x - 3, y - 3, x + w + 3, y + h + 3, INK);
        g.fill(x, y, x + w, y + h, PAPER);
        g.fill(x, y, x + w, y + 1, TEAL);
        g.fill(x + 6, y + 6, x + w - 6, y + h - 6, PAPER_DARK);
        g.fill(x + 7, y + 7, x + w - 7, y + h - 7, PAPER);
        g.drawCenteredString(font, Component.literal("Tablet " + (tablet + 1) + " of " + LoreTabletBlock.TABLETS).withStyle(s -> s.withColor(GOLD)), x + w / 2, y + 12, GOLD);
        g.drawCenteredString(font, title, x + w / 2, y + 24, INK);
        g.fill(x + 24, y + 35, x + w - 24, y + 36, GOLD);
        List<FormattedCharSequence> lines = font.split(Component.translatable("lore.tribalpower.tablet." + tablet + ".text"), w - 32);
        int ly = y + 42;
        for (FormattedCharSequence line : lines) {
            if (ly > y + h - 28) break;
            g.drawString(font, line, x + 16, ly, INK, false);
            ly += 10;
        }
        g.drawCenteredString(font, Component.translatable("screen.tribalpower.lore_tablet.footer"), x + w / 2, y + h - 16, 0xFF5B5346);
    }

    @Override
    protected void renderBlurredBackground(float partial) { }
}
