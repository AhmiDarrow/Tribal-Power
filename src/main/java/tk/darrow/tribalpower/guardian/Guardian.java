package tk.darrow.tribalpower.guardian;

import net.minecraft.world.item.Item;
import tk.darrow.tribalpower.entity.CreatureProfile;
import tk.darrow.tribalpower.item.CreatureItems;
import tk.darrow.tribalpower.tribe.TribeDefinition;

/**
 * The eight guardians of the March: one to a biome, each the end of a tribe's story (the Loom-stitchers' ends at
 * The Unsung). A guardian is called at its altar with a fistful of its biome's reagent, fights in two phases,
 * and drops a core that feeds the last rite. Numbers here are the design; the config scales them.
 */
public enum Guardian {
    SLAG_TITAN("slag_titan", "march_ember_wastes", TribeDefinition.SPARK, 0xff8a3c, 360, 12, 12, 0.24, 2.6F, 4.4F, 2.2F,
            Ability.SLAM, CreatureProfile.CINDER_IMP, CreatureProfile.MAGMA_CREEPER, false, "slag_core"),
    BOG_MATRIARCH("bog_matriarch", "march_reed_fen", TribeDefinition.SPROUT, 0x9be08a, 320, 8, 10, 0.26, 2.4F, 3.6F, 2.0F,
            Ability.SNARE, CreatureProfile.REED_STALKER, CreatureProfile.REED_STALKER, false, "fen_heart"),
    CAIRN_WIGHT("cairn_wight", "march_snow_fields", TribeDefinition.SWARM, 0xbfe9ff, 300, 6, 11, 0.28, 1.6F, 3.8F, 1.9F,
            Ability.FROST, CreatureProfile.RIME_CRAWLER, CreatureProfile.RIME_CRAWLER, false, "cairn_stone"),
    PRISM_SERPENT("prism_serpent", "march_crystal_fields", TribeDefinition.STONE, 0xd4c6f2, 300, 8, 9, 0.3, 2.2F, 2.4F, 2.0F,
            Ability.BOLT, CreatureProfile.SHARD_STALKER, CreatureProfile.SHARDBACK, false, "prism_core"),
    VAULT_SENTINEL("vault_sentinel", "march_glimmer_ridge", TribeDefinition.SIGIL, 0xbafbe8, 380, 16, 11, 0.22, 2.0F, 4.0F, 2.1F,
            Ability.SWEEP, CreatureProfile.GLOOM_CRAWLER, CreatureProfile.HOLLOW_SENTINEL, false, "vault_seal"),
    TIDE_DRUMMER("tide_drummer", "march_shallows", TribeDefinition.CLOCK, 0xa0d6ec, 320, 8, 10, 0.26, 2.4F, 3.2F, 2.0F,
            Ability.TIDE, CreatureProfile.BRINE_LURKER, CreatureProfile.BRINE_LURKER, false, "tide_shell"),
    STAMPEDE_SPIRIT("stampede_spirit", "march_steppe", TribeDefinition.SOIL, 0xecdcaa, 300, 6, 12, 0.34, 2.2F, 3.0F, 2.0F,
            Ability.CHARGE, CreatureProfile.BURROW_GNASHER, CreatureProfile.LONGSHANK, false, "herd_horn"),
    STORM_ROC("storm_roc", "march_highlands", TribeDefinition.CLAW, 0xadffff, 280, 4, 10, 0.3, 2.8F, 2.6F, 2.2F,
            Ability.SWOOP, CreatureProfile.STORM_MOTH, CreatureProfile.STORM_MOTH, true, "storm_plume");

    /** The signature attack; the second phase sharpens it. */
    public enum Ability { SLAM, SNARE, FROST, BOLT, SWEEP, TIDE, CHARGE, SWOOP }

    public final String id, biome, coreId;
    public final TribeDefinition tribe;
    public final int colour;
    public final double health, armor, damage, speed;
    public final float width, height, scale;
    public final Ability ability;
    public final CreatureProfile adds, call;
    public final boolean flying;

    Guardian(String id, String biome, TribeDefinition tribe, int colour, double health, double armor, double damage, double speed,
             float width, float height, float scale, Ability ability, CreatureProfile adds, CreatureProfile call, boolean flying, String coreId) {
        this.id = id; this.biome = biome; this.tribe = tribe; this.colour = colour; this.health = health; this.armor = armor;
        this.damage = damage; this.speed = speed; this.width = width; this.height = height; this.scale = scale;
        this.ability = ability; this.adds = adds; this.call = call; this.flying = flying; this.coreId = coreId;
    }

    /** The reagent that calls this guardian at its altar. */
    public Item callItem() { return CreatureItems.REAGENTS.get(call).get(); }

    public String nameKey() { return "entity.tribalpower." + id; }

    public static Guardian byId(String id) {
        for (Guardian guardian : values()) if (guardian.id.equals(id)) return guardian;
        return null;
    }

    /** The guardian a tribe's story ends at, or null for the Loom-stitchers (The Unsung). */
    public static Guardian of(TribeDefinition tribe) {
        for (Guardian guardian : values()) if (guardian.tribe == tribe) return guardian;
        return null;
    }
}
