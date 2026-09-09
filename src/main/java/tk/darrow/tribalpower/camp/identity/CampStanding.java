package tk.darrow.tribalpower.camp.identity;

import net.minecraft.server.level.ServerPlayer;
import tk.darrow.tribalpower.tribe.StandingListener;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.tribe.TribeStanding;

/**
 * Hearth standing mirror (design 3.0 §6): every member's tribe standing change lands on the camp at 25%.
 * Registered once from the mod constructor via {@link #register()}. Kin may trade at the camp's rank when it is
 * higher than the player's own: see {@link #effectiveStanding}.
 */
public final class CampStanding implements StandingListener {
    public static final CampStanding INSTANCE=new CampStanding();
    private CampStanding(){}
    public static void register() { TribeStanding.addListener(INSTANCE); }
    @Override public void onStandingChanged(ServerPlayer player,TribeDefinition tribe,int delta,int total) {
        var data=Camps.data(player.server);
        var camp=data.campOf(player.getUUID());
        if(camp!=null)data.mirrorStanding(camp,tribe.id(),delta);
    }
    /** Personal standing or the camp's mirrored standing, whichever is higher. */
    public static int effectiveStanding(ServerPlayer player,TribeDefinition tribe) {
        return Math.max(TribeStanding.get(player.server,player.getUUID(),tribe),Camps.campStanding(player.server,player.getUUID(),tribe.id()));
    }
}
