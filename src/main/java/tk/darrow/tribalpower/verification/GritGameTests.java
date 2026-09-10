package tk.darrow.tribalpower.verification;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.echo.ProcessingRecipes;
import tk.darrow.tribalpower.grit.GritRegistry;
import tk.darrow.tribalpower.grit.MineralGritItem;
import tk.darrow.tribalpower.item.ModItems;

/** Metals and gems are two different crafts (design 3.1 section 1). */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class GritGameTests {

    @GameTest(template = "empty")
    public static void metalsRouteToGritAndGemsRouteToThemselves(GameTestHelper h) {
        GritRegistry.rebuild();

        GritRegistry.Material iron = GritRegistry.material("iron");
        h.assertTrue(iron != null && iron.metal(), "Iron must be discovered as a metal");
        h.assertTrue(iron.ingot() == Items.IRON_INGOT, "Iron grit must know it fires into an iron ingot");

        GritRegistry.Material coal = GritRegistry.material("coal");
        h.assertTrue(coal != null && !coal.metal(), "Coal must be discovered as a gem, not a metal");
        h.assertTrue(coal.gem() == Items.COAL, "The coal band returns coal itself");

        // A metal shatters from its raw item; a gem never shatters from the gem.
        var rawIron = ProcessingRecipes.find(h.getLevel(), GritRegistry.STATION, new ItemStack(Items.RAW_IRON));
        h.assertTrue(rawIron != null, "Raw iron must shatter into grit");
        h.assertTrue(rawIron.result().is(ModItems.IRON_GRIT.get()), "Raw iron must give iron grit, got " + rawIron.result());

        var oreCoal = ProcessingRecipes.find(h.getLevel(), GritRegistry.STATION, new ItemStack(Items.COAL_ORE));
        h.assertTrue(oreCoal != null, "A silk-touched coal ore must shatter");
        h.assertTrue(oreCoal.result().is(Items.COAL), "Coal ore must give coal itself, got " + oreCoal.result());
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void gemInputIsNeverAcceptedBecauseThatWouldBeADupe(GameTestHelper h) {
        GritRegistry.rebuild();
        for (var gem : new net.minecraft.world.item.Item[]{Items.COAL, Items.DIAMOND, Items.EMERALD,
                Items.REDSTONE, Items.LAPIS_LAZULI, Items.QUARTZ}) {
            var formula = GritRegistry.formula(GritRegistry.STATION, new ItemStack(gem));
            h.assertTrue(formula == null, gem + " must never be a shattering input: gem to gem is a dupe");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void multiDropGemsReturnTwiceTheOresOwnDrop(GameTestHelper h) {
        GritRegistry.rebuild();
        // Twice the base drop, not a flat two: redstone and lapis drop in handfuls.
        record Expected(String material, int count) {}
        for (Expected expected : new Expected[]{new Expected("coal", 2), new Expected("diamond", 2),
                new Expected("emerald", 2), new Expected("quartz", 2),
                new Expected("redstone", 8), new Expected("lapis", 8)}) {
            GritRegistry.Material material = GritRegistry.material(expected.material());
            if (material == null) continue;
            h.assertTrue(material.gemCount() == expected.count(),
                    expected.material() + " must yield " + expected.count() + ", got " + material.gemCount());
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void ancientDebrisIsNeverAShatteringInput(GameTestHelper h) {
        GritRegistry.rebuild();
        h.assertTrue(GritRegistry.formula(GritRegistry.STATION, new ItemStack(Items.ANCIENT_DEBRIS)) == null,
                "Netherite must require the Nether: ancient debris is never a shattering input");
        h.assertTrue(GritRegistry.material("netherite") == null && GritRegistry.material("netherite_scrap") == null,
                "Netherite must not be a discovered material");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aSyntheticModdedMetalRidesTheComponentItem(GameTestHelper h) {
        // Nothing in this test needs the metal to exist in the world: the point is that one item and one
        // component carry any material a tag scan could ever find.
        ItemStack tin = MineralGritItem.of("tin");
        h.assertTrue(MineralGritItem.materialOf(tin).equals("tin"), "Mineral grit must remember its material");
        h.assertTrue(GritRegistry.materialOf(tin).equals("tin"), "The registry must read the component back");
        h.assertTrue(!tin.getHoverName().getString().isBlank(), "Mineral grit must name itself");
        h.assertTrue(MineralGritItem.tint("tin") != MineralGritItem.tint("lead"),
                "Two metals must not share a tint, or the one sprite would be unreadable");
        h.assertTrue(MineralGritItem.tint("tin") == MineralGritItem.tint("tin"), "A material's tint must be stable");

        // The canonical three keep their own items so 3.0 saves and 3.0 recipes still mean what they meant.
        h.assertTrue(GritRegistry.stackFor("iron").is(ModItems.IRON_GRIT.get()), "Iron keeps iron_grit");
        h.assertTrue(GritRegistry.stackFor("copper").is(ModItems.COPPER_GRIT.get()), "Copper keeps copper_grit");
        h.assertTrue(GritRegistry.stackFor("gold").is(ModItems.GOLD_GRIT.get()), "Gold keeps gold_grit");
        h.assertTrue(MineralGritItem.materialOf(GritRegistry.stackFor("iron")) == null,
                "iron_grit is not a component item");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void writtenRecipesOutrankTheScan(GameTestHelper h) {
        GritRegistry.rebuild();
        // data/tribalpower/recipe/lattice/iron_grit.json gives two grit per raw iron; the scan agrees, but
        // the written recipe is what must be found, so a pack can always override the synthesised one.
        var formula = ProcessingRecipes.findWritten(h.getLevel(), GritRegistry.STATION, new ItemStack(Items.RAW_IRON));
        h.assertTrue(formula != null, "The shipped iron grit recipe must still be found");
        h.assertTrue(ProcessingRecipes.find(h.getLevel(), GritRegistry.STATION, new ItemStack(Items.RAW_IRON))
                .id().equals(formula.id()), "A written recipe must outrank the scan");
        h.succeed();
    }
}
