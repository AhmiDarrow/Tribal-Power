package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.blockentity.DrumheartBlockEntity;
import tk.darrow.tribalpower.blockentity.SpiritCisternBlockEntity;
import tk.darrow.tribalpower.camp.CampBlockEntity;
import tk.darrow.tribalpower.camp.CampRegistry;
import tk.darrow.tribalpower.device.DeviceRegistry;
import tk.darrow.tribalpower.device.RecipeSealItem;
import tk.darrow.tribalpower.device.SealLoomBlockEntity;
import tk.darrow.tribalpower.device.WorkshopBlock;
import tk.darrow.tribalpower.device.WorkshopBlockEntity;
import tk.darrow.tribalpower.logic.LogicPlateBlock;
import tk.darrow.tribalpower.logic.LogicRegistry;
import tk.darrow.tribalpower.logic.SongThreadBlock;

@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class WorkshopGameTests {
    private static DrumheartBlockEntity pulse(GameTestHelper h, int x, int y, int z, int amount) {
        h.setBlock(x, y, z, ModBlocks.DRUMHEART.get());
        var drum = (DrumheartBlockEntity) h.getBlockEntity(new BlockPos(x, y, z));
        drum.insertPulse(amount, false);
        return drum;
    }

    @GameTest(template = "empty")
    public static void groveTenderHarvestsThreeHighCane(GameTestHelper h) {
        h.setBlock(5, 2, 5, CampRegistry.DEVICES.get("grove_tender").get());
        var be = (CampBlockEntity) h.getBlockEntity(new BlockPos(5, 2, 5));
        be.owner = java.util.UUID.randomUUID();
        be.pulse = 24;
        h.setBlock(4, 1, 5, Blocks.SAND);
        h.setBlock(4, 2, 5, Blocks.SUGAR_CANE);
        h.setBlock(4, 3, 5, Blocks.SUGAR_CANE);
        h.setBlock(4, 4, 5, Blocks.SUGAR_CANE);
        for (int i = 0; i < 90; i++) be.work(h.getLevel());
        h.assertTrue(h.getBlockState(new BlockPos(4, 4, 5)).isAir(), "Three-high cane must lose its top");
        h.assertTrue(h.getBlockState(new BlockPos(4, 2, 5)).is(Blocks.SUGAR_CANE), "Cane base must stay");
        h.assertTrue(be.pulse == 12, "Harvest spends 12 Pulse");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void groveTenderPlantsSapling(GameTestHelper h) {
        h.setBlock(5, 2, 5, CampRegistry.DEVICES.get("grove_tender").get());
        var be = (CampBlockEntity) h.getBlockEntity(new BlockPos(5, 2, 5));
        be.owner = java.util.UUID.randomUUID();
        be.pulse = 16;
        be.setItem(0, new ItemStack(Items.OAK_SAPLING, 2));
        h.setBlock(4, 1, 1, Blocks.DIRT);
        h.setBlock(4, 2, 1, Blocks.AIR);
        for (int i = 0; i < 90; i++) be.work(h.getLevel());
        h.assertTrue(h.getBlockState(new BlockPos(4, 2, 1)).is(Blocks.OAK_SAPLING), "Sapling must plant on dirt");
        h.assertTrue(be.pulse == 12 && be.getItem(0).getCount() == 1, "Planting spends 4 Pulse and one sapling");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void groveTenderStillHarvestsWheat(GameTestHelper h) {
        h.setBlock(5, 2, 5, CampRegistry.DEVICES.get("grove_tender").get());
        var be = (CampBlockEntity) h.getBlockEntity(new BlockPos(5, 2, 5));
        be.owner = java.util.UUID.randomUUID();
        be.pulse = 24;
        h.setBlock(4, 1, 1, Blocks.FARMLAND);
        h.setBlock(4, 2, 1, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        be.setItem(0, new ItemStack(Items.WHEAT_SEEDS));
        for (int i = 9; i < 27; i++) be.setItem(i, new ItemStack(Items.STONE, 64));
        be.work(h.getLevel());
        h.assertTrue(h.getBlockState(new BlockPos(4, 2, 1)).getValue(CropBlock.AGE) == 7 && be.pulse == 24,
                "Full inventory preserves crop and Pulse");
        for (int i = 9; i < 27; i++) be.setItem(i, ItemStack.EMPTY);
        for (int i = 0; i < 10; i++) be.work(h.getLevel());
        h.assertTrue(h.getBlockState(new BlockPos(4, 2, 1)).getValue(CropBlock.AGE) == 0 && be.pulse == 12,
                "Harvest replants and spends one operation");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tidePumpMovesWaterBothWays(GameTestHelper h) {
        h.setBlock(2, 2, 2, ModBlocks.SPIRIT_CISTERN.get());
        h.setBlock(4, 2, 2, ModBlocks.SPIRIT_CISTERN.get());
        h.setBlock(3, 2, 2, DeviceRegistry.TIDE_PUMP.get().defaultBlockState().setValue(WorkshopBlock.FACING, Direction.EAST));
        var from = (SpiritCisternBlockEntity) h.getBlockEntity(new BlockPos(4, 2, 2));
        var to = (SpiritCisternBlockEntity) h.getBlockEntity(new BlockPos(2, 2, 2));
        from.tank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        pulse(h, 3, 2, 3, 40);
        var be = (WorkshopBlockEntity) h.getBlockEntity(new BlockPos(3, 2, 2));
        be.beat(h.getLevel());
        h.assertTrue(from.tank.getFluidAmount() == 750 && to.tank.getFluidAmount() == 250,
                "Extract mode draws 250 mB from the face into the back, from=" + from.tank.getFluidAmount() + " to=" + to.tank.getFluidAmount() + " " + be.status().getString());
        var player = h.makeMockServerPlayerInLevel();
        be.cycle(player);
        be.beat(h.getLevel());
        h.assertTrue(from.tank.getFluidAmount() == 1000 && to.tank.getFluidAmount() == 0,
                "Reversed pump pushes toward the face, from=" + from.tank.getFluidAmount() + " to=" + to.tank.getFluidAmount());
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void windSnareCatchesDroppedItems(GameTestHelper h) {
        h.setBlock(4, 2, 4, DeviceRegistry.WIND_SNARE.get());
        pulse(h, 4, 2, 5, 40);
        var be = (WorkshopBlockEntity) h.getBlockEntity(new BlockPos(4, 2, 4));
        var abs = h.absolutePos(new BlockPos(6, 2, 4));
        var drop = new ItemEntity(h.getLevel(), abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, new ItemStack(Items.DIRT, 16));
        drop.setNeverPickUp();
        h.getLevel().addFreshEntity(drop);
        be.beat(h.getLevel());
        h.assertTrue(be.countItem(Items.DIRT) == 16, "Wind Snare must catch the dropped stack, reason=" + be.status().getString());
        h.assertTrue(drop.isRemoved(), "Caught item entity is discarded");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void wardDrumStrikesHostileNotPlayer(GameTestHelper h) {
        h.setBlock(4, 2, 4, DeviceRegistry.WARD_DRUM.get());
        pulse(h, 4, 2, 5, 40);
        var be = (WorkshopBlockEntity) h.getBlockEntity(new BlockPos(4, 2, 4));
        var zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(6, 2, 4));
        zombie.setHealth(20);
        float before = zombie.getHealth();
        be.beat(h.getLevel());
        h.assertTrue(zombie.getHealth() < before, "Ward Drum must strike a zombie, health=" + zombie.getHealth() + " reason=" + be.status().getString());
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void sealLoomImprintsAndCrafts(GameTestHelper h) {
        h.setBlock(4, 2, 4, DeviceRegistry.SEAL_LOOM.get());
        pulse(h, 4, 2, 5, 40);
        var loom = (SealLoomBlockEntity) h.getBlockEntity(new BlockPos(4, 2, 4));
        loom.setItem(0, new ItemStack(Items.OAK_PLANKS));
        loom.setItem(3, new ItemStack(Items.OAK_PLANKS));
        loom.setItem(SealLoomBlockEntity.SEAL, new ItemStack(DeviceRegistry.RECIPE_SEAL.get()));
        var player = h.makeMockServerPlayerInLevel();
        loom.imprint(player);
        h.assertTrue(RecipeSealItem.recipeId(loom.getItem(SealLoomBlockEntity.SEAL)) != null, "Blank seal must imprint a recipe");
        loom.setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
        loom.setItem(3, new ItemStack(Items.OAK_PLANKS, 4));
        loom.beat(h.getLevel());
        int sticks = 0;
        for (int i = SealLoomBlockEntity.OUTPUT; i < SealLoomBlockEntity.SIZE; i++)
            if (loom.getItem(i).is(Items.STICK)) sticks += loom.getItem(i).getCount();
        h.assertTrue(sticks >= 4, "Imprinted loom must craft sticks");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void songThreadCarriesDecayingPower(GameTestHelper h) {
        h.setBlock(2, 2, 2, Blocks.REDSTONE_BLOCK);
        h.setBlock(3, 2, 2, LogicRegistry.SONG_THREAD.get());
        h.setBlock(4, 2, 2, LogicRegistry.SONG_THREAD.get());
        h.setBlock(5, 2, 2, LogicRegistry.SONG_THREAD.get());
        for (int i = 0; i < 8; i++) {
            for (int x = 3; x <= 5; x++) {
                var pos = new BlockPos(x, 2, 2);
                var state = h.getBlockState(pos);
                if (state.getBlock() instanceof SongThreadBlock)
                    state.tick(h.getLevel(), h.absolutePos(pos), h.getLevel().random);
            }
        }
        int a = h.getBlockState(new BlockPos(3, 2, 2)).getValue(SongThreadBlock.POWER);
        int b = h.getBlockState(new BlockPos(5, 2, 2)).getValue(SongThreadBlock.POWER);
        h.assertTrue(a >= 14, "Thread beside a redstone block must be nearly full, got " + a);
        h.assertTrue(b == a - 2 || b == a - 1 || b < a, "Power must decay along the line, start " + a + " end " + b);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void chorusPlateAndsTwoInputs(GameTestHelper h) {
        var facing = Direction.NORTH;
        h.setBlock(4, 2, 4, LogicRegistry.CHORUS_PLATE.get().defaultBlockState().setValue(LogicPlateBlock.FACING, facing));
        h.setBlock(3, 2, 4, Blocks.REDSTONE_BLOCK);
        h.setBlock(5, 2, 4, Blocks.AIR);
        var pos = new BlockPos(4, 2, 4);
        var be = h.getBlockEntity(pos);
        tk.darrow.tribalpower.logic.LogicPlateBlockEntity.tick(h.getLevel(), h.absolutePos(pos), h.getBlockState(pos),
                (tk.darrow.tribalpower.logic.LogicPlateBlockEntity) be);
        h.assertTrue(h.getBlockState(pos).getValue(LogicPlateBlock.POWER) == 0, "Chorus stays quiet on one input");
        h.setBlock(5, 2, 4, Blocks.REDSTONE_BLOCK);
        tk.darrow.tribalpower.logic.LogicPlateBlockEntity.tick(h.getLevel(), h.absolutePos(pos), h.getBlockState(pos),
                (tk.darrow.tribalpower.logic.LogicPlateBlockEntity) be);
        h.assertTrue(h.getBlockState(pos).getValue(LogicPlateBlock.POWER) == 15, "Chorus sings when both sides are live");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void songVineCrawlsEveryFaceAndDecays(GameTestHelper h) {
        h.setBlock(2, 1, 2, Blocks.STONE);
        h.setBlock(3, 1, 2, Blocks.STONE);
        h.setBlock(4, 1, 2, Blocks.STONE);
        var vine = LogicRegistry.SONG_VINE.get().defaultBlockState().setValue(tk.darrow.tribalpower.logic.SongVineBlock.property(Direction.DOWN), true);
        h.setBlock(2, 2, 2, vine);
        h.setBlock(3, 2, 2, vine);
        h.setBlock(4, 2, 2, vine);
        h.setBlock(2, 2, 1, Blocks.REDSTONE_BLOCK);
        for (int i = 0; i < 8; i++) {
            for (int x = 2; x <= 4; x++) {
                var pos = new BlockPos(x, 2, 2);
                h.getBlockState(pos).tick(h.getLevel(), h.absolutePos(pos), h.getLevel().random);
            }
        }
        var start = (tk.darrow.tribalpower.logic.SongVineBlockEntity) h.getBlockEntity(new BlockPos(2, 2, 2));
        var end = (tk.darrow.tribalpower.logic.SongVineBlockEntity) h.getBlockEntity(new BlockPos(4, 2, 2));
        h.assertTrue(start.maxPower() >= 14, "Vine beside a redstone block must be nearly full, got " + start.maxPower());
        h.assertTrue(end.maxPower() < start.maxPower(), "Vine power must decay along the floor, start " + start.maxPower() + " end " + end.maxPower());
        var stacked = vine.setValue(tk.darrow.tribalpower.logic.SongVineBlock.property(Direction.NORTH), true);
        h.setBlock(3, 2, 2, stacked);
        h.assertTrue(tk.darrow.tribalpower.logic.SongVineBlock.count(h.getBlockState(new BlockPos(3, 2, 2))) == 2,
                "One cell must hold tendrils on more than one face");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void verseCallAnswersMatchingVerse(GameTestHelper h) {
        var facing = Direction.NORTH;
        h.setBlock(2, 2, 3, Blocks.REDSTONE_BLOCK);
        h.setBlock(6, 2, 3, Blocks.STONE);
        h.setBlock(2, 2, 2, LogicRegistry.VERSE_CALL.get().defaultBlockState().setValue(tk.darrow.tribalpower.logic.VerseLinkBlock.FACING, facing));
        h.setBlock(6, 2, 2, LogicRegistry.VERSE_ANSWER.get().defaultBlockState().setValue(tk.darrow.tribalpower.logic.VerseLinkBlock.FACING, facing));
        var callPos = new BlockPos(2, 2, 2);
        var answerPos = new BlockPos(6, 2, 2);
        var call = (tk.darrow.tribalpower.logic.VerseLinkBlockEntity) h.getBlockEntity(callPos);
        var answer = (tk.darrow.tribalpower.logic.VerseLinkBlockEntity) h.getBlockEntity(answerPos);
        tk.darrow.tribalpower.logic.VerseLinkBlockEntity.tick(h.getLevel(), h.absolutePos(callPos), h.getBlockState(callPos), call);
        tk.darrow.tribalpower.logic.VerseLinkBlockEntity.tick(h.getLevel(), h.absolutePos(answerPos), h.getBlockState(answerPos), answer);
        h.assertTrue(h.getBlockState(answerPos).getValue(tk.darrow.tribalpower.logic.VerseLinkBlock.POWER) >= 1,
                "Answer must hear a Call of the same verse");
        answer.cycle();
        tk.darrow.tribalpower.logic.VerseLinkBlockEntity.tick(h.getLevel(), h.absolutePos(answerPos), h.getBlockState(answerPos), answer);
        h.assertTrue(h.getBlockState(answerPos).getValue(tk.darrow.tribalpower.logic.VerseLinkBlock.POWER) == 0,
                "A mismatched verse must stay silent");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void songVineDropsWhenSupportBreaks(GameTestHelper h) {
        h.setBlock(2, 1, 2, Blocks.STONE);
        h.setBlock(2, 2, 1, Blocks.STONE);
        var vine = LogicRegistry.SONG_VINE.get().defaultBlockState()
                .setValue(tk.darrow.tribalpower.logic.SongVineBlock.property(Direction.DOWN), true)
                .setValue(tk.darrow.tribalpower.logic.SongVineBlock.property(Direction.NORTH), true);
        h.setBlock(2, 2, 2, vine);
        h.setBlock(2, 2, 1, Blocks.AIR);
        var vinePos = new BlockPos(2, 2, 2);
        h.getBlockState(vinePos).handleNeighborChanged(h.getLevel(), h.absolutePos(vinePos), Blocks.STONE,
                h.absolutePos(new BlockPos(2, 2, 1)), false);
        h.assertTrue(tk.darrow.tribalpower.logic.SongVineBlock.has(h.getBlockState(vinePos), Direction.DOWN),
                "Floor tendril must remain while the floor is still there");
        h.assertTrue(!tk.darrow.tribalpower.logic.SongVineBlock.has(h.getBlockState(vinePos), Direction.NORTH),
                "A tendril whose wall is gone must peel");
        h.setBlock(2, 1, 2, Blocks.AIR);
        h.getBlockState(vinePos).handleNeighborChanged(h.getLevel(), h.absolutePos(vinePos), Blocks.STONE,
                h.absolutePos(new BlockPos(2, 1, 2)), false);
        h.assertBlockNotPresent(LogicRegistry.SONG_VINE.get(), vinePos);
        int vines = 0;
        for (var item : h.getLevel().getEntitiesOfClass(ItemEntity.class, h.getBounds().inflate(8))) {
            if (item.getItem().is(LogicRegistry.SONG_VINE_ITEM.get())) vines += item.getItem().getCount();
        }
        h.assertTrue(vines >= 2, "Each peeled tendril must drop a vine, found " + vines);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void verseCallDoesNotLatchOnAdjacentAnswer(GameTestHelper h) {
        // Sit on the floor so removing the side redstone does not pop the plates.
        var facing = Direction.UP;
        h.setBlock(2, 2, 2, LogicRegistry.VERSE_CALL.get().defaultBlockState()
                .setValue(tk.darrow.tribalpower.logic.VerseLinkBlock.FACING, facing));
        h.setBlock(2, 2, 1, LogicRegistry.VERSE_ANSWER.get().defaultBlockState()
                .setValue(tk.darrow.tribalpower.logic.VerseLinkBlock.FACING, facing));
        h.setBlock(2, 2, 3, Blocks.REDSTONE_BLOCK);
        var callPos = new BlockPos(2, 2, 2);
        var answerPos = new BlockPos(2, 2, 1);
        var call = (tk.darrow.tribalpower.logic.VerseLinkBlockEntity) h.getBlockEntity(callPos);
        var answer = (tk.darrow.tribalpower.logic.VerseLinkBlockEntity) h.getBlockEntity(answerPos);
        tk.darrow.tribalpower.logic.VerseLinkBlockEntity.tick(h.getLevel(), h.absolutePos(callPos), h.getBlockState(callPos), call);
        tk.darrow.tribalpower.logic.VerseLinkBlockEntity.tick(h.getLevel(), h.absolutePos(answerPos), h.getBlockState(answerPos), answer);
        h.assertTrue(h.getBlockState(answerPos).getValue(tk.darrow.tribalpower.logic.VerseLinkBlock.POWER) >= 1,
                "Answer must hear the powered Call before the source is pulled");
        h.setBlock(2, 2, 3, Blocks.AIR);
        for (int i = 0; i < 4; i++) {
            tk.darrow.tribalpower.logic.VerseLinkBlockEntity.tick(h.getLevel(), h.absolutePos(callPos), h.getBlockState(callPos), call);
            tk.darrow.tribalpower.logic.VerseLinkBlockEntity.tick(h.getLevel(), h.absolutePos(answerPos), h.getBlockState(answerPos), answer);
        }
        h.assertTrue(h.getBlockState(callPos).getValue(tk.darrow.tribalpower.logic.VerseLinkBlock.POWER) == 0,
                "A Call must not keep singing from an adjacent Answer of the same verse");
        h.assertTrue(h.getBlockState(answerPos).getValue(tk.darrow.tribalpower.logic.VerseLinkBlock.POWER) == 0,
                "An Answer must go quiet when its Call has no input");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void sealLoomDoesNotDupeWhenOutputAlmostFull(GameTestHelper h) {
        h.setBlock(4, 2, 4, DeviceRegistry.SEAL_LOOM.get());
        pulse(h, 4, 2, 5, 40);
        var loom = (SealLoomBlockEntity) h.getBlockEntity(new BlockPos(4, 2, 4));
        loom.setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
        loom.setItem(3, new ItemStack(Items.OAK_PLANKS, 4));
        loom.setItem(SealLoomBlockEntity.SEAL, new ItemStack(DeviceRegistry.RECIPE_SEAL.get()));
        loom.imprint(h.makeMockServerPlayerInLevel());
        loom.setItem(SealLoomBlockEntity.OUTPUT, new ItemStack(Items.STICK, 62));
        for (int i = SealLoomBlockEntity.OUTPUT + 1; i < SealLoomBlockEntity.SIZE; i++)
            loom.setItem(i, new ItemStack(Items.DIRT, 64));
        loom.beat(h.getLevel());
        h.assertTrue(loom.getItem(SealLoomBlockEntity.OUTPUT).getCount() == 62,
                "Partial output must not keep a half-craft, sticks=" + loom.getItem(SealLoomBlockEntity.OUTPUT).getCount());
        h.assertTrue(loom.getItem(0).getCount() == 4 && loom.getItem(3).getCount() == 4,
                "Ingredients stay when the result cannot fully fit");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void windSnareDoesNotCatchWithoutPulse(GameTestHelper h) {
        h.setBlock(4, 2, 4, DeviceRegistry.WIND_SNARE.get());
        var be = (WorkshopBlockEntity) h.getBlockEntity(new BlockPos(4, 2, 4));
        var abs = h.absolutePos(new BlockPos(6, 2, 4));
        var drop = new ItemEntity(h.getLevel(), abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, new ItemStack(Items.DIRT, 16));
        drop.setNeverPickUp();
        h.getLevel().addFreshEntity(drop);
        be.beat(h.getLevel());
        h.assertTrue(be.countItem(Items.DIRT) == 0, "Wind Snare must not pocket items when Pulse is missing");
        h.assertTrue(!drop.isRemoved() && drop.getItem().getCount() == 16, "Unpaid catch must leave the entity on the ground");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void songThreadDoesNotLatchOnSolid(GameTestHelper h) {
        h.setBlock(2, 2, 2, Blocks.REDSTONE_BLOCK);
        h.setBlock(3, 2, 2, LogicRegistry.SONG_THREAD.get());
        h.setBlock(3, 2, 3, Blocks.STONE);
        for (int i = 0; i < 6; i++) {
            var pos = new BlockPos(3, 2, 2);
            h.getBlockState(pos).tick(h.getLevel(), h.absolutePos(pos), h.getLevel().random);
        }
        h.assertTrue(h.getBlockState(new BlockPos(3, 2, 2)).getValue(SongThreadBlock.POWER) >= 14,
                "Thread beside a redstone block must light");
        h.setBlock(2, 2, 2, Blocks.AIR);
        for (int i = 0; i < 6; i++) {
            var pos = new BlockPos(3, 2, 2);
            h.getBlockState(pos).tick(h.getLevel(), h.absolutePos(pos), h.getLevel().random);
        }
        h.assertTrue(h.getBlockState(new BlockPos(3, 2, 2)).getValue(SongThreadBlock.POWER) == 0,
                "Thread must drop when the source is gone, even next to stone");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void inversePlateDoesNotLatchOnMount(GameTestHelper h) {
        h.setBlock(4, 2, 5, Blocks.STONE);
        var pos = new BlockPos(4, 2, 4);
        h.setBlock(pos, LogicRegistry.INVERSE_PLATE.get().defaultBlockState().setValue(LogicPlateBlock.FACING, Direction.NORTH));
        var be = (tk.darrow.tribalpower.logic.LogicPlateBlockEntity) h.getBlockEntity(pos);
        for (int i = 0; i < 8; i++)
            tk.darrow.tribalpower.logic.LogicPlateBlockEntity.tick(h.getLevel(), h.absolutePos(pos), h.getBlockState(pos), be);
        h.assertTrue(h.getBlockState(pos).getValue(LogicPlateBlock.POWER) == 15,
                "Inverse with no input must stay high, not oscillate through the mount, power="
                        + h.getBlockState(pos).getValue(LogicPlateBlock.POWER));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void groveTenderPlantsCocoaOnLog(GameTestHelper h) {
        h.setBlock(5, 2, 5, CampRegistry.DEVICES.get("grove_tender").get());
        var be = (CampBlockEntity) h.getBlockEntity(new BlockPos(5, 2, 5));
        be.owner = java.util.UUID.randomUUID();
        be.pulse = 16;
        be.setItem(0, new ItemStack(Items.COCOA_BEANS, 2));
        h.setBlock(4, 2, 0, Blocks.JUNGLE_LOG);
        h.setBlock(4, 2, 1, Blocks.AIR);
        for (int i = 0; i < 90; i++) be.work(h.getLevel());
        h.assertTrue(h.getBlockState(new BlockPos(4, 2, 1)).is(Blocks.COCOA),
                "Cocoa must plant on the air beside a jungle log, state=" + h.getBlockState(new BlockPos(4, 2, 1)));
        h.assertTrue(be.pulse == 12 && be.getItem(0).getCount() == 1, "Cocoa planting spends 4 Pulse and one bean");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void machineRankPaysForSpeed(GameTestHelper h) {
        h.setBlock(4, 2, 4, DeviceRegistry.SEAL_LOOM.get());
        var loom = (SealLoomBlockEntity) h.getBlockEntity(new BlockPos(4, 2, 4));
        tk.darrow.tribalpower.item.MachineRank.apply(loom, 3);
        int seconds = 20;
        int pulse = 32;
        int time = tk.darrow.tribalpower.item.MachineRank.scaleTime(loom, seconds);
        int draw = tk.darrow.tribalpower.item.MachineRank.scalePulse(loom, pulse);
        h.assertTrue(time < seconds, "Rank 3 must shorten work, got " + time);
        h.assertTrue(draw > pulse, "Rank 3 must draw more Pulse a second, got " + draw);
        int ranked = time * draw;
        int base = seconds * pulse;
        h.assertTrue(Math.abs(ranked - base) * 10 <= base,
                "Pulse per craft must stay within 10%: " + ranked + " vs " + base);
        h.assertTrue(tk.darrow.tribalpower.item.MachineRank.itemBurst(loom, 16)
                        == tk.darrow.tribalpower.item.MachineRank.scalePulse(loom, 16),
                "Relay burst must track Pulse");
        h.assertTrue(tk.darrow.tribalpower.item.MachineRank.beatTicks(loom) < 20,
                "Ranked hands must beat faster");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void machineRankSurvivesThePlacingTick(GameTestHelper h) {
        var pos = new BlockPos(3, 2, 3);
        var stack = new ItemStack(ModBlocks.DRUMHEART.get());
        tk.darrow.tribalpower.item.MachineRank.setRank(stack, 2);
        h.assertTrue(tk.darrow.tribalpower.item.MachineRank.rank(stack) == 2, "rank must live on the item");
        h.setBlock(pos, ModBlocks.DRUMHEART.get());
        var be = h.getBlockEntity(pos);
        tk.darrow.tribalpower.item.MachineRank.copyToBlock(stack, be);
        h.assertTrue(tk.darrow.tribalpower.item.MachineRank.rank(be) == 2, "copyToBlock must write rank onto the machine");
        var drop = new ItemStack(ModBlocks.DRUMHEART.get());
        tk.darrow.tribalpower.item.MachineRank.copyToItem(be, drop);
        h.assertTrue(tk.darrow.tribalpower.item.MachineRank.rank(drop) == 2, "breaking must keep rank on the drop");

        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var ranked = new ItemStack(ModBlocks.DRUMHEART.get());
        tk.darrow.tribalpower.item.MachineRank.setRank(ranked, 3);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ranked);
        var worldPos = h.absolutePos(pos);
        var hit = new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(worldPos),
                Direction.UP, worldPos, false);
        var click = new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
                player, net.minecraft.world.InteractionHand.MAIN_HAND, worldPos, hit);
        tk.darrow.tribalpower.item.MachineRank.beforePlace(click);
        h.setBlock(pos, Blocks.AIR);
        h.setBlock(pos, ModBlocks.DRUMHEART.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        var snapshot = net.neoforged.neoforge.common.util.BlockSnapshot.create(
                h.getLevel().dimension(), h.getLevel(), worldPos);
        var place = new net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent(
                snapshot, Blocks.AIR.defaultBlockState(), player);
        tk.darrow.tribalpower.item.MachineRank.placed(place);
        h.assertTrue(tk.darrow.tribalpower.item.MachineRank.rank(h.getBlockEntity(pos)) == 3,
                "rank must survive the placing tick after the last item is consumed");
        h.succeed();
    }
}
