package tk.darrow.tribalpower.ley;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.joml.Vector3f;

/**
 * Client HUD line "Ley: NN%" while a Ley Lens is held in either hand. Computed client-side from
 * {@link LeyMath}, whose inputs (sky, time, rain, water, greenery) are all visible to the client.
 * Register {@code LeyLensHud::render} on the NeoForge bus from the client mod class only.
 */
public final class LeyLensHud {
    private LeyLensHud() {}

    public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.screen != null || mc.player.isSpectator()) return;
        if (!LeyLensItem.holding(mc.player)) return;
        LeyMath.Factors factors = LeyMath.factors(mc.level, mc.player.blockPosition());
        int percent = (int) Math.round(factors.strength() * 100);
        Vector3f c = LeyLensItem.colour(factors.strength());
        int colour = 0xFF000000 | ((int) (c.x * 255) << 16) | ((int) (c.y * 255) << 8) | (int) (c.z * 255);
        int width = 116, height = 34;
        int x = mc.getWindow().getGuiScaledWidth() - 128, y = mc.getWindow().getGuiScaledHeight() - 69 - height - 4;
        var g = event.getGuiGraphics();
        g.fill(x, y, x + width, y + height, 0xD9101B22);
        g.fill(x, y, x + 2, y + height, colour);
        g.drawString(mc.font, Component.translatable("gui.tribalpower.ley", percent), x + 8, y + 5, 0xFFE7DCC1, false);
        g.fill(x + 8, y + 18, x + 108, y + 21, 0xFF30494A);
        g.fill(x + 8, y + 18, x + 8 + percent, y + 21, colour);
        StringBuilder tags = new StringBuilder();
        if (factors.sky()) tags.append(factors.night() ? "night sky " : "sky ");
        if (factors.rain()) tags.append("rain ");
        if (factors.water()) tags.append("water ");
        if (factors.greenery()) tags.append("green ");
        if (tags.isEmpty()) tags.append("sheltered");
        g.drawString(mc.font, tags.toString().trim(), x + 8, y + 24, 0xFF99C9BD, false);
    }
}
