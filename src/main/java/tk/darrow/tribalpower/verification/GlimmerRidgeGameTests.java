package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.block.QuartzGlass;
import tk.darrow.tribalpower.echo.ProcessingRecipes;

/** The Glimmer Ridge: its stones, its crystal, and the glass the crystal becomes. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class GlimmerRidgeGameTests {

    /** Every pane of glass must have a lit twin to turn into, and the map must not loop back. */
    @GameTest(template="empty")
    public static void everyGlassHasALitTwinAndTheChainEnds(GameTestHelper h) {
        h.assertTrue(QuartzGlass.LIT_OF.size() == 34,
                "Clear and sixteen dyes, as blocks and panes, is 34 unlit ids; found " + QuartzGlass.LIT_OF.size());
        for (var entry : QuartzGlass.LIT_OF.entrySet()) {
            Item lit = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("tribalpower", entry.getValue()));
            h.assertTrue(lit != null && lit != Items.AIR, entry.getKey() + " has no registered lit twin");
            h.assertTrue(!QuartzGlass.LIT_OF.containsKey(entry.getValue()),
                    entry.getValue() + " is itself listed as unlit, which would let it be lit again");
        }
        h.succeed();
    }

    /** Lighting glass is a station job with a glowstone catalyst, not a crafting recipe. */
    @GameTest(template="empty")
    public static void lightingGlassAsksForGlowstoneInTheCatalystSlot(GameTestHelper h) {
        ItemStack glass = new ItemStack(QuartzGlass.ITEMS.get("quartz_glass").get());
        ProcessingRecipes.Formula f = QuartzGlass.litFormula("echo_attune", glass);
        h.assertTrue(f != null, "Echo Attune must have a formula for quartz glass");
        h.assertTrue(f.catalysts().size() == 1 && f.catalysts().get(0).is(Items.GLOWSTONE_DUST),
                "The formula must ask for glowstone dust, asked for " + f.catalysts());
        h.assertTrue(f.recipe().output().is(QuartzGlass.ITEMS.get("lit_quartz_glass").get()),
                "Clear glass must light into lit clear glass, gave " + f.recipe().output());
        h.assertTrue(QuartzGlass.litFormula("echo_bind", glass) == null,
                "Only Echo Attune binds light, and only there");
        h.assertTrue(QuartzGlass.litFormula("echo_attune",
                new ItemStack(QuartzGlass.ITEMS.get("lit_quartz_glass").get())) == null,
                "Already-lit glass must not light again");
        h.assertTrue(QuartzGlass.litFormula("echo_attune", new ItemStack(Items.STONE)) == null,
                "Anything that is not quartz glass must fall through to the other formulae");
        h.succeed();
    }

    /** A dyed pane keeps its colour through the lighting, rather than reverting to clear. */
    @GameTest(template="empty")
    public static void lightingKeepsTheDye(GameTestHelper h) {
        for (String id : new String[]{"red_quartz_glass", "blue_quartz_glass_pane", "black_quartz_glass"}) {
            ProcessingRecipes.Formula f = QuartzGlass.litFormula("echo_attune",
                    new ItemStack(QuartzGlass.ITEMS.get(id).get()));
            h.assertTrue(f != null, id + " must have a lighting formula");
            h.assertTrue(f.recipe().output().is(QuartzGlass.ITEMS.get("lit_" + id).get()),
                    id + " must light into lit_" + id + ", gave " + f.recipe().output());
        }
        h.succeed();
    }

    /** Quartz grows on a floor or a ceiling and goes when what it grew on goes. */
    @GameTest(template="empty")
    public static void quartzNeedsSomethingToGrowOn(GameTestHelper h) {
        BlockPos floor = new BlockPos(2, 1, 2), crystal = new BlockPos(2, 2, 2);
        h.setBlock(floor, Blocks.STONE);
        h.setBlock(crystal, ModBlocks.MARCH_QUARTZ.get().defaultBlockState());
        h.assertBlockPresent(ModBlocks.MARCH_QUARTZ.get(), crystal);
        h.assertTrue(h.getBlockState(crystal).getValue(
                tk.darrow.tribalpower.block.MarchQuartzBlock.FACING) == Direction.UP,
                "Quartz placed on a floor stands up");
        h.setBlock(floor, Blocks.AIR);
        h.succeedWhen(() -> h.assertBlockNotPresent(ModBlocks.MARCH_QUARTZ.get(), crystal));
    }

    /** The ridge has to be a real biome the March can actually roll. */
    @GameTest(template="empty")
    public static void theGlimmerRidgeIsARegisteredBiome(GameTestHelper h) {
        var key = ResourceKey.create(Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath("tribalpower", "march_glimmer_ridge"));
        h.assertTrue(h.getLevel().registryAccess().registryOrThrow(Registries.BIOME).containsKey(key),
                "march_glimmer_ridge must be loaded");
        for (String feature : new String[]{"moonstone_body", "moss_agate_seam", "march_quartz_scatter",
                "march_quartz_cave", "march_quartz_cave_roof"}) {
            var fk = ResourceKey.create(Registries.PLACED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath("tribalpower", feature));
            h.assertTrue(h.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE).containsKey(fk),
                    feature + " must be a loaded placed feature");
        }
        h.succeed();
    }

    /** The deep structures are placed on an absolute height band, not hung off the surface. */
    @GameTest(template="empty")
    public static void theDeepStructuresAreDeepAndComplete(GameTestHelper h) {
        var structures = h.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        var loot = h.getLevel().getServer().reloadableRegistries().getKeys(Registries.LOOT_TABLE);
        for (String id : new String[]{"deep_temple", "delving_gallery", "glimmer_vault", "ossuary"}) {
            var key = ResourceKey.create(Registries.STRUCTURE,
                    ResourceLocation.fromNamespaceAndPath("tribalpower", id));
            h.assertTrue(structures.containsKey(key), id + " must be a registered structure");
            var structure = structures.get(key);
            h.assertTrue(structure != null && structure.step()
                    == net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_STRUCTURES,
                    id + " must generate in the underground step, not on the surface");
            h.assertTrue(loot.contains(ResourceLocation.fromNamespaceAndPath("tribalpower", "chests/" + id)),
                    id + " must have its chest loot table");
        }
        h.succeed();
    }

    /** The ridge stones came with a full building set, and none of it may be half-registered. */
    @GameTest(template="empty")
    public static void bothRidgeStonesHaveTheirWholeBuildingSet(GameTestHelper h) {
        for (String stone : new String[]{"moonstone", "moss_agate"}) {
            for (String part : new String[]{"", "_stairs", "_slab", "_wall",
                    "_bricks", "_brick_stairs", "_brick_slab", "_brick_wall"}) {
                String id = stone + part;
                h.assertTrue(BuiltInRegistries.BLOCK.containsKey(
                        ResourceLocation.fromNamespaceAndPath("tribalpower", id)), id + " is missing");
            }
            for (String part : new String[]{"", "_stairs", "_slab"}) {
                String id = "polished_" + stone + part;
                h.assertTrue(BuiltInRegistries.BLOCK.containsKey(
                        ResourceLocation.fromNamespaceAndPath("tribalpower", id)), id + " is missing");
            }
        }
        h.succeed();
    }
}
