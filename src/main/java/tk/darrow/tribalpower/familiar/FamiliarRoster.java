package tk.darrow.tribalpower.familiar;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import tk.darrow.tribalpower.entity.CreatureProfile;
import tk.darrow.tribalpower.tribe.TribeDefinition;

/** Who can be bonded, which tribe must be Voice, and which slot they occupy. */
public final class FamiliarRoster {
    public static final int COMBAT_OUT=1,SUPPORT_OUT=2;
    private FamiliarRoster(){}

    /**
     * Bonded company is still a {@link Enemy} remnant; camp-bred persist young stay Enemy too.
     * Camp defense and spirit tools must skip both, and any nametagged remnant that learned to stay.
     */
    public static boolean hostile(LivingEntity entity) {
        if(!(entity instanceof Enemy))return false;
        if(entity instanceof Familiar familiar && (familiar.isBonded() || familiar.asMob().isPersistenceRequired()))return false;
        return true;
    }

    public static boolean tameable(CreatureProfile profile) {
        return switch(profile) {
            case ASHBOUND,ROOTBOUND,REED_STALKER,HOLLOW_SENTINEL -> false;
            default -> true;
        };
    }

    public static boolean combat(CreatureProfile profile) {
        return profile==CreatureProfile.SHARDBACK || profile==CreatureProfile.RIFT_HOUND;
    }

    /** Tribe whose Voice rank unlocks a hostile bond. Gentle animals ignore this. */
    public static TribeDefinition voiceTribe(CreatureProfile profile) {
        return switch(profile) {
            case SHARDBACK -> TribeDefinition.STONE;
            case RIFT_HOUND -> TribeDefinition.CLAW;
            case CINDER_IMP -> TribeDefinition.SPARK;
            case STORM_MOTH -> TribeDefinition.CLOCK;
            case MOURNING_BELL -> TribeDefinition.SIGIL;
            case ECHO_WEAVER -> TribeDefinition.SPINDLE;
            default -> null;
        };
    }
}
