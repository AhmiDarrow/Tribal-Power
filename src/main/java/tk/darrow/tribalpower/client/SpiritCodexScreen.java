package tk.darrow.tribalpower.client;

import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import tk.darrow.tribalpower.client.codex.CodexBook;
import tk.darrow.tribalpower.client.codex.CodexBook.Entry;
import tk.darrow.tribalpower.client.codex.CodexBook.Page;
import tk.darrow.tribalpower.client.codex.CodexScene;
import tk.darrow.tribalpower.client.codex.CodexText;
import tk.darrow.tribalpower.echo.LatticeRecipe;
import tk.darrow.tribalpower.integration.jei.CodexJeiLinks;

/**
 * The Spirit Codex: a two-page book. The landing spread lists categories; a category lists its entries; an
 * entry is a run of pages -- text, a spotlighted item, its live recipe, a picture, or a stepped 3D scene --
 * turned two at a time. Every item on a page links to the entry about it. Content lives in {@code assets/tribalpower/codex/} ({@link CodexBook}).
 */
public final class SpiritCodexScreen extends Screen {
    private static final int INK = 0xFF101C27, TEAL = 0xFF74DBCB, GOLD = 0xFFE4C18A, PAPER = 0xFFE4E5DA, DIM = 0xFF9CB8B9;
    private static final int SCENE_H = 118;

    private enum View { LANDING, CATEGORY, ENTRY, SEARCH, BOOKMARKS, ITEM }

    private record State(View view, String category, String entry, int spread, String item) {}

    private record Area(int x, int y, int w, int h, Runnable action) {
        boolean inside(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private record Hit(int x, int y, ItemStack item) {}

    /** A page as laid out on screen: a book page plus the slice of its text that fits. */
    private record Leaf(Page page, List<CodexText.Line> lines, boolean first, boolean continuation) {}

    private final Set<String> bookmarks = new HashSet<>();
    private final Deque<State> history = new ArrayDeque<>();
    private final List<Hit> hits = new ArrayList<>();
    private final List<Area> areas = new ArrayList<>();
    private final List<CodexText.Link> links = new ArrayList<>();
    private final Map<Page, Integer> sceneStep = new HashMap<>();
    private final Map<Page, Float> sceneYaw = new HashMap<>();
    private final Map<Page, Double> sceneSince = new HashMap<>();
    private final Map<Page, List<CodexBook.Step>> patternSteps = new HashMap<>();
    private final Map<Page, Integer> recipeIndex = new HashMap<>();
    private final long epoch = System.nanoTime();
    private View view = View.LANDING;
    private String category = "", entry = "", item = "", query = "";
    private int spread, listPage, recipePage;
    private boolean spoilers, showUses;
    private List<Leaf> leaves = List.of();
    private Area dragging;
    private Page dragPage;
    private int left, top, bookWidth, bookHeight, pageWidth, pageHeight, leftX, rightX, pageY;
    private EditBox search;

    public SpiritCodexScreen() {
        super(Component.translatable("gui.tribalpower.codex.title"));
        try {
            var file = Minecraft.getInstance().gameDirectory.toPath().resolve("config/tribalpower-codex-bookmarks.txt");
            if (Files.isRegularFile(file)) Files.readAllLines(file).stream().limit(128).forEach(bookmarks::add);
        } catch (java.io.IOException ignored) {
        }
        spoilers = spoilerReaders().contains(reader());
    }

    // Spoilers, once shown, stay shown for that player until they hide them again. Kept per player id beside the
    // bookmarks, so two people sharing a machine each keep their own choice.
    private static final String SPOILERS_FILE = "config/tribalpower-codex-spoilers.txt";

    private static String reader() {
        var player = Minecraft.getInstance().player;
        return player == null ? "" : player.getUUID().toString();
    }

    private static Set<String> spoilerReaders() {
        Set<String> readers = new TreeSet<>();
        try {
            var file = Minecraft.getInstance().gameDirectory.toPath().resolve(SPOILERS_FILE);
            if (Files.isRegularFile(file)) Files.readAllLines(file).stream().map(String::trim).filter(line -> !line.isEmpty()).limit(256).forEach(readers::add);
        } catch (java.io.IOException ignored) {
        }
        return readers;
    }

    private static void rememberSpoilers(boolean on) {
        String id = reader();
        if (id.isEmpty()) return;
        Set<String> readers = spoilerReaders();
        if (on ? !readers.add(id) : !readers.remove(id)) return;
        try {
            var file = Minecraft.getInstance().gameDirectory.toPath().resolve(SPOILERS_FILE);
            Files.createDirectories(file.getParent());
            Files.write(file, readers);
        } catch (java.io.IOException ignored) {
        }
    }

    private CodexBook.Book book() {
        return CodexBook.get();
    }

    private CodexBook.Book linkedBook;
    private CodexText.Linker linker = CodexText.Linker.NONE;

    /** Mentions of other pages link to them; the name table is built once per loaded book. */
    private CodexText.Linker linker() {
        if (linkedBook != book()) {
            linkedBook = book();
            linker = CodexText.Linker.of(linkedBook);
        }
        return linker.from(view == View.ENTRY ? entry : "");
    }

    private double time() {
        return (System.nanoTime() - epoch) / 1_000_000_000.0;
    }

    // ------------------------------------------------------------------ layout and widgets

    @Override
    protected void init() {
        bookWidth = Math.min(width - 12, 760);
        bookHeight = Math.min(height - 12, 440);
        left = (width - bookWidth) / 2;
        top = (height - bookHeight) / 2;
        pageWidth = (bookWidth - 44) / 2;
        pageHeight = bookHeight - 76;
        leftX = left + 16;
        rightX = left + bookWidth / 2 + 6;
        pageY = top + 38;
        search = addRenderableWidget(new EditBox(font, left + bookWidth - 196, top + 10, 150, 16, Component.translatable("gui.tribalpower.codex.search")));
        search.setMaxLength(64);
        search.setHint(Component.translatable("gui.tribalpower.codex.search_hint"));
        search.setValue(query);
        search.setResponder(value -> {
            query = value;
            listPage = 0;
            // The first letter opens search and rebuilds this box, which would drop the caret.
            boolean typing = search.isFocused();
            if (!value.isBlank() && view != View.SEARCH) go(new State(View.SEARCH, "", "", 0, ""), false);
            else if (value.isBlank() && view == View.SEARCH) go(new State(View.LANDING, "", "", 0, ""), false);
            if (typing) setFocused(search);
        });
        button(Component.literal("✕"), left + bookWidth - 40, top + 9, 24, b -> onClose());
        int by = top + bookHeight - 30;
        // The row is laid out from both ends so nothing overlaps at a narrow book: the arrows and the entry button
        // hang from the right edge, the rest from the left, and the spoilers button takes what is between.
        int arrowsLeft = left + bookWidth - 124;
        int entryLeft = arrowsLeft - 84;
        button(Component.translatable("gui.tribalpower.codex.home"), left + 16, by, 52, b -> go(new State(View.LANDING, "", "", 0, ""), true));
        button(Component.translatable("gui.tribalpower.codex.back"), left + 72, by, 52, b -> back());
        button(Component.translatable("gui.tribalpower.codex.bookmarks"), left + 128, by, 76, b -> go(new State(View.BOOKMARKS, "", "", 0, ""), true));
        int spoilerLeft = left + 208, spoilerWidth = Math.max(40, Math.min(96, entryLeft - 4 - spoilerLeft));
        button(Component.translatable(spoilers ? "gui.tribalpower.codex.hide_spoilers" : "gui.tribalpower.codex.spoilers"), spoilerLeft, by, spoilerWidth, b -> {
            if (spoilers) {
                spoilers = false;
                rememberSpoilers(false);
                go(new State(View.LANDING, "", "", 0, ""), false);
                history.clear();
            } else confirmSpoilers(() -> {});
        });
        if (view == View.ENTRY) {
            button(Component.translatable(bookmarks.contains(entry) ? "gui.tribalpower.codex.unmark" : "gui.tribalpower.codex.bookmark"),
                    entryLeft, by, 80, b -> {
                        if (!bookmarks.remove(entry)) bookmarks.add(entry);
                        saveBookmarks();
                        rebuildWidgets();
                    });
            layoutEntry();
        }
        if (view == View.ITEM && CodexJeiLinks.available())
            button(Component.translatable("gui.tribalpower.codex.jei"), entryLeft, by, 80, b -> openJei(stack(item), showUses));
        button(Component.literal("◀"), left + bookWidth - 124, by, 30, b -> turn(-1));
        button(Component.literal("▶"), left + bookWidth - 46, by, 30, b -> turn(1));
    }

    private Button button(Component text, int x, int y, int w, java.util.function.Consumer<Button> action) {
        return addRenderableWidget(new Button(x, y, w, 18, text, action::accept, narration -> narration.get()) {
            @Override
            protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
                boolean hot = isHoveredOrFocused();
                g.fillGradient(getX(), getY(), getX() + getWidth(), getY() + getHeight(), hot ? 0xFF345A5D : 0xFF223A45, 0xFF142530);
                g.renderOutline(getX(), getY(), getWidth(), getHeight(), hot ? GOLD : 0xFF42636A);
                g.drawCenteredString(font, getMessage(), getX() + getWidth() / 2, getY() + 5, active ? (hot ? GOLD : PAPER) : 0xFF667A80);
            }
        });
    }

    /** Lays the current entry's pages out, splitting text that does not fit onto continuation pages. */
    private void layoutEntry() {
        Entry e = book().byId().get(entry);
        if (e == null) {
            leaves = List.of();
            return;
        }
        List<Leaf> out = new ArrayList<>();
        boolean first = true;
        for (Page page : e.pages()) {
            int fixed = fixedHeight(page) + (first ? 20 : 0) + (page.title().isEmpty() ? 0 : 14);
            List<CodexText.Line> lines = CodexText.layout(font, page.text(), pageWidth - 8, linker());
            List<CodexText.Line> taken = new ArrayList<>();
            int used = fixed;
            int i = 0;
            while (i < lines.size() && used + CodexText.lineHeight(lines.get(i)) <= pageHeight) {
                used += CodexText.lineHeight(lines.get(i));
                taken.add(lines.get(i++));
            }
            out.add(new Leaf(page, taken, first, false));
            first = false;
            while (i < lines.size()) {
                List<CodexText.Line> more = new ArrayList<>();
                int h = 0;
                while (i < lines.size() && h + CodexText.lineHeight(lines.get(i)) <= pageHeight) {
                    h += CodexText.lineHeight(lines.get(i));
                    more.add(lines.get(i++));
                }
                if (more.isEmpty()) more.add(lines.get(i++));
                out.add(new Leaf(new CodexBook.Text("", ""), more, false, true));
            }
        }
        leaves = out;
        spread = Math.min(spread, Math.max(0, (leaves.size() - 1) / 2));
    }

    private int fixedHeight(Page page) {
        return switch (page) {
            case CodexBook.Spotlight s -> 58;
            case CodexBook.Recipe r -> 104;
            case CodexBook.Image i -> Math.min(pageWidth - 8, 120) + 8;
            case CodexBook.Scene s -> SCENE_H + 52;
            case CodexBook.Pattern p -> SCENE_H + 52;
            case CodexBook.Quests q -> 96;
            default -> 0;
        };
    }

    // ------------------------------------------------------------------ navigation

    private void go(State state, boolean remember) {
        if (remember) history.push(new State(view, category, entry, spread, item));
        if (history.size() > 128) history.removeLast();
        view = state.view();
        category = state.category();
        entry = state.entry();
        spread = state.spread();
        item = state.item();
        listPage = 0;
        recipePage = 0;
        showUses = false;
        if (view != View.SEARCH) query = "";
        rebuildWidgets();
    }

    private void back() {
        if (history.isEmpty()) {
            if (view == View.LANDING) onClose();
            else go(new State(View.LANDING, "", "", 0, ""), false);
            return;
        }
        State state = history.pop();
        go(state, false);
    }

    private void turn(int direction) {
        switch (view) {
            case ENTRY -> spread = Math.clamp(spread + direction, 0, Math.max(0, (leaves.size() - 1) / 2));
            case ITEM -> recipePage += direction;
            default -> listPage = Math.max(0, listPage + direction);
        }
    }

    private void openEntry(String id) {
        Entry target = book().byId().get(id);
        if (target == null) return;
        if (!visible(target)) {
            confirmSpoilers(() -> openEntry(id));
            return;
        }
        go(new State(View.ENTRY, target.category(), id, 0, ""), true);
    }

    /** An item on a page: the entry that teaches it, else its recipes. */
    private void openItem(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String teaching = book().itemEntries().get(id);
        if (teaching != null && !teaching.equals(entry)) {
            openEntry(teaching);
            return;
        }
        if (!spoilers) {
            confirmSpoilers(() -> openItem(stack));
            return;
        }
        go(new State(View.ITEM, "", "", 0, id), true);
    }

    private void openJei(ItemStack stack, boolean uses) {
        if (stack.isEmpty() || !CodexJeiLinks.available()) return;
        if (!spoilers) {
            confirmSpoilers(() -> openJei(stack, uses));
            return;
        }
        CodexJeiLinks.show(stack, uses);
    }

    private void confirmSpoilers(Runnable after) {
        minecraft.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                spoilers = true;
                rememberSpoilers(true);
            }
            minecraft.setScreen(this);
            if (yes) after.run();
        }, Component.translatable("gui.tribalpower.codex.spoilers_title"), Component.translatable("gui.tribalpower.codex.spoilers_body"),
                Component.translatable("gui.tribalpower.codex.spoilers_yes"), Component.translatable("gui.tribalpower.codex.spoilers_no")));
    }

    private boolean visible(Entry e) {
        return spoilers || !e.spoiler() || CodexUnlocks.unlocked(e.unlock());
    }

    private boolean visible(CodexBook.Category c) {
        return spoilers || !c.spoiler() || book().in(c.id()).stream().anyMatch(this::visible);
    }

    private void saveBookmarks() {
        try {
            var file = minecraft.gameDirectory.toPath().resolve("config/tribalpower-codex-bookmarks.txt");
            Files.createDirectories(file.getParent());
            Files.write(file, new TreeSet<>(bookmarks));
        } catch (java.io.IOException ignored) {
        }
    }

    private ItemStack stack(String id) {
        if (id == null || id.isEmpty()) return ItemStack.EMPTY;
        var key = CodexBook.itemId(id);
        return BuiltInRegistries.ITEM.containsKey(key) ? new ItemStack(BuiltInRegistries.ITEM.get(key)) : ItemStack.EMPTY;
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g, mx, my, partial);
        g.blit(ResourceLocation.parse("tribalpower:textures/gui/codex/quest_atlas.png"), left, top, bookWidth, bookHeight, 0F, 0F, 1536, 1024, 1536, 1024);
        g.fill(leftX - 6, pageY - 6, leftX + pageWidth + 6, pageY + pageHeight + 6, 0xDD101C27);
        g.fill(rightX - 6, pageY - 6, rightX + pageWidth + 6, pageY + pageHeight + 6, 0xDD101C27);
        g.drawString(font, Component.literal(book().title()), left + 18, top + 14, GOLD, false);
        Component mode = Component.translatable(spoilers ? "gui.tribalpower.codex.veil_open" : "gui.tribalpower.codex.spoiler_safe");
        g.drawString(font, mode, left + bookWidth - 206 - font.width(mode), top + 14, TEAL, false);
        hits.clear();
        areas.clear();
        links.clear();
        switch (view) {
            case LANDING -> landing(g);
            case CATEGORY -> categoryView(g);
            case ENTRY -> entryView(g, mx, my);
            case SEARCH, BOOKMARKS -> listView(g);
            case ITEM -> itemView(g);
        }
        for (var widget : renderables) widget.render(g, mx, my, partial);
        for (Hit h : hits)
            if (mx >= h.x && mx < h.x + 16 && my >= h.y && my < h.y + 16) g.renderTooltip(font, h.item, mx, my);
    }

    private void heading(GuiGraphics g, Component text, int x, int y) {
        g.drawString(font, text, x, y, GOLD, false);
        g.fill(x, y + 11, x + pageWidth - 8, y + 12, 0x6674DBCB);
    }

    private int paragraph(GuiGraphics g, String text, int x, int y) {
        List<CodexText.Line> lines = CodexText.layout(font, text, pageWidth - 8, linker());
        links.addAll(CodexText.draw(g, font, lines, x, y));
        return y + CodexText.height(lines);
    }

    private void item(GuiGraphics g, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        g.renderItem(stack, x, y);
        g.renderItemDecorations(font, stack, x, y);
        hits.add(new Hit(x, y, stack));
    }

    private void landing(GuiGraphics g) {
        heading(g, Component.literal(book().title()), leftX, pageY);
        paragraph(g, book().landing(), leftX, pageY + 20);
        heading(g, Component.translatable("gui.tribalpower.codex.categories"), rightX, pageY);
        int y = pageY + 20;
        List<CodexBook.Category> shown = book().categories().stream().filter(this::visible).toList();
        int per = Math.max(1, (pageHeight - 20) / 22);
        listPage = Math.min(listPage, Math.max(0, (shown.size() - 1) / per));
        for (CodexBook.Category c : shown.subList(Math.min(shown.size(), listPage * per), Math.min(shown.size(), (listPage + 1) * per))) {
            row(g, stack(c.icon()), Component.literal(c.name()), progress(c), rightX, y, () -> go(new State(View.CATEGORY, c.id(), "", 0, ""), true));
            y += 22;
        }
    }

    private Component progress(CodexBook.Category c) {
        return switch (c.progress()) {
            case "tribes" -> Component.translatable("gui.tribalpower.codex.tribes_met", CodexUnlocks.tribesUnlocked(), tk.darrow.tribalpower.tribe.TribeDefinition.values().length);
            case "tablets" -> Component.translatable("gui.tribalpower.codex.tablets_read", CodexUnlocks.tabletsUnlocked(), tk.darrow.tribalpower.world.structure.LoreTabletBlock.TABLETS);
            default -> null;
        };
    }

    /** One clickable list row: icon, name, and an optional quiet note beneath. */
    private void row(GuiGraphics g, ItemStack icon, Component name, Component note, int x, int y, Runnable action) {
        g.renderItem(icon, x, y + 1);
        g.drawString(font, font.plainSubstrByWidth(name.getString(), pageWidth - 30), x + 22, y + (note == null ? 5 : 1), PAPER, false);
        if (note != null) g.drawString(font, note, x + 22, y + 11, DIM, false);
        areas.add(new Area(x, y, pageWidth - 8, 20, action));
    }

    private void categoryView(GuiGraphics g) {
        CodexBook.Category c = book().category(category);
        if (c == null) return;
        item(g, stack(c.icon()), leftX, pageY - 2);
        g.drawString(font, c.name(), leftX + 22, pageY + 3, GOLD, false);
        g.fill(leftX, pageY + 16, leftX + pageWidth - 8, pageY + 17, 0x6674DBCB);
        int y = paragraph(g, c.description(), leftX, pageY + 24);
        Component hint = progress(c);
        if (hint != null) g.drawString(font, hint, leftX, y + 4, TEAL, false);
        entryList(g, book().in(category), Component.translatable("gui.tribalpower.codex.entries"));
    }

    private void listView(GuiGraphics g) {
        List<Entry> found;
        Component title;
        if (view == View.BOOKMARKS) {
            found = book().entries().stream().filter(e -> bookmarks.contains(e.id())).toList();
            title = Component.translatable("gui.tribalpower.codex.bookmarks");
        } else {
            String needle = query.toLowerCase(Locale.ROOT);
            found = book().entries().stream().filter(this::visible).filter(e -> haystack(e).contains(needle)).toList();
            title = Component.translatable("gui.tribalpower.codex.search_results", query);
        }
        heading(g, title, leftX, pageY);
        paragraph(g, Component.translatable(found.isEmpty() ? "gui.tribalpower.codex.no_pages" : "gui.tribalpower.codex.found", found.size()).getString(), leftX, pageY + 20);
        entryList(g, found, Component.translatable("gui.tribalpower.codex.entries"));
    }

    private final Map<String, String> haystacks = new HashMap<>();

    private String haystack(Entry e) {
        return haystacks.computeIfAbsent(e.id(), id -> {
            StringBuilder s = new StringBuilder(e.name()).append(' ');
            for (Page p : e.pages()) s.append(p.title()).append(' ').append(CodexText.plain(p.text())).append(' ');
            for (String i : e.items()) s.append(stack(i).getHoverName().getString()).append(' ');
            return s.toString().toLowerCase(Locale.ROOT);
        });
    }

    private void entryList(GuiGraphics g, List<Entry> entries, Component title) {
        heading(g, title, rightX, pageY);
        int per = Math.max(1, (pageHeight - 20) / 20);
        listPage = Math.min(listPage, Math.max(0, (entries.size() - 1) / per));
        int y = pageY + 20;
        for (Entry e : entries.subList(Math.min(entries.size(), listPage * per), Math.min(entries.size(), (listPage + 1) * per))) {
            boolean open = visible(e);
            ItemStack icon = open ? stack(e.icon()) : new ItemStack(net.minecraft.world.item.Items.BOOK);
            Component name = open ? Component.literal(e.name()) : Component.translatable("gui.tribalpower.codex.hidden_entry");
            row(g, icon, name, null, rightX, y, () -> openEntry(e.id()));
            y += 20;
        }
        if (entries.size() > per) {
            Component pages = Component.translatable("gui.tribalpower.codex.page_n", listPage + 1, (entries.size() - 1) / per + 1);
            g.drawString(font, pages, rightX + pageWidth - 8 - font.width(pages), pageY + pageHeight - 6, DIM, false);
        }
    }

    private void entryView(GuiGraphics g, int mx, int my) {
        Entry e = book().byId().get(entry);
        if (e == null) return;
        for (int side = 0; side < 2; side++) {
            int index = spread * 2 + side;
            if (index >= leaves.size()) break;
            leaf(g, e, leaves.get(index), side == 0 ? leftX : rightX, mx, my);
        }
        int total = Math.max(1, (leaves.size() + 1) / 2);
        Component pages = Component.translatable("gui.tribalpower.codex.page_n", spread + 1, total);
        g.drawCenteredString(font, pages, left + bookWidth - 70, top + bookHeight - 25, DIM);
        if (spread == total - 1 && !e.next().isEmpty() && book().byId().containsKey(e.next())) {
            Entry next = book().byId().get(e.next());
            Component label = Component.translatable("gui.tribalpower.codex.next_entry",
                    visible(next) ? Component.literal(next.name()) : Component.translatable("gui.tribalpower.codex.hidden_entry"));
            int x = rightX, y = pageY + pageHeight + 2;
            g.drawString(font, label, x, y, TEAL, false);
            areas.add(new Area(x, y - 2, font.width(label), 12, () -> openEntry(next.id())));
        }
    }

    private void leaf(GuiGraphics g, Entry e, Leaf leaf, int x, int mx, int my) {
        int y = pageY;
        if (leaf.first()) {
            item(g, stack(e.icon()), x, y - 3);
            g.drawString(font, e.name(), x + 20, y + 1, GOLD, false);
            g.fill(x, y + 15, x + pageWidth - 8, y + 16, 0x6674DBCB);
            y += 20;
        }
        Page page = leaf.page();
        if (!page.title().isEmpty() && !leaf.continuation()) {
            g.drawString(font, page.title(), x, y, TEAL, false);
            y += 14;
        }
        if (!leaf.continuation()) y = body(g, page, x, y, mx, my);
        links.addAll(CodexText.draw(g, font, leaf.lines(), x, y));
    }

    private int body(GuiGraphics g, Page page, int x, int y, int mx, int my) {
        switch (page) {
            case CodexBook.Spotlight s -> {
                ItemStack stack = stack(s.item());
                int cx = x + (pageWidth - 8) / 2;
                g.fill(cx - 20, y, cx + 20, y + 40, 0x44000000);
                g.pose().pushPose();
                g.pose().translate(cx - 16, y + 4, 0);
                g.pose().scale(2, 2, 1);
                g.renderItem(stack, 0, 0);
                g.pose().popPose();
                hits.add(new Hit(cx - 16, y + 4, stack));
                Component name = stack.getHoverName();
                g.drawString(font, name, cx - font.width(name) / 2, y + 43, GOLD, false);
                return y + 58;
            }
            case CodexBook.Recipe r -> {
                recipe(g, page, stack(r.item()), x, y);
                return y + 104;
            }
            case CodexBook.Image i -> {
                int size = Math.min(pageWidth - 8, 120);
                g.blit(ResourceLocation.parse("tribalpower:textures/gui/codex/" + i.image() + ".png"), x + (pageWidth - 8 - size) / 2, y, 0, 0, size, size, size, size);
                return y + size + 8;
            }
            case CodexBook.Scene s -> {
                return scene(g, page, s.steps(), x, y, mx, my);
            }
            case CodexBook.Pattern p -> {
                return scene(g, page, patternSteps.computeIfAbsent(page, k -> CodexScene.pattern(p.pattern(), p.tier())), x, y, mx, my);
            }
            case CodexBook.Quests q -> {
                return quests(g, q, x, y);
            }
            default -> {
                return y;
            }
        }
    }

    /** One tribe's open request and story, from the last state the server sent. */
    private int quests(GuiGraphics g, CodexBook.Quests q, int x, int y) {
        tk.darrow.tribalpower.tribe.TribeDefinition tribe = null;
        for (var t : tk.darrow.tribalpower.tribe.TribeDefinition.values()) if (t.id().equals(q.tribe())) tribe = t;
        if (tribe == null) return y;
        var state = tk.darrow.tribalpower.quest.QuestStatePayload.latest.of(tribe);
        int w = pageWidth - 8;
        g.fill(x, y, x + w, y + 92, 0x22000000);
        g.drawString(font, Component.translatable("gui.tribalpower.codex.quests.request"), x + 4, y + 4, GOLD, false);
        var template = state.request().isEmpty() ? null : tk.darrow.tribalpower.quest.Requests.template(tribe, state.request());
        Component request = template == null ? Component.translatable("gui.tribalpower.codex.quests.no_request")
                : template.name().copy().append(": ").append(template.describe());
        int ly = y + 14;
        for (var line : font.split(request, w - 8)) { g.drawString(font, line, x + 4, ly, INK, false); ly += 10; }
        if (template != null && (template.kind() == tk.darrow.tribalpower.quest.Requests.Kind.SLAY || template.kind() == tk.darrow.tribalpower.quest.Requests.Kind.RITE))
            g.drawString(font, Component.translatable("gui.tribalpower.codex.quests.progress", state.requestProgress(), template.count()), x + 4, ly, DIM, false);
        g.drawString(font, Component.translatable("gui.tribalpower.codex.quests.completed", state.completed()), x + 4, y + 40, DIM, false);
        g.drawString(font, Component.translatable("gui.tribalpower.codex.quests.story"), x + 4, y + 54, GOLD, false);
        var step = tk.darrow.tribalpower.quest.Questline.step(tribe, state.step());
        Component story = step == null ? Component.translatable(state.relic() ? "gui.tribalpower.codex.quests.story_done" : "gui.tribalpower.codex.quests.story_done")
                : Component.translatable("gui.tribalpower.codex.quests.step", state.step() + 1, tk.darrow.tribalpower.quest.Questline.STEPS).append(" ").append(step.describe(tribe));
        ly = y + 64;
        for (var line : font.split(story, w - 8)) { if (ly > y + 84) break; g.drawString(font, line, x + 4, ly, INK, false); ly += 10; }
        return y + 96;
    }

    /** A stepped scene with its caption and step controls; returns the y below it. */
    private int scene(GuiGraphics g, Page page, List<CodexBook.Step> steps, int x, int y, int mx, int my) {
        int w = pageWidth - 8;
        g.fill(x, y, x + w, y + SCENE_H, 0x55000000);
        g.renderOutline(x, y, w, SCENE_H, 0xFF2E4A52);
        if (steps.isEmpty()) return y + SCENE_H + 52;
        int step = Math.clamp(sceneStep.getOrDefault(page, 0), 0, steps.size() - 1);
        double now = time();
        double since = now - sceneSince.getOrDefault(page, 0.0);
        float yaw = sceneYaw.getOrDefault(page, 225F) + (dragPage == page ? 0 : (float) (10 * Math.sin(now * 0.35)));
        CodexScene.draw(g, steps, step, x + 1, y + 1, w - 2, SCENE_H - 2, yaw, since, now);
        Area drag = new Area(x, y, w, SCENE_H, () -> {});
        areas.add(new Area(x, y, w, SCENE_H, () -> { dragging = drag; dragPage = page; }));
        int cy = y + SCENE_H + 4;
        List<CodexText.Line> caption = CodexText.layout(font, steps.get(step).caption(), w, linker());
        int ch = 0;
        for (CodexText.Line line : caption) {
            if (ch >= 34) break;
            links.addAll(CodexText.draw(g, font, List.of(line), x, cy + ch));
            ch += 11;
        }
        int controls = y + SCENE_H + 38;
        Component counter = Component.translatable("gui.tribalpower.codex.step_n", step + 1, steps.size());
        int prevX = x, nextX = x + w - 14;
        g.drawString(font, "◀", prevX + 3, controls, step > 0 ? GOLD : 0xFF4A5A60, false);
        g.drawString(font, "▶", nextX + 3, controls, step < steps.size() - 1 ? GOLD : 0xFF4A5A60, false);
        g.drawString(font, counter, x + w / 2 - font.width(counter) / 2, controls, DIM, false);
        areas.add(new Area(prevX, controls - 3, 14, 13, () -> stepScene(page, steps, -1)));
        areas.add(new Area(nextX, controls - 3, 14, 13, () -> stepScene(page, steps, 1)));
        return y + SCENE_H + 52;
    }

    private void stepScene(Page page, List<CodexBook.Step> steps, int direction) {
        int step = Math.clamp(sceneStep.getOrDefault(page, 0) + direction, 0, steps.size() - 1);
        if (step != sceneStep.getOrDefault(page, 0)) sceneSince.put(page, time());
        sceneStep.put(page, step);
    }

    // ------------------------------------------------------------------ recipes

    /** Per (item, uses) lookups; dropped when the level's RecipeManager changes (reload, new world). */
    private final Map<String, List<RecipeHolder<?>>> recipeCache = new HashMap<>();
    private Object recipeCacheOwner;

    private List<RecipeHolder<?>> recipesFor(ItemStack stack, boolean uses) {
        if (minecraft.level == null || stack.isEmpty()) return List.of();
        var manager = minecraft.level.getRecipeManager();
        if (manager != recipeCacheOwner) { recipeCache.clear(); recipeCacheOwner = manager; }
        return recipeCache.computeIfAbsent(BuiltInRegistries.ITEM.getKey(stack.getItem()) + (uses ? "/uses" : "/makes"), k ->
                manager.getRecipes().stream().filter(h -> {
                    ItemStack output = h.value().getResultItem(minecraft.level.registryAccess());
                    return uses ? h.value().getIngredients().stream().anyMatch(i -> i.test(stack))
                            : !output.isEmpty() && output.is(stack.getItem());
                }).sorted(Comparator.comparing(h -> h.id().toString())).toList());
    }

    private void recipe(GuiGraphics g, Page page, ItemStack output, int x, int y) {
        List<RecipeHolder<?>> found = recipesFor(output, false);
        int w = pageWidth - 8;
        g.fill(x, y, x + w, y + 98, 0x44000000);
        if (found.isEmpty()) {
            paragraph(g, Component.translatable(minecraft.level == null ? "gui.tribalpower.codex.recipes_need_world" : "gui.tribalpower.codex.no_recipe").getString(), x + 4, y + 6);
            return;
        }
        int index = Math.floorMod(recipeIndex.getOrDefault(page, 0), found.size());
        drawRecipe(g, found.get(index).value(), x + 6, y + 6, w - 12);
        if (found.size() > 1) {
            Component counter = Component.translatable("gui.tribalpower.codex.recipe_n", index + 1, found.size());
            g.drawString(font, counter, x + w - 6 - font.width(counter), y + 86, DIM, false);
            areas.add(new Area(x, y, w, 98, () -> recipeIndex.put(page, recipeIndex.getOrDefault(page, 0) + 1)));
        }
    }

    /** One recipe: its grid, an arrow and the result, and what station and voice a lattice recipe needs. */
    private void drawRecipe(GuiGraphics g, Recipe<?> recipe, int x, int y, int w) {
        int columns = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
        int index = 0;
        for (Ingredient ingredient : recipe.getIngredients()) {
            ItemStack[] options = ingredient.getItems();
            int cx = x + (index % columns) * 18, cy = y + (index / columns) * 18;
            g.fill(cx - 1, cy - 1, cx + 17, cy + 17, 0xFF23373F);
            if (options.length > 0) item(g, options[(int) (System.currentTimeMillis() / 1400 % options.length)], cx, cy);
            index++;
        }
        int resultX = x + Math.max(3, columns) * 18 + 22;
        g.drawString(font, "→", resultX - 16, y + 23, GOLD, false);
        g.fill(resultX - 3, y + 15, resultX + 19, y + 37, 0xFF2E4A52);
        item(g, recipe.getResultItem(minecraft.level.registryAccess()), resultX, y + 18);
        Component kind;
        if (recipe instanceof LatticeRecipe lattice) {
            kind = Component.translatable("gui.tribalpower.codex.lattice_kind",
                    Component.translatable("block.tribalpower." + lattice.station()),
                    Component.translatable("attunement.tribalpower." + lattice.attunement().getSerializedName()));
            g.drawString(font, Component.translatable("gui.tribalpower.codex.lattice_base", lattice.seconds(), lattice.pulse(), lattice.seconds() * lattice.pulse()), x, y + 70, DIM, false);
        } else if (recipe instanceof ShapedRecipe) kind = Component.translatable("gui.tribalpower.codex.shaped");
        else if (recipe instanceof ShapelessRecipe) kind = Component.translatable("gui.tribalpower.codex.shapeless");
        else kind = Component.literal(String.valueOf(BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType())));
        for (FormattedCharSequence line : font.split(kind, w - (resultX - x) - 24))
            g.drawString(font, line, resultX + 24, y + 18, TEAL, false);
    }

    private void itemView(GuiGraphics g) {
        ItemStack focus = stack(item);
        item(g, focus, leftX, pageY - 2);
        g.drawString(font, focus.getHoverName(), leftX + 22, pageY + 3, GOLD, false);
        g.fill(leftX, pageY + 16, leftX + pageWidth - 8, pageY + 17, 0x6674DBCB);
        int y = paragraph(g, Component.translatable(CodexJeiLinks.available() ? "gui.tribalpower.codex.recipe_intro_jei" : "gui.tribalpower.codex.recipe_intro").getString(), leftX, pageY + 24);
        Component toggle = Component.translatable(showUses ? "gui.tribalpower.codex.show_recipes" : "gui.tribalpower.codex.show_uses");
        g.drawString(font, toggle, leftX, y + 6, TEAL, false);
        areas.add(new Area(leftX, y + 4, font.width(toggle), 12, () -> { showUses = !showUses; recipePage = 0; }));
        List<RecipeHolder<?>> found = recipesFor(focus, showUses);
        heading(g, Component.translatable(showUses ? "gui.tribalpower.codex.item_uses" : "gui.tribalpower.codex.item_recipes", focus.getHoverName()), rightX, pageY);
        if (found.isEmpty()) {
            paragraph(g, Component.translatable(showUses ? "gui.tribalpower.codex.no_uses" : "gui.tribalpower.codex.no_recipe").getString(), rightX, pageY + 20);
            return;
        }
        int index = Math.floorMod(recipePage, found.size());
        g.drawString(font, Component.translatable("gui.tribalpower.codex.recipe_n", index + 1, found.size()), rightX, pageY + 20, DIM, false);
        drawRecipe(g, found.get(index).value(), rightX + 4, pageY + 36, pageWidth - 12);
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        for (Hit h : hits)
            if (x >= h.x && x < h.x + 16 && y >= h.y && y < h.y + 16) {
                if (button == 1 && CodexJeiLinks.available()) openJei(h.item, false);
                else if (button == 0) openItem(h.item);
                return true;
            }
        if (button == 0) {
            for (CodexText.Link link : links)
                if (x >= link.x() && x < link.x() + link.width() && y >= link.y() - 1 && y < link.y() + 10) {
                    openEntry(link.entry());
                    return true;
                }
            for (Area area : List.copyOf(areas))
                if (area.inside(x, y)) {
                    area.action().run();
                    if (dragging == null) return true;
                }
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (dragging != null && dragPage != null) {
            sceneYaw.put(dragPage, sceneYaw.getOrDefault(dragPage, 225F) + (float) dx * 1.5F);
            return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        dragging = null;
        dragPage = null;
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        turn(vertical < 0 ? 1 : -1);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (!search.isFocused()) {
            if (key == 262 || key == 267) { turn(1); return true; }
            if (key == 263 || key == 266) { turn(-1); return true; }
            if (key == 259) { back(); return true; }
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ JEI and verification hooks

    public int bookLeft() { return left; }
    public int bookTop() { return top; }
    public int bookWidth() { return bookWidth; }
    public int bookHeight() { return bookHeight; }

    public record ItemHover(ItemStack item, int x, int y) {}

    public ItemHover itemHover(double x, double y) {
        for (Hit h : hits) if (x >= h.x && x < h.x + 16 && y >= h.y && y < h.y + 16) return new ItemHover(h.item, h.x, h.y);
        return null;
    }

    /** Showcase driver hook ({@link ShowcaseVerification}): reveal spoilers and jump straight to an entry or item. */
    void showcaseOpen(String id) {
        if (!Boolean.getBoolean("tribalpower.showcaseVerification")) return;
        spoilers = true;
        if (id.startsWith("item:")) go(new State(View.ITEM, "", "", 0, id.substring(5)), true);
        else openEntry(id);
    }

    void verificationStage(int stage) {
        if (!Boolean.getBoolean("tribalpower.codexVerification")) return;
        switch (stage) {
            case 1 -> openEntry("drumheart");
            case 2 -> confirmSpoilers(() -> openEntry("dawn_stag"));
            case 4 -> openEntry("lantern_fox");
            case 5 -> { spoilers = false; history.clear(); go(new State(View.LANDING, "", "", 0, ""), false); }
            case 6 -> { spoilers = true; go(new State(View.ITEM, "", "", 0, "tribalpower:drumheart"), true); }
            case 7 -> openEntry("pulse_resonator");
            case 8 -> turn(1);
            case 9 -> openEntry("summoning_cradle");
            case 10 -> go(new State(View.CATEGORY, "power", "", 0, ""), true);
            default -> { }
        }
    }
}
