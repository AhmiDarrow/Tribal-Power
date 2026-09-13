package tk.darrow.tribalpower.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import tk.darrow.tribalpower.device.SealLoomMenu;

public class SealLoomScreen extends AbstractContainerScreen<SealLoomMenu> {
    public SealLoomScreen(SealLoomMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF101B22);
        g.renderOutline(x, y, imageWidth, imageHeight, 0xFFB58A58);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 16, 0xFF20333A);
        for (var slot : menu.slots) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xFF081317);
            g.renderOutline(x + slot.x - 1, y + slot.y - 1, 18, 18, 0xFF385456);
        }
        g.drawString(font, ">", x + 100, y + 38, 0xFFDBBE89, false);
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, 0xFFE7DCC1, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xFF98ACA5, false);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
