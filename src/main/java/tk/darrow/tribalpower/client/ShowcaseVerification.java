package tk.darrow.tribalpower.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Headless exhibit mode for release verification ({@code -Dtribalpower.showcaseVerification=true}).
 *
 * <p>Takes a screenshot every 4 s named {@code showcase-<n>.png} while a level is loaded. With
 * {@code -Dtribalpower.showcaseScreens=true} it also polls {@code <gameDir>/showcase-command.txt} twice a second so an
 * external driver ({@code tools/showcase_verification.py}, which owns the server through RCON) can open client-only
 * screens and take labelled captures. One command per file, executed on the render thread, then the file is replaced
 * by {@code showcase-done.txt} holding the result:
 * <ul>
 *   <li>{@code shot <label>} — capture {@code showcase-<n>-<label>.png} now.</li>
 *   <li>{@code codex <entry id>} — open the Spirit Codex on that entry (spoilers revealed).</li>
 *   <li>{@code tablet <n>} — open the Lore Tablet screen for tablet {@code n}.</li>
 *   <li>{@code interact <entity id> [sneak]} — right-click the nearest entity of that type within 8 blocks
 *       (with sneak: opens a bonded Mossback's saddlebag; an Elder opens its trading screen).</li>
 *   <li>{@code use <x> <y> <z> [times]} — right-click a block's top face with the held item that many times
 *       (offerings at a Tribe Hearth raise standing so an Elder will trade).</li>
 *   <li>{@code reload} — reload resource packs (models and textures edited on disk).</li>
 *   <li>{@code close} — close any open screen.</li>
 * </ul>
 */
public final class ShowcaseVerification {
    private static final boolean ENABLED = Boolean.getBoolean("tribalpower.showcaseVerification");
    private static final boolean SCREENS = Boolean.getBoolean("tribalpower.showcaseScreens");
    private static final long INTERVAL_MS = 4000, POLL_MS = 500;
    private static long nextCapture, nextPoll;
    private static int captures;

    private ShowcaseVerification() {}

    public static void install() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ShowcaseVerification::gui);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ShowcaseVerification::screen);
    }

    /** HUD phase: the frame already holds the level and HUD; captures happen here unless a screen is open. */
    public static void gui(RenderGuiEvent.Post event) { frame(false); }
    /** Screen phase: fires after an open screen has drawn, so GUI captures include it. */
    public static void screen(ScreenEvent.Render.Post event) { frame(true); }

    private static void frame(boolean screenPhase) {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || (mc.screen != null) != screenPhase) return;
        long now = System.currentTimeMillis();
        if (SCREENS && now >= nextPoll) { nextPoll = now + POLL_MS; poll(mc); }
        if (now < nextCapture) return;
        nextCapture = now + INTERVAL_MS;
        grab(mc, "showcase-" + (++captures) + ".png");
    }

    private static void grab(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> {});
    }

    private static void poll(Minecraft mc) {
        Path command = mc.gameDirectory.toPath().resolve("showcase-command.txt");
        if (!Files.isRegularFile(command)) return;
        String line;
        try { line = Files.readString(command).strip(); Files.deleteIfExists(command); }
        catch (IOException e) { return; }
        if (line.isEmpty()) return;
        String result;
        try { result = execute(mc, line); }
        catch (RuntimeException e) { result = "error " + e; }
        try { Files.writeString(mc.gameDirectory.toPath().resolve("showcase-done.txt"), line + "\n" + result + "\n"); }
        catch (IOException ignored) { }
    }

    private static String execute(Minecraft mc, String line) {
        String[] parts = line.split("\\s+");
        switch (parts[0]) {
            case "shot" -> {
                String label = parts.length > 1 ? parts[1].replaceAll("[^A-Za-z0-9_-]", "_") : "shot";
                grab(mc, "showcase-" + (++captures) + "-" + label + ".png");
                return "ok showcase-" + captures + "-" + label + ".png";
            }
            case "codex" -> {
                if (parts.length < 2) return "error missing entry id";
                SpiritCodexScreen screen = mc.screen instanceof SpiritCodexScreen open ? open : new SpiritCodexScreen();
                if (mc.screen != screen) mc.setScreen(screen);
                screen.showcaseOpen(parts[1]);
                return "ok " + parts[1];
            }
            case "tablet" -> {
                int tablet = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                mc.setScreen(new LoreTabletScreen(tablet));
                return "ok tablet " + tablet;
            }
            case "interact" -> {
                if (parts.length < 2) return "error missing entity id";
                ResourceLocation id = ResourceLocation.parse(parts[1]);
                boolean sneak = parts.length > 2 && parts[2].equals("sneak");
                Entity target = mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(8), e ->
                        BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).equals(id) && e.isAlive())
                        .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(mc.player))).orElse(null);
                if (target == null) return "error no " + id + " within 8 blocks";
                if (mc.screen != null) mc.setScreen(null);
                // LocalPlayer reads sneaking from its Input; the interact packet copies that flag for the server.
                mc.player.input.shiftKeyDown = sneak;
                mc.player.setShiftKeyDown(sneak);
                mc.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
                var result = mc.gameMode.interact(mc.player, target, InteractionHand.MAIN_HAND);
                mc.player.input.shiftKeyDown = false;
                mc.player.setShiftKeyDown(false);
                return "ok " + result + " " + target.getId();
            }
            case "use" -> {
                // use <x> <y> <z> [times]: right-click the top face of a block with the held item (hearth offerings).
                if (parts.length < 4) return "error missing block position";
                BlockPos pos = new BlockPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                int times = parts.length > 4 ? Integer.parseInt(parts[4]) : 1;
                if (mc.screen != null) mc.setScreen(null);
                mc.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(pos));
                var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
                InteractionResult last = InteractionResult.PASS;
                for (int i = 0; i < times; i++) last = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
                return "ok " + last + " x" + times;
            }
            case "reload" -> { mc.reloadResourcePacks(); return "ok reloading"; }
            case "close" -> { mc.setScreen(null); return "ok closed"; }
            default -> { return "error unknown command " + parts[0]; }
        }
    }
}
