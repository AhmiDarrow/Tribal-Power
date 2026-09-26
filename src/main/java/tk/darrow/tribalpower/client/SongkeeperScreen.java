package tk.darrow.tribalpower.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import tk.darrow.tribalpower.gate.DrumPractice;
import tk.darrow.tribalpower.gate.Songbook;
import tk.darrow.tribalpower.gate.Songbook.Difficulty;

/**
 * The Songkeeper Drum's song list: every song by album, four difficulties each, your bests and stars, the drum's
 * best five, the settings that keep the notes on the music, and (at a pair of drums) the duel.
 */
public class SongkeeperScreen extends Screen {
    private static final int INK = 0xF00B1216, PANEL = 0xFF101B22, RIM = 0xFFB58A58, TEXT = 0xFFF2E8CF, QUIET = 0xFF8FA7A0,
            TEAL = 0xFF5CE0C8, GOLD = 0xFFFFD36B, ROW_HOT = 0xFF1C3338, ROW_SEL = 0xFF24464C;
    private static final int[] DIFF_COLOUR = {0xFF6FD08C, 0xFF5CC8E0, 0xFFFFB84D, 0xFFFF6A5A};
    private static final int ROW = 20, HEADER = 16;

    private static SongkeeperScreen current;
    private final DrumPractice.Browse browse;
    private final Map<Integer, DrumPractice.Board> boards = new HashMap<>();
    private final List<Object> rows = new ArrayList<>();   // String album headers and Song rows
    private int selected;
    private Difficulty difficulty;
    private double scroll;
    private int panelX, panelY, panelW, panelH, listW;
    private final List<Button> rivalButtons = new ArrayList<>();
    private Button playButton;

    public SongkeeperScreen(DrumPractice.Browse browse) {
        super(Component.translatable("block.tribalpower.songkeeper_drum"));
        this.browse = browse;
        this.difficulty = Difficulty.of(SkyConfig.SONG_DIFFICULTY.get());
        Songbook.albums().forEach((album, songs) -> {
            rows.add(album);
            rows.addAll(songs);
        });
    }

    public static void open(DrumPractice.Browse browse) {
        current = new SongkeeperScreen(browse);
        Minecraft.getInstance().setScreen(current);
    }

    public static void board(DrumPractice.Board board) {
        if (current != null) current.boards.put(board.song() * 4 + board.difficulty(), board);
    }

    private Songbook.Song song() {
        return Songbook.song(selected);
    }

    private void askBoard() {
        int key = selected * 4 + difficulty.ordinal();
        if (!boards.containsKey(key)) {
            boards.put(key, null);
            PacketDistributor.sendToServer(new DrumPractice.BoardRequest(selected, difficulty.ordinal()));
        }
    }

    @Override
    public void removed() {
        if (current == this) current = null;
        super.removed();
    }

    @Override
    protected void init() {
        panelW = Math.min(620, width - 16);
        panelH = Math.min(350, height - 16);
        panelX = width / 2 - panelW / 2;
        panelY = height / 2 - panelH / 2;
        listW = (int) (panelW * 0.5);
        int rx = panelX + listW + 12, rw = panelW - listW - 22;
        playButton = addRenderableWidget(Button.builder(Component.translatable("gui.tribalpower.songkeeper.play"), b -> play())
                .bounds(rx, panelY + panelH - 28, rw / 2 - 2, 20).build());
        rivalButtons.clear();
        if (!browse.rivals().isEmpty()) {
            String name = browse.rivals().get(0);
            rivalButtons.add(addRenderableWidget(Button.builder(Component.translatable("gui.tribalpower.songkeeper.duel.challenge", name), b -> {
                PacketDistributor.sendToServer(new DrumPractice.Challenge(browse.pos(), name, selected, difficulty.ordinal()));
                onClose();
            }).bounds(rx + rw / 2 + 2, panelY + panelH - 28, rw / 2 - 2, 20).build()));
        }
        // settings: [-] Offset 0 ms [+]   [-] Speed 1.0 [+] on one row, hit sounds above it
        int sy = settingsY(), col = rw / 2;
        addRenderableWidget(Button.builder(Component.literal("-"), b -> nudgeOffset(-10)).bounds(rx, sy, 14, 14).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> nudgeOffset(10)).bounds(rx + col - 18, sy, 14, 14).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> nudgeSpeed(-0.1)).bounds(rx + col + 2, sy, 14, 14).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> nudgeSpeed(0.1)).bounds(rx + rw - 14, sy, 14, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.tribalpower.songkeeper.hit_sounds"), b -> {
            SkyConfig.SONG_HIT_SOUNDS.set(!SkyConfig.SONG_HIT_SOUNDS.get());
            SkyConfig.SPEC.save();
        }).bounds(rx, sy - 18, 90, 14).build());
        askBoard();
    }

    private int settingsY() {
        return panelY + panelH - 50;
    }

    private void nudgeOffset(int ms) {
        SkyConfig.SONG_OFFSET_MS.set(Mth.clamp(SkyConfig.SONG_OFFSET_MS.get() + ms, -300, 300));
        SkyConfig.SPEC.save();
    }

    private void nudgeSpeed(double by) {
        SkyConfig.SONG_SPEED.set(Math.round(Mth.clamp(SkyConfig.SONG_SPEED.get() + by, 0.5, 2.5) * 10) / 10.0);
        SkyConfig.SPEC.save();
    }

    private void play() {
        SkyConfig.SONG_DIFFICULTY.set(difficulty.ordinal());
        SkyConfig.SPEC.save();
        PacketDistributor.sendToServer(new DrumPractice.Play(browse.pos(), selected, difficulty.ordinal()));
        onClose();
    }

    private void setDifficulty(Difficulty to) {
        difficulty = to;
        askBoard();
    }

    private void select(int index) {
        selected = Mth.clamp(index, 0, Songbook.songs().size() - 1);
        askBoard();
        // keep the selection in view
        int y = rowY(selected);
        int top = panelY + 30, bottom = panelY + panelH - 10;
        if (y < top) scroll -= top - y;
        if (y + ROW > bottom) scroll += y + ROW - bottom;
    }

    private int rowY(int songIndex) {
        int y = panelY + 30 - (int) scroll;
        for (Object row : rows) {
            if (row instanceof Songbook.Song s && s.index() == songIndex) return y;
            y += row instanceof String ? HEADER : ROW;
        }
        return y;
    }

    private int contentHeight() {
        int h = 0;
        for (Object row : rows) h += row instanceof String ? HEADER : ROW;
        return h;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        if (mouseX < panelX + listW) {
            scroll = Mth.clamp(scroll - dy * ROW * 1.5, 0, Math.max(0, contentHeight() - (panelH - 40)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, dx, dy);
    }

    private long lastClick;
    private int lastClicked = -1;

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (mouseX >= panelX + 6 && mouseX < panelX + listW && mouseY >= panelY + 30 && mouseY < panelY + panelH - 8) {
            int y = panelY + 30 - (int) scroll;
            for (Object row : rows) {
                int h = row instanceof String ? HEADER : ROW;
                if (row instanceof Songbook.Song s && mouseY >= y && mouseY < y + h) {
                    long now = System.currentTimeMillis();
                    if (lastClicked == s.index() && now - lastClick < 350) play();
                    lastClick = now;
                    lastClicked = s.index();
                    select(s.index());
                    return true;
                }
                y += h;
            }
        }
        int tx = panelX + listW + 12, ty = panelY + 74, tw = (panelW - listW - 22) / 4;
        if (mouseY >= ty && mouseY < ty + 16 && mouseX >= tx && mouseX < tx + tw * 4) {
            setDifficulty(Difficulty.of((int) ((mouseX - tx) / tw)));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        switch (key) {
            case GLFW.GLFW_KEY_UP -> { select(selected - 1); return true; }
            case GLFW.GLFW_KEY_DOWN -> { select(selected + 1); return true; }
            case GLFW.GLFW_KEY_LEFT -> { setDifficulty(Difficulty.of(Math.max(0, difficulty.ordinal() - 1))); return true; }
            case GLFW.GLFW_KEY_RIGHT -> { setDifficulty(Difficulty.of(Math.min(3, difficulty.ordinal() + 1))); return true; }
            case GLFW.GLFW_KEY_ENTER -> { play(); return true; }
            default -> { return super.keyPressed(key, scan, modifiers); }
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, INK);
        g.renderOutline(panelX, panelY, panelW, panelH, RIM);
        g.fill(panelX + listW + 4, panelY + 26, panelX + listW + 5, panelY + panelH - 6, 0xFF2A3A40);
    }

    private static String clock(long ms) {
        long s = ms / 1000;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    private int best(int song, Difficulty d) {
        return browse.bests()[song * 4 + d.ordinal()];
    }

    private int starsOf(int song, Difficulty d) {
        return browse.stars()[song * 4 + d.ordinal()];
    }

    private void stars(GuiGraphics g, int x, int y, int packed) {
        if (packed < 0) return;
        boolean fc = packed >= 10;
        int n = packed % 10;
        for (int i = 0; i < 5; i++) g.drawString(font, "★", x + i * 7, y, i < n ? (fc ? 0xFFFFE070 : GOLD) : 0xFF33413F, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawString(font, title, panelX + 10, panelY + 9, GOLD, false);
        Component record = Component.translatable("gui.tribalpower.songkeeper.record", browse.won(), browse.lost());
        g.drawString(font, record, panelX + panelW - 10 - font.width(record), panelY + 9, QUIET, false);

        // the list
        g.enableScissor(panelX + 4, panelY + 26, panelX + listW, panelY + panelH - 6);
        int y = panelY + 30 - (int) scroll;
        for (Object row : rows) {
            if (row instanceof String album) {
                g.drawString(font, album, panelX + 10, y + 4, TEAL, false);
                y += HEADER;
                continue;
            }
            Songbook.Song s = (Songbook.Song) row;
            boolean hot = mouseX >= panelX + 6 && mouseX < panelX + listW && mouseY >= y && mouseY < y + ROW && mouseY >= panelY + 26;
            if (s.index() == selected) g.fill(panelX + 6, y, panelX + listW - 2, y + ROW - 2, ROW_SEL);
            else if (hot) g.fill(panelX + 6, y, panelX + listW - 2, y + ROW - 2, ROW_HOT);
            String length = clock(s.lengthMs());
            int right = panelX + listW - 8;
            boolean played = starsOf(s.index(), difficulty) >= 0;
            int room = right - font.width(length) - (played ? 42 : 6) - (panelX + 14);
            String name = font.width(s.title()) <= room ? s.title() : font.plainSubstrByWidth(s.title(), room - font.width("…")) + "…";
            g.drawString(font, name, panelX + 14, y + 5, TEXT, false);
            g.drawString(font, length, right - font.width(length), y + 5, QUIET, false);
            if (played) stars(g, right - font.width(length) - 40, y + 5, starsOf(s.index(), difficulty));
            y += ROW;
        }
        g.disableScissor();
        int total = contentHeight(), view = panelH - 36;
        if (total > view) {
            int barH = Math.max(20, view * view / total), barY = panelY + 30 + (int) ((view - barH) * scroll / (total - view));
            g.fill(panelX + listW - 1, barY, panelX + listW + 1, barY + barH, 0xFF4A5A60);
        }

        // the song
        Songbook.Song song = song();
        if (song == null) return;
        int rx = panelX + listW + 12, rw = panelW - listW - 22, ry = panelY + 30;
        g.pose().pushPose();
        g.pose().translate(rx, ry, 0);
        g.pose().scale(1.3F, 1.3F, 1);
        g.drawString(font, font.plainSubstrByWidth(song.title(), (int) (rw / 1.3F)), 0, 0, TEXT, false);
        g.pose().popPose();
        g.drawString(font, song.album(), rx, ry + 16, QUIET, false);
        Songbook.Chart chart = Songbook.chart(song, difficulty);
        double perSecond = chart.size() / Math.max(1.0, song.lengthMs() / 1000.0);
        g.drawString(font, Component.translatable("gui.tribalpower.songkeeper.about", clock(song.lengthMs()), song.bpm(), chart.size()),
                rx, ry + 28, QUIET, false);

        // difficulty tabs
        int tw = rw / 4, ty = panelY + 74;
        for (Difficulty d : Difficulty.values()) {
            int tx = rx + d.ordinal() * tw;
            boolean on = d == difficulty;
            g.fill(tx + 1, ty, tx + tw - 1, ty + 16, on ? DIFF_COLOUR[d.ordinal()] : 0xFF18252A);
            Component name = Component.translatable("gui.tribalpower.songkeeper.difficulty." + d.key());
            g.drawCenteredString(font, name, tx + tw / 2, ty + 4, on ? 0xFF0A1014 : DIFF_COLOUR[d.ordinal()]);
        }
        int pips = perSecond < 1.2 ? 1 : perSecond < 2.0 ? 2 : perSecond < 3.0 ? 3 : perSecond < 4.2 ? 4 : 5;
        g.drawString(font, Component.translatable("gui.tribalpower.songkeeper.intensity"), rx, ty + 22, QUIET, false);
        for (int i = 0; i < 5; i++) {
            int px = rx + font.width(Component.translatable("gui.tribalpower.songkeeper.intensity")) + 6 + i * 9;
            g.fill(px, ty + 22, px + 7, ty + 29, i < pips ? DIFF_COLOUR[difficulty.ordinal()] : 0xFF263238);
        }

        // your best
        int by = ty + 38;
        int mine = best(selected, difficulty);
        if (mine > 0) {
            g.drawString(font, Component.translatable("gui.tribalpower.songkeeper.your_best", String.format("%,d", mine)), rx, by, GOLD, false);
            stars(g, rx + rw - 36, by, starsOf(selected, difficulty));
            if (starsOf(selected, difficulty) >= 10)
                g.drawString(font, Component.translatable("gui.tribalpower.songkeeper.full_combo_small"), rx, by + 10, 0xFFFFE070, false);
        } else g.drawString(font, Component.translatable("gui.tribalpower.songkeeper.unplayed"), rx, by, QUIET, false);

        // the board
        int boardY = by + 26;
        g.drawString(font, Component.translatable("gui.tribalpower.songkeeper.board"), rx, boardY, TEAL, false);
        DrumPractice.Board board = boards.get(selected * 4 + difficulty.ordinal());
        if (board == null) g.drawString(font, "…", rx, boardY + 12, QUIET, false);
        else if (board.names().isEmpty()) g.drawString(font, Component.translatable("gui.tribalpower.songkeeper.nobody"), rx, boardY + 12, QUIET, false);
        else for (int i = 0; i < Math.min(board.names().size(), Math.max(1, (settingsY() - 22 - boardY - 12) / 10)); i++) {
            int ly = boardY + 12 + i * 10;
            g.drawString(font, (i + 1) + ".  " + board.names().get(i), rx, ly, i == 0 ? GOLD : TEXT, false);
            String points = String.format("%,d", board.points().get(i));
            g.drawString(font, points, rx + rw - font.width(points), ly, i == 0 ? GOLD : TEXT, false);
        }

        // settings
        int sy = settingsY(), col = rw / 2;
        g.drawCenteredString(font, Component.translatable("gui.tribalpower.songkeeper.offset", SkyConfig.SONG_OFFSET_MS.get()), rx + col / 2 - 2, sy + 3, QUIET);
        g.drawCenteredString(font, Component.translatable("gui.tribalpower.songkeeper.speed", String.format("%.1f", SkyConfig.SONG_SPEED.get())),
                rx + col + (rw - col) / 2 + 1, sy + 3, QUIET);
        g.drawString(font, Component.translatable(SkyConfig.SONG_HIT_SOUNDS.get() ? "options.on" : "options.off"), rx + 94, sy - 15, QUIET, false);

        // the duel
        if (rivalButtons.isEmpty()) {
            Component hint = Component.translatable(browse.paired() ? "gui.tribalpower.songkeeper.duel.nobody_there" : "gui.tribalpower.songkeeper.duel.pair_hint");
            var lines = font.split(hint, (int) ((rw / 2 - 6) / 0.75F));
            int hy = panelY + panelH - 28 + Math.max(0, (20 - lines.size() * 7) / 2);
            g.pose().pushPose();
            g.pose().translate(rx + rw / 2F + 6, hy, 0);
            g.pose().scale(0.75F, 0.75F, 1);
            for (int i = 0; i < Math.min(3, lines.size()); i++) g.drawString(font, lines.get(i), 0, i * 9, 0xFF667A74, false);
            g.pose().popPose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
