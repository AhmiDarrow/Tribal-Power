package tk.darrow.tribalpower.verification;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.item.PulseCellItem;
import tk.darrow.tribalpower.item.SpiritGear;
import tk.darrow.tribalpower.item.TreeFelling;
import tk.darrow.tribalpower.world.MarchTreeFeature;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** The Earth-voiced Spiritgear axe fells whole trees, and only trees. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class TreeFellingGameTests {
    private static final BlockPos BASE = new BlockPos(3, 2, 3);
    private static final int HEIGHT = 5;

    /** A survival feller (mock players count as creative) with an Earth axe and a 200 Pulse cell. */
    private static FakePlayer feller(GameTestHelper h, String name) {
        FakePlayer player = FakePlayerFactory.get(h.getLevel(),
                new GameProfile(UUID.nameUUIDFromBytes(("tribalpower_feller_" + name).getBytes()), "tp_" + name));
        equip(h, player);
        return player;
    }

    private static void equip(GameTestHelper h, ServerPlayer player) {
        player.getInventory().clearContent();
        player.setShiftKeyDown(false);
        BlockPos stand = h.absolutePos(BASE.offset(2, 0, 0));
        player.setPos(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
        ItemStack axe = new ItemStack(ModItems.SPIRITGEAR_AXE.get());
        SpiritGear.setVoice(axe, Attunement.EARTH);
        player.setItemInHand(InteractionHand.MAIN_HAND, axe);
        player.setItemInHand(InteractionHand.OFF_HAND, PulseCellItem.createFilled(200));
    }

    /** A five-log oak; with leaves, a crown of natural leaves around its top two logs and over it. */
    private static void oak(GameTestHelper h, boolean leaves, boolean placed) {
        h.setBlock(BASE.below(), Blocks.DIRT);
        for (int y = 0; y < HEIGHT; y++) h.setBlock(BASE.above(y), Blocks.OAK_LOG);
        if (!leaves) return;
        var leaf = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1).setValue(LeavesBlock.PERSISTENT, placed);
        for (int y = HEIGHT - 2; y <= HEIGHT; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            BlockPos pos = BASE.offset(x, y, z);
            if (h.getBlockState(pos).isAir()) h.setBlock(pos, leaf);
        }
    }

    private static int logsStanding(GameTestHelper h) {
        int standing = 0;
        for (int y = 0; y < HEIGHT; y++) if (h.getBlockState(BASE.above(y)).is(Blocks.OAK_LOG)) standing++;
        return standing;
    }

    private static int logDrops(GameTestHelper h) {
        int count = 0;
        for (ItemEntity item : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(BASE)).inflate(8, 10, 8)))
            if (item.getItem().is(Items.OAK_LOG)) count += item.getItem().getCount();
        return count;
    }

    /** Cancels the break of every log {@code guarded} names, as a protection mod would, while {@code strike} runs. */
    private static void guarded(ServerPlayer player, java.util.function.Predicate<BlockPos> guarded, Runnable strike) {
        Consumer<BlockEvent.BreakEvent> guard = event -> {
            if (event.getPlayer() == player && guarded.test(event.getPos())) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(guard);
        try {
            strike.run();
        } finally {
            NeoForge.EVENT_BUS.unregister(guard);
        }
    }

    /** A planned March tree as a world of its own, so the felling flood can be run over trees too big for a test. */
    private static BlockGetter planned(MarchTreeFeature.Plan plan) {
        return new BlockGetter() {
            @Override
            public BlockEntity getBlockEntity(BlockPos pos) {
                return null;
            }

            @Override
            public BlockState getBlockState(BlockPos pos) {
                return plan.planned(pos);
            }

            @Override
            public FluidState getFluidState(BlockPos pos) {
                return Fluids.EMPTY.defaultFluidState();
            }

            @Override
            public int getHeight() {
                return 4096;
            }

            @Override
            public int getMinBuildHeight() {
                return -2048;
            }
        };
    }

    @GameTest(template = "empty")
    public static void earthAxeFellsTheWholeTreeFromItsFoot(GameTestHelper h) {
        oak(h, true, false);
        FakePlayer player = feller(h, "foot");
        ItemStack axe = player.getMainHandItem();
        ItemStack cell = player.getOffhandItem();
        h.assertTrue(player.gameMode.destroyBlock(h.absolutePos(BASE)), "The struck log breaks");
        h.assertTrue(logsStanding(h) == 0, "Struck at its foot, the whole tree comes down, " + logsStanding(h) + " logs stand");
        h.assertTrue(logDrops(h) == HEIGHT, "Every log drops as if the player broke it, got " + logDrops(h));
        // The struck log and the four felled ones are all paid in Pulse, so none of them wears the edge.
        h.assertTrue(axe.getDamageValue() == 0, "A paid felling takes no wear, damage " + axe.getDamageValue());
        // 2 Pulse for the swing, and one more swing's worth for up to 16 felled logs.
        h.assertTrue(PulseCellItem.getPulse(cell) == 196, "Swing plus one felling charge, cell at " + PulseCellItem.getPulse(cell));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void earthAxeFellsTheLogsBelowAMiddleStrikeToo(GameTestHelper h) {
        oak(h, true, false);
        FakePlayer player = feller(h, "middle");
        h.assertTrue(player.gameMode.destroyBlock(h.absolutePos(BASE.above(2))), "The struck log breaks");
        h.assertTrue(logsStanding(h) == 0, "Struck in the middle, the logs above and below fall, " + logsStanding(h) + " stand");
        h.assertTrue(h.getBlockState(BASE.below()).is(Blocks.DIRT), "The ground under the tree stays");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aLeaflessPillarIsNotATree(GameTestHelper h) {
        oak(h, false, false);
        FakePlayer player = feller(h, "pillar");
        ItemStack axe = player.getMainHandItem();
        player.gameMode.destroyBlock(h.absolutePos(BASE));
        h.assertTrue(logsStanding(h) == HEIGHT - 1, "Only the struck log of a bare pillar breaks, " + logsStanding(h) + " stand");
        h.assertTrue(axe.getDamageValue() == 0, "No felling, no wear");
        h.assertTrue(PulseCellItem.getPulse(player.getOffhandItem()) == 198, "No felling charge");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void placedLeavesDoNotMakeATree(GameTestHelper h) {
        oak(h, true, true);
        FakePlayer player = feller(h, "placed");
        player.gameMode.destroyBlock(h.absolutePos(BASE));
        h.assertTrue(logsStanding(h) == HEIGHT - 1, "Persistent (placed) leaves do not make a build a tree, " + logsStanding(h) + " stand");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void sneakingCutsOneLog(GameTestHelper h) {
        oak(h, true, false);
        FakePlayer player = feller(h, "sneak");
        player.setShiftKeyDown(true);
        player.gameMode.destroyBlock(h.absolutePos(BASE.above(2)));
        player.setShiftKeyDown(false);
        h.assertTrue(logsStanding(h) == HEIGHT - 1, "Crouching cuts only the struck log, " + logsStanding(h) + " stand");
        h.assertTrue(h.getBlockState(BASE.above(2)).isAir(), "The struck log is the one that goes");
        h.assertTrue(PulseCellItem.getPulse(player.getOffhandItem()) == 198, "No felling charge while crouching");
        h.succeed();
    }

    /** Paid in Pulse, a felling never touches the edge: an axe on its very last point fells the whole tree and lives. */
    @GameTest(template = "empty")
    public static void aWornAxeFellsWithoutBreaking(GameTestHelper h) {
        oak(h, true, false);
        FakePlayer player = feller(h, "worn");
        ItemStack axe = player.getMainHandItem();
        axe.setDamageValue(axe.getMaxDamage() - 1);
        player.gameMode.destroyBlock(h.absolutePos(BASE));
        h.assertTrue(player.getMainHandItem() == axe && !axe.isEmpty(), "The axe survives the felling");
        h.assertTrue(axe.getDamageValue() == axe.getMaxDamage() - 1, "It is still on its last point, damage " + axe.getDamageValue());
        h.assertTrue(logsStanding(h) == 0, "The whole tree comes down, " + logsStanding(h) + " logs stand");
        h.succeed();
    }

    /** A felling is only as big as the Pulse the axe can reach: with only the swing's own, just the struck log goes. */
    @GameTest(template = "empty")
    public static void aFellingStopsWhereThePulseRunsOut(GameTestHelper h) {
        oak(h, true, false);
        FakePlayer player = feller(h, "dry");
        PulseCellItem.setPulse(player.getOffhandItem(), 2);
        player.gameMode.destroyBlock(h.absolutePos(BASE.above(2)));
        h.assertTrue(logsStanding(h) == HEIGHT - 1, "No Pulse left for a run of logs, so only the struck log falls, " + logsStanding(h) + " stand");
        h.assertTrue(PulseCellItem.getPulse(player.getOffhandItem()) == 0, "The swing took its 2 Pulse");
        h.assertTrue(player.getMainHandItem().getDamageValue() == 0, "The paid swing took no wear");
        h.succeed();
    }

    /** A 4x4x4 block of trunk under a natural crown: 64 logs, more than one tick's batch. */
    @GameTest(template = "empty")
    public static void aBigTreeKeepsFallingOverTheNextTicks(GameTestHelper h) {
        BlockPos corner = new BlockPos(5, 2, 5);
        var leaf = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1);
        for (int x = -1; x <= 4; x++) for (int z = -1; z <= 4; z++) h.setBlock(corner.offset(x, 4, z), leaf);
        for (int x = 0; x < 4; x++) for (int z = 0; z < 4; z++) {
            h.setBlock(corner.offset(x, -1, z), Blocks.DIRT);
            for (int y = 0; y < 4; y++) h.setBlock(corner.offset(x, y, z), Blocks.OAK_LOG);
        }
        // A real (mock) player in survival: fake players fell a whole tree at once, players a batch a tick.
        ServerPlayer player = VerificationPlayers.inLevel(h);
        try {
            player.setGameMode(GameType.SURVIVAL);
            player.getAbilities().instabuild = false;
            equip(h, player);
            ItemStack axe = player.getMainHandItem();
            h.assertTrue(player.gameMode.destroyBlock(h.absolutePos(corner)), "The struck log breaks");
            int standing = 0;
            for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
                if (h.getBlockState(corner.offset(x, y, z)).is(Blocks.OAK_LOG)) standing++;
            h.assertTrue(standing == 63 - TreeFelling.PER_TICK, "The swing fells one batch, top down, " + standing + " logs stand");
            h.assertTrue(h.getBlockState(corner.offset(3, 3, 3)).isAir(), "The top comes down first");
            TreeFelling.tick(player);
            for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
                h.assertTrue(!h.getBlockState(corner.offset(x, y, z)).is(Blocks.OAK_LOG), "The next tick fells the rest, " + corner.offset(x, y, z) + " stands");
            h.assertTrue(logDrops(h) == 64, "Every log drops, got " + logDrops(h));
            h.assertTrue(axe.getDamageValue() == 0, "Paid logs take no wear, damage " + axe.getDamageValue());
            // 2 Pulse for the swing, and 2 for each run of 16 felled logs as it starts: four runs for 63 logs.
            h.assertTrue(PulseCellItem.getPulse(player.getOffhandItem()) == 190, "Swing plus four runs, cell at " + PulseCellItem.getPulse(player.getOffhandItem()));
            TreeFelling.tick(player);
            h.assertTrue(PulseCellItem.getPulse(player.getOffhandItem()) == 190, "A finished felling charges nothing more");
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aProtectedLogStandsAndTheRestStillFall(GameTestHelper h) {
        oak(h, true, false);
        FakePlayer player = feller(h, "guarded");
        ItemStack axe = player.getMainHandItem();
        BlockPos keep = h.absolutePos(BASE.above(2));
        guarded(player, keep::equals, () -> player.gameMode.destroyBlock(h.absolutePos(BASE)));
        h.assertTrue(h.getBlockState(BASE.above(2)).is(Blocks.OAK_LOG), "The log protection refused stands");
        h.assertTrue(logsStanding(h) == 1, "Every other log still falls, " + logsStanding(h) + " stand");
        h.assertTrue(logDrops(h) == HEIGHT - 1, "Only felled logs drop, got " + logDrops(h));
        h.assertTrue(axe.getDamageValue() == 0, "Paid felled logs wear nothing, damage " + axe.getDamageValue());
        h.assertTrue(PulseCellItem.getPulse(player.getOffhandItem()) == 196, "One run paid, cell at " + PulseCellItem.getPulse(player.getOffhandItem()));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aFullyProtectedTreeCostsNoFellingPulse(GameTestHelper h) {
        oak(h, true, false);
        FakePlayer player = feller(h, "warded");
        BlockPos struck = h.absolutePos(BASE);
        guarded(player, pos -> !pos.equals(struck), () -> player.gameMode.destroyBlock(struck));
        h.assertTrue(logsStanding(h) == HEIGHT - 1, "Protection keeps every felled log, " + logsStanding(h) + " stand");
        h.assertTrue(player.getMainHandItem().getDamageValue() == 0, "No log felled, no wear");
        h.assertTrue(PulseCellItem.getPulse(player.getOffhandItem()) == 198,
                "The run that felled nothing is handed back, cell at " + PulseCellItem.getPulse(player.getOffhandItem()));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aCabinGrownIntoATreeIsNotFelled(GameTestHelper h) {
        oak(h, true, false);
        // A cabin corner of the same oak logs against the trunk, with planks laid on its wall.
        for (int z = 1; z <= 3; z++) h.setBlock(BASE.offset(0, 0, z), Blocks.OAK_LOG);
        h.setBlock(BASE.offset(0, 1, 3), Blocks.OAK_LOG);
        h.setBlock(BASE.offset(0, 1, 2), Blocks.OAK_PLANKS);
        FakePlayer player = feller(h, "cabin");
        player.gameMode.destroyBlock(h.absolutePos(BASE.above(2)));
        h.assertTrue(logsStanding(h) == HEIGHT - 1, "Wood touching a build is not felled, " + logsStanding(h) + " tree logs stand");
        for (int z = 1; z <= 3; z++) h.assertTrue(h.getBlockState(BASE.offset(0, 0, z)).is(Blocks.OAK_LOG), "The cabin stands");
        h.assertTrue(PulseCellItem.getPulse(player.getOffhandItem()) == 198, "No felling charge");
        h.succeed();
    }

    /** The caps must fit the March's own trees, the Weeping Colossus most of all, or they would never fell. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void fellingCapsFitTheMarchTrees(GameTestHelper h) {
        for (long seed = 1; seed <= 3; seed++) {
            int colossus = MarchTreeFeature.grow(MarchTreeFeature.Shape.WEEPING_COLOSSUS, BlockPos.ZERO, seed, 1.1).logCount();
            h.assertTrue(colossus > TreeFelling.CAP && colossus <= TreeFelling.WILLOW_CAP,
                    "A largest-size colossus (" + colossus + " logs) fits the willow cap " + TreeFelling.WILLOW_CAP);
            for (MarchTreeFeature.Shape shape : MarchTreeFeature.Shape.values()) {
                if (shape == MarchTreeFeature.Shape.WEEPING_COLOSSUS || shape == MarchTreeFeature.Shape.YOUNG_WILLOW) continue;
                int logs = MarchTreeFeature.grow(shape, BlockPos.ZERO, seed, 1.0).logCount();
                h.assertTrue(logs <= TreeFelling.CAP, shape + " (" + logs + " logs) fits the tree cap " + TreeFelling.CAP);
            }
            int young = MarchTreeFeature.grow(MarchTreeFeature.Shape.YOUNG_WILLOW, BlockPos.ZERO, seed, 1.0).logCount();
            h.assertTrue(young <= TreeFelling.WILLOW_CAP, "A young willow fits the willow cap");
        }
        h.succeed();
    }

    /**
     * Every leafy March tree, struck at its foot, has the leaves to count as one; and a colossus, far bigger than one
     * cell's Pulse fells, gives a 200 Pulse cell (and one four times that) its crown, the scan stopping long before
     * the whole tree.
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void everyMarchTreeFellsFromItsFoot(GameTestHelper h) {
        // what a 200 Pulse cell fells, at 2 Pulse for every run of 16 logs
        int fresh = 200 / 2 * TreeFelling.LOGS_PER_CHARGE;
        for (long seed = 1; seed <= 2; seed++) {
            for (MarchTreeFeature.Shape shape : MarchTreeFeature.Shape.values()) {
                // A cinder snag may grow no tufts at all, and is then deadwood rather than a tree.
                if (shape == MarchTreeFeature.Shape.CINDER_SNAG) continue;
                boolean colossus = shape == MarchTreeFeature.Shape.WEEPING_COLOSSUS;
                MarchTreeFeature.Plan plan = MarchTreeFeature.grow(shape, BlockPos.ZERO, seed, colossus ? 1.1 : 1.0);
                BlockPos foot = plan.foot();
                Block wood = plan.planned(foot).getBlock();
                int cap = colossus || shape == MarchTreeFeature.Shape.YOUNG_WILLOW ? TreeFelling.WILLOW_CAP : TreeFelling.CAP;
                if (!colossus) {
                    List<BlockPos> whole = TreeFelling.tree(planned(plan), pos -> true, foot, wood, cap, Integer.MAX_VALUE);
                    h.assertTrue(!whole.isEmpty(), shape + " (seed " + seed + ") struck at its foot is a tree to the axe");
                    continue;
                }
                for (int room : new int[]{fresh, fresh * 4}) {
                    List<BlockPos> first = TreeFelling.tree(planned(plan), pos -> true, foot, wood, cap, room);
                    h.assertTrue(first.size() == room, "Pulse good for " + room + " logs fells that many of a colossus (seed " + seed + "), got " + first.size());
                    h.assertTrue(first.getLast().getY() > foot.getY() + 40,
                            "The colossus comes down from its crown, not its foot: lowest felled at " + first.getLast().getY() + ", top " + plan.top);
                }
            }
        }
        h.succeed();
    }
}
