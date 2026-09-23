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
import tk.darrow.tribalpower.song.PulseBowItem;
import tk.darrow.tribalpower.song.ReagentPouch;
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
            h.assertTrue(bench.getItem(0).isEmpty(), "Paper is spent");
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
}
