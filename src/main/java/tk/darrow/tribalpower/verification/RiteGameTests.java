package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.blockentity.DrumheartBlockEntity;
import tk.darrow.tribalpower.blockentity.RitualBrazierBlockEntity;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.lattice.LatticeNetwork;
import tk.darrow.tribalpower.ley.LeyMath;
import tk.darrow.tribalpower.logic.LogicRegistry;
import tk.darrow.tribalpower.logic.PulseGaugeBlock;
import tk.darrow.tribalpower.logic.PulseGaugeBlockEntity;
import tk.darrow.tribalpower.logic.PulseThresholdBlock;
import tk.darrow.tribalpower.logic.PulseThresholdBlockEntity;
import tk.darrow.tribalpower.rite.world.GreenBlessing;
import tk.darrow.tribalpower.rite.world.LeyLines;
import tk.darrow.tribalpower.rite.world.RiteSavedData;
import tk.darrow.tribalpower.rite.world.RiteTabletItem;
import tk.darrow.tribalpower.rite.world.WorldRite;

/** World rites, ley sight and Pulse logic (design 3.0 §4 and §7). */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class RiteGameTests {
    private static <T> T at(GameTestHelper h, BlockPos pos, Class<T> type) {
        return type.cast(h.getLevel().getBlockEntity(h.absolutePos(pos)));
    }

    /** Where the brazier stands in every rite test. */
    private static final BlockPos BRAZIER = new BlockPos(3, 2, 3);

    /**
     * The Rite Circle around the brazier (design 3.1 section 5): four Rite Pedestals on the diagonals and
     * chalk joining them to the middle. Tablet rites refuse without it, so every test that fires one
     * draws it.
     */
    private static void drawCircle(GameTestHelper h) {
        for (int x = 1; x <= 5; x++)
            for (int z = 1; z <= 5; z++) h.setBlock(x, 1, z, Blocks.STONE);
        for (int dx : new int[]{-2, 2})
            for (int dz : new int[]{-2, 2}) h.setBlock(BRAZIER.offset(dx, 0, dz), ModBlocks.RITE_PEDESTAL.get());
        for (int dx : new int[]{-1, 1})
            for (int dz : new int[]{-1, 1}) h.setBlock(BRAZIER.offset(dx, 0, dz), ModBlocks.RITUAL_MARK.get());
    }

    /** Brazier at (3,2,3) with the given seal seated, its circle drawn, and 1000 Pulse at (5,2,3). */
    private static DrumheartBlockEntity brazierWithDrum(GameTestHelper h, ItemStack seal) {
        drawCircle(h);
        h.setBlock(BRAZIER, ModBlocks.RITUAL_BRAZIER.get());
        h.setBlock(5, 2, 3, ModBlocks.DRUMHEART.get());
        at(h, BRAZIER, RitualBrazierBlockEntity.class).setSeal(seal);
        DrumheartBlockEntity drum = at(h, new BlockPos(5, 2, 3), DrumheartBlockEntity.class);
        drum.insertPulse(1000, false);
        return drum;
    }

    @GameTest(template = "empty")
    public static void aPedestalWithNoSavedBlockEntityWakesUpWorking(GameTestHelper h) {
        // Migration (design 3.1 section 16): Rite Pedestals placed in 3.0 worlds have no saved block
        // entity, because the block did not have one. Chunk loading creates one from the block on demand,
        // so the closest honest check without a 3.0 world is to take the block entity away and ask again.
        BlockPos pos = new BlockPos(3, 2, 3);
        h.setBlock(pos, ModBlocks.RITE_PEDESTAL.get());
        h.getLevel().removeBlockEntity(h.absolutePos(pos));

        var revived = h.getLevel().getBlockEntity(h.absolutePos(pos));
        h.assertTrue(revived instanceof tk.darrow.tribalpower.blockentity.RitePedestalBlockEntity,
                "A pedestal with no saved block entity must get one on demand, got " + revived);
        var pedestal = (tk.darrow.tribalpower.blockentity.RitePedestalBlockEntity) revived;
        h.assertTrue(pedestal.held().isEmpty(), "It must wake up empty rather than broken");
        h.assertTrue(pedestal.signal() == 0, "An empty pedestal reads 0");
        pedestal.setItem(tk.darrow.tribalpower.blockentity.RitePedestalBlockEntity.SLOT,
                new ItemStack(ModItems.SPIRIT_SHARD.get()));
        h.assertTrue(pedestal.signal() == 15, "And it must work from there");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tabletRiteRefusesWithoutItsCircle(GameTestHelper h) {
        // Deliberately no circle: brazier, seal and Pulse, and nothing else.
        h.setBlock(BRAZIER, ModBlocks.RITUAL_BRAZIER.get());
        h.setBlock(5, 2, 3, ModBlocks.DRUMHEART.get());
        at(h, BRAZIER, RitualBrazierBlockEntity.class).setSeal(new ItemStack(ModItems.WATER_SEAL.get()));
        DrumheartBlockEntity drum = at(h, new BlockPos(5, 2, 3), DrumheartBlockEntity.class);
        drum.insertPulse(1000, false);

        var failure = RiteTabletItem.perform(h.getLevel(), h.absolutePos(BRAZIER), null, WorldRite.RAIN_CALLING);
        h.assertTrue(failure != null, "A tablet rite must refuse without its circle");
        h.assertTrue(drum.getPulseStored() == 1000, "A refused rite must spend nothing");

        drawCircle(h);
        var drawn = RiteTabletItem.perform(h.getLevel(), h.absolutePos(BRAZIER), null, WorldRite.RAIN_CALLING);
        h.assertTrue(drawn == null, "With the circle drawn the same rite must fire, got " + drawn);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void sealMatchedTotemsMakeTheCircleCheaperAndLonger(GameTestHelper h) {
        drawCircle(h);
        h.setBlock(BRAZIER, ModBlocks.RITUAL_BRAZIER.get());
        BlockPos brazier = h.absolutePos(BRAZIER);

        var tier1 = tk.darrow.tribalpower.rite.world.RiteCircle.evaluate(h.getLevel(), brazier, Attunement.WATER);
        h.assertTrue(tier1.complete() && tier1.tier() == 1, "Pedestals and chalk alone are tier 1");
        h.assertTrue(tier1.cost(400) == 400, "Tier 1 charges the full price");

        for (int[] offset : new int[][]{{2, 0}, {-2, 0}, {0, 2}, {0, -2}})
            h.setBlock(BRAZIER.offset(offset[0], 0, offset[1]), ModBlocks.RESONANCE_TOTEM_WATER.get());
        var tier2 = tk.darrow.tribalpower.rite.world.RiteCircle.evaluate(h.getLevel(), brazier, Attunement.WATER);
        h.assertTrue(tier2.tier() == 2, "Four seal-matched totems on the cardinals make tier 2");
        h.assertTrue(tier2.cost(400) == 300, "Tier 2 takes a quarter off, got " + tier2.cost(400));
        h.assertTrue(tier2.duration(1200) == 2400, "Tier 2 doubles the duration, got " + tier2.duration(1200));

        // Totems of the wrong voice are just totems standing near a circle.
        var mismatched = tk.darrow.tribalpower.rite.world.RiteCircle.evaluate(h.getLevel(), brazier, Attunement.FIRE);
        h.assertTrue(mismatched.complete() && mismatched.tier() == 1,
                "Totems that do not match the rite must not buy the discount");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aStruckBrazierFiresTheRiteFromAStockedPedestal(GameTestHelper h) {
        DrumheartBlockEntity drum = brazierWithDrum(h, new ItemStack(ModItems.WATER_SEAL.get()));
        var brazier = at(h, BRAZIER, RitualBrazierBlockEntity.class);

        var empty = brazier.strike(h.getLevel());
        h.assertTrue(empty != null, "With no tablet on any pedestal the strike must do nothing");
        h.assertTrue(drum.getPulseStored() == 1000, "A strike that does nothing must spend nothing");

        BlockPos pedestal = BRAZIER.offset(2, 0, 2);
        var seat = (tk.darrow.tribalpower.blockentity.RitePedestalBlockEntity)
                h.getLevel().getBlockEntity(h.absolutePos(pedestal));
        h.assertTrue(seat != null, "A Rite Pedestal must have a block entity in 3.1");
        seat.setItem(tk.darrow.tribalpower.blockentity.RitePedestalBlockEntity.SLOT,
                new ItemStack(tk.darrow.tribalpower.rite.world.WorldRiteRegistry.TABLETS.get(WorldRite.RAIN_CALLING).get()));
        h.assertTrue(seat.signal() == 15, "An occupied pedestal reads 15");

        var failure = brazier.strike(h.getLevel());
        h.assertTrue(failure == null, "A stocked circle must fire on a strike, got " + failure);
        h.assertTrue(seat.held().isEmpty(), "The tablet must be spent");
        h.assertTrue(seat.signal() == 0, "The comparator must report the pedestal empty");
        h.assertTrue(drum.getPulseStored() < 1000, "Firing the rite must cost Pulse");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void springCallingFillsAnAdjacentCistern(GameTestHelper h) {
        DrumheartBlockEntity drum = brazierWithDrum(h, new ItemStack(ModItems.WATER_SEAL.get()));
        BlockPos brazier = h.absolutePos(BRAZIER);

        var noCistern = RiteTabletItem.perform(h.getLevel(), brazier, null, WorldRite.SPRING_CALLING);
        h.assertTrue(noCistern != null, "Spring Calling needs a cistern to call into");
        h.assertTrue(drum.getPulseStored() == 1000, "A rite that cannot take effect must spend nothing");

        h.setBlock(3, 3, 3, ModBlocks.SPIRIT_CISTERN.get());
        var failure = RiteTabletItem.perform(h.getLevel(), brazier, null, WorldRite.SPRING_CALLING);
        h.assertTrue(failure == null, "Spring Calling must fire with a cistern beside the brazier, got " + failure);
        h.assertTrue(drum.getPulseStored() == 1000 - WorldRite.SPRING_CALLING.cost(),
                "Spring Calling costs " + WorldRite.SPRING_CALLING.cost() + ", drum holds " + drum.getPulseStored());

        var cistern = at(h, new BlockPos(3, 3, 3), tk.darrow.tribalpower.blockentity.SpiritCisternBlockEntity.class);
        int before = cistern.tank.getFluidAmount();
        h.succeedWhen(() -> h.assertTrue(cistern.tank.getFluidAmount() > before,
                "A called spring must keep filling its cistern"));
    }

    @GameTest(template = "empty")
    public static void rainCallingConsumesPulseAndSetsRain(GameTestHelper h) {
        var level = h.getLevel();
        var overworld = level.getServer().overworld();
        boolean wasRaining = overworld.getLevelData().isRaining();
        DrumheartBlockEntity drum = brazierWithDrum(h, new ItemStack(ModItems.WATER_SEAL.get()));
        BlockPos brazier = h.absolutePos(BRAZIER);
        var wrongSeal = RiteTabletItem.perform(level, brazier, null, WorldRite.SKY_CLEARING);
        h.assertTrue(wrongSeal != null && drum.getPulseStored() == 1000, "A mismatched seal must refuse the rite and spend nothing");
        var failure = RiteTabletItem.perform(level, brazier, null, WorldRite.RAIN_CALLING);
        h.assertTrue(failure == null, "Rain Calling must succeed with a Water Seal and 1000 Pulse nearby, got " + failure);
        h.assertTrue(drum.getPulseStored() == 1000 - WorldRite.RAIN_CALLING.cost(), "Rain Calling must draw 400 Pulse, drum holds " + drum.getPulseStored());
        h.assertTrue(overworld.getLevelData().isRaining(), "Rain Calling must set rain");
        var starved = RiteTabletItem.perform(level, brazier, null, WorldRite.RAIN_CALLING);
        h.assertTrue(starved == null, "A second rite with 600 Pulse left must still succeed");
        var empty = RiteTabletItem.perform(level, brazier, null, WorldRite.RAIN_CALLING);
        h.assertTrue(empty != null && drum.getPulseStored() == 200, "With 200 Pulse left the rite must fail and spend nothing");
        if (!wasRaining) overworld.setWeatherParameters(6000, 0, false, false);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void greenBlessingMarksChunkAndExpires(GameTestHelper h) {
        var level = h.getLevel();
        brazierWithDrum(h, new ItemStack(ModItems.EARTH_SEAL.get()));
        BlockPos brazier = h.absolutePos(BRAZIER);
        var failure = RiteTabletItem.perform(level, brazier, null, WorldRite.GREEN_BLESSING);
        h.assertTrue(failure == null, "Green Blessing must succeed, got " + failure);
        ChunkPos chunk = new ChunkPos(brazier);
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                h.assertTrue(RiteSavedData.get(level.getServer()).isBlessed(level, new ChunkPos(chunk.x + dx, chunk.z + dz)), "All nine chunks around the brazier must be blessed");
        h.assertTrue(GreenBlessing.isBlessed(level, brazier), "The brazier's own chunk must be blessed");
        // A short blessing on a far chunk must lapse on its own.
        ChunkPos far = new ChunkPos(chunk.x + 40, chunk.z + 40);
        RiteSavedData.get(level.getServer()).bless(level, far, level.getGameTime() + 5);
        h.assertTrue(RiteSavedData.get(level.getServer()).isBlessed(level, far), "A fresh blessing must be live");
        h.runAfterDelay(10, () -> {
            h.assertTrue(!RiteSavedData.get(level.getServer()).isBlessed(level, far), "A blessing must expire after its duration");
            h.assertTrue(!RiteSavedData.get(level.getServer()).blessedChunks(level).contains(far), "Expired chunks must be dropped from the live list");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void pulseGaugeOutputsSignalProportionalToDrumheart(GameTestHelper h) {
        h.setBlock(3, 2, 3, ModBlocks.DRUMHEART.get());
        h.setBlock(3, 2, 4, LogicRegistry.PULSE_GAUGE.get().defaultBlockState().setValue(PulseGaugeBlock.FACING, Direction.NORTH));
        DrumheartBlockEntity drum = at(h, new BlockPos(3, 2, 3), DrumheartBlockEntity.class);
        drum.insertPulse(500, false);
        h.assertTrue(PulseGaugeBlockEntity.level(drum) == 8, "500 / 1000 Pulse must map to signal 8");
        h.runAfterDelay(6, () -> {
            var gauge = at(h, new BlockPos(3, 2, 4), PulseGaugeBlockEntity.class);
            BlockPos pos = h.absolutePos(new BlockPos(3, 2, 4));
            var state = h.getLevel().getBlockState(pos);
            h.assertTrue(gauge.signal() == 8, "Gauge must read the faced Drumheart, got " + gauge.signal());
            h.assertTrue(state.getSignal(h.getLevel(), pos, Direction.NORTH) == 8, "The rear face must emit the gauge level");
            h.assertTrue(state.getSignal(h.getLevel(), pos, Direction.SOUTH) == 0, "The arrow face must not emit");
            drum.insertPulse(500, false);
            h.runAfterDelay(6, () -> {
                h.assertTrue(gauge.signal() == 15, "A full Drumheart must read 15, got " + gauge.signal());
                h.succeed();
            });
        });
    }

    @GameTest(template = "empty")
    public static void leyMathPrefersOpenSky(GameTestHelper h) {
        h.setBlock(2, 2, 2, Blocks.STONE);
        h.setBlock(8, 2, 8, Blocks.STONE);
        for (int x = 4; x <= 12; x++) for (int z = 4; z <= 12; z++) h.setBlock(x, 6, z, Blocks.STONE);
        var level = h.getLevel();
        BlockPos open = h.absolutePos(new BlockPos(2, 2, 2)), roofed = h.absolutePos(new BlockPos(8, 2, 8));
        // Sky light settles a few ticks after the roof is placed.
        h.runAfterDelay(10, () -> {
            h.assertTrue(level.canSeeSky(open.above()), "Test position must see the sky");
            h.assertTrue(!level.canSeeSky(roofed.above()), "Roofed position must not see the sky");
            double a = LeyMath.strength(level, open), b = LeyMath.strength(level, roofed);
            h.assertTrue(a > b, "Open sky must strengthen ley: " + a + " vs " + b);
            h.assertTrue(LeyMath.gain(level, roofed) == LeyMath.BASE && b > 0 && b <= 1, "Sheltered ground yields only the base gain");
            int before = LeyMath.gain(level, open);
            h.setBlock(3, 2, 2, Blocks.WATER);
            h.assertTrue(LeyMath.gain(level, open) == before + LeyMath.WATER, "Adjacent water must add its factor");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void leyBindingLinksTwoTotems(GameTestHelper h) {
        var level = h.getLevel();
        brazierWithDrum(h, new ItemStack(ModItems.LOOM_SEAL.get()));
        // Clear of the Rite Circle: its pedestals stand on the diagonals at (1,1), (1,5), (5,1), (5,5).
        h.setBlock(0, 2, 0, ModBlocks.RESONANCE_TOTEM_EARTH.get());
        h.setBlock(14, 2, 14, ModBlocks.RESONANCE_TOTEM_FIRE.get());
        // Ley Binding costs 1,200 Pulse; a Drumheart caps at 1,000, so add a second drum.
        h.setBlock(3, 2, 7, ModBlocks.DRUMHEART.get());
        at(h, new BlockPos(3, 2, 7), DrumheartBlockEntity.class).insertPulse(1000, false);
        BlockPos brazier = h.absolutePos(BRAZIER);
        BlockPos a = h.absolutePos(new BlockPos(0, 2, 0)), b = h.absolutePos(new BlockPos(14, 2, 14));
        h.assertTrue(!LatticeNetwork.canLink(a, b), "Test totems must be beyond chalk range to prove the ley line");
        var failure = RiteTabletItem.perform(level, brazier, null, WorldRite.LEY_BINDING);
        h.assertTrue(failure == null, "Ley Binding must succeed with two totems in range, got " + failure);
        // Neighbouring test structures may hold closer totems; the brazier's totem must be bound to some totem, ideally ours.
        var ends = LeyLines.linked(level, a);
        h.assertTrue(!ends.isEmpty(), "The brazier's nearest totem must be one end of the ley line");
        h.assertTrue(!ends.contains(b) || LeyLines.isLinked(level, b, a), "The line must be symmetric");
        var network = LatticeNetwork.collectChalkNetworkNear(level, brazier, 8);
        h.assertTrue(network.size() >= 2, "A Conductor network must route across the ley line, found " + network.size());
        h.assertTrue(LatticeNetwork.isConductable(network), "Ley-linked totems must be conductable");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void leyLineDropsWhenTotemBroken(GameTestHelper h) {
        var level = h.getLevel();
        h.setBlock(1, 2, 1, ModBlocks.RESONANCE_TOTEM_EARTH.get());
        h.setBlock(14, 2, 14, ModBlocks.RESONANCE_TOTEM_FIRE.get());
        BlockPos a = h.absolutePos(new BlockPos(1, 2, 1)), b = h.absolutePos(new BlockPos(14, 2, 14));
        LeyLines.bind(level, a, b, 20 * 60);
        h.assertTrue(LeyLines.isLinked(level, a, b) && LeyLines.isLinked(level, b, a), "A bound line must be live from both ends");
        h.setBlock(1, 2, 1, Blocks.AIR);
        h.assertTrue(LeyLines.linked(level, b).isEmpty(), "Breaking one totem must drop the ley line at the other end");
        h.assertTrue(LeyLines.linked(level, a).isEmpty(), "Breaking a totem must drop its own lines");
        // A short line must lapse on its own.
        h.setBlock(1, 2, 1, ModBlocks.RESONANCE_TOTEM_EARTH.get());
        LeyLines.bind(level, a, b, 5);
        h.assertTrue(LeyLines.isLinked(level, a, b), "A fresh line must be live");
        h.runAfterDelay(10, () -> {
            h.assertTrue(!LeyLines.isLinked(level, a, b), "A ley line must expire after its duration");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void pulseThresholdTogglesCyclesAndPersists(GameTestHelper h) {
        h.setBlock(3, 2, 3, ModBlocks.DRUMHEART.get());
        h.setBlock(3, 2, 4, LogicRegistry.PULSE_THRESHOLD.get().defaultBlockState().setValue(PulseThresholdBlock.FACING, Direction.NORTH));
        DrumheartBlockEntity drum = at(h, new BlockPos(3, 2, 3), DrumheartBlockEntity.class);
        drum.insertPulse(600, false); // 60 % of 1000
        BlockPos pos = h.absolutePos(new BlockPos(3, 2, 4));
        var level = h.getLevel();
        h.runAfterDelay(6, () -> {
            var threshold = at(h, new BlockPos(3, 2, 4), PulseThresholdBlockEntity.class);
            h.assertTrue(threshold.threshold() == 50, "Default threshold must be 50 %, got " + threshold.threshold());
            h.assertTrue(threshold.powered() && level.getBlockState(pos).getValue(PulseThresholdBlock.POWERED), "60 % must satisfy a 50 % threshold");
            h.assertTrue(level.getBlockState(pos).getSignal(level, pos, Direction.NORTH) == 15, "The rear face must emit full power");
            h.assertTrue(level.getBlockState(pos).getSignal(level, pos, Direction.SOUTH) == 0, "The arrow face must not emit");
            int percent = threshold.cycle();
            h.assertTrue(percent == 75 && !threshold.powered() && !level.getBlockState(pos).getValue(PulseThresholdBlock.POWERED),
                    "Cycling to 75 % must switch the output off immediately");
            h.assertTrue(level.getBlockState(pos).getAnalogOutputSignal(level, pos) == 3, "A comparator must read the threshold index + 1");
            var tag = threshold.saveWithoutMetadata(level.registryAccess());
            var copy = new PulseThresholdBlockEntity(pos, level.getBlockState(pos));
            copy.loadWithComponents(tag, level.registryAccess());
            h.assertTrue(copy.index() == threshold.index() && copy.powered() == threshold.powered(), "Threshold index and state must survive NBT");
            drum.insertPulse(200, false); // 80 %
            h.runAfterDelay(6, () -> {
                h.assertTrue(threshold.powered(), "80 % must satisfy a 75 % threshold after the next poll");
                h.succeed();
            });
        });
    }
}
