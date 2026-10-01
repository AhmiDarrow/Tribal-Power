package tk.darrow.tribalpower.integration;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import tk.darrow.tribalpower.guardian.Guardian;
import tk.darrow.tribalpower.rite.world.WorldRite;
import tk.darrow.tribalpower.song.Anointment;

/**
 * The words the recipe viewers print, and the wrapping that keeps them inside a panel: a line longer than the room
 * it has breaks onto the next, and each panel is tall enough for its longest text. Client only; both viewer
 * plugins use it so their panels read the same.
 */
public final class ViewerText {
    private ViewerText() {}

    /** One line of text and the pixel of space under it. */
    public static final int LINE = 10;

    public static Component recipeCost(int seconds, int pulse) { return Component.translatable("gui.tribalpower.recipe_cost", seconds, pulse); }
    /**
     * An Echo station recipe's cost as the station really spends it: the pack's consumption setting applied to the
     * Pulse a second, rounded as the station rounds it, then times the seconds. Unranked and without an array discount.
     */
    public static Component stationCost(int seconds, int pulsePerSecond) {
        return recipeCost(seconds, seconds * tk.darrow.tribalpower.config.TribalConfig.scaleConsumption(pulsePerSecond));
    }
    public static Component hearthSeconds(int seconds) { return Component.translatable("gui.tribalpower.hearth_pot.seconds", seconds); }
    public static Component riteName(WorldRite rite) { return Component.translatable("item.tribalpower." + rite.tabletId()); }
    public static Component riteCircle(WorldRite rite) {
        return Component.translatable("gui.tribalpower.rite.circle", Component.translatable("attunement.tribalpower." + rite.element().getSerializedName()), rite.cost());
    }
    public static Component guardianName(Guardian guardian) { return Component.translatable(guardian.nameKey()); }
    public static Component guardianBiome(Guardian guardian) { return Component.translatable("biome.tribalpower." + guardian.biome); }
    public static Component anointName(Anointment anointment) { return Component.translatable(CompatDisplays.anointmentKey(anointment)); }
    public static Component anointCost(int reagents, int pulse) { return Component.translatable("gui.tribalpower.anoint.cost", reagents, pulse); }

    /** The text broken into lines no wider than {@code width}. */
    public static List<FormattedCharSequence> wrap(Component text, int width) { return Minecraft.getInstance().font.split(text, width); }

    /** How many lines the text takes at this width; never fewer than one, so an empty string still keeps its row. */
    public static int lines(Component text, int width) { return Math.max(1, wrap(text, width).size()); }

    /** Draws the text wrapped to {@code width} from (x, y) down; returns the y under its last line. */
    public static int draw(GuiGraphics g, Component text, int x, int y, int width, int colour) {
        List<FormattedCharSequence> lines = wrap(text, width);
        for (int i = 0; i < lines.size(); i++) g.drawString(Minecraft.getInstance().font, lines.get(i), x, y + i * LINE, colour, false);
        return y + Math.max(1, lines.size()) * LINE;
    }
}
