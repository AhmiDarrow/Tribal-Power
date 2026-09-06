package tk.darrow.tribalpower.guide;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the Spirit Codex as a written book from chapter text.
 * Chapters also live under data/tribalpower/guide/ for Patchouli or future UI.
 */
public final class GuideBookFactory {
    private GuideBookFactory() {}

    public static ItemStack createBook() {
        List<Filterable<Component>> pages = new ArrayList<>();
        for (String text : GuidePages.allPages()) {
            pages.add(Filterable.passThrough(Component.literal(text)));
        }

        WrittenBookContent content = new WrittenBookContent(
                Filterable.passThrough("Spirit Codex"),
                "Tribal Power",
                0,
                pages,
                true
        );

        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
        return book;
    }
}
