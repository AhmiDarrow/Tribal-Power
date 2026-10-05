package tk.darrow.tribalpower.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import tk.darrow.tribalpower.blockentity.PulseResonatorBlockEntity;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.leyheart.LeyHeartBlockEntity;
import tk.darrow.tribalpower.leyheart.LeyHeartMenu;
import tk.darrow.tribalpower.song.ReagentThread;

import java.util.ArrayList;
import java.util.List;

/**
 * The Ley Heart: crystal and reagent with their burn, the water, and what the star is singing. The fuel line says
 * what each burning fuel is worth in quarters and the harmony they make together; hovering it names the crystal,
 * the reagent's Thread, the machine rank and the lines raised.
 */
public class LeyHeartScreen extends AbstractContainerScreen<LeyHeartMenu> {
    private static final int INK = 0xFF101B22, PANEL = 0xFF14262C, RIM = 0xFFB58A58;
    private static final int SLOT = 0xFF081317, SLOT_RIM = 0xFF385456, TEXT = 0xFFE7DCC1, QUIET = 0xFF98ACA5, TEAL = 0xFF65D7C0;
    private static final int WARN = 0xFFE3B55A, WATER = 0xFF3A8FD9, EMBER = 0xFFE0703A, VIOLET = 0xFFB98AF0;
    private static final int TANK_X = 42, TANK_Y = 19, TANK_W = 14, TANK_H = 47;
    private static final int READ_X = 66;

    public LeyHeartScreen(LeyHeartMenu menu, Inventory inventory, Component title) {
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
        // The panel stops above the inventory label, as the Kettle's and the Hearth Pot's do.
        g.fill(x + 8, y + 15, x + 168, y + 70, PANEL);
        g.renderOutline(x + 8, y + 15, 160, 55, 0xFF26414A);
        for (var slot : menu.slots) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, SLOT);
            g.renderOutline(x + slot.x - 1, y + slot.y - 1, 18, 18, SLOT_RIM);
        }
        // How much of the burning crystal and reagent is left.
        bar(g, x + LeyHeartMenu.CRYSTAL_X - 1, y + LeyHeartMenu.CRYSTAL_Y + 19,
                menu.data(LeyHeartBlockEntity.DATA_CRYSTAL), menu.data(LeyHeartBlockEntity.DATA_CRYSTAL_TOTAL), VIOLET);
        bar(g, x + LeyHeartMenu.REAGENT_X - 1, y + LeyHeartMenu.REAGENT_Y + 19,
                menu.data(LeyHeartBlockEntity.DATA_REAGENT), menu.data(LeyHeartBlockEntity.DATA_REAGENT_TOTAL), EMBER);
        // The water, filling from the bottom.
        g.fill(x + TANK_X, y + TANK_Y, x + TANK_X + TANK_W, y + TANK_Y + TANK_H, SLOT);
        int water = menu.data(LeyHeartBlockEntity.DATA_WATER);
        int filled = (int) ((long) TANK_H * Math.max(0, water) / LeyHeartBlockEntity.TANK_CAPACITY);
        if (filled > 0) g.fill(x + TANK_X, y + TANK_Y + TANK_H - filled, x + TANK_X + TANK_W, y + TANK_Y + TANK_H, WATER);
        for (int notch = 1; notch < 4; notch++)
            g.fill(x + TANK_X, y + TANK_Y + notch * TANK_H / 4, x + TANK_X + 4, y + TANK_Y + notch * TANK_H / 4 + 1, SLOT_RIM);
        g.renderOutline(x + TANK_X - 1, y + TANK_Y - 1, TANK_W + 2, TANK_H + 2, SLOT_RIM);
        // Six beads for the six totems: lit for each one answering.
        int answered = menu.data(LeyHeartBlockEntity.DATA_ANSWERED);
        for (int i = 0; i < 6; i++)
            g.fill(x + READ_X + i * 8, y + 30, x + READ_X + i * 8 + 5, y + 34, i < answered ? TEAL : 0xFF26414A);
    }

    private static void bar(GuiGraphics g, int x, int y, int left, int total, int colour) {
        g.fill(x, y, x + 18, y + 2, 0xFF20333A);
        if (total > 0 && left > 0) g.fill(x, y, x + Math.max(1, 18 * left / total), y + 2, colour);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 4, TEXT, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, QUIET, false);
        int ley = menu.data(LeyHeartBlockEntity.DATA_LEY);
        g.drawString(font, Component.translatable("gui.tribalpower.ley_heart.ley", ley, tk.darrow.tribalpower.ley.LeyMath.MAX_GAIN), READ_X, 19, TEAL, false);
        g.drawString(font, Component.translatable("gui.tribalpower.ley_heart.totems", menu.data(LeyHeartBlockEntity.DATA_ANSWERED)), READ_X + 50, 28, QUIET, false);
        g.drawString(font, Component.translatable("gui.tribalpower.ley_heart.rate",
                menu.wide(LeyHeartBlockEntity.DATA_OUTPUT, LeyHeartBlockEntity.DATA_OUTPUT_HI)), READ_X, 38, TEXT, false);
        int rank = menu.data(LeyHeartBlockEntity.DATA_CRYSTAL_RANK), thread = menu.data(LeyHeartBlockEntity.DATA_REAGENT_THREAD) - 1;
        boolean water = menu.data(LeyHeartBlockEntity.DATA_WATER) >= LeyHeartBlockEntity.WATER_PER_BEAT;
        g.drawString(font, Component.translatable("gui.tribalpower.ley_heart.worth",
                PulseResonatorBlockEntity.quarters(rank), thread < 0 ? 0 : LeyHeartBlockEntity.reagentQuarters(thread),
                water ? LeyHeartBlockEntity.WATER_QUARTERS : 0, String.valueOf(LeyHeartBlockEntity.harmony(kinds(rank, thread, water)))),
                READ_X, 48, VIOLET, false);
        int state = menu.data(LeyHeartBlockEntity.DATA_STATE);
        g.drawString(font, Component.translatable("gui.tribalpower.ley_heart.state." + state), READ_X, 58,
                state == LeyHeartBlockEntity.STATE_SINGING ? TEXT : WARN, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= TANK_X && rx < TANK_X + TANK_W && ry >= TANK_Y && ry < TANK_Y + TANK_H) {
            g.renderTooltip(font, Component.translatable("gui.tribalpower.ley_heart.water",
                    menu.data(LeyHeartBlockEntity.DATA_WATER), LeyHeartBlockEntity.TANK_CAPACITY,
                    LeyHeartBlockEntity.WATER_PER_BEAT), mouseX, mouseY);
            return;
        }
        if (rx >= READ_X && rx < READ_X + 100 && ry >= 37 && ry < 47) {
            g.renderTooltip(font, Component.translatable("gui.tribalpower.ley_heart.stored",
                    menu.wide(LeyHeartBlockEntity.DATA_STORED, LeyHeartBlockEntity.DATA_STORED_HI), LeyHeartBlockEntity.CAPACITY),
                    mouseX, mouseY);
            return;
        }
        if (rx >= READ_X && rx < READ_X + 100 && ry >= 47 && ry < 57) {
            g.renderComponentTooltip(font, worth(), mouseX, mouseY);
            return;
        }
        if (burnTip(g, mouseX, mouseY, LeyHeartMenu.CRYSTAL_X, LeyHeartMenu.CRYSTAL_Y, LeyHeartBlockEntity.DATA_CRYSTAL, "crystal")) return;
        if (burnTip(g, mouseX, mouseY, LeyHeartMenu.REAGENT_X, LeyHeartMenu.REAGENT_Y, LeyHeartBlockEntity.DATA_REAGENT, "reagent")) return;
        renderTooltip(g, mouseX, mouseY);
    }

    private static int kinds(int rank, int thread, boolean water) {
        return (rank > 0 ? 1 : 0) + (thread >= 0 ? 1 : 0) + (water ? 1 : 0);
    }

    /** What drives the output, fuel by fuel: the crystal, the reagent's Thread, the water, harmony, rank, lines. */
    private List<Component> worth() {
        int rank = menu.data(LeyHeartBlockEntity.DATA_CRYSTAL_RANK), thread = menu.data(LeyHeartBlockEntity.DATA_REAGENT_THREAD) - 1;
        boolean water = menu.data(LeyHeartBlockEntity.DATA_WATER) >= LeyHeartBlockEntity.WATER_PER_BEAT;
        Component none = Component.translatable("gui.tribalpower.ley_heart.worth.none");
        Item crystal = switch (rank) {
            case 1 -> ModItems.ECHO_SHARD.get();
            case 2 -> ModItems.ATTUNED_ECHO.get();
            case 3 -> ModItems.BOUND_ECHO.get();
            case 4 -> ModItems.RESONANT_CORE.get();
            default -> null;
        };
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.tribalpower.ley_heart.worth.crystal", crystal == null ? none : crystal.getDescription(),
                PulseResonatorBlockEntity.quarters(rank)).withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("gui.tribalpower.ley_heart.worth.reagent", thread < 0 ? none : ReagentThread.name(thread),
                thread < 0 ? 0 : LeyHeartBlockEntity.reagentQuarters(thread)).withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("gui.tribalpower.ley_heart.worth.water", water ? LeyHeartBlockEntity.WATER_QUARTERS : 0)
                .withStyle(ChatFormatting.AQUA));
        int kinds = kinds(rank, thread, water);
        lines.add(Component.translatable("gui.tribalpower.ley_heart.worth.harmony", String.valueOf(LeyHeartBlockEntity.harmony(kinds)), kinds)
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("gui.tribalpower.ley_heart.worth.rank", 15 * menu.data(LeyHeartBlockEntity.DATA_RANK))
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("gui.tribalpower.ley_heart.threads", menu.data(LeyHeartBlockEntity.DATA_THREADS))
                .withStyle(ChatFormatting.DARK_AQUA));
        return lines;
    }

    /** The burn bar under a slot says how long the fuel already lit has left. */
    private boolean burnTip(GuiGraphics g, int mouseX, int mouseY, int slotX, int slotY, int index, String what) {
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx < slotX - 1 || rx >= slotX + 17 || ry < slotY + 18 || ry >= slotY + 22) return false;
        int left = menu.data(index);
        g.renderTooltip(font, Component.translatable("gui.tribalpower.ley_heart.burn." + what, left / 60, String.format("%02d", left % 60)),
                mouseX, mouseY);
        return true;
    }
}
