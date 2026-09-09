package tk.darrow.tribalpower.camp.identity;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Overworld-persisted camp identities (design 3.0 §6): camps {@code {id, name, leader, members, colour, vault}} and
 * pending invitations that expire after five minutes. NBT: {@code Camps} list, {@code Invitations} list.
 */
public class CampSavedData extends SavedData {
    public static final String FILE_ID="tribalpower_camps";
    public static final int VAULT_SLOTS=54,MAX_NAME=24;
    public static final long INVITE_MILLIS=5*60*1000L;
    public static final int[] COLOURS={0x7effcb,0xc08a4e,0x62d1c9,0xb5a3ff,0xffb46c,0xa2e8b9,0xb6e4ff,0xe4baff,0xffcb89};

    /** One camp. Members always include the leader. */
    public static final class Camp {
        public final UUID id;
        public String name;
        public UUID leader;
        public final Set<UUID> members=new LinkedHashSet<>();
        public int colour;
        public final NonNullList<ItemStack> vault=NonNullList.withSize(VAULT_SLOTS,ItemStack.EMPTY);
        /** Mirrored tribe standing, stored in quarter points (members' gains land here at 25%). Key: tribe id. */
        public final Map<String,Integer> standingQuarters=new HashMap<>();
        public int standing(String tribeId) { return Math.floorDiv(standingQuarters.getOrDefault(tribeId,0),4); }
        Camp(UUID id,String name,UUID leader,int colour) { this.id=id;this.name=name;this.leader=leader;this.colour=colour;members.add(leader); }
        public boolean isMember(UUID player) { return members.contains(player); }
    }
    public record Invitation(UUID camp,UUID inviter,long expiresAt) { public boolean expired(long now) { return now>expiresAt; } }

    private final Map<UUID,Camp> camps=new LinkedHashMap<>();
    private final Map<UUID,Camp> memberIndex=new HashMap<>();
    private final Map<UUID,Invitation> invitations=new HashMap<>();
    private final ListTag unreadable=new ListTag();

    public static SavedData.Factory<CampSavedData> factory() { return new SavedData.Factory<>(CampSavedData::new,CampSavedData::load); }
    public static CampSavedData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(factory(),FILE_ID); }

    // ---- queries ------------------------------------------------------------------------------------------------
    public Collection<Camp> camps() { return Collections.unmodifiableCollection(camps.values()); }
    public Camp camp(UUID id) { return id==null?null:camps.get(id); }
    public Camp campOf(UUID player) { return player==null?null:memberIndex.get(player); }
    public Camp byName(String name) {
        if(name==null)return null;
        for(var camp:camps.values())if(camp.name.equalsIgnoreCase(name.trim()))return camp;
        return null;
    }
    public Invitation invitation(UUID player) {
        var invite=invitations.get(player);
        if(invite==null)return null;
        if(invite.expired(System.currentTimeMillis()) || !camps.containsKey(invite.camp())) { invitations.remove(player);setDirty();return null; }
        return invite;
    }
    public static String cleanName(String name) {
        String clean=name==null?"":name.trim().replaceAll("[^A-Za-z0-9 _'\\-]","");
        return clean.length()>MAX_NAME?clean.substring(0,MAX_NAME):clean;
    }

    // ---- mutations ----------------------------------------------------------------------------------------------
    /** @return the new camp, or null when the name is taken/blank or the leader already belongs to a camp. */
    public Camp create(String name,UUID leader) {
        String clean=cleanName(name);
        if(clean.isBlank() || byName(clean)!=null || memberIndex.containsKey(leader))return null;
        UUID id=UUID.randomUUID();
        while(camps.containsKey(id))id=UUID.randomUUID();
        var camp=new Camp(id,clean,leader,COLOURS[camps.size()%COLOURS.length]);
        camps.put(id,camp);memberIndex.put(leader,camp);invitations.remove(leader);setDirty();
        return camp;
    }
    public boolean rename(Camp camp,String name) {
        String clean=cleanName(name);
        if(clean.isBlank() || (byName(clean)!=null && byName(clean)!=camp))return false;
        camp.name=clean;setDirty();return true;
    }
    /** Invite a player; refused when they already belong to a camp. */
    public boolean invite(Camp camp,UUID inviter,UUID invitee) {
        if(memberIndex.containsKey(invitee))return false;
        invitations.put(invitee,new Invitation(camp.id,inviter,System.currentTimeMillis()+INVITE_MILLIS));setDirty();return true;
    }
    /** Accept a standing invitation to {@code camp}. @return false without a valid invitation or when already in a camp. */
    public boolean join(Camp camp,UUID player) {
        var invite=invitation(player);
        if(invite==null || !invite.camp().equals(camp.id) || memberIndex.containsKey(player))return false;
        camp.members.add(player);memberIndex.put(player,camp);invitations.remove(player);setDirty();return true;
    }
    /** Remove a member. The leadership passes to the oldest member; an empty camp is dissolved (vault items are returned by the caller). */
    public Camp leave(UUID player) {
        var camp=memberIndex.remove(player);
        if(camp==null)return null;
        camp.members.remove(player);
        if(camp.members.isEmpty())camps.remove(camp.id);
        else if(camp.leader.equals(player))camp.leader=camp.members.iterator().next();
        setDirty();return camp;
    }
    /** Mirror a member's standing change at 25%; negative changes are mirrored too so a camp cannot farm losses away. */
    public int mirrorStanding(Camp camp,String tribeId,int delta) {
        if(delta==0)return camp.standing(tribeId);
        camp.standingQuarters.merge(tribeId,delta,Integer::sum);
        if(camp.standingQuarters.get(tribeId)<0)camp.standingQuarters.put(tribeId,0);
        setDirty();return camp.standing(tribeId);
    }
    public NonNullList<ItemStack> vault(UUID campId) { var camp=camps.get(campId);return camp==null?null:camp.vault; }

    // ---- persistence ---------------------------------------------------------------------------------------------
    public static CampSavedData load(CompoundTag tag,HolderLookup.Provider registries) {
        var data=new CampSavedData();
        ListTag list=tag.getList("Camps",Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++) {
            CompoundTag entry=list.getCompound(i);
            if(!entry.hasUUID("Id") || !entry.hasUUID("Leader")) { data.unreadable.add(entry.copy());continue; }
            var camp=new Camp(entry.getUUID("Id"),cleanName(entry.getString("Name")),entry.getUUID("Leader"),entry.getInt("Colour"));
            if(camp.name.isBlank())camp.name="Camp "+camp.id.toString().substring(0,4);
            ListTag members=entry.getList("Members",Tag.TAG_INT_ARRAY);
            for(int m=0;m<members.size();m++)camp.members.add(net.minecraft.core.UUIDUtil.uuidFromIntArray(members.getIntArray(m)));
            ContainerHelper.loadAllItems(entry.getCompound("Vault"),camp.vault,registries);
            CompoundTag standing=entry.getCompound("Standing");
            for(String key:standing.getAllKeys())camp.standingQuarters.put(key,Math.max(0,standing.getInt(key)));
            data.camps.put(camp.id,camp);
            for(var member:camp.members)data.memberIndex.putIfAbsent(member,camp);
        }
        ListTag invites=tag.getList("Invitations",Tag.TAG_COMPOUND);
        long now=System.currentTimeMillis();
        for(int i=0;i<invites.size();i++) {
            CompoundTag entry=invites.getCompound(i);
            if(!entry.hasUUID("Player") || !entry.hasUUID("Camp") || !entry.hasUUID("Inviter"))continue;
            var invite=new Invitation(entry.getUUID("Camp"),entry.getUUID("Inviter"),entry.getLong("Expires"));
            if(!invite.expired(now) && data.camps.containsKey(invite.camp()))data.invitations.put(entry.getUUID("Player"),invite);
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries) {
        ListTag list=unreadable.copy();
        for(var camp:camps.values()) {
            CompoundTag entry=new CompoundTag();
            entry.putUUID("Id",camp.id);entry.putString("Name",camp.name);entry.putUUID("Leader",camp.leader);entry.putInt("Colour",camp.colour);
            ListTag members=new ListTag();
            for(var member:camp.members)members.add(net.minecraft.nbt.NbtUtils.createUUID(member));
            entry.put("Members",members);
            CompoundTag vault=new CompoundTag();ContainerHelper.saveAllItems(vault,camp.vault,registries);entry.put("Vault",vault);
            CompoundTag standing=new CompoundTag();camp.standingQuarters.forEach(standing::putInt);entry.put("Standing",standing);
            list.add(entry);
        }
        tag.put("Camps",list);
        ListTag invites=new ListTag();
        for(var e:invitations.entrySet()) {
            CompoundTag entry=new CompoundTag();
            entry.putUUID("Player",e.getKey());entry.putUUID("Camp",e.getValue().camp());entry.putUUID("Inviter",e.getValue().inviter());entry.putLong("Expires",e.getValue().expiresAt());
            invites.add(entry);
        }
        tag.put("Invitations",invites);
        return tag;
    }
}
