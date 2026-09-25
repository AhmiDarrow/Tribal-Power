package tk.darrow.tribalpower.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import tk.darrow.tribalpower.item.GearSettingsPayload;
import tk.darrow.tribalpower.item.SpiritStaffItem;

/**
 * Every Tribal Power hotkey, rebindable under its own heading in Controls. The Gear screen key opens the settings
 * for worn Spiritweave, goggles and the vault; the rest are shortcuts for what sneak-use also does. The rite lanes
 * only matter inside the Gate Rite screen, so they may share keys with movement.
 */
public final class TribalKeys {
    private static final String CATEGORY = "key.categories.tribalpower";
    public static final KeyMapping GEAR = key("gear", KeyConflictContext.IN_GAME, GLFW.GLFW_KEY_APOSTROPHE);
    public static final KeyMapping GOGGLES = key("goggles", KeyConflictContext.IN_GAME, GLFW.GLFW_KEY_UNKNOWN);
    public static final KeyMapping STAFF_VOICE = key("staff_voice", KeyConflictContext.IN_GAME, GLFW.GLFW_KEY_UNKNOWN);
    public static final KeyMapping VAULT = key("vault", KeyConflictContext.IN_GAME, GLFW.GLFW_KEY_UNKNOWN);
    public static final KeyMapping HUD = key("hud", KeyConflictContext.IN_GAME, GLFW.GLFW_KEY_UNKNOWN);
    public static final KeyMapping[] RITE_LANES = {
            key("rite_lane_1", KeyConflictContext.GUI, GLFW.GLFW_KEY_A),
            key("rite_lane_2", KeyConflictContext.GUI, GLFW.GLFW_KEY_S),
            key("rite_lane_3", KeyConflictContext.GUI, GLFW.GLFW_KEY_D),
            key("rite_lane_4", KeyConflictContext.GUI, GLFW.GLFW_KEY_F),
    };

    private TribalKeys() {}

    private static KeyMapping key(String name, KeyConflictContext context, int code) {
        return new KeyMapping("key.tribalpower." + name, context, InputConstants.Type.KEYSYM, code, CATEGORY);
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(GEAR);
        event.register(GOGGLES);
        event.register(STAFF_VOICE);
        event.register(VAULT);
        event.register(HUD);
        for (KeyMapping lane : RITE_LANES) event.register(lane);
    }

    /** The rite lane a key press means, or -1. The arrow keys always work as well, for players who never rebind. */
    public static int riteLane(int key, int scan) {
        for (int i = 0; i < RITE_LANES.length; i++) if (RITE_LANES[i].matches(key, scan)) return i;
        return switch (key) {
            case GLFW.GLFW_KEY_LEFT -> 0;
            case GLFW.GLFW_KEY_DOWN -> 1;
            case GLFW.GLFW_KEY_UP -> 2;
            case GLFW.GLFW_KEY_RIGHT -> 3;
            default -> -1;
        };
    }

    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        while (GEAR.consumeClick()) if (mc.screen == null) mc.setScreen(new GearScreen());
        while (GOGGLES.consumeClick()) PacketDistributor.sendToServer(new GearSettingsPayload(GearSettingsPayload.GOGGLES, 0, false));
        while (VAULT.consumeClick()) PacketDistributor.sendToServer(new GearSettingsPayload(GearSettingsPayload.VAULT, 0, false));
        while (STAFF_VOICE.consumeClick()) {
            if (mc.player.getMainHandItem().getItem() instanceof SpiritStaffItem || mc.player.getOffhandItem().getItem() instanceof SpiritStaffItem)
                PacketDistributor.sendToServer(new GearSettingsPayload(GearSettingsPayload.STAFF_VOICE, 0, false));
        }
        while (HUD.consumeClick()) PulseHud.hidden = !PulseHud.hidden;
    }
}
