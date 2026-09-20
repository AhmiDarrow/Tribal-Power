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
        var player=VerificationPlayers.inLevel(h);var effigy=new ItemStack(CampRegistry.EFFIGY.get());BoundEffigyItem.bind(effigy,"minecraft:cow",0);
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
    @GameTest(template="empty", timeoutTicks=80)
    public static void pulseLampLightsFromLatticeAndDimsOnRedstone(GameTestHelper h){
        h.setBlock(2,2,2,ModBlocks.SHARD_LAMP.get());
        h.setBlock(3,2,4,ModBlocks.DRUMHEART.get());
        ((DrumheartBlockEntity)h.getBlockEntity(new BlockPos(3,2,4))).insertPulse(1000,false);
        h.runAfterDelay(25,()->{
            h.assertTrue(h.getBlockState(new BlockPos(2,2,2)).getValue(tk.darrow.tribalpower.block.PulseLightBlock.LIT),"Shard Lamp must light from nearby Pulse");
            h.setBlock(2,2,3,Blocks.REDSTONE_BLOCK);
            h.runAfterDelay(25,()->{
                h.assertTrue(!h.getBlockState(new BlockPos(2,2,2)).getValue(tk.darrow.tribalpower.block.PulseLightBlock.LIT),"Redstone must dim the lamp");
                h.succeed();
            });
        });
    }
    @GameTest(template="empty")
    public static void campDecorPopsWithoutAHost(GameTestHelper h){
        h.setBlock(2,1,2,Blocks.STONE);
        h.setBlock(2,2,2,ModBlocks.WOVEN_MAT.get());
        h.setBlock(4,2,2,Blocks.STONE);
        h.setBlock(3,2,2,ModBlocks.ECHO_SCONCE.get().defaultBlockState().setValue(tk.darrow.tribalpower.block.EchoSconceBlock.FACING,Direction.WEST));
        h.setBlock(2,3,4,Blocks.STONE);
        h.setBlock(2,2,4,ModBlocks.WIND_CHARM.get());
        h.setBlock(2,1,2,Blocks.AIR);
        h.assertTrue(h.getBlockState(new BlockPos(2,2,2)).isAir(),"Woven Mat pops without a floor");
        h.setBlock(4,2,2,Blocks.AIR);
        h.assertTrue(h.getBlockState(new BlockPos(3,2,2)).isAir(),"Echo Sconce pops without a wall");
        h.setBlock(2,3,4,Blocks.AIR);
        h.assertTrue(h.getBlockState(new BlockPos(2,2,4)).isAir(),"Wind Charm pops without a ceiling");
        h.succeed();
    }

    // ---- Wall Shelf ------------------------------------------------------------------------
    // The shelf holds four things along its board. It is furniture, so a player's hands are the
    // only way on and off it: no face is open to a hopper, and a comparator reads how full it is.

    /** Builds a shelf on the south face of a wall at (2,2,2) and hands back its block entity. */
    private static CampDisplayBlockEntity shelf(GameTestHelper h) {
        h.setBlock(2, 2, 2, Blocks.STONE);
        BlockPos pos = new BlockPos(2, 2, 3);
        h.setBlock(pos, ModBlocks.WALL_SHELF.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        return (CampDisplayBlockEntity) h.getBlockEntity(pos);
    }

    @GameTest(template = "empty")
    public static void shelfTakesOneItemPerPlaceAndGivesItBack(GameTestHelper h) {
        CampDisplayBlockEntity board = shelf(h);
        ItemStack held = new ItemStack(Items.DIAMOND, 3);

        ItemStack left = board.place(0, held);
        h.assertTrue(board.at(0).is(Items.DIAMOND) && board.at(0).getCount() == 1,
                "A shelf shows one item, not the stack");
        h.assertTrue(left.getCount() == 2, "The rest of the stack stays in hand");

        h.assertTrue(board.place(0, new ItemStack(Items.EMERALD)).getCount() == 1,
                "A taken place refuses a second item");
        h.assertTrue(board.at(0).is(Items.DIAMOND), "and keeps what was already there");

        h.assertTrue(board.take(0).is(Items.DIAMOND), "Taking gives the item back");
        h.assertTrue(board.at(0).isEmpty(), "and leaves the place bare");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void shelfFillsTheNextFreePlaceAndThenRefuses(GameTestHelper h) {
        CampDisplayBlockEntity board = shelf(h);
        for (int i = 0; i < CampDisplayBlockEntity.MAX_SLOTS; i++) {
            h.assertTrue(board.freeSlotFrom(0) == i, "The next free place is " + i);
            board.place(board.freeSlotFrom(0), new ItemStack(Items.STICK));
        }
        h.assertTrue(board.freeSlotFrom(0) == -1, "A full board reports no free place");
        h.assertTrue(board.filledSlotFrom(2) == 2, "Taking starts from the place clicked");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void shelfSpillsWhatItHoldsWhenBroken(GameTestHelper h) {
        CampDisplayBlockEntity board = shelf(h);
        board.place(1, new ItemStack(Items.GOLD_INGOT));
        h.setBlock(2, 2, 3, Blocks.AIR);
        h.succeedWhen(() -> h.assertItemEntityPresent(Items.GOLD_INGOT, new BlockPos(2, 2, 3), 2.0));
    }

    @GameTest(template = "empty")
    public static void shelfDropsWhenItsWallGoesAndIsClosedToHoppers(GameTestHelper h) {
        CampDisplayBlockEntity board = shelf(h);
        h.assertTrue(board.getSlotsForFace(Direction.UP).length == 0
                        && board.getSlotsForFace(Direction.SOUTH).length == 0,
                "No face is open, so hoppers and pipes leave furniture alone");
        h.assertTrue(!board.canPlaceItemThroughFace(0, new ItemStack(Items.STICK), Direction.UP),
                "Nothing can be pushed onto a shelf");

        h.assertTrue(board.signal() == 0, "A bare board reads 0");
        board.place(0, new ItemStack(Items.STICK));
        board.place(1, new ItemStack(Items.STICK));
        h.assertTrue(board.signal() > 0 && board.signal() < 15, "A half-full board reads between");
        for (int i = 2; i < CampDisplayBlockEntity.MAX_SLOTS; i++) board.place(i, new ItemStack(Items.STICK));
        h.assertTrue(board.signal() == 15, "A full board reads 15");

        h.setBlock(2, 2, 2, Blocks.AIR);
        h.assertTrue(h.getBlockState(new BlockPos(2, 2, 3)).isAir(), "The shelf falls with its wall");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tableTakesFourAndUrnTakesOne(GameTestHelper h) {
        h.setBlock(3, 1, 3, ModBlocks.MARCH_TABLE.get());
        var table = (CampDisplayBlockEntity) h.getBlockEntity(new BlockPos(3, 1, 3));
        h.assertTrue(table.capacity() == 4, "A table top has four places");
        for (int i = 0; i < 4; i++) table.place(i, new ItemStack(Items.BREAD));
        h.assertTrue(table.freeSlotFrom(0) == -1 && table.signal() == 15, "A laid table is full and reads 15");

        h.setBlock(5, 1, 3, ModBlocks.SPIRIT_URN.get());
        var urn = (CampDisplayBlockEntity) h.getBlockEntity(new BlockPos(5, 1, 3));
        h.assertTrue(urn.capacity() == 1, "An urn is a single vessel");
        urn.place(0, new ItemStack(Items.BONE));
        h.assertTrue(urn.freeSlotFrom(0) == -1, "A filled urn takes nothing more");
        h.assertTrue(urn.signal() == 15, "A filled urn reads full");
        h.assertTrue(urn.place(1, new ItemStack(Items.BONE)).getCount() == 1,
                "There is no second place in an urn to put anything");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void stoolSeatsOnePlayerAndClearsUpAfterThem(GameTestHelper h) {
        BlockPos stool = new BlockPos(4, 1, 4);
        h.setBlock(stool, ModBlocks.MARCH_STOOL.get());
        var player = VerificationPlayers.inLevel(h);

        h.assertTrue(tk.darrow.tribalpower.entity.SeatEntity.sit(h.getLevel(), h.absolutePos(stool), player, 0.4),
                "A free stool seats the player");
        h.assertTrue(player.isPassenger(), "and the player is riding it");

        var second = VerificationPlayers.inLevel(h);
        h.assertTrue(!tk.darrow.tribalpower.entity.SeatEntity.sit(h.getLevel(), h.absolutePos(stool), second, 0.4),
                "A taken stool seats nobody else");

        player.stopRiding();
        h.succeedWhen(() -> h.assertTrue(
                h.getLevel().getEntitiesOfClass(tk.darrow.tribalpower.entity.SeatEntity.class,
                        new net.minecraft.world.phys.AABB(h.absolutePos(stool)).inflate(2.0)).isEmpty(),
                "Standing up leaves no seat behind"));
    }

    @GameTest(template = "empty")
    public static void stoolHoldsNothingAndHasNoBlockEntity(GameTestHelper h) {
        h.setBlock(3, 1, 5, ModBlocks.MARCH_STOOL.get());
        h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(3, 1, 5))) == null,
                "A stool is for sitting on, so it carries no block entity");
        h.succeed();
    }
}
