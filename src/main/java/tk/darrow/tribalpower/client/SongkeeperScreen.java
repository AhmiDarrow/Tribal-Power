package tk.darrow.tribalpower.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import tk.darrow.tribalpower.gate.DrumPractice;

/** The Songkeeper Drum's track list: every track, how hard it is, your best, and the drum's best five. */
public class SongkeeperScreen extends Screen {
    private static final int INK = 0xFF101B22, RIM = 0xFFB58A58, TEXT = 0xFFE7DCC1, QUIET = 0xFF98ACA5, TEAL = 0xFF65D7C0, GOLD = 0xFFE3B55A;
    private static final int ROW = 26, WIDTH = 340;
    private final BlockPos pos;
    private final List<DrumPractice.TrackScore> tracks;
    private int selected;

    public SongkeeperScreen(BlockPos pos, List<DrumPractice.TrackScore> tracks) {
        super(Component.translatable("block.tribalpower.songkeeper_drum"));
        this.pos = pos;
        this.tracks = tracks;
    }

    public static void open(BlockPos pos, List<DrumPractice.TrackScore> tracks) {
        Minecraft.getInstance().setScreen(new SongkeeperScreen(pos, tracks));
    }

    private int left() { return width / 2 - WIDTH / 2; }
    private int top() { return height / 2 - (tracks.size() * ROW + 84) / 2; }

    @Override
    protected void init() {
        for (int i = 0; i < tracks.size(); i++) {
            int track = tracks.get(i).track();
            addRenderableWidget(Button.builder(Component.translatable("gui.tribalpower.practice.play"), b -> {
                PacketDistributor.sendToServer(new DrumPractice.Play(pos, track));
                onClose();
            }).bounds(left() + WIDTH - 50, top() + 28 + i * ROW, 44, 18).build());
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x = left(), y = top(), h = tracks.size() * ROW + 84;
        if (tracks.isEmpty()) return;
        g.drawCenteredString(font, title, width / 2, y + 8, TEXT);
        for (int i = 0; i < tracks.size(); i++) {
            var row = tracks.get(i);
            var pattern = DrumPractice.pattern(row.track());
            int ry = y + 26 + i * ROW;
            boolean hot = mouseY >= ry && mouseY < ry + ROW && mouseX >= x && mouseX < x + WIDTH;
            if (hot) selected = i;
            if (i == selected) g.fill(x + 4, ry, x + WIDTH - 4, ry + ROW - 2, 0xFF1C3338);
            g.drawString(font, Component.translatable("gui.tribalpower.practice.track." + row.track()), x + 10, ry + 3, TEXT, false);
            g.drawString(font, Component.translatable("gui.tribalpower.practice.about", pattern.bpm(), pattern.notes().size(),
                    (pattern.endMs() + 999) / 1000), x + 10, ry + 14, QUIET, false);
            Component best = row.best() > 0 ? Component.translatable("gui.tribalpower.practice.your_best", row.best(), row.accuracy())
                    : Component.translatable("gui.tribalpower.practice.unplayed");
            g.drawString(font, best, x + WIDTH - 60 - font.width(best), ry + 8, row.best() > 0 ? GOLD : QUIET, false);
        }
        // The best five for the track under the mouse.
        var row = tracks.get(Math.min(selected, tracks.size() - 1));
        int by = y + 30 + tracks.size() * ROW;
        g.drawString(font, Component.translatable("gui.tribalpower.practice.board"), x + 10, by, TEAL, false);
        if (row.names().isEmpty()) g.drawString(font, Component.translatable("gui.tribalpower.practice.nobody"), x + 10, by + 12, TEXT, false);
        for (int i = 0; i < row.names().size(); i++) {
            // Two columns of the five: three left, two right.
            int col = i < 3 ? 0 : 1, line = i < 3 ? i : i - 3;
            g.drawString(font, Component.literal((i + 1) + ". " + row.names().get(i) + "  " + row.points().get(i)),
                    x + 10 + col * (WIDTH / 2), by + 12 + line * 10, TEXT, false);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        int x = left(), y = top(), h = tracks.size() * ROW + 84;
        g.fill(x, y, x + WIDTH, y + h, INK);
        g.renderOutline(x, y, WIDTH, h, RIM);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
