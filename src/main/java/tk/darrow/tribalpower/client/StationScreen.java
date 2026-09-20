package tk.darrow.tribalpower.client;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import tk.darrow.tribalpower.echo.ProcessingRecipes;
import tk.darrow.tribalpower.echo.StationMenu;
import tk.darrow.tribalpower.lattice.SideIo;

/**
 * An Echo station: what goes in and what it needs, the job's progress, what came out, and which faces
 * take and give items. A copper-and-ink panel drawn without external textures.
 */
public class StationScreen extends AbstractContainerScreen<StationMenu> {
    private static final int INK = 0xFF101B22, PANEL = 0xFF14262C, RIM = 0xFFB58A58, SLOT = 0xFF081317, SLOT_RIM = 0xFF385456;
    private static final int TEXT = 0xFFE7DCC1, QUIET = 0xFF98ACA5, TEAL = 0xFF65D7C0, AMBER = 0xFFE3B55A, RED = 0xFFE0674F;

    public StationScreen(StationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = StationMenu.WIDTH;
        imageHeight = StationMenu.HEIGHT;
        inventoryLabelX = StationMenu.INVENTORY_X;
        inventoryLabelY = StationMenu.INVENTORY_Y - 11;
    }

    private int ioX() { return leftPos + 168; }
    private int ioY() { return topPos + 34; }

    private ItemStack lookedUp = ItemStack.EMPTY;
    private ProcessingRecipes.Formula cached;

    /**
     * The job for the item in the input slot, worked out from the synced recipes. The lookup walks every
     * recipe in the pack, and both renderBg and renderLabels want it, so it is only redone when the input
     * changes rather than twice per frame.
     */
    private ProcessingRecipes.Formula formula() {
        if (minecraft == null || minecraft.level == null || menu.input().isEmpty()) {
            lookedUp = ItemStack.EMPTY;
            return cached = null;
        }
        ItemStack input = menu.input();
        if (!ItemStack.matches(input, lookedUp)) {
            lookedUp = input.copy();
            var state = minecraft.level.getBlockState(menu.machinePos());
            String station = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
            cached = ProcessingRecipes.find(minecraft.level, station, input);
        }
        return cached;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, INK);
        g.renderOutline(x, y, imageWidth, imageHeight, RIM);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 20, 0xFF20333A);
        g.fill(x + 6, y + 22, x + imageWidth - 6, y + 112, PANEL);
        g.renderOutline(x + 6, y + 22, imageWidth - 12, 90, 0xFF26414A);
        // The side pad sits in its own well, apart from the work.
        g.fill(x + 161, y + 24, x + imageWidth - 8, y + 88, 0xFF0E1D23);
        for (var slot : menu.slots) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, SLOT);
            g.renderOutline(x + slot.x - 1, y + slot.y - 1, 18, 18, SLOT_RIM);
        }
        ProcessingRecipes.Formula recipe = formula();
        // Catalysts the job wants, ghosted in the slots that are still short.
        List<ItemStack> wants = recipe == null ? List.of() : recipe.catalysts();
        for (int i = 0; i < 2; i++) {
            var slot = menu.slots.get(9 + i);
            if (i < wants.size() && !slot.hasItem()) {
                g.renderFakeItem(wants.get(i), x + slot.x, y + slot.y);
                g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xAA081317);
                g.drawString(font, String.valueOf(wants.get(i).getCount()), x + slot.x + 17 - font.width(String.valueOf(wants.get(i).getCount())), y + slot.y + 9, AMBER, true);
            }
        }
        // Progress: an arrow that fills as the job runs.
        int ax = x + 38, ay = y + 41, length = 32;
        g.fill(ax, ay, ax + length, ay + 6, 0xFF263A40);
        int done = menu.state().equals("idle") ? 0 : length * Math.min(menu.work(), menu.duration()) / menu.duration();
        g.fill(ax, ay, ax + done, ay + 6, TEAL);
        for (int i = 0; i < 5; i++) g.fill(ax + length + i, ay - 2 + i, ax + length + i + 1, ay + 8 - i, done >= length ? TEAL : 0xFF3A5157);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 7, TEXT, false);
        ProcessingRecipes.Formula recipe = formula();
        if (recipe != null) {
            Component voice = Component.translatable("attunement.tribalpower." + recipe.attunement().getSerializedName());
            int colour = 0xFF000000 | VoiceGlow.colour(recipe.attunement());
            int w = font.width(voice) + 10;
            g.fill(imageWidth - 8 - w, 5, imageWidth - 8, 16, 0xFF0E1D23);
            g.renderOutline(imageWidth - 8 - w, 5, w, 11, colour);
            g.drawString(font, voice, imageWidth - 3 - w, 7, colour, false);
        }
        g.drawString(font, Component.translatable("gui.tribalpower.station.input"), 14, 25, QUIET, false);
        g.drawString(font, Component.translatable("gui.tribalpower.station.catalyst"), 14, 59, QUIET, false);
        g.drawString(font, Component.translatable("gui.tribalpower.station.output"), 76, 25, QUIET, false);
        g.drawString(font, Component.translatable("gui.tribalpower.station.sides"), 164, 25, QUIET, false);
        // Status: a coloured dot, what the station is doing, and what the job costs.
        String state = menu.state();
        int dot = switch (state) {
            case "working" -> TEAL;
            case "idle" -> QUIET;
            case "paused" -> RED;
            default -> AMBER;
        };
        g.fill(14, 94, 19, 99, dot);
        Component status = Component.translatable(menu.statusKey(), menu.work());
        g.drawString(font, font.plainSubstrByWidth(status.getString(), 138), 23, 93, dot, false);
        if (recipe != null) {
            int left = Math.max(0, menu.duration() - menu.work());
            Component cost = Component.translatable("gui.tribalpower.station.cost", menu.pulsePerSecond(), left);
            g.drawString(font, cost, 76, 74, QUIET, false);
        }
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, QUIET, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        SideIo io = new SideIo(SideIo.Mode.BOTH);
        io.unpack(menu.ioPacked());
        renderTooltip(g, mouseX, mouseY);
        SideIoWidget.render(g, font, ioX(), ioY(), io, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (SideIoWidget.click(mouseX, mouseY, ioX(), ioY(), menu.machinePos())) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
