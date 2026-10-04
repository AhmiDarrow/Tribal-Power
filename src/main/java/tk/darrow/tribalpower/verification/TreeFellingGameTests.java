package tk.darrow.tribalpower.verification;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.item.PulseCellItem;
import tk.darrow.tribalpower.item.SpiritGear;
import tk.darrow.tribalpower.item.TreeFelling;
import tk.darrow.tribalpower.world.MarchTreeFeature;

import java.util.UUID;

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
        player.getInventory().clearContent();
        player.setShiftKeyDown(false);
        BlockPos stand = h.absolutePos(BASE.offset(2, 0, 0));
        player.setPos(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
        ItemStack axe = new ItemStack(ModItems.SPIRITGEAR_AXE.get());
        SpiritGear.setVoice(axe, Attunement.EARTH);
        player.setItemInHand(InteractionHand.MAIN_HAND, axe);
        player.setItemInHand(InteractionHand.OFF_HAND, PulseCellItem.createFilled(200));
        return player;
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
        for (ItemEntity item : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(BASE)).inflate(6, 10, 6)))
            if (item.getItem().is(Items.OAK_LOG)) count += item.getItem().getCount();
        return count;
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
        // The struck log is a normal paid swing (no wear); each of the four felled logs costs one point of the edge.
        h.assertTrue(axe.getDamageValue() == HEIGHT - 1, "Felling spends a point per felled log, damage " + axe.getDamageValue());
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

    @GameTest(template = "empty")
    public static void aWornAxeStopsBeforeItBreaks(GameTestHelper h) {
        oak(h, true, false);
        FakePlayer player = feller(h, "worn");
        ItemStack axe = player.getMainHandItem();
        axe.setDamageValue(axe.getMaxDamage() - 3);
        player.gameMode.destroyBlock(h.absolutePos(BASE));
        h.assertTrue(player.getMainHandItem() == axe && !axe.isEmpty(), "The axe survives the felling");
        h.assertTrue(axe.getDamageValue() == axe.getMaxDamage() - 1, "It is left on its last point, damage " + axe.getDamageValue());
        h.assertTrue(h.getBlockState(BASE.above(4)).isAir() && h.getBlockState(BASE.above(3)).isAir(),
                "The two points it had went on the top two logs");
        h.assertTrue(h.getBlockState(BASE.above(1)).is(Blocks.OAK_LOG) && h.getBlockState(BASE.above(2)).is(Blocks.OAK_LOG),
                "The rest of the trunk stands once the edge is spent");
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
}
