package tk.darrow.tribalpower.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import tk.darrow.tribalpower.entity.CreatureProfile;
import tk.darrow.tribalpower.song.Note;
import tk.darrow.tribalpower.song.ReagentPouch;
import tk.darrow.tribalpower.song.Reagents;
import tk.darrow.tribalpower.song.SongBenchMenu;

/** The Song Bench: a scroll of the pouch, the verse being written, and the four seats. */
public class SongBenchScreen extends AbstractContainerScreen<SongBenchMenu> {
    private static final int INK = 0xFF101B22, PANEL = 0xFF14262C, RIM = 0xFFB58A58;
    private static final int SLOT = 0xFF081317, SLOT_RIM = 0xFF385456, TEXT = 0xFFE7DCC1, QUIET = 0xFF98ACA5, TEAL = 0xFF65D7C0;
    private static final int ROW = 12, VISIBLE = 7;
    private final List<Row> rows = new ArrayList<>();
    private int scroll;
    private int hovered = -1;

    public SongBenchScreen(SongBenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = SongBenchMenu.WIDTH;
        imageHeight = SongBenchMenu.HEIGHT;
        inventoryLabelX = SongBenchMenu.INVENTORY_X;
        inventoryLabelY = SongBenchMenu.INVENTORY_Y - 11;
        for (var entry : Reagents.byNote().entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            rows.add(new Row(entry.getKey(), null));
            for (CreatureProfile profile : entry.getValue()) rows.add(new Row(null, profile));
        }
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos + 156;
        int y = topPos + 24;
        addRenderableWidget(Button.builder(Component.translatable("gui.tribalpower.song.empower"), b -> send(SongBenchMenu.EMPOWER)).bounds(x, y, 84, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.tribalpower.song.add"), b -> send(SongBenchMenu.APPEND)).bounds(x, y + 16, 84, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.tribalpower.song.fletch"), b -> send(SongBenchMenu.FLETCH)).bounds(x, y + 32, 84, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.tribalpower.song.take"), b -> send(SongBenchMenu.WITHDRAW)).bounds(x, y + 48, 84, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.tribalpower.song.write"), b -> click(SongBenchMenu.WRITE)).bounds(x, y + 66, 84, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.tribalpower.song.voice"), b -> click(SongBenchMenu.CYCLE)).bounds(x, y + 82, 40, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.tribalpower.song.clear"), b -> click(SongBenchMenu.CLEAR)).bounds(x + 42, y + 82, 42, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.tribalpower.song.lift"), b -> click(SongBenchMenu.LIFT)).bounds(leftPos + 74, topPos + 124, 52, 16).build());
    }

    private void send(int base) {
        if (hovered < 0 || hovered >= rows.size() || rows.get(hovered).profile == null) return;
        click(base + rows.get(hovered).profile.ordinal());
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, INK);
        g.renderOutline(x, y, imageWidth, imageHeight, RIM);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 18, 0xFF20333A);
        g.fill(x + 6, y + 22, x + 150, y + 22 + VISIBLE * ROW + 4, PANEL);
        g.renderOutline(x + 6, y + 22, 144, VISIBLE * ROW + 4, 0xFF26414A);
        hovered = -1;
        ItemStack pouch = minecraft == null || minecraft.player == null ? ItemStack.EMPTY : itemPouch();
        int start = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - VISIBLE)));
        for (int i = 0; i < VISIBLE && start + i < rows.size(); i++) {
            Row row = rows.get(start + i);
            int ry = y + 24 + i * ROW;
            boolean hot = mouseX >= x + 8 && mouseX < x + 148 && mouseY >= ry && mouseY < ry + ROW;
            if (hot && row.profile != null) {
                hovered = start + i;
                g.fill(x + 8, ry, x + 148, ry + ROW, 0xFF1C3338);
            }
            if (row.header != null) {
                g.drawString(font, Component.translatable("song.tribalpower.note." + row.header.name().toLowerCase(java.util.Locale.ROOT)), x + 10, ry + 2, TEAL, false);
            } else {
                int raw = pouch.isEmpty() ? 0 : ReagentPouch.raw(pouch, row.profile);
                int empowered = pouch.isEmpty() ? 0 : ReagentPouch.empowered(pouch, row.profile);
                g.drawString(font, Component.translatable("item.tribalpower." + row.profile.reagent), x + 10, ry + 2, TEXT, false);
                String counts = raw + "/" + empowered;
                g.drawString(font, counts, x + 146 - font.width(counts), ry + 2, empowered > 0 ? 0xFFE3B55A : QUIET, false);
            }
        }
        for (var slot : menu.slots) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, SLOT);
            g.renderOutline(x + slot.x - 1, y + slot.y - 1, 18, 18, SLOT_RIM);
        }
        int sx = x + 8;
        for (int i = 0; i < 7; i++) {
            g.fill(sx + i * 18, y + 112, sx + i * 18 + 16, y + 122, SLOT);
            CreatureProfile profile = menu.reagentAt(i);
            if (profile != null) g.renderItem(new ItemStack(Reagents.item(profile)), sx + i * 18, y + 108);
        }
    }

    private ItemStack itemPouch() {
        ItemStack pouch = Reagents.hotbarPouch(minecraft.player);
        return pouch == null ? ItemStack.EMPTY : pouch;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, TEXT, false);
        var voice = menu.voice();
        Component voiceName = voice == null
                ? Component.translatable("gui.tribalpower.song.no_voice")
                : Component.translatable("attunement.tribalpower." + voice.getSerializedName());
        g.drawString(font, voiceName, 156, 112, TEAL, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, QUIET, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= leftPos + 6 && mouseX <= leftPos + 150 && mouseY >= topPos + 22 && mouseY <= topPos + 22 + VISIBLE * ROW) {
            scroll = Math.max(0, Math.min(scroll - (int) Math.signum(scrollY), Math.max(0, rows.size() - VISIBLE)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hovered >= 0) {
            send(hasShiftDown() ? SongBenchMenu.EMPOWER : SongBenchMenu.APPEND);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    private record Row(Note header, CreatureProfile profile) {}
}
