package tk.darrow.tribalpower.song;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

/** The pages bound into a songbook, and which one is open. */
public final class SongPages {
    private static final String PAGES = "Pages";
    private static final String OPEN = "Open";

    private SongPages() {}

    public static List<SongVerse> pages(ItemStack book) {
        CompoundTag tag = book.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag list = tag.getList(PAGES, Tag.TAG_COMPOUND);
        List<SongVerse> pages = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            SongVerse verse = SongVerse.read(list.getCompound(i));
            if (verse != null) pages.add(verse);
        }
        return pages;
    }

    public static int open(ItemStack book) {
        List<SongVerse> pages = pages(book);
        if (pages.isEmpty()) return 0;
        int index = book.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(OPEN);
        return Math.floorMod(index, pages.size());
    }

    public static @Nullable SongVerse selected(ItemStack book) {
        List<SongVerse> pages = pages(book);
        return pages.isEmpty() ? null : pages.get(open(book));
    }

    public static boolean add(ItemStack book, SongbookTier tier, SongVerse verse) {
        if (!verse.sheetLength() || !tier.accepts(verse.reagents().size())) return false;
        List<SongVerse> pages = pages(book);
        if (pages.size() >= tier.pages) return false;
        pages.add(verse);
        write(book, pages, pages.size() - 1);
        return true;
    }

    public static @Nullable SongVerse removeOpen(ItemStack book) {
        List<SongVerse> pages = pages(book);
        if (pages.isEmpty()) return null;
        int index = open(book);
        SongVerse removed = pages.remove(index);
        write(book, pages, pages.isEmpty() ? 0 : Math.min(index, pages.size() - 1));
        return removed;
    }

    public static void cycle(ItemStack book) {
        List<SongVerse> pages = pages(book);
        if (pages.size() < 2) return;
        write(book, pages, (open(book) + 1) % pages.size());
    }

    private static void write(ItemStack book, List<SongVerse> pages, int open) {
        CustomData.update(DataComponents.CUSTOM_DATA, book, tag -> {
            ListTag list = new ListTag();
            for (SongVerse verse : pages) {
                CompoundTag page = new CompoundTag();
                verse.write(page);
                list.add(page);
            }
            if (list.isEmpty()) tag.remove(PAGES);
            else tag.put(PAGES, list);
            tag.putInt(OPEN, open);
        });
    }
}
