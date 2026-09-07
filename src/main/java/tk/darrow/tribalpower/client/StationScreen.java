package tk.darrow.tribalpower.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import tk.darrow.tribalpower.echo.StationMenu;

/** A quiet copper-and-ink workshop panel, rendered without external UI dependencies. */
public class StationScreen extends AbstractContainerScreen<StationMenu> {
    public StationScreen(StationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 176; imageHeight = 188; inventoryLabelY = 92;
    }
    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x+176, y+188, 0xFF101B22);
        g.renderOutline(x, y, 176, 188, 0xFFB58A58);
        g.fill(x+1, y+1, x+175, y+24, 0xFF20333A);
        g.fill(x+8, y+26, x+168, y+88, 0xFF14262C);
        for (var slot : menu.slots) {
            g.fill(x+slot.x-1, y+slot.y-1, x+slot.x+17, y+slot.y+17, 0xFF081317);
            g.renderOutline(x+slot.x-1, y+slot.y-1, 18, 18, 0xFF385456);
        }
        g.fill(x+52, y+47, x+79, y+53, 0xFF314448);
        g.fill(x+52, y+47, x+52+27*menu.work()/menu.duration(), y+53, 0xFF65D7C0);
        g.drawString(font, ">", x+78, y+46, 0xFFDBBE89, false);
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 8, 0xFFE7DCC1, false);
        g.drawString(font, playerInventoryTitle, 8, 92, 0xFF98ACA5, false);
        Component status = Component.translatable(menu.statusKey(), menu.work());
        g.drawString(font, font.plainSubstrByWidth(status.getString(), 157), 9, 76, 0xFF99C9BD, false);
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick); renderTooltip(g, mouseX, mouseY);
    }
}
