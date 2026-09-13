package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.blockentity.DrumheartBlockEntity;
import tk.darrow.tribalpower.blockentity.ResonanceTotemBlockEntity;
import tk.darrow.tribalpower.lattice.Keeping;
import tk.darrow.tribalpower.ley.LeyMath;

@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class KeepingGameTests {
    @GameTest(template = "empty")
    public static void quietTotemPausesNewWorkButKeepsProgress(GameTestHelper h) {
        h.setBlock(5, 2, 4, ModBlocks.RESONANCE_TOTEM_EARTH.get());
        var totem = (ResonanceTotemBlockEntity) h.getBlockEntity(new BlockPos(5, 2, 4));
        BlockPos at = h.absolutePos(new BlockPos(4, 2, 4));
        totem.setAttention(0);
        h.assertTrue(Keeping.voice(h.getLevel(), at, tk.darrow.tribalpower.api.pulse.Attunement.EARTH) == Keeping.State.QUIET,
                "A starved Earth totem must read Quiet");
        h.assertTrue(Keeping.quiet(h.getLevel(), at, tk.darrow.tribalpower.api.pulse.Attunement.EARTH),
                "Quiet must be the stall reason when the totem is present");
        int stretched = Keeping.stretch(Keeping.State.DIM, 10);
        h.assertTrue(stretched > 10, "Dim must stretch work, got " + stretched);
        totem.feed();
        h.assertTrue(Keeping.voice(h.getLevel(), at, tk.darrow.tribalpower.api.pulse.Attunement.EARTH) == Keeping.State.ANSWERED,
                "Feed must answer the totem");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void handBeatWakesQuietAndRedstoneDoesNot(GameTestHelper h) {
        h.setBlock(4, 2, 4, ModBlocks.RESONANCE_TOTEM_EARTH.get());
        h.setBlock(4, 2, 5, ModBlocks.DRUMHEART.get());
        var totem = (ResonanceTotemBlockEntity) h.getBlockEntity(new BlockPos(4, 2, 4));
        var drum = (DrumheartBlockEntity) h.getBlockEntity(new BlockPos(4, 2, 5));
        totem.setAttention(0);
        h.assertTrue(totem.keeping() == Keeping.State.QUIET, "Start quiet");
        drum.onRedstonePulse();
        h.assertTrue(totem.keeping() == Keeping.State.QUIET, "A Heartbeat plate must not keep a totem");
        drum.drumBeat();
        h.assertTrue(totem.keeping() == Keeping.State.ANSWERED, "A hand strike must wake nearby totems");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void leySurveyHearsWaterEightAwayNotNine(GameTestHelper h) {
        BlockPos origin = new BlockPos(4, 2, 4);
        h.setBlock(origin, Blocks.STONE);
        var level = h.getLevel();
        h.runAfterDelay(8, () -> {
            int roofed = LeyMath.gain(level, h.absolutePos(origin));
            h.setBlock(4, 2, 12, Blocks.WATER);
            int eight = LeyMath.gain(level, h.absolutePos(origin));
            h.setBlock(4, 2, 12, Blocks.AIR);
            h.setBlock(4, 2, 13, Blocks.WATER);
            int nine = LeyMath.gain(level, h.absolutePos(origin));
            h.assertTrue(eight > roofed, "Water eight away must add, " + roofed + " -> " + eight);
            h.assertTrue(nine == roofed, "Water nine away must not add, roofed=" + roofed + " nine=" + nine);
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void glimpseCountsGrassUnderfootAndPadsWithWater(GameTestHelper h) {
        BlockPos origin = new BlockPos(4, 2, 4);
        h.setBlock(origin, Blocks.STONE);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                h.setBlock(4 + dx, 1, 4 + dz, Blocks.GRASS_BLOCK);
            }
        }
        var level = h.getLevel();
        BlockPos at = h.absolutePos(origin);
        var dry = LeyMath.glimpse(level, at);
        h.assertTrue(dry.greenery() > 0, "Grass under a totem must count as greenery, got " + dry.greenery());
        h.setBlock(3, 2, 4, Blocks.WATER);
        h.setBlock(5, 2, 4, Blocks.WATER);
        h.setBlock(4, 2, 3, Blocks.WATER);
        h.setBlock(4, 2, 5, Blocks.WATER);
        var lush = LeyMath.glimpse(level, at);
        h.assertTrue(lush.pad(), "Water beside grass underfoot must be a keeping pad, greenery="
                + lush.greenery() + " water=" + lush.water() + " gain=" + lush.gain());
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void kinshipTotemCountsAsAnsweredVoice(GameTestHelper h) {
        h.setBlock(4, 2, 4, tk.darrow.tribalpower.tribe.TribeRegistry.KINSHIP_TOTEM.get().defaultBlockState()
                .setValue(tk.darrow.tribalpower.tribe.KinshipTotemBlock.TRIBE,
                        tk.darrow.tribalpower.tribe.TribeDefinition.SOIL.ordinal()));
        BlockPos at = h.absolutePos(new BlockPos(4, 2, 5));
        h.assertTrue(Keeping.voice(h.getLevel(), at, tk.darrow.tribalpower.api.pulse.Attunement.EARTH)
                        == Keeping.State.ANSWERED,
                "A Kinship Totem of Earth's tribe must keep the Earth voice answered");
        h.succeed();
    }

}
