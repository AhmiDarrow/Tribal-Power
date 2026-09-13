package tk.darrow.tribalpower.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import tk.darrow.tribalpower.echo.CacheMenu;
import tk.darrow.tribalpower.lattice.SideIo;

public class CacheScreen extends AbstractContainerScreen<CacheMenu> {
    public CacheScreen(CacheMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 222;
        inventoryLabelY = 128;
    }

    private int ioX() { return leftPos + 178; }
    private int ioY() { return topPos + 18; }

    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + 176, y + 222, 0xFF101B22);
        g.renderOutline(x, y, 176, 222, 0xFFB58A58);
        g.fill(x + 1, y + 1, x + 175, y + 16, 0xFF20333A);
        for (var slot : menu.slots) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xFF081317);
            g.renderOutline(x + slot.x - 1, y + slot.y - 1, 18, 18, 0xFF385456);
        }
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, 0xFFE7DCC1, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xFF98ACA5, false);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        SideIo io = new SideIo(SideIo.Mode.BOTH);
        io.unpack(menu.ioPacked());
        renderTooltip(g, mouseX, mouseY);
        SideIoWidget.render(g, font, ioX(), ioY(), io, mouseX, mouseY);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (SideIoWidget.click(mouseX, mouseY, ioX(), ioY(), menu.machinePos())) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
