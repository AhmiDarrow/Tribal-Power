package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.ley.LeyField;
import tk.darrow.tribalpower.ley.LeyMath;

/**
 * Planetary ley lines are a function of the seed, so a meeting of six can be found without walking the world,
 * and a collector's beat is the land's capped share plus what those lines are worth.
 */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class LeyFieldGameTests {
    @GameTest(template = "empty")
    public static void sixLinesMeetAndNeverMore(GameTestHelper h) {
        var level = h.getLevel();
        long salt = LeyField.salt(level);
        int cellX = 0, cellZ = 0;
        boolean found = false;
        for (int cx = -120; cx <= 120 && !found; cx++) {
            for (int cz = -120; cz <= 120; cz++) {
                if (LeyField.nexusRank(salt, cx, cz) != LeyField.MAX_LINES) continue;
                cellX = cx;
                cellZ = cz;
                found = true;
                break;
            }
        }
        h.assertTrue(found, "This dimension's salt must contain a six-line meeting");
        BlockPos heart = LeyField.nexusPos(level, cellX, cellZ);
        LeyField.Reading at = LeyField.sample(level, heart);
        h.assertTrue(at.lines() == LeyField.MAX_LINES, "The meeting itself must be six veins, saw " + at.lines());
        h.assertTrue(at.points() == LeyField.MAX_LINES * LeyField.PER_LINE,
                "Standing in the meeting must be worth every line, points " + at.points());
        LeyField.Reading again = LeyField.sample(level, heart);
        h.assertTrue(again.lines() == at.lines() && again.voices() == at.voices(), "The same place must read the same veins");
        // A step far from every nexus still cannot invent a seventh vein.
        LeyField.Reading far = LeyField.sample(level, heart.offset(0, 80, 0));
        h.assertTrue(far.lines() <= LeyField.MAX_LINES, "A reading must not count past six, saw " + far.lines());
        var beat = LeyMath.factors(level, heart);
        int land = Math.min(LeyMath.LAND_CAP, beat.environment());
        h.assertTrue(beat.lines() == LeyField.MAX_LINES, "The collector survey must see the six lines");
        h.assertTrue(beat.gain() == Math.min(LeyMath.MAX_GAIN, land + at.points()),
                "The beat is the capped land plus the lines, gain " + beat.gain() + " land " + land + " points " + at.points());
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void gogglesKeepTheHoodsRank(GameTestHelper h) {
        var level = h.getLevel();
        var hood = tk.darrow.tribalpower.item.SpiritGear.withRank(
                new net.minecraft.world.item.ItemStack(tk.darrow.tribalpower.item.ModItems.SPIRITWEAVE_HOOD.get()), 2);
        var lens = new net.minecraft.world.item.ItemStack(tk.darrow.tribalpower.ley.LeyRegistry.LEY_LENS.get());
        var input = net.minecraft.world.item.crafting.CraftingInput.of(1, 2, java.util.List.of(hood, lens));
        var found = level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level);
        h.assertTrue(found.isPresent(), "A Spiritweave Hood and a Ley Lens fit the goggles");
        var out = found.get().value().assemble(input, level.registryAccess());
        h.assertTrue(tk.darrow.tribalpower.item.SpiritGear.goggles(out), "The hood comes out wearing goggles");
        h.assertTrue(tk.darrow.tribalpower.item.SpiritGear.rank(out) == 2, "The hood keeps its rank");
        h.assertTrue(!tk.darrow.tribalpower.item.SpiritGear.goggles(hood), "The hood in the grid is not rewritten");
        var again = net.minecraft.world.item.crafting.CraftingInput.of(1, 2, java.util.List.of(out, lens.copy()));
        h.assertTrue(level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, again, level).isEmpty(),
                "Goggles are fitted once");
        var wearer = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        h.assertTrue(tk.darrow.tribalpower.item.SpiritGear.gogglesOpen(out), "Fresh goggles start open");
        h.assertTrue(!tk.darrow.tribalpower.item.SpiritGear.toggleGoggles(wearer, out), "Sneak-use turns the goggles off");
        h.assertTrue(!tk.darrow.tribalpower.item.SpiritGear.gogglesOpen(out), "Off goggles stay fitted");
        h.assertTrue(tk.darrow.tribalpower.item.SpiritGear.toggleGoggles(wearer, out), "Sneak-use again turns the goggles on");
        h.succeed();
    }
}
