package tk.darrow.tribalpower.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import tk.darrow.tribalpower.echo.RelayMenu;

/** Rune on the left, the eight-slot filter, and a button that flips it between whitelist and blacklist. */
public class RelayScreen extends AbstractContainerScreen<RelayMenu> {
    private Button mode;
    private boolean shownAllow;

    public RelayScreen(RelayMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override protected void init() {
        super.init();
        shownAllow = menu.allowing();
        mode = addRenderableWidget(Button.builder(modeLabel(shownAllow), b -> {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, RelayMenu.TOGGLE_ALLOW);
        }).bounds(leftPos + 117, topPos + 38, 52, 20).tooltip(Tooltip.create(modeHint(shownAllow))).build());
    }

    private static Component modeLabel(boolean allow) {
        return Component.translatable(allow ? "gui.tribalpower.relay.allow" : "gui.tribalpower.relay.block");
    }

    private static Component modeHint(boolean allow) {
        return Component.translatable(allow ? "gui.tribalpower.relay.allow.hint" : "gui.tribalpower.relay.block.hint");
    }

    @Override protected void containerTick() {
        super.containerTick();
        // The mode lives on the server and comes back through the menu's data; follow it when it changes.
        if (mode != null && menu.allowing() != shownAllow) {
            shownAllow = menu.allowing();
            mode.setMessage(modeLabel(shownAllow));
            mode.setTooltip(Tooltip.create(modeHint(shownAllow)));
        }
    }

    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + 176, y + 166, 0xFF101B22);
        g.renderOutline(x, y, 176, 166, 0xFFB58A58);
        g.fill(x + 1, y + 1, x + 175, y + 18, 0xFF20333A);
        g.fill(x + 8, y + 20, x + 168, y + 70, 0xFF14262C);
        for (var slot : menu.slots) {
            boolean ghost = RelayMenu.isFilter(menu.slots.indexOf(slot));
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, ghost ? 0xFF0D1C21 : 0xFF081317);
            g.renderOutline(x + slot.x - 1, y + slot.y - 1, 18, 18, ghost ? (menu.allowing() ? 0xFF3F8C7C : 0xFF8C4A3F) : 0xFF385456);
        }
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, 0xFFE7DCC1, false);
        g.drawString(font, playerInventoryTitle, 8, 72, 0xFF98ACA5, false);
        g.drawString(font, Component.translatable("gui.tribalpower.relay.rune"), 12, 22, 0xFF98ACA5, false);
        g.drawString(font, Component.translatable("gui.tribalpower.relay.filter"), 42, 22, 0xFF98ACA5, false);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
