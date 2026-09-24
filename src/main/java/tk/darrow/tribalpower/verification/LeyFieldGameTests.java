package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.ley.LeyField;
import tk.darrow.tribalpower.ley.LeyMagnets;
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

    /** A flight should meet the web within a short stretch, on the ground and well above it. */
    @GameTest(template = "empty")
    public static void theWebCrossesTheWorld(GameTestHelper h) {
        var level = h.getLevel();
        h.assertTrue(LeyField.REACH > LeyField.CELL, "A vein must run past the next meeting");
        h.assertTrue(LeyField.CELL <= 96, "Meetings must stand close enough to read as a web, cell " + LeyField.CELL);
        int quiet = 0;
        int checked = 0;
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int rise : new int[]{8, 96, 180}) {
                    BlockPos pos = new BlockPos(x * 64, level.getSeaLevel() + rise, z * 64);
                    checked++;
                    var ropes = LeyField.ropes(level, pos);
                    if (ropes.size() < 3) quiet++;
                }
            }
        }
        h.assertTrue(quiet == 0, "The web must show several veins from the ground and from flight, quiet " + quiet + " of " + checked);
        boolean[] voices = new boolean[6];
        for (var rope : LeyField.ropes(level, new BlockPos(0, level.getSeaLevel() + 48, 0))) {
            h.assertTrue(rope.voice() >= 0 && rope.voice() < voices.length, "A thread must carry a totem voice");
            voices[rope.voice()] = true;
        }
        int kinds = 0;
        for (boolean heard : voices) if (heard) kinds++;
        h.assertTrue(kinds >= 4, "The web near one place must carry several totem colours, saw " + kinds);
        h.succeed();
    }

    /** A totem swings the thread of its own colour through itself, and a different colour does not take hold. */
    @GameTest(template = "empty")
    public static void aTotemPullsItsOwnColourThroughItself(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos near = h.absolutePos(new BlockPos(2, 2, 2));
        var ropes = LeyField.ropes(level, near);
        h.assertFalse(ropes.isEmpty(), "A thread must pass near the structure");
        LeyField.Rope rope = ropes.getFirst();
        Attunement voice = Attunement.values()[rope.voice()];
        Vec3 on = LeyField.along(rope, rope.travel());
        BlockPos spot = BlockPos.containing(on);
        level.setBlockAndUpdate(spot, totem(voice).defaultBlockState());
        try {
            var magnets = LeyMagnets.near(level, spot);
            h.assertTrue(magnets.stream().anyMatch(m -> m.voice() == voice && m.pos().equals(spot)),
                    "The standing totem must be a magnet");
            var pulls = LeyMagnets.pulls(magnets, rope);
            Vec3 centre = Vec3.atCenterOf(spot);
            LeyMagnets.Pull ours = null;
            for (var pull : pulls) {
                if (centre.distanceToSqr(pull.x(), pull.y(), pull.z()) < 0.01) ours = pull;
            }
            h.assertTrue(ours != null, "The thread must be held by the totem standing on it");
            Vec3 through = LeyMagnets.apply(rope, ours.t(), pulls);
            h.assertTrue(through.distanceTo(centre) < 0.05, "The thread must run through the totem, off by " + through.distanceTo(centre));
            Attunement other = voice == Attunement.EARTH ? Attunement.FIRE : Attunement.EARTH;
            level.setBlockAndUpdate(spot, totem(other).defaultBlockState());
            // Only this totem's hold counts: neighbouring tests stand totems of their own within reach.
            h.assertTrue(LeyMagnets.pulls(LeyMagnets.near(level, spot), rope).stream()
                            .noneMatch(pull -> centre.distanceToSqr(pull.x(), pull.y(), pull.z()) < 0.01),
                    "A totem must not take hold of a thread of another colour");
            h.succeed();
        } finally {
            level.removeBlock(spot, false);
        }
    }

    private static net.minecraft.world.level.block.Block totem(Attunement voice) {
        return switch (voice) {
            case EARTH -> ModBlocks.RESONANCE_TOTEM_EARTH.get();
            case FIRE -> ModBlocks.RESONANCE_TOTEM_FIRE.get();
            case WATER -> ModBlocks.RESONANCE_TOTEM_WATER.get();
            case AIR -> ModBlocks.RESONANCE_TOTEM_AIR.get();
            case SPIRIT -> ModBlocks.RESONANCE_TOTEM_SPIRIT.get();
            case LOOM -> ModBlocks.RESONANCE_TOTEM_LOOM.get();
        };
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
