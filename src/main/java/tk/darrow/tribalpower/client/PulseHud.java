package tk.darrow.tribalpower.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import tk.darrow.tribalpower.item.*;

/** Contextual rather than permanent HUD chrome. Only appears while holding Pulse equipment. */
public final class PulseHud {
    private PulseHud() {}
    public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null || mc.player.isSpectator()) return;
        var held = mc.player.getMainHandItem();
        if (!(held.getItem() instanceof SpiritStaffItem) && !(held.getItem() instanceof PulseCellItem)
                && !(held.getItem() instanceof ResonanceMaulItem) && !held.is(ModItems.SPIRITGEAR_BLADE.get())) return;
        int pulse = SpiritgearHelper.availablePulse(mc.player), capacity = 0;
        for (var stack : mc.player.getInventory().items) if (stack.getItem() instanceof PulseCellItem) capacity += PulseCellItem.capacity(stack);
        capacity += PulseCellItem.capacity(mc.player.getOffhandItem());
        int x = mc.getWindow().getGuiScaledWidth()-128, y = mc.getWindow().getGuiScaledHeight()-69;
        var g = event.getGuiGraphics();
        g.fill(x, y, x+116, y+34, 0xD9101B22); g.fill(x, y, x+2, y+34, 0xFFB58A58);
        g.drawString(mc.font, Component.translatable("gui.tribalpower.pulse", pulse), x+8, y+5, 0xFFE7DCC1, false);
        g.fill(x+8, y+18, x+108, y+21, 0xFF30494A);
        g.fill(x+8, y+18, x+8+(capacity == 0 ? 0 : Math.min(100, pulse*100/capacity)), y+21, 0xFF65D7C0);
        if (held.getItem() instanceof SpiritStaffItem)
            g.drawString(mc.font, Component.translatable("spell.tribalpower." + SpiritStaffItem.element(held).getSerializedName()), x+8, y+24, 0xFF99C9BD, false);
    }
}
