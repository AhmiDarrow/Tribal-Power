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
    private static long nextScan;
    private static long leyReadAt = Long.MIN_VALUE;
    private static LeyMath.Factors factors;
    private static int[] counts;
    private static java.util.Set<tk.darrow.tribalpower.api.pulse.Attunement> voices;

    private LeyLensHud() {}

    public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.screen != null || mc.player.isSpectator()) return;
        ItemStack lens = LeyLensItem.held(mc.player);
        if (lens.isEmpty()) return;
        int mode = LeyLensItem.mode(lens);
        // Pulse mode lists a row per machine, so the panel grows to fit what it actually found.
        java.util.List<LensPulsePayload.Entry> rows = mode == LeyLensItem.PULSE
                ? LensPulsePayload.latest.machines() : java.util.List.of();
        int width = mode == LeyLensItem.PULSE ? 152 : 116;
        int height = 46 + (rows.isEmpty() ? 0 : 4 + rows.size() * 10);
        int x = mc.getWindow().getGuiScaledWidth() - width - 12, y = mc.getWindow().getGuiScaledHeight() - 69 - height - 4;
        var g = event.getGuiGraphics();
        g.fill(x, y, x + width, y + height, 0xD9101B22);
        g.fill(x, y, x + 2, y + height, 0xFF74DBCB);
        g.drawString(mc.font, Component.translatable("gui.tribalpower.lens.mode." + mode), x + 8, y + 4, 0xFFE7DCC1, false);
        long time = mc.level.getGameTime();
        if (mode == LeyLensItem.LEY) {
            // The reading takes a sweep of the surrounding blocks and the weather; a frame is far too often.
            if (factors == null || time != leyReadAt) {
                leyReadAt = time;
                factors = LeyMath.glimpse(mc.level, mc.player.blockPosition());
            }
            LeyMath.Factors reading = factors;
            int percent = (int) Math.round(reading.strength() * 100);
            Vector3f c = LeyLensItem.colour(reading.strength());
            int colour = 0xFF000000 | ((int) (c.x * 255) << 16) | ((int) (c.y * 255) << 8) | (int) (c.z * 255);
            g.drawString(mc.font, Component.translatable("gui.tribalpower.ley", percent), x + 8, y + 16, 0xFFE7DCC1, false);
            g.fill(x + 8, y + 28, x + width - 8, y + 31, 0xFF30494A);
            g.fill(x + 8, y + 28, x + 8 + Math.min(width - 16, percent), y + 31, colour);
            StringBuilder tags = new StringBuilder();
            if (reading.sky()) tags.append(reading.night() ? "night sky " : "sky ");
            if (reading.rain()) tags.append(reading.thunder() ? "storm " : "rain ");
            if (reading.water() > 0) tags.append("water ");
            if (reading.greenery() > 0) tags.append("green ");
            if (tags.isEmpty()) tags.append("sheltered");
            g.drawString(mc.font, tags.toString().trim(), x + 8, y + 34, 0xFF99C9BD, false);
            return;
        }
        var pos = mc.player.blockPosition();
        int r = tk.darrow.tribalpower.lattice.LatticeNetwork.DEFAULT_RADIUS;
        long now = time;
        boolean due = now >= nextScan || now < nextScan - 20;
        if (due) nextScan = now + 20;
        if (mode == LeyLensItem.PULSE) {
            // Pulse stores are server-side only; ask once a second.
            if (due) net.neoforged.neoforge.network.PacketDistributor.sendToServer(LensPulsePayload.empty());
            LensPulsePayload reading = LensPulsePayload.latest;
            g.drawString(mc.font, Component.translatable("gui.tribalpower.lens.zone", r), x + 8, y + 16, 0xFF99C9BD, false);
            g.drawString(mc.font, Component.translatable("gui.tribalpower.lens.pulse", reading.stored(), reading.capacity(), reading.count()), x + 8, y + 28, 0xFF65D7C0, false);
            int rowY = y + 42;
            for (LensPulsePayload.Entry entry : rows) {
                machineRow(mc, g, entry, x, rowY, width);
                rowY += 10;
            }
            if (reading.count() > rows.size())
                g.drawString(mc.font, Component.translatable("gui.tribalpower.lens.more", reading.count() - rows.size()),
                        x + 8, rowY, 0xFF667A80, false);
        } else if (mode == LeyLensItem.VOICE) {
            if (due || voices == null) voices = tk.darrow.tribalpower.lattice.LatticeNetwork.collectAttunements(mc.level, pos, r);
            g.drawString(mc.font, Component.translatable("gui.tribalpower.lens.voices", voices.size()), x + 8, y + 16, 0xFF99C9BD, false);
            String names = voices.stream().map(v -> v.getSerializedName()).reduce((a, b) -> a + " " + b).orElse("none");
            g.drawString(mc.font, mc.font.plainSubstrByWidth(names, 100), x + 8, y + 28, 0xFF74DBCB, false);
        } else {
            // Counting the machines walks every block entity in four chunks: once a second is plenty.
            if (due || counts == null) {
                int stations = 0, relays = 0, caches = 0;
                for (var be : nearby(mc, pos, r)) {
                    if (be instanceof tk.darrow.tribalpower.blockentity.EchoStationBlockEntity) stations++;
                    else if (be instanceof tk.darrow.tribalpower.blockentity.WirelessRelayBlockEntity) relays++;
                    else if (be instanceof tk.darrow.tribalpower.blockentity.AncestralCacheBlockEntity) caches++;
                }
                counts = new int[]{stations, relays, caches};
            }
            g.drawString(mc.font, Component.translatable("gui.tribalpower.lens.machines", counts[0], counts[1], counts[2]), x + 8, y + 16, 0xFF99C9BD, false);
            g.drawString(mc.font, Component.translatable("gui.tribalpower.lens.hint"), x + 8, y + 28, 0xFF667A80, false);
        }
    }

    /**
     * One machine: its name on the left, what it holds and what it is making on the right. The store is
     * tinted by how full it is, so a row of them reads as a gauge without needing a bar each.
     */
    private static void machineRow(Minecraft mc, net.minecraft.client.gui.GuiGraphics g,
                                   LensPulsePayload.Entry entry, int x, int rowY, int width) {
        String value = entry.stored() + "/" + entry.capacity();
        String rate = entry.perSecond() > 0 ? "  +" + entry.perSecond() + "/s" : "";
        int valueWidth = mc.font.width(value + rate);
        float fill = entry.capacity() <= 0 ? 0F : Math.min(1F, (float) entry.stored() / entry.capacity());
        int store = 0xFF000000 | (lerp(0xC9, 0x65, fill) << 16) | (lerp(0x7A, 0xD7, fill) << 8) | lerp(0x80, 0xC0, fill);
        Component name = mc.level == null ? Component.empty()
                : mc.level.getBlockState(entry.pos()).getBlock().getName();
        g.drawString(mc.font, mc.font.plainSubstrByWidth(name.getString(), width - 20 - valueWidth),
                x + 8, rowY, 0xFFE7DCC1, false);
        g.drawString(mc.font, value, x + width - 8 - valueWidth, rowY, store, false);
        if (!rate.isEmpty())
            g.drawString(mc.font, rate, x + width - 8 - mc.font.width(rate), rowY, 0xFFF0C95E, false);
    }

    private static int lerp(int from, int to, float t) {
        return Math.round(from + (to - from) * t);
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
