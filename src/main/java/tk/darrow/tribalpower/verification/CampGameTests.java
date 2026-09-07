package tk.darrow.tribalpower.verification;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.*;
import tk.darrow.tribalpower.camp.*;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.blockentity.*;
import tk.darrow.tribalpower.item.ModItems;

@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class CampGameTests {
    private static CampBlockEntity camp(GameTestHelper h,String kind,int x,int y,int z){h.setBlock(x,y,z,CampRegistry.DEVICES.get(kind).get());return (CampBlockEntity)h.getBlockEntity(new BlockPos(x,y,z));}
    @GameTest(template="empty")
    public static void effigyThreadsPersistAndCannotBecomeInfinite(GameTestHelper h){
        var effigy=new ItemStack(CampRegistry.EFFIGY.get());BoundEffigyItem.bind(effigy,"minecraft:cow",512);
        for(int i=0;i<511;i++)BoundEffigyItem.spend(effigy);
        var restored=ItemStack.parse(h.getLevel().registryAccess(),effigy.save(h.getLevel().registryAccess())).orElseThrow();
        h.assertTrue(BoundEffigyItem.remaining(restored)==1,"Pickup/save must preserve the exact remaining thread");
        BoundEffigyItem.spend(restored);BoundEffigyItem.spend(restored);
        h.assertTrue(BoundEffigyItem.remaining(restored)==0,"Spent bindings must never wrap or refill");
        BoundEffigyItem.bind(restored,"minecraft:wither",Integer.MAX_VALUE);
        h.assertTrue(BoundEffigyItem.remaining(restored)==0,"Bosses cannot be bound, including corrupted item data");h.succeed();
    }
    @GameTest(template="empty")
    public static void bindingRitualRequiresAllVoicesAndSpendsOnce(GameTestHelper h){
        var player=h.makeMockServerPlayerInLevel();var effigy=new ItemStack(CampRegistry.EFFIGY.get());BoundEffigyItem.bind(effigy,"minecraft:cow",0);
        player.setItemInHand(InteractionHand.MAIN_HAND,effigy);player.getInventory().add(new ItemStack(ModItems.SPIRITWEAVE.get(),3));
        h.setBlock(4,2,4,ModBlocks.RITUAL_BRAZIER.get());var pos=h.absolutePos(new BlockPos(4,2,4));
        var brazier=(RitualBrazierBlockEntity)h.getLevel().getBlockEntity(pos);brazier.setSeal(new ItemStack(ModItems.SPIRIT_SEAL.get()));
        h.setBlock(3,2,4,ModBlocks.DRUMHEART.get());var drum=(DrumheartBlockEntity)h.getBlockEntity(new BlockPos(3,2,4));drum.insertPulse(200,false);
        var use=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false));
        CampRegistry.EFFIGY.get().useOn(use);
        h.assertTrue(BoundEffigyItem.remaining(effigy)==0&&drum.getPulseStored()==200,"Missing voices must spend no Pulse");
        h.setBlock(4,2,3,ModBlocks.RESONANCE_TOTEM_EARTH.get());h.setBlock(5,2,4,ModBlocks.RESONANCE_TOTEM_AIR.get());h.setBlock(4,2,5,ModBlocks.RESONANCE_TOTEM_SPIRIT.get());
        CampRegistry.EFFIGY.get().useOn(use);
        h.assertTrue(BoundEffigyItem.remaining(effigy)==512&&drum.getPulseStored()==0,"Completed ritual binds 512 threads for 200 Pulse");
        h.assertTrue(player.getInventory().countItem(ModItems.SPIRITWEAVE.get())==0,"Completed ritual consumes three Spiritweave");h.succeed();
    }
    @GameTest(template="empty")
    public static void redstoneAndBlockedSummonsSpendNothing(GameTestHelper h){
        var be=camp(h,"summoning_cradle",4,2,4);be.pulse=160;var effigy=new ItemStack(CampRegistry.EFFIGY.get());BoundEffigyItem.bind(effigy,"minecraft:cow",2);be.setItem(0,effigy);be.setItem(1,new ItemStack(ModItems.SPIRITWEAVE.get(),2));
        h.setBlock(5,2,4,Blocks.REDSTONE_BLOCK);
        h.assertTrue(!be.summon(h.getLevel())&&be.pulse==160&&BoundEffigyItem.remaining(effigy)==2,"Redstone must spend nothing");
        h.setBlock(5,2,4,Blocks.AIR);
        for(int x=1;x<=7;x++)for(int z=1;z<=7;z++)h.setBlock(x,1,z,Blocks.AIR);
        h.assertTrue(!be.summon(h.getLevel())&&be.pulse==160&&be.getItem(1).getCount()==2&&BoundEffigyItem.remaining(effigy)==2,"No safe floor must preserve offering, threads and Pulse");h.succeed();
    }
    @GameTest(template="empty")
    public static void successfulSummonSpendsExactlyOneThread(GameTestHelper h){
        var be=camp(h,"summoning_cradle",4,2,4);be.pulse=80;var effigy=new ItemStack(CampRegistry.EFFIGY.get());BoundEffigyItem.bind(effigy,"minecraft:chicken",1);be.setItem(0,effigy);be.setItem(1,new ItemStack(ModItems.SPIRITWEAVE.get()));
        for(int x=1;x<=7;x++)for(int z=1;z<=7;z++)h.setBlock(x,1,z,Blocks.STONE);
        h.assertTrue(be.summon(h.getLevel()),"Prepared cradle must summon on a safe floor");
        h.assertTrue(be.pulse==0&&be.getItem(1).isEmpty()&&BoundEffigyItem.remaining(effigy)==0,"Only successful spawning spends one of each cost");
        h.assertTrue(!be.summon(h.getLevel()),"Exhausted effigy cannot summon again");h.succeed();
    }
    @GameTest(template="empty")
    public static void wardStopsImmediatelyOnRedstone(GameTestHelper h){
        var be=camp(h,"hush_totem",4,2,4);be.pulse=16;be.work(h.getLevel());var pos=be.getBlockPos();
        h.assertTrue(CampHooks.warded(h.getLevel(),pos.offset(10,0,0)),"Paid ward protects its radius");
        h.assertTrue(!CampHooks.warded(h.getLevel(),pos.offset(25,0,0)),"Ward has a bounded radius");
        h.setBlock(5,2,4,Blocks.REDSTONE_BLOCK);h.assertTrue(!CampHooks.warded(h.getLevel(),pos),"Redstone immediately silences ward");h.succeed();
    }
    @GameTest(template="empty")
    public static void anchorRemovalReleasesTicketWithoutResurrection(GameTestHelper h){
        var be=camp(h,"wayanchor",4,2,4);be.pulse=32;be.work(h.getLevel());var pos=be.getBlockPos();var chunk=new net.minecraft.world.level.ChunkPos(pos);
        h.assertTrue(!CampHooks.TICKETS.forceChunk(h.getLevel(),pos,chunk.x,chunk.z,true,true),"Paid anchor already owns its ticket");
        h.getLevel().setBlock(pos,be.getBlockState().setValue(CampBlock.LIT,true),3);
        h.setBlock(4,2,4,Blocks.AIR);
        h.assertTrue(h.getLevel().getBlockState(pos).isAir(),"Lit anchor removal must not resurrect the block");
        h.assertTrue(!CampHooks.TICKETS.forceChunk(h.getLevel(),pos,chunk.x,chunk.z,false,true),"Removed anchor must leave no ticket");h.succeed();
    }
    @GameTest(template="empty")
    public static void cropTenderPreservesFullOutputThenHarvests(GameTestHelper h){
        var be=camp(h,"grove_tender",5,2,5);be.owner=java.util.UUID.randomUUID();be.pulse=24;
        h.setBlock(4,1,1,Blocks.FARMLAND);h.setBlock(4,2,1,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7));
        be.setItem(0,new ItemStack(Items.WHEAT_SEEDS));for(int i=9;i<27;i++)be.setItem(i,new ItemStack(Items.STONE,64));
        be.work(h.getLevel());h.assertTrue(h.getBlockState(new BlockPos(4,2,1)).getValue(CropBlock.AGE)==7&&be.pulse==24,"Full inventory preserves crop and Pulse");
        for(int i=9;i<27;i++)be.setItem(i,ItemStack.EMPTY);
        for(int i=0;i<10;i++)be.work(h.getLevel());
        h.assertTrue(h.getBlockState(new BlockPos(4,2,1)).getValue(CropBlock.AGE)==0&&be.pulse==12,"Harvest replants and spends one operation");h.succeed();
    }
}
