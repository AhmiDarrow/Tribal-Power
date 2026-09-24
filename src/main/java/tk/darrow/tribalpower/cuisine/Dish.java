package tk.darrow.tribalpower.cuisine;

import java.util.Locale;
import tk.darrow.tribalpower.tribe.TribeDefinition;

/**
 * The nine tribe dishes, one per tribe, each cooked at a Hearth Pot and each carrying its tribe's boon for a while.
 * The recipes are data (data/tribalpower/recipe/hearth/); this is only the item and what it does when eaten.
 */
public enum Dish {
    KEEPERS_ROOT_MASH(TribeDefinition.SOIL, 8, 0.7F),
    GRIT_BAKED_BREAD(TribeDefinition.STONE, 7, 0.6F),
    ROOTBINDER_RICE(TribeDefinition.SPROUT, 8, 0.7F),
    EDGE_WALKER_JERKY(TribeDefinition.CLAW, 6, 0.8F),
    DRUMHEART_CHILI(TribeDefinition.SPARK, 9, 0.8F),
    PATTERN_TART(TribeDefinition.CLOCK, 7, 0.7F),
    HONEYCAKE(TribeDefinition.SWARM, 8, 0.6F),
    SEAL_CARVERS_BROTH(TribeDefinition.SIGIL, 7, 0.8F),
    LOOM_THREAD_NOODLES(TribeDefinition.SPINDLE, 9, 0.7F);

    public final TribeDefinition tribe;
    public final int nutrition;
    public final float saturation;

    Dish(TribeDefinition tribe, int nutrition, float saturation) {
        this.tribe = tribe;
        this.nutrition = nutrition;
        this.saturation = saturation;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Dish of(TribeDefinition tribe) {
        for (Dish dish : values()) if (dish.tribe == tribe) return dish;
        throw new IllegalArgumentException(tribe.name());
    }
}
