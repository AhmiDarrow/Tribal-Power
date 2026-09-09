package tk.darrow.tribalpower.camp;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.minecraft.resources.ResourceLocation;

public final class CampHooks {
    /** Anchor position -> owner UUID (null for unowned); one map per dimension. */
    private static final Map<ServerLevel,Map<BlockPos,UUID>> ANCHORS=new WeakHashMap<>();
    /** Solo players keep 32 anchors per dimension; a camp shares 12 across every member and dimension (design 3.0 §6). */
    public static final int SOLO_ANCHOR_CAP=32,CAMP_ANCHOR_BUDGET=12;
    private static final Map<ServerLevel,Map<Long,Map<BlockPos,Long>>> WARDS=new WeakHashMap<>();
    public static final TicketController TICKETS=new TicketController(ResourceLocation.parse("tribalpower:wayanchors"),(level,helper)->{
        int count=0;
        for(var entry:helper.getBlockTickets().entrySet()){
            BlockPos pos=entry.getKey();
            if(!(level.getBlockEntity(pos) instanceof CampBlockEntity be)||!be.kind().equals("wayanchor")||be.pulse<16||level.hasNeighborSignal(pos)||count++>=32)helper.removeAllTickets(pos);
            else {
                // A valid anchor owns exactly its own ticking chunk; discard any stale broader tickets.
                for(long chunk:entry.getValue().ticking())if(chunk!=new ChunkPos(pos).toLong())helper.removeTicket(pos,chunk,true);
                for(long chunk:entry.getValue().nonTicking())helper.removeTicket(pos,chunk,false);
                ANCHORS.computeIfAbsent(level,l->new HashMap<>()).putIfAbsent(pos.immutable(),be.owner);
            }
        }
        for(var id:helper.getEntityTickets().keySet())helper.removeAllTickets(id);
    });
    public static boolean anchor(ServerLevel level,BlockPos pos,boolean active){return anchor(level,pos,null,active);}
    /** Claim or release an anchor for {@code owner}. Camp members draw on the shared camp budget; everyone else on the per-dimension cap. */
    public static boolean anchor(ServerLevel level,BlockPos pos,UUID owner,boolean active){
        var anchors=ANCHORS.computeIfAbsent(level,l->new HashMap<>());
        if(active&&!anchors.containsKey(pos)){
            var camp=tk.darrow.tribalpower.camp.identity.Camps.campOf(level.getServer(),owner);
            if(camp!=null?campAnchors(level.getServer(),camp.id)>=CAMP_ANCHOR_BUDGET:anchors.size()>=SOLO_ANCHOR_CAP)return false;
        }
        if(active)anchors.put(pos.immutable(),owner);else anchors.remove(pos);
        var chunk=new ChunkPos(pos);TICKETS.forceChunk(level,pos,chunk.x,chunk.z,active,true);return active;
    }
    /** Active anchors owned by members of {@code campId} across every dimension. */
    public static int campAnchors(net.minecraft.server.MinecraftServer server,UUID campId){
        var camp=tk.darrow.tribalpower.camp.identity.Camps.data(server).camp(campId);if(camp==null)return 0;
        int count=0;
        for(var entry:ANCHORS.entrySet())for(var owner:entry.getValue().values())if(owner!=null&&camp.isMember(owner))count++;
        return count;
    }
    public static int anchors(ServerLevel level){var anchors=ANCHORS.get(level);return anchors==null?0:anchors.size();}
    public static void ward(ServerLevel level,BlockPos pos,boolean active){
        var chunks=WARDS.computeIfAbsent(level,l->new HashMap<>());long chunk=new ChunkPos(pos).toLong();
        if(active)chunks.computeIfAbsent(chunk,c->new HashMap<>()).put(pos.immutable(),level.getGameTime()+24);
        else if(chunks.containsKey(chunk)){chunks.get(chunk).remove(pos);if(chunks.get(chunk).isEmpty())chunks.remove(chunk);}
    }
    public static boolean warded(ServerLevel level,BlockPos target){
        if(tk.darrow.tribalpower.rite.world.TemporaryWards.warded(level,target))return true; // Still Night rite (rite/world)
        var chunks=WARDS.get(level);if(chunks==null)return false;
        var center=new ChunkPos(target);
        for(int x=center.x-2;x<=center.x+2;x++)for(int z=center.z-2;z<=center.z+2;z++){
            var entries=chunks.get(ChunkPos.asLong(x,z));if(entries==null)continue;
            entries.entrySet().removeIf(e->e.getValue()<level.getGameTime());
            if(entries.isEmpty()){chunks.remove(ChunkPos.asLong(x,z));continue;}
            for(var pos:entries.keySet())if(pos.distSqr(target)<=24*24&&level.hasChunkAt(pos)&&!level.hasNeighborSignal(pos))return true;
        }
        return false;
    }
    public static void award(ServerLevel level,UUID owner,String name){
        if(owner==null)return;var player=level.getServer().getPlayerList().getPlayer(owner);if(player==null)return;
        var advancement=level.getServer().getAdvancements().get(ResourceLocation.parse("tribalpower:"+name));
        if(advancement!=null)player.getAdvancements().award(advancement,"done");
    }
    public static void spawn(MobSpawnEvent.SpawnPlacementCheck event){
        if(event.getEntityType().getCategory()==MobCategory.MONSTER&&event.getSpawnType()!=MobSpawnType.COMMAND&&event.getSpawnType()!=MobSpawnType.SPAWN_EGG
                &&warded(event.getLevel().getLevel(),event.getPos()))event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
    }
    public static void finalizeSpawn(net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent event){
        if(event.getEntity().getType().getCategory()==MobCategory.MONSTER&&event.getSpawnType()!=MobSpawnType.COMMAND&&event.getSpawnType()!=MobSpawnType.SPAWN_EGG
                &&warded(event.getLevel().getLevel(),event.getEntity().blockPosition()))event.setSpawnCancelled(true);
    }
    private CampHooks(){}
}
