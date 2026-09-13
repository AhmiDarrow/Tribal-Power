package tk.darrow.tribalpower.ley;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
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
        ItemStack lens = LeyLensItem.held(mc.player);
        if (lens.isEmpty()) return;
        int mode = LeyLensItem.mode(lens);
        int width = 116, height = 46;
        int x = mc.getWindow().getGuiScaledWidth() - 128, y = mc.getWindow().getGuiScaledHeight() - 69 - height - 4;
        var g = event.getGuiGraphics();
        g.fill(x, y, x + width, y + height, 0xD9101B22);
        g.fill(x, y, x + 2, y + height, 0xFF74DBCB);
        g.drawString(mc.font, Component.translatable("gui.tribalpower.lens.mode." + mode), x + 8, y + 4, 0xFFE7DCC1, false);
        if (mode == LeyLensItem.LEY) {
            LeyMath.Factors factors = LeyMath.glimpse(mc.level, mc.player.blockPosition());
            int percent = (int) Math.round(factors.strength() * 100);
            Vector3f c = LeyLensItem.colour(factors.strength());
            int colour = 0xFF000000 | ((int) (c.x * 255) << 16) | ((int) (c.y * 255) << 8) | (int) (c.z * 255);
            g.drawString(mc.font, Component.translatable("gui.tribalpower.ley", percent), x + 8, y + 16, 0xFFE7DCC1, false);
            g.fill(x + 8, y + 28, x + 108, y + 31, 0xFF30494A);
            g.fill(x + 8, y + 28, x + 8 + Math.min(100, percent), y + 31, colour);
            StringBuilder tags = new StringBuilder();
            if (factors.sky()) tags.append(factors.night() ? "night sky " : "sky ");
            if (factors.rain()) tags.append(factors.thunder() ? "storm " : "rain ");
            if (factors.water() > 0) tags.append("water ");
            if (factors.greenery() > 0) tags.append("green ");
            if (tags.isEmpty()) tags.append("sheltered");
            g.drawString(mc.font, tags.toString().trim(), x + 8, y + 34, 0xFF99C9BD, false);
            return;
        }
        var pos = mc.player.blockPosition();
        int r = tk.darrow.tribalpower.lattice.LatticeNetwork.DEFAULT_RADIUS;
        if (mode == LeyLensItem.PULSE) {
            int stored = 0, cap = 0, n = 0;
            for (var be : nearby(mc, pos, r)) {
                if (be instanceof tk.darrow.tribalpower.api.pulse.PulseHandler pulse && pulse.getPulseCapacity() > 0) {
                    stored += pulse.getPulseStored();
                    cap += pulse.getPulseCapacity();
                    n++;
                }
            }
            g.drawString(mc.font, Component.translatable("gui.tribalpower.lens.zone", r), x + 8, y + 16, 0xFF99C9BD, false);
            g.drawString(mc.font, Component.translatable("gui.tribalpower.lens.pulse", stored, cap, n), x + 8, y + 28, 0xFF65D7C0, false);
        } else if (mode == LeyLensItem.VOICE) {
            var voices = tk.darrow.tribalpower.lattice.LatticeNetwork.collectAttunements(mc.level, pos, r);
            g.drawString(mc.font, Component.translatable("gui.tribalpower.lens.voices", voices.size()), x + 8, y + 16, 0xFF99C9BD, false);
            String names = voices.stream().map(v -> v.getSerializedName()).reduce((a, b) -> a + " " + b).orElse("none");
            g.drawString(mc.font, mc.font.plainSubstrByWidth(names, 100), x + 8, y + 28, 0xFF74DBCB, false);
        } else {
            int stations = 0, relays = 0, caches = 0;
            for (var be : nearby(mc, pos, r)) {
                if (be instanceof tk.darrow.tribalpower.blockentity.EchoStationBlockEntity) stations++;
                else if (be instanceof tk.darrow.tribalpower.blockentity.WirelessRelayBlockEntity) relays++;
                else if (be instanceof tk.darrow.tribalpower.blockentity.AncestralCacheBlockEntity) caches++;
            }
            g.drawString(mc.font, Component.translatable("gui.tribalpower.lens.machines", stations, relays, caches), x + 8, y + 16, 0xFF99C9BD, false);
            g.drawString(mc.font, Component.translatable("gui.tribalpower.lens.hint"), x + 8, y + 28, 0xFF667A80, false);
        }
    }

    private static java.util.List<net.minecraft.world.level.block.entity.BlockEntity> nearby(Minecraft mc, net.minecraft.core.BlockPos origin, int r) {
        java.util.List<net.minecraft.world.level.block.entity.BlockEntity> out = new java.util.ArrayList<>();
        if (mc.level == null) return out;
        int minCx = (origin.getX() - r) >> 4, maxCx = (origin.getX() + r) >> 4;
        int minCz = (origin.getZ() - r) >> 4, maxCz = (origin.getZ() + r) >> 4;
        for (int cx = minCx; cx <= maxCx; cx++)
            for (int cz = minCz; cz <= maxCz; cz++) {
                if (!mc.level.hasChunk(cx, cz)) continue;
                for (var be : mc.level.getChunk(cx, cz).getBlockEntities().values()) {
                    var p = be.getBlockPos();
                    if (Math.abs(p.getX() - origin.getX()) > r || Math.abs(p.getY() - origin.getY()) > r || Math.abs(p.getZ() - origin.getZ()) > r) continue;
                    out.add(be);
                }
            }
        return out;
    }
}
