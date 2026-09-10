package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.blockentity.DrumheartBlockEntity;
import tk.darrow.tribalpower.blockentity.PulseCairnBlockEntity;
import tk.darrow.tribalpower.generator.EmberHornBlockEntity;
import tk.darrow.tribalpower.generator.GeneratorRegistry;
import tk.darrow.tribalpower.generator.LoomAnchorBlockEntity;
import tk.darrow.tribalpower.generator.WakeBellBlockEntity;
import tk.darrow.tribalpower.generator.WaveDrumBlockEntity;
import tk.darrow.tribalpower.generator.WaveMath;
import tk.darrow.tribalpower.generator.WindHarpBlockEntity;
import tk.darrow.tribalpower.generator.WindMath;
import tk.darrow.tribalpower.pit.OreBand;

/** The six voices, their density rules, and the perpetual-motion check (design 3.1 section 9). */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class GeneratorGameTests {

    private static <T> T place(GameTestHelper h, BlockPos pos, net.minecraft.world.level.block.Block block, Class<T> type) {
        h.setBlock(pos, block);
        return type.cast(h.getLevel().getBlockEntity(h.absolutePos(pos)));
    }

    // ---- Fire ----------------------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void emberHornPaysBurnTimeOverTwentyAndIsCappedPerSecond(GameTestHelper h) {
        h.assertTrue(EmberHornBlockEntity.pulseOf(new ItemStack(Items.COAL)) == 80,
                "Coal is 80 Pulse, got " + EmberHornBlockEntity.pulseOf(new ItemStack(Items.COAL)));
        h.assertTrue(EmberHornBlockEntity.pulseOf(new ItemStack(Items.COAL_BLOCK)) == 800, "A coal block is 800");
        h.assertTrue(EmberHornBlockEntity.pulseOf(new ItemStack(Items.LAVA_BUCKET)) == 1000, "A lava bucket is 1000");
        h.assertTrue(EmberHornBlockEntity.pulseOf(new ItemStack(Items.BLAZE_ROD)) == 120, "A blaze rod is 120");
        h.assertTrue(EmberHornBlockEntity.pulseOf(new ItemStack(Items.STONE)) == 0, "Stone is not fuel");

        EmberHornBlockEntity horn = place(h, new BlockPos(3, 2, 3), GeneratorRegistry.EMBER_HORN.get(), EmberHornBlockEntity.class);
        horn.setItem(EmberHornBlockEntity.SLOT, new ItemStack(Items.COAL_BLOCK));
        h.assertTrue(horn.currentOutput() == EmberHornBlockEntity.MAX_RATE,
                "However rich the fuel, a horn gives at most " + EmberHornBlockEntity.MAX_RATE + " a second, got " + horn.currentOutput());
        h.assertTrue(horn.voice() == Attunement.FIRE, "The Ember Horn speaks Fire");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theFuelLoopIsNetNegativeOnEveryAutomatablePath(GameTestHelper h) {
        // The pit's common band spends this much to return one coal ore block...
        int cycle = OreBand.COMMON.seconds() * OreBand.COMMON.pulsePerSecond();
        // ...and shattering that block costs this much more.
        int shatter = tk.darrow.tribalpower.grit.GritRegistry.SHATTER_SECONDS
                * tk.darrow.tribalpower.grit.GritRegistry.SHATTER_PULSE;
        int spent = cycle + shatter;
        int returned = 2 * EmberHornBlockEntity.pulseOf(new ItemStack(Items.COAL));
        h.assertTrue(spent > returned,
                "Coal through the pit must be net negative: spent " + spent + ", returned " + returned);

        // Fortune only closes it to break-even, and cannot be automated -- there are no fake players here.
        int fortuneIII = (int) Math.round(2.2 * EmberHornBlockEntity.pulseOf(new ItemStack(Items.COAL)));
        h.assertTrue(spent > fortuneIII,
                "Even hand-mined Fortune III must not turn a profit: spent " + spent + ", best case " + fortuneIII);
        h.succeed();
    }

    // ---- Air -----------------------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void windHarpPaysTheDocumentedRates(GameTestHelper h) {
        // The documented rates are the contract, and Factors is what computes them. Asserting on the
        // record keeps this test off the test world's own sky, which is not the thing being checked.
        h.assertTrue(new WindMath.Factors(true, 0, false, false, 1).gain() == 2,
                "Two a second at y 80 under open sky");
        h.assertTrue(new WindMath.Factors(true, WindMath.HIGHER, true, true, 1).gain() == 6,
                "Six a second high in a storm");
        h.assertTrue(new WindMath.Factors(false, WindMath.HIGHER, true, true, 1).gain() == 0,
                "A harp under a roof is a decoration");
        h.assertTrue(new WindMath.Factors(true, WindMath.HIGHER, true, true, 3).gain() == 2,
                "Three harps divide one wind three ways");
        h.assertTrue(WindMath.MAX_GAIN == 6, "Six is the whole of the wind");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void windHarpsStealEachOthersWind(GameTestHelper h) {
        BlockPos first = new BlockPos(3, 2, 3);
        WindHarpBlockEntity harp = place(h, first, GeneratorRegistry.WIND_HARP.get(), WindHarpBlockEntity.class);
        h.assertTrue(harp.voice() == Attunement.AIR, "The Wind Harp speaks Air");
        h.assertTrue(WindMath.crowd(h.getLevel(), h.absolutePos(first)) == 1, "One harp is a crowd of one");

        place(h, new BlockPos(6, 2, 3), GeneratorRegistry.WIND_HARP.get(), WindHarpBlockEntity.class);
        h.assertTrue(WindMath.crowd(h.getLevel(), h.absolutePos(first)) == 2,
                "A harp within " + WindMath.CROWD_RANGE + " blocks must be counted");

        // Far enough away to be its own wind.
        place(h, new BlockPos(3, 2, 15), GeneratorRegistry.WIND_HARP.get(), WindHarpBlockEntity.class);
        h.assertTrue(WindMath.crowd(h.getLevel(), h.absolutePos(first)) == 2,
                "A harp beyond the range must not be counted");
        h.succeed();
    }

    // ---- Water ---------------------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void waveDrumPaysMoreWhenPipedAndOnlyTheFreeTierIsCrowded(GameTestHelper h) {
        BlockPos pos = new BlockPos(4, 2, 4);
        WaveDrumBlockEntity drum = place(h, pos, GeneratorRegistry.WAVE_DRUM.get(), WaveDrumBlockEntity.class);
        h.assertTrue(drum.voice() == Attunement.WATER, "The Wave Drum speaks Water");
        h.assertTrue(drum.currentOutput() == 0, "A dry drum makes nothing, got " + drum.currentOutput());

        h.setBlock(pos.east(), Blocks.WATER);
        h.assertTrue(WaveMath.adjacentWater(h.getLevel(), h.absolutePos(pos)), "Water beside the drum must be seen");
        int passive = drum.currentOutput();
        h.assertTrue(passive >= WaveMath.PASSIVE, "Beside water the drum makes at least " + WaveMath.PASSIVE);

        drum.tank.fill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(drum.piped(), "A filled tank means the piped tier");
        h.assertTrue(drum.currentOutput() > passive,
                "Piped must beat passive: passive " + passive + ", piped " + drum.currentOutput());
        h.succeed();
    }

    // ---- Spirit ---------------------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void wakeBellHoldsABurstAndLetsItOutSteadily(GameTestHelper h) {
        WakeBellBlockEntity bell = place(h, new BlockPos(4, 2, 4), GeneratorRegistry.WAKE_BELL.get(), WakeBellBlockEntity.class);
        h.assertTrue(bell.voice() == Attunement.SPIRIT, "The Wake Bell speaks Spirit");
        h.assertTrue(bell.currentOutput() == 0, "A quiet bell makes nothing");

        for (int i = 0; i < 200; i++) bell.mourn(WakeBellBlockEntity.HOSTILE);
        h.assertTrue(bell.reservoir() == WakeBellBlockEntity.RESERVOIR,
                "The reservoir must brim rather than grow, got " + bell.reservoir());
        h.assertTrue(bell.currentOutput() == WakeBellBlockEntity.MAX_RATE,
                "However big the farm, a bell tolls at most " + WakeBellBlockEntity.MAX_RATE + " a second, got " + bell.currentOutput());
        h.succeed();
    }

    // ---- Loom -----------------------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void loomAnchorPaysOnePerVoiceAndIsCappedAtSix(GameTestHelper h) {
        BlockPos pos = new BlockPos(5, 2, 5);
        LoomAnchorBlockEntity anchor = place(h, pos, GeneratorRegistry.LOOM_ANCHOR.get(), LoomAnchorBlockEntity.class);
        h.assertTrue(anchor.voice() == Attunement.LOOM, "The Loom Anchor speaks Loom");
        h.assertTrue(anchor.currentOutput() == 0, "With no voices in reach it makes nothing");

        var totems = new net.minecraft.world.level.block.Block[]{
                ModBlocks.RESONANCE_TOTEM_EARTH.get(), ModBlocks.RESONANCE_TOTEM_FIRE.get(),
                ModBlocks.RESONANCE_TOTEM_WATER.get(), ModBlocks.RESONANCE_TOTEM_AIR.get(),
                ModBlocks.RESONANCE_TOTEM_SPIRIT.get(), ModBlocks.RESONANCE_TOTEM_LOOM.get()};
        for (int i = 0; i < totems.length; i++) h.setBlock(new BlockPos(2 + i, 2, 2), totems[i]);
        h.assertTrue(anchor.voices(h.getLevel(), h.absolutePos(pos)) == LoomAnchorBlockEntity.MAX_VOICES,
                "All six voices in reach must count, got " + anchor.voices(h.getLevel(), h.absolutePos(pos)));
        h.assertTrue(anchor.currentOutput() == LoomAnchorBlockEntity.MAX_VOICES,
                "One a second per voice, got " + anchor.currentOutput());
        h.succeed();
    }

    // ---- Earth ------------------------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void drumheartPaysByTempoOnTheRedstonePathToo(GameTestHelper h) {
        h.assertTrue(DrumheartBlockEntity.beatValue(20) == DrumheartBlockEntity.ON_TEMPO,
                "A one-second clock is on tempo");
        h.assertTrue(DrumheartBlockEntity.beatValue(DrumheartBlockEntity.TEMPO_MIN) == DrumheartBlockEntity.ON_TEMPO,
                "The window is inclusive at the bottom");
        h.assertTrue(DrumheartBlockEntity.beatValue(DrumheartBlockEntity.TEMPO_MAX) == DrumheartBlockEntity.ON_TEMPO,
                "The window is inclusive at the top");
        h.assertTrue(DrumheartBlockEntity.beatValue(40) == DrumheartBlockEntity.OFF_TEMPO,
                "A slow clock pays the off-tempo rate");
        h.assertTrue(DrumheartBlockEntity.beatValue(8) == DrumheartBlockEntity.OFF_TEMPO,
                "A spam clock at the minimum spacing pays the off-tempo rate");
        h.assertTrue(DrumheartBlockEntity.beatValue(7) == 0,
                "Anything closer than the minimum spacing is not a beat at all");

        DrumheartBlockEntity drum = place(h, new BlockPos(3, 2, 3), ModBlocks.DRUMHEART.get(), DrumheartBlockEntity.class);
        h.assertTrue(drum.voice() == Attunement.EARTH, "The Drumheart rings Earth");
        h.succeed();
    }

    // ---- storage -----------------------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void cairnsStackToFiveAndThenStopBeingCairns(GameTestHelper h) {
        for (int i = 0; i < 7; i++) h.setBlock(new BlockPos(4, 1 + i, 4), ModBlocks.PULSE_CAIRN.get());
        for (int i = 0; i < PulseCairnBlockEntity.MAX_COLUMN; i++) {
            var cairn = (PulseCairnBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(4, 1 + i, 4)));
            h.assertTrue(cairn.counted(), "Stone " + (i + 1) + " of the pile must count");
            h.assertTrue(cairn.getPulseCapacity() == PulseCairnBlockEntity.CAPACITY,
                    "Each counted cairn holds " + PulseCairnBlockEntity.CAPACITY);
        }
        for (int i = PulseCairnBlockEntity.MAX_COLUMN; i < 7; i++) {
            var cairn = (PulseCairnBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(4, 1 + i, 4)));
            h.assertFalse(cairn.counted(), "Past the fifth stone a cairn is only a stone");
            h.assertTrue(cairn.getPulseCapacity() == 0 && cairn.insertPulse(100, false) == 0,
                    "An uncounted cairn holds nothing at all");
        }

        var bottom = (PulseCairnBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(4, 1, 4)));
        h.assertTrue(bottom.signal() == 0, "An empty cairn reads 0");
        bottom.insertPulse(PulseCairnBlockEntity.CAPACITY, false);
        h.assertTrue(bottom.signal() == 15, "A full cairn reads 15, got " + bottom.signal());
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void everyGeneratorIsAPulseHandlerTheLatticeCanFind(GameTestHelper h) {
        var blocks = new net.minecraft.world.level.block.Block[]{
                GeneratorRegistry.EMBER_HORN.get(), GeneratorRegistry.WIND_HARP.get(),
                GeneratorRegistry.WAVE_DRUM.get(), GeneratorRegistry.WAKE_BELL.get(),
                GeneratorRegistry.LOOM_ANCHOR.get()};
        var voices = new java.util.HashSet<Attunement>();
        for (int i = 0; i < blocks.length; i++) {
            BlockPos pos = new BlockPos(2 + i * 2, 2, 2);
            h.setBlock(pos, blocks[i]);
            var be = h.getLevel().getBlockEntity(h.absolutePos(pos));
            h.assertTrue(be instanceof tk.darrow.tribalpower.api.pulse.PulseGenerator,
                    blocks[i] + " must be a PulseGenerator so gauges and cairns need no special case");
            var generator = (tk.darrow.tribalpower.api.pulse.PulseGenerator) be;
            h.assertTrue(voices.add(generator.voice()), blocks[i] + " must not share a voice with another generator");
            h.assertTrue(!generator.breakdown().isEmpty(), blocks[i] + " must explain itself");
        }
        h.assertTrue(voices.size() == 5, "Five blocks, five voices; Earth is the Drumheart");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void heldSignalStillsEveryGenerator(GameTestHelper h) {
        var blocks = new net.minecraft.world.level.block.Block[]{
                GeneratorRegistry.EMBER_HORN.get(), GeneratorRegistry.WIND_HARP.get(),
                GeneratorRegistry.WAVE_DRUM.get(), GeneratorRegistry.WAKE_BELL.get(),
                GeneratorRegistry.LOOM_ANCHOR.get()};
        for (int i = 0; i < blocks.length; i++) {
            BlockPos pos = new BlockPos(2 + i * 3, 3, 2);
            h.setBlock(pos, blocks[i]);
            var be = (tk.darrow.tribalpower.generator.GeneratorBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
            if (be instanceof EmberHornBlockEntity horn) horn.setItem(EmberHornBlockEntity.SLOT, new ItemStack(Items.COAL, 8));
            if (be instanceof WakeBellBlockEntity bell) bell.mourn(WakeBellBlockEntity.RESERVOIR);
            h.setBlock(pos.below(), Blocks.REDSTONE_BLOCK);
            h.assertTrue(be.stilled(), blocks[i] + " must notice a held signal");
            h.assertTrue(be.currentOutput() == 0, blocks[i] + " must make nothing while stilled");
        }
        h.succeed();
    }
}
