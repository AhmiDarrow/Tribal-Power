package tk.darrow.tribalpower.item;

import java.util.Locale;

/**
 * The Spiritgear weapon family beyond the Blade. Each kind carries its shipped numbers; the live ones are read
 * from the {@code weapons} section of the common config, so a pack can rebalance any of them.
 *
 * <p>Damage and speed are what the tooltip shows at rank 0 (attack damage, attacks per second). Reach is added
 * to the wielder's entity reach. {@code trait} is the one number each kind's special move turns on, and
 * {@code weight} slows (or, for the dagger, quickens) the wielder while it is in hand. No kind wins on every
 * count: reach, speed, damage, weight and its move are traded against each other.
 */
public enum WeaponKind {
    /** Long reach; a sprinting thrust hits harder. trait = extra damage fraction while sprinting. */
    SPEAR(7.0, 1.3, 1.5, false, false, 0.35, 0.0, Swing.THRUST),
    /** Reach and weight; sweeps and breaks shields. trait = sweeping damage ratio added. */
    HALBERD(10.0, 0.9, 1.0, true, true, 0.25, -0.04, Swing.SLASH),
    /** Heavy cleaver; sweeps and breaks shields. trait = sweeping damage ratio added. */
    BATTLE_AXE(11.0, 0.8, 0.0, true, true, 0.35, -0.06, Swing.CHOP),
    /** The heaviest blow; breaks shields and armour. trait = fraction of the target's armour dealt as bonus damage. */
    WARHAMMER(12.0, 0.7, 0.0, false, true, 0.3, -0.08, Swing.CHOP),
    /** Quick and close; a strike from behind lands far harder. trait = damage multiplier from behind. */
    DAGGER(4.5, 2.4, -0.5, false, false, 1.75, 0.05, Swing.STAB),
    /** Reaps a ring around the wielder. trait = fraction of the blow dealt to every hostile nearby. */
    SCYTHE(8.0, 1.0, 0.5, false, false, 0.5, -0.02, Swing.SLASH),
    /** Two hands of steel; the widest sweep. trait = sweeping damage ratio added. */
    GREATSWORD(10.0, 0.85, 0.5, true, false, 0.5, -0.06, Swing.SLASH),
    /** Three points and a long reach; hits harder on anything wet. trait = extra damage fraction vs targets in water or rain. */
    TRIDENT(8.0, 1.1, 1.0, false, false, 0.5, 0.0, Swing.THRUST);

    /** How the weapon moves through a blow: the client animates it this way, in hand and on the body. */
    public enum Swing { THRUST, STAB, CHOP, SWEEP, SLASH }

    public final double damage, speed, reach, trait, weight;
    public final boolean sweeps, breaksShields;
    public final Swing swing;

    WeaponKind(double damage, double speed, double reach, boolean sweeps, boolean breaksShields, double trait, double weight, Swing swing) {
        this.swing = swing;
        this.weight = weight;
        this.damage = damage;
        this.speed = speed;
        this.reach = reach;
        this.sweeps = sweeps;
        this.breaksShields = breaksShields;
        this.trait = trait;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String itemId() {
        return "spiritgear_" + id();
    }

    /** What the trait number means, for the config comment. */
    public String traitComment() {
        return switch (this) {
            case SPEAR -> "Extra damage, as a fraction, when the wielder is sprinting.";
            case HALBERD, BATTLE_AXE, GREATSWORD -> "Sweeping damage ratio added to the wielder while held.";
            case WARHAMMER -> "Fraction of the target's armour points dealt as bonus damage.";
            case DAGGER -> "Damage multiplier for a strike from behind the target.";
            case SCYTHE -> "Fraction of the blow also dealt to every hostile within reach of the swing.";
            case TRIDENT -> "Extra damage, as a fraction, against a target standing in water or rain.";
        };
    }
}
