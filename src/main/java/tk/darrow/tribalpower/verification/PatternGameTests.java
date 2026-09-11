package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.blockentity.StoneFontBlockEntity;
import tk.darrow.tribalpower.pattern.ModPatterns;
import tk.darrow.tribalpower.pattern.PatternMatcher;
import tk.darrow.tribalpower.pattern.RitualPattern;
import tk.darrow.tribalpower.item.ModItems;

/** The pattern framework and the Stone Font (design 3.1 sections 4 and 6). */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class PatternGameTests {

    /** Stone under everything the marks stand on: a chalk mark needs a sturdy face beneath it. */
    private static void floor(GameTestHelper h, int x0, int z0, int x1, int z1) {
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++) h.setBlock(x, 1, z, Blocks.STONE);
    }

    /** The tier 1 Stone Font shape around {@code centre}: the font and four chalk marks. */
    private static void fontTier1(GameTestHelper h, BlockPos centre) {
        floor(h, centre.getX() - 2, centre.getZ() - 2, centre.getX() + 2, centre.getZ() + 2);
        h.setBlock(centre, ModBlocks.STONE_FONT.get());
        for (BlockPos mark : new BlockPos[]{centre.north(), centre.south(), centre.east(), centre.west()})
            h.setBlock(mark, ModBlocks.RITUAL_MARK.get());
    }

    @GameTest(template = "empty")
    public static void patternMatchesInEveryRotationAndReportsWhatIsMissing(GameTestHelper h) {
        BlockPos centre = new BlockPos(4, 2, 4);
        fontTier1(h, centre);
        BlockPos anchor = h.absolutePos(centre);

        PatternMatcher.Match match = PatternMatcher.match(h.getLevel(), anchor, ModPatterns.STONE_FONT);
        h.assertTrue(match.found(), "A font and four marks is a tier 1 Stone Font");
        h.assertTrue(match.tier() == 1, "Without anchor stones the shape is tier 1, got " + match.tier());

        // The tier 1 font is rotationally symmetric, so every rotation must match it identically.
        for (Rotation rotation : Rotation.values()) {
            RitualPattern.Tier tier = ModPatterns.STONE_FONT.tier(1);
            int misses = 0;
            for (RitualPattern.Cell cell : tier.cells()) {
                if (cell.predicate().trivial()) continue;
                if (!cell.predicate().test(h.getLevel(), cell.at(anchor, rotation))) misses++;
            }
            h.assertTrue(misses == 0, "A symmetric pattern must match in rotation " + rotation + ", missed " + misses);
        }

        // Break one mark: the match must fail and say which cell it wanted.
        h.setBlock(centre.north(), Blocks.AIR);
        PatternMatcher.Match broken = PatternMatcher.match(h.getLevel(), anchor, ModPatterns.STONE_FONT);
        h.assertFalse(broken.found(), "A missing chalk mark must break the pattern");
        h.assertTrue(!broken.misses().isEmpty(), "A failed match must report what is missing");
        h.assertTrue(broken.misses().size() <= 4, "The best partial match must be reported, not the worst; got "
                + broken.misses().size() + " misses");
        h.assertTrue(!broken.report(3).isEmpty(), "A failed match must produce readable lines");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void anchorStonesRaiseTheFontToTierTwo(GameTestHelper h) {
        BlockPos centre = new BlockPos(4, 2, 4);
        fontTier1(h, centre);
        for (int dx : new int[]{-2, 2})
            for (int dz : new int[]{-2, 2}) h.setBlock(centre.offset(dx, 0, dz), ModBlocks.ANCHOR_STONE.get());
        BlockPos anchor = h.absolutePos(centre);
        PatternMatcher.Match match = PatternMatcher.match(h.getLevel(), anchor, ModPatterns.STONE_FONT);
        h.assertTrue(match.found() && match.tier() == 2, "Four anchor stones make a tier 2 font, got tier " + match.tier());
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void fontAsksForCobbleThenStoneAsVoicesArrive(GameTestHelper h) {
        BlockPos centre = new BlockPos(4, 2, 4);
        fontTier1(h, centre);
        StoneFontBlockEntity font = (StoneFontBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(centre));
        h.assertTrue(font != null, "The font must have a block entity");

        h.assertTrue(font.best(h.getLevel(), h.absolutePos(centre)) == StoneFontBlockEntity.Ask.COBBLE,
                "A tier 1 font asks for cobblestone");

        // Tier 2 without Earth is still only cobble: shape alone does not buy the better stone.
        for (int dx : new int[]{-2, 2})
            for (int dz : new int[]{-2, 2}) h.setBlock(centre.offset(dx, 0, dz), ModBlocks.ANCHOR_STONE.get());
        font.patternState().invalidate();
        h.assertTrue(font.best(h.getLevel(), h.absolutePos(centre)) == StoneFontBlockEntity.Ask.COBBLE,
                "Tier 2 without the Earth voice still only asks for cobblestone");

        // Clear of the pattern: the corners belong to the anchor stones.
        h.setBlock(8, 2, 4, ModBlocks.RESONANCE_TOTEM_EARTH.get());
        font.patternState().invalidate();
        h.assertTrue(font.best(h.getLevel(), h.absolutePos(centre)) == StoneFontBlockEntity.Ask.STONE,
                "Tier 2 with Earth asks for stone");

        h.setBlock(8, 2, 6, ModBlocks.RESONANCE_TOTEM_FIRE.get());
        h.setBlock(8, 2, 2, ModBlocks.RESONANCE_TOTEM_WATER.get());
        font.patternState().invalidate();
        h.assertTrue(font.best(h.getLevel(), h.absolutePos(centre)) == StoneFontBlockEntity.Ask.OBSIDIAN,
                "Fire and Water together open the obsidian tier");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void heldSignalStillsTheFont(GameTestHelper h) {
        BlockPos centre = new BlockPos(4, 2, 4);
        fontTier1(h, centre);
        h.setBlock(6, 2, 4, ModBlocks.DRUMHEART.get());
        var drum = (tk.darrow.tribalpower.blockentity.DrumheartBlockEntity)
                h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(6, 2, 4)));
        drum.insertPulse(1000, false);
        StoneFontBlockEntity font = (StoneFontBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(centre));

        h.assertFalse(font.stilled(), "The font starts unstilled");
        h.setBlock(centre.above(), Blocks.REDSTONE_BLOCK);
        h.assertTrue(font.stilled(), "A held signal must still the font");
        h.assertTrue(font.progressSignal() >= 0, "A stilled font still answers a comparator");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void ritualChalkDrawsAndRubsOutMarks(GameTestHelper h) {
        // Every pattern that claims to be a rite is joined with chalk marks. Without a way to draw one,
        // the Stone Font, the Listening Pit and the Rite Circle cannot be built outside creative at all.
        h.setBlock(2, 1, 2, Blocks.STONE);
        net.minecraft.server.level.ServerPlayer player = h.makeMockServerPlayerInLevel();
        net.minecraft.world.item.ItemStack chalk =
                new net.minecraft.world.item.ItemStack(ModItems.RITUAL_CHALK.get(), 4);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, chalk);

        BlockPos floor = h.absolutePos(new BlockPos(2, 1, 2));
        net.minecraft.world.phys.BlockHitResult onFloor = new net.minecraft.world.phys.BlockHitResult(
                floor.getCenter().add(0, 0.5, 0), net.minecraft.core.Direction.UP, floor, false);
        chalk.useOn(new net.minecraft.world.item.context.UseOnContext(
                player, net.minecraft.world.InteractionHand.MAIN_HAND, onFloor));
        h.assertBlockPresent(ModBlocks.RITUAL_MARK.get(), 2, 2, 2);
        if (!player.getAbilities().instabuild)
            h.assertTrue(chalk.getCount() == 3, "Drawing a mark spends one chalk, held " + chalk.getCount());

        BlockPos mark = h.absolutePos(new BlockPos(2, 2, 2));
        net.minecraft.world.phys.BlockHitResult onMark = new net.minecraft.world.phys.BlockHitResult(
                mark.getCenter(), net.minecraft.core.Direction.UP, mark, false);
        chalk.useOn(new net.minecraft.world.item.context.UseOnContext(
                player, net.minecraft.world.InteractionHand.MAIN_HAND, onMark));
        h.assertBlockNotPresent(ModBlocks.RITUAL_MARK.get(), 2, 2, 2);
        h.succeed();
    }

}
