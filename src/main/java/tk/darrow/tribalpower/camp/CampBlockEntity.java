package tk.darrow.tribalpower.camp;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.*;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.lattice.LatticeNetwork;
import java.util.*;

/** Bounded camp automation. Outputs and world changes are checked before spending resources. */
public class CampBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer, tk.darrow.tribalpower.lattice.HasSideIo, tk.darrow.tribalpower.camp.Ownership.Owned {
    public static final int CAPACITY=2400;
    private NonNullList<ItemStack> items=NonNullList.withSize(27,ItemStack.EMPTY);
    public int pulse;
    public UUID owner;
    public String ownerName="Skybound";
    int cursor;
    boolean active;
    String reason="waiting";
    String reasonName="";
    public CampBlockEntity(BlockPos pos,BlockState state){
        super(CampRegistry.TYPE.get(),pos,state);
        String path=BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        sides="grove_tender".equals(path)?tk.darrow.tribalpower.lattice.SideIo.mesh()
                :new tk.darrow.tribalpower.lattice.SideIo(tk.darrow.tribalpower.lattice.SideIo.Mode.BOTH);
    }
    public String kind(){return BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath();}
    public boolean hasInventory(){return Set.of("grove_tender","summoning_cradle","offering_table").contains(kind());}
    private final tk.darrow.tribalpower.lattice.SideIo sides;
    @Override public tk.darrow.tribalpower.lattice.SideIo sideIo(){return sides;}
    @Override public int[] inputSlots(Direction face){return getSlotsForFace(face);}
    @Override public int[] outputSlots(Direction face){return getSlotsForFace(face);}
    @Override protected NonNullList<ItemStack> getItems(){return items;}
    @Override protected void setItems(NonNullList<ItemStack> value){items=value;}
    @Override public int getContainerSize(){return 27;}
    @Override protected Component getDefaultName(){return Component.translatable("block.tribalpower."+kind());}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory){return ChestMenu.threeRows(id,inventory,this);}
    @Override public boolean stillValid(Player player){return super.stillValid(player)&&!level.hasNeighborSignal(worldPosition)&&canAccess(player);}
    /** Owner-based access: the placer, anyone sharing the placer's camp (design 3.0 §6), or everyone when unclaimed. */
    public boolean canAccess(Player player){return owner==null||player.getUUID().equals(owner)||tk.darrow.tribalpower.camp.identity.Camps.sameCamp(level instanceof ServerLevel server?server.getServer():null,owner,player.getUUID());}
    @Override public UUID owner(){return owner;}
    @Override public void setOwner(UUID owner){this.owner=owner;setChanged();}
    @Override public int[] getSlotsForFace(Direction side){
        if(kind().equals("offering_table"))return java.util.stream.IntStream.range(0,27).toArray();
        if(kind().equals("summoning_cradle"))return side==Direction.DOWN?new int[]{0}:new int[]{1};
        return side==Direction.DOWN?java.util.stream.IntStream.range(9,27).toArray():java.util.stream.IntStream.range(0,9).toArray();
    }
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return !level.hasNeighborSignal(worldPosition)&&sides.get(side).insert()&&canPlaceItem(slot,stack);}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){
        if(kind().equals("summoning_cradle"))return slot==0?stack.getItem() instanceof BoundEffigyItem:slot==1&&stack.is(ModItems.SPIRITWEAVE.get());
        if(kind().equals("grove_tender"))return slot<9&&GroveWork.isSeed(stack);
        return kind().equals("offering_table");
    }
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return !level.hasNeighborSignal(worldPosition)&&sides.get(side).extract()&&(!kind().equals("summoning_cradle")||slot==0&&BoundEffigyItem.remaining(stack)==0);}
    public Component status(){
        if(kind().equals("summoning_cradle"))
            return Component.translatable("message.tribalpower.hand.cradle_line",reasonComponent(),pulse,CAPACITY,BoundEffigyItem.targetName(items.get(0)),BoundEffigyItem.remaining(items.get(0)));
        return Component.translatable("message.tribalpower.hand.line",reasonComponent(),pulse,CAPACITY);
    }
    Component reasonComponent(){
        return switch(reason){
            case "need_anchor"->Component.translatable("message.tribalpower.hand.need_anchor",CampHooks.SOLO_ANCHOR_CAP,CampHooks.CAMP_ANCHOR_BUDGET);
            case "summoned"->Component.translatable("message.tribalpower.hand.summoned",reasonName);
            case "waiting","paused","wait_pulse","holding","hush","cradle_slots","lantern","thunder","rain","clear","offerings",
                    "bind_effigy","need_summon","crowded","peaceful","hush_block","need_floor","tending","unclaimed",
                    "output_full","berries","urged","plant_protected","planted","planted_cocoa","crop_protected","need_seed",
                    "replanted","harvested"->Component.translatable("message.tribalpower.hand."+reason);
            default->Component.literal(reason);
        };
    }
    void setReason(String key){reason=key;reasonName="";}
    void setReason(String key,String name){reason=key;reasonName=name;}
    public int signal(){
        if(level==null||level.hasNeighborSignal(worldPosition))return 0;
        if(kind().equals("rain_chime"))return level.isThundering()?15:level.isRaining()?8:0;
        if(kind().equals("summoning_cradle"))return BoundEffigyItem.remaining(items.get(0))==0?0:1+14*BoundEffigyItem.remaining(items.get(0))/BoundEffigyItem.MAX_USES;
        return hasInventory()?AbstractContainerMenu.getRedstoneSignalFromContainer(this):active?15:0;
    }
    public void deactivate(){
        if(level instanceof ServerLevel server){
            if(kind().equals("wayanchor"))CampHooks.anchor(server,worldPosition,owner,false);
            if(kind().equals("hush_totem"))CampHooks.ward(server,worldPosition,false);
            active=false;setReason("paused");
            if(level.getBlockState(worldPosition).is(getBlockState().getBlock())&&getBlockState().getValue(CampBlock.LIT))level.setBlock(worldPosition,getBlockState().setValue(CampBlock.LIT,false),3);
        }
    }
    public static void tick(Level level,BlockPos pos,BlockState state,CampBlockEntity be){
        tk.darrow.tribalpower.lattice.SideIoAdjacency.beat(level,be);
        if((level.getGameTime()+pos.asLong())%20!=0)return;
        be.work((ServerLevel)level);boolean lit=be.active;
        if(be.getBlockState().getValue(CampBlock.LIT)!=lit)level.setBlock(pos,be.getBlockState().setValue(CampBlock.LIT,lit),3);
        level.updateNeighbourForOutputSignal(pos,state.getBlock());
    }
    public void work(ServerLevel server){
        if(server.hasNeighborSignal(worldPosition)){deactivate();return;}
        active=false;setReason("wait_pulse");
        String kind=kind();
        if(!Set.of("spirit_lantern","rain_chime","offering_table").contains(kind)){
            int add=LatticeNetwork.extractPulseNearby(server,worldPosition,8,Math.min(80,CAPACITY-pulse),false);
            if(add>0){pulse+=add;setChanged();}
        }
        switch(kind){
            case "wayanchor" -> {
                if(pulse>=16&&CampHooks.anchor(server,worldPosition,owner,true)){spend(16);active=true;setReason("holding");}
                else {CampHooks.anchor(server,worldPosition,owner,false);setReason("need_anchor");}
            }
            case "hush_totem" -> {active=pulse>=8;if(active){spend(8);setReason("hush");}CampHooks.ward(server,worldPosition,active);}
            case "summoning_cradle" -> {setReason("cradle_slots");if((server.getGameTime()+worldPosition.asLong())%200==0)summon(server);}
            case "grove_tender" -> GroveWork.tend(this, server);
            case "spirit_lantern" -> {active=true;setReason("lantern");}
            case "rain_chime" -> {active=server.isRaining();setReason(server.isThundering()?"thunder":active?"rain":"clear");
                if(active&&(server.getGameTime()+worldPosition.asLong())%200==0)server.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.BLOCKS,0.25F,1.4F);
            }
            case "offering_table" -> {active=true;setReason("offerings");}
        }
    }
    void spendPublic(int amount){spend(amount);}
    void spend(int amount){pulse-=amount;setChanged();}
    NonNullList<ItemStack> copyItemsPublic(){return copyItems();}
    void replaceItems(NonNullList<ItemStack> next){items=next;setChanged();}
    public boolean summon(ServerLevel server){
        if(server.hasNeighborSignal(worldPosition))return false;
        ItemStack effigy=items.get(0),offering=items.get(1);
        if(BoundEffigyItem.remaining(effigy)==0){setReason("bind_effigy");return false;}
        if(pulse<80||offering.isEmpty()||!offering.is(ModItems.SPIRITWEAVE.get())){setReason("need_summon");return false;}
        if(server.getEntitiesOfClass(Mob.class,new AABB(worldPosition).inflate(12)).size()>=8){setReason("crowded");return false;}
        var type=BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(BoundEffigyItem.target(effigy)));
        if(type.getCategory()==MobCategory.MONSTER&&server.getDifficulty()==Difficulty.PEACEFUL){setReason("peaceful");return false;}
        for(int attempt=0;attempt<8;attempt++){
            BlockPos pos=worldPosition.offset(server.random.nextInt(7)-3,0,server.random.nextInt(7)-3);
            if(pos.equals(worldPosition)||!server.hasChunkAt(pos)||!server.getWorldBorder().isWithinBounds(pos)||!server.getBlockState(pos.below()).isFaceSturdy(server,pos.below(),Direction.UP))continue;
            if(type.getCategory()==MobCategory.MONSTER&&CampHooks.warded(server,pos)){setReason("hush_block");continue;}
            var entity=type.create(server);if(!(entity instanceof Mob mob))return false;
            mob.moveTo(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,server.random.nextFloat()*360,0);
            if(!server.noCollision(mob)||server.containsAnyLiquid(mob.getBoundingBox()))continue;
            EventHooks.finalizeMobSpawn(mob,server,server.getCurrentDifficultyAt(pos),MobSpawnType.SPAWNER,null);
            if(mob.isSpawnCancelled()||!server.addFreshEntity(mob))continue;
            offering.shrink(1);if(offering.isEmpty())setItem(1,ItemStack.EMPTY);BoundEffigyItem.spend(effigy);spend(80);active=true;setReason("summoned",mob.getName().getString());CampHooks.award(server,owner,"first_summon");
            tk.darrow.tribalpower.effect.SpiritEffects.ring(server,pos.getCenter(),tk.darrow.tribalpower.api.pulse.Attunement.SPIRIT,1,16);
            return true;
        }
        setReason("need_floor");return false;
    }
    private NonNullList<ItemStack> copyItems(){var result=NonNullList.withSize(27,ItemStack.EMPTY);for(int i=0;i<27;i++)result.set(i,items.get(i).copy());return result;}
    public static boolean storeDrops(NonNullList<ItemStack> slots,List<ItemStack> drops){
        for(var drop:drops){int left=drop.getCount();for(int i=9;i<27&&left>0;i++){
            var in=slots.get(i);if(in.isEmpty()){int n=Math.min(left,drop.getMaxStackSize());slots.set(i,drop.copyWithCount(n));left-=n;}
            else if(ItemStack.isSameItemSameComponents(in,drop)){int n=Math.min(left,Math.max(0,in.getMaxStackSize()-in.getCount()));in.grow(n);left-=n;}
        }if(left>0)return false;}return true;
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);ContainerHelper.saveAllItems(tag,items,registries);tag.putInt("Pulse",pulse);tag.putInt("Cursor",Math.floorMod(cursor,81));if(owner!=null)tag.putUUID("Owner",owner);tag.putString("OwnerName",ownerName);sides.save(tag);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);items=NonNullList.withSize(27,ItemStack.EMPTY);ContainerHelper.loadAllItems(tag,items,registries);pulse=Math.clamp(tag.getInt("Pulse"),0,CAPACITY);cursor=Math.floorMod(tag.getInt("Cursor"),81);owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;ownerName=tag.getString("OwnerName");if(ownerName.isBlank()||ownerName.length()>16)ownerName="Skybound";active=false;sides.load(tag);}
}
