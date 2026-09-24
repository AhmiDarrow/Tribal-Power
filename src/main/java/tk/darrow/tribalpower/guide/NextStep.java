package tk.darrow.tribalpower.guide;

import java.util.List;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * The guided path: one spine of advancements from the first hearth to the Ninth Agreement, and the one concrete
 * thing to do next. The Codex's "Next step" panel shows it; the server works it out so it is always true.
 */
public final class NextStep {
    /** The spine, in order. Each is an advancement id under {@code tribalpower:} and the lang key of its guidance. */
    public static final List<String> SPINE = List.of(
            "journey/root", "first_camp", "journey/hum", "journey/shatter", "journey/attune", "journey/bind", "journey/manifest",
            "journey/core", "journey/staff", "journey/ritual", "first_rite", "journey/march", "march/ancestor_hall",
            "tribes/offering", "tribes/friend", "tribes/first_story", "march/drum_remembers", "tribes/all_stories", "march/ninth_agreement");
    public static final String DONE = "done";

    private NextStep() {}

    /** The first spine advancement the player has not earned, as its id, or {@link #DONE}. */
    public static String of(ServerPlayer player) {
        var advancements = player.getServer().getAdvancements();
        for (String id : SPINE) {
            AdvancementHolder holder = advancements.get(ResourceLocation.fromNamespaceAndPath("tribalpower", id));
            if (holder == null) continue;
            if (!player.getAdvancements().getOrStartProgress(holder).isDone()) return id;
        }
        return DONE;
    }

    /** How far along the spine a step is, 0-based, or the spine's length when done. */
    public static int index(String step) {
        int i = SPINE.indexOf(step);
        return i < 0 ? SPINE.size() : i;
    }

    public static String key(String step) { return "guide.tribalpower." + step.replace('/', '.'); }
}
