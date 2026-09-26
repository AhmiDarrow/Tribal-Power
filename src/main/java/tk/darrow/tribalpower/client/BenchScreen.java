package tk.darrow.tribalpower.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import tk.darrow.tribalpower.bench.BenchMenu;

/**
 * The Tribal Bench's grid, drawn in the mod's own panel style rather than on a vanilla sheet, so it
 * sits beside the Echo stations instead of looking like a borrowed crafting table. The green recipe book works
 * here exactly as it does at a crafting table.
 */
public class BenchScreen extends AbstractContainerScreen<BenchMenu> implements RecipeUpdateListener {
    private static final int INK = 0xFF101B22, PANEL = 0xFF14262C, RIM = 0xFFB58A58;
    private static final int SLOT = 0xFF081317, SLOT_RIM = 0xFF385456;
    private static final int TEXT = 0xFFE7DCC1, QUIET = 0xFF98ACA5;
    private final RecipeBookComponent recipeBook = new RecipeBookComponent();
    private boolean widthTooNarrow;

    public BenchScreen(BenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        widthTooNarrow = width < 379;
        recipeBook.init(width, height, minecraft, widthTooNarrow, menu);
        leftPos = recipeBook.updateScreenPosition(width, imageWidth);
        addRenderableWidget(new ImageButton(leftPos + 5, height / 2 - 49, 20, 18, RecipeBookComponent.RECIPE_BUTTON_SPRITES, button -> {
            recipeBook.toggleVisibility();
            leftPos = recipeBook.updateScreenPosition(width, imageWidth);
            button.setPosition(leftPos + 5, height / 2 - 49);
        }));
        addWidget(recipeBook);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        recipeBook.tick();
    }

    @Override
    protected boolean isHovering(int x, int y, int w, int h, double mouseX, double mouseY) {
        return (!widthTooNarrow || !recipeBook.isVisible()) && super.isHovering(x, y, w, h, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (recipeBook.mouseClicked(mouseX, mouseY, button)) {
            setFocused(recipeBook);
            return true;
        }
        return widthTooNarrow && recipeBook.isVisible() || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        boolean outside = mouseX < left || mouseY < top || mouseX >= left + imageWidth || mouseY >= top + imageHeight;
        return recipeBook.hasClickedOutside(mouseX, mouseY, leftPos, topPos, imageWidth, imageHeight, button) && outside;
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int button, ClickType type) {
        super.slotClicked(slot, slotId, button, type);
        recipeBook.slotClicked(slot);
    }

    @Override
    public void recipesUpdated() {
        recipeBook.recipesUpdated();
    }

    @Override
    public RecipeBookComponent getRecipeBookComponent() {
        return recipeBook;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, INK);
        g.renderOutline(x, y, imageWidth, imageHeight, RIM);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 16, 0xFF20333A);
        g.fill(x + 24, y + 11, x + 92, y + 79, PANEL);                 // the work area
        g.renderOutline(x + 24, y + 11, 68, 68, 0xFF26414A);
        g.fill(x + 118, y + 29, x + 142, y + 53, 0xFF0E1D23);          // the result well
        g.renderOutline(x + 118, y + 29, 24, 24, 0xFF26414A);
        // The arrow from work to result.
        g.fill(x + 98, y + 40, x + 114, y + 42, 0xFF5D7E74);
        g.fill(x + 110, y + 37, x + 112, y + 45, 0xFF5D7E74);
        for (var slot : menu.slots) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, SLOT);
            g.renderOutline(x + slot.x - 1, y + slot.y - 1, 18, 18, SLOT_RIM);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 4, TEXT, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, QUIET, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (recipeBook.isVisible() && widthTooNarrow) {
            renderBackground(g, mouseX, mouseY, partialTick);
            recipeBook.render(g, mouseX, mouseY, partialTick);
        } else {
            super.render(g, mouseX, mouseY, partialTick);
            recipeBook.render(g, mouseX, mouseY, partialTick);
            recipeBook.renderGhostRecipe(g, leftPos, topPos, true, partialTick);
        }
        renderTooltip(g, mouseX, mouseY);
        recipeBook.renderTooltip(g, leftPos, topPos, mouseX, mouseY);
    }
}
