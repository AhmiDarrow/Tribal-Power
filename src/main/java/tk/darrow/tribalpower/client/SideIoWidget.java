package tk.darrow.tribalpower.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import tk.darrow.tribalpower.lattice.SideIo;
import tk.darrow.tribalpower.lattice.SideIoPayload;

/** Six-face IO pad shared by station and cache screens. */
public final class SideIoWidget {
    private static final Direction[] FACES = {Direction.UP, Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.DOWN};
    private static final int[] OX = {12, 0, 12, 24, 12, 12};
    private static final int[] OY = {0, 12, 12, 12, 24, 36};

    private SideIoWidget() {}

    public static void render(GuiGraphics g, Font font, int bx, int by, SideIo io, int mouseX, int mouseY) {
        for (int i = 0; i < FACES.length; i++) {
            int px = bx + OX[i], py = by + OY[i];
            g.fill(px, py, px + 11, py + 11, colour(io.get(FACES[i])));
            g.renderOutline(px, py, 11, 11, 0xFF385456);
            g.drawString(font, FACES[i].getSerializedName().substring(0, 1).toUpperCase(), px + 3, py + 2, 0xFFE7DCC1, false);
        }
        for (int i = 0; i < FACES.length; i++) {
            int px = bx + OX[i], py = by + OY[i];
            if (mouseX >= px && mouseX < px + 11 && mouseY >= py && mouseY < py + 11)
                g.renderTooltip(font, Component.translatable("gui.tribalpower.io.face", FACES[i].getSerializedName(), io.get(FACES[i]).label()), mouseX, mouseY);
        }
    }

    public static boolean click(double mouseX, double mouseY, int bx, int by, BlockPos pos) {
        for (int i = 0; i < FACES.length; i++) {
            int px = bx + OX[i], py = by + OY[i];
            if (mouseX >= px && mouseX < px + 11 && mouseY >= py && mouseY < py + 11) {
                PacketDistributor.sendToServer(new SideIoPayload(pos, FACES[i].get3DDataValue()));
                return true;
            }
        }
        return false;
    }

    public static int colour(SideIo.Mode mode) {
        return switch (mode) {
            case BOTH -> 0xFF3D6B62;
            case INPUT -> 0xFF3A6EB5;
            case OUTPUT -> 0xFFC49A5A;
            case NONE -> 0xFF1A2830;
        };
    }
}
