package tk.darrow.tribalpower.cuisine;

import java.util.Locale;
import tk.darrow.tribalpower.tribe.TribeDefinition;

/**
 * The nine tribe dishes, one per tribe, each cooked at a Hearth Pot and each carrying its tribe's boon for a while.
 * The recipes are data (data/tribalpower/recipe/hearth/); this is only the item and what it does when eaten.
 */
public enum Dish {
    KEEPERS_ROOT_MASH(TribeDefinition.SOIL, 8, 0.7F, true),
    GRIT_BAKED_BREAD(TribeDefinition.STONE, 7, 0.6F, false),
    ROOTBINDER_RICE(TribeDefinition.SPROUT, 8, 0.7F, true),
    EDGE_WALKER_JERKY(TribeDefinition.CLAW, 6, 0.8F, false),
    DRUMHEART_CHILI(TribeDefinition.SPARK, 9, 0.8F, true),
    PATTERN_TART(TribeDefinition.CLOCK, 7, 0.7F, false),
    HONEYCAKE(TribeDefinition.SWARM, 8, 0.6F, false),
    SEAL_CARVERS_BROTH(TribeDefinition.SIGIL, 7, 0.8F, true),
    LOOM_THREAD_NOODLES(TribeDefinition.SPINDLE, 9, 0.7F, true);

    public final TribeDefinition tribe;
    public final int nutrition;
    public final float saturation;
    /** Served in a bowl (the recipe's container), so eating it hands the bowl back. */
    public final boolean bowl;

    Dish(TribeDefinition tribe, int nutrition, float saturation, boolean bowl) {
        this.tribe = tribe;
        this.nutrition = nutrition;
        this.saturation = saturation;
        this.bowl = bowl;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Dish of(TribeDefinition tribe) {
        for (Dish dish : values()) if (dish.tribe == tribe) return dish;
        throw new IllegalArgumentException(tribe.name());
    }
}
