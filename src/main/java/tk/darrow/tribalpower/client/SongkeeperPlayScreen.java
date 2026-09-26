package tk.darrow.tribalpower.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import tk.darrow.tribalpower.gate.DrumPractice;
import tk.darrow.tribalpower.gate.Songbook;
import tk.darrow.tribalpower.gate.Songbook.Difficulty;

/**
 * The Songkeeper Drum's game: a whole song, start to finish, on four drums.
 *
 * <p>Notes ride a highway toward four drum heads and are struck with A, S, D and F (or the arrows). The clock is the
 * song's own: it counts from the instant the sound engine starts the recording ({@link SongClock}), less the
 * player's calibration, and every note in the chart sits on an attack in that recording. A streak builds a multiplier
 * up to x4; clearing a glowing phrase charges Spirit Surge, which Space unleashes for double points; the Resonance
 * meter falls with every miss, and when it empties the song breaks (never on Easy). A missed note lets the song
 * dip, as a band does when its drummer drops a beat.
 *
 * <p>In a duel the other drum's score, streak and meter ride alongside, with a tug-of-war bar across the top.
 */
public class SongkeeperPlayScreen extends Screen {
    // timing windows either side of a note
    private static final long PERFECT_MS = 45, GREAT_MS = 90, GOOD_MS = 135;
    private static final long VIEW_MS = 1700;
    private static final int[] LANE = {0xFF3FD8C0, 0xFFFFC34D, 0xFFB57CFF, 0xFFFF7A45};
    private static final int[] LANE_DARK = {0xFF16695E, 0xFF8A5F10, 0xFF4E2A8A, 0xFF8A2E10};
    private static final String[] KEYS = {"A", "S", "D", "F"};
    private static final int INK = 0xFF0B1216, TEXT = 0xFFF2E8CF, QUIET = 0xFF8FA7A0, GOLD = 0xFFFFD36B, TEAL = 0xFF5CE0C8, RED = 0xFFFF6A5A;

    private final BlockPos pos;
    private final Songbook.Song song;
    private final Difficulty difficulty;
    private final Songbook.Chart chart;
    private final Songbook.Charts charts;
    private final int duel;
    private final String opponent;
    /** At a Gate Drum: the rite's seed. The same game, one fixed track, no scores kept; the accuracy opens the way. */
    private final boolean rite;
    private final long riteSeed;
    private final byte[] state;          // 0 waiting, 1 hit, 2 missed
    private final byte[] grade;          // 0 perfect, 1 great, 2 good
    private final boolean[] surgeNote;
    private final boolean[] surgeWindowClean, surgeWindowSettled;
    private final int[] surgeWindowOf;
    private final long offset;
    private final double speed;
    private final boolean hitSounds;

    private SongSound music;
    private long requested = -1, fallbackStart = -1;
    private int first;                    // first note still worth looking at
    private long score;
    private int streak, bestStreak, hits, perfects, greats, goods, misses, strays;
    private double resonance = 0.6, surge, surgeActiveUntil = -1;
    private boolean failed, done, leaving;
    private long failedAt = -1, doneAt = -1, leaveAsk = -9999, lastProgress = -9999;
    private final long[] padAt = {-9999, -9999, -9999, -9999};
    private final boolean[] padDown = new boolean[4];
    private final List<Spark> sparks = new ArrayList<>();
    private final List<Popup> popups = new ArrayList<>();
    private long calloutAt = -9999;
    private String callout = "";
    private long lastFrame = -1;
    private final RandomSource random = RandomSource.create();

    /** Set only by the development screenshot driver: strike every note dead on, to exercise the whole game. */
    public static boolean autoplay;

    // duel
    private static SongkeeperPlayScreen current;
    private DrumPractice.Rival rival;
    private DrumPractice.Outcome outcome;

    private record Spark(float[] p, int colour, long born, long life) {}
    private record Popup(int lane, String text, int colour, long at) {}

    public SongkeeperPlayScreen(BlockPos pos, Songbook.Song song, Difficulty difficulty, int duel, String opponent) {
        this(pos, song, difficulty, Songbook.charts(song).chart(difficulty), duel, opponent, false, 0);
    }

    private SongkeeperPlayScreen(BlockPos pos, Songbook.Song song, Difficulty difficulty, Songbook.Chart chart, int duel,
                                 String opponent, boolean rite, long riteSeed) {
        super(rite ? Component.translatable("gui.tribalpower.rite.title") : Component.literal(song.title()));
        this.pos = pos;
        this.song = song;
        this.difficulty = difficulty;
        this.charts = Songbook.charts(song);
        this.chart = chart;
        this.duel = duel;
        this.opponent = opponent;
        this.rite = rite;
        this.riteSeed = riteSeed;
        int n = chart.size();
        state = new byte[n];
        grade = new byte[n];
        surgeNote = new boolean[n];
        surgeWindowOf = new int[n];
        surgeWindowClean = new boolean[charts.surge().length];
        surgeWindowSettled = new boolean[charts.surge().length];
        java.util.Arrays.fill(surgeWindowClean, true);
        for (int i = 0; i < n; i++) {
            surgeWindowOf[i] = -1;
            if (rite) continue;
            for (int w = 0; w < charts.surge().length; w++) {
                long[] window = charts.surge()[w];
                if (chart.times()[i] >= window[0] && chart.times()[i] < window[1]) {
                    surgeNote[i] = true;
                    surgeWindowOf[i] = w;
                }
            }
        }
        offset = SkyConfig.SONG_OFFSET_MS.get();
        speed = SkyConfig.SONG_SPEED.get();
        hitSounds = SkyConfig.SONG_HIT_SOUNDS.get();
    }

    // ---- network entry points ------------------------------------------------------------------------------------

    public static void start(DrumPractice.Start start) {
        Songbook.Song song = Songbook.song(start.song());
        if (song == null) return;
        current = new SongkeeperPlayScreen(start.pos(), song, Difficulty.of(start.difficulty()), start.duel(), start.opponent());
        Minecraft.getInstance().setScreen(current);
    }

    /**
     * A Gate Drum's rite: the track the seed names, charted from the drum hits it was composed with (the very notes the
     * server checks), played on the Songkeeper's highway. No song list, no scores kept, and it never breaks partway:
     * the accuracy at the end decides whether the way to the March opens.
     */
    public static void rite(BlockPos pos, long seed) {
        tk.darrow.tribalpower.gate.DrumRite.Pattern pattern = tk.darrow.tribalpower.gate.DrumRite.pattern(seed);
        Songbook.Song song = null;
        for (Songbook.Song candidate : Songbook.songs()) if (candidate.sound().equals("tribalpower:" + pattern.sound())) song = candidate;
        if (song == null) return;
        long[] times = new long[pattern.notes().size()];
        byte[] lanes = new byte[times.length];
        for (int i = 0; i < times.length; i++) {
            times[i] = pattern.notes().get(i).timeMs();
            lanes[i] = (byte) pattern.notes().get(i).lane();
        }
        current = new SongkeeperPlayScreen(pos, song, Difficulty.HARD, new Songbook.Chart(times, lanes), 0, "", true, seed);
        Minecraft.getInstance().setScreen(current);
    }

    public static void invite(DrumPractice.Invite invite) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof SongkeeperPlayScreen) return;
        mc.setScreen(new SongkeeperDuelInviteScreen(invite, mc.screen));
    }

    public static void rival(DrumPractice.Rival rival) {
        if (current != null && current.duel == rival.duel()) current.rival = rival;
    }

    public static void outcome(DrumPractice.Outcome outcome) {
        if (current != null && current.duel == outcome.duel()) current.outcome = outcome;
        else {
            var mc = Minecraft.getInstance();
            if (mc.player != null) mc.player.displayClientMessage(Component.translatable(outcome.result() > 0 ? "gui.tribalpower.songkeeper.duel.win"
                    : outcome.result() < 0 ? "gui.tribalpower.songkeeper.duel.loss" : "gui.tribalpower.songkeeper.duel.draw"), true);
        }
    }

    // ---- the music ------------------------------------------------------------------------------------------------

    /** The song, playing flat in both ears, with a volume and pitch the game can bend while it plays. */
    private static final class SongSound extends AbstractTickableSoundInstance {
        float targetVolume = 1, targetPitch = 1;

        SongSound(SoundEvent event) {
            super(event, SoundSource.MASTER, SoundInstance.createUnseededRandom());
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = 1;
            this.looping = false;
        }

        @Override
        public void tick() {
            volume += (targetVolume - volume) * 0.35F;
            pitch += (targetPitch - pitch) * 0.12F;
        }

        void end() {
            stop();
        }
    }

    @Override
    protected void init() {
        if (music != null) return;
        Minecraft mc = Minecraft.getInstance();
        mc.getMusicManager().stopPlaying();
        // every song is a sounds.json event; the imported ones are not registry entries, so make the event from its name
        ResourceLocation id = ResourceLocation.parse(song.sound());
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.getOptional(id).orElseGet(() -> SoundEvent.createVariableRangeEvent(id));
        {
            music = new SongSound(event);
            SongClock.watch(music);
            mc.getSoundManager().play(music);
        }
        requested = System.nanoTime();
    }

    /** Milliseconds into the song, as the ears hear it; negative until the recording starts. */
    private long now() {
        long started = SongClock.started();
        if (started < 0) {
            // no sound at all (muted master volume never starts a source): run on the wall clock after a beat
            if (System.nanoTime() - requested > 2_500_000_000L) {
                if (fallbackStart < 0) fallbackStart = System.nanoTime();
                started = fallbackStart;
            } else return -1;
        }
        return (System.nanoTime() - started) / 1_000_000 - offset;
    }

    @Override
    public void removed() {
        if (music != null) music.end();
        if (current == this) current = null;
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

    // ---- input and judging ----------------------------------------------------------------------------------------

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (done) {
            if (key == GLFW.GLFW_KEY_ENTER && duel == 0 && !rite) {
                PacketDistributor.sendToServer(new DrumPractice.Play(pos, song.index(), difficulty.ordinal()));
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE) onClose();
            return true;
        }
        long t = now();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (t - leaveAsk < 2500 || failed) {
                leaving = true;
                send(!failed);
                onClose();
            } else leaveAsk = t;
            return true;
        }
        if (failed || t < 0) return true;
        if (key == GLFW.GLFW_KEY_SPACE) {
            unleash(t);
            return true;
        }
        int lane = TribalKeys.riteLane(key, scan);
        if (lane >= 0 && !padDown[lane]) {
            padDown[lane] = true;
            strike(lane, t);
        }
        return true;
    }

    @Override
    public boolean keyReleased(int key, int scan, int modifiers) {
        int lane = TribalKeys.riteLane(key, scan);
        if (lane >= 0) padDown[lane] = false;
        return true;
    }

    private boolean surging(long t) {
        return t < surgeActiveUntil;
    }

    private void unleash(long t) {
        if (rite || surging(t) || surge < 0.5) return;
        // a full bar lasts sixteen seconds, half a bar eight
        surgeActiveUntil = t + (long) (surge * 16000);
        surge = 0;
        callout = Component.translatable("gui.tribalpower.songkeeper.surge_on").getString();
        calloutAt = t;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(tk.darrow.tribalpower.sound.ModSounds.WIND_CHARM_GUST.get(), 1.0F, 0.9F));
    }

    private void strike(int lane, long t) {
        padAt[lane] = t;
        int best = -1;
        long bestGap = Long.MAX_VALUE;
        for (int i = first; i < chart.size(); i++) {
            long at = chart.times()[i];
            if (at - t > GOOD_MS) break;
            if (state[i] != 0 || chart.lanes()[i] != lane) continue;
            long gap = Math.abs(at - t);
            if (gap <= GOOD_MS && gap < bestGap) {
                best = i;
                bestGap = gap;
            }
        }
        if (best < 0) {
            strays++;
            resonance = Math.max(0, resonance - 0.006);
            if (rite) riteMeter();
            if (hitSounds) tap(0.6F, 0.25F);
            return;
        }
        state[best] = 1;
        int g = bestGap <= PERFECT_MS ? 0 : bestGap <= GREAT_MS ? 1 : 2;
        grade[best] = (byte) g;
        hits++;
        if (g == 0) perfects++;
        else if (g == 1) greats++;
        else goods++;
        int base = g == 0 ? Songbook.PERFECT : g == 1 ? Songbook.GREAT : Songbook.GOOD;
        score += (long) base * Songbook.multiplier(streak) * (surging(t) ? 2 : 1);
        streak++;
        bestStreak = Math.max(bestStreak, streak);
        resonance = Math.min(1, resonance + (g == 0 ? 0.02 : 0.013) * (surging(t) ? 2 : 1));
        if (streak % 50 == 0 && !rite) {
            callout = Component.translatable("gui.tribalpower.songkeeper.streak", streak).getString();
            calloutAt = t;
        }
        popups.add(new Popup(lane, Component.translatable("gui.tribalpower.songkeeper.grade." + g).getString(), g == 0 ? GOLD : g == 1 ? TEAL : TEXT, t));
        burst(lane, g == 0 ? 14 : 8, g == 0 ? GOLD : LANE[lane], t);
        if (hitSounds) tap(1.0F + lane * 0.12F, 0.35F);
        if (music != null) music.targetVolume = 1;
        if (rite) riteMeter();
    }

    private void tap(float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(tk.darrow.tribalpower.sound.ModSounds.WAVE_DRUM_SLAP.get(), pitch, volume));
    }

    private void miss(int i, long t) {
        state[i] = 2;
        misses++;
        streak = 0;
        if (surgeWindowOf[i] >= 0) surgeWindowClean[surgeWindowOf[i]] = false;
        // about fifteen misses in a row empties a full-strength meter on Hard; the first eight seconds never break
        double loss = difficulty == Difficulty.EASY ? 0.025 : difficulty == Difficulty.NORMAL ? 0.032 : difficulty == Difficulty.HARD ? 0.04 : 0.048;
        resonance = Math.max(t < 8000 ? 0.05 : 0, resonance - loss);
        if (music != null && !failed) music.targetVolume = 0.45F;
        popups.add(new Popup(chart.lanes()[i], Component.translatable("gui.tribalpower.songkeeper.grade.miss").getString(), RED, t));
        if (rite) riteMeter();
        else if (resonance <= 0 && difficulty != Difficulty.EASY) fail(t);
    }

    /** At a Gate Drum the meter is the rite's own measure: notes landed, less a quarter per stray, over notes so far. */
    private void riteMeter() {
        int judged = hits + misses;
        resonance = judged == 0 ? 1 : tk.darrow.tribalpower.gate.DrumRite.accuracy(hits, strays, judged);
    }

    private void fail(long t) {
        if (failed) return;
        failed = true;
        failedAt = t;
        if (music != null) {
            music.targetVolume = 0;
            music.targetPitch = 0.5F;
        }
        send(false);
    }

    @Override
    public void tick() {
        long t = now();
        if (t < 0) return;
        if (music != null && music.targetVolume < 1 && !failed) music.targetVolume = Math.min(1, music.targetVolume + 0.12F);
        if (!failed && !done) {
            while (first < chart.size() && state[first] != 0) first++;
            for (int i = first; i < chart.size(); i++) {
                long at = chart.times()[i];
                if (at > t - GOOD_MS) break;
                if (state[i] == 0) miss(i, t);
            }
            // a phrase that ends clean charges a quarter of Spirit Surge
            for (int w = 0; w < charts.surge().length; w++) {
                long end = charts.surge()[w][1];
                if (!surgeWindowSettled[w] && t > end + GOOD_MS) {
                    surgeWindowSettled[w] = true;
                    boolean any = false;
                    for (int i = 0; i < chart.size(); i++) if (surgeWindowOf[i] == w) { any = true; if (state[i] != 1) { any = false; break; } }
                    if (any && surgeWindowClean[w] && !surging(t)) {
                        surge = Math.min(1, surge + 0.25);
                        callout = Component.translatable(surge >= 0.5 ? "gui.tribalpower.songkeeper.surge_ready" : "gui.tribalpower.songkeeper.surge_charged").getString();
                        calloutAt = t;
                    }
                }
            }
            boolean ended = t >= song.lengthMs() - 300
                    || (music != null && SongClock.started() > 0 && t > 3000 && !Minecraft.getInstance().getSoundManager().isActive(music));
            if (ended && (chart.size() == 0 || t > chart.times()[chart.size() - 1] + GOOD_MS)) {
                done = true;
                doneAt = t;
                send(false);
            }
        }
        if (duel != 0 && t - lastProgress > 250) {
            lastProgress = t;
            PacketDistributor.sendToServer(new DrumPractice.Progress(duel, score, streak, Songbook.multiplier(streak),
                    (int) Math.round(resonance * 100), hits, failed));
        }
        if (rite && done && t - doneAt > 3200) onClose();
        if (failed && !done && t - failedAt > 2200) {
            done = true;
            doneAt = t;
            if (music != null) music.end();
        }
    }

    private boolean sent;

    private void send(boolean cancelled) {
        if (sent) return;
        sent = true;
        if (rite) PacketDistributor.sendToServer(new tk.darrow.tribalpower.gate.DrumRite.Result(pos, riteSeed, hits, strays, cancelled));
        else PacketDistributor.sendToServer(new DrumPractice.Result(pos, song.index(), difficulty.ordinal(), score, hits, perfects,
                bestStreak, misses, strays, failed, cancelled));
    }

    // ---- drawing helpers ------------------------------------------------------------------------------------------

    private static int argb(int colour, float alpha) {
        return ((int) (Mth.clamp(alpha, 0, 1) * ((colour >>> 24) & 0xFF)) << 24) | (colour & 0xFFFFFF);
    }

    private static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF, aa = (a >>> 24);
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF, ba = (b >>> 24);
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    private static void vertex(VertexConsumer v, Matrix4f m, float x, float y, int colour) {
        v.addVertex(m, x, y, 0).setColor(colour);
    }

    /** A four-cornered shape, corners in the same turn as GuiGraphics.fill (top-left, bottom-left, bottom-right, top-right). */
    private static void quad(GuiGraphics g, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, int top, int bottom) {
        VertexConsumer v = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        vertex(v, m, x1, y1, top);
        vertex(v, m, x2, y2, bottom);
        vertex(v, m, x3, y3, bottom);
        vertex(v, m, x4, y4, top);
    }

    /** A filled ellipse from a fan of thin quads. */
    private static void ellipse(GuiGraphics g, float cx, float cy, float rx, float ry, int centre, int edge) {
        VertexConsumer v = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        int steps = Math.max(12, Math.min(40, (int) (rx * 1.2F)));
        for (int i = 0; i < steps; i++) {
            double a0 = Math.PI * 2 * i / steps, a1 = Math.PI * 2 * (i + 1) / steps;
            vertex(v, m, cx, cy, centre);
            vertex(v, m, cx + (float) Math.cos(a1) * rx, cy + (float) Math.sin(a1) * ry, edge);
            vertex(v, m, cx + (float) Math.cos(a0) * rx, cy + (float) Math.sin(a0) * ry, edge);
            vertex(v, m, cx, cy, centre);
        }
    }

    private static void ring(GuiGraphics g, float cx, float cy, float rx, float ry, float thick, int colour) {
        VertexConsumer v = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        int steps = Math.max(14, Math.min(48, (int) (rx * 1.4F)));
        for (int i = 0; i < steps; i++) {
            double a0 = Math.PI * 2 * i / steps, a1 = Math.PI * 2 * (i + 1) / steps;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            vertex(v, m, cx + c0 * (rx - thick), cy + s0 * (ry - thick * ry / rx), colour);
            vertex(v, m, cx + c1 * (rx - thick), cy + s1 * (ry - thick * ry / rx), colour);
            vertex(v, m, cx + c1 * rx, cy + s1 * ry, colour);
            vertex(v, m, cx + c0 * rx, cy + s0 * ry, colour);
        }
    }

    private static void star(GuiGraphics g, float cx, float cy, float r, int fill) {
        VertexConsumer v = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        for (int i = 0; i < 10; i++) {
            double a0 = -Math.PI / 2 + Math.PI * i / 5, a1 = -Math.PI / 2 + Math.PI * (i + 1) / 5;
            float r0 = i % 2 == 0 ? r : r * 0.45F, r1 = i % 2 == 0 ? r * 0.45F : r;
            vertex(v, m, cx, cy, fill);
            vertex(v, m, cx + (float) Math.cos(a1) * r1, cy + (float) Math.sin(a1) * r1, fill);
            vertex(v, m, cx + (float) Math.cos(a0) * r0, cy + (float) Math.sin(a0) * r0, fill);
            vertex(v, m, cx, cy, fill);
        }
    }

    private void text(GuiGraphics g, String s, float x, float y, float scale, int colour, boolean centred) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        if (centred) g.drawString(font, s, -font.width(s) / 2, 0, colour, true);
        else g.drawString(font, s, 0, 0, colour, true);
        g.pose().popPose();
    }

    // ---- the highway ----------------------------------------------------------------------------------------------

    private float topY, strikeY, bottomY, halfBottom, halfTop, centre;

    /** How far up the highway (0 at the strike line, 1 at the far end) a note {@code ahead} ms away sits, with depth. */
    private float depth(long ahead) {
        float u = Mth.clamp((float) (ahead * speed) / VIEW_MS, -0.15F, 1);
        float k = 1.9F;
        return u * (1 + k) / (1 + k * Math.max(0, u));
    }

    private float yAt(float d) {
        return strikeY - (strikeY - topY) * d;
    }

    private float halfAt(float d) {
        return Mth.lerp(Math.max(0, d), halfBottom, halfTop) * (d < 0 ? 1 - d * 0.6F : 1);
    }

    private float laneX(int lane, float d) {
        return centre + (lane - 1.5F) * (halfAt(d) * 2 / 4);
    }

    private int railColour(long t) {
        if (surging(t)) return mix(GOLD, 0xFFFFFFFF, 0.5F + 0.5F * Mth.sin(t / 90F));
        return switch (Songbook.multiplier(streak)) {
            case 4 -> 0xFFC99BFF;
            case 3 -> GOLD;
            case 2 -> TEAL;
            default -> 0xFF8FA7A0;
        };
    }

    private void layout() {
        centre = width / 2F;
        topY = height * 0.10F;
        strikeY = height * 0.80F;
        bottomY = height * 0.93F;
        halfBottom = Math.min(190, width * 0.24F);
        halfTop = halfBottom * 0.34F;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, width, height, 0xC0060A0E, 0xE0020406);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        layout();
        long raw = now();
        long t = Math.max(-1, raw);
        long frame = System.nanoTime() / 1_000_000;
        lastFrame = frame;
        if (autoplay && !done && !failed && t >= 0) {
            for (int i = first; i < chart.size() && chart.times()[i] <= t; i++)
                if (state[i] == 0) { padDown[chart.lanes()[i]] = false; strike(chart.lanes()[i], chart.times()[i]); }
            if (surge >= 0.5) unleash(t);
        }
        boolean hot = surging(t);
        int rail = railColour(t);

        // the highway surface, its lanes and rails
        float farHalf = halfAt(1), nearHalf = halfAt(-0.12F), nearY = bottomY;
        quad(g, centre - farHalf, topY, centre - nearHalf, nearY, centre + nearHalf, nearY, centre + farHalf, topY,
                hot ? 0x5A3A2A08 : 0x40101C22, hot ? 0xF03A2A10 : 0xF0101A20);
        for (int lane = 0; lane < 4; lane++) {
            float lit = Mth.clamp(1 - (t - padAt[lane]) / 220F, 0, 1);
            if (padDown[lane]) lit = Math.max(lit, 0.55F);
            if (lit <= 0) continue;
            float lx0 = centre + (lane - 2) * (farHalf * 2 / 4), lx1 = lx0 + farHalf * 2 / 4;
            float nx0 = centre + (lane - 2) * (halfBottom * 2 / 4), nx1 = nx0 + halfBottom * 2 / 4;
            quad(g, lx0, topY, nx0, strikeY, nx1, strikeY, lx1, topY, argb(LANE[lane], 0), argb(LANE[lane], lit * 0.35F));
        }
        for (int k = 1; k < 4; k++) {
            float fx = centre + (k - 2) * (farHalf * 2 / 4), nx = centre + (k - 2) * (nearHalf * 2 / 4);
            quad(g, fx - 0.4F, topY, nx - 0.8F, nearY, nx + 0.8F, nearY, fx + 0.4F, topY, 0x30FFFFFF, 0x50FFFFFF);
        }
        for (int side = -1; side <= 1; side += 2) {
            float fx = centre + side * farHalf, nx = centre + side * nearHalf;
            quad(g, fx - 1, topY, nx - 2.5F, nearY, nx + 2.5F, nearY, fx + 1, topY, argb(rail, 0.35F), argb(rail, 1));
        }
        // beat lines from the song's own beats; every fourth is a bar
        long[] beats = charts.beats();
        for (int b = 0; b < beats.length; b++) {
            long ahead = beats[b] - t;
            if (ahead < -120 || ahead * speed > VIEW_MS) continue;
            float d = depth(ahead), y = yAt(d), h = halfAt(d);
            boolean bar = b % 4 == 0;
            float thick = bar ? 1.6F : 0.7F;
            g.fill((int) (centre - h), (int) (y - thick / 2), (int) (centre + h), (int) Math.ceil(y + thick / 2),
                    argb(bar ? 0xFFFFFFFF : 0xFFB0C8C0, (bar ? 0.45F : 0.18F) * (1 - d * 0.6F)));
        }

        // the drum heads at the strike line
        float padRx = halfBottom / 4 * 0.78F, padRy = padRx * 0.42F;
        for (int lane = 0; lane < 4; lane++) {
            float x = laneX(lane, 0);
            boolean pressed = padDown[lane] || t - padAt[lane] < 90;
            float press = pressed ? 0.9F : 1;
            ellipse(g, x, strikeY + 3, padRx * 1.08F, padRy * 1.15F, 0xFF1A1208, 0xFF120C06);
            ellipse(g, x, strikeY, padRx * press, padRy * press, pressed ? mix(LANE[lane], 0xFFFFFFFF, 0.55F) : 0xFFD9C9A3,
                    pressed ? LANE[lane] : 0xFFB39A6E);
            ring(g, x, strikeY, padRx * press, padRy * press, 3, pressed ? 0xFFFFFFFF : LANE[lane]);
            ring(g, x, strikeY, padRx * press * 0.55F, padRy * press * 0.55F, 1.2F, argb(LANE_DARK[lane], 0.6F));
        }

        // the notes, far to near
        int last = first;
        while (last < chart.size() && (chart.times()[last] - t) * speed <= VIEW_MS) last++;
        for (int i = last - 1; i >= Math.max(0, first - 8); i--) {
            if (state[i] == 1) continue;
            long ahead = chart.times()[i] - t;
            if (ahead < -260) continue;
            float d = depth(ahead);
            int lane = chart.lanes()[i];
            float x = laneX(lane, d), y = yAt(d);
            float scale = halfAt(d) / halfBottom;
            float rx = padRx * 0.92F * scale, ry = padRy * 0.95F * scale;
            float fade = d > 0.85F ? (1 - d) / 0.15F : ahead < 0 ? Math.max(0, 1 + ahead / 180F) : 1;
            boolean missed = state[i] == 2;
            if (fade <= 0) continue;
            int body = missed ? 0xFF5A4A4A : LANE[lane], dark = missed ? 0xFF2A2222 : LANE_DARK[lane];
            if (surgeNote[i] && !missed) {
                float glow = 0.55F + 0.45F * Mth.sin(frame / 110F + i);
                ellipse(g, x, y, rx * 1.45F, ry * 1.6F, argb(0xFFFFF0B0, 0.55F * glow * fade), argb(0xFFFFE080, 0));
            }
            ellipse(g, x, y + ry * 0.35F, rx, ry, argb(dark, fade), argb(dark, fade));
            ellipse(g, x, y, rx, ry, argb(mix(body, 0xFFFFFFFF, 0.35F), fade), argb(body, fade));
            ring(g, x, y, rx, ry, Math.max(1, 2.2F * scale), argb(surgeNote[i] && !missed ? 0xFFFFF4C8 : mix(body, 0xFF000000, 0.4F), fade));
            ellipse(g, x - rx * 0.25F, y - ry * 0.3F, rx * 0.35F, ry * 0.3F, argb(0xFFFFFFFF, 0.55F * fade), argb(0xFFFFFFFF, 0));
        }

        // sparks
        for (Iterator<Spark> it = sparks.iterator(); it.hasNext(); ) {
            Spark s = it.next();
            long age = frame - s.born;
            if (age > s.life) { it.remove(); continue; }
            float k = age / (float) s.life;
            float x = s.p[0] + s.p[2] * age / 16F, y = s.p[1] + s.p[3] * age / 16F + 0.012F * age * age / 16F;
            float r = 2.2F * (1 - k);
            g.fill((int) (x - r), (int) (y - r), (int) (x + r + 1), (int) (y + r + 1), argb(s.colour, 1 - k));
        }
        g.flush();

        // hit judgements floating above the drums
        for (Iterator<Popup> it = popups.iterator(); it.hasNext(); ) {
            Popup p = it.next();
            long age = t - p.at;
            if (age > 520) { it.remove(); continue; }
            float k = age / 520F;
            text(g, p.text, laneX(p.lane, 0), strikeY - padRy - 16 - k * 18, 1, argb(p.colour, 1 - k * k), true);
        }
        for (int lane = 0; lane < 4; lane++)
            text(g, KEYS[lane], laneX(lane, 0), strikeY + padRy + 6, 1, padDown[lane] ? 0xFFFFFFFF : argb(LANE[lane], 0.8F), true);

        if (!done) {
            hud(g, t, frame);
            if (duel != 0) duelHud(g, t);
        }
        if (t < 0 || t < 1400) {
            float a = t < 0 ? 1 : 1 - t / 1400F;
            text(g, Component.translatable("gui.tribalpower.songkeeper.get_ready").getString(), centre, height * 0.4F, 2, argb(TEXT, a), true);
        }
        if (t - calloutAt < 1400 && !done) {
            float k = (t - calloutAt) / 1400F;
            float s = 2.4F + (k < 0.15F ? (0.15F - k) * 6 : 0);
            text(g, callout, centre, height * 0.30F, s, argb(GOLD, k > 0.7F ? (1 - k) / 0.3F : 1), true);
        }
        if (t - leaveAsk < 2500 && !done)
            text(g, Component.translatable("gui.tribalpower.songkeeper.leave").getString(), centre, height * 0.46F, 1, 0xFFFFB0A0, true);
        if (failed && !done) {
            float k = Mth.clamp((t - failedAt) / 800F, 0, 1);
            text(g, Component.translatable("gui.tribalpower.songkeeper.failed").getString(), centre, height * 0.38F, 3.2F, argb(RED, k), true);
        }
        if (done) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            results(g, t);
            g.pose().popPose();
        }
    }

    private void burst(int lane, int count, int colour, long t) {
        float x = laneX(lane, 0), y = strikeY;
        long frame = System.nanoTime() / 1_000_000;
        for (int i = 0; i < count; i++) {
            float angle = (float) (-Math.PI / 2 + (random.nextFloat() - 0.5F) * 2.2F);
            float v = 1.2F + random.nextFloat() * 2.4F;
            sparks.add(new Spark(new float[]{x + (random.nextFloat() - 0.5F) * 16, y, (float) Math.cos(angle) * v, (float) Math.sin(angle) * v},
                    random.nextFloat() < 0.3F ? 0xFFFFFFFF : colour, frame, 260 + random.nextInt(260)));
        }
    }

    // ---- heads-up display -----------------------------------------------------------------------------------------

    private void hud(GuiGraphics g, long t, long frame) {
        if (rite) {
            riteHud(g, t, frame);
            return;
        }
        int mult = Songbook.multiplier(streak) * (surging(t) ? 2 : 1);
        // score and multiplier on a dark card left of the highway, right-aligned so a big score grows away from it;
        // on a narrow screen the card shrinks, the score gets smaller and the multiplier ring moves under it
        float right = centre - halfBottom - 12;
        float cardW = Mth.clamp(right - 6, 64, 132), left = right - cardW;
        boolean compact = cardW < 124;
        float top = strikeY - (compact ? 126 : 104), bottom = strikeY - 6;
        g.fill((int) left, (int) top, (int) right, (int) bottom, 0xB0080C10);
        g.renderOutline((int) left, (int) top, (int) (right - left), (int) (bottom - top), argb(railColour(t), 0.5F));
        String points = String.format("%,d", score);
        float scoreScale = Math.min(2, (cardW - 12) / Math.max(1, font.width(points)));
        text(g, points, right - 6 - font.width(points) * scoreScale, top + 6, scoreScale, TEXT, false);
        String run = Component.translatable("gui.tribalpower.songkeeper.streak_small", streak).getString();
        text(g, run, right - 6 - font.width(run), top + 8 + 9 * scoreScale, 1, QUIET, false);
        float radius = compact ? Math.min(22, cardW / 2 - 8) : 24;
        float cx = compact ? (left + right) / 2 : left + 32, cy = compact ? bottom - radius - 8 : strikeY - 38;
        ellipse(g, cx, cy, radius + 1, radius + 1, 0xFF101A20, 0xFF0A1014);
        int segs = streak >= 30 ? 10 : streak % 10;
        int col = railColour(t);
        float inner = radius * 0.75F;
        for (int i = 0; i < 10; i++) {
            double a0 = -Math.PI / 2 + Math.PI * 2 * i / 10 + 0.06, a1 = -Math.PI / 2 + Math.PI * 2 * (i + 1) / 10 - 0.06;
            int c = i < segs ? col : 0xFF263238;
            quad(g, cx + (float) Math.cos(a0) * inner, cy + (float) Math.sin(a0) * inner, cx + (float) Math.cos(a0) * radius, cy + (float) Math.sin(a0) * radius,
                    cx + (float) Math.cos(a1) * radius, cy + (float) Math.sin(a1) * radius, cx + (float) Math.cos(a1) * inner, cy + (float) Math.sin(a1) * inner, c, c);
        }
        g.flush();
        text(g, "x" + mult, cx, cy - 6, radius / 15F, col, true);
        if (surging(t)) text(g, Component.translatable("gui.tribalpower.songkeeper.surge_on").getString(), right - 6 - font.width(
                Component.translatable("gui.tribalpower.songkeeper.surge_on").getString()), top + 20 + 9 * scoreScale, 1, GOLD, false);

        // Resonance meter, right of the highway: red, amber, green, with a needle
        float rx = centre + halfBottom + 26, ry = strikeY - 130, rh = 120, rw = 12;
        g.fill((int) rx - 2, (int) ry - 2, (int) (rx + rw + 2), (int) (ry + rh + 2), 0xFF0A1014);
        g.fill((int) rx, (int) ry, (int) (rx + rw), (int) (ry + rh / 3), 0xFF2E7D4F);
        g.fill((int) rx, (int) (ry + rh / 3), (int) (rx + rw), (int) (ry + rh * 2 / 3), 0xFF8A7A2A);
        g.fill((int) rx, (int) (ry + rh * 2 / 3), (int) (rx + rw), (int) (ry + rh), 0xFF8A2E2A);
        if (rite) {
            // the mark the rite must reach to open the gate
            int mark = (int) (ry + rh * (1 - tk.darrow.tribalpower.gate.DrumRite.PASS));
            g.fill((int) rx - 3, mark, (int) (rx + rw + 3), mark + 1, GOLD);
        }
        float needle = ry + rh * (1 - (float) resonance);
        boolean danger = (rite ? resonance < tk.darrow.tribalpower.gate.DrumRite.PASS : resonance < 0.33) && !failed;
        int needleColour = danger && (frame / 200) % 2 == 0 ? RED : 0xFFFFFFFF;
        g.fill((int) rx - 5, (int) needle - 1, (int) (rx + rw + 5), (int) needle + 2, needleColour);
        text(g, Component.translatable("gui.tribalpower.songkeeper.resonance").getString(), rx + rw / 2, ry + rh + 6, 0.75F, QUIET, true);
        // Spirit Surge, under it
        float sy = ry + rh + 20;
        for (int i = 0; i < 4; i++) {
            float fill = (float) Mth.clamp((surging(t) ? (surgeActiveUntil - t) / 16000.0 : surge) * 4 - i, 0, 1);
            int x0 = (int) (rx - 8 + i * 8);
            g.fill(x0, (int) sy, x0 + 7, (int) sy + 5, 0xFF1C262C);
            if (fill > 0) g.fill(x0, (int) sy, x0 + (int) (7 * fill), (int) sy + 5, surging(t) ? GOLD : 0xFFFFE7A0);
        }
        if (surge >= 0.5 && !surging(t))
            text(g, Component.translatable("gui.tribalpower.songkeeper.surge_key").getString(), rx + rw / 2, sy + 8, 0.75F,
                    argb(GOLD, 0.6F + 0.4F * Mth.sin(frame / 120F)), true);

        // song, difficulty, progress along the top
        text(g, song.title(), 10, 8, 1, TEXT, false);
        text(g, rite ? Component.translatable("gui.tribalpower.songkeeper.rite_line", (int) (tk.darrow.tribalpower.gate.DrumRite.PASS * 100)).getString()
                : song.album() + "  ·  " + Component.translatable("gui.tribalpower.songkeeper.difficulty." + difficulty.key()).getString(), 10, 19, 0.75F, QUIET, false);
        float progress = Mth.clamp(Math.max(0, t) / (float) song.lengthMs(), 0, 1);
        g.fill(10, 30, 160, 32, 0xFF1C262C);
        g.fill(10, 30, 10 + (int) (150 * progress), 32, TEAL);
        long shown = Math.max(0, t) / 1000, total = song.lengthMs() / 1000;
        text(g, String.format("%d:%02d / %d:%02d", shown / 60, shown % 60, total / 60, total % 60), 164, 28, 0.75F, QUIET, false);
    }

    /**
     * The Gate Rite's heads-up: what it is, how long it has left, and the Resonance meter with the mark the gate needs.
     * No score, no multiplier, no Surge: the rite is the way to the March, not a game to be ranked on.
     */
    private void riteHud(GuiGraphics g, long t, long frame) {
        text(g, Component.translatable("gui.tribalpower.rite.title").getString(), 10, 8, 1, TEXT, false);
        text(g, Component.translatable("gui.tribalpower.songkeeper.rite_line", (int) (tk.darrow.tribalpower.gate.DrumRite.PASS * 100)).getString(),
                10, 19, 0.75F, QUIET, false);
        float progress = Mth.clamp(Math.max(0, t) / (float) song.lengthMs(), 0, 1);
        g.fill(10, 30, 160, 32, 0xFF1C262C);
        g.fill(10, 30, 10 + (int) (150 * progress), 32, TEAL);
        long left = Math.max(0, song.lengthMs() - Math.max(0, t)) / 1000;
        text(g, Component.translatable("gui.tribalpower.rite.time", left).getString(), 164, 28, 0.75F, QUIET, false);
        if (streak > 1) text(g, Component.translatable("gui.tribalpower.rite.combo", streak).getString(), centre, topY + 10, 1, TEXT, true);
        float rx = centre + halfBottom + 26, ry = strikeY - 130, rh = 120, rw = 12;
        g.fill((int) rx - 2, (int) ry - 2, (int) (rx + rw + 2), (int) (ry + rh + 2), 0xFF0A1014);
        float pass = (float) tk.darrow.tribalpower.gate.DrumRite.PASS;
        g.fill((int) rx, (int) ry, (int) (rx + rw), (int) (ry + rh * (1 - pass)), 0xFF2E7D4F);
        g.fill((int) rx, (int) (ry + rh * (1 - pass)), (int) (rx + rw), (int) (ry + rh), 0xFF8A2E2A);
        int mark = (int) (ry + rh * (1 - pass));
        g.fill((int) rx - 3, mark, (int) (rx + rw + 3), mark + 1, GOLD);
        float needle = ry + rh * (1 - (float) resonance);
        boolean short_ = resonance < pass;
        g.fill((int) rx - 5, (int) needle - 1, (int) (rx + rw + 5), (int) needle + 2, short_ && (frame / 250) % 2 == 0 ? RED : 0xFFFFFFFF);
        text(g, Component.translatable("gui.tribalpower.rite.resonance", (int) Math.round(resonance * 100)).getString(), rx + rw / 2, ry + rh + 6, 0.75F, QUIET, true);
    }

    private void duelHud(GuiGraphics g, long t) {
        long theirs = rival == null ? 0 : rival.score();
        float share = score + theirs == 0 ? 0.5F : (float) score / (score + theirs);
        int w = Math.min(320, width - 40), x = width / 2 - w / 2, y = 6;
        g.fill(x - 1, y - 1, x + w + 1, y + 9, 0xFF0A1014);
        g.fill(x, y, x + (int) (w * share), y + 8, TEAL);
        g.fill(x + (int) (w * share), y, x + w, y + 8, 0xFFFF8A5A);
        g.fill(x + w / 2, y - 2, x + w / 2 + 1, y + 10, 0xFFFFFFFF);
        text(g, Component.translatable("gui.tribalpower.songkeeper.duel.you").getString(), x, y + 11, 0.75F, TEAL, false);
        String name = opponent;
        text(g, name, x + w - font.width(name) * 0.75F, y + 11, 0.75F, 0xFFFF8A5A, false);
        // their card, top right
        int cx = width - 130, cy = 40;
        g.fill(cx, cy, cx + 120, cy + 52, 0xC00A1014);
        g.renderOutline(cx, cy, 120, 52, 0xFFFF8A5A);
        text(g, name, cx + 6, cy + 5, 1, TEXT, false);
        text(g, String.format("%,d", theirs), cx + 6, cy + 17, 1.3F, 0xFFFFB08A, false);
        if (rival != null) {
            text(g, "x" + rival.multiplier() + "  " + Component.translatable("gui.tribalpower.songkeeper.streak_small", rival.streak()).getString(),
                    cx + 6, cy + 32, 0.75F, QUIET, false);
            g.fill(cx + 6, cy + 43, cx + 114, cy + 46, 0xFF1C262C);
            g.fill(cx + 6, cy + 43, cx + 6 + (int) (108 * rival.meter() / 100F), cy + 46, rival.meter() < 33 ? RED : TEAL);
            if (rival.failed()) text(g, Component.translatable("gui.tribalpower.songkeeper.duel.broke").getString(), cx + 60, cy + 20, 1, RED, true);
        }
    }

    // ---- results ----------------------------------------------------------------------------------------------------

    private void results(GuiGraphics g, long t) {
        float k = Mth.clamp((t - doneAt) / 400F, 0, 1);
        int w = 280, h = duel != 0 ? 196 : 176, x = width / 2 - w / 2, y = height / 2 - h / 2;
        g.fill(0, 0, width, height, argb(0xC0000000, k));
        g.fill(x, y, x + w, y + h, argb(0xF00C1418, k));
        g.renderOutline(x, y, w, h, argb(failed ? RED : GOLD, k));
        boolean fullCombo = !failed && misses == 0 && hits == chart.size() && chart.size() > 0;
        if (rite) {
            double accuracy = tk.darrow.tribalpower.gate.DrumRite.accuracy(hits, strays, chart.size());
            boolean opened = accuracy >= tk.darrow.tribalpower.gate.DrumRite.PASS;
            text(g, Component.translatable(opened ? "gui.tribalpower.rite.opened" : "gui.tribalpower.rite.broken").getString().toUpperCase(java.util.Locale.ROOT),
                    width / 2F, y + 24, 2, opened ? TEAL : RED, true);
            text(g, Component.translatable("gui.tribalpower.songkeeper.rite_result", (int) Math.round(accuracy * 100),
                    (int) (tk.darrow.tribalpower.gate.DrumRite.PASS * 100)).getString(), width / 2F, y + 72, 1.3F, opened ? TEAL : TEXT, true);
            text(g, Component.translatable("gui.tribalpower.songkeeper.breakdown", perfects, greats, goods, misses).getString(), width / 2F, y + 96, 0.75F, QUIET, true);
            text(g, Component.translatable("gui.tribalpower.songkeeper.best_streak", bestStreak).getString(), width / 2F, y + 108, 0.75F, QUIET, true);
            text(g, Component.translatable(opened ? "gui.tribalpower.songkeeper.rite_on" : "gui.tribalpower.songkeeper.rite_again").getString(),
                    width / 2F, y + 132, 0.75F, argb(QUIET, k), true);
            return;
        }
        String head = Component.translatable(failed ? "gui.tribalpower.songkeeper.failed" : fullCombo ? "gui.tribalpower.songkeeper.full_combo"
                : "gui.tribalpower.songkeeper.complete").getString();
        text(g, head, width / 2F, y + 10, 2, failed ? RED : fullCombo ? GOLD : TEXT, true);
        text(g, song.title() + "  ·  " + Component.translatable("gui.tribalpower.songkeeper.difficulty." + difficulty.key()).getString(),
                width / 2F, y + 32, 0.75F, QUIET, true);
        int stars = failed ? 0 : Songbook.stars(song, difficulty, score);
        for (int i = 0; i < 5; i++) {
            float appear = Mth.clamp((t - doneAt - 300 - i * 180) / 200F, 0, 1);
            int fill = i < stars ? (fullCombo ? 0xFFFFE070 : GOLD) : 0xFF2A343A;
            if (appear > 0) star(g, width / 2F - 56 + i * 28, y + 58, 11 * (i < stars ? 0.8F + 0.2F * appear : 1), argb(fill, appear));
        }
        g.flush();
        text(g, String.format("%,d", score), width / 2F, y + 76, 2, TEXT, true);
        int total = chart.size();
        int accuracy = total == 0 ? 0 : Math.round(100F * hits / total);
        text(g, Component.translatable("gui.tribalpower.songkeeper.accuracy", accuracy, hits, total).getString(), width / 2F, y + 98, 1, TEAL, true);
        text(g, Component.translatable("gui.tribalpower.songkeeper.breakdown", perfects, greats, goods, misses).getString(), width / 2F, y + 112, 0.75F, QUIET, true);
        text(g, Component.translatable("gui.tribalpower.songkeeper.best_streak", bestStreak).getString(), width / 2F, y + 124, 0.75F, QUIET, true);
        int line = y + 140;
        if (duel != 0) {
            if (outcome == null) text(g, Component.translatable("gui.tribalpower.songkeeper.duel.waiting", opponent).getString(), width / 2F, line, 1, QUIET, true);
            else {
                String verdict = Component.translatable(outcome.result() > 0 ? "gui.tribalpower.songkeeper.duel.win"
                        : outcome.result() < 0 ? "gui.tribalpower.songkeeper.duel.loss" : "gui.tribalpower.songkeeper.duel.draw").getString();
                text(g, verdict, width / 2F, line - 2, 1.6F, outcome.result() > 0 ? GOLD : outcome.result() < 0 ? RED : TEXT, true);
                text(g, String.format("%,d  —  %,d  %s", outcome.yours(), outcome.theirs(), outcome.opponent()), width / 2F, line + 16, 0.75F, QUIET, true);
            }
            line += 30;
        }
        text(g, Component.translatable(duel == 0 ? "gui.tribalpower.songkeeper.after" : "gui.tribalpower.songkeeper.after_duel").getString(),
                width / 2F, line + 6, 0.75F, argb(QUIET, k), true);
    }
}
