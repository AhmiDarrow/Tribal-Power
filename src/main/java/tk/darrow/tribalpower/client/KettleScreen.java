package tk.darrow.tribalpower.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import tk.darrow.tribalpower.healing.KettleMenu;
import tk.darrow.tribalpower.healing.SpiritKettleBlockEntity;

/** The Spirit Kettle: reagent, herb and base, the brew, how far along it is, and what is holding it up. */
public class KettleScreen extends AbstractContainerScreen<KettleMenu> {
    private static final int INK = 0xFF101B22, PANEL = 0xFF14262C, RIM = 0xFFB58A58;
    private static final int SLOT = 0xFF081317, SLOT_RIM = 0xFF385456, TEXT = 0xFFE7DCC1, QUIET = 0xFF98ACA5, TEAL = 0xFF65D7C0;

    public KettleScreen(KettleMenu menu, Inventory inventory, Component title) {
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
        g.fill(x + 36, y + 12, x + 150, y + 76, PANEL);
        g.renderOutline(x + 36, y + 12, 114, 64, 0xFF26414A);
        for (var slot : menu.slots) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, SLOT);
            g.renderOutline(x + slot.x - 1, y + slot.y - 1, 18, 18, SLOT_RIM);
        }
        // The pour, filling as the batch brews.
        int width = 44 * menu.progress() / menu.total();
        g.fill(x + 66, y + 41, x + 110, y + 45, 0xFF20333A);
        g.fill(x + 66, y + 41, x + 66 + width, y + 45, TEAL);
        // Coals under the kettle: lit when there is heat.
        boolean hot = menu.state() != SpiritKettleBlockEntity.NO_HEAT;
        for (int i = 0; i < 5; i++) g.fill(x + 70 + i * 7, y + 62, x + 75 + i * 7, y + 66, hot ? 0xFFE0703A : 0xFF3A3530);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 4, TEXT, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, QUIET, false);
        var voice = menu.voice();
        Component sung = voice == null ? Component.translatable("gui.tribalpower.kettle.no_voice")
                : Component.translatable("gui.tribalpower.kettle.voice", Component.translatable("attunement.tribalpower." + voice.getSerializedName()));
        g.drawString(font, sung, 66, 18, TEAL, false);
        Component state = Component.translatable("gui.tribalpower.kettle.state." + menu.state());
        g.drawString(font, state, 66, 50, menu.state() == SpiritKettleBlockEntity.BREWING ? TEXT : 0xFFE3B55A, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
