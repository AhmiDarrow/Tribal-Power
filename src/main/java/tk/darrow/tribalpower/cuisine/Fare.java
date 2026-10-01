package tk.darrow.tribalpower.cuisine;

import java.util.List;
import java.util.Locale;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * Camp fare: everyday Hearth Pot cooking that carries no tribe's boon. Each has its own place on the road, from a
 * cheap loaf for the long walk to a stew eaten before a fight, and most mix the March's crops and game with what a
 * farm already grows. The recipes are data (data/tribalpower/recipe/hearth/); this is the item and what eating it
 * does. Hunger and saturation sit beside the tribe dishes' (constants, as theirs are); how long each side effect
 * lasts is in the config ({@code cuisine.fare}).
 *
 * <p>Ceiling: no fare outfills the best tribe dish (9 hunger at 0.8), so a dish's boon is never the worse buy.
 */
public enum Fare {
    // the road: cheap, stackable, most of them cooked several at a time
    DROVERS_FLATBREAD(5, 0.6F, Serving.PLAIN, 64, null, 0),
    GLIMMER_CRISPS(3, 0.4F, Serving.PLAIN, 64, null, 0, Trait.FAST),
    HUNTERS_SKEWER(6, 0.6F, Serving.PLAIN, 64, MobEffects.MOVEMENT_SPEED, 20),
    STEPPE_PEMMICAN(4, 0.9F, Serving.PLAIN, 64, null, 0),
    // the country: each meant for one stretch of the March
    FROSTBERRY_PRESERVE(4, 0.4F, Serving.BOTTLE, 16, MobEffects.FIRE_RESISTANCE, 30),
    THISTLE_TEA(2, 0.6F, Serving.BOTTLE, 16, null, 0, Trait.DRINK, Trait.ALWAYS),
    HIGHLAND_CRUMBLE(6, 0.6F, Serving.PLAIN, 16, MobEffects.JUMP, 45),
    DRIFT_BELL_DUMPLINGS(5, 0.6F, Serving.PLAIN, 16, MobEffects.SLOW_FALLING, 30),
    DELVERS_POTTAGE(7, 0.6F, Serving.BOWL, 16, MobEffects.DIG_SPEED, 90),
    FEN_CALLERS_CHOWDER(8, 0.7F, Serving.BOWL, 16, MobEffects.WATER_BREATHING, 90),
    REED_WRAPPED_EEL(7, 0.8F, Serving.PLAIN, 16, MobEffects.NIGHT_VISION, 120),
    // the fight: the fullest fare, for before a hard night
    EMBER_ROAST_SQUASH(6, 0.7F, Serving.PLAIN, 16, MobEffects.DAMAGE_BOOST, 30),
    LONG_HUNT_STEW(9, 0.7F, Serving.BOWL, 16, MobEffects.ABSORPTION, 60),
    BLOOMSONG_CUSTARD(6, 0.8F, Serving.BOWL, 16, MobEffects.REGENERATION, 15);

    /** What the fare is served in: the pot asks for it as the container, and eating hands it back. */
    public enum Serving {
        PLAIN(null), BOWL(Items.BOWL), BOTTLE(Items.GLASS_BOTTLE);

        public final @Nullable Item container;

        Serving(@Nullable Item container) { this.container = container; }
    }

    public enum Trait { FAST, ALWAYS, DRINK }

    public final int nutrition;
    public final float saturation;
    public final Serving serving;
    public final int stack;
    /** The side effect, at level I, or null for plain food. */
    public final @Nullable Holder<MobEffect> effect;
    /** The config's default for how many seconds the side effect lasts. */
    public final int defaultSeconds;
    private final List<Trait> traits;

    Fare(int nutrition, float saturation, Serving serving, int stack, @Nullable Holder<MobEffect> effect, int defaultSeconds, Trait... traits) {
        this.nutrition = nutrition;
        this.saturation = saturation;
        this.serving = serving;
        this.stack = stack;
        this.effect = effect;
        this.defaultSeconds = defaultSeconds;
        this.traits = List.of(traits);
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean has(Trait trait) {
        return traits.contains(trait);
    }

    /** What eating it settles. Thistle Tea is the camp's antidote for a bad meal. */
    public List<Holder<MobEffect>> cures() {
        return this == THISTLE_TEA ? List.of(MobEffects.POISON, MobEffects.HUNGER, MobEffects.CONFUSION) : List.of();
    }
}
