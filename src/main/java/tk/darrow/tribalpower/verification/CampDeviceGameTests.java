package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.block.RelayBlock;
import tk.darrow.tribalpower.blockentity.DrumheartBlockEntity;
import tk.darrow.tribalpower.blockentity.WirelessRelayBlockEntity;
import tk.darrow.tribalpower.camp.BoundEffigyItem;
import tk.darrow.tribalpower.camp.CampBlockEntity;
import tk.darrow.tribalpower.camp.CampRegistry;
import tk.darrow.tribalpower.device.DeviceRegistry;
import tk.darrow.tribalpower.device.SealLoomBlockEntity;
import tk.darrow.tribalpower.device.WorkshopBlockEntity;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.lattice.SideIo;

/**
 * Camp devices and the world around them: relay plates sit on every device that holds goods and carry them, and
 * each device's work reaches vanilla creatures and blocks, not only the March's.
 */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class CampDeviceGameTests {
    private static final int RELAY_WAIT = 70;

    private static CampBlockEntity camp(GameTestHelper h, String kind, int x, int y, int z, boolean voice) {
        h.setBlock(x, y, z, CampRegistry.DEVICES.get(kind).get());
        var needed = tk.darrow.tribalpower.lattice.Voices.required(kind);
        if (voice && needed != null) h.setBlock(x, y + 3, z, ModBlocks.totemFor(needed).get());
        return (CampBlockEntity) h.getBlockEntity(new BlockPos(x, y, z));
    }

    private static void power(GameTestHelper h, int x, int y, int z) {
        h.setBlock(x, y, z, ModBlocks.DRUMHEART.get());
        Weaving.weave(h); // Pulse reaches the machine only through the lattice
        ((DrumheartBlockEntity) h.getBlockEntity(new BlockPos(x, y, z))).insertPulse(4000, false);
    }

    /** An item plate on the given face of the block next to it; the plate's FACING is the host face it lies on. */
    private static WirelessRelayBlockEntity plate(GameTestHelper h, BlockPos at, Direction hostFace) {
        h.setBlock(at, ModBlocks.ITEM_RELAY.get().defaultBlockState().setValue(RelayBlock.FACING, hostFace));
        return (WirelessRelayBlockEntity) h.getBlockEntity(at);
    }

    private static ChestBlockEntity chest(GameTestHelper h, int x, int y, int z) {
        h.setBlock(x, y, z, Blocks.CHEST);
        return (ChestBlockEntity) h.getBlockEntity(new BlockPos(x, y, z));
    }

    private static void tune(GameTestHelper h, WirelessRelayBlockEntity plate, BlockPos target, Direction face) {
        plate.bindChannel(0, h.absolutePos(target), face, h.getLevel().dimension().location().toString());
    }

    /** Holding a plate, a plain right-click (no crouch) puts it on any face of a camp device, even one whose click opens a screen. */
    @GameTest(template = "empty")
    public static void platesClickOntoEveryCampDevice(GameTestHelper h) {
        var player = FakePlayerFactory.getMinecraft(h.getLevel());
        java.util.List<Block> devices = java.util.List.of(CampRegistry.DEVICES.get("grove_tender").get(),
                CampRegistry.DEVICES.get("summoning_cradle").get(), CampRegistry.DEVICES.get("offering_table").get(),
                DeviceRegistry.WIND_SNARE.get(), DeviceRegistry.SEAL_LOOM.get());
        BlockPos device = new BlockPos(3, 3, 3);
        for (Block block : devices) {
            for (Direction face : new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH}) {
                h.setBlock(device.relative(face), Blocks.AIR);
                h.setBlock(device, block);
                player.setShiftKeyDown(false);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.ITEM_RELAY.get(), 2));
                BlockPos abs = h.absolutePos(device);
                var hit = new BlockHitResult(Vec3.atCenterOf(abs).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5), face, abs, false);
                player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
                var placed = h.getLevel().getBlockState(abs.relative(face));
                h.assertTrue(placed.is(ModBlocks.ITEM_RELAY.get()) && placed.getValue(RelayBlock.FACING) == face,
                        "A plain click puts a plate on the " + face + " of " + block + ", got " + placed);
                h.assertTrue(player.containerMenu == player.inventoryMenu, "The click placed the plate instead of opening " + block);
                h.setBlock(device.relative(face), Blocks.AIR);
            }
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.succeed();
    }

    /** A plate under a Grove Tender sends the harvest away; a plate tuned to its top feeds the seed row. */
    @GameTest(template = "empty", timeoutTicks = 140)
    public static void relayPlatesWorkTheGroveTender(GameTestHelper h) {
        var tender = camp(h, "grove_tender", 2, 3, 2, false);
        h.setBlock(0, 2, 0, ModBlocks.RESONANCE_TOTEM_AIR.get());
        power(h, 4, 2, 4);
        tender.setItem(9, new ItemStack(Items.WHEAT, 16));
        var store = chest(h, 6, 1, 2);
        tune(h, plate(h, new BlockPos(2, 2, 2), Direction.DOWN), new BlockPos(6, 1, 2), Direction.UP);
        var seeds = chest(h, 2, 1, 5);
        seeds.setItem(0, new ItemStack(Items.WHEAT_SEEDS, 16));
        tune(h, plate(h, new BlockPos(2, 2, 5), Direction.UP), new BlockPos(2, 3, 2), Direction.UP);
        h.runAfterDelay(RELAY_WAIT, () -> {
            h.assertTrue(store.countItem(Items.WHEAT) == 16, "The harvest leaves the tender's bottom through a plate, chest=" + store.countItem(Items.WHEAT));
            int row = 0;
            for (int i = 0; i < 9; i++) if (tender.getItem(i).is(Items.WHEAT_SEEDS)) row += tender.getItem(i).getCount();
            h.assertTrue(row == 16 && seeds.isEmpty(), "A plate tuned to the top fills the seed row, row=" + row);
            h.succeed();
        });
    }

    /** Spiritweave reaches the cradle's offering slot through a tuned plate; a plate on its bottom hands out the spent effigy. */
    @GameTest(template = "empty", timeoutTicks = 140)
    public static void relayPlatesWorkTheSummoningCradle(GameTestHelper h) {
        var cradle = camp(h, "summoning_cradle", 2, 3, 2, false);
        h.setBlock(0, 2, 0, ModBlocks.RESONANCE_TOTEM_AIR.get());
        power(h, 4, 2, 4);
        var spent = new ItemStack(CampRegistry.EFFIGY.get());
        BoundEffigyItem.bind(spent, "minecraft:cow", 0);
        cradle.setItem(0, spent);
        var out = chest(h, 6, 1, 2);
        tune(h, plate(h, new BlockPos(2, 2, 2), Direction.DOWN), new BlockPos(6, 1, 2), Direction.UP);
        var weave = chest(h, 2, 1, 5);
        weave.setItem(0, new ItemStack(ModItems.SPIRITWEAVE.get(), 8));
        tune(h, plate(h, new BlockPos(2, 2, 5), Direction.UP), new BlockPos(2, 3, 2), Direction.NORTH);
        h.runAfterDelay(RELAY_WAIT, () -> {
            h.assertTrue(out.countItem(CampRegistry.EFFIGY.get()) == 1 && cradle.getItem(0).isEmpty(), "The spent effigy leaves through the bottom plate");
            h.assertTrue(cradle.getItem(1).is(ModItems.SPIRITWEAVE.get()) && cradle.getItem(1).getCount() == 8, "Spiritweave arrives in the offering slot, got " + cradle.getItem(1));
            h.succeed();
        });
    }

    /** A plate on an Offering Table empties it; a face switched to Closed offers the plate nothing. */
    @GameTest(template = "empty", timeoutTicks = 140)
    public static void relayPlatesEmptyTheOfferingTableButNotAClosedFace(GameTestHelper h) {
        var open = camp(h, "offering_table", 2, 1, 2, false);
        var closed = camp(h, "offering_table", 2, 1, 5, false);
        closed.sideIo().set(Direction.UP, SideIo.Mode.NONE);
        open.setItem(4, new ItemStack(Items.BREAD, 12));
        closed.setItem(4, new ItemStack(Items.BREAD, 12));
        h.setBlock(0, 2, 0, ModBlocks.RESONANCE_TOTEM_AIR.get());
        power(h, 4, 2, 4);
        var store = chest(h, 6, 1, 2);
        tune(h, plate(h, new BlockPos(2, 2, 2), Direction.UP), new BlockPos(6, 1, 2), Direction.UP);
        tune(h, plate(h, new BlockPos(2, 2, 5), Direction.UP), new BlockPos(6, 1, 2), Direction.UP);
        h.runAfterDelay(RELAY_WAIT, () -> {
            h.assertTrue(open.isEmpty(), "A plate on the table's top takes its offerings away");
            h.assertTrue(closed.countItem(Items.BREAD) == 12, "A Closed face offers the plate no slots");
            h.assertTrue(store.countItem(Items.BREAD) == 12, "Only the open table's bread arrives, chest=" + store.countItem(Items.BREAD));
            h.succeed();
        });
    }

    /** A plate under a Wind Snare sends the catch on, the way a hopper under it would. */
    @GameTest(template = "empty", timeoutTicks = 140)
    public static void relayPlateEmptiesTheWindSnare(GameTestHelper h) {
        h.setBlock(2, 3, 2, DeviceRegistry.WIND_SNARE.get());
        var snare = (WorkshopBlockEntity) h.getBlockEntity(new BlockPos(2, 3, 2));
        snare.setItem(0, new ItemStack(Items.COBBLESTONE, 16));
        h.setBlock(0, 2, 0, ModBlocks.RESONANCE_TOTEM_AIR.get());
        power(h, 4, 2, 4);
        var store = chest(h, 6, 1, 2);
        tune(h, plate(h, new BlockPos(2, 2, 2), Direction.DOWN), new BlockPos(6, 1, 2), Direction.UP);
        h.runAfterDelay(RELAY_WAIT, () -> {
            h.assertTrue(store.countItem(Items.COBBLESTONE) == 16 && snare.isEmpty(), "The snare's catch leaves through the bottom plate, chest=" + store.countItem(Items.COBBLESTONE));
            h.succeed();
        });
    }

    /** A plate under a Seal Loom sends its results on; a plate tuned to its top stocks the grid. */
    @GameTest(template = "empty", timeoutTicks = 140)
    public static void relayPlatesWorkTheSealLoom(GameTestHelper h) {
        h.setBlock(2, 3, 2, DeviceRegistry.SEAL_LOOM.get());
        var loom = (SealLoomBlockEntity) h.getBlockEntity(new BlockPos(2, 3, 2));
        loom.setItem(SealLoomBlockEntity.OUTPUT, new ItemStack(Items.STICK, 16));
        h.setBlock(0, 2, 0, ModBlocks.RESONANCE_TOTEM_AIR.get());
        power(h, 4, 2, 4);
        var store = chest(h, 6, 1, 2);
        tune(h, plate(h, new BlockPos(2, 2, 2), Direction.DOWN), new BlockPos(6, 1, 2), Direction.UP);
        var logs = chest(h, 2, 1, 5);
        logs.setItem(0, new ItemStack(Items.OAK_LOG, 8));
        tune(h, plate(h, new BlockPos(2, 2, 5), Direction.UP), new BlockPos(2, 3, 2), Direction.UP);
        h.runAfterDelay(RELAY_WAIT, () -> {
            h.assertTrue(store.countItem(Items.STICK) == 16, "The loom's results leave through the bottom plate, chest=" + store.countItem(Items.STICK));
            int grid = 0;
            for (int i = 0; i < SealLoomBlockEntity.GRID; i++) if (loom.getItem(i).is(Items.OAK_LOG)) grid += loom.getItem(i).getCount();
            h.assertTrue(grid == 8, "A plate tuned to the top stocks the grid, grid=" + grid);
            h.succeed();
        });
    }

    /** The Ward Drum strikes vanilla hostiles of every kind, and leaves villagers and bosses alone. */
    @GameTest(template = "empty")
    public static void wardDrumStrikesVanillaHostilesButNotBosses(GameTestHelper h) {
        h.setBlock(4, 2, 4, DeviceRegistry.WARD_DRUM.get());
        h.setBlock(4, 5, 4, ModBlocks.RESONANCE_TOTEM_SPIRIT.get());
        power(h, 4, 2, 5);
        var drum = (WorkshopBlockEntity) h.getBlockEntity(new BlockPos(4, 2, 4));
        for (EntityType<? extends Mob> type : java.util.List.<EntityType<? extends Mob>>of(EntityType.SKELETON, EntityType.CREEPER,
                EntityType.SPIDER, EntityType.PHANTOM, EntityType.DROWNED, EntityType.WITCH)) {
            Mob mob = h.spawn(type, new BlockPos(6, 2, 4));
            mob.setNoAi(true);
            float before = mob.getHealth();
            drum.beat(h.getLevel());
            h.assertTrue(mob.getHealth() < before && drum.signal() == 15, "The drum strikes a vanilla " + type.getDescriptionId() + ", health=" + mob.getHealth());
            mob.discard();
        }
        for (EntityType<? extends Mob> type : java.util.List.<EntityType<? extends Mob>>of(EntityType.VILLAGER, EntityType.WITHER)) {
            Mob mob = h.spawn(type, new BlockPos(6, 2, 4));
            mob.setNoAi(true);
            float before = mob.getHealth();
            drum.beat(h.getLevel());
            h.assertTrue(mob.getHealth() == before && drum.signal() == 0, "The drum leaves a " + type.getDescriptionId() + " alone");
            mob.discard();
        }
        h.succeed();
    }

    /** A Hush Totem keeps the sky quiet too: no phantoms are rolled for a player standing in its ward. */
    @GameTest(template = "empty")
    public static void hushTotemTurnsAwayPhantoms(GameTestHelper h) {
        var totem = camp(h, "hush_totem", 4, 2, 4, true);
        totem.pulse = 16;
        totem.work(h.getLevel());
        var player = FakePlayerFactory.getMinecraft(h.getLevel());
        var inside = h.absolutePos(new BlockPos(8, 2, 4));
        player.moveTo(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5);
        var warded = NeoForge.EVENT_BUS.post(new PlayerSpawnPhantomsEvent(player, 2));
        h.assertTrue(warded.getResult() == PlayerSpawnPhantomsEvent.Result.DENY, "No phantoms for a player inside the ward, got " + warded.getResult());
        player.moveTo(inside.getX() + 60.5, inside.getY(), inside.getZ() + 0.5);
        var open = NeoForge.EVENT_BUS.post(new PlayerSpawnPhantomsEvent(player, 2));
        // tests run side by side, so another test's ward may cover the far spot; then it must deny as well
        var expected = tk.darrow.tribalpower.camp.CampHooks.warded(h.getLevel(), player.blockPosition())
                ? PlayerSpawnPhantomsEvent.Result.DENY : PlayerSpawnPhantomsEvent.Result.DEFAULT;
        h.assertTrue(open.getResult() == expected, "Outside the ward the night goes on as usual, got " + open.getResult());
        h.succeed();
    }

    /** The tender cuts a pumpkin or melon its stem set down, plants an azalea, and keeps the stem. */
    @GameTest(template = "empty")
    public static void groveTenderHarvestsGourdsAndPlantsAzaleas(GameTestHelper h) {
        var tender = camp(h, "grove_tender", 5, 2, 5, true);
        tender.owner = java.util.UUID.randomUUID();
        h.setBlock(4, 1, 3, Blocks.FARMLAND);
        h.setBlock(3, 1, 3, Blocks.STONE);
        h.setBlock(3, 2, 3, Blocks.PUMPKIN);
        h.setBlock(4, 2, 3, Blocks.ATTACHED_PUMPKIN_STEM.defaultBlockState().setValue(AttachedStemBlock.FACING, Direction.WEST));
        h.setBlock(6, 1, 7, Blocks.FARMLAND);
        h.setBlock(7, 1, 7, Blocks.STONE);
        h.setBlock(7, 2, 7, Blocks.MELON);
        h.setBlock(6, 2, 7, Blocks.ATTACHED_MELON_STEM.defaultBlockState().setValue(AttachedStemBlock.FACING, Direction.EAST));
        h.setBlock(7, 1, 3, Blocks.FARMLAND);
        var pitcher = Blocks.PITCHER_CROP.defaultBlockState().setValue(net.minecraft.world.level.block.PitcherCropBlock.AGE, 4);
        h.setBlock(7, 2, 3, pitcher);
        h.setBlock(7, 3, 3, pitcher.setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        tender.pulse = 36;
        for (int i = 0; i < 12; i++) tender.work(h.getLevel());
        h.assertTrue(h.getBlockState(new BlockPos(3, 2, 3)).isAir() && h.getBlockState(new BlockPos(7, 2, 7)).isAir(), "Both gourds are cut, reason=" + tender.status().getString());
        h.assertTrue(tender.countItem(Items.PUMPKIN) == 1 && tender.countItem(Items.MELON_SLICE) > 0, "The gourds go to the store");
        h.assertTrue(h.getBlockState(new BlockPos(7, 2, 3)).isAir() && h.getBlockState(new BlockPos(7, 3, 3)).isAir() && tender.countItem(Items.PITCHER_PLANT) == 1,
                "A grown pitcher is cut, both halves, and its plant stored");
        h.assertTrue(h.getBlockState(new BlockPos(4, 2, 3)).is(Blocks.PUMPKIN_STEM) && h.getBlockState(new BlockPos(6, 2, 7)).is(Blocks.MELON_STEM),
                "The stems stay to grow again");
        h.assertTrue(tender.canPlaceItem(0, new ItemStack(Items.AZALEA)) && tender.canPlaceItem(0, new ItemStack(Items.FLOWERING_AZALEA))
                && tender.canPlaceItem(0, new ItemStack(Items.MANGROVE_PROPAGULE)) && tender.canPlaceItem(0, new ItemStack(Items.PITCHER_POD)),
                "Azaleas, propagules and pitcher pods count as seeds");
        h.assertFalse(tender.canPlaceItem(0, new ItemStack(Items.DIRT)), "Dirt is still no seed");
        h.setBlock(7, 1, 3, Blocks.STONE);   // the pitcher's farmland, bare now, would take an azalea too
        h.setBlock(2, 1, 7, Blocks.DIRT);
        tender.setItem(0, new ItemStack(Items.AZALEA));
        tender.pulse = 4;
        for (int i = 0; i < 12 && !tender.getItem(0).isEmpty(); i++) tender.work(h.getLevel());
        h.assertTrue(h.getBlockState(new BlockPos(2, 2, 7)).is(Blocks.AZALEA), "The azalea is planted on the dirt, reason=" + tender.status().getString());
        h.succeed();
    }

    /** Any vanilla animal or monster can be bound and called, but never a boss, a villager, the trader or a water creature. */
    @GameTest(template = "empty")
    public static void cradleBindsAndCallsVanillaCreatures(GameTestHelper h) {
        for (String id : java.util.List.of("minecraft:horse", "minecraft:rabbit", "minecraft:fox", "minecraft:blaze", "minecraft:enderman", "minecraft:witch", "minecraft:bat"))
            h.assertTrue(BoundEffigyItem.allowed().contains(id), id + " can be imprinted");
        for (String id : java.util.List.of("minecraft:wither", "minecraft:ender_dragon", "minecraft:warden", "minecraft:elder_guardian", "minecraft:evoker",
                "minecraft:villager", "minecraft:wandering_trader", "minecraft:iron_golem", "minecraft:cod", "minecraft:squid", "minecraft:player"))
            h.assertFalse(BoundEffigyItem.allowed().contains(id), id + " can never be imprinted");
        var player = FakePlayerFactory.getMinecraft(h.getLevel());
        player.setShiftKeyDown(true);
        var effigy = new ItemStack(CampRegistry.EFFIGY.get());
        var villager = h.spawn(EntityType.VILLAGER, new BlockPos(1, 2, 1));
        h.assertTrue(CampRegistry.EFFIGY.get().interactLivingEntity(effigy, player, villager, InteractionHand.MAIN_HAND) == InteractionResult.FAIL
                && BoundEffigyItem.target(effigy).isEmpty(), "A villager is not imprinted");
        var horse = h.spawn(EntityType.HORSE, new BlockPos(1, 2, 3));
        CampRegistry.EFFIGY.get().interactLivingEntity(effigy, player, horse, InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(false);
        h.assertTrue("minecraft:horse".equals(BoundEffigyItem.target(effigy)), "A horse is imprinted, got " + BoundEffigyItem.target(effigy));
        villager.discard();
        horse.discard();
        var cradle = camp(h, "summoning_cradle", 4, 2, 4, true);
        cradle.pulse = 80;
        var rabbit = new ItemStack(CampRegistry.EFFIGY.get());
        BoundEffigyItem.bind(rabbit, "minecraft:rabbit", 1);
        cradle.setItem(0, rabbit);
        cradle.setItem(1, new ItemStack(ModItems.SPIRITWEAVE.get()));
        for (int x = 1; x <= 7; x++) for (int z = 1; z <= 7; z++) h.setBlock(x, 1, z, Blocks.STONE);
        h.assertTrue(cradle.summon(h.getLevel()), "The cradle calls a vanilla rabbit, reason=" + cradle.status().getString());
        h.assertTrue(!h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.Rabbit.class, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(4, 2, 4))).inflate(5)).isEmpty(),
                "The rabbit stands on the cradle floor");
        h.succeed();
    }
}
