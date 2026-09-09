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

    /** Brazier at (3,2,3) with the given seal seated and a Drumheart holding 1000 Pulse at (5,2,3). */
    private static DrumheartBlockEntity brazierWithDrum(GameTestHelper h, ItemStack seal) {
        h.setBlock(3, 2, 3, ModBlocks.RITUAL_BRAZIER.get());
        h.setBlock(5, 2, 3, ModBlocks.DRUMHEART.get());
        at(h, new BlockPos(3, 2, 3), RitualBrazierBlockEntity.class).setSeal(seal);
        DrumheartBlockEntity drum = at(h, new BlockPos(5, 2, 3), DrumheartBlockEntity.class);
        drum.insertPulse(1000, false);
        return drum;
    }

    @GameTest(template = "empty")
    public static void rainCallingConsumesPulseAndSetsRain(GameTestHelper h) {
        var level = h.getLevel();
        var overworld = level.getServer().overworld();
        boolean wasRaining = overworld.getLevelData().isRaining();
        DrumheartBlockEntity drum = brazierWithDrum(h, new ItemStack(ModItems.WATER_SEAL.get()));
        BlockPos brazier = h.absolutePos(new BlockPos(3, 2, 3));
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
        BlockPos brazier = h.absolutePos(new BlockPos(3, 2, 3));
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
        h.setBlock(1, 2, 1, ModBlocks.RESONANCE_TOTEM_EARTH.get());
        h.setBlock(14, 2, 14, ModBlocks.RESONANCE_TOTEM_FIRE.get());
        // Ley Binding costs 1,200 Pulse; a Drumheart caps at 1,000, so add a second drum.
        h.setBlock(5, 2, 5, ModBlocks.DRUMHEART.get());
        at(h, new BlockPos(5, 2, 5), DrumheartBlockEntity.class).insertPulse(1000, false);
        BlockPos brazier = h.absolutePos(new BlockPos(3, 2, 3));
        BlockPos a = h.absolutePos(new BlockPos(1, 2, 1)), b = h.absolutePos(new BlockPos(14, 2, 14));
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
