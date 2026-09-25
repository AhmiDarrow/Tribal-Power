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
        h.assertTrue(DrumheartBlockEntity.beatValue(8) * 20 / 8 <= DrumheartBlockEntity.OFF_TEMPO,
                "A spam clock at the minimum spacing earns no more a second than the off-tempo rate");
        h.assertTrue(DrumheartBlockEntity.beatValue(7) == 0,
                "Anything closer than the minimum spacing is not a beat at all");

        DrumheartBlockEntity drum = place(h, new BlockPos(3, 2, 3), ModBlocks.DRUMHEART.get(), DrumheartBlockEntity.class);
        h.assertTrue(drum.voice() == Attunement.EARTH, "The Drumheart rings Earth");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aHeldSignalIsNotADrumheartBeat(GameTestHelper h) {
        h.setBlock(new BlockPos(3, 1, 3), Blocks.REDSTONE_BLOCK);
        DrumheartBlockEntity drum = place(h, new BlockPos(3, 2, 3), ModBlocks.DRUMHEART.get(), DrumheartBlockEntity.class);
        drum.seedSignal(h.getLevel());
        int stored = drum.getPulseStored();
        drum.onRedstoneChanged();
        h.assertTrue(drum.getPulseStored() == stored, "A held signal is not a beat");
        h.setBlock(new BlockPos(3, 1, 3), Blocks.AIR);
        drum.onRedstoneChanged();
        h.assertTrue(drum.getPulseStored() == stored, "A falling edge is not a beat");
        h.setBlock(new BlockPos(3, 1, 3), Blocks.REDSTONE_BLOCK);
        drum.onRedstoneChanged();
        h.assertTrue(drum.getPulseStored() > stored, "A rising edge is a beat");
        h.succeed();
    }

    // ---- storage -----------------------------------------------------------------------------------

    private static PulseCairnBlockEntity cairn(GameTestHelper h, BlockPos pos) {
        return (PulseCairnBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
    }

    @GameTest(template = "empty")
    public static void touchingCairnsAreOneStoreFromEitherStone(GameTestHelper h) {
        BlockPos a = new BlockPos(4, 1, 4), b = new BlockPos(5, 1, 4);
        h.setBlock(a, ModBlocks.PULSE_CAIRN.get());
        h.setBlock(b, ModBlocks.PULSE_CAIRN.get());
        var first = cairn(h, a);
        var second = cairn(h, b);
        h.assertTrue(first.getPulseCapacity() == 2 * PulseCairnBlockEntity.CAPACITY
                        && second.getPulseCapacity() == 2 * PulseCairnBlockEntity.CAPACITY,
                "Two touching cairns hold 8,000 from either stone, got " + first.getPulseCapacity() + " and " + second.getPulseCapacity());
        h.assertTrue(first.signal() == 0 && second.signal() == 0, "An empty pile reads 0");
        h.assertTrue(first.insertPulse(6000, true) == 6000 && first.getPulseStored() == 0,
                "A simulated insert must not fill the pile");
        h.assertTrue(first.insertPulse(6000, false) == 6000, "One stone must accept 6,000 for the pile");
        h.assertTrue(second.getPulseStored() == 6000, "The other stone must read the pile's 6,000, read " + second.getPulseStored());
        h.assertTrue(first.ownPulse() + second.ownPulse() == 6000 && first.ownPulse() <= PulseCairnBlockEntity.CAPACITY
                        && second.ownPulse() <= PulseCairnBlockEntity.CAPACITY,
                "The stones' own shares must add up to the pile, " + first.ownPulse() + " + " + second.ownPulse());
        h.assertTrue(second.extractPulse(5000, true) == 5000 && first.getPulseStored() == 6000,
                "A simulated draw must not move anything");
        h.assertTrue(second.extractPulse(5000, false) == 5000 && first.getPulseStored() == 1000,
                "A draw from either stone is paid by the pile, holds " + first.getPulseStored());
        first.insertPulse(100000, false);
        h.assertTrue(first.signal() == 15 && second.signal() == 15, "A full pile reads 15 from every stone");

        // The old column rule is gone: a sixth stone on a stack just joins the pile.
        for (int i = 0; i < 6; i++) h.setBlock(new BlockPos(2, 1 + i, 2), ModBlocks.PULSE_CAIRN.get());
        h.assertTrue(cairn(h, new BlockPos(2, 6, 2)).getPulseCapacity() == 6 * PulseCairnBlockEntity.CAPACITY,
                "Six stacked stones are one pile of 24,000, got " + cairn(h, new BlockPos(2, 6, 2)).getPulseCapacity());
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aLatticeDrawCountsAPileOnce(GameTestHelper h) {
        for (int x = 4; x <= 6; x++) h.setBlock(new BlockPos(x, 1, 4), ModBlocks.PULSE_CAIRN.get());
        var middle = cairn(h, new BlockPos(5, 1, 4));
        middle.insertPulse(3000, false);
        var level = h.getLevel();
        BlockPos origin = h.absolutePos(new BlockPos(2, 1, 2));
        int seen = tk.darrow.tribalpower.lattice.LatticeNetwork.extractPulseNearby(level, origin, 8, 100000, true);
        h.assertTrue(seen == 3000, "A simulated draw beside a pile of three must see its 3,000 once, saw " + seen);
        int room = tk.darrow.tribalpower.lattice.LatticeNetwork.insertPulseNearby(level, origin, 8, 100000, true);
        h.assertTrue(room == 9000, "A simulated push must see the pile's 9,000 of room once, saw " + room);
        int drawn = tk.darrow.tribalpower.lattice.LatticeNetwork.extractPulseNearby(level, origin, 8, 100000, false);
        h.assertTrue(drawn == 3000 && middle.getPulseStored() == 0,
                "The real draw takes exactly the pile's 3,000, drew " + drawn + ", left " + middle.getPulseStored());
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void breakingAStoneLeavesTheRestItsOwnShare(GameTestHelper h) {
        BlockPos a = new BlockPos(4, 1, 4), b = new BlockPos(4, 2, 4);
        h.setBlock(a, ModBlocks.PULSE_CAIRN.get());
        h.setBlock(b, ModBlocks.PULSE_CAIRN.get());
        var stays = cairn(h, a);
        var leaves = cairn(h, b);
        stays.insertPulse(6000, false);
        int kept = stays.ownPulse();
        int gone = leaves.ownPulse();
        h.assertTrue(kept + gone == 6000, "The shares must add up before the split");
        h.setBlock(b, Blocks.AIR);
        h.assertTrue(stays.getPulseCapacity() == PulseCairnBlockEntity.CAPACITY,
                "A lone stone holds 4,000 again, got " + stays.getPulseCapacity());
        h.assertTrue(stays.getPulseStored() == kept,
                "The stone that stays keeps only its own " + kept + ", holds " + stays.getPulseStored());
        h.assertTrue(stays.getPulseStored() + gone == 6000, "Nothing may be created or lost by the split");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aPileStopsAtSixtyFourStones(GameTestHelper h) {
        java.util.List<BlockPos> stones = new java.util.ArrayList<>();
        for (int x = 2; x < 6; x++) for (int y = 1; y < 5; y++) for (int z = 2; z < 6; z++) stones.add(new BlockPos(x, y, z));
        stones.add(new BlockPos(6, 1, 2));
        for (BlockPos pos : stones) h.setBlock(pos, ModBlocks.PULSE_CAIRN.get());
        int alone = 0, joined = 0;
        for (BlockPos pos : stones) {
            var stone = cairn(h, pos);
            if (stone.getPulseCapacity() == PulseCairnBlockEntity.CAPACITY && stone.pile().overflow()) alone++;
            else if (stone.getPulseCapacity() == PulseCairnBlockEntity.MAX_GROUP * PulseCairnBlockEntity.CAPACITY) joined++;
        }
        h.assertTrue(joined == 64 && alone == 1, "Sixty-four stones join and the 65th stands alone, joined "
                + joined + ", alone " + alone);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oneUseFillsACellAsFarAsTheSourceAllows(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        BlockPos a = new BlockPos(4, 1, 4), b = new BlockPos(5, 1, 4);
        h.setBlock(a, ModBlocks.PULSE_CAIRN.get());
        h.setBlock(b, ModBlocks.PULSE_CAIRN.get());
        var pile = cairn(h, a);
        pile.insertPulse(8000, false);
        ItemStack cell = new ItemStack(tk.darrow.tribalpower.item.ModItems.GREATER_PULSE_CELL.get());
        player.setItemInHand(hand, cell);
        h.getLevel().getBlockState(h.absolutePos(b)).useItemOn(cell, h.getLevel(), player, hand, hitOn(h, b));
        int full = tk.darrow.tribalpower.item.PulseCellItem.GREATER_CAPACITY;
        int got = tk.darrow.tribalpower.item.PulseCellItem.getPulse(cell);
        h.assertTrue(got == full, "One use on a pile fills a Greater cell to " + full + ", got " + got);
        h.assertTrue(pile.getPulseStored() == 8000 - full, "The pile loses exactly what the cell gained");

        BlockPos drumAt = new BlockPos(2, 1, 2);
        DrumheartBlockEntity drum = place(h, drumAt, ModBlocks.DRUMHEART.get(), DrumheartBlockEntity.class);
        drum.insertPulse(drum.getPulseCapacity(), false);
        int held = drum.getPulseStored();
        ItemStack small = new ItemStack(tk.darrow.tribalpower.item.ModItems.PULSE_CELL.get());
        player.setItemInHand(hand, small);
        h.getLevel().getBlockState(h.absolutePos(drumAt)).useItemOn(small, h.getLevel(), player, hand, hitOn(h, drumAt));
        int moved = tk.darrow.tribalpower.item.PulseCellItem.getPulse(small);
        h.assertTrue(held > 25 && moved == Math.min(held, tk.darrow.tribalpower.item.PulseCellItem.CAPACITY)
                        && drum.getPulseStored() == held - moved,
                "One use on a drum moves all it holds that the cell has room for, cell " + moved + ", drum " + drum.getPulseStored());
        h.succeed();
    }

    private static net.minecraft.world.phys.BlockHitResult hitOn(GameTestHelper h, BlockPos pos) {
        return new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(pos)),
                net.minecraft.core.Direction.UP, h.absolutePos(pos), false);
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
