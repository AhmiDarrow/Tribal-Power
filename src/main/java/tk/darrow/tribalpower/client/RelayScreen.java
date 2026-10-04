package tk.darrow.tribalpower.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import tk.darrow.tribalpower.blockentity.WirelessRelayBlockEntity;
import tk.darrow.tribalpower.echo.RelayMenu;

/**
 * Rune on the left, the selected channel's eight-slot filter, and its controls: whitelist/blacklist, routing (once the
 * plate has more than one channel) and unlink. Channel tabs sit in the title bar; ranks open the locked ones.
 */
public class RelayScreen extends AbstractContainerScreen<RelayMenu> {
    private final Button[] tabs = new Button[WirelessRelayBlockEntity.MAX_CHANNELS];
    private Button mode, route, unlink;
    /** Everything the buttons show, so they are rebuilt only when the server's state changes. */
    private int shown = -1;

    public RelayScreen(RelayMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override protected void init() {
        super.init();
        for (int i = 0; i < tabs.length; i++) {
            int channel = i;
            tabs[i] = addRenderableWidget(Button.builder(Component.literal(String.valueOf(i + 1)), b -> press(RelayMenu.SELECT + channel))
                    .bounds(leftPos + 104 + i * 17, topPos + 3, 16, 13).build());
        }
        mode = addRenderableWidget(Button.builder(Component.empty(), b -> press(RelayMenu.TOGGLE_ALLOW))
                .bounds(leftPos + 117, topPos + 22, 52, 14).build());
        route = addRenderableWidget(Button.builder(Component.empty(), b -> press(RelayMenu.TOGGLE_ROUTE))
                .bounds(leftPos + 117, topPos + 38, 52, 14).build());
        unlink = addRenderableWidget(Button.builder(Component.empty(), b -> press(RelayMenu.UNLINK))
                .bounds(leftPos + 117, topPos + 54, 52, 14).build());
        shown = -1;
        refresh();
    }

    private int state() {
        int linked = 0;
        for (int i = 0; i < tabs.length; i++) if (menu.linked(i)) linked |= 1 << i;
        return menu.selected() | menu.channels() << 3 | (menu.allowing() ? 1 : 0) << 6 | (menu.roundRobin() ? 1 : 0) << 7 | linked << 8;
    }

    /** The mode and route live on the server and come back through the menu's data; follow them when they change. */
    private void refresh() {
        int now = state();
        if (now == shown || mode == null) return;
        shown = now;
        int channels = menu.channels(), selected = menu.selected();
        for (int i = 0; i < tabs.length; i++) {
            boolean open = i < channels;
            tabs[i].active = open;
            tabs[i].visible = channels > 1 || i == 0;
            ChatFormatting colour = i == selected ? ChatFormatting.YELLOW : menu.linked(i) ? ChatFormatting.WHITE : ChatFormatting.GRAY;
            tabs[i].setMessage(Component.literal(String.valueOf(i + 1)).withStyle(colour));
            tabs[i].setTooltip(Tooltip.create(open
                    ? Component.translatable(menu.linked(i) ? "gui.tribalpower.relay.channel.linked" : "gui.tribalpower.relay.channel.free", i + 1)
                    : Component.translatable("gui.tribalpower.relay.channel.locked")));
        }
        boolean allow = menu.allowing();
        mode.setMessage(Component.translatable(allow ? "gui.tribalpower.relay.allow" : "gui.tribalpower.relay.block"));
        mode.setTooltip(Tooltip.create(Component.translatable(allow ? "gui.tribalpower.relay.allow.hint" : "gui.tribalpower.relay.block.hint")));
        route.visible = channels > 1;
        boolean robin = menu.roundRobin();
        route.setMessage(Component.translatable(robin ? "gui.tribalpower.relay.round_robin" : "gui.tribalpower.relay.priority"));
        route.setTooltip(Tooltip.create(Component.translatable(robin ? "gui.tribalpower.relay.round_robin.hint" : "gui.tribalpower.relay.priority.hint")));
        boolean linked = menu.linked(selected);
        unlink.active = linked;
        unlink.setMessage(Component.translatable(linked ? "gui.tribalpower.relay.unlink" : "gui.tribalpower.relay.not_linked"));
        unlink.setTooltip(Tooltip.create(Component.translatable(linked ? "gui.tribalpower.relay.unlink.hint" : "gui.tribalpower.relay.not_linked.hint")));
    }

    @Override protected void containerTick() {
        super.containerTick();
        refresh();
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
        g.drawString(font, font.plainSubstrByWidth(title.getString(), menu.channels() > 1 ? 92 : 150), 8, 6, 0xFFE7DCC1, false);
        g.drawString(font, playerInventoryTitle, 8, 72, 0xFF98ACA5, false);
        g.drawString(font, Component.translatable("gui.tribalpower.relay.rune"), 12, 22, 0xFF98ACA5, false);
        boolean burns = menu.getSlot(RelayMenu.RUNE_SLOT).getItem().is(tk.darrow.tribalpower.item.ModItems.FIRE_SEAL.get());
        Component filter = burns ? Component.translatable("gui.tribalpower.relay.void_filter")
                : menu.channels() > 1
                ? Component.translatable("gui.tribalpower.relay.filter_channel", menu.selected() + 1)
                : Component.translatable("gui.tribalpower.relay.filter");
        g.drawString(font, filter, 42, 22, 0xFF98ACA5, false);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
