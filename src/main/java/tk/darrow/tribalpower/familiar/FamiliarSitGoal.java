package tk.darrow.tribalpower.familiar;

import java.util.EnumSet;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/** Holds a bonded familiar still while it is ordered to stay. Flying sitters need not touch the ground. */
public class FamiliarSitGoal extends Goal {
    private final Familiar familiar;
    private final Mob mob;
    public FamiliarSitGoal(Familiar familiar) {
        this.familiar=familiar;this.mob=familiar.asMob();
        setFlags(EnumSet.of(Flag.JUMP,Flag.MOVE));
    }
    @Override public boolean canUse() {
        // Every wild creature carries this goal: ask whether it was told to wait before the water and block checks.
        if(!familiar.isBonded() || !familiar.isSitting() || mob.isVehicle())return false;
        // A swimmer waits where it is in the water; anything else is let finish swimming to shore first.
        boolean wet=mob.isInWaterOrBubble();
        boolean swimming=wet && tk.darrow.tribalpower.entity.CreatureSwimming.swims(familiar.profile());
        return (!wet || swimming) && (familiar.profile().flying || mob.onGround() || swimming);
    }
    @Override public boolean canContinueToUse() { return familiar.isBonded() && familiar.isSitting() && !mob.isVehicle(); }
    @Override public void start() { mob.getNavigation().stop(); }
}
