package tk.darrow.tribalpower.familiar;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/**
 * Wolf-style following for bonded familiars: walk toward the owner beyond start distance, stop inside stop
 * distance, teleport once further than 12 blocks.
 */
public class FamiliarFollowGoal extends Goal {
    public static final double TELEPORT_DISTANCE_SQ=144;
    private final Familiar familiar;
    private final Mob mob;
    private final double speed;
    private final PathNavigation navigation;
    private LivingEntity owner;
    private int recalc;
    private float oldWaterCost;
    public FamiliarFollowGoal(Familiar familiar,double speed) {
        this.familiar=familiar;this.mob=familiar.asMob();this.speed=speed;navigation=mob.getNavigation();
        setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
    }
    private float startDistance() { return familiar.lattice().followStart(); }
    private float stopDistance() { return familiar.lattice().followStop(); }
    @Override public boolean canUse() {
        LivingEntity target=familiar.getOwner();
        float start=startDistance();
        if(target==null || familiar.unableToMoveToOwner() || mob.distanceToSqr(target)<start*start)return false;
        owner=target;return true;
    }
    @Override public boolean canContinueToUse() {
        float stop=stopDistance();
        return !navigation.isDone() && !familiar.unableToMoveToOwner() && mob.distanceToSqr(owner)>stop*stop;
    }
    @Override public void start() { recalc=0;oldWaterCost=mob.getPathfindingMalus(PathType.WATER);mob.setPathfindingMalus(PathType.WATER,0); }
    @Override public void stop() { owner=null;navigation.stop();mob.setPathfindingMalus(PathType.WATER,oldWaterCost); }
    @Override public void tick() {
        boolean teleport=mob.distanceToSqr(owner)>=TELEPORT_DISTANCE_SQ;
        if(!teleport)mob.getLookControl().setLookAt(owner,10,mob.getMaxHeadXRot());
        if(--recalc<=0) {
            recalc=adjustedTickDelay(10);
            if(teleport)teleportToOwner(familiar,owner);else navigation.moveTo(owner,speed);
        }
    }
    public static boolean teleportToOwner(Familiar familiar,LivingEntity owner) {
        Mob mob=familiar.asMob();
        BlockPos base=owner.blockPosition();
        boolean flying=familiar.profile().flying;
        for(int i=0;i<10;i++) {
            int dx=mob.getRandom().nextIntBetweenInclusive(-3,3),dz=mob.getRandom().nextIntBetweenInclusive(-3,3);
            if(Math.abs(dx)<2 && Math.abs(dz)<2)continue;
            BlockPos pos=base.offset(dx,flying?1:mob.getRandom().nextIntBetweenInclusive(-1,1),dz);
            if(!canTeleportTo(familiar,pos))continue;
            mob.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,mob.getYRot(),mob.getXRot());
            mob.getNavigation().stop();
            return true;
        }
        return false;
    }
    private static boolean canTeleportTo(Familiar familiar,BlockPos pos) {
        Mob mob=familiar.asMob();
        if(familiar.profile().flying)
            return mob.level().noCollision(mob,mob.getBoundingBox().move(pos.subtract(mob.blockPosition())));
        if(WalkNodeEvaluator.getPathTypeStatic(mob,pos)!=PathType.WALKABLE)return false;
        if(mob.level().getBlockState(pos.below()).getBlock() instanceof LeavesBlock)return false;
        return mob.level().noCollision(mob,mob.getBoundingBox().move(pos.subtract(mob.blockPosition())));
    }
}
