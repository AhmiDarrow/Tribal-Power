package tk.darrow.tribalpower.familiar;

import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** One combat familiar may follow; two support or utility may follow. Sitters do not count. */
public final class FamiliarSlots {
    private FamiliarSlots(){}

    public static boolean canFollow(ServerLevel level,UUID owner,Familiar self) {
        int combat=0,support=0;
        var ownerPlayer=level.getPlayerByUUID(owner);
        AABB box=self.asMob().getBoundingBox().inflate(96);
        if(ownerPlayer!=null)box=box.minmax(ownerPlayer.getBoundingBox().inflate(96));
        for(Mob mob:level.getEntitiesOfClass(Mob.class,box,m->m.isAlive() && m instanceof Familiar)) {
            Familiar other=(Familiar)mob;
            if(other==self || !other.isBonded() || !owner.equals(other.ownerUUID().orElse(null)) || other.isSitting())continue;
            Mob ridden=other.asMob();
            if(ridden.isVehicle() || ridden.isPassenger())continue;
            if(FamiliarRoster.combat(other.profile()))combat++;else support++;
        }
        if(FamiliarRoster.combat(self.profile()))return combat<FamiliarRoster.COMBAT_OUT;
        return support<FamiliarRoster.SUPPORT_OUT;
    }

    /** @return true if the familiar is now following. */
    public static boolean tryFollow(Familiar familiar) {
        if(!(familiar.asMob().level() instanceof ServerLevel level)) { familiar.setSitting(false);return true; }
        Player owner=familiar.getOwner();
        if(owner==null) { familiar.setSitting(false);return true; }
        if(canFollow(level,owner.getUUID(),familiar)) { familiar.setSitting(false);return true; }
        familiar.setSitting(true);
        owner.displayClientMessage(Component.translatable(
                FamiliarRoster.combat(familiar.profile())?"message.tribalpower.familiar.combat_full":"message.tribalpower.familiar.support_full",
                familiar.asMob().getDisplayName()),true);
        return false;
    }

    public static void afterBond(Familiar familiar,Player owner) {
        if(familiar.asMob().level() instanceof ServerLevel level && !canFollow(level,owner.getUUID(),familiar)) {
            familiar.setSitting(true);
            owner.displayClientMessage(Component.translatable("message.tribalpower.familiar.company_wait",familiar.asMob().getDisplayName()),false);
        }
    }
}
