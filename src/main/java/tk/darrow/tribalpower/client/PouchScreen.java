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
import tk.darrow.tribalpower.song.PouchMenu;
import tk.darrow.tribalpower.song.ReagentPouch;
import tk.darrow.tribalpower.song.Reagents;
import tk.darrow.tribalpower.song.SongBenchMenu;

/** The pouch, opened on its own: counts, and a way to take raw reagents back. */
public class PouchScreen extends AbstractContainerScreen<PouchMenu> {
    private static final int ROW = 12, VISIBLE = 7;
    private final List<SongBenchScreenRow> rows = new ArrayList<>();
    private int scroll;
    private int hovered = -1;

    public PouchScreen(PouchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = PouchMenu.WIDTH;
        imageHeight = PouchMenu.HEIGHT;
        inventoryLabelY = 102;
        for (var entry : Reagents.byNote().entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            rows.add(new SongBenchScreenRow(entry.getKey(), null));
            for (CreatureProfile profile : entry.getValue()) rows.add(new SongBenchScreenRow(null, profile));
        }
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.tribalpower.song.take"), b -> {
            if (hovered < 0 || hovered >= rows.size() || rows.get(hovered).profile == null || minecraft == null || minecraft.gameMode == null) return;
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SongBenchMenu.WITHDRAW + rows.get(hovered).profile.ordinal());
        }).bounds(leftPos + 112, topPos + 78, 72, 16).build());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF101B22);
        g.renderOutline(x, y, imageWidth, imageHeight, 0xFFB58A58);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 16, 0xFF20333A);
        g.fill(x + 6, y + 20, x + imageWidth - 6, y + 20 + VISIBLE * ROW + 4, 0xFF14262C);
        hovered = -1;
        ItemStack pouch = menu.pouch();
        int start = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - VISIBLE)));
        for (int i = 0; i < VISIBLE && start + i < rows.size(); i++) {
            var row = rows.get(start + i);
            int ry = y + 22 + i * ROW;
            if (row.profile != null && mouseX >= x + 8 && mouseX < x + imageWidth - 8 && mouseY >= ry && mouseY < ry + ROW) {
                hovered = start + i;
                g.fill(x + 8, ry, x + imageWidth - 8, ry + ROW, 0xFF1C3338);
            }
            if (row.header != null) {
                g.drawString(font, Component.translatable("song.tribalpower.note." + row.header.name().toLowerCase(java.util.Locale.ROOT)), x + 10, ry + 2, 0xFF65D7C0, false);
            } else {
                int raw = ReagentPouch.raw(pouch, row.profile);
                int empowered = ReagentPouch.empowered(pouch, row.profile);
                g.drawString(font, Component.translatable("item.tribalpower." + row.profile.reagent), x + 10, ry + 2, 0xFFE7DCC1, false);
                String counts = raw + "/" + empowered;
                g.drawString(font, counts, x + imageWidth - 12 - font.width(counts), ry + 2, empowered > 0 ? 0xFFE3B55A : 0xFF98ACA5, false);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 5, 0xFFE7DCC1, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xFF98ACA5, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, Math.min(scroll - (int) Math.signum(scrollY), Math.max(0, rows.size() - VISIBLE)));
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    private record SongBenchScreenRow(Note header, CreatureProfile profile) {}
}
