package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.blockentity.ResonanceTotemBlockEntity;
import tk.darrow.tribalpower.charm.CharmHooks;
import tk.darrow.tribalpower.charm.CharmSlots;
import tk.darrow.tribalpower.entity.CreatureEntities;
import tk.darrow.tribalpower.entity.CreatureProfile;
import tk.darrow.tribalpower.entity.LatticeAnimal;
import tk.darrow.tribalpower.familiar.BondingCharmItem;
import tk.darrow.tribalpower.familiar.FamiliarBoost;
import tk.darrow.tribalpower.familiar.FamiliarRegistry;
import tk.darrow.tribalpower.item.GearCell;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.item.PulseCellItem;
import tk.darrow.tribalpower.item.SpiritGear;
import tk.darrow.tribalpower.lattice.Keeping;

/** Familiars take a voice at a totem, and lend it to their keeper's magic (FamiliarBoost). */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class FamiliarVoiceGameTests {
    private static void floor(GameTestHelper h) { for(int x=0;x<8;x++)for(int z=0;z<8;z++)h.setBlock(x,1,z,Blocks.STONE); }

    /** A bonded fox that waits where it was put, so it cannot wander off mid-test. */
    private static LatticeAnimal waitingFox(GameTestHelper h,ServerPlayer player,int x,int z) {
        var fox=CreatureEntities.ANIMALS.get(CreatureProfile.LANTERN_FOX).get().create(h.getLevel());
        var pos=h.absolutePos(new BlockPos(x,2,z));
        fox.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        h.getLevel().addFreshEntity(fox);
        BondingCharmItem.conclude(h.getLevel(),player,fox,ItemStack.EMPTY,true,false);
        fox.setSitting(true);
        return fox;
    }

    private static void sneakUse(GameTestHelper h,ServerPlayer player,ItemStack stack,BlockPos rel) {
        BlockPos at=h.absolutePos(rel);
        player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        player.setShiftKeyDown(true);
        player.gameMode.useItemOn(player,h.getLevel(),stack,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at),Direction.UP,at,false));
        player.setShiftKeyDown(false);
    }

    @GameTest(template="empty")
    public static void attuningAtATotemGivesTheVoiceAndSaveKeepsIt(GameTestHelper h) {
        floor(h);
        var player=VerificationPlayers.inLevel(h);
        // a survival keeper, so the charm check below is real: creative hands the used stack's count back
        player.setGameMode(GameType.SURVIVAL);
        h.setBlock(1,2,1,ModBlocks.RESONANCE_TOTEM_FIRE.get());
        h.setBlock(6,2,1,ModBlocks.RESONANCE_TOTEM_WATER.get());
        var fox=waitingFox(h,player,4,4);
        h.assertTrue(fox.isBonded() && fox.lattice().voice()==null && fox.syncedVoice()==null,"A new bond carries no voice");

        var charm=new ItemStack(FamiliarRegistry.BONDING_CHARM.get());
        sneakUse(h,player,charm,new BlockPos(1,2,1));
        h.assertTrue(fox.lattice().voice()==Attunement.FIRE,"Sneak-using a Bonding Charm on a Fire totem gives the fox Fire, has "+fox.lattice().voice());
        h.assertTrue(fox.syncedVoice()==Attunement.FIRE,"The voice is synced for the client");
        h.assertTrue(charm.getCount()==1,"Attuning does not spend the charm");

        var tag=new CompoundTag();
        fox.saveWithoutId(tag);
        var copy=CreatureEntities.ANIMALS.get(CreatureProfile.LANTERN_FOX).get().create(h.getLevel());
        copy.load(tag);
        h.assertTrue(copy.lattice().voice()==Attunement.FIRE && copy.syncedVoice()==Attunement.FIRE,"The voice survives a save and load, has "+copy.lattice().voice());

        sneakUse(h,player,charm,new BlockPos(6,2,1));
        h.assertTrue(fox.lattice().voice()==Attunement.WATER,"Another totem re-attunes it, has "+fox.lattice().voice());

        // a familiar too far from the totem is left as it is
        h.setBlock(1,2,7,ModBlocks.RESONANCE_TOTEM_EARTH.get());
        var away=h.absolutePos(new BlockPos(7,2,0));
        fox.teleportTo(away.getX()+.5,away.getY(),away.getZ()+.5);
        sneakUse(h,player,charm,new BlockPos(1,2,7));
        h.assertTrue(fox.lattice().voice()==Attunement.WATER,"A familiar beyond "+FamiliarBoost.ATTUNE_RANGE+" blocks keeps its voice");
        fox.discard();
        h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=300)
    public static void attunedFamiliarKeepsItsTotemAnswered(GameTestHelper h) {
        floor(h);
        var player=VerificationPlayers.inLevel(h);
        h.setBlock(1,2,1,ModBlocks.RESONANCE_TOTEM_SPIRIT.get());
        h.setBlock(6,2,6,ModBlocks.RESONANCE_TOTEM_AIR.get());
        var spirit=(ResonanceTotemBlockEntity)h.getBlockEntity(new BlockPos(1,2,1));
        var air=(ResonanceTotemBlockEntity)h.getBlockEntity(new BlockPos(6,2,6));
        spirit.setAttention(0);
        air.setAttention(0);
        var fox=waitingFox(h,player,3,3);
        fox.lattice().setVoice(Attunement.SPIRIT);
        fox.applyLattice();
        h.succeedWhen(()->{
            h.assertTrue(spirit.keeping()==Keeping.State.ANSWERED,"A Spirit fox beside a quiet Spirit totem wakes it");
            h.assertTrue(air.keeping()==Keeping.State.QUIET,"It leaves a totem of another voice alone");
            fox.discard();
        });
    }

    /** Pulse one charm upkeep beat takes from the cell in slot 1. Only call on a beat (game time divisible by 40). */
    private static int upkeep(ServerPlayer player) {
        FamiliarBoost.forget(player);
        int before=PulseCellItem.getPulse(player.getInventory().getItem(1));
        CharmHooks.playerTick(new PlayerTickEvent.Post(player));
        return before-PulseCellItem.getPulse(player.getInventory().getItem(1));
    }

    @GameTest(template="empty",timeoutTicks=200)
    public static void attunedFamiliarCheapensOnlyItsVoicesCharms(GameTestHelper h) {
        floor(h);
        var player=VerificationPlayers.inLevel(h);
        player.getAbilities().instabuild=false;
        var at=h.absolutePos(new BlockPos(2,2,2));
        player.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5);
        player.getInventory().setItem(1,PulseCellItem.createFilled(PulseCellItem.CAPACITY));
        h.assertTrue(CharmSlots.of(player).equip(new ItemStack(ModItems.EMBER_CHARM.get())),"The Ember Charm (Fire) is worn");
        var fox=waitingFox(h,player,4,4);
        int[][] result={null};
        h.succeedWhen(()->{
            if(result[0]==null) {
                // the upkeep is only drawn on a beat; wait for one, then measure every case on it
                if(h.getLevel().getGameTime()%40!=0)throw new GameTestAssertException("Waiting for a charm upkeep beat");
                int bare=upkeep(player);
                fox.lattice().setVoice(Attunement.WATER);fox.applyLattice();
                int otherVoice=upkeep(player);
                fox.lattice().setVoice(Attunement.FIRE);fox.applyLattice();
                int kin=upkeep(player);
                fox.discard();
                int gone=upkeep(player);
                result[0]=new int[]{bare,otherVoice,kin,gone};
            }
            int[] r=result[0];
            h.assertTrue(r[3]==2,"With no familiar a one-voice charm costs 2 Pulse a beat as before, cost "+r[3]);
            h.assertTrue(r[0]==2,"An unattuned familiar gives no discount, cost "+r[0]);
            h.assertTrue(r[1]==2,"A familiar of another voice gives no discount, cost "+r[1]);
            h.assertTrue(r[2]==FamiliarBoost.discounted(2) && r[2]<2,"A Fire familiar near cheapens the Fire charm, cost "+r[2]);
        });
    }

    @GameTest(template="empty")
    public static void attunedFamiliarCheapensItsVoicesSpiritgear(GameTestHelper h) {
        floor(h);
        var player=VerificationPlayers.inLevel(h);
        player.getAbilities().instabuild=false;
        var at=h.absolutePos(new BlockPos(2,2,2));
        player.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5);
        player.getInventory().setItem(1,PulseCellItem.createFilled(PulseCellItem.CAPACITY));
        var blade=new ItemStack(ModItems.SPIRITGEAR_BLADE.get());
        SpiritGear.setVoice(blade,Attunement.EARTH);
        var fox=waitingFox(h,player,4,4);
        java.util.function.IntSupplier hit=()->{
            FamiliarBoost.forget(player);
            int before=PulseCellItem.getPulse(player.getInventory().getItem(1));
            h.assertTrue(GearCell.spend(player,blade,SpiritGear.hitCost(blade)),"The blow is paid");
            return before-PulseCellItem.getPulse(player.getInventory().getItem(1));
        };
        int base=SpiritGear.hitCost(blade);
        h.assertTrue(hit.getAsInt()==base,"An unattuned familiar leaves a blow at "+base+" Pulse");
        fox.lattice().setVoice(Attunement.EARTH);fox.applyLattice();
        int kin=hit.getAsInt();
        h.assertTrue(kin==FamiliarBoost.discounted(base) && kin<base,"An Earth familiar near cheapens an Earth blade's blow, cost "+kin);
        h.assertTrue(FamiliarBoost.gearChance(player,blade,0.2F)>0.2F,"It also raises the blade's voice perk odds");
        int free=0;
        for(int i=0;i<200;i++)if(FamiliarBoost.gearCost(player,blade,1)==0)free++;
        h.assertTrue(free>40 && free<120,"A 1-Pulse spend cannot round lower, so it is free about "+Math.round(FamiliarBoost.DISCOUNT*100)+"% of the time, free "+free+" of 200");
        fox.discard();
        h.assertTrue(hit.getAsInt()==base,"Without the familiar the blow costs "+base+" again");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void onlyTheNearestSittingFamiliarLendsItsVoice(GameTestHelper h) {
        floor(h);
        var player=VerificationPlayers.inLevel(h);
        var at=h.absolutePos(new BlockPos(1,2,1));
        player.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5);
        var near=waitingFox(h,player,3,3);
        var far=waitingFox(h,player,7,7);
        near.lattice().setVoice(Attunement.FIRE);near.applyLattice();
        far.lattice().setVoice(Attunement.WATER);far.applyLattice();
        FamiliarBoost.forget(player);
        h.assertTrue(FamiliarBoost.voices(player).equals(java.util.Set.of(Attunement.FIRE)),"Of two sitting familiars only the nearest counts, has "+FamiliarBoost.voices(player));
        far.setSitting(false);
        FamiliarBoost.forget(player);
        h.assertTrue(FamiliarBoost.voices(player).equals(java.util.Set.of(Attunement.FIRE,Attunement.WATER)),"A following familiar counts beside the sitting one, has "+FamiliarBoost.voices(player));
        near.discard();
        far.discard();
        h.succeed();
    }
}
