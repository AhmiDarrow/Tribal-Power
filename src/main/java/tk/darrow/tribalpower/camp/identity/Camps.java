package tk.darrow.tribalpower.camp.identity;

import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Static camp membership API. The no-server overloads use the running server and answer "no camp" on a client. */
public final class Camps {
    public static final String PERSONAL_VAULT_KEY="TribalVaultPersonal";
    private Camps(){}
    public static CampSavedData data(MinecraftServer server) { return CampSavedData.get(server); }
    private static MinecraftServer server() { return ServerLifecycleHooks.getCurrentServer(); }
    public static CampSavedData.Camp campOf(MinecraftServer server,UUID player) { return server==null?null:data(server).campOf(player); }
    public static CampSavedData.Camp campOf(UUID player) { return campOf(server(),player); }
    public static boolean isMember(MinecraftServer server,UUID player,UUID campId) { var camp=campOf(server,player);return camp!=null && camp.id.equals(campId); }
    public static boolean isMember(UUID player,UUID campId) { return isMember(server(),player,campId); }
    /** True when the two players are the same person or share a camp. */
    public static boolean sameCamp(MinecraftServer server,UUID a,UUID b) {
        if(a==null || b==null)return false;
        if(a.equals(b))return true;
        var camp=campOf(server,a);
        return camp!=null && camp.isMember(b);
    }
    public static boolean sameCamp(UUID a,UUID b) { return sameCamp(server(),a,b); }
    /** The camp's mirrored standing with a tribe (0 when the player has no camp). Kin trade at max(personal, camp). */
    public static int campStanding(MinecraftServer server,UUID player,String tribeId) { var camp=campOf(server,player);return camp==null?0:camp.standing(tribeId); }
    /** Whether the player has switched their Deep Cache / satchel back to the personal vault (player persistent data). */
    public static boolean personalVault(Player player) { return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(PERSONAL_VAULT_KEY); }
    public static void setPersonalVault(Player player,boolean personal) {
        var data=player.getPersistentData();
        var persisted=data.getCompound(Player.PERSISTED_NBT_TAG);
        if(personal)persisted.putBoolean(PERSONAL_VAULT_KEY,true);else persisted.remove(PERSONAL_VAULT_KEY);
        data.put(Player.PERSISTED_NBT_TAG,persisted);
    }
}
