package tk.darrow.tribalpower.verification;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.tribe.CodexUnlocksPayload;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.tribe.TribeStandingSavedData;
import tk.darrow.tribalpower.world.structure.LoreTabletBlock;

import java.util.UUID;

/** Server-side tracking behind {@code tribalpower:codex_unlocks}: Tribe Mark and Lore Tablet bitmasks. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class CodexUnlockGameTests {
    @GameTest(template = "empty")
    public static void markGrantSetsTribeBit(GameTestHelper h) {
        var data = TribeStandingSavedData.get(h.getLevel().getServer());
        UUID id = UUID.randomUUID();
        h.assertTrue(data.marks(id) == 0, "A fresh player holds no Marks");
        data.grantMark(id, TribeDefinition.SPARK);
        h.assertTrue((data.marks(id) & (1 << TribeDefinition.SPARK.ordinal())) != 0, "Granting a Mark sets that tribe's bit");
        h.assertTrue(data.hasMark(id, TribeDefinition.SPARK) && !data.hasMark(id, TribeDefinition.SOIL),
                "Only the granted tribe is marked");
        data.grantMark(id, TribeDefinition.SPINDLE);
        h.assertTrue(data.marks(id) == ((1 << TribeDefinition.SPARK.ordinal()) | (1 << TribeDefinition.SPINDLE.ordinal())),
                "The mask accumulates exactly the granted tribes, got " + data.marks(id));
        var player = h.makeMockServerPlayerInLevel();
        data.grantMark(player.getUUID(), TribeDefinition.CLAW);
        var payload = CodexUnlocksPayload.of(player);
        h.assertTrue(payload.tribes() == (1 << TribeDefinition.CLAW.ordinal()) && payload.tablets() == 0,
                "The payload carries the player's Mark bitmask and an empty tablet mask");
        CodexUnlocksPayload.sync(player); // no negotiated channel on a mock player: must be a silent no-op
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tabletReadSetsTabletBit(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        h.assertTrue(LoreTabletBlock.readMask(player) == 0, "A fresh player has read no tablets");
        h.assertTrue(LoreTabletBlock.markRead(player, 4), "The first read of a tablet reports true");
        h.assertTrue(!LoreTabletBlock.markRead(player, 4), "Re-reading the same tablet reports false");
        LoreTabletBlock.markRead(player, 11);
        int expected = (1 << 4) | (1 << 11);
        h.assertTrue(LoreTabletBlock.readMask(player) == expected, "The tablet mask tracks exactly the tablets read");
        var payload = CodexUnlocksPayload.of(player);
        h.assertTrue(payload.tablets() == expected && payload.tribes() == 0,
                "The payload carries the tablet bitmask, got " + payload.tablets());
        h.succeed();
    }
}
