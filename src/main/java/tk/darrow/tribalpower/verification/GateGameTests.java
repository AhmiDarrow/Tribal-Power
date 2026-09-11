package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.gate.GateKeystoneBlockEntity;
import tk.darrow.tribalpower.gate.GatePortal;
import tk.darrow.tribalpower.gate.GateRegistry;
import tk.darrow.tribalpower.gate.GateSavedData;

import java.util.UUID;

/** The gates (design 3.1 section 8). */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class GateGameTests {

    /**
     * A Way Gate frame around a keystone at {@code keystone}, running along X: the 5x5 ring without its
     * corners, twelve cells, one of them the keystone.
     */
    private static GateKeystoneBlockEntity wayGate(GameTestHelper h, BlockPos keystone) {
        int x = keystone.getX(), y = keystone.getY(), z = keystone.getZ();
        for (int dx = -1; dx <= 1; dx++) h.setBlock(new BlockPos(x + dx, y, z), GateRegistry.GATE_FRAME.get());
        for (int dx = -1; dx <= 1; dx++) h.setBlock(new BlockPos(x + dx, y + 4, z), GateRegistry.GATE_FRAME.get());
        for (int dy = 1; dy <= 3; dy++) {
            h.setBlock(new BlockPos(x - 2, y + dy, z), GateRegistry.GATE_FRAME.get());
            h.setBlock(new BlockPos(x + 2, y + dy, z), GateRegistry.GATE_FRAME.get());
        }
        h.setBlock(keystone, GateRegistry.GATE_KEYSTONE.get());
        return (GateKeystoneBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(keystone));
    }

    @GameTest(template = "empty")
    public static void aFrameMakesAKeystoneAWayGateAndPulseLightsIt(GameTestHelper h) {
        BlockPos pos = new BlockPos(4, 2, 4);
        GateKeystoneBlockEntity keystone = wayGate(h, pos);
        h.assertTrue(keystone != null, "The keystone must have a block entity");
        h.assertTrue(keystone.kind(h.getLevel()) == GateKeystoneBlockEntity.Kind.WAY,
                "Twelve frame cells around a 3x3 interior is a Way Gate: " + keystone.match(h.getLevel()).report(3));

        // Lighting is paid for out of the keystone's own store, and refused when it cannot be.
        var starved = keystone.light(h.getLevel());
        h.assertTrue(starved != null, "An empty keystone must refuse to light");
        keystone.insertPulse(GateKeystoneBlockEntity.Kind.WAY.capacity(), false);
        var failure = keystone.light(h.getLevel());
        h.assertTrue(failure == null, "A charged keystone must light, got " + failure);
        h.assertTrue(keystone.lit(), "The keystone must report itself lit");
        h.assertTrue(keystone.getPulseStored()
                        == GateKeystoneBlockEntity.Kind.WAY.capacity() - GateKeystoneBlockEntity.Kind.WAY.lightCost(),
                "Lighting must spend exactly its cost, stored is " + keystone.getPulseStored());

        // The plane fills the interior and nothing else.
        int planes = 0;
        for (BlockPos cell : GatePortal.interior(h.getLevel(), keystone, GateKeystoneBlockEntity.Kind.WAY))
            if (h.getLevel().getBlockState(cell).is(GateRegistry.GATE_PORTAL.get())) planes++;
        h.assertTrue(planes == 9, "A Way Gate opens a 3x3 plane, got " + planes);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void heldSignalTakesThePlaneDownAndKeepsItDown(GameTestHelper h) {
        BlockPos pos = new BlockPos(4, 2, 4);
        GateKeystoneBlockEntity keystone = wayGate(h, pos);
        keystone.insertPulse(GateKeystoneBlockEntity.Kind.WAY.capacity(), false);
        keystone.light(h.getLevel());
        h.assertTrue(keystone.lit(), "The gate starts lit");

        // The lever goes on: a rising edge on a lit gate darkens it, and holding it changes nothing more.
        h.setBlock(pos.below(), Blocks.REDSTONE_BLOCK);
        h.assertFalse(keystone.lit(), "A struck signal on a lit gate must take the plane down");
        keystone.onRedstoneChanged(h.getLevel());
        h.assertFalse(keystone.lit(), "Holding the line high must not relight it");
        h.assertTrue(keystone.stilled(), "A held signal stills the keystone");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aStruckSignalCallsTheGateOnce(GameTestHelper h) {
        BlockPos pos = new BlockPos(4, 2, 4);
        GateKeystoneBlockEntity keystone = wayGate(h, pos);
        keystone.insertPulse(GateKeystoneBlockEntity.Kind.WAY.capacity(), false);

        // Placing the block is the strike: the block entity hears it through neighborChanged.
        h.setBlock(pos.below(), Blocks.REDSTONE_BLOCK);
        h.assertTrue(keystone.lit(), "A struck signal must light a dark gate");
        int afterLighting = keystone.getPulseStored();

        // A falling edge is not a call: the gate stays as it is, and nothing is charged for.
        h.setBlock(pos.below(), Blocks.AIR);
        h.assertTrue(keystone.lit(), "A falling edge must not change anything");
        h.assertTrue(keystone.getPulseStored() == afterLighting, "A falling edge must cost nothing");

        // The next strike is the other half of the toggle.
        h.setBlock(pos.below(), Blocks.REDSTONE_BLOCK);
        h.assertFalse(keystone.lit(), "A struck signal must darken a lit gate");
        h.assertTrue(keystone.getPulseStored() == afterLighting, "Darkening a gate costs nothing");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void landingInPowerIsNotAStrike(GameTestHelper h) {
        BlockPos pos = new BlockPos(4, 2, 4);
        h.setBlock(pos.below(), Blocks.REDSTONE_BLOCK);
        GateKeystoneBlockEntity keystone = wayGate(h, pos);
        keystone.insertPulse(GateKeystoneBlockEntity.Kind.WAY.capacity(), false);
        h.assertFalse(keystone.lit(), "A keystone placed into power must stay dark");
        keystone.onRedstoneChanged(h.getLevel());
        h.assertFalse(keystone.lit(), "The first neighbour update after landing in power is not a strike");
        h.assertTrue(keystone.getPulseStored() == GateKeystoneBlockEntity.Kind.WAY.capacity(),
                "Landing in power must not spend the lighting cost");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void linkingIsMutualAndBreakingOneEndReleasesTheOther(GameTestHelper h) {
        GateKeystoneBlockEntity a = wayGate(h, new BlockPos(3, 2, 3));
        GateKeystoneBlockEntity b = wayGate(h, new BlockPos(3, 2, 9));
        GateSavedData data = GateSavedData.get(h.getLevel().getServer());

        GateSavedData.Gate first = a.record(h.getLevel());
        GateSavedData.Gate second = b.record(h.getLevel());
        data.link(first.id(), second.id());
        h.assertTrue(second.id().equals(data.gate(first.id()).partner()), "Linking must be recorded on the first gate");
        h.assertTrue(first.id().equals(data.gate(second.id()).partner()), "Linking must be recorded on both ends");

        // Breaking a keystone forgets it and lets its partner go.
        tk.darrow.tribalpower.gate.GateLinking.onKeystoneBroken(h.getLevel(), a);
        h.assertTrue(data.gate(first.id()) == null, "A broken keystone must be forgotten");
        h.assertTrue(data.gate(second.id()).partner() == null, "Its partner must be released, not left dangling");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void comparatorReadsUnlinkedThenStoredThenTransit(GameTestHelper h) {
        BlockPos pos = new BlockPos(4, 2, 4);
        GateKeystoneBlockEntity keystone = wayGate(h, pos);
        h.assertTrue(keystone.signal(h.getLevel()) == 0, "An unlinked keystone reads 0");

        GateSavedData data = GateSavedData.get(h.getLevel().getServer());
        GateKeystoneBlockEntity other = wayGate(h, new BlockPos(4, 2, 10));
        data.link(keystone.record(h.getLevel()).id(), other.record(h.getLevel()).id());
        keystone.insertPulse(GateKeystoneBlockEntity.Kind.WAY.capacity(), false);
        int stored = keystone.signal(h.getLevel());
        h.assertTrue(stored >= 1 && stored <= 14, "A linked, charged keystone reads 1 to 14, got " + stored);

        keystone.markTransit();
        h.assertTrue(keystone.signal(h.getLevel()) == 15, "Mid-transit reads 15");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void malformedGateRecordDoesNotDisableTheRest(GameTestHelper h) {
        GateSavedData data = new GateSavedData();
        var gate = data.record(h.getLevel(), new BlockPos(1, 2, 3), "Kept", UUID.randomUUID());
        var saved = data.save(new net.minecraft.nbt.CompoundTag(), h.getLevel().registryAccess());
        var broken = new net.minecraft.nbt.CompoundTag();
        broken.putString("Id", "invalid");
        broken.putString("Recovery", "preserve this record");
        saved.getList("Gates", 10).add(0, broken.copy());

        var loaded = GateSavedData.load(saved, h.getLevel().registryAccess());
        h.assertTrue(loaded.gate(gate.id()) != null, "One unreadable keystone must not take the rest down with it");
        var roundtrip = loaded.save(new net.minecraft.nbt.CompoundTag(), h.getLevel().registryAccess());
        h.assertTrue(roundtrip.getList("Gates", 10).contains(broken),
                "Unreadable records must remain recoverable after saving");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void anIncompleteFrameSaysWhatIsMissing(GameTestHelper h) {
        BlockPos pos = new BlockPos(4, 2, 4);
        GateKeystoneBlockEntity keystone = wayGate(h, pos);
        h.setBlock(pos.offset(2, 2, 0), Blocks.AIR);
        keystone.onNeighbourChanged(pos.offset(2, 2, 0));
        h.assertTrue(keystone.kind(h.getLevel()) == null, "A holed frame is not a gate");
        var match = keystone.match(h.getLevel());
        h.assertFalse(match.found(), "The match must fail");
        h.assertTrue(!match.misses().isEmpty(), "It must say which cell it wanted");
        h.assertTrue(!match.report(3).isEmpty(), "It must produce readable lines");
        h.succeed();
    }
}
