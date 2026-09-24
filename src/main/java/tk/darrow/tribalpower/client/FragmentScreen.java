package tk.darrow.tribalpower.client;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import tk.darrow.tribalpower.lore.Chronicle;

/** A stone-grey panel showing one fragment of the Chronicle, read off a carving or a mural. Client only. */
public final class FragmentScreen extends Screen {
    private static final int INK = 0xFFE8E2D0, STONE = 0xFF2E3238, STONE_DARK = 0xFF1E2126, TEAL = 0xFF62D1C9, GOLD = 0xFFD8B36A;
    private final int fragment;

    public FragmentScreen(int fragment) {
        super(Component.translatable(Chronicle.titleKey(fragment)));
        this.fragment = Math.floorMod(fragment, Chronicle.FRAGMENTS);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        int w = Math.min(width - 24, 260), h = Math.min(height - 24, 176);
        int x = (width - w) / 2, y = (height - h) / 2;
        g.fill(x - 3, y - 3, x + w + 3, y + h + 3, STONE_DARK);
        g.fill(x, y, x + w, y + h, STONE);
        g.fill(x, y, x + w, y + 1, TEAL);
        g.fill(x + 6, y + 6, x + w - 6, y + h - 6, STONE_DARK);
        g.fill(x + 7, y + 7, x + w - 7, y + h - 7, STONE);
        g.drawCenteredString(font, Component.translatable("screen.tribalpower.chronicle.index", fragment + 1, Chronicle.FRAGMENTS), x + w / 2, y + 12, GOLD);
        g.drawCenteredString(font, title, x + w / 2, y + 24, INK);
        g.fill(x + 24, y + 35, x + w - 24, y + 36, GOLD);
        List<FormattedCharSequence> lines = font.split(Component.translatable(Chronicle.textKey(fragment)), w - 32);
        int ly = y + 42;
        for (FormattedCharSequence line : lines) {
            if (ly > y + h - 28) break;
            g.drawString(font, line, x + 16, ly, INK, false);
            ly += 10;
        }
        g.drawCenteredString(font, Component.translatable("screen.tribalpower.chronicle.footer"), x + w / 2, y + h - 16, 0xFF8A9096);
    }

    @Override protected void renderBlurredBackground(float partial) {}
}
