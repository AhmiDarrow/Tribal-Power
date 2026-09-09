package tk.darrow.tribalpower.familiar;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import tk.darrow.tribalpower.entity.LatticeAnimal;

/**
 * Wolf-style following for bonded {@link LatticeAnimal}s (which extend Animal, not TamableAnimal): walk toward the
 * owner beyond {@code startDistance}, stop inside {@code stopDistance}, teleport to a walkable spot beside the
 * owner once further than 12 blocks.
 */
public class FamiliarFollowGoal extends Goal {
    public static final double TELEPORT_DISTANCE_SQ=144;
    private final LatticeAnimal animal;
    private final double speed;
    private final float startDistance,stopDistance;
    private final PathNavigation navigation;
    private LivingEntity owner;
    private int recalc;
    private float oldWaterCost;
    public FamiliarFollowGoal(LatticeAnimal animal,double speed,float startDistance,float stopDistance) {
        this.animal=animal;this.speed=speed;this.startDistance=startDistance;this.stopDistance=stopDistance;navigation=animal.getNavigation();
        setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
    }
    @Override public boolean canUse() {
        LivingEntity target=animal.getOwner();
        if(target==null || animal.unableToMoveToOwner() || animal.distanceToSqr(target)<startDistance*startDistance)return false;
        owner=target;return true;
    }
    @Override public boolean canContinueToUse() { return !navigation.isDone() && !animal.unableToMoveToOwner() && animal.distanceToSqr(owner)>stopDistance*stopDistance; }
    @Override public void start() { recalc=0;oldWaterCost=animal.getPathfindingMalus(PathType.WATER);animal.setPathfindingMalus(PathType.WATER,0); }
    @Override public void stop() { owner=null;navigation.stop();animal.setPathfindingMalus(PathType.WATER,oldWaterCost); }
    @Override public void tick() {
        boolean teleport=animal.distanceToSqr(owner)>=TELEPORT_DISTANCE_SQ;
        if(!teleport)animal.getLookControl().setLookAt(owner,10,animal.getMaxHeadXRot());
        if(--recalc<=0) {
            recalc=adjustedTickDelay(10);
            if(teleport)teleportToOwner(animal,owner);else navigation.moveTo(owner,speed);
        }
    }
    /** Try up to ten random walkable spots 2-3 blocks from the owner. @return true if the animal moved. */
    public static boolean teleportToOwner(LatticeAnimal animal,LivingEntity owner) {
        BlockPos base=owner.blockPosition();
        for(int i=0;i<10;i++) {
            int dx=animal.getRandom().nextIntBetweenInclusive(-3,3),dz=animal.getRandom().nextIntBetweenInclusive(-3,3);
            if(Math.abs(dx)<2 && Math.abs(dz)<2)continue;
            BlockPos pos=base.offset(dx,animal.getRandom().nextIntBetweenInclusive(-1,1),dz);
            if(!canTeleportTo(animal,pos))continue;
            animal.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,animal.getYRot(),animal.getXRot());
            animal.getNavigation().stop();
            return true;
        }
        return false;
    }
    private static boolean canTeleportTo(LatticeAnimal animal,BlockPos pos) {
        if(WalkNodeEvaluator.getPathTypeStatic(animal,pos)!=PathType.WALKABLE)return false;
        if(animal.level().getBlockState(pos.below()).getBlock() instanceof LeavesBlock)return false;
        return animal.level().noCollision(animal,animal.getBoundingBox().move(pos.subtract(animal.blockPosition())));
    }
}
