package tk.darrow.tribalpower.verification;

import java.util.HashSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import tk.darrow.tribalpower.camp.CampHooks;
import tk.darrow.tribalpower.camp.identity.*;
import tk.darrow.tribalpower.entity.*;
import tk.darrow.tribalpower.familiar.*;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.blockentity.DrumheartBlockEntity;
import tk.darrow.tribalpower.item.CreatureItems;
import tk.darrow.tribalpower.ley.LeyRegistry;
import tk.darrow.tribalpower.storage.DeepCacheManager;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.tribe.TribeStanding;

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
        var player=VerificationPlayers.inLevel(h);
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
        var player=VerificationPlayers.inLevel(h);
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
        var player=VerificationPlayers.inLevel(h);
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
        var stranger=VerificationPlayers.inLevel(h);stranger.moveTo(mossback.getX()+1,mossback.getY(),mossback.getZ(),0,0);
        h.assertTrue(!menu.stillValid(stranger),"Strangers may not");
        mossback.discard();h.succeed();
    }
    @GameTest(template="empty")
    public static void campCreateInviteJoinSharesVault(GameTestHelper h) {
        var server=h.getLevel().getServer();
        var a=VerificationPlayers.inLevel(h);var b=VerificationPlayers.inLevel(h);
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
        h.assertTrue(vaultB.stillValid(b) && !vaultB.stillValid(VerificationPlayers.inLevel(h)),"Only members keep the camp vault open");
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
        var a=VerificationPlayers.inLevel(h);var b=VerificationPlayers.inLevel(h);
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
    @GameTest(template="empty")
    public static void foxLightClearedOnDimensionChangeAndKeptOffWater(GameTestHelper h) {
        floor(h);
        var level=h.getLevel();var server=level.getServer();
        var player=VerificationPlayers.inLevel(h);
        var fox=spawn(h,CreatureProfile.LANTERN_FOX,3,2,3);
        bond(h,player,fox);
        var light=FamiliarRegistry.SPIRIT_LIGHT.get();
        var at=fox.blockPosition();
        FamiliarAbilities.carryLight(level,fox);
        h.assertTrue(level.getBlockState(at).is(light) && at.equals(fox.lastLight()),"A bonded fox lights the air it stands in");
        // Moving onto water must neither replace the water nor leave the old light behind.
        var wet=h.absolutePos(new BlockPos(5,2,5));
        level.setBlock(wet,Blocks.WATER.defaultBlockState(),3);
        fox.moveTo(wet.getX()+.5,wet.getY(),wet.getZ()+.5,0,0);
        FamiliarAbilities.carryLight(level,fox);
        h.assertTrue(level.getBlockState(wet).is(Blocks.WATER) && !level.getBlockState(at).is(light) && fox.lastLight()==null,"Water is never replaced and the previous light is cleared");
        level.setBlock(wet,Blocks.AIR.defaultBlockState(),3);
        fox.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
        FamiliarAbilities.carryLight(level,fox);
        h.assertTrue(level.getBlockState(at).is(light),"Light returns on air");
        // Portals copy the entity into the other level and drop the old one without calling remove().
        var nether=server.getLevel(net.minecraft.world.level.Level.NETHER);
        h.assertTrue(nether!=null,"The test server has a Nether");
        var moved=fox.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(nether,new net.minecraft.world.phys.Vec3(at.getX()+.5,200,at.getZ()+.5),net.minecraft.world.phys.Vec3.ZERO,0,0,net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING));
        h.assertTrue(!level.getBlockState(at).is(light),"Leaving the dimension clears the fox's light");
        h.assertTrue(moved instanceof LatticeAnimal copy && copy!=fox && copy.isBonded() && copy.lastLight()==null,"The copy keeps its bond but not the old light position");
        if(moved!=null)moved.discard();
        h.succeed();
    }
    @GameTest(template="empty")
    public static void spiritLightSweepKeepsClaimedLights(GameTestHelper h) {
        floor(h);
        var level=h.getLevel();
        var player=VerificationPlayers.inLevel(h);
        var fox=spawn(h,CreatureProfile.LANTERN_FOX,1,2,1);
        bond(h,player,fox);
        var light=FamiliarRegistry.SPIRIT_LIGHT.get();
        var claimed=h.absolutePos(new BlockPos(2,2,2));var stray=h.absolutePos(new BlockPos(6,2,6));
        level.setBlock(claimed,light.defaultBlockState(),3);level.setBlock(stray,light.defaultBlockState(),3);
        fox.setLastLight(claimed);
        // A fleeing fox can be several blocks from the light it placed up to ten ticks ago.
        fox.moveTo(claimed.getX()+.5+SpiritLightBlock.SWEEP_REACH-1,claimed.getY(),claimed.getZ()+.5,0,0);
        level.getBlockState(claimed).tick(level,claimed,level.random);
        level.getBlockState(stray).tick(level,stray,level.random);
        h.assertTrue(level.getBlockState(claimed).is(light),"The sweep keeps a light its fox still claims");
        h.assertTrue(!level.getBlockState(stray).is(light),"The sweep removes a light no fox claims");
        fox.discard();
        h.assertTrue(!level.getBlockState(claimed).is(light) && fox.lastLight()==null,"Discarding the fox clears its light");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void wildLatticeVariesAndNeverRollsFour(GameTestHelper h) {
        var fingerprints=new HashSet<String>();
        int sawZero=0,sawTwo=0;
        for(int seed=1;seed<=200;seed++) {
            var data=new FamiliarData();
            data.rollWild(CreatureProfile.LANTERN_FOX,RandomSource.create(seed),false);
            h.assertTrue(data.rolled(),"A wild roll must mark the lattice written");
            StringBuilder key=new StringBuilder();
            for(var thread:FamiliarData.Thread.values()) {
                int a=data.alleleA(thread),b=data.alleleB(thread);
                h.assertTrue(a>=0 && a<=3 && b>=0 && b<=3,"Wild alleles stay 0-3, got "+a+"/"+b+" on "+thread);
                if(a==0 || b==0)sawZero++;
                if(a>=2 || b>=2)sawTwo++;
                key.append(a).append(b);
            }
            key.append(data.markA().id).append(data.markB().id);
            fingerprints.add(key.toString());
        }
        h.assertTrue(fingerprints.size()>20,"Starting tames must not be clones: "+fingerprints.size()+" distinct rolls");
        h.assertTrue(sawZero>0 && sawTwo>0,"Wild stock includes both weak and strong alleles");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void latticeInheritsMutatesAndFrays(GameTestHelper h) {
        var mother=new FamiliarData();mother.fill(2);
        var father=new FamiliarData();father.fill(2);
        UUID owner=UUID.randomUUID();
        var child=FamiliarData.inherit(mother,father,CreatureProfile.MOSSBACK,RandomSource.create(7L),owner,owner,0,0,0);
        h.assertTrue(child.generation()==1 && child.alleleA(FamiliarData.Thread.FRAME)==2 && child.alleleB(FamiliarData.Thread.KEEP)==2,"Unmutated children copy a parent allele per thread");
        h.assertTrue(child.fray()==1,"The first identical Frame+Fang birth under one owner starts the fray count");
        var second=FamiliarData.inherit(child,child,CreatureProfile.MOSSBACK,RandomSource.create(8L),owner,owner,0,0,0);
        var third=FamiliarData.inherit(second,second,CreatureProfile.MOSSBACK,RandomSource.create(9L),owner,owner,0,0,0);
        h.assertTrue(third.fray()>=3 && third.expressed(FamiliarData.Mark.FRAYED),"Three identical Frame+Fang generations under one owner land Frayed");
        h.assertTrue(third.phenotype(FamiliarData.Thread.HUM)<2,"Fray lowers Hum");
        var keen=new FamiliarData();keen.fill(1);keen.setMarks(FamiliarData.Mark.KEEN,FamiliarData.Mark.NONE);
        h.assertTrue(!keen.expressed(FamiliarData.Mark.KEEN),"Keen is recessive");
        keen.setMarks(FamiliarData.Mark.KEEN,FamiliarData.Mark.KEEN);
        h.assertTrue(keen.expressed(FamiliarData.Mark.KEEN),"Two Keen copies express");
        var clash=new FamiliarData();clash.fill(1);clash.setMarks(FamiliarData.Mark.STONEHIDE,FamiliarData.Mark.DRIFT);
        h.assertTrue(clash.expressed(FamiliarData.Mark.STONEHIDE) && !clash.expressed(FamiliarData.Mark.DRIFT),"Stonehide wins over Drift");
        var tag=child.save();
        var loaded=new FamiliarData();loaded.load(tag);
        h.assertTrue(loaded.rolled() && loaded.alleleA(FamiliarData.Thread.FRAME)==child.alleleA(FamiliarData.Thread.FRAME) && loaded.generation()==child.generation(),"Lattice NBT round-trips");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void latticeAppliesAndLensReads(GameTestHelper h) {
        floor(h);
        var player=VerificationPlayers.inLevel(h);
        var fox=spawn(h,CreatureProfile.LANTERN_FOX,3,2,3);
        fox.lattice().fill(1);
        fox.applyLattice();
        h.assertTrue(Math.abs(fox.getMaxHealth()-CreatureProfile.LANTERN_FOX.health)<0.2,"Phenotype 1 stays on the species baseline");
        fox.lattice().fill(4);
        fox.applyLattice();
        h.assertTrue(fox.getMaxHealth()>CreatureProfile.LANTERN_FOX.health+3,"A 4 Frame fox is tougher than wild stock");
        var pocket=spawn(h,CreatureProfile.MOSSBACK,5,2,5);
        pocket.lattice().fill(1);
        pocket.lattice().setMarks(FamiliarData.Mark.DEEP_POCKET,FamiliarData.Mark.DEEP_POCKET);
        pocket.applyLattice();
        bond(h,player,pocket);
        var closed=new MossbackMenu(1,player.getInventory(),pocket.saddlebag(),pocket);
        h.assertTrue(closed.getSlot(9).mayPlace(new ItemStack(Items.SEAGRASS)) && closed.getSlot(9).isActive(),"Deep Pocket opens the extra three slots");
        var plain=spawn(h,CreatureProfile.MOSSBACK,6,2,3);
        plain.lattice().fill(1);
        plain.applyLattice();
        bond(h,player,plain);
        var locked=new MossbackMenu(2,player.getInventory(),plain.saddlebag(),plain);
        h.assertTrue(!locked.getSlot(9).mayPlace(new ItemStack(Items.SEAGRASS)) && !locked.getSlot(9).isActive(),"Without Deep Pocket the extra slots stay closed");
        var lens=new ItemStack(LeyRegistry.LEY_LENS.get());
        player.setItemInHand(InteractionHand.MAIN_HAND,lens);
        player.setShiftKeyDown(true);
        h.assertTrue(lens.getItem().interactLivingEntity(lens,player,fox,InteractionHand.MAIN_HAND).consumesAction(),"Sneak-use of the Ley Lens reads a lattice animal");
        var lines=fox.lattice().lensLines(fox.getDisplayName());
        h.assertTrue(lines.size()>=6 && FamiliarData.dots(4).equals("●●●●○"),"The lens prints five threads as dots");
        var tag=new CompoundTag();fox.saveWithoutId(tag);
        h.assertTrue(tag.contains("Lattice"),"Bonded animals persist their lattice");
        fox.discard();pocket.discard();plain.discard();h.succeed();
    }
    private static LatticeMonster spawnMonster(GameTestHelper h,CreatureProfile profile,int x,int y,int z) {
        var mob=CreatureEntities.MONSTERS.get(profile).get().create(h.getLevel());
        var pos=h.absolutePos(new BlockPos(x,y,z));
        mob.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        mob.setNoAi(true);
        h.getLevel().addFreshEntity(mob);
        return mob;
    }
    private static void voice(GameTestHelper h,net.minecraft.server.level.ServerPlayer player,TribeDefinition tribe) {
        TribeStanding.add(player,tribe,TribeStanding.get(h.getLevel().getServer(),player.getUUID(),tribe)>=800?0:800);
    }
    private static ItemStack bondHostile(GameTestHelper h,net.minecraft.server.level.ServerPlayer player,LatticeMonster mob) {
        player.getAbilities().instabuild=false;
        var charm=new ItemStack(FamiliarRegistry.BONDING_CHARM.get(),16);
        h.assertTrue(BondingCharmItem.attempt(h.getLevel(),player,mob,charm,true) && mob.isBonded(),"A forced remnant attempt must bond");
        h.assertTrue(charm.getCount()==15,"A charm is spent on a remnant success, got "+charm.getCount());
        return charm;
    }
    @GameTest(template="empty")
    public static void remnantsNeedVoiceAndSpendCharmOnFail(GameTestHelper h) {
        floor(h);
        var player=VerificationPlayers.inLevel(h);
        player.getAbilities().instabuild=false;
        var hound=spawnMonster(h,CreatureProfile.RIFT_HOUND,3,2,3);
        var charm=new ItemStack(FamiliarRegistry.BONDING_CHARM.get(),4);
        player.setItemInHand(InteractionHand.MAIN_HAND,charm);
        charm.getItem().interactLivingEntity(charm,player,hound,InteractionHand.MAIN_HAND);
        h.assertTrue(!hound.isBonded() && charm.getCount()==4,"Without Voice the remnant keeps its distance and the charm");
        voice(h,player,TribeDefinition.CLAW);
        h.assertTrue(!BondingCharmItem.conclude(h.getLevel(),player,hound,charm,false,true) && !hound.isBonded() && charm.getCount()==3,"A failed remnant charm is spent");
        h.assertTrue(BondingCharmItem.attempt(h.getLevel(),player,hound,charm,true) && hound.isBonded() && charm.getCount()==2,"Voice plus a successful charm bonds the remnant");
        var ash=spawnMonster(h,CreatureProfile.ASHBOUND,5,2,5);
        voice(h,player,TribeDefinition.SPARK);
        var refuse=new ItemStack(FamiliarRegistry.BONDING_CHARM.get(),2);
        refuse.getItem().interactLivingEntity(refuse,player,ash,InteractionHand.MAIN_HAND);
        h.assertTrue(!ash.isBonded() && refuse.getCount()==2,"Ashbound, Rootbound, Reed Stalkers and Hollow Sentinels never answer");
        hound.discard();ash.discard();h.succeed();
    }
    @GameTest(template="empty")
    public static void companyCapsOneCombatAndTwoSupport(GameTestHelper h) {
        floor(h);
        var player=VerificationPlayers.inLevel(h);
        var at=h.absolutePos(new BlockPos(3,2,3));
        player.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
        voice(h,player,TribeDefinition.STONE);
        voice(h,player,TribeDefinition.CLAW);
        var a=spawn(h,CreatureProfile.LANTERN_FOX,2,2,2);
        var b=spawn(h,CreatureProfile.MOSSBACK,3,2,2);
        var c=spawn(h,CreatureProfile.DAWN_STAG,4,2,2);
        bond(h,player,a);bond(h,player,b);bond(h,player,c);
        h.assertTrue(!a.isSitting() && !b.isSitting() && c.isSitting(),"A third helper waits");
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        player.setShiftKeyDown(false);
        c.mobInteract(player,InteractionHand.MAIN_HAND);
        h.assertTrue(player.getVehicle()==c && !c.isSitting(),"The waiting stag can still be ridden");
        h.assertTrue(!a.isSitting() && !b.isSitting(),"A rider does not steal a helper slot");
        player.stopRiding();
        h.assertTrue(c.isSitting() && player.getVehicle()==null,"A dismount sits the stag when the helper cap is full");
        var fighter=spawnMonster(h,CreatureProfile.SHARDBACK,5,2,3);
        var hound=spawnMonster(h,CreatureProfile.RIFT_HOUND,6,2,3);
        bondHostile(h,player,fighter);
        var prey=spawnMonster(h,CreatureProfile.ASHBOUND,7,2,3);
        fighter.setLastHurtByMob(prey);
        h.assertTrue(new FamiliarOwnerTargetGoals.HurtBy(fighter).canUse(),"A fighter answers a blow");
        prey.discard();
        bondHostile(h,player,hound);
        h.assertTrue(!fighter.isSitting() && hound.isSitting(),"A second fighter waits");
        hound.hurt(h.getLevel().damageSources().cactus(),1);
        h.assertTrue(hound.isSitting(),"A waiting fighter does not steal the combat slot when hurt");
        fighter.setSitting(true);
        h.assertTrue(FamiliarSlots.tryFollow(hound) && !hound.isSitting(),"A sitter frees the combat slot");
        a.discard();b.discard();c.discard();fighter.discard();hound.discard();h.succeed();
    }
    @GameTest(template="empty")
    public static void mothClickImpRefundBellCleanseWeaverPouch(GameTestHelper h) {
        floor(h);
        var level=h.getLevel();
        var player=VerificationPlayers.inLevel(h);
        voice(h,player,TribeDefinition.CLOCK);
        voice(h,player,TribeDefinition.SPARK);
        voice(h,player,TribeDefinition.SIGIL);
        voice(h,player,TribeDefinition.SPINDLE);
        var moth=spawnMonster(h,CreatureProfile.STORM_MOTH,3,2,3);
        bondHostile(h,player,moth);
        moth.setSitting(true);
        h.assertTrue(FamiliarAbilities.placeClick(level,moth) && level.getBlockState(moth.blockPosition()).is(FamiliarRegistry.SPIRIT_CLICK.get()),"A sitting moth leaves a spirit click");
        level.getBlockState(moth.blockPosition()).tick(level,moth.blockPosition(),level.random);
        h.assertTrue(!level.getBlockState(moth.blockPosition()).is(FamiliarRegistry.SPIRIT_CLICK.get()),"The click expires on its scheduled tick");
        moth.setSitting(false);
        var stray=spawnMonster(h,CreatureProfile.ASHBOUND,6,2,6);
        moth.setLastHurtByMob(stray);
        h.assertTrue(!new FamiliarOwnerTargetGoals.HurtBy(moth).canUse(),"A helper does not pick a fight");
        stray.discard();

        h.setBlock(4,2,4,ModBlocks.DRUMHEART.get());
        var drum=(DrumheartBlockEntity)h.getBlockEntity(new BlockPos(4,2,4));
        var imp=spawnMonster(h,CreatureProfile.CINDER_IMP,4,2,5);
        bondHostile(h,player,imp);
        int before=drum.getPulseStored();
        drum.drumBeat();
        h.assertTrue(drum.getPulseStored()>before,"A hand beat stores Pulse without an Imp");
        int afterBeat=drum.getPulseStored();
        int refund=FamiliarAbilities.onOwnerDrum(level,h.absolutePos(new BlockPos(4,2,4)),player,drum);
        h.assertTrue(refund==FamiliarAbilities.IMP_REFUND && drum.getPulseStored()==afterBeat+refund,"A following Imp refunds hand-drum Pulse");
        imp.setSitting(true);
        h.assertTrue(FamiliarAbilities.onOwnerDrum(level,h.absolutePos(new BlockPos(4,2,4)),player,drum)==0,"A sitting Imp refunds nothing");
        int redstoneBefore=drum.getPulseStored();
        drum.onRedstonePulse();
        h.assertTrue(drum.getPulseStored()>=redstoneBefore,"A redstone beat never routes through the Imp refund");

        var bell=spawnMonster(h,CreatureProfile.MOURNING_BELL,2,2,5);
        bondHostile(h,player,bell);
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,200,0));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER,200,0));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,200,0));
        FamiliarAbilities.cleanse(player,false);
        h.assertTrue(!player.hasEffect(net.minecraft.world.effect.MobEffects.POISON),"The bell lifts poison");
        h.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.WITHER),"The bell never lifts Wither");
        h.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN),"Without Still, slowness stays");
        FamiliarAbilities.cleanse(player,true);
        h.assertTrue(!player.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN),"Still also lifts slowness");

        var weaver=spawnMonster(h,CreatureProfile.ECHO_WEAVER,5,2,5);
        bondHostile(h,player,weaver);
        var dropPos=weaver.position();
        var drop=new net.minecraft.world.entity.item.ItemEntity(level,dropPos.x,dropPos.y,dropPos.z,new ItemStack(Items.DIAMOND,3));
        drop.setPickUpDelay(0);
        level.addFreshEntity(drop);
        h.assertTrue(FamiliarAbilities.forage(level,weaver)>0 && !weaver.pouch().isEmpty(),"A weaver pockets nearby drops");
        var lens=new ItemStack(LeyRegistry.LEY_LENS.get());
        player.setItemInHand(InteractionHand.MAIN_HAND,lens);
        player.setShiftKeyDown(true);
        h.assertTrue(lens.getItem().interactLivingEntity(lens,player,weaver,InteractionHand.MAIN_HAND).consumesAction(),"The Ley Lens reads a remnant lattice");
        moth.discard();imp.discard();bell.discard();weaver.discard();h.succeed();
    }
    @GameTest(template="empty")
    public static void mothClickIsNotADrumAndBondedRemnantsLetYouSleep(GameTestHelper h) {
        floor(h);
        var player=VerificationPlayers.inLevel(h);
        voice(h,player,TribeDefinition.CLOCK);
        h.setBlock(4,2,4,ModBlocks.DRUMHEART.get());
        var drum=(DrumheartBlockEntity)h.getBlockEntity(new BlockPos(4,2,4));
        int before=drum.getPulseStored();
        h.setBlock(4,3,4,FamiliarRegistry.SPIRIT_CLICK.get());
        h.assertTrue(drum.getPulseStored()==before,"A moth click next to a Drumheart is not a beat");
        h.setBlock(3,2,4,Blocks.STONE);
        h.assertTrue(drum.getPulseStored()==before,"A later neighbour update still does not treat a click as a beat");
        h.setBlock(5,3,4,tk.darrow.tribalpower.logic.LogicRegistry.SONG_THREAD.get());
        var threadPos=new BlockPos(5,3,4);
        h.getBlockState(threadPos).tick(h.getLevel(),h.absolutePos(threadPos),h.getLevel().random);
        h.assertTrue(h.getBlockState(threadPos).getValue(tk.darrow.tribalpower.logic.SongThreadBlock.POWER)>0,"Song Thread still hears a moth click");
        h.setBlock(4,2,5,Blocks.REDSTONE_BLOCK);
        h.assertTrue(drum.getPulseStored()>before,"A real rising edge still beats the drum");
        var moth=spawnMonster(h,CreatureProfile.STORM_MOTH,3,2,3);
        var wild=spawnMonster(h,CreatureProfile.ASHBOUND,5,2,5);
        bondHostile(h,player,moth);
        h.assertTrue(!moth.isPreventingPlayerRest(player),"A bonded remnant does not keep the owner awake");
        h.assertTrue(wild.isPreventingPlayerRest(player),"A wild remnant still does");
        moth.setBabyFlag(true);
        h.assertTrue(moth.getBbHeight()<wild.getBbHeight(),"Remnant young are smaller");
        moth.discard();wild.discard();h.succeed();
    }
    @GameTest(template="empty")
    public static void sittingHelperKeepsTheCapWhenHurt(GameTestHelper h) {
        floor(h);
        var player=VerificationPlayers.inLevel(h);
        var at=h.absolutePos(new BlockPos(3,2,3));
        player.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
        var a=spawn(h,CreatureProfile.LANTERN_FOX,2,2,2);
        var b=spawn(h,CreatureProfile.MOSSBACK,3,2,2);
        var c=spawn(h,CreatureProfile.DAWN_STAG,4,2,2);
        bond(h,player,a);bond(h,player,b);bond(h,player,c);
        h.assertTrue(c.isSitting(),"The third helper waits");
        c.hurt(h.getLevel().damageSources().cactus(),1);
        h.assertTrue(c.isSitting(),"A waiting helper does not steal a follow slot when hurt");
        a.discard();b.discard();c.discard();h.succeed();
    }
    @GameTest(template="empty")
    public static void bondedRemnantsBreedAPersistentChild(GameTestHelper h) {
        floor(h);
        var player=VerificationPlayers.inLevel(h);
        voice(h,player,TribeDefinition.CLAW);
        var a=spawnMonster(h,CreatureProfile.RIFT_HOUND,3,2,3);
        var b=spawnMonster(h,CreatureProfile.RIFT_HOUND,4,2,3);
        bondHostile(h,player,a);bondHostile(h,player,b);
        var food=new ItemStack(CreatureItems.REAGENTS.get(CreatureProfile.RIFT_HOUND).get(),2);
        player.setItemInHand(InteractionHand.MAIN_HAND,food);
        a.mobInteract(player,InteractionHand.MAIN_HAND);
        b.mobInteract(player,InteractionHand.MAIN_HAND);
        a.aiStep();
        var young=h.getLevel().getEntitiesOfClass(LatticeMonster.class,a.getBoundingBox().inflate(8),m->m.isBaby() && m.getType()==a.getType());
        h.assertTrue(!young.isEmpty(),"Bonded remnants breed a child");
        var child=young.get(0);
        h.assertTrue(child.isBonded() && child.isPersistenceRequired() && !child.removeWhenFarAway(128*128),
                "The child joins its parents' owner, and it does not wander off — including Peaceful");
        h.assertTrue(!child.isPreventingPlayerRest(player),"Remnant young do not keep you awake");
        player.moveTo(child.getX(),child.getY(),child.getZ(),0,0);
        for(int i=0;i<40;i++)child.aiStep();
        h.assertTrue(child.getTarget()==null,"Remnant young do not hunt the player");
        child.setBabyFlag(false);
        for(int i=0;i<40;i++)child.aiStep();
        h.assertTrue(!child.isBaby() && child.isPersistenceRequired() && child.getTarget()==null
                && !child.isPreventingPlayerRest(player),
                "Grown remnant young stay, do not hunt, and still let you sleep");
        a.discard();b.discard();child.discard();h.succeed();
    }
    @GameTest(template="empty")
    public static void remnantCompanyIsNotAWardDrumTarget(GameTestHelper h) {
        floor(h);
        h.setBlock(4,2,4,tk.darrow.tribalpower.device.DeviceRegistry.WARD_DRUM.get());
        h.setBlock(4,2,5,ModBlocks.DRUMHEART.get());
        ((DrumheartBlockEntity)h.getBlockEntity(new BlockPos(4,2,5))).insertPulse(40,false);
        var drum=(tk.darrow.tribalpower.device.WorkshopBlockEntity)h.getBlockEntity(new BlockPos(4,2,4));
        var player=VerificationPlayers.inLevel(h);
        voice(h,player,TribeDefinition.CLAW);
        var hound=spawnMonster(h,CreatureProfile.RIFT_HOUND,6,2,4);
        bondHostile(h,player,hound);
        float before=hound.getHealth();
        drum.beat(h.getLevel());
        h.assertTrue(hound.getHealth()==before,"A Ward Drum must not strike bonded company");
        var cub=spawnMonster(h,CreatureProfile.RIFT_HOUND,2,2,4);
        cub.setBabyFlag(true);
        float cubBefore=cub.getHealth();
        drum.beat(h.getLevel());
        h.assertTrue(!FamiliarRoster.hostile(cub) && cub.getHealth()==cubBefore,
                "A Ward Drum must not strike persist remnant young");
        hound.discard();cub.discard();h.succeed();
    }
    @GameTest(template="empty")
    public static void aSitterStaysPutWhenTheOwnerIsGone(GameTestHelper h) {
        floor(h);
        var player=VerificationPlayers.inLevel(h);
        var fox=spawn(h,CreatureProfile.LANTERN_FOX,3,2,3);
        bond(h,player,fox);
        fox.setSitting(true);
        h.getLevel().getServer().getPlayerList().remove(player);
        h.assertTrue(!FamiliarSlots.tryFollow(fox) && fox.isSitting(),
                "A sitter does not stand when the owner is not in this world");
        fox.discard();h.succeed();
    }
    @GameTest(template="empty")
    public static void aWeaverKeepsThePouchWhenTheOwnerCannotCarry(GameTestHelper h) {
        floor(h);
        var player=VerificationPlayers.inLevel(h);
        voice(h,player,TribeDefinition.SPINDLE);
        var weaver=spawnMonster(h,CreatureProfile.ECHO_WEAVER,3,2,3);
        bondHostile(h,player,weaver);
        weaver.pouch().setItem(0,new ItemStack(Items.DIAMOND,3));
        for(int i=0;i<player.getInventory().getContainerSize();i++)
            player.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
        player.moveTo(weaver.getX(),weaver.getY(),weaver.getZ(),0,0);
        FamiliarAbilities.forage(h.getLevel(),weaver);
        h.assertTrue(weaver.pouch().getItem(0).is(Items.DIAMOND) && weaver.pouch().getItem(0).getCount()==3,
                "A full pack leaves the pouch alone instead of tossing a yo-yo");
        weaver.discard();h.succeed();
    }
    @GameTest(template="empty")
    public static void rideGenesReachTheClientCopy(GameTestHelper h) {
        floor(h);
        var stag=spawn(h,CreatureProfile.DAWN_STAG,3,2,3);
        stag.lattice().fill(4);
        stag.lattice().setMarks(FamiliarData.Mark.LEAP,FamiliarData.Mark.LEAP);
        stag.applyLattice();
        h.assertTrue(stag.rideLeap() && stag.rideStride()==4,"Leap and Stride must sit on synched data so a rider sees them");
        stag.discard();h.succeed();
    }
    @GameTest(template="empty")
    public static void aSecondFighterSitsWhenBothAreAlreadyOut(GameTestHelper h) {
        floor(h);
        var player=VerificationPlayers.inLevel(h);
        voice(h,player,TribeDefinition.STONE);
        voice(h,player,TribeDefinition.CLAW);
        var a=spawnMonster(h,CreatureProfile.SHARDBACK,3,2,3);
        var b=spawnMonster(h,CreatureProfile.RIFT_HOUND,4,2,3);
        bondHostile(h,player,a);bondHostile(h,player,b);
        a.setSitting(false);b.setSitting(false);
        a.aiStep();b.aiStep();
        h.assertTrue(a.isSitting() || b.isSitting(),"A second fighter must sit when both are already following");
        h.assertTrue(!(!a.isSitting() && !b.isSitting()),"One combat slot");
        a.discard();b.discard();h.succeed();
    }
    @GameTest(template="empty")
    public static void remnantAdultsRestAfterABirth(GameTestHelper h) {
        floor(h);
        var player=VerificationPlayers.inLevel(h);
        voice(h,player,TribeDefinition.CLAW);
        var a=spawnMonster(h,CreatureProfile.RIFT_HOUND,3,2,3);
        var b=spawnMonster(h,CreatureProfile.RIFT_HOUND,4,2,3);
        bondHostile(h,player,a);bondHostile(h,player,b);
        var food=new ItemStack(CreatureItems.REAGENTS.get(CreatureProfile.RIFT_HOUND).get(),8);
        player.setItemInHand(InteractionHand.MAIN_HAND,food);
        a.mobInteract(player,InteractionHand.MAIN_HAND);
        b.mobInteract(player,InteractionHand.MAIN_HAND);
        a.aiStep();
        int cubs=h.getLevel().getEntitiesOfClass(LatticeMonster.class,a.getBoundingBox().inflate(8),m->m.isBaby()).size();
        h.assertTrue(cubs==1,"The first birth makes one cub");
        a.mobInteract(player,InteractionHand.MAIN_HAND);
        b.mobInteract(player,InteractionHand.MAIN_HAND);
        a.aiStep();
        int after=h.getLevel().getEntitiesOfClass(LatticeMonster.class,a.getBoundingBox().inflate(8),m->m.isBaby()).size();
        h.assertTrue(after==1,"Adults rest after a birth");
        a.discard();b.discard();h.succeed();
    }
    @GameTest(template="empty")
    public static void aWildRemnantHitsAPlayer(GameTestHelper h) {
        floor(h);
        var player=VerificationPlayers.inLevel(h);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var ash=spawnMonster(h,CreatureProfile.ASHBOUND,3,2,3);
        h.assertTrue(!FamiliarOwnerTargetGoals.forbidden(ash,player),"Wild remnants may strike a player");
        var hound=spawnMonster(h,CreatureProfile.RIFT_HOUND,5,2,3);
        voice(h,player,TribeDefinition.CLAW);
        bondHostile(h,player,hound);
        h.assertTrue(FamiliarOwnerTargetGoals.forbidden(hound,player),"Bonded remnants never strike players");
        ash.discard();hound.discard();h.succeed();
    }
    @GameTest(template="empty")
    public static void remnantFoodAgesACub(GameTestHelper h) {
        floor(h);
        var player=VerificationPlayers.inLevel(h);
        var cub=spawnMonster(h,CreatureProfile.RIFT_HOUND,3,2,3);
        cub.setBaby(true);
        var food=new ItemStack(CreatureItems.REAGENTS.get(CreatureProfile.RIFT_HOUND).get(),16);
        player.setItemInHand(InteractionHand.MAIN_HAND,food);
        for(int i=0;i<12;i++)cub.mobInteract(player,InteractionHand.MAIN_HAND);
        h.assertTrue(!cub.isBaby(),"Food ages a remnant cub");
        cub.discard();h.succeed();
    }
    /** Like a wolf: a wild stag bonds after being fed wheat a few times, and wheat heals it once it is yours. */
    @GameTest(template="empty")
    public static void foodTamesAndHealsLikeAWolf(GameTestHelper h) {
        floor(h);
        var player=VerificationPlayers.inLevel(h);
        player.getAbilities().instabuild=false;
        var stag=spawn(h,CreatureProfile.DAWN_STAG,3,2,3);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WHEAT,64));
        for(int i=0;i<60 && !stag.isBonded();i++)stag.mobInteract(player,InteractionHand.MAIN_HAND);
        h.assertTrue(stag.isBonded() && stag.isOwnedBy(player),"Wheat must tame a wild stag in time");
        h.assertTrue(player.getMainHandItem().getCount()<64,"Taming feeds spend the food");
        stag.setHealth(stag.getMaxHealth()-6);
        float before=stag.getHealth();
        stag.mobInteract(player,InteractionHand.MAIN_HAND);
        h.assertTrue(stag.getHealth()>before,"Food heals a hurt companion");
        stag.discard();h.succeed();
    }

    /** Selective breeding pays: two strong parents can give a child better than either, up to the bred-only 5. */
    @GameTest(template="empty")
    public static void lineBreedingClimbsPastWildStock(GameTestHelper h) {
        var mother=new FamiliarData();mother.fill(3);
        var father=new FamiliarData();father.fill(3);
        var random=RandomSource.create(11L);
        int best=0;
        for(int i=0;i<200;i++) {
            var child=FamiliarData.inherit(mother,father,CreatureProfile.RIFT_HOUND,random,null,null);
            for(var thread:FamiliarData.Thread.values())best=Math.max(best,child.phenotype(thread));
        }
        h.assertTrue(best>=4,"Parents at 3 must sometimes breed a 4, best was "+best);
        var elite=new FamiliarData();elite.fill(5);
        h.assertTrue(elite.phenotype(FamiliarData.Thread.FANG)==5 && elite.multiplier(FamiliarData.Thread.FANG)>1.47,"An Exalted thread is +48%");
        int packed=elite.pack();
        h.assertTrue(FamiliarData.unpackThread(packed,FamiliarData.Thread.KEEP)==5 && elite.bloodline()==25,"Stats pack for the client panel");
        var tag=elite.save();var loaded=new FamiliarData();loaded.load(tag);
        h.assertTrue(loaded.phenotype(FamiliarData.Thread.FRAME)==5,"A 5 survives saving");
        h.succeed();
    }
}
