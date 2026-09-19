package tk.darrow.tribalpower.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import tk.darrow.tribalpower.gate.DrumRite;
import tk.darrow.tribalpower.sound.ModSounds;

/**
 * The Gate Rite on screen: four drums in a row (A, S, D, F; the arrow keys work too), beats falling toward the strike
 * line in time with the rite's music (its drums are the beats), a count-in, and a resonance meter.
 * Timing is measured here, so network lag never costs a beat; the world keeps running while you drum.
 */
public class DrumRiteScreen extends Screen {
    private static final long GOOD_MS = 180, PERFECT_MS = 80, FALL_MS = 1800, RESULT_MS = 2600;
    private static final int LANE_WIDTH = 40, FIELD_HEIGHT = 200;
    private static final String[] KEYS = {"A", "S", "D", "F"};
    private static final int[] COLOURS = {0xFF5CE0C8, 0xFFFFC857, 0xFFC08CFF, 0xFFFF8A4C};

    private final BlockPos pos;
    private final long seed;
    private final DrumRite.Pattern pattern;
    private final boolean[] judged;
    private final boolean[] landed;
    private final long[] flash = new long[4];
    private long start = -1;
    private int hits, perfects, strays, misses, combo, bestCombo;
    private String feedback = "";
    private int feedbackColour;
    private long feedbackAt = -9999, sentAt = -1, requested = -1;
    private SimpleSoundInstance music;

    public DrumRiteScreen(BlockPos pos, long seed) {
        super(Component.translatable("gui.tribalpower.rite.title"));
        this.pos = pos;
        this.seed = seed;
        this.pattern = DrumRite.pattern(seed);
        this.judged = new boolean[pattern.notes().size()];
        this.landed = new boolean[pattern.notes().size()];
    }

    public static void open(BlockPos pos, long seed) {
        Minecraft.getInstance().setScreen(new DrumRiteScreen(pos, seed));
    }

    @Override
    protected void init() {
        if (music != null) return;
        var sound = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tribalpower", pattern.sound()));
        if (sound != null) {
            music = SimpleSoundInstance.forUI(sound, 1.0F, 1.0F);
            Minecraft.getInstance().getSoundManager().play(music);
        }
        requested = System.nanoTime();
    }

    /** The rite's clock starts when the music is actually heard, so the beats line up with the drums. */
    private long now() {
        if (start < 0) {
            boolean playing = music != null && Minecraft.getInstance().getSoundManager().isActive(music);
            if (playing || System.nanoTime() - requested > 1_500_000_000L) start = System.nanoTime();
            else return -1;
        }
        return (System.nanoTime() - start) / 1_000_000;
    }

    @Override
    public void removed() {
        if (music != null) Minecraft.getInstance().getSoundManager().stop(music);
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private void play(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (sentAt < 0) send(true);
            onClose();
            return true;
        }
        int lane = switch (key) {
            case GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT -> 0;
            case GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN -> 1;
            case GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_UP -> 2;
            case GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_RIGHT -> 3;
            default -> -1;
        };
        if (lane >= 0 && sentAt < 0 && now() > pattern.leadMs() - GOOD_MS) strike(lane);
        return true;
    }

    private void strike(int lane) {
        long t = now();
        flash[lane] = t;
        List<DrumRite.Note> notes = pattern.notes();
        int best = -1;
        long bestGap = Long.MAX_VALUE;
        for (int i = 0; i < notes.size(); i++) {
            if (judged[i] || notes.get(i).lane() != lane) continue;
            long gap = Math.abs(notes.get(i).timeMs() - t);
            if (gap <= GOOD_MS && gap < bestGap) {
                best = i;
                bestGap = gap;
            }
        }
        if (best < 0) {
            strays++;
            combo = 0;
            say("gui.tribalpower.rite.stray", 0xFFFF6A6A);
            play(ModSounds.DRUMHEART_OFF_TEMPO.get(), 1.0F, 0.3F);
            return;
        }
        judged[best] = true;
        landed[best] = true;
        hits++;
        combo++;
        bestCombo = Math.max(bestCombo, combo);
        boolean perfect = bestGap <= PERFECT_MS;
        if (perfect) perfects++;
        // The music already sounds the drum; the lane lights up instead.
        say(perfect ? "gui.tribalpower.rite.perfect" : "gui.tribalpower.rite.good", perfect ? 0xFFFFE08A : 0xFF9CFFE0);
    }

    private void say(String key, int colour) {
        feedback = key;
        feedbackColour = colour;
        feedbackAt = now();
    }

    @Override
    public void tick() {
        long t = now();
        if (t < 0) return;
        List<DrumRite.Note> notes = pattern.notes();
        for (int i = 0; i < notes.size(); i++) {
            if (!judged[i] && t - notes.get(i).timeMs() > GOOD_MS) {
                judged[i] = true;
                misses++;
                combo = 0;
                say("gui.tribalpower.rite.miss", 0xFFB0B0B0);
            }
        }
        if (sentAt < 0 && t > pattern.endMs() + GOOD_MS + 150) send(false);
        if (sentAt >= 0 && t > sentAt + RESULT_MS) onClose();
    }

    private void send(boolean cancelled) {
        sentAt = now();
        PacketDistributor.sendToServer(new DrumRite.Result(pos, seed, hits, strays, cancelled));
    }

    private double resonance() {
        int judgedCount = hits + misses;
        return judgedCount == 0 ? 1 : DrumRite.accuracy(hits, strays, judgedCount);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x88060A10);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        long t = Math.max(0, now());
        int fieldWidth = LANE_WIDTH * 4;
        int left = width / 2 - fieldWidth / 2, top = height / 2 - FIELD_HEIGHT / 2 - 6;
        int strikeY = top + FIELD_HEIGHT - 24;
        double resonance = resonance();
        int total = pattern.notes().size();
        double finalScore = DrumRite.accuracy(hits, strays, total);

        // The gate's glow grows with the resonance.
        int glow = (int) (Mth.clamp(resonance, 0, 1) * 150);
        for (int r = 0; r < 4; r++) {
            int pad = 6 + r * 5;
            g.renderOutline(left - pad, top - pad, fieldWidth + pad * 2, FIELD_HEIGHT + pad * 2, (Math.max(0, glow - r * 35) << 24) | 0x5CE0C8);
        }
        g.fill(left, top, left + fieldWidth, top + FIELD_HEIGHT, 0xE00C141A);
        for (int lane = 0; lane < 4; lane++) {
            int x = left + lane * LANE_WIDTH;
            g.fill(x + LANE_WIDTH - 1, top, x + LANE_WIDTH, top + FIELD_HEIGHT, 0xFF1C2A30);
            float lit = Mth.clamp(1 - (t - flash[lane]) / 160F, 0, 1);
            if (lit > 0) g.fill(x + 1, top, x + LANE_WIDTH - 1, strikeY, ((int) (lit * 40) << 24) | (COLOURS[lane] & 0xFFFFFF));
        }
        // Falling beats.
        List<DrumRite.Note> notes = pattern.notes();
        for (int i = 0; i < notes.size(); i++) {
            if (landed[i]) continue;
            long ahead = notes.get(i).timeMs() - t;
            if (ahead > FALL_MS || ahead < -300) continue;
            int y = strikeY - (int) (ahead * (strikeY - top) / FALL_MS);
            int x = left + notes.get(i).lane() * LANE_WIDTH + 5;
            int colour = judged[i] ? 0x88FF5050 : COLOURS[notes.get(i).lane()];
            g.fill(x, y - 5, x + LANE_WIDTH - 10, y + 5, colour);
            g.fill(x + 4, y - 2, x + LANE_WIDTH - 14, y + 2, judged[i] ? 0x66FFFFFF : 0xFFFFFFFF);
        }
        // The strike line and the four drums.
        g.fill(left, strikeY - 1, left + fieldWidth, strikeY + 1, 0xFFE7DCC1);
        for (int lane = 0; lane < 4; lane++) {
            int x = left + lane * LANE_WIDTH;
            boolean hot = t - flash[lane] < 120;
            g.fill(x + 6, strikeY + 6, x + LANE_WIDTH - 6, strikeY + 20, hot ? COLOURS[lane] : 0xFF24343A);
            g.drawCenteredString(font, KEYS[lane], x + LANE_WIDTH / 2, strikeY + 9, hot ? 0xFF0A1014 : COLOURS[lane]);
        }

        // Header: title, resonance against the mark needed, combo and time left.
        g.drawCenteredString(font, title, width / 2, top - 40, 0xFFE7DCC1);
        int barLeft = left - 20, barWidth = fieldWidth + 40, barY = top - 26;
        g.fill(barLeft, barY, barLeft + barWidth, barY + 6, 0xFF1C2A30);
        g.fill(barLeft, barY, barLeft + (int) (barWidth * Mth.clamp(resonance, 0, 1)), barY + 6,
                resonance >= DrumRite.PASS ? 0xFF5CE0C8 : 0xFFE0A050);
        int mark = barLeft + (int) (barWidth * DrumRite.PASS);
        g.fill(mark, barY - 2, mark + 1, barY + 8, 0xFFFFFFFF);
        g.drawString(font, Component.translatable("gui.tribalpower.rite.resonance", (int) Math.round(resonance * 100)), barLeft, barY - 11, 0xFF99C9BD, false);
        long left_ms = Math.max(0, pattern.endMs() - t);
        var clock = Component.translatable("gui.tribalpower.rite.time", (left_ms + 999) / 1000);
        g.drawString(font, clock, barLeft + barWidth - font.width(clock), barY - 11, 0xFF99C9BD, false);
        if (combo > 1) g.drawCenteredString(font, Component.translatable("gui.tribalpower.rite.combo", combo), width / 2, top + 8, 0xFFE7DCC1);

        if (t < pattern.leadMs()) {
            long beatsLeft = (pattern.leadMs() - t) / pattern.beatMs() + 1;
            g.pose().pushPose();
            g.pose().translate(width / 2F, top + FIELD_HEIGHT / 2F - 20, 0);
            g.pose().scale(3, 3, 1);
            g.drawCenteredString(font, beatsLeft > 4 ? "" : String.valueOf(Math.min(4, beatsLeft)), 0, 0, 0xFFE7DCC1);
            g.pose().popPose();
            g.drawCenteredString(font, Component.translatable("gui.tribalpower.rite.ready"), width / 2, top + FIELD_HEIGHT / 2 + 20, 0xFF99C9BD);
        }
        if (t - feedbackAt < 450 && sentAt < 0)
            g.drawCenteredString(font, Component.translatable(feedback), width / 2, strikeY - 40, feedbackColour);

        if (sentAt >= 0) {
            boolean opened = finalScore >= DrumRite.PASS;
            g.fill(left, top + FIELD_HEIGHT / 2 - 28, left + fieldWidth, top + FIELD_HEIGHT / 2 + 28, 0xEE0A1014);
            g.drawCenteredString(font, Component.translatable(opened ? "gui.tribalpower.rite.opened" : "gui.tribalpower.rite.broken"),
                    width / 2, top + FIELD_HEIGHT / 2 - 18, opened ? 0xFF5CE0C8 : 0xFFFF8A6A);
            g.drawCenteredString(font, Component.translatable("gui.tribalpower.rite.score", (int) Math.round(finalScore * 100), perfects, bestCombo),
                    width / 2, top + FIELD_HEIGHT / 2 + 2, 0xFFE7DCC1);
        }
        g.drawCenteredString(font, Component.translatable("gui.tribalpower.rite.hint"), width / 2, top + FIELD_HEIGHT + 12, 0xFF667A74);
    }
}
