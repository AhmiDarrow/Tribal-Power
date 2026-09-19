package tk.darrow.tribalpower.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import tk.darrow.tribalpower.lattice.SideIo;
import tk.darrow.tribalpower.lattice.SideIoPayload;

/**
 * Six-face IO pad shared by station and cache screens: a cross of faces (up, the four sides, down beneath),
 * each coloured by what it does, with a legend under it. Click a face to cycle it.
 */
public final class SideIoWidget {
    private static final Direction[] FACES = {Direction.UP, Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.DOWN};
    private static final int CELL = 10, STEP = 11;
    private static final int[] OX = {STEP, 0, STEP, 2 * STEP, STEP, STEP};
    private static final int[] OY = {0, STEP, STEP, STEP, 2 * STEP, 3 * STEP};
    private static final SideIo.Mode[] LEGEND = {SideIo.Mode.INPUT, SideIo.Mode.OUTPUT, SideIo.Mode.BOTH, SideIo.Mode.NONE};

    private SideIoWidget() {}

    public static void render(GuiGraphics g, Font font, int bx, int by, SideIo io, int mouseX, int mouseY) {
        int hovered = -1;
        for (int i = 0; i < FACES.length; i++) {
            int px = bx + OX[i], py = by + OY[i];
            boolean hot = mouseX >= px && mouseX < px + CELL && mouseY >= py && mouseY < py + CELL;
            if (hot) hovered = i;
            g.fill(px, py, px + CELL, py + CELL, colour(io.get(FACES[i])));
            g.renderOutline(px, py, CELL, CELL, hot ? 0xFFE4C18A : 0xFF0A1418);
            String letter = FACES[i].getSerializedName().substring(0, 1).toUpperCase();
            g.drawString(font, letter, px + (CELL - font.width(letter)) / 2 + 1, py + 1, 0xFFF2EAD5, true);
        }
        // Legend: the four modes as small swatches under the pad.
        int ly = by + 4 * STEP + 2;
        for (int i = 0; i < LEGEND.length; i++) {
            int lx = bx + i * 8;
            g.fill(lx, ly, lx + 6, ly + 6, colour(LEGEND[i]));
            g.renderOutline(lx, ly, 6, 6, 0xFF0A1418);
        }
        if (hovered >= 0)
            g.renderComponentTooltip(font, java.util.List.of(
                    Component.translatable("gui.tribalpower.io.face", FACES[hovered].getSerializedName(), io.get(FACES[hovered]).label()),
                    Component.translatable("gui.tribalpower.io.hint").withStyle(net.minecraft.ChatFormatting.DARK_GRAY)), mouseX, mouseY);
        else for (int i = 0; i < LEGEND.length; i++) {
            int lx = bx + i * 8;
            if (mouseX >= lx && mouseX < lx + 6 && mouseY >= ly && mouseY < ly + 6)
                g.renderTooltip(font, LEGEND[i].label(), mouseX, mouseY);
        }
    }

    public static boolean click(double mouseX, double mouseY, int bx, int by, BlockPos pos) {
        for (int i = 0; i < FACES.length; i++) {
            int px = bx + OX[i], py = by + OY[i];
            if (mouseX >= px && mouseX < px + CELL && mouseY >= py && mouseY < py + CELL) {
                PacketDistributor.sendToServer(new SideIoPayload(pos, FACES[i].get3DDataValue()));
                return true;
            }
        }
        return false;
    }

    public static int colour(SideIo.Mode mode) {
        return switch (mode) {
            case BOTH -> 0xFF3F8C7C;
            case INPUT -> 0xFF3F74C8;
            case OUTPUT -> 0xFFD29A4A;
            case NONE -> 0xFF26343B;
        };
    }
}
