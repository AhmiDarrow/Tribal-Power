package tk.darrow.tribalpower.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import tk.darrow.tribalpower.echo.RelayMenu;

public class RelayScreen extends AbstractContainerScreen<RelayMenu> {
    public RelayScreen(RelayMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + 176, y + 166, 0xFF101B22);
        g.renderOutline(x, y, 176, 166, 0xFFB58A58);
        g.fill(x + 1, y + 1, x + 175, y + 24, 0xFF20333A);
        g.fill(x + 8, y + 26, x + 168, y + 70, 0xFF14262C);
        for (var slot : menu.slots) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xFF081317);
            g.renderOutline(x + slot.x - 1, y + slot.y - 1, 18, 18, 0xFF385456);
        }
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 8, 0xFFE7DCC1, false);
        g.drawString(font, playerInventoryTitle, 8, 72, 0xFF98ACA5, false);
        g.drawString(font, Component.translatable("gui.tribalpower.relay.link"), 44, 24, 0xFF98ACA5, false);
        g.drawString(font, Component.translatable("gui.tribalpower.relay.rune"), 116, 24, 0xFF98ACA5, false);
        g.drawString(font, Component.translatable(menu.extracting() ? "message.tribalpower.relay.extract" : "message.tribalpower.relay.insert"),
                8, 58, 0xFF99C9BD, false);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
