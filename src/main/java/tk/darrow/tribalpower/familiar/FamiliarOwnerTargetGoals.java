package tk.darrow.tribalpower.familiar;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;

/** Wolf-style "help the owner" targeting for bonded combat familiars. */
public final class FamiliarOwnerTargetGoals {
    private FamiliarOwnerTargetGoals(){}

    public static boolean forbidden(Familiar familiar,LivingEntity target) {
        if(target==null || !target.isAlive())return true;
        if(target instanceof Player)return familiar.isBonded();
        if(target instanceof Familiar && !FamiliarRoster.hostile(target))return true;
        if(target instanceof Familiar other && other.isBonded() && familiar.ownerUUID().equals(other.ownerUUID()))return true;
        if(familiar.lattice().expressed(FamiliarData.Mark.SOFT_MAW)
                && (target instanceof AbstractVillager || target instanceof AbstractGolem))return true;
        return false;
    }

    public static final class OwnerHurtBy extends TargetGoal {
        private final Familiar familiar;
        private LivingEntity attacker;
        private int timestamp;
        public OwnerHurtBy(Familiar familiar) {
            super(familiar.asMob(),false);this.familiar=familiar;setFlags(EnumSet.of(Goal.Flag.TARGET));
        }
        @Override public boolean canUse() {
            if(!familiar.isBonded() || familiar.isSitting() || !FamiliarRoster.combat(familiar.profile()))return false;
            Player owner=familiar.getOwner();
            if(owner==null)return false;
            attacker=owner.getLastHurtByMob();
            return owner.getLastHurtByMobTimestamp()!=timestamp && attacker!=null && !forbidden(familiar,attacker)
                    && canAttack(attacker,TargetingConditions.DEFAULT);
        }
        @Override public void start() {
            Player owner=familiar.getOwner();
            if(owner!=null)timestamp=owner.getLastHurtByMobTimestamp();
            mob.setTarget(attacker);super.start();
        }
    }

    public static final class OwnerHurt extends TargetGoal {
        private final Familiar familiar;
        private LivingEntity victim;
        private int timestamp;
        public OwnerHurt(Familiar familiar) {
            super(familiar.asMob(),false);this.familiar=familiar;setFlags(EnumSet.of(Goal.Flag.TARGET));
        }
        @Override public boolean canUse() {
            if(!familiar.isBonded() || familiar.isSitting() || !FamiliarRoster.combat(familiar.profile()))return false;
            Player owner=familiar.getOwner();
            if(owner==null)return false;
            victim=owner.getLastHurtMob();
            return owner.getLastHurtMobTimestamp()!=timestamp && victim!=null && !forbidden(familiar,victim)
                    && canAttack(victim,TargetingConditions.DEFAULT);
        }
        @Override public void start() {
            Player owner=familiar.getOwner();
            if(owner!=null)timestamp=owner.getLastHurtMobTimestamp();
            mob.setTarget(victim);super.start();
        }
    }

    public static final class HurtBy extends TargetGoal {
        private final Familiar familiar;
        public HurtBy(Familiar familiar) {
            super(familiar.asMob(),false);this.familiar=familiar;setFlags(EnumSet.of(Goal.Flag.TARGET));
        }
        @Override public boolean canUse() {
            Mob mob=familiar.asMob();
            LivingEntity attacker=mob.getLastHurtByMob();
            if(attacker==null || familiar.isOwnedBy(attacker) || forbidden(familiar,attacker))return false;
            if(familiar.isBonded() && (familiar.isSitting() || !FamiliarRoster.combat(familiar.profile())))return false;
            return canAttack(attacker,TargetingConditions.DEFAULT);
        }
        @Override public void start() { mob.setTarget(mob.getLastHurtByMob());super.start(); }
    }
}
