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
    private static final Map<ServerLevel,Set<BlockPos>> ANCHORS=new WeakHashMap<>();
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
                ANCHORS.computeIfAbsent(level,l->new HashSet<>()).add(pos.immutable());
            }
        }
        for(var id:helper.getEntityTickets().keySet())helper.removeAllTickets(id);
    });
    public static boolean anchor(ServerLevel level,BlockPos pos,boolean active){
        var set=ANCHORS.computeIfAbsent(level,l->new HashSet<>());
        if(active&&!set.contains(pos)&&set.size()>=32)return false;
        if(active)set.add(pos.immutable());else set.remove(pos);
        var chunk=new ChunkPos(pos);TICKETS.forceChunk(level,pos,chunk.x,chunk.z,active,true);return active;
    }
    public static void ward(ServerLevel level,BlockPos pos,boolean active){
        var chunks=WARDS.computeIfAbsent(level,l->new HashMap<>());long chunk=new ChunkPos(pos).toLong();
        if(active)chunks.computeIfAbsent(chunk,c->new HashMap<>()).put(pos.immutable(),level.getGameTime()+24);
        else if(chunks.containsKey(chunk)){chunks.get(chunk).remove(pos);if(chunks.get(chunk).isEmpty())chunks.remove(chunk);}
    }
    public static boolean warded(ServerLevel level,BlockPos target){
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
