package tk.darrow.tribalpower.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import tk.darrow.tribalpower.item.*;

/** Contextual rather than permanent HUD chrome. Only appears while holding Pulse equipment. */
public final class PulseHud {
    private static long readAt = Long.MIN_VALUE;
    private static net.minecraft.world.item.ItemStack readHeld = net.minecraft.world.item.ItemStack.EMPTY;
    private static int pulse, capacity;
    /** The readout's lines, built with the sums rather than every frame; the staff line is null off a staff. */
    private static Component pulseText = Component.empty(), staffText;

    private PulseHud() {}
    /** Switched off with the Pulse HUD hotkey or in the Gear screen; back on next launch. */
    public static boolean hidden;

    public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (hidden || mc.player == null || mc.options.hideGui || mc.screen != null || mc.player.isSpectator()) return;
        var held = mc.player.getMainHandItem();
        // Anything that can seat a cell spends Pulse: every Spiritgear tool and piece, the maul, the staff,
        // songbooks and the Pulse bows, not just the few that were listed here by hand.
        if (!(held.getItem() instanceof PulseCellItem) && !GearCell.accepts(held)) return;
        // A piece's own seated cell counts too: it is what the piece spends first. Both sums walk the
        // whole inventory, so they are taken once a tick rather than once a frame.
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        if (now != readAt || held != readHeld) {
            readAt = now;
            readHeld = held;
            pulse = SpiritgearHelper.availablePulse(mc.player) + GearCell.pulse(held);
            capacity = GearCell.capacity(held);
            for (var stack : mc.player.getInventory().items) if (stack.getItem() instanceof PulseCellItem) capacity += PulseCellItem.capacity(stack);
            capacity += PulseCellItem.capacity(mc.player.getOffhandItem());
            pulseText = Component.translatable("gui.tribalpower.pulse", pulse);
            staffText = held.getItem() instanceof SpiritStaffItem
                    ? Component.translatable("spell.tribalpower." + SpiritStaffItem.element(held).getSerializedName()) : null;
        }
        int x = mc.getWindow().getGuiScaledWidth()-128, y = mc.getWindow().getGuiScaledHeight()-69;
        var g = event.getGuiGraphics();
        g.fill(x, y, x+116, y+34, 0xD9101B22); g.fill(x, y, x+2, y+34, 0xFFB58A58);
        g.drawString(mc.font, pulseText, x+8, y+5, 0xFFE7DCC1, false);
        g.fill(x+8, y+18, x+108, y+21, 0xFF30494A);
        g.fill(x+8, y+18, x+8+(capacity == 0 ? 0 : Math.min(100, pulse*100/capacity)), y+21, 0xFF65D7C0);
        if (staffText != null) g.drawString(mc.font, staffText, x+8, y+24, 0xFF99C9BD, false);
    }
}
