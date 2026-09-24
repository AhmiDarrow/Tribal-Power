package tk.darrow.tribalpower.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import tk.darrow.tribalpower.quest.DialogueSession;
import tk.darrow.tribalpower.tribe.TribeDefinition;

/** An Elder speaking: the hearth's parchment panel with the tribe's name, its words, and the things you may say back. */
public class DialogueScreen extends Screen {
    private static final int INK = 0xFF1B1A22, PAPER = 0xFFE9DFC4, PAPER_DARK = 0xFFCDBF9B, GOLD = 0xFFB88A3C;
    private final DialogueSession.Open open;
    private final TribeDefinition tribe;
    private final List<Component> choices = new ArrayList<>();

    public DialogueScreen(DialogueSession.Open open) {
        super(Component.translatable("entity.tribalpower.tribal_kin.elder", TribeDefinition.byOrdinal(open.tribe()).displayNameComponent()));
        this.open = open;
        this.tribe = TribeDefinition.byOrdinal(open.tribe());
        for (String key : open.choices()) choices.add(Component.translatable(key));
    }

    public static void open(DialogueSession.Open payload) {
        Minecraft.getInstance().setScreen(new DialogueScreen(payload));
    }

    private int panelWidth() { return Math.min(width - 24, 300); }
    private int panelHeight() { return Math.min(height - 24, 120 + choices.size() * 22); }

    @Override
    protected void init() {
        int w = panelWidth(), h = panelHeight();
        int x = (width - w) / 2, y = (height - h) / 2;
        int by = y + h - 12 - choices.size() * 22;
        for (int i = 0; i < choices.size(); i++) {
            String key = open.choices().get(i);
            addRenderableWidget(Button.builder(choices.get(i), b -> {
                PacketDistributor.sendToServer(new DialogueSession.Choose(open.kin(), open.node(), key));
                onClose();
            }).bounds(x + 16, by + i * 22, w - 32, 18).build());
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        int w = panelWidth(), h = panelHeight();
        int x = (width - w) / 2, y = (height - h) / 2;
        g.fill(x - 3, y - 3, x + w + 3, y + h + 3, INK);
        g.fill(x, y, x + w, y + h, PAPER);
        g.fill(x, y, x + w, y + 1, 0xFF000000 | tribe.colour());
        g.fill(x + 6, y + 6, x + w - 6, y + h - 6, PAPER_DARK);
        g.fill(x + 7, y + 7, x + w - 7, y + h - 7, PAPER);
        g.drawCenteredString(font, title, x + w / 2, y + 12, 0xFF000000 | tribe.colour());
        g.fill(x + 24, y + 23, x + w - 24, y + 24, GOLD);
        int ly = y + 30, limit = y + h - 16 - choices.size() * 22;
        for (String key : open.lines()) {
            for (FormattedCharSequence line : font.split(Component.translatable(key), w - 32)) {
                if (ly > limit) break;
                g.drawString(font, line, x + 16, ly, INK, false);
                ly += 10;
            }
            ly += 4;
        }
    }

    @Override
    protected void renderBlurredBackground(float partial) {}
}
