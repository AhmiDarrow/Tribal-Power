package tk.darrow.tribalpower.verification;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import tk.darrow.tribalpower.camp.CampHooks;
import tk.darrow.tribalpower.camp.identity.*;
import tk.darrow.tribalpower.entity.*;
import tk.darrow.tribalpower.familiar.*;
import tk.darrow.tribalpower.storage.DeepCacheManager;

/** Bonded spirits and shared camps (design 3.0 §5-6). */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class FamiliarGameTests {
    private static LatticeAnimal spawn(GameTestHelper h,CreatureProfile profile,int x,int y,int z) {
        var animal=CreatureEntities.ANIMALS.get(profile).get().create(h.getLevel());
        var pos=h.absolutePos(new BlockPos(x,y,z));
        animal.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        h.getLevel().addFreshEntity(animal);
        return animal;
    }
    private static void floor(GameTestHelper h) { for(int x=0;x<8;x++)for(int z=0;z<8;z++)h.setBlock(x,1,z,Blocks.STONE); }
    /** Deterministic bond: forced success in survival mode spends exactly one charm. */
    private static ItemStack bond(GameTestHelper h,net.minecraft.server.level.ServerPlayer player,LatticeAnimal animal) {
        player.getAbilities().instabuild=false;
        var charm=new ItemStack(FamiliarRegistry.BONDING_CHARM.get(),16);
        h.assertTrue(BondingCharmItem.attempt(h.getLevel(),player,animal,charm,true) && animal.isBonded(),"A forced attempt must bond");
        h.assertTrue(charm.getCount()==15,"A charm is spent on success, got "+charm.getCount());
        return charm;
    }
    @GameTest(template="empty")
    public static void bondingStoresOwnerAndSurvivesNbtRoundTrip(GameTestHelper h) {
        floor(h);
        var player=h.makeMockServerPlayerInLevel();
        var fox=spawn(h,CreatureProfile.LANTERN_FOX,3,2,3);
        bond(h,player,fox);
        h.assertTrue(fox.ownerUUID().orElse(null).equals(player.getUUID()) && fox.isOwnedBy(player),"Bonding must store the owner");
        h.assertTrue(fox.requiresCustomPersistence() && !fox.removeWhenFarAway(4096),"Bonded animals never despawn");
        float health=fox.getHealth();
        h.assertTrue(!fox.hurt(h.getLevel().damageSources().playerAttack(player),4) && fox.getHealth()==health,"Owners cannot hurt their familiars");
        fox.setSitting(true);
        var tag=new CompoundTag();fox.saveWithoutId(tag);
        h.assertTrue(tag.hasUUID("Owner") && tag.getBoolean("Sitting"),"NBT must carry Owner and Sitting");
        var copy=CreatureEntities.ANIMALS.get(CreatureProfile.LANTERN_FOX).get().create(h.getLevel());
        copy.load(tag);
        h.assertTrue(copy.isBonded() && copy.isOwnedBy(player) && copy.isSitting(),"Owner and Sitting must survive a round-trip");
        var baby=spawn(h,CreatureProfile.LANTERN_FOX,5,2,5);baby.setBaby(true);
        var charm=new ItemStack(FamiliarRegistry.BONDING_CHARM.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,charm);
        charm.getItem().interactLivingEntity(charm,player,baby,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(!baby.isBonded() && charm.getCount()==1,"Babies cannot be bonded and keep the charm");
        var other=spawn(h,CreatureProfile.LANTERN_FOX,6,2,3);
        h.assertTrue(!fox.hurt(h.getLevel().damageSources().playerAttack(player),4) && other.hurt(h.getLevel().damageSources().playerAttack(player),1),"Only the bonded animal ignores its owner");
        other.discard();
        fox.discard();baby.discard();h.succeed();
    }
    @GameTest(template="empty")
    public static void sittingStopsMovement(GameTestHelper h) {
        floor(h);
        var player=h.makeMockServerPlayerInLevel();
        var owner=h.absolutePos(new BlockPos(1,2,1));player.moveTo(owner.getX()+.5,owner.getY(),owner.getZ()+.5,0,0);
        var stag=spawn(h,CreatureProfile.DAWN_STAG,6,2,6);
        bond(h,player,stag);
        stag.setSitting(true);
        var start=stag.position();
        h.runAfterDelay(60,()->{
            h.assertTrue(stag.isSitting() && stag.position().distanceToSqr(start)<.25,"A waiting familiar must stay put: moved "+stag.position().distanceTo(start));
            h.assertTrue(stag.getNavigation().isDone(),"Sitting cancels navigation");
            stag.discard();h.succeed();
        });
    }
    @GameTest(template="empty")
    public static void mossbackSaddlebagPersists(GameTestHelper h) {
        floor(h);
        var player=h.makeMockServerPlayerInLevel();
        var mossback=spawn(h,CreatureProfile.MOSSBACK,3,2,3);
        bond(h,player,mossback);
        mossback.saddlebag().setItem(0,new ItemStack(Items.SEAGRASS,7));
        mossback.saddlebag().setItem(8,new ItemStack(Items.MOSS_BLOCK,3));
        var tag=new CompoundTag();mossback.saveWithoutId(tag);
        h.assertTrue(tag.contains("Saddlebag"),"NBT must carry the Saddlebag list");
        var copy=CreatureEntities.ANIMALS.get(CreatureProfile.MOSSBACK).get().create(h.getLevel());
        copy.load(tag);
        h.assertTrue(copy.saddlebag().getItem(0).is(Items.SEAGRASS) && copy.saddlebag().getItem(0).getCount()==7 && copy.saddlebag().getItem(8).getCount()==3,"Saddlebag contents must survive a round-trip");
        var menu=new MossbackMenu(1,player.getInventory(),mossback.saddlebag(),mossback);
        player.moveTo(mossback.getX()+1,mossback.getY(),mossback.getZ(),0,0);
        h.assertTrue(menu.stillValid(player),"The owner may keep the saddlebag open");
        var stranger=h.makeMockServerPlayerInLevel();stranger.moveTo(mossback.getX()+1,mossback.getY(),mossback.getZ(),0,0);
        h.assertTrue(!menu.stillValid(stranger),"Strangers may not");
        mossback.discard();h.succeed();
    }
    @GameTest(template="empty")
    public static void campCreateInviteJoinSharesVault(GameTestHelper h) {
        var server=h.getLevel().getServer();
        var a=h.makeMockServerPlayerInLevel();var b=h.makeMockServerPlayerInLevel();
        var data=Camps.data(server);
        String name="Test camp "+UUID.randomUUID().toString().substring(0,4);
        var camp=data.create(name,a.getUUID());
        h.assertTrue(camp!=null && camp.leader.equals(a.getUUID()) && camp.isMember(a.getUUID()),"Creating a camp makes the founder its leader");
        h.assertTrue(data.create(name,b.getUUID())==null,"Names are unique");
        h.assertTrue(!data.join(camp,b.getUUID()),"Joining needs an invitation");
        h.assertTrue(data.invite(camp,a.getUUID(),b.getUUID()) && data.join(camp,b.getUUID()),"Invite then join");
        h.assertTrue(Camps.sameCamp(server,a.getUUID(),b.getUUID()) && Camps.isMember(server,b.getUUID(),camp.id),"Members share a camp");
        h.assertTrue(new CampSavedData.Invitation(camp.id,a.getUUID(),System.currentTimeMillis()-1).expired(System.currentTimeMillis()),"Old invitations expire");
        var vaultA=DeepCacheManager.openContainer(server,a.getUUID(),false);
        vaultA.setItem(3,new ItemStack(Items.DIAMOND,5));
        var vaultB=DeepCacheManager.openContainer(server,b.getUUID(),false);
        h.assertTrue(vaultB instanceof CampVaultContainer && vaultB.getItem(3).is(Items.DIAMOND) && vaultB.getItem(3).getCount()==5,"Members open the same camp vault");
        h.assertTrue(vaultB.stillValid(b) && !vaultB.stillValid(h.makeMockServerPlayerInLevel()),"Only members keep the camp vault open");
        var personal=DeepCacheManager.openContainer(server,b.getUUID(),true);
        h.assertTrue(!(personal instanceof CampVaultContainer) && personal.getItem(3).isEmpty(),"Personal mode opens the private vault");
        var tag=data.save(new CompoundTag(),server.registryAccess());
        var loaded=CampSavedData.load(tag,server.registryAccess());
        var loadedCamp=loaded.camp(camp.id);
        h.assertTrue(loadedCamp!=null && loadedCamp.isMember(b.getUUID()) && loadedCamp.vault.get(3).getCount()==5 && loaded.campOf(b.getUUID())==loadedCamp,"Camps, members and vaults persist");
        data.leave(b.getUUID());
        h.assertTrue(!Camps.sameCamp(server,a.getUUID(),b.getUUID()) && camp.isMember(a.getUUID()),"Leaving removes membership");
        data.leave(a.getUUID());
        h.assertTrue(data.camp(camp.id)==null,"An empty camp dissolves");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void anchorBudgetEnforced(GameTestHelper h) {
        var server=h.getLevel().getServer();var level=h.getLevel();
        var a=h.makeMockServerPlayerInLevel();var b=h.makeMockServerPlayerInLevel();
        var data=Camps.data(server);
        var camp=data.create("Anchor camp "+UUID.randomUUID().toString().substring(0,4),a.getUUID());
        data.invite(camp,a.getUUID(),b.getUUID());data.join(camp,b.getUUID());
        var base=h.absolutePos(new BlockPos(0,2,0));
        var claimed=new java.util.ArrayList<BlockPos>();
        try {
            for(int i=0;i<CampHooks.CAMP_ANCHOR_BUDGET;i++) {
                var pos=base.offset(i*16,0,0);
                h.assertTrue(CampHooks.anchor(level,pos,i%2==0?a.getUUID():b.getUUID(),true),"Anchor "+i+" fits the camp budget");claimed.add(pos);
            }
            h.assertTrue(CampHooks.campAnchors(server,camp.id)==CampHooks.CAMP_ANCHOR_BUDGET,"Budget counts anchors from every member");
            var extra=base.offset(0,0,16*4);
            h.assertTrue(!CampHooks.anchor(level,extra,b.getUUID(),true),"The thirteenth camp anchor is refused");
            h.assertTrue(CampHooks.anchor(level,extra,null,true),"Solo anchors keep the per-dimension rule");claimed.add(extra);
            h.assertTrue(CampHooks.anchor(level,claimed.get(0),a.getUUID(),true),"Re-confirming an existing anchor never counts twice");
        } finally {
            for(var pos:claimed)CampHooks.anchor(level,pos,null,false);
            data.leave(b.getUUID());data.leave(a.getUUID());
        }
        h.assertTrue(CampHooks.campAnchors(server,camp.id)==0,"Released anchors free the budget");
        h.succeed();
    }
}
