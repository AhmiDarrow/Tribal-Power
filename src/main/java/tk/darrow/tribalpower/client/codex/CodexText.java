package tk.darrow.tribalpower.client.codex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/**
 * Codex page text. Paragraphs are split by blank lines; a line starting "- " is a bullet. Inline:
 * {@code **words**} highlights, {@code [words](entry_id)} links to another entry, and {@code {item:ns:id}}
 * prints that item's name in the reader's language. With a {@link Linker}, any mention of another entry, or of
 * an item an entry covers, becomes a link by itself.
 */
public final class CodexText {
    public static final int INK = 0xFFE4E5DA, HIGHLIGHT = 0xFFE4C18A, LINK = 0xFF74DBCB, BULLET = 0xFF8FB8B2;
    private static final Pattern TOKEN = Pattern.compile("\\*\\*(.+?)\\*\\*|\\[(.+?)\\]\\(([a-z0-9_]+)\\)|\\{item:([a-z0-9_.:/-]+)\\}");
    private static final int LINE = 11;

    public record Span(String text, int color, String link) {}

    /** One laid-out line: spans with x offsets. */
    public record Line(List<Placed> spans, boolean paragraphEnd) {}

    public record Placed(int x, Span span) {}

    public record Link(int x, int y, int width, String entry) {}

    private CodexText() {}

    /**
     * Finds mentions of other entries in plain text: every entry's name, and the name of every item an entry
     * covers. The longest name wins, whole words only, and an entry never links to itself.
     */
    public record Linker(Map<String, String> names, Pattern pattern, String self) {
        public static final Linker NONE = new Linker(Map.of(), null, "");

        public static Linker of(CodexBook.Book book) {
            Map<String, String> names = new HashMap<>();
            for (CodexBook.Entry e : book.entries()) {
                for (String item : e.items()) {
                    var key = CodexBook.itemId(item);
                    if (BuiltInRegistries.ITEM.containsKey(key)) {
                        String name = BuiltInRegistries.ITEM.get(key).getDescription().getString();
                        if (name.length() >= 4) names.putIfAbsent(name, book.itemEntries().getOrDefault(item, e.id()));
                    }
                }
            }
            for (CodexBook.Entry e : book.entries()) if (e.name().length() >= 4) names.put(e.name(), e.id());
            if (names.isEmpty()) return NONE;
            String alternatives = names.keySet().stream().sorted(Comparator.comparingInt(String::length).reversed())
                    .map(Pattern::quote).collect(Collectors.joining("|"));
            return new Linker(Map.copyOf(names), Pattern.compile("(?<![\\w'])(" + alternatives + ")(?![\\w'])"), "");
        }

        public Linker from(String entry) {
            return new Linker(names, pattern, entry);
        }

        String target(String name) {
            String id = names.get(name);
            return id == null || id.equals(self) ? null : id;
        }

        String item(String itemId) {
            if (pattern == null) return null;
            var key = CodexBook.itemId(itemId);
            return BuiltInRegistries.ITEM.containsKey(key) ? target(BuiltInRegistries.ITEM.get(key).getDescription().getString()) : null;
        }
    }

    /** Plain words for search: markup removed, item ids resolved. */
    public static String plain(String text) {
        StringBuilder out = new StringBuilder();
        for (List<Span> paragraph : parse(text, Linker.NONE)) {
            for (Span span : paragraph) out.append(span.text());
            out.append(' ');
        }
        return out.toString();
    }

    public static List<Line> layout(Font font, String text, int width) {
        return layout(font, text, width, Linker.NONE);
    }

    public static List<Line> layout(Font font, String text, int width, Linker linker) {
        List<Line> lines = new ArrayList<>();
        for (List<Span> paragraph : parse(text, linker)) {
            boolean bullet = !paragraph.isEmpty() && paragraph.getFirst().text().startsWith("- ");
            int indent = bullet ? 8 : 0;
            List<Placed> current = new ArrayList<>();
            if (bullet) current.add(new Placed(0, new Span("•", BULLET, null)));
            int x = indent;
            boolean first = true;
            for (Span span : paragraph) {
                String text2 = span.text();
                if (first && bullet) text2 = text2.substring(2);
                first = false;
                for (String word : words(text2)) {
                    int w = font.width(word);
                    if (x + w > width && x > indent && !word.isBlank()) {
                        lines.add(new Line(current, false));
                        current = new ArrayList<>();
                        x = indent;
                        if (word.isBlank()) continue;
                    }
                    if (x == indent && word.isBlank()) continue;
                    current.add(new Placed(x, new Span(word, span.color(), span.link())));
                    x += w;
                }
            }
            lines.add(new Line(current, true));
        }
        return lines;
    }

    /** Draws lines and returns the links drawn, in screen coordinates. */
    public static List<Link> draw(GuiGraphics g, Font font, List<Line> lines, int x, int y) {
        List<Link> links = new ArrayList<>();
        for (Line line : lines) {
            for (Placed placed : line.spans()) {
                Span span = placed.span();
                Component text = Component.literal(span.text()).withStyle(Style.EMPTY.withUnderlined(span.link() != null && !span.text().isBlank()));
                g.drawString(font, text, x + placed.x(), y, span.color(), false);
                if (span.link() != null) links.add(new Link(x + placed.x(), y, font.width(span.text()), span.link()));
            }
            y += LINE + (line.paragraphEnd() ? 4 : 0);
        }
        return links;
    }

    public static int height(List<Line> lines) {
        int h = 0;
        for (Line line : lines) h += LINE + (line.paragraphEnd() ? 4 : 0);
        return h;
    }

    public static int lineHeight(Line line) {
        return LINE + (line.paragraphEnd() ? 4 : 0);
    }

    private static List<String> words(String text) {
        List<String> out = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c == ' ') {
                if (!word.isEmpty()) out.add(word.toString());
                out.add(" ");
                word.setLength(0);
            } else word.append(c);
        }
        if (!word.isEmpty()) out.add(word.toString());
        return out;
    }

    private static List<List<Span>> parse(String text, Linker linker) {
        List<List<Span>> paragraphs = new ArrayList<>();
        for (String paragraph : text.split("\n\\s*\n|\n(?=- )")) {
            paragraph = paragraph.replace('\n', ' ').strip();
            if (paragraph.isEmpty()) continue;
            List<Span> spans = new ArrayList<>();
            Matcher m = TOKEN.matcher(paragraph);
            int at = 0;
            while (m.find()) {
                if (m.start() > at) mentions(spans, paragraph.substring(at, m.start()), INK, linker);
                if (m.group(1) != null) mentions(spans, m.group(1), HIGHLIGHT, linker);
                else if (m.group(2) != null) spans.add(new Span(m.group(2), LINK, m.group(3)));
                else {
                    String target = linker.item(m.group(4));
                    spans.add(new Span(itemName(m.group(4)), target == null ? HIGHLIGHT : LINK, target));
                }
                at = m.end();
            }
            if (at < paragraph.length()) mentions(spans, paragraph.substring(at), INK, linker);
            paragraphs.add(spans);
        }
        return paragraphs;
    }

    /** Adds text, turning any mention of another entry into a link. */
    private static void mentions(List<Span> spans, String text, int colour, Linker linker) {
        if (linker.pattern() == null) {
            spans.add(new Span(text, colour, null));
            return;
        }
        Matcher m = linker.pattern().matcher(text);
        int at = 0;
        while (m.find()) {
            String target = linker.target(m.group(1));
            if (target == null) continue;
            if (m.start() > at) spans.add(new Span(text.substring(at, m.start()), colour, null));
            spans.add(new Span(m.group(1), LINK, target));
            at = m.end();
        }
        if (at < text.length()) spans.add(new Span(text.substring(at), colour, null));
    }

    private static String itemName(String id) {
        var key = CodexBook.itemId(id);
        return BuiltInRegistries.ITEM.containsKey(key) ? BuiltInRegistries.ITEM.get(key).getDescription().getString() : id;
    }
}
