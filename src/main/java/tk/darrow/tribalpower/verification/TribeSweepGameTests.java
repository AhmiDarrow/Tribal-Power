package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.lattice.LatticeNetwork;
import tk.darrow.tribalpower.tribe.KinRole;
import tk.darrow.tribalpower.tribe.KinshipTotemBlock;
import tk.darrow.tribalpower.tribe.TribalKinEntity;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.tribe.TribeHooks;
import tk.darrow.tribalpower.tribe.TribeRank;
import tk.darrow.tribalpower.tribe.TribeRegistry;

/** Regression tests from the 3.0 tribes bug sweep: Elder stock persistence, met flags, kinship totem scans. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class TribeSweepGameTests {
    /** Closing and reopening the trade screen (or saving and reloading the Elder) must not restock exhausted offers. */
    @GameTest(template = "empty")
    public static void elderStockPersistsAcrossReopen(GameTestHelper h) {
        TribalKinEntity kin = TribeRegistry.TRIBAL_KIN.get().create(h.getLevel());
        kin.setTribe(TribeDefinition.STONE);
        kin.setRole(KinRole.ELDER);
        MerchantOffers first = kin.offersFor(TribeRank.GUEST);
        h.assertTrue(first.size() == 2, "Guests see two offers");
        MerchantOffer offer = first.get(1);
        for (int i = 0; i < offer.getMaxUses(); i++) kin.notifyTrade(offer);
        h.assertTrue(offer.isOutOfStock(), "The offer is sold out after maxUses trades");
        // Reopen at the same rank: same stock.
        MerchantOffers again = kin.offersFor(TribeRank.GUEST);
        h.assertTrue(again.get(1).isOutOfStock() && !again.get(0).isOutOfStock(), "Reopening keeps the spent stock");
        // Rank up: the same offer keeps its uses, new offers are fresh.
        MerchantOffers friend = kin.offersFor(TribeRank.FRIEND);
        h.assertTrue(friend.size() == 4 && friend.get(1).isOutOfStock() && !friend.get(2).isOutOfStock(),
                "Rank changes filter the same stock instead of rebuilding it");
        // Save and reload into a fresh Elder.
        CompoundTag tag = new CompoundTag();
        kin.addAdditionalSaveData(tag);
        h.assertTrue(tag.getIntArray("TradeUses").length == 6 && tag.getIntArray("TradeUses")[1] == offer.getMaxUses(),
                "TradeUses is persisted per trade-table entry");
        TribalKinEntity reloaded = TribeRegistry.TRIBAL_KIN.get().create(h.getLevel());
        reloaded.readAdditionalSaveData(tag);
        h.assertTrue(reloaded.tribe() == TribeDefinition.STONE && reloaded.role() == KinRole.ELDER, "Tribe and role round-trip");
        MerchantOffers loaded = reloaded.offersFor(TribeRank.GUEST);
        h.assertTrue(loaded.get(1).isOutOfStock() && !loaded.get(0).isOutOfStock(), "Spent stock survives save and load");
        h.assertTrue(kin.buildOffers(TribeRank.GUEST).get(1).getUses() == 0, "buildOffers still describes a fresh table");
        h.succeed();
    }

    /** First-meeting flags must live under PlayerPersisted so death and dimension changes keep them. */
    @GameTest(template = "empty")
    public static void metFlagsSurviveClone(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        h.assertFalse(TribeHooks.hasMet(player, TribeDefinition.SWARM), "A new player has not met the Colony-keepers");
        TribeHooks.markMet(player, TribeDefinition.SWARM);
        TribeHooks.markMet(player, TribeDefinition.SIGIL);
        h.assertTrue(TribeHooks.hasMet(player, TribeDefinition.SWARM) && TribeHooks.hasMet(player, TribeDefinition.SIGIL)
                && !TribeHooks.hasMet(player, TribeDefinition.SOIL), "Met flags are per tribe");
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        h.assertTrue(persisted.contains("tribalpower_tribes_met"), "Met flags are stored under " + Player.PERSISTED_NBT_TAG);
        h.succeed();
    }

    /** The chunk-walking kinship scan must respect the cube radius, including totems across a chunk border. */
    @GameTest(template = "empty")
    public static void kinshipScanRespectsRadius(GameTestHelper h) {
        BlockPos origin = h.absolutePos(new BlockPos(1, 2, 1));
        // Place the totem 8 blocks away in x so the cube edge is tested, and pick a position that may cross a chunk border.
        BlockPos inside = origin.offset(8, 0, 0);
        BlockPos outside = origin.offset(0, 0, 9);
        h.getLevel().setBlock(inside, TribeRegistry.KINSHIP_TOTEM.get().defaultBlockState().setValue(KinshipTotemBlock.TRIBE, TribeDefinition.CLAW.ordinal()), 3);
        h.getLevel().setBlock(outside, TribeRegistry.KINSHIP_TOTEM.get().defaultBlockState().setValue(KinshipTotemBlock.TRIBE, TribeDefinition.CLOCK.ordinal()), 3);
        h.assertTrue(LatticeNetwork.findNearbyKinshipTotems(h.getLevel(), origin, 8).size() == 1, "Only the totem within 8 blocks is found");
        h.assertTrue(LatticeNetwork.countKinshipTribes(h.getLevel(), origin, 8) == 1, "One tribe within radius 8");
        h.assertTrue(LatticeNetwork.countKinshipTribes(h.getLevel(), origin, 9) == 2, "Both tribes within radius 9");
        h.getLevel().setBlock(inside, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        h.getLevel().setBlock(outside, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        h.succeed();
    }
}
