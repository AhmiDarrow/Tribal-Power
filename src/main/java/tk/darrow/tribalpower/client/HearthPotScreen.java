package tk.darrow.tribalpower.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import tk.darrow.tribalpower.cuisine.HearthPotBlockEntity;
import tk.darrow.tribalpower.cuisine.HearthPotMenu;

/** The Hearth Pot: four ingredients, the bowl, the fire under it and the meal. */
public class HearthPotScreen extends AbstractContainerScreen<HearthPotMenu> {
    private static final int INK = 0xFF101B22, PANEL = 0xFF14262C, RIM = 0xFFB58A58;
    private static final int SLOT = 0xFF081317, SLOT_RIM = 0xFF385456, TEXT = 0xFFE7DCC1, QUIET = 0xFF98ACA5, TEAL = 0xFF65D7C0;

    public HearthPotScreen(HearthPotMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, INK);
        g.renderOutline(x, y, imageWidth, imageHeight, RIM);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 14, 0xFF20333A);
        g.fill(x + 28, y + 18, x + 150, y + 76, PANEL);
        g.renderOutline(x + 28, y + 18, 122, 58, 0xFF26414A);
        for (var slot : menu.slots) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, SLOT);
            g.renderOutline(x + slot.x - 1, y + slot.y - 1, 18, 18, SLOT_RIM);
        }
        int width = 30 * menu.progress() / menu.total();
        g.fill(x + 88, y + 34, x + 118, y + 38, 0xFF20333A);
        g.fill(x + 88, y + 34, x + 88 + width, y + 38, TEAL);
        boolean hot = menu.state() != HearthPotBlockEntity.NO_HEAT;
        for (int i = 0; i < 4; i++) g.fill(x + 36 + i * 7, y + 66, x + 41 + i * 7, y + 70, hot ? 0xFFE0703A : 0xFF3A3530);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 4, TEXT, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, QUIET, false);
        g.drawString(font, Component.translatable("gui.tribalpower.hearth_pot.state." + menu.state()), 88, 22,
                menu.state() == HearthPotBlockEntity.COOKING ? TEXT : 0xFFE3B55A, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
