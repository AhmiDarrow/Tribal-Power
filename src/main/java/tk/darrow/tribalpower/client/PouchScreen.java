package tk.darrow.tribalpower.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
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

/**
 * The pouch, opened on its own: every reagent it holds with its icon, raw and empowered side by side, and a way to
 * take raw reagents back. Reagents the pouch has none of are tucked away until "show all" asks for them.
 */
public class PouchScreen extends AbstractContainerScreen<PouchMenu> {
    private static final int INK = 0xFF101B22, BAR = 0xFF20333A, WELL = 0xFF14262C, RIM = 0xFFB58A58;
    private static final int SLOT = 0xFF081317, SLOT_RIM = 0xFF385456;
    private static final int TEXT = 0xFFE7DCC1, QUIET = 0xFF98ACA5, TEAL = 0xFF65D7C0, GOLD = 0xFFE3B55A, DIM = 0xFF55686A;
    private static final int ROW = 14, VISIBLE = 5, LIST_TOP = 28, LIST_LEFT = 6;

    private static boolean showAll;
    private final List<Row> rows = new ArrayList<>();
    private int scroll;
    private int hovered = -1;
    /** The reagent last clicked; Take works on it, since the mouse leaves the list to reach the button. */
    private CreatureProfile selected;
    private long lastClick;
    private Button take, toggle;

    private record Row(Note header, CreatureProfile profile) {}

    public PouchScreen(PouchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = PouchMenu.WIDTH;
        imageHeight = PouchMenu.HEIGHT;
        inventoryLabelY = 103;
    }

    /** The rows to show now: note headers, each followed by its reagents (only the ones held, unless showing all). */
    private void rebuild() {
        rows.clear();
        ItemStack pouch = menu.pouch();
        for (var entry : Reagents.byNote().entrySet()) {
            List<Row> held = new ArrayList<>();
            for (CreatureProfile profile : entry.getValue())
                if (showAll || ReagentPouch.raw(pouch, profile) > 0 || ReagentPouch.empowered(pouch, profile) > 0) held.add(new Row(null, profile));
            if (held.isEmpty()) continue;
            rows.add(new Row(entry.getKey(), null));
            rows.addAll(held);
        }
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - VISIBLE)));
    }

    @Override
    protected void init() {
        super.init();
        take = addRenderableWidget(Button.builder(Component.translatable("gui.tribalpower.song.take"), b -> takeSelected())
                .bounds(leftPos + imageWidth - 70, topPos + 2, 64, 12).build());
        toggle = addRenderableWidget(Button.builder(Component.empty(), b -> {
            showAll = !showAll;
            rebuild();
        }).bounds(leftPos + imageWidth - 110, topPos + 2, 38, 12).build());
        rebuild();
    }

    private void takeSelected() {
        if (selected == null || minecraft == null || minecraft.gameMode == null) return;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SongBenchMenu.WITHDRAW + selected.ordinal());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        rebuild();
        int raw = selected == null ? 0 : ReagentPouch.raw(menu.pouch(), selected);
        take.active = raw > 0;
        take.setMessage(raw > 0 ? Component.translatable("gui.tribalpower.pouch.take", raw) : Component.translatable("gui.tribalpower.song.take"));
        toggle.setMessage(Component.translatable(showAll ? "gui.tribalpower.pouch.held_only" : "gui.tribalpower.pouch.show_all"));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, INK);
        g.renderOutline(x, y, imageWidth, imageHeight, RIM);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 16, BAR);
        // the list well, with a legend over its two count columns
        int wellTop = y + LIST_TOP - 2, wellBottom = y + LIST_TOP + VISIBLE * ROW + 1;
        g.fill(x + LIST_LEFT, wellTop, x + imageWidth - LIST_LEFT, wellBottom, WELL);
        g.renderOutline(x + LIST_LEFT, wellTop, imageWidth - LIST_LEFT * 2, wellBottom - wellTop, 0xFF26414A);
        drawSmall(g, Component.translatable("gui.tribalpower.pouch.raw").getString(), x + imageWidth - 58, y + 19, QUIET, true);
        drawSmall(g, Component.translatable("gui.tribalpower.pouch.empowered").getString(), x + imageWidth - 22, y + 19, GOLD, true);
        // the player's slots, framed the way the bench frames them
        for (var slot : menu.slots) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, SLOT);
            g.renderOutline(x + slot.x - 1, y + slot.y - 1, 18, 18, SLOT_RIM);
        }

        hovered = -1;
        ItemStack pouch = menu.pouch();
        if (rows.isEmpty()) {
            int cy = y + LIST_TOP + VISIBLE * ROW / 2 - 8;
            g.drawCenteredString(font, Component.translatable("gui.tribalpower.pouch.empty"), x + imageWidth / 2, cy, QUIET);
            g.drawCenteredString(font, Component.translatable("gui.tribalpower.pouch.empty_hint"), x + imageWidth / 2, cy + 11, DIM);
            return;
        }
        for (int i = 0; i < VISIBLE && scroll + i < rows.size(); i++) {
            Row row = rows.get(scroll + i);
            int ry = y + LIST_TOP + i * ROW;
            if (row.header != null) {
                drawSmall(g, Component.translatable("song.tribalpower.note." + row.header.name().toLowerCase(java.util.Locale.ROOT)).getString().toUpperCase(java.util.Locale.ROOT),
                        x + LIST_LEFT + 4, ry + 5, TEAL, false);
                g.fill(x + LIST_LEFT + 4, ry + ROW - 3, x + imageWidth - LIST_LEFT - 10, ry + ROW - 2, 0xFF26414A);
                continue;
            }
            boolean hot = mouseX >= x + LIST_LEFT + 1 && mouseX < x + imageWidth - LIST_LEFT - 6 && mouseY >= ry && mouseY < ry + ROW;
            if (hot) hovered = scroll + i;
            if (row.profile == selected) g.fill(x + LIST_LEFT + 1, ry, x + imageWidth - LIST_LEFT - 6, ry + ROW, 0xFF2A4A40);
            else if (hot) g.fill(x + LIST_LEFT + 1, ry, x + imageWidth - LIST_LEFT - 6, ry + ROW, 0xFF1C3338);
            int raw = ReagentPouch.raw(pouch, row.profile), empowered = ReagentPouch.empowered(pouch, row.profile);
            boolean any = raw > 0 || empowered > 0;
            g.pose().pushPose();
            g.pose().translate(x + LIST_LEFT + 3, ry + 1, 0);
            g.pose().scale(0.75F, 0.75F, 1);
            g.renderItem(new ItemStack(Reagents.item(row.profile)), 0, 0);
            g.pose().popPose();
            g.drawString(font, font.plainSubstrByWidth(Component.translatable("item.tribalpower." + row.profile.reagent).getString(), imageWidth - 100),
                    x + LIST_LEFT + 18, ry + 3, any ? TEXT : DIM, false);
            String rawText = String.valueOf(raw), empText = String.valueOf(empowered);
            g.drawString(font, rawText, x + imageWidth - 58 - font.width(rawText) / 2, ry + 3, raw > 0 ? TEXT : DIM, false);
            g.drawString(font, empText, x + imageWidth - 22 - font.width(empText) / 2, ry + 3, empowered > 0 ? GOLD : DIM, false);
        }
        // a thin scrollbar when the list runs longer than the well
        if (rows.size() > VISIBLE) {
            int trackTop = y + LIST_TOP, trackH = VISIBLE * ROW;
            int barH = Math.max(8, trackH * VISIBLE / rows.size());
            int barY = trackTop + (trackH - barH) * scroll / Math.max(1, rows.size() - VISIBLE);
            g.fill(x + imageWidth - LIST_LEFT - 4, trackTop, x + imageWidth - LIST_LEFT - 2, trackTop + trackH, 0xFF1C2A30);
            g.fill(x + imageWidth - LIST_LEFT - 4, barY, x + imageWidth - LIST_LEFT - 2, barY + barH, 0xFF5D7E74);
        }
    }

    private void drawSmall(GuiGraphics g, String text, float x, float y, int colour, boolean centred) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.75F, 0.75F, 1);
        g.drawString(font, text, centred ? -font.width(text) / 2 : 0, 0, colour, false);
        g.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 5, TEXT, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, QUIET, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hovered >= 0 && hovered < rows.size() && rows.get(hovered).profile != null) {
            CreatureProfile clicked = rows.get(hovered).profile;
            long now = System.currentTimeMillis();
            boolean again = clicked == selected && now - lastClick < 350;
            selected = clicked;
            lastClick = now;
            // shift-click or a double-click takes the raw reagents straight out
            if (Screen.hasShiftDown() || again) takeSelected();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, Math.min(scroll - (int) Math.signum(scrollY), Math.max(0, rows.size() - VISIBLE)));
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (hovered >= 0 && hovered < rows.size() && rows.get(hovered).profile != null)
            setTooltipForNextRenderPass(Component.translatable("gui.tribalpower.pouch.row_tip"));
        renderTooltip(g, mouseX, mouseY);
    }
}
