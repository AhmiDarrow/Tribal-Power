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
        if(!(familiar.asMob().level() instanceof ServerLevel level)) return !familiar.isSitting();
        var ownerId=familiar.ownerUUID().orElse(null);
        if(ownerId==null) { familiar.setSitting(true);return false; }
        Player owner=level.getPlayerByUUID(ownerId);
        // Logged out or in another world: leave sitters sitting so they do not steal a follow slot.
        if(owner==null) return false;
        if(canFollow(level,owner.getUUID(),familiar)) { familiar.setSitting(false);return true; }
        boolean already=familiar.isSitting();
        familiar.setSitting(true);
        if(!already) owner.displayClientMessage(Component.translatable(
                FamiliarRoster.combat(familiar.profile())?"message.tribalpower.familiar.combat_full":"message.tribalpower.familiar.support_full",
                familiar.asMob().getDisplayName()),true);
        return false;
    }

    /** Sit extras that walked into a full company after a portal or a second bond. */
    public static void enforceCap(Familiar familiar) {
        if(familiar.isSitting() || !(familiar.asMob().level() instanceof ServerLevel level))return;
        Player owner=familiar.getOwner();
        if(owner==null)return;
        if(canFollow(level,owner.getUUID(),familiar))return;
        familiar.setSitting(true);
        owner.displayClientMessage(Component.translatable(
                FamiliarRoster.combat(familiar.profile())?"message.tribalpower.familiar.combat_full":"message.tribalpower.familiar.support_full",
                familiar.asMob().getDisplayName()),true);
    }

    public static void afterBond(Familiar familiar,Player owner) {
        if(familiar.asMob().level() instanceof ServerLevel level && !canFollow(level,owner.getUUID(),familiar)) {
            familiar.setSitting(true);
            owner.displayClientMessage(Component.translatable("message.tribalpower.familiar.company_wait",familiar.asMob().getDisplayName()),false);
        }
    }
}
