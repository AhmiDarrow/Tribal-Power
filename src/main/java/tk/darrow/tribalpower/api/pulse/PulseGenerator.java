package tk.darrow.tribalpower.api.pulse;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A block that makes Pulse rather than only holding it.
 *
 * <p>The lattice drains generators before anything else, so a station never empties a Pulse Cairn while a
 * drum beside it is full. Every generator can also say what it is producing and why, in the same integers
 * the server uses -- that is what keeps the Codex, the Ley Lens and a comparator telling one story.
 */
public interface PulseGenerator extends PulseHandler {
    /** Which of the six voices this generator speaks with. */
    Attunement voice();

    /** Pulse produced this second at the current conditions. Zero when stilled or starved of input. */
    int currentOutput();

    /** One line per factor, then the total: the same shape as the Ley Collector's breakdown. */
    List<Component> breakdown();
}
