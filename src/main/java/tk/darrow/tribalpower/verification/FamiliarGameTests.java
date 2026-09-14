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
    @GameTest(template="empty")
    public static void foxLightClearedOnDimensionChangeAndKeptOffWater(GameTestHelper h) {
        floor(h);
        var level=h.getLevel();var server=level.getServer();
        var player=h.makeMockServerPlayerInLevel();
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
        var player=h.makeMockServerPlayerInLevel();
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
        var player=h.makeMockServerPlayerInLevel();
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
        h.assertTrue(closed.getSlot(9).mayPlace(new ItemStack(Items.SEAGRASS)),"Deep Pocket opens the extra three slots");
        var plain=spawn(h,CreatureProfile.MOSSBACK,6,2,3);
        plain.lattice().fill(1);
        plain.applyLattice();
        bond(h,player,plain);
        var locked=new MossbackMenu(2,player.getInventory(),plain.saddlebag(),plain);
        h.assertTrue(!locked.getSlot(9).mayPlace(new ItemStack(Items.SEAGRASS)),"Without Deep Pocket the extra slots refuse inserts");
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
        var player=h.makeMockServerPlayerInLevel();
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
        var player=h.makeMockServerPlayerInLevel();
        var at=h.absolutePos(new BlockPos(3,2,3));
        player.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
        voice(h,player,TribeDefinition.STONE);
        voice(h,player,TribeDefinition.CLAW);
        var a=spawn(h,CreatureProfile.LANTERN_FOX,2,2,2);
        var b=spawn(h,CreatureProfile.MOSSBACK,3,2,2);
        var c=spawn(h,CreatureProfile.DAWN_STAG,4,2,2);
        bond(h,player,a);bond(h,player,b);bond(h,player,c);
        h.assertTrue(!a.isSitting() && !b.isSitting() && c.isSitting(),"A third helper waits");
        var fighter=spawnMonster(h,CreatureProfile.SHARDBACK,5,2,3);
        var hound=spawnMonster(h,CreatureProfile.RIFT_HOUND,6,2,3);
        bondHostile(h,player,fighter);
        bondHostile(h,player,hound);
        h.assertTrue(!fighter.isSitting() && hound.isSitting(),"A second fighter waits");
        fighter.setSitting(true);
        h.assertTrue(FamiliarSlots.tryFollow(hound) && !hound.isSitting(),"A sitter frees the combat slot");
        a.discard();b.discard();c.discard();fighter.discard();hound.discard();h.succeed();
    }
    @GameTest(template="empty")
    public static void mothClickImpRefundBellCleanseWeaverPouch(GameTestHelper h) {
        floor(h);
        var level=h.getLevel();
        var player=h.makeMockServerPlayerInLevel();
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
}
