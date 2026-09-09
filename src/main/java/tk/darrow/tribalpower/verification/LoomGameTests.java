package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.blockentity.RitualBrazierBlockEntity;
import tk.darrow.tribalpower.echo.LatticeRecipe;
import tk.darrow.tribalpower.echo.ProcessingRecipes;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.item.PulseCellItem;
import tk.darrow.tribalpower.item.SpiritStaffItem;
import tk.darrow.tribalpower.lattice.LatticeNetwork;

import java.util.EnumSet;

/** The sixth voice — Loom: distinct attunement, Unweave recipes and the Sixfold Staff. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class LoomGameTests {
    @GameTest(template = "empty")
    public static void loomTotemCountsAsDistinctVoice(GameTestHelper h) {
        h.setBlock(2, 2, 2, ModBlocks.RESONANCE_TOTEM_EARTH.get());
        h.setBlock(4, 2, 2, ModBlocks.RESONANCE_TOTEM_LOOM.get());
        h.setBlock(3, 2, 4, ModBlocks.PULSE_RESONATOR.get());
        var voices = LatticeNetwork.collectAttunements(h.getLevel(), h.absolutePos(new BlockPos(3, 2, 4)), 8);
        h.assertTrue(voices.equals(EnumSet.of(Attunement.EARTH, Attunement.LOOM)),
                "Loom totem must register as its own voice beside Earth, got " + voices);
        h.assertTrue(Attunement.byName("loom") == Attunement.LOOM, "Loom must serialise as \"loom\"");
        h.assertTrue(RitualBrazierBlockEntity.element(new ItemStack(ModItems.LOOM_SEAL.get())) == Attunement.LOOM,
                "Loom Seal must seat as the Loom voice in a brazier");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void unweaveReversesManifestedIngot(GameTestHelper h) {
        var formula = ProcessingRecipes.find(h.getLevel(), "echo_unweave", new ItemStack(ModItems.MANIFESTED_INGOT.get()));
        h.assertTrue(formula != null, "Echo Unweave must accept a Manifested Ingot");
        h.assertTrue(formula.result().is(ModItems.BOUND_ECHO.get()) && formula.result().getCount() == 2,
                "Unweaving a Manifested Ingot must yield 2 Bound Echo");
        h.assertTrue(formula.attunement() == Attunement.LOOM, "Unweave recipes require the Loom voice");
        var salvage = ProcessingRecipes.find(h.getLevel(), "echo_unweave", new ItemStack(ModItems.SPIRITGEAR_PICKAXE.get()));
        h.assertTrue(salvage != null && salvage.result().is(ModItems.MANIFESTED_INGOT.get()),
                "Spiritgear tools must salvage into a Manifested Ingot");
        long unweave = h.getLevel().getRecipeManager().getAllRecipesFor(LatticeRecipe.TYPE.get()).stream()
                .filter(r -> r.value().station().equals("echo_unweave")).count();
        h.assertTrue(unweave >= 6, "Expected the six Unweave formulas, found " + unweave);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void staffCyclesThroughAllSixVoices(GameTestHelper h) {
        var staff = new ItemStack(ModItems.SPIRIT_STAFF.get());
        var seen = EnumSet.noneOf(Attunement.class);
        for (int i = 0; i < Attunement.values().length; i++) {
            CustomData.update(DataComponents.CUSTOM_DATA, staff, tag -> tag.putInt("Attunement", tag.getInt("Attunement") + 1));
            seen.add(SpiritStaffItem.element(staff));
        }
        h.assertTrue(seen.size() == 6 && seen.contains(Attunement.LOOM), "Staff must cycle through all six voices, got " + seen);
        h.assertTrue(SpiritStaffItem.cost(Attunement.LOOM) == 6 && SpiritStaffItem.STITCH_COST == 10, "Tether costs 6 Pulse, Stitch 10");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tensionThreadsPulseIntoCarriedCells(GameTestHelper h) {
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var cell = PulseCellItem.createFilled(10);
        player.getInventory().setItem(0, cell);
        RitualBrazierBlockEntity.tension(player);
        h.assertTrue(PulseCellItem.getPulse(player.getInventory().getItem(0)) == 12, "Tension must add 2 Pulse to a carried cell");
        h.succeed();
    }
}
