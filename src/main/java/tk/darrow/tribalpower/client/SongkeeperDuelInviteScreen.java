package tk.darrow.tribalpower.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import tk.darrow.tribalpower.gate.DrumPractice;
import tk.darrow.tribalpower.gate.Songbook;

/** Someone at the partner drum wants a duel: the song, the difficulty, and thirty seconds to say yes. */
public class SongkeeperDuelInviteScreen extends Screen {
    private static final long WAIT_MS = 30_000;
    private final DrumPractice.Invite invite;
    private final Screen behind;
    private final long opened = System.currentTimeMillis();
    private boolean answered;

    public SongkeeperDuelInviteScreen(DrumPractice.Invite invite, Screen behind) {
        super(Component.translatable("gui.tribalpower.songkeeper.duel.invite_title"));
        this.invite = invite;
        this.behind = behind instanceof SongkeeperDuelInviteScreen ? null : behind;
    }

    private void answer(boolean yes) {
        if (answered) return;
        answered = true;
        PacketDistributor.sendToServer(new DrumPractice.Answer(invite.duel(), yes));
        if (!yes) minecraft.setScreen(behind);
    }

    @Override
    protected void init() {
        int y = height / 2 + 26;
        addRenderableWidget(Button.builder(Component.translatable("gui.tribalpower.songkeeper.duel.accept"), b -> answer(true))
                .bounds(width / 2 - 104, y, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.tribalpower.songkeeper.duel.decline"), b -> answer(false))
                .bounds(width / 2 + 4, y, 100, 20).build());
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_Y || key == GLFW.GLFW_KEY_ENTER) { answer(true); return true; }
        if (key == GLFW.GLFW_KEY_N || key == GLFW.GLFW_KEY_ESCAPE) { answer(false); return true; }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void tick() {
        if (!answered && System.currentTimeMillis() - opened > WAIT_MS) answer(false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int w = 260, h = 96, x = width / 2 - w / 2, y = height / 2 - 60;
        Songbook.Song song = Songbook.song(invite.song());
        g.drawCenteredString(font, Component.translatable("gui.tribalpower.songkeeper.duel.invite", invite.from()), width / 2, y + 10, 0xFFFFD36B);
        if (song != null) {
            g.drawCenteredString(font, song.title(), width / 2, y + 28, 0xFFF2E8CF);
            long s = song.lengthMs() / 1000;
            g.drawCenteredString(font, Component.literal(song.album() + "  ·  ").append(Component.translatable(
                    "gui.tribalpower.songkeeper.difficulty." + Songbook.Difficulty.of(invite.difficulty()).key()))
                    .append(String.format("  ·  %d:%02d", s / 60, s % 60)), width / 2, y + 42, 0xFF8FA7A0);
        }
        long left = Math.max(0, (WAIT_MS - (System.currentTimeMillis() - opened)) / 1000);
        g.drawCenteredString(font, Component.translatable("gui.tribalpower.songkeeper.duel.invite_keys", left), width / 2, y + h + 34, 0xFF667A74);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        int w = 260, h = 96, x = width / 2 - w / 2, y = height / 2 - 60;
        g.fill(x, y, x + w, y + h + 50, 0xF00C1418);
        g.renderOutline(x, y, w, h + 50, 0xFFFFD36B);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
