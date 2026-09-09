package tk.darrow.tribalpower.familiar;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import tk.darrow.tribalpower.entity.LatticeAnimal;

/** Holds a bonded animal still while it is ordered to stay (mirrors {@code SitWhenOrderedToGoal}). */
public class FamiliarSitGoal extends Goal {
    private final LatticeAnimal animal;
    public FamiliarSitGoal(LatticeAnimal animal) { this.animal=animal;setFlags(EnumSet.of(Flag.JUMP,Flag.MOVE)); }
    @Override public boolean canUse() { return animal.isBonded() && animal.isSitting() && !animal.isInWaterOrBubble() && animal.onGround() && !animal.isVehicle(); }
    @Override public boolean canContinueToUse() { return animal.isBonded() && animal.isSitting() && !animal.isVehicle(); }
    @Override public void start() { animal.getNavigation().stop(); }
}
