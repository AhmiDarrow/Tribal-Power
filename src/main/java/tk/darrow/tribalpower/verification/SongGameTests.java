package tk.darrow.tribalpower.verification;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.blockentity.DrumheartBlockEntity;
import tk.darrow.tribalpower.blockentity.SongBenchBlockEntity;
import tk.darrow.tribalpower.entity.CreatureProfile;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.item.RitualChalkItem;
import tk.darrow.tribalpower.song.Note;
import tk.darrow.tribalpower.song.PouchMenu;
import tk.darrow.tribalpower.song.PulseBowItem;
import tk.darrow.tribalpower.song.ReagentPouch;
import tk.darrow.tribalpower.song.SongBenchMenu;
import tk.darrow.tribalpower.song.ReagentPouchHooks;
import tk.darrow.tribalpower.song.Reagents;
import tk.darrow.tribalpower.song.SongBenchLogic;
import tk.darrow.tribalpower.song.SongPages;
import tk.darrow.tribalpower.song.SongShape;
import tk.darrow.tribalpower.song.SongSheetItem;
import tk.darrow.tribalpower.song.SongVerse;
import tk.darrow.tribalpower.song.SongbookTier;
import tk.darrow.tribalpower.song.VerseArrowItem;

/** The Song Bench writes verses. It does not refine Echo. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class SongGameTests {
    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> T at(GameTestHelper h, BlockPos pos, Class<T> type) {
        var be = h.getBlockEntity(pos);
        h.assertTrue(type.isInstance(be), pos + " should hold " + type.getSimpleName());
        return type.cast(be);
    }

    @GameTest(template = "empty")
    public static void pouchKeepsReagentsAndNothingElse(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            var pouch = new ItemStack(ModItems.REAGENT_POUCH.get());
            player.getInventory().setItem(0, pouch);
            var stone = new ItemStack(Items.STONE, 8);
            h.assertTrue(ReagentPouchHooks.absorb(player, stone) == 8, "Stone is not a reagent");
            var velvet = new ItemStack(Reagents.item(CreatureProfile.DAWN_STAG), 10);
            h.assertTrue(ReagentPouchHooks.absorb(player, velvet) == 0, "A hotbar pouch takes the whole pickup");
            h.assertTrue(ReagentPouch.raw(pouch, CreatureProfile.DAWN_STAG) == 10, "Raw count is the pickup");
            h.assertTrue(ReagentPouch.empowered(pouch, CreatureProfile.DAWN_STAG) == 0, "Pickup does not empower");
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void benchEmpowersWritesAndFletches(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, ModBlocks.SONG_BENCH.get());
        h.setBlock(4, 2, 2, ModBlocks.RESONANCE_TOTEM_FIRE.get());
        h.setBlock(2, 2, 4, ModBlocks.DRUMHEART.get());
        var bench = at(h, pos, SongBenchBlockEntity.class);
        var drum = at(h, new BlockPos(2, 2, 4), DrumheartBlockEntity.class);
        drum.insertPulse(400, false);
        var player = VerificationPlayers.inLevel(h);
        try {
            var pouch = new ItemStack(ModItems.REAGENT_POUCH.get());
            player.getInventory().setItem(0, pouch);
            ReagentPouch.addRaw(pouch, CreatureProfile.ASHBOUND, 1);
            ReagentPouch.addRaw(pouch, CreatureProfile.DAWN_STAG, 2);
            var origin = bench.getBlockPos();
            var empowered = SongBenchLogic.empower((net.minecraft.server.level.ServerLevel) h.getLevel(), origin, bench, pouch, CreatureProfile.ASHBOUND);
            h.assertTrue(empowered.ok(), empowered.message().getString());
            SongBenchLogic.empower((net.minecraft.server.level.ServerLevel) h.getLevel(), origin, bench, pouch, CreatureProfile.DAWN_STAG);
            SongBenchLogic.empower((net.minecraft.server.level.ServerLevel) h.getLevel(), origin, bench, pouch, CreatureProfile.DAWN_STAG);
            h.assertTrue(ReagentPouch.empowered(pouch, CreatureProfile.ASHBOUND) == 1, "Ember heart is empowered");
            h.assertTrue(drum.getPulseStored() == 400 - 16 * 3, "Each empower spends 16 Pulse, drum holds " + drum.getPulseStored());

            player.getAbilities().instabuild = false;
            bench.setItem(SongBenchBlockEntity.PAPER, new ItemStack(Items.PAPER));
            bench.setItem(SongBenchBlockEntity.CHALK, new ItemStack(ModItems.RITUAL_CHALK.get()));
            bench.setVoice(Attunement.FIRE);
            var order = List.of(CreatureProfile.ASHBOUND.reagent, CreatureProfile.DAWN_STAG.reagent, CreatureProfile.DAWN_STAG.reagent);
            var shortSong = SongBenchLogic.write((net.minecraft.server.level.ServerLevel) h.getLevel(), origin, bench, player, pouch,
                    List.of(order.get(0), order.get(1)), Attunement.FIRE,
                    bench.getItem(0), bench.getItem(1), ItemStack.EMPTY, ItemStack.EMPTY);
            h.assertFalse(shortSong.ok(), "Two reagents is not a sheet");

            var written = SongBenchLogic.write((net.minecraft.server.level.ServerLevel) h.getLevel(), origin, bench, player, pouch, order, Attunement.FIRE,
                    bench.getItem(0), bench.getItem(1), ItemStack.EMPTY, ItemStack.EMPTY);
            h.assertTrue(written.ok(), written.message().getString());
            var verse = SongSheetItem.verse(written.made());
            h.assertTrue(verse != null && verse.reagents().equals(order), "The sheet remembers the order");
            h.assertTrue(verse.shape() == SongShape.BOLT && verse.power() == 1 && verse.voice() == Attunement.FIRE, "Lead ember is a fire bolt");
            h.assertTrue(verse.riders().equals(List.of(Note.QUIET)), "The second note is the only rider");
            h.assertTrue(bench.getItem(0).isEmpty() && !bench.getItem(0).is(Items.PAPER), "Paper leaves the slot");
            h.assertTrue(RitualChalkItem.remaining(bench.getItem(1)) == RitualChalkItem.USES - 1, "One chalk mark is spent");
            h.assertTrue(ReagentPouch.empowered(pouch, CreatureProfile.ASHBOUND) == 0, "The sheet spent the empowered heart");

            ReagentPouch.addRaw(pouch, CreatureProfile.ASHBOUND, 1);
            SongBenchLogic.empower((net.minecraft.server.level.ServerLevel) h.getLevel(), origin, bench, pouch, CreatureProfile.ASHBOUND);
            var fletched = SongBenchLogic.fletch((net.minecraft.server.level.ServerLevel) h.getLevel(), origin, bench, pouch,
                    CreatureProfile.ASHBOUND, Attunement.FIRE, ItemStack.EMPTY);
            h.assertTrue(fletched.ok() && fletched.made().getCount() == 4, "One reagent fletches four arrows");
            var arrow = VerseArrowItem.verse(fletched.made());
            h.assertTrue(arrow != null && arrow.leadProfile() == CreatureProfile.ASHBOUND && arrow.voice() == Attunement.FIRE,
                    "The arrow carries the reagent and the voice");
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void booksHoldOnlyWhatTheirTierAllows(GameTestHelper h) {
        var first = new ItemStack(ModItems.SONGBOOK.get());
        var chorus = new ItemStack(ModItems.CHORUS_SONGBOOK.get());
        var three = new SongVerse(List.of("ember_heart", "dawn_velvet", "dawn_velvet"), Attunement.FIRE);
        var seven = new SongVerse(List.of("ember_heart", "dawn_velvet", "lantern_down", "mossback_scale", "storm_wing", "reed_fang", "ember_heart"), Attunement.FIRE);
        h.assertTrue(SongbookTier.FIRST.accepts(3) && !SongbookTier.FIRST.accepts(4), "The first book stops at three reagents");
        h.assertTrue(SongPages.add(first, SongbookTier.FIRST, three), "A three-note sheet binds");
        h.assertFalse(SongPages.add(first, SongbookTier.FIRST, three), "One page is the whole first book");
        h.assertTrue(SongPages.add(chorus, SongbookTier.CHORUS, seven), "The chorus book holds a seven-note sheet");
        h.assertTrue(three.castPulse() == 8 + 4 * 3, "Cast Pulse climbs with the sheet");
        h.assertTrue(PulseBowItem.cost(1.0F, false) == PulseBowItem.PLAIN_PULSE, "A plain draw spends the plain price");
        h.assertTrue(PulseBowItem.cost(1.0F, true) == PulseBowItem.PLAIN_PULSE + PulseBowItem.VERSE_PULSE, "A verse arrow adds its price");
        h.assertTrue(PulseBowItem.cost(0.05F, true) == 0, "A twitch does not spend Pulse");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void lastPaperAndLastChalkLeaveTheSlot(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, ModBlocks.SONG_BENCH.get());
        h.setBlock(4, 2, 2, ModBlocks.RESONANCE_TOTEM_FIRE.get());
        h.setBlock(2, 2, 4, ModBlocks.DRUMHEART.get());
        var bench = at(h, pos, SongBenchBlockEntity.class);
        at(h, new BlockPos(2, 2, 4), DrumheartBlockEntity.class).insertPulse(200, false);
        var player = VerificationPlayers.inLevel(h);
        try {
            player.getAbilities().instabuild = false;
            var pouch = new ItemStack(ModItems.REAGENT_POUCH.get());
            player.getInventory().setItem(0, pouch);
            ReagentPouch.addRaw(pouch, CreatureProfile.ASHBOUND, 2);
            ReagentPouch.addRaw(pouch, CreatureProfile.DAWN_STAG, 4);
            var level = (net.minecraft.server.level.ServerLevel) h.getLevel();
            var origin = bench.getBlockPos();
            SongBenchLogic.empower(level, origin, bench, pouch, CreatureProfile.ASHBOUND);
            SongBenchLogic.empower(level, origin, bench, pouch, CreatureProfile.ASHBOUND);
            SongBenchLogic.empower(level, origin, bench, pouch, CreatureProfile.DAWN_STAG);
            SongBenchLogic.empower(level, origin, bench, pouch, CreatureProfile.DAWN_STAG);
            SongBenchLogic.empower(level, origin, bench, pouch, CreatureProfile.DAWN_STAG);
            SongBenchLogic.empower(level, origin, bench, pouch, CreatureProfile.DAWN_STAG);
            var chalk = new ItemStack(ModItems.RITUAL_CHALK.get());
            for (int i = 0; i < RitualChalkItem.USES - 1; i++) RitualChalkItem.spend(chalk, player);
            h.assertTrue(RitualChalkItem.remaining(chalk) == 1, "The stick has one mark left");
            bench.setItem(SongBenchBlockEntity.PAPER, new ItemStack(Items.PAPER, 1));
            bench.setItem(SongBenchBlockEntity.CHALK, chalk);
            bench.setVoice(Attunement.FIRE);
            var order = List.of(CreatureProfile.ASHBOUND.reagent, CreatureProfile.DAWN_STAG.reagent, CreatureProfile.DAWN_STAG.reagent);
            var written = SongBenchLogic.write(level, origin, bench, player, pouch, order, Attunement.FIRE,
                    bench.getItem(SongBenchBlockEntity.PAPER), bench.getItem(SongBenchBlockEntity.CHALK),
                    ItemStack.EMPTY, ItemStack.EMPTY);
            h.assertTrue(written.ok(), written.message().getString());
            h.assertFalse(bench.getItem(SongBenchBlockEntity.PAPER).is(Items.PAPER), "The last paper is gone");
            h.assertFalse(bench.getItem(SongBenchBlockEntity.CHALK).getItem() instanceof RitualChalkItem, "The last chalk mark is gone");
            bench.setItem(SongBenchBlockEntity.PAPER, new ItemStack(Items.PAPER, 1));
            var again = SongBenchLogic.write(level, origin, bench, player, pouch, order, Attunement.FIRE,
                    bench.getItem(SongBenchBlockEntity.PAPER), bench.getItem(SongBenchBlockEntity.CHALK),
                    ItemStack.EMPTY, ItemStack.EMPTY);
            h.assertFalse(again.ok(), "A cleared chalk slot cannot write another sheet");
            h.assertTrue(bench.getItem(SongBenchBlockEntity.PAPER).getCount() == 1, "The refused write keeps its paper");
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void lastVerseArrowLeavesTheInventory(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            player.getAbilities().instabuild = false;
            var arrow = VerseArrowItem.create(CreatureProfile.ASHBOUND, Attunement.FIRE, 1);
            player.getInventory().setItem(0, arrow);
            h.assertTrue(PulseBowItem.findVerse(player) == arrow, "The verse arrow is in the hotbar");
            PulseBowItem.takeVerse(player, arrow);
            h.assertTrue(player.getInventory().getItem(0).isEmpty(), "The last arrow leaves the slot");
            h.assertTrue(PulseBowItem.findVerse(player) == null, "An emptied slot is not another arrow");
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void withdrawingAPouchKeepsWhatDoesNotFit(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            player.getAbilities().instabuild = false;
            var pouch = new ItemStack(ModItems.REAGENT_POUCH.get());
            player.getInventory().setItem(0, pouch);
            ReagentPouch.addRaw(pouch, CreatureProfile.DAWN_STAG, 64);
            var reagent = Reagents.item(CreatureProfile.DAWN_STAG);
            for (int slot = 1; slot < player.getInventory().items.size(); slot++)
                player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
            player.getInventory().setItem(10, new ItemStack(reagent, 60));
            h.assertTrue(Reagents.inventoryRoom(player, CreatureProfile.DAWN_STAG) == 4,
                    "Four fit beside a full inventory");
            var menu = new PouchMenu(0, player.getInventory(), 0);
            h.assertTrue(menu.clickMenuButton(player, SongBenchMenu.WITHDRAW + CreatureProfile.DAWN_STAG.ordinal()),
                    "Taking from the pouch is accepted");
            int held = 0;
            for (ItemStack stack : player.getInventory().items) if (stack.is(reagent)) held += stack.getCount();
            int stored = ReagentPouch.raw(pouch, CreatureProfile.DAWN_STAG);
            h.assertTrue(stored == 60, "The pouch keeps what the inventory cannot hold, left " + stored);
            h.assertTrue(held == 64, "The four that fit stay in the inventory, holds " + held);
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }
}
