package tk.darrow.tribalpower.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import java.util.ArrayDeque;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.StringJoiner;

/**
 * Headless exhibit mode for release verification ({@code -Dtribalpower.showcaseVerification=true}).
 *
 * <p>Takes a screenshot every 4 s named {@code showcase-<n>.png} while a level is loaded. With
 * {@code -Dtribalpower.showcaseScreens=true} it also polls {@code <gameDir>/showcase-command.txt} twice a second so an
 * external driver ({@code tools/showcase_verification.py}, which owns the server through RCON) can open client-only
 * screens, act as the player and take labelled captures. One command per file, executed on the render thread, then
 * the file is replaced by {@code showcase-done.txt} holding the result:
 * <ul>
 *   <li>{@code shot <label>} — capture {@code showcase-<n>-<label>.png} now.</li>
 *   <li>{@code codex <entry id>} — open the Spirit Codex on that entry (spoilers revealed).</li>
 *   <li>{@code tablet <n>} — open the Lore Tablet screen for tablet {@code n}.</li>
 *   <li>{@code interact <entity id | uuid=... | name=...> [sneak]} — right-click the nearest entity of that type (or that one) within 8 blocks
 *       (with sneak: opens a bonded Mossback's saddlebag; an Elder opens its trading screen).</li>
 *   <li>{@code attack <entity id>} — left-click the nearest entity of that type within 8 blocks.</li>
 *   <li>{@code use <x> <y> <z> [times] [face]} — right-click a block face (default up) with the held item that many
 *       times (offerings at a Tribe Hearth raise standing so an Elder will trade).</li>
 *   <li>{@code useitem [times]} — right-click the air with the held item.</li>
 *   <li>{@code hold <ticks>} / {@code key <key.name> [ticks]} — hold the use key (or any key mapping by its
 *       translation name, {@code key.attack}, {@code key.sneak}, ...) for that many ticks, then release; the reply
 *       comes after the release.</li>
 *   <li>{@code mine <x> <y> <z>} — break a block the way the player does (survival swings until it gives).</li>
 *   <li>{@code hotbar <0-8>}, {@code sneak on|off}, {@code look <x> <y> <z>}, {@code chat <text or /command>}.</li>
 *   <li>{@code buttons} — the open screen's widgets with their text; {@code press <index|text>} — click one
 *       (dialogue choices, the codex's buttons).</li>
 *   <li>{@code button <id>} — press a menu button of the open menu (the Song Bench's actions);
 *       {@code click <slot> [button] [type]} — click a slot of the open menu ({@code PICKUP}, {@code QUICK_MOVE},
 *       {@code SWAP}, {@code THROW}); {@code state} — position, held item, open screen and its non-empty slots.</li>
 *   <li>{@code messages [clear]} — the chat and action-bar lines received since the last clear, newest last,
 *       separated by {@code |}.</li>
 *   <li>{@code swingshot <label>} — swing the held item and capture the next six frames as {@code <label>_t<n>}.</li>
 *   <li>{@code view first|back|front} — the camera, for shots of the player's own swing.</li>
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
    // a command that spans ticks (a held key, a survival mine) keeps the poller busy until it finishes
    private static String pendingLine;
    private static KeyMapping heldKey;
    private static int heldTicks;
    private static String heldLabel;
    private static int heldFrame;
    private static BlockPos mining;
    private static int miningTicks;
    private static String swingLabel;
    private static int swingTick;
    private static final ArrayDeque<String> MESSAGES = new ArrayDeque<>();
    private static String lastOverlay = "";
    private static int lastOverlayTime;

    private ShowcaseVerification() {}

    public static void install() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ShowcaseVerification::gui);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ShowcaseVerification::screen);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ShowcaseVerification::tick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ShowcaseVerification::chat);
    }

    /** Action-bar text reaches the HUD without an event, so it is read off the Gui each tick. */
    private static void overlay(Minecraft mc) {
        try {
            var text = net.minecraft.client.gui.Gui.class.getDeclaredField("overlayMessageString");
            var time = net.minecraft.client.gui.Gui.class.getDeclaredField("overlayMessageTime");
            text.setAccessible(true);
            time.setAccessible(true);
            var component = (net.minecraft.network.chat.Component) text.get(mc.gui);
            int ticks = time.getInt(mc.gui);
            String shown = component == null ? "" : component.getString();
            boolean fresh = ticks > lastOverlayTime || (!shown.equals(lastOverlay) && ticks > 0);
            lastOverlayTime = ticks;
            if (fresh && !shown.isEmpty()) {
                lastOverlay = shown;
                synchronized (MESSAGES) {
                    MESSAGES.addLast("[bar] " + shown.replace('|', '/').replace('\n', ' '));
                    while (MESSAGES.size() > 200) MESSAGES.removeFirst();
                }
            }
        } catch (ReflectiveOperationException ignored) { }
    }

    /** Every chat, system and action-bar line the player is shown, kept for the driver's {@code messages}. */
    public static void chat(ClientChatReceivedEvent event) {
        if (!ENABLED) return;
        synchronized (MESSAGES) {
            MESSAGES.addLast(event.getMessage().getString().replace('|', '/').replace('\n', ' '));
            while (MESSAGES.size() > 200) MESSAGES.removeFirst();
        }
    }

    /** Screen changes must happen outside ClientHooks' iteration over layered screens. */
    public static void tick(ClientTickEvent.Post event) {
        if (!ENABLED || !SCREENS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        // a headless window is never focused: the game menu would open on every focus loss and eat the keys
        if (mc.options.pauseOnLostFocus) mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        overlay(mc);
        if (pendingLine != null) { advance(mc); return; }
        long now = System.currentTimeMillis();
        if (now >= nextPoll) { nextPoll = now + POLL_MS; poll(mc); }
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
        if (result == null) { pendingLine = line; return; }   // finishes over the next ticks
        done(mc, line, result);
    }

    private static void done(Minecraft mc, String line, String result) {
        try { Files.writeString(mc.gameDirectory.toPath().resolve("showcase-done.txt"), line + "\n" + result + "\n"); }
        catch (IOException ignored) { }
    }

    /** One tick of a command that spans ticks. */
    private static void advance(Minecraft mc) {
        if (heldKey != null) {
            heldFrame++;
            // a labelled hold films a frame every five ticks, so a bow's draw can be looked at
            if (heldLabel != null && heldFrame % 5 == 0) grab(mc, "showcase-" + (++captures) + "-" + heldLabel + "_t" + heldFrame + ".png");
            if (--heldTicks > 0) return;
            KeyMapping.set(heldKey.getKey(), false);
            heldKey.setDown(false);
            String line = pendingLine;
            pendingLine = null; heldKey = null; heldLabel = null;
            done(mc, line, "ok released");
        } else if (swingLabel != null) {
            // a frame of the swing every tick while it lasts, labelled by tick
            swingTick++;
            grab(mc, "showcase-" + (++captures) + "-" + swingLabel + "_t" + swingTick + ".png");
            if (swingTick < 6) return;
            String line = pendingLine;
            pendingLine = null; swingLabel = null;
            done(mc, line, "ok " + swingTick + " frames");
        } else if (mining != null) {
            boolean gone = mc.level.getBlockState(mining).isAir();
            if (!gone && --miningTicks > 0) {
                mc.gameMode.continueDestroyBlock(mining, Direction.UP);
                mc.player.swing(InteractionHand.MAIN_HAND);
                return;
            }
            mc.gameMode.stopDestroyBlock();
            String line = pendingLine;
            pendingLine = null; mining = null;
            done(mc, line, gone ? "ok broken" : "error still standing");
        } else {
            pendingLine = null;
        }
    }

    /** The nearest living entity of a type, or {@code uuid=<uuid>} / {@code name=<name>} for one in particular (command tags never reach the client). */
    private static Entity nearest(Minecraft mc, String rawId) {
        java.util.function.Predicate<Entity> match;
        if (rawId.startsWith("uuid=")) {
            String uuid = rawId.substring(5).toLowerCase(java.util.Locale.ROOT);
            match = e -> e.getUUID().toString().equals(uuid);
        } else if (rawId.startsWith("name=")) {
            String name = rawId.substring(5);
            match = e -> e.getName().getString().equalsIgnoreCase(name);
        } else {
            ResourceLocation id = ResourceLocation.parse(rawId);
            match = e -> BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).equals(id);
        }
        return mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(8), e -> match.test(e) && e.isAlive())
                .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(mc.player))).orElse(null);
    }

    private static void sneak(Minecraft mc, boolean sneak) {
        // LocalPlayer reads sneaking from its Input; the interact packet copies that flag for the server.
        mc.player.input.shiftKeyDown = sneak;
        mc.player.setShiftKeyDown(sneak);
    }

    private static KeyMapping key(Minecraft mc, String name) {
        for (KeyMapping mapping : mc.options.keyMappings) if (mapping.getName().equals(name)) return mapping;
        return null;
    }

    private static String describe(ItemStack stack) {
        return stack.isEmpty() ? "empty" : BuiltInRegistries.ITEM.getKey(stack.getItem()) + "x" + stack.getCount();
    }

    /** Runs one command; a null result means it continues over the next ticks and replies from {@link #advance}. */
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
                boolean sneak = parts.length > 2 && parts[2].equals("sneak");
                Entity target = nearest(mc, parts[1]);
                if (target == null) return "error no " + parts[1] + " within 8 blocks";
                if (mc.screen != null) mc.setScreen(null);
                // an explicit sneak is held for the click only; otherwise the held key state (sneak on|off) stands
                if (sneak) sneak(mc, true);
                mc.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
                var result = mc.gameMode.interact(mc.player, target, InteractionHand.MAIN_HAND);
                if (sneak) sneak(mc, mc.options.keyShift.isDown());
                return "ok " + result + " " + target.getId();
            }
            case "attack" -> {
                if (parts.length < 2) return "error missing entity id";
                Entity target = nearest(mc, parts[1]);
                if (target == null) return "error no " + parts[1] + " within 8 blocks";
                if (mc.screen != null) mc.setScreen(null);
                mc.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
                mc.gameMode.attack(mc.player, target);
                mc.player.swing(InteractionHand.MAIN_HAND);
                return "ok attacked " + target.getId();
            }
            case "use" -> {
                // use <x> <y> <z> [times] [face]: right-click a block face with the held item (hearth offerings).
                if (parts.length < 4) return "error missing block position";
                BlockPos pos = new BlockPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                int times = parts.length > 4 ? Integer.parseInt(parts[4]) : 1;
                Direction face = parts.length > 5 ? Direction.byName(parts[5]) : Direction.UP;
                if (face == null) face = Direction.UP;
                if (mc.screen != null) mc.setScreen(null);
                mc.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(pos));
                var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5)), face, pos, false);
                InteractionResult last = InteractionResult.PASS;
                for (int i = 0; i < times; i++) last = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
                return "ok " + last + " x" + times;
            }
            case "useitem" -> {
                int times = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                if (mc.screen != null) mc.setScreen(null);
                InteractionResult last = InteractionResult.PASS;
                for (int i = 0; i < times; i++) last = mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                return "ok " + last + " x" + times;
            }
            case "hold", "key" -> {
                // hold <ticks> [label] / key <key.name> [ticks] [label]: a label films a frame every five ticks
                String name = parts[0].equals("hold") ? "key.use" : (parts.length > 1 ? parts[1] : "key.use");
                String label = null;
                int ticks = 20;
                for (int p = 1; p < parts.length; p++) {
                    if (parts[p].matches("\\d+")) ticks = Integer.parseInt(parts[p]);
                    else if (!parts[p].startsWith("key.")) label = parts[p].replaceAll("[^A-Za-z0-9_-]", "_");
                }
                heldLabel = label; heldFrame = 0;
                KeyMapping mapping = key(mc, name);
                if (mapping == null) return "error no key mapping " + name;
                if (mc.screen != null) mc.setScreen(null);
                KeyMapping.set(mapping.getKey(), true);
                KeyMapping.click(mapping.getKey());
                mapping.setDown(true);
                heldKey = mapping; heldTicks = Math.max(1, ticks);
                return null;
            }
            case "mine" -> {
                if (parts.length < 4) return "error missing block position";
                BlockPos pos = new BlockPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                if (mc.screen != null) mc.setScreen(null);
                mc.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(pos));
                mc.gameMode.startDestroyBlock(pos, Direction.UP);
                mc.player.swing(InteractionHand.MAIN_HAND);
                mining = pos; miningTicks = 400;
                return null;
            }
            case "hotbar" -> {
                int slot = Integer.parseInt(parts[1]);
                mc.player.getInventory().selected = Math.floorMod(slot, 9);
                return "ok slot " + mc.player.getInventory().selected + " " + describe(mc.player.getMainHandItem());
            }
            case "sneak" -> {
                // the keyboard input overwrites the flag every tick, so a lasting sneak has to hold the key itself
                boolean on = parts.length > 1 && parts[1].equals("on");
                KeyMapping.set(mc.options.keyShift.getKey(), on);
                mc.options.keyShift.setDown(on);
                sneak(mc, on);
                return "ok sneak " + on;
            }
            case "look" -> {
                if (parts.length < 4) return "error missing position";
                mc.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
                        new Vec3(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3])));
                return "ok looking";
            }
            case "chat" -> {
                String text = line.substring(parts[0].length()).strip();
                if (text.startsWith("/")) mc.player.connection.sendCommand(text.substring(1));
                else mc.player.connection.sendChat(text);
                return "ok sent";
            }
            case "click" -> {
                if (!(mc.screen instanceof AbstractContainerScreen<?> screen)) return "error no menu open";
                int slot = Integer.parseInt(parts[1]);
                int button = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                ClickType type = parts.length > 3 ? ClickType.valueOf(parts[3]) : ClickType.PICKUP;
                mc.gameMode.handleInventoryMouseClick(screen.getMenu().containerId, slot, button, type, mc.player);
                return "ok clicked " + slot + " carrying " + describe(screen.getMenu().getCarried());
            }
            case "button" -> {
                if (!(mc.screen instanceof AbstractContainerScreen<?> screen)) return "error no menu open";
                int id = Integer.parseInt(parts[1]);
                mc.gameMode.handleInventoryButtonClick(screen.getMenu().containerId, id);
                return "ok button " + id;
            }
            case "buttons" -> {
                if (mc.screen == null) return "error no screen open";
                StringJoiner out = new StringJoiner("|");
                int i = 0;
                for (var child : mc.screen.children())
                    if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget)
                        out.add((i++) + ":" + widget.getClass().getSimpleName() + ":" + widget.getMessage().getString().replace('|', '/'));
                return "ok " + out;
            }
            case "press" -> {
                // press <index | text>: click the widget with that index from `buttons`, or whose text contains the words
                if (mc.screen == null) return "error no screen open";
                String want = line.substring(parts[0].length()).strip();
                int i = 0;
                net.minecraft.client.gui.components.AbstractWidget hit = null;
                for (var child : mc.screen.children()) {
                    if (!(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)) continue;
                    if (want.matches("\\d+") ? i == Integer.parseInt(want) : widget.getMessage().getString().toLowerCase().contains(want.toLowerCase())) { hit = widget; break; }
                    i++;
                }
                if (hit == null) return "error no widget " + want;
                if (!hit.active) return "error widget inactive " + hit.getMessage().getString();
                double cx = hit.getX() + hit.getWidth() / 2.0, cy = hit.getY() + hit.getHeight() / 2.0;
                hit.mouseClicked(cx, cy, 0);
                hit.mouseReleased(cx, cy, 0);
                return "ok pressed " + hit.getMessage().getString().replace('|', '/');
            }
            case "state" -> {
                StringJoiner out = new StringJoiner(" ");
                out.add("ok pos=" + mc.player.blockPosition().toShortString().replace(" ", ""));
                out.add("held=" + describe(mc.player.getMainHandItem()));
                out.add("screen=" + (mc.screen == null ? "none" : mc.screen.getClass().getSimpleName()));
                if (mc.screen != null) out.add("title=" + mc.screen.getTitle().getString().replace(' ', '_'));
                if (mc.screen instanceof AbstractContainerScreen<?> screen) {
                    StringJoiner slots = new StringJoiner(",");
                    for (Slot slot : screen.getMenu().slots) if (slot.hasItem()) slots.add(slot.index + ":" + describe(slot.getItem()));
                    out.add("slots=" + screen.getMenu().slots.size() + "[" + slots + "]");
                }
                return out.toString();
            }
            case "messages" -> {
                synchronized (MESSAGES) {
                    String all = String.join("|", MESSAGES);
                    if (parts.length > 1 && parts[1].equals("clear")) MESSAGES.clear();
                    return "ok " + all;
                }
            }
            case "swingshot" -> {
                // swingshot <label>: swing the held item and capture the next six frames
                String label = parts.length > 1 ? parts[1].replaceAll("[^A-Za-z0-9_-]", "_") : "swing";
                if (mc.screen != null) mc.setScreen(null);
                mc.player.swing(InteractionHand.MAIN_HAND);
                swingLabel = label; swingTick = 0;
                return null;
            }
            case "view" -> {
                // view first|back|front: the camera, for shots of the player's own body
                String which = parts.length > 1 ? parts[1] : "first";
                mc.options.setCameraType(switch (which) {
                    case "back" -> net.minecraft.client.CameraType.THIRD_PERSON_BACK;
                    case "front" -> net.minecraft.client.CameraType.THIRD_PERSON_FRONT;
                    default -> net.minecraft.client.CameraType.FIRST_PERSON;
                });
                return "ok view " + which;
            }
            case "reload" -> { mc.reloadResourcePacks(); return "ok reloading"; }
            case "close" -> {
                // the way Escape closes it: a container screen then tells the server its menu is shut
                if (mc.screen != null) mc.screen.onClose(); else mc.setScreen(null);
                return "ok closed";
            }
            default -> { return "error unknown command " + parts[0]; }
        }
    }
}
