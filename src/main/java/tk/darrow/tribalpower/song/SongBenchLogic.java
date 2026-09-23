package tk.darrow.tribalpower.song;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.blockentity.ResonanceTotemBlockEntity;
import tk.darrow.tribalpower.entity.CreatureProfile;
import tk.darrow.tribalpower.item.MachineRank;
import tk.darrow.tribalpower.item.RitualChalkItem;
import tk.darrow.tribalpower.lattice.LatticeNetwork;

/**
 * Empowering, sheet-writing and fletching. The screen asks. This is what actually spends the pouch,
 * the chalk, the paper and the Pulse.
 */
public final class SongBenchLogic {
    public static final int EMPOWER_PULSE = 16;
    public static final int WRITE_PULSE_EACH = 12;
    public static final int FLETCH_PULSE = 8;
    public static final int FLETCH_COUNT = 4;

    private SongBenchLogic() {}

    public record Attempt(boolean ok, Component message, ItemStack made) {
        public static Attempt done(String key, Object... args) {
            return new Attempt(true, Component.translatable(key, args), ItemStack.EMPTY);
        }

        public static Attempt made(ItemStack stack, String key, Object... args) {
            return new Attempt(true, Component.translatable(key, args), stack);
        }

        public static Attempt fail(String key, Object... args) {
            return new Attempt(false, Component.translatable(key, args), ItemStack.EMPTY);
        }
    }

    public static List<Attunement> voices(Level level, BlockPos origin) {
        Set<Attunement> found = new LinkedHashSet<>();
        for (var be : LatticeNetwork.blockEntitiesAround(level, origin, LatticeNetwork.DEFAULT_RADIUS)) {
            if (be instanceof ResonanceTotemBlockEntity totem) found.add(totem.getAttunement());
        }
        return new ArrayList<>(found);
    }

    public static int cost(net.minecraft.world.level.block.entity.BlockEntity bench, int base) {
        return Math.max(1, Math.round(base * MachineRank.timeFactor(MachineRank.rank(bench))));
    }

    public static Attempt empower(ServerLevel level, BlockPos origin, net.minecraft.world.level.block.entity.BlockEntity bench,
                                  ItemStack pouch, CreatureProfile profile) {
        if (pouch == null || !(pouch.getItem() instanceof ReagentPouchItem)) return Attempt.fail("message.tribalpower.song_bench.no_pouch");
        if (level.hasNeighborSignal(origin)) return Attempt.fail("message.tribalpower.redstone.locked");
        List<Attunement> voices = voices(level, origin);
        if (voices.isEmpty()) return Attempt.fail("message.tribalpower.song_bench.no_totems");
        if (ReagentPouch.raw(pouch, profile) <= 0) return Attempt.fail("message.tribalpower.song_bench.no_raw");
        if (ReagentPouch.empowered(pouch, profile) >= ReagentPouch.CAP) return Attempt.fail("message.tribalpower.song_bench.pouch_full");
        int price = cost(bench, EMPOWER_PULSE);
        if (!pay(level, origin, price)) return Attempt.fail("message.tribalpower.song_bench.no_pulse");
        ReagentPouch.empower(pouch, profile, 1);
        return Attempt.done("message.tribalpower.song_bench.empowered", Component.translatable("item.tribalpower." + profile.reagent));
    }

    public static Attempt write(ServerLevel level, BlockPos origin, net.minecraft.world.level.block.entity.BlockEntity bench,
                                Player player, ItemStack pouch, List<String> sequence, @Nullable Attunement voice,
                                ItemStack paper, ItemStack chalk, ItemStack book, ItemStack output) {
        if (pouch == null || !(pouch.getItem() instanceof ReagentPouchItem)) return Attempt.fail("message.tribalpower.song_bench.no_pouch");
        if (level.hasNeighborSignal(origin)) return Attempt.fail("message.tribalpower.redstone.locked");
        if (sequence.size() < SongVerse.MIN_SHEET || sequence.size() > SongVerse.MAX_SHEET) {
            return Attempt.fail("message.tribalpower.song_bench.need_item");
        }
        if (voice == null || !voices(level, origin).contains(voice)) return Attempt.fail("message.tribalpower.song_bench.no_totems");
        if (!paper.is(Items.PAPER)) return Attempt.fail("message.tribalpower.song_bench.need_paper");
        if (!(chalk.getItem() instanceof RitualChalkItem) || RitualChalkItem.remaining(chalk) <= 0) {
            return Attempt.fail("message.tribalpower.song_bench.need_chalk");
        }
        SongVerse verse = new SongVerse(sequence, voice);
        boolean intoBook = false;
        if (book.getItem() instanceof SongbookItem songbook && songbook.tier().accepts(sequence.size())) {
            if (SongPages.pages(book).size() >= songbook.tier().pages) return Attempt.fail("message.tribalpower.song_bench.book_full");
            intoBook = true;
        } else if (!output.isEmpty()) {
            return Attempt.fail("message.tribalpower.song_bench.output_full");
        }
        for (String id : sequence) {
            if (Reagents.byId(id) == null) return Attempt.fail("message.tribalpower.song_bench.need_item");
        }
        // Count before spending, so a short stack fails with the pouch untouched.
        var needed = new java.util.HashMap<CreatureProfile, Integer>();
        for (String id : sequence) needed.merge(Reagents.byId(id), 1, Integer::sum);
        for (var entry : needed.entrySet()) {
            if (ReagentPouch.empowered(pouch, entry.getKey()) < entry.getValue()) {
                return Attempt.fail("message.tribalpower.song_bench.need_empowered",
                        Component.translatable("item.tribalpower." + entry.getKey().reagent));
            }
        }
        int price = cost(bench, WRITE_PULSE_EACH * sequence.size());
        if (!pay(level, origin, price)) return Attempt.fail("message.tribalpower.song_bench.no_pulse");
        for (var entry : needed.entrySet()) ReagentPouch.consumeEmpowered(pouch, entry.getKey(), entry.getValue());
        paper.shrink(1);
        RitualChalkItem.spend(chalk, player);
        if (intoBook) {
            SongPages.add(book, ((SongbookItem) book.getItem()).tier(), verse);
            return Attempt.done("message.tribalpower.song_bench.bound", verse.name());
        }
        return Attempt.made(SongSheetItem.create(verse), "message.tribalpower.song_bench.written", verse.name());
    }

    public static Attempt fletch(ServerLevel level, BlockPos origin, net.minecraft.world.level.block.entity.BlockEntity bench,
                                 ItemStack pouch, CreatureProfile profile, @Nullable Attunement voice, ItemStack output) {
        if (pouch == null || !(pouch.getItem() instanceof ReagentPouchItem)) return Attempt.fail("message.tribalpower.song_bench.no_pouch");
        if (level.hasNeighborSignal(origin)) return Attempt.fail("message.tribalpower.redstone.locked");
        if (voice == null || !voices(level, origin).contains(voice)) return Attempt.fail("message.tribalpower.song_bench.no_totems");
        if (ReagentPouch.empowered(pouch, profile) < 1) {
            return Attempt.fail("message.tribalpower.song_bench.need_empowered", Component.translatable("item.tribalpower." + profile.reagent));
        }
        ItemStack arrows = VerseArrowItem.create(profile, voice, FLETCH_COUNT);
        if (!output.isEmpty() && (!ItemStack.isSameItemSameComponents(output, arrows) || output.getCount() + FLETCH_COUNT > output.getMaxStackSize())) {
            return Attempt.fail("message.tribalpower.song_bench.output_full");
        }
        int price = cost(bench, FLETCH_PULSE);
        if (!pay(level, origin, price)) return Attempt.fail("message.tribalpower.song_bench.no_pulse");
        ReagentPouch.consumeEmpowered(pouch, profile, 1);
        ItemStack made = output.isEmpty() ? arrows : output.copy();
        if (!output.isEmpty()) made.grow(FLETCH_COUNT);
        return Attempt.made(made, "message.tribalpower.song_bench.fletched", FLETCH_COUNT);
    }

    private static boolean pay(ServerLevel level, BlockPos origin, int price) {
        if (LatticeNetwork.extractPulseNearby(level, origin, LatticeNetwork.DEFAULT_RADIUS, price, true) < price) return false;
        LatticeNetwork.extractPulseNearby(level, origin, LatticeNetwork.DEFAULT_RADIUS, price, false);
        return true;
    }
}
