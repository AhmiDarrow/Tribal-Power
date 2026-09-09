package tk.darrow.tribalpower.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import tk.darrow.tribalpower.guide.CodexEntries.Entry;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.tribe.TribeRegistry;

import java.util.function.BiConsumer;

/**
 * Animated flow diagrams for the Spirit Codex, drawn only with fills, items and strings.
 *
 * <p>Every teaching maps to one {@link Flow} (label, end items, moving item, optional middle icon, accent colour and a
 * {@link Kind} that picks the animation) through {@link #flow(Entry, double)}: exact ids first, then id prefixes,
 * then categories. The caller passes the animation time in seconds ({@code 0} when Motion is off).
 */
final class CodexDiagrams {
    static final int HEIGHT = 72;
    private static final int INK = 0xFF0C1922, TEAL = 0xFF74DBCB, GOLD = 0xFFE4C18A, PAPER = 0xFFE4E5DA, RAIL = 0xFF36555B, DIM = 0xFF29484F;
    private static final String[] TRIBE_REAGENTS = {"echo_shard", "attuned_echo", "mossback_scale", "rift_tooth", "bone_chime", "storm_wing", "lantern_down", "spirit_shard", "loom_thread"};
    private static final String[] RITES = {"rite_rain_calling", "rite_sky_clearing", "rite_dawn_calling", "rite_green_blessing", "rite_still_night", "rite_ley_binding"};
    private static final String[] RITE_SEALS = {"water_seal", "air_seal", "fire_seal", "earth_seal", "spirit_seal", "loom_seal"};
    private static final String[] FAMILIARS = {"lantern_fox", "mossback", "dawn_stag"};
    private static final String[] STATUS = {"PAUSED", "NO VOICE", "OUTPUT FULL", "HUMMING"};
    private static final int[] STATUS_COLOUR = {0xFFE07A5F, 0xFFE4C18A, 0xFFE4C18A, 0xFF74DBCB};

    enum Kind { FLOW, BEATS, TABLET, CHAIN, RITE, FAMILIAR, CAMP, LENS, LOGIC, DIAGNOSE }

    /** One diagram: {@code middle} may be empty; {@code moving} changes shape mid-line for the CHAIN and workshop flows. */
    record Flow(Kind kind, String label, ItemStack first, ItemStack last, ItemStack moving, ItemStack middle, int accent, int variant) {}

    private CodexDiagrams() {}

    static ItemStack stack(String id) {
        var key = ResourceLocation.tryParse(id.contains(":") ? id : "tribalpower:" + id);
        return key == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(key));
    }

    private static Flow flow(Kind kind, String label, String first, String last, String moving, String middle, int accent, int variant) {
        return new Flow(kind, label, stack(first), stack(last), stack(moving), middle.isEmpty() ? ItemStack.EMPTY : stack(middle), accent, variant);
    }

    private static Flow tribe(TribeDefinition tribe) {
        String reagent = TRIBE_REAGENTS[tribe.ordinal()];
        return new Flow(Kind.FLOW, "OFFER > STAND > TRADE", stack(reagent), tribe.stamped(TribeRegistry.TRIBE_MARK.get()), stack(reagent), stack("tribe_hearth"), 0xFF000000 | tribe.colour(), tribe.ordinal());
    }

    /** The flow for a teaching at animation time {@code t} (seconds); cycling flows pick their variant from {@code t}. */
    static Flow flow(Entry e, double t) {
        String id = e.id();
        TribeDefinition cycling = TribeDefinition.byOrdinal((int) (t / 3) % TribeDefinition.values().length);
        int rite = (int) (t / 3) % RITES.length, familiar = (int) (t / 3) % FAMILIARS.length;
        Flow exact = switch (id) {
            case "tribes_kinship_totem" -> new Flow(Kind.FLOW, "MARK > BIND > VOICE", cycling.stamped(TribeRegistry.TRIBE_MARK.get()), cycling.stamped(TribeRegistry.KINSHIP_TOTEM_ITEM.get()), stack("spiritweave"), ItemStack.EMPTY, 0xFF000000 | cycling.colour(), cycling.ordinal());
            case "tribes_standing", "tribes_kin", "walk_first_camp" -> tribe(cycling);
            case "march_the_unsung", "walk_wake_the_unsung" -> flow(Kind.BEATS, "BEAT x4 > WAKE > RESYNC", "bone_chime", "unsung_heart", "silent_drum", "", 0xFFE07A5F, 0);
            case "march_drum_circle" -> flow(Kind.BEATS, "BEAT x4 > WAKE > RESYNC", "bone_chime", "silent_drum", "silent_drum", "", 0xFFE07A5F, 0);
            case "march_ancestor_hall", "march_crystal_spire" -> flow(Kind.FLOW, "FIND > READ > KEEP", "lore_tablet", "spirit_codex", "loom_thread", "", GOLD, 0);
            case "loom_sixth_voice", "walk_sixth_voice" -> flow(Kind.CHAIN, "WEAVE > UNWEAVE", "echo_shard", "manifested_ingot", "manifested_ingot", "echo_unweave", 0xFF62D1C9, 0);
            case "rite_world_rites", "rite_land_rites", "walk_first_rite" -> flow(Kind.RITE, "SEAL > RITE > CHANGE", RITE_SEALS[rite], RITES[rite], RITES[rite], "ritual_brazier", GOLD, rite);
            case "familiars_bonding", "walk_bond_a_familiar" -> flow(Kind.FAMILIAR, "CHARM > BOND > FOLLOW", "bonding_charm", FAMILIARS[familiar] + "_spawn_egg", "bonding_charm", "", 0xFFE07A9F, familiar);
            case "familiars_fox" -> flow(Kind.FAMILIAR, "CHARM > BOND > FOLLOW", "bonding_charm", "lantern_fox_spawn_egg", "bonding_charm", "", 0xFFE07A9F, 0);
            case "familiars_mossback_stag" -> flow(Kind.FAMILIAR, "CHARM > BOND > FOLLOW", "bonding_charm", FAMILIARS[1 + (int) (t / 3) % 2] + "_spawn_egg", "bonding_charm", "", 0xFFE07A9F, 1 + (int) (t / 3) % 2);
            case "camps_identity", "walk_found_a_camp" -> flow(Kind.CAMP, "CHARTER > INVITE > SHARE", "camp_charter", "deep_cache", "wayfarer_satchel", "", TEAL, 0);
            case "ley_lens" -> flow(Kind.LENS, "HOLD > READ > PLACE", "ley_lens", "ley_collector", "ley_lens", "", GOLD, 0);
            case "pulse_logic" -> flow(Kind.LOGIC, "STORE > MEASURE > SWITCH", "drumheart", "minecraft:redstone_lamp", "pulse_gauge", "pulse_threshold", 0xFFE07A5F, 0);
            case "codex_diagnostics" -> flow(Kind.DIAGNOSE, "SNEAK-USE > LISTEN > MEND", "spirit_codex", "echo_shatter", "spirit_codex", "", TEAL, 0);
            case "camp_binding_effigy" -> flow(Kind.FLOW, "IMPRINT > AWAKEN > RENEW", "binding_effigy", "pulse_cell", "spiritweave", "", TEAL, 0);
            case "camp_binding_ritual" -> flow(Kind.FLOW, "IMPRINT > AWAKEN > RENEW", "ritual_brazier", "binding_effigy", "spiritweave", "", TEAL, 0);
            case "camp_summoning_cradle" -> flow(Kind.FLOW, "BIND > SUMMON > RENEW", "binding_effigy", "summoning_cradle", "spiritweave", "", TEAL, 0);
            case "camp_grove_tender" -> flow(Kind.FLOW, "PLANT > GROW > HARVEST", "minecraft:wheat_seeds", "minecraft:wheat", "minecraft:wheat_seeds", "", 0xFF7BC96F, 1);
            case "camp_wayanchor" -> flow(Kind.FLOW, "SUPPLY > SUSTAIN > RELEASE", "wayanchor", "pulse_cell", "wayanchor", "", TEAL, 0);
            case "camp_hush_totem" -> flow(Kind.FLOW, "SUPPLY > WARD > REST", "hush_totem", "pulse_cell", "hush_totem", "", TEAL, 0);
            default -> null;
        };
        if (exact != null) return exact;
        if (id.startsWith("tribe_")) return tribe(TribeDefinition.byId(id.substring(6)));
        if (id.startsWith("tablet_")) return flow(Kind.TABLET, "READ > REMEMBER", "lore_tablet", "spirit_codex", "lore_tablet", "", GOLD, 0);
        String category = e.category();
        if (category.contains("transport")) return flow(Kind.FLOW, "LINK > TRANSFER > REST", "ancestral_cache", "ancestral_cache", "minecraft:wheat", "", TEAL, 0);
        if (category.equals("Workshops")) return flow(Kind.FLOW, "ATTUNE > PROCESS > COLLECT", "minecraft:stone", "echo_shard", "minecraft:stone", "", TEAL, 2);
        if (category.contains("rites")) return flow(Kind.FLOW, "CHARGE > CAST > RECOVER", "pulse_cell", e.icon(), "spirit_shard", "", TEAL, 0);
        if (category.equals("Camp stewardship")) return flow(Kind.FLOW, "PLACE > CONNECT > ENJOY", e.icon(), "pulse_cell", e.icon(), "", TEAL, 0);
        return flow(Kind.FLOW, "BEAT > GATHER > STORE", "drumheart", "pulse_cell", "spirit_shard", "", TEAL, 0);
    }

    /**
     * Draws the diagram box for {@code e} at ({@code x},{@code y}) spanning {@code w} pixels; {@code item} renders an
     * item stack at a position (so the screen can register tooltips). Returns the box height.
     */
    static int draw(GuiGraphics g, Font font, Entry e, int x, int y, int w, double t, BiConsumer<ItemStack, int[]> item) {
        Flow f = flow(e, t);
        g.fill(x, y, x + w, y + HEIGHT, INK);
        g.fill(x, y, x + 3, y + HEIGHT, f.accent());
        int bx = x + 8, span = w - 16;
        g.drawString(font, font.plainSubstrByWidth(f.label(), span - 4), bx, y + 6, TEAL, false);
        int lineStart = bx + 24, lineEnd = bx + span - 22, railY = y + 37;
        switch (f.kind()) {
            case BEATS -> beats(g, font, f, bx, y, span, lineStart, lineEnd, railY, t);
            case TABLET -> tablet(g, bx, y, span, lineStart, lineEnd, t);
            case CHAIN -> chain(g, font, f, bx, y, span, lineStart, lineEnd, railY, t, item);
            case RITE -> rite(g, f, bx, y, span, lineStart, lineEnd, railY, t, item);
            case FAMILIAR -> familiar(g, f, bx, y, span, lineStart, lineEnd, railY, t);
            case CAMP -> camp(g, f, bx, y, span, lineStart, lineEnd, railY, t);
            case LENS -> lens(g, f, bx, y, span, lineStart, lineEnd, t);
            case LOGIC -> logic(g, font, f, bx, y, span, lineStart, lineEnd, railY, t, item);
            case DIAGNOSE -> diagnose(g, font, f, bx, y, span, lineStart, lineEnd, railY, t);
            default -> plain(g, f, bx, y, span, lineStart, lineEnd, railY, t, item);
        }
        item.accept(f.first(), new int[]{bx, y + 29});
        item.accept(f.last(), new int[]{bx + span - 16, y + 29});
        return HEIGHT;
    }

    private static void rail(GuiGraphics g, int lineStart, int lineEnd, int railY, double t, int colour) {
        g.fill(lineStart, railY, lineEnd, railY + 1, RAIL);
        for (int n = 0; n < 4; n++) {
            double phase = (t / 6 + n / 4.0) % 1;
            int dx = lineStart + (int) ((lineEnd - lineStart) * phase), dy = railY - 1 + (int) (Math.sin(t * 1.5 + n) * 3);
            g.fill(dx, dy, dx + 3, dy + 3, colour);
        }
    }

    private static void meter(GuiGraphics g, int bx, int y, int span, double progress, int colour) {
        int meterY = y + 55;
        g.fill(bx, meterY, bx + span, meterY + 3, DIM);
        g.fill(bx, meterY, bx + (int) (span * Math.clamp(progress, 0, 1)), meterY + 3, colour);
    }

    private static int lerp(int a, int b, double f) {
        f = Math.clamp(f, 0, 1);
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * f), gg = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * f), bb = (int) ((a & 255) + ((b & 255) - (a & 255)) * f);
        return 0xFF000000 | r << 16 | gg << 8 | bb;
    }

    private static void plain(GuiGraphics g, Flow f, int bx, int y, int span, int lineStart, int lineEnd, int railY, double t, BiConsumer<ItemStack, int[]> item) {
        rail(g, lineStart, lineEnd, railY, t, f.accent());
        if (!f.middle().isEmpty()) item.accept(f.middle(), new int[]{bx + span / 2 - 8, y + 29});
        double progress = (t / 6) % 1;
        int movingX = lineStart + (int) ((lineEnd - lineStart - 16) * progress);
        ItemStack moving = f.moving();
        if (f.variant() == 1 && progress > 0.6) moving = stack("minecraft:wheat");   // grove tender ripens
        if (f.variant() == 2 && progress > 0.6) moving = stack("echo_shard");        // workshops shatter stone
        g.renderItem(moving, movingX, y + 25);
        meter(g, bx, y, span, progress, f.accent());
    }

    /** Four drum beats one second apart, then a boss-bar meter draining through Beat, Chorus and Silence. */
    private static void beats(GuiGraphics g, Font font, Flow f, int bx, int y, int span, int lineStart, int lineEnd, int railY, double t) {
        g.fill(lineStart, railY, lineEnd, railY + 1, RAIL);
        double cycle = t % 16;                      // 0-4 s beats, 4-16 s the boss bar drains
        for (int n = 0; n < 4; n++) {
            int cx = lineStart + (lineEnd - lineStart) * (n + 1) / 5;
            double since = cycle - n;
            int r = since >= 0 && since < 0.35 ? 5 : 2;
            int colour = since >= 0 && since < 1 ? f.accent() : (cycle >= 4 ? GOLD : DIM);
            g.fill(cx - r, railY - r, cx + r + 1, railY + r + 1, colour);
        }
        double drain = cycle < 4 ? 1 : 1 - (cycle - 4) / 12;
        int meterY = y + 52, third = span / 3;
        int[] bands = {0xFF7A2E2E, 0xFF4A2E7A, 0xFF1C3A44};
        for (int b = 0; b < 3; b++) g.fill(bx + b * third, meterY, b == 2 ? bx + span : bx + (b + 1) * third, meterY + 5, bands[b]);
        int fillX = bx + (int) (span * drain);
        boolean silence = drain < 1 / 3.0, blink = ((int) (t * 4)) % 2 == 0;
        if (!silence || blink) g.fill(bx, meterY, fillX, meterY + 5, drain > 2 / 3.0 ? 0xFFE07A5F : drain > 1 / 3.0 ? 0xFFB07AE0 : TEAL);
        g.fill(bx, meterY + 5, bx + span, meterY + 6, 0xFF0A1218);
        String[] names = {"Beat", "Chorus", "Silence"};
        for (int b = 0; b < 3; b++) g.drawString(font, names[b], bx + b * third + 2, meterY + 8, b == 2 && silence && blink ? TEAL : 0xFF9CB8B9, false);
        if (cycle >= 4 && cycle < 4.6) g.drawString(font, "WAKE", lineEnd - 24 - font.width("WAKE"), y + 18, f.accent(), false);
    }

    /** A tablet at the left and three ink lines that fill slowly, like reading etched text. */
    private static void tablet(GuiGraphics g, int bx, int y, int span, int lineStart, int lineEnd, double t) {
        double progress = (t / 8) % 1;
        int width = lineEnd - lineStart;
        for (int row = 0; row < 3; row++) {
            int ly = y + 27 + row * 6, rowWidth = width - (row == 2 ? width / 3 : 0);
            g.fill(lineStart, ly, lineStart + rowWidth, ly + 2, DIM);
            double rowProgress = Math.clamp(progress * 3 - row, 0, 1);
            int inkEnd = lineStart + (int) (rowWidth * rowProgress);
            g.fill(lineStart, ly, inkEnd, ly + 2, GOLD);
            if (rowProgress > 0 && rowProgress < 1) g.fill(inkEnd, ly - 1, inkEnd + 2, ly + 3, PAPER);
        }
        meter(g, bx, y, span, progress, GOLD);
    }

    /** Echo Unweave runs the lattice backwards: the moving item travels right to left and loses a stage at each stop. */
    private static void chain(GuiGraphics g, Font font, Flow f, int bx, int y, int span, int lineStart, int lineEnd, int railY, double t, BiConsumer<ItemStack, int[]> item) {
        g.fill(lineStart, railY, lineEnd, railY + 1, RAIL);
        String[] stages = {"echo_shard", "attuned_echo", "bound_echo", "manifested_ingot"};
        int width = lineEnd - lineStart;
        item.accept(stack(stages[1]), new int[]{lineStart + width / 3 - 8, y + 29});
        item.accept(stack(stages[2]), new int[]{lineStart + width * 2 / 3 - 8, y + 29});
        if (!f.middle().isEmpty()) item.accept(f.middle(), new int[]{Math.max(bx + font.width(f.label()) + 8, bx + span / 2 - 8), y + 6});
        for (int n = 0; n < 4; n++) {
            double phase = 1 - (t / 6 + n / 4.0) % 1;
            int dx = lineStart + (int) (width * phase), dy = railY - 1 + (int) (Math.sin(t * 1.5 + n) * 3);
            g.fill(dx, dy, dx + 3, dy + 3, f.accent());
        }
        double progress = (t / 8) % 1;
        int stage = 3 - Math.min(3, (int) (progress * 4));
        int movingX = lineEnd - 16 - (int) ((width - 16) * progress);
        g.renderItem(stack(stages[stage]), movingX, y + 22);
        meter(g, bx, y, span, progress, f.accent());
    }

    /** Seal into brazier, tablet along the line, and the rite's effect drawn as a glyph at the right. */
    private static void rite(GuiGraphics g, Flow f, int bx, int y, int span, int lineStart, int lineEnd, int railY, double t, BiConsumer<ItemStack, int[]> item) {
        rail(g, lineStart, lineEnd, railY, t, f.accent());
        item.accept(f.middle(), new int[]{bx + span / 2 - 8, y + 29});
        double progress = (t / 3) % 1;
        int movingX = lineStart + (int) ((lineEnd - lineStart - 16) * Math.min(1, progress * 1.4));
        g.renderItem(f.moving(), movingX, y + 25);
        int gx = bx + span - 54, gy = y + 8;   // glyph panel above the rail, left of the tablet
        g.fill(gx, gy, gx + 28, gy + 20, 0xFF16303A);
        glyph(g, f.variant(), gx, gy, t);
        meter(g, bx, y, span, progress, f.accent());
    }

    private static void glyph(GuiGraphics g, int rite, int gx, int gy, double t) {
        switch (rite) {
            case 0 -> { // rain streaks
                g.fill(gx + 6, gy + 3, gx + 22, gy + 8, 0xFF9CB8B9);
                for (int n = 0; n < 5; n++) { int sy = gy + 9 + (int) ((t * 14 + n * 3) % 9); g.fill(gx + 5 + n * 4, sy, gx + 6 + n * 4, sy + 3, 0xFF74B8FF); }
            }
            case 1 -> { // sky clearing: sun with rays, clouds parting
                g.fill(gx + 11, gy + 6, gx + 17, gy + 12, GOLD);
                for (int n = 0; n < 4; n++) if ((int) (t * 3 + n) % 2 == 0) { g.fill(gx + 13, gy + 2, gx + 15, gy + 4, GOLD); g.fill(gx + 13, gy + 14, gx + 15, gy + 16, GOLD); g.fill(gx + 7, gy + 8, gx + 9, gy + 10, GOLD); g.fill(gx + 19, gy + 8, gx + 21, gy + 10, GOLD); }
                int part = (int) ((t * 4) % 6);
                g.fill(gx + 1, gy + 14, gx + 6 - part / 2, gy + 18, 0xFF9CB8B9); g.fill(gx + 22 + part / 2, gy + 14, gx + 27, gy + 18, 0xFF9CB8B9);
            }
            case 2 -> { // dawn: a sun rising over the horizon line
                int rise = (int) (Math.min(1, (t % 3) / 2.4) * 8);
                g.fill(gx + 10, gy + 16 - rise, gx + 18, gy + 16, GOLD);
                g.fill(gx + 2, gy + 16, gx + 26, gy + 18, 0xFF6C5A3C);
                g.fill(gx + 2, gy + 2, gx + 26, gy + 16 - rise - 1, lerp(0xFF16303A, 0xFF2E4C5C, rise / 8.0));
                g.fill(gx + 10, gy + 16 - rise, gx + 18, gy + 16, GOLD);
            }
            case 3 -> { // green blessing sparkles over a sprout
                g.fill(gx + 13, gy + 8, gx + 15, gy + 18, 0xFF4F9A5A); g.fill(gx + 9, gy + 9, gx + 13, gy + 11, 0xFF7BC96F); g.fill(gx + 15, gy + 6, gx + 19, gy + 8, 0xFF7BC96F);
                for (int n = 0; n < 6; n++) { double p = (t * 0.8 + n * 0.37) % 1; int sx = gx + 3 + (n * 7) % 22, sy = gy + 17 - (int) (p * 15); if (((int) (t * 6 + n)) % 3 != 0) g.fill(sx, sy, sx + 2, sy + 2, 0xFFA8F0A0); }
            }
            case 4 -> { // still night: moon and slow stars, no spawns
                g.fill(gx + 17, gy + 4, gx + 24, gy + 11, PAPER); g.fill(gx + 15, gy + 3, gx + 21, gy + 9, 0xFF16303A);
                for (int n = 0; n < 4; n++) if ((int) (t * 2 + n * 1.3) % 3 != 0) g.fill(gx + 4 + n * 5, gy + 5 + (n * 3) % 8, gx + 5 + n * 5, gy + 6 + (n * 3) % 8, PAPER);
                g.fill(gx + 3, gy + 15, gx + 25, gy + 17, 0xFF3A4A52);
            }
            default -> { // ley binding: a humming thread between two totems
                g.fill(gx + 3, gy + 4, gx + 7, gy + 17, 0xFF62D1C9); g.fill(gx + 21, gy + 4, gx + 25, gy + 17, 0xFF62D1C9);
                for (int px = gx + 7; px < gx + 21; px++) { int ty = gy + 10 + (int) (Math.sin(t * 6 + px * 0.9) * 2); g.fill(px, ty, px + 1, ty + 1, TEAL); }
            }
        }
    }

    /** Charm moves to the animal, a heart pulses at the bond, then each species shows its trick. */
    private static void familiar(GuiGraphics g, Flow f, int bx, int y, int span, int lineStart, int lineEnd, int railY, double t) {
        rail(g, lineStart, lineEnd, railY, t, f.accent());
        double progress = (t / 3) % 1;
        int movingX = lineStart + (int) ((lineEnd - lineStart - 16) * Math.min(1, progress * 1.6));
        g.renderItem(f.moving(), movingX, y + 25);
        int cx = bx + span / 2, hy = y + 14, pulse = (int) ((Math.sin(t * 6) + 1) * 1.5);
        heart(g, cx, hy, pulse, f.accent());
        int ax = bx + span - 8, ay = y + 37;   // the animal's slot centre
        switch (f.variant()) {
            case 0 -> { // fox: a light radius ring
                int r = 9 + (int) ((t * 4) % 6);
                int c = lerp(GOLD, INK, ((t * 4) % 6) / 6);
                g.fill(ax - r, ay - r, ax + r, ay - r + 1, c); g.fill(ax - r, ay + r - 1, ax + r, ay + r, c);
                g.fill(ax - r, ay - r, ax - r + 1, ay + r, c); g.fill(ax + r - 1, ay - r, ax + r, ay + r, c);
            }
            case 1 -> { // mossback: a nine-slot saddlebag grid
                int gx = ax - 28, gy = y + 8;
                for (int i = 0; i < 9; i++) { boolean filled = ((int) (t * 2) % 10) > i; g.fill(gx + (i % 3) * 5, gy + (i / 3) * 5, gx + (i % 3) * 5 + 4, gy + (i / 3) * 5 + 4, filled ? 0xFF7BC96F : DIM); }
            }
            default -> { // stag: a jump arc
                double p = (t % 2) / 2;
                int jx = ax - 30 + (int) (p * 24), jy = y + 24 - (int) (Math.sin(p * Math.PI) * 14);
                for (int n = 0; n < 6; n++) { double q = n / 6.0; g.fill(ax - 30 + (int) (q * 24), y + 24 - (int) (Math.sin(q * Math.PI) * 14), ax - 29 + (int) (q * 24), y + 25 - (int) (Math.sin(q * Math.PI) * 14), DIM); }
                g.fill(jx - 1, jy - 1, jx + 2, jy + 2, GOLD);
            }
        }
        meter(g, bx, y, span, progress, f.accent());
    }

    private static void heart(GuiGraphics g, int cx, int cy, int grow, int colour) {
        g.fill(cx - 4 - grow, cy - 2 - grow / 2, cx - 1, cy + 1, colour); g.fill(cx + 1, cy - 2 - grow / 2, cx + 4 + grow, cy + 1, colour);
        g.fill(cx - 4 - grow, cy, cx + 4 + grow, cy + 2, colour); g.fill(cx - 2, cy + 2, cx + 2, cy + 4 + grow / 2, colour);
    }

    /** Two player heads walk toward the shared vault while the charter and satchel move along the line. */
    private static void camp(GuiGraphics g, Flow f, int bx, int y, int span, int lineStart, int lineEnd, int railY, double t) {
        rail(g, lineStart, lineEnd, railY, t, f.accent());
        double progress = (t / 6) % 1;
        int movingX = lineStart + (int) ((lineEnd - lineStart - 16) * progress);
        g.renderItem(f.moving(), movingX, y + 25);
        int cx = bx + span / 2, gap = 30 - (int) (Math.min(1, progress * 1.5) * 20);
        head(g, cx - gap - 4, y + 17, 0xFFB98A62, 0xFF5A3A22);
        head(g, cx + gap - 4, y + 17, 0xFF8A6A48, 0xFF2A1A12);
        if (gap <= 12) heart(g, cx, y + 9, 0, f.accent());
        meter(g, bx, y, span, progress, f.accent());
    }

    private static void head(GuiGraphics g, int x, int y, int skin, int hair) {
        g.fill(x, y, x + 8, y + 8, skin); g.fill(x, y, x + 8, y + 3, hair);
        g.fill(x + 1, y + 4, x + 3, y + 6, PAPER); g.fill(x + 5, y + 4, x + 7, y + 6, PAPER);
        g.fill(x + 2, y + 4, x + 3, y + 6, 0xFF2A4A6A); g.fill(x + 5, y + 4, x + 6, y + 6, 0xFF2A4A6A);
    }

    /** The lens's 5x5 ley grid cycling quiet blue to singing gold. */
    private static void lens(GuiGraphics g, Flow f, int bx, int y, int span, int lineStart, int lineEnd, double t) {
        int cell = 6, gridW = cell * 5 + 4, gx = bx + span / 2 - gridW / 2, gy = y + 17;
        for (int i = 0; i < 25; i++) {
            int col = i % 5, row = i / 5;
            double dist = Math.hypot(col - 2, row - 2);
            double sing = 0.5 + 0.5 * Math.sin(t * 1.2 - dist * 0.9 + (col * 7 + row * 3) % 5 * 0.4);
            g.fill(gx + col * (cell + 1), gy + row * (cell + 1), gx + col * (cell + 1) + cell, gy + row * (cell + 1) + cell, lerp(0xFF3A6EB5, 0xFFE0A32D, sing));
        }
        g.fill(lineStart, y + 37, gx - 4, y + 38, RAIL); g.fill(gx + gridW + 3, y + 37, lineEnd, y + 38, RAIL);
        double read = 0.5 + 0.5 * Math.sin(t * 1.2);
        meter(g, bx, y, span, read, lerp(0xFF3A6EB5, 0xFFE0A32D, read));
    }

    /** A drumheart filling a meter; the gauge arrow turns with it and the lamp lights past the threshold. */
    private static void logic(GuiGraphics g, Font font, Flow f, int bx, int y, int span, int lineStart, int lineEnd, int railY, double t, BiConsumer<ItemStack, int[]> item) {
        double fill = (t / 8) % 1;
        int mx = lineStart + 2, mw = lineEnd - lineStart - 4;
        g.fill(mx, y + 31, mx + mw, y + 41, DIM);
        g.fill(mx, y + 31, mx + (int) (mw * fill), y + 41, fill >= 0.5 ? 0xFFE07A5F : TEAL);
        g.fill(mx + mw / 2, y + 29, mx + mw / 2 + 1, y + 43, GOLD);   // the 50 % threshold
        // bottom row: gauge icon with its turning arrow, the redstone readout, the threshold icon
        item.accept(f.moving(), new int[]{lineStart + 2, y + 50});
        int cx = lineStart + 32, cy = y + 62; double ang = Math.PI + fill * Math.PI;
        g.fill(cx - 9, cy + 1, cx + 10, cy + 2, RAIL);
        for (int n = 0; n < 8; n++) { int px = cx + (int) Math.round(Math.cos(ang) * n), py = cy + (int) Math.round(Math.sin(ang) * n); g.fill(px, py, px + 1, py + 1, GOLD); }
        String readout = "signal " + (fill >= 0.5 ? "15" : String.valueOf((int) (fill * 15)));
        g.drawString(font, readout, lineStart + 48, y + 54, fill >= 0.5 ? 0xFFE07A5F : TEAL, false);
        item.accept(f.middle(), new int[]{lineEnd - 18, y + 50});
        // the lamp at the far end lights when the stored charge passes the threshold
        int lx = bx + span - 16, ly = y + 29;
        g.fill(lx - 3, ly - 3, lx + 19, ly + 19, fill >= 0.5 ? 0xFFFFD27A : 0xFF3A2A1A);
    }

    /** The Codex over a station with a status readout cycling through what diagnostics report. */
    private static void diagnose(GuiGraphics g, Font font, Flow f, int bx, int y, int span, int lineStart, int lineEnd, int railY, double t) {
        rail(g, lineStart, lineEnd, railY, t, f.accent());
        double progress = (t / 6) % 1;
        int movingX = lineStart + (int) ((lineEnd - lineStart - 16) * progress);
        g.renderItem(f.moving(), movingX, y + 25);
        int status = (int) (t / 1.5) % STATUS.length;
        String text = "> " + STATUS[status];
        int tw = font.width(text) + 8, tx = bx + span / 2 - tw / 2, ty = y + 52;
        g.fill(tx, ty, tx + tw, ty + 13, 0xFF16303A); g.renderOutline(tx, ty, tw, 13, STATUS_COLOUR[status]);
        g.drawString(font, text, tx + 4, ty + 3, STATUS_COLOUR[status], false);
        boolean blink = ((int) (t * 2)) % 2 == 0;
        if (status == 3 || blink) g.fill(bx + span - 20, y + 25, bx + span - 18, y + 27, STATUS_COLOUR[status]);
        g.fill(bx, y + 55, tx - 6, y + 58, DIM); g.fill(tx + tw + 6, y + 55, bx + span, y + 58, DIM);
        g.fill(bx, y + 55, Math.min(tx - 6, bx + (int) (span * progress)), y + 58, STATUS_COLOUR[status]);
    }
}
