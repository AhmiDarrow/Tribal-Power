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
public class CampBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
    public static final int CAPACITY=2400;
    private NonNullList<ItemStack> items=NonNullList.withSize(27,ItemStack.EMPTY);
    public int pulse;
    public UUID owner;
    public String ownerName="Skybound";
    private int cursor;
    private boolean active;
    private String reason="Waiting";
    public CampBlockEntity(BlockPos pos,BlockState state){super(CampRegistry.TYPE.get(),pos,state);}
    public String kind(){return BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath();}
    public boolean hasInventory(){return Set.of("grove_tender","summoning_cradle","offering_table").contains(kind());}
    @Override protected NonNullList<ItemStack> getItems(){return items;}
    @Override protected void setItems(NonNullList<ItemStack> value){items=value;}
    @Override public int getContainerSize(){return 27;}
    @Override protected Component getDefaultName(){return Component.translatable("block.tribalpower."+kind());}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory){return ChestMenu.threeRows(id,inventory,this);}
    @Override public boolean stillValid(Player player){return super.stillValid(player)&&!level.hasNeighborSignal(worldPosition)&&canAccess(player);}
    /** Owner-based access: the placer, anyone sharing the placer's camp (design 3.0 §6), or everyone when unclaimed. */
    public boolean canAccess(Player player){return owner==null||player.getUUID().equals(owner)||tk.darrow.tribalpower.camp.identity.Camps.sameCamp(level instanceof ServerLevel server?server.getServer():null,owner,player.getUUID());}
    @Override public int[] getSlotsForFace(Direction side){
        if(kind().equals("offering_table"))return java.util.stream.IntStream.range(0,27).toArray();
        if(kind().equals("summoning_cradle"))return side==Direction.DOWN?new int[]{0}:new int[]{1};
        return side==Direction.DOWN?java.util.stream.IntStream.range(9,27).toArray():java.util.stream.IntStream.range(0,9).toArray();
    }
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return !level.hasNeighborSignal(worldPosition)&&canPlaceItem(slot,stack);}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){
        if(kind().equals("summoning_cradle"))return slot==0?stack.getItem() instanceof BoundEffigyItem:slot==1&&stack.is(ModItems.SPIRITWEAVE.get());
        if(kind().equals("grove_tender"))return slot<9&&stack.getItem() instanceof BlockItem block&&block.getBlock() instanceof CropBlock;
        return kind().equals("offering_table");
    }
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return !level.hasNeighborSignal(worldPosition)&&(!kind().equals("summoning_cradle")||slot==0&&BoundEffigyItem.remaining(stack)==0);}
    public Component status(){
        var status=Component.literal(reason+" | "+pulse+" / "+CAPACITY+" Pulse");
        if(kind().equals("summoning_cradle"))status.append(" | ").append(BoundEffigyItem.targetName(items.get(0))).append(" | "+BoundEffigyItem.remaining(items.get(0))+" threads");
        return status;
    }
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
            active=false;reason="Paused by redstone";
            if(level.getBlockState(worldPosition).is(getBlockState().getBlock())&&getBlockState().getValue(CampBlock.LIT))level.setBlock(worldPosition,getBlockState().setValue(CampBlock.LIT,false),3);
        }
    }
    public static void tick(Level level,BlockPos pos,BlockState state,CampBlockEntity be){
        if((level.getGameTime()+pos.asLong())%20!=0)return;
        be.work((ServerLevel)level);boolean lit=be.active;
        if(be.getBlockState().getValue(CampBlock.LIT)!=lit)level.setBlock(pos,be.getBlockState().setValue(CampBlock.LIT,lit),3);
        level.updateNeighbourForOutputSignal(pos,state.getBlock());
    }
    public void work(ServerLevel server){
        if(server.hasNeighborSignal(worldPosition)){deactivate();return;}
        active=false;reason="Waiting for Pulse";
        String kind=kind();
        if(!Set.of("spirit_lantern","rain_chime","offering_table").contains(kind)){
            int add=LatticeNetwork.extractPulseNearby(server,worldPosition,8,Math.min(80,CAPACITY-pulse),false);
            if(add>0){pulse+=add;setChanged();}
        }
        switch(kind){
            case "wayanchor" -> {
                if(pulse>=16&&CampHooks.anchor(server,worldPosition,owner,true)){spend(16);active=true;reason="Holding this chunk";}
                else {CampHooks.anchor(server,worldPosition,owner,false);reason="Need 16 Pulse/s; maximum "+CampHooks.SOLO_ANCHOR_CAP+" anchors per dimension, or "+CampHooks.CAMP_ANCHOR_BUDGET+" shared by a camp";}
            }
            case "hush_totem" -> {active=pulse>=8;if(active){spend(8);reason="Hostile spawn ward: 24 blocks";}CampHooks.ward(server,worldPosition,active);}
            case "summoning_cradle" -> {reason="Effigy in slot 1; Spiritweave in slot 2";if((server.getGameTime()+worldPosition.asLong())%200==0)summon(server);}
            case "grove_tender" -> tend(server);
            case "spirit_lantern" -> {active=true;reason="Lantern lit; redstone dims it";}
            case "rain_chime" -> {active=server.isRaining();reason=server.isThundering()?"Thunder: signal 15":active?"Rain: signal 8":"Clear skies: signal 0";
                if(active&&(server.getGameTime()+worldPosition.asLong())%200==0)server.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.BLOCKS,0.25F,1.4F);
            }
            case "offering_table" -> {active=true;reason="27 offering slots; comparator reads fullness";}
        }
    }
    private void spend(int amount){pulse-=amount;setChanged();}
    public boolean summon(ServerLevel server){
        if(server.hasNeighborSignal(worldPosition))return false;
        ItemStack effigy=items.get(0),offering=items.get(1);
        if(BoundEffigyItem.remaining(effigy)==0){reason="Bind or renew the effigy";return false;}
        if(pulse<80||!offering.is(ModItems.SPIRITWEAVE.get())){reason="Needs 80 Pulse and 1 Spiritweave per summon";return false;}
        if(server.getEntitiesOfClass(Mob.class,new AABB(worldPosition).inflate(12)).size()>=8){reason="Eight nearby mobs: waiting";return false;}
        var type=BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(BoundEffigyItem.target(effigy)));
        if(type.getCategory()==MobCategory.MONSTER&&server.getDifficulty()==Difficulty.PEACEFUL){reason="Hostile spirits sleep in Peaceful";return false;}
        for(int attempt=0;attempt<8;attempt++){
            BlockPos pos=worldPosition.offset(server.random.nextInt(7)-3,0,server.random.nextInt(7)-3);
            if(pos.equals(worldPosition)||!server.hasChunkAt(pos)||!server.getWorldBorder().isWithinBounds(pos)||!server.getBlockState(pos.below()).isFaceSturdy(server,pos.below(),Direction.UP))continue;
            if(type.getCategory()==MobCategory.MONSTER&&CampHooks.warded(server,pos)){reason="A Hush Totem blocks this summoning";continue;}
            var entity=type.create(server);if(!(entity instanceof Mob mob))return false;
            mob.moveTo(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,server.random.nextFloat()*360,0);
            if(!server.noCollision(mob)||server.containsAnyLiquid(mob.getBoundingBox()))continue;
            EventHooks.finalizeMobSpawn(mob,server,server.getCurrentDifficultyAt(pos),MobSpawnType.SPAWNER,null);
            if(mob.isSpawnCancelled()||!server.addFreshEntity(mob))continue;
            offering.shrink(1);BoundEffigyItem.spend(effigy);spend(80);active=true;reason="Summoned "+mob.getName().getString();CampHooks.award(server,owner,"first_summon");
            tk.darrow.tribalpower.effect.SpiritEffects.ring(server,pos.getCenter(),tk.darrow.tribalpower.api.pulse.Attunement.SPIRIT,1,16);
            return true;
        }
        reason="Clear a safe floor within 3 blocks";return false;
    }
    private void tend(ServerLevel server){
        reason="Tending a 9x9 bed at this height";
        if(owner==null){reason="Place this tender yourself to claim it";return;}
        if(pulse<4)return;
        var farmer=FakePlayerFactory.get(server,new GameProfile(owner,ownerName));
        for(int scan=0;scan<9;scan++){
            int n=cursor;cursor=(cursor+1)%81;BlockPos pos=worldPosition.offset(n%9-4,0,n/9-4);
            if(pos.equals(worldPosition)||!server.hasChunkAt(pos)||!server.getWorldBorder().isWithinBounds(pos))continue;
            var state=server.getBlockState(pos);if(!server.mayInteract(farmer,pos))continue;
            if(state.getBlock() instanceof CropBlock crop&&crop.isMaxAge(state)){
                if(pulse<12)continue;
                if(NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(server,pos,state,farmer)).isCanceled()){reason="Crop protected";continue;}
                List<ItemStack> drops=new ArrayList<>(Block.getDrops(state,server,pos,null,farmer,ItemStack.EMPTY));
                Item seed=crop.asItem();boolean reserved=false;
                for(var drop:drops)if(drop.is(seed)&&!drop.isEmpty()){drop.shrink(1);reserved=true;break;}
                var preview=copyItems();
                if(!reserved)for(int i=0;i<9;i++)if(preview.get(i).is(seed)){preview.get(i).shrink(1);reserved=true;break;}
                if(!reserved){reason="Needs one seed to replant";continue;}
                if(!storeDrops(preview,drops)){reason="Output full; crop preserved";return;}
                if(!server.setBlock(pos,crop.getStateForAge(0),3))continue;
                items=preview;spend(12);active=true;reason="Harvested and replanted";return;
            }
            if(state.isAir())for(int slot=0;slot<9;slot++){
                ItemStack seed=items.get(slot);
                if(!(seed.getItem() instanceof BlockItem block)||!(block.getBlock() instanceof CropBlock crop))continue;
                var planted=crop.getStateForAge(0);if(!planted.canSurvive(server,pos))continue;
                var snapshot=BlockSnapshot.create(server.dimension(),server,pos);
                if(!server.setBlock(pos,planted,3))continue;
                if(EventHooks.onBlockPlace(farmer,snapshot,Direction.UP)){snapshot.restore(3);reason="Planting protected";break;}
                seed.shrink(1);spend(4);active=true;reason="Planted a seed";return;
            }
        }
    }
    private NonNullList<ItemStack> copyItems(){var result=NonNullList.withSize(27,ItemStack.EMPTY);for(int i=0;i<27;i++)result.set(i,items.get(i).copy());return result;}
    public static boolean storeDrops(NonNullList<ItemStack> slots,List<ItemStack> drops){
        for(var drop:drops){int left=drop.getCount();for(int i=9;i<27&&left>0;i++){
            var in=slots.get(i);if(in.isEmpty()){int n=Math.min(left,drop.getMaxStackSize());slots.set(i,drop.copyWithCount(n));left-=n;}
            else if(ItemStack.isSameItemSameComponents(in,drop)){int n=Math.min(left,Math.max(0,in.getMaxStackSize()-in.getCount()));in.grow(n);left-=n;}
        }if(left>0)return false;}return true;
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);ContainerHelper.saveAllItems(tag,items,registries);tag.putInt("Pulse",pulse);tag.putInt("Cursor",Math.floorMod(cursor,81));if(owner!=null)tag.putUUID("Owner",owner);tag.putString("OwnerName",ownerName);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);items=NonNullList.withSize(27,ItemStack.EMPTY);ContainerHelper.loadAllItems(tag,items,registries);pulse=Math.clamp(tag.getInt("Pulse"),0,CAPACITY);cursor=Math.floorMod(tag.getInt("Cursor"),81);owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;ownerName=tag.getString("OwnerName");if(ownerName.isBlank()||ownerName.length()>16)ownerName="Skybound";active=false;}
}
