package tk.darrow.tribalpower.client;

import tk.darrow.tribalpower.guide.CodexEntries.Entry;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.world.structure.LoreTabletBlock;

/**
 * Client-side copy of the player's Codex unlocks, fed by {@code tribalpower:codex_unlocks} and cleared on disconnect.
 * Deliberately free of client-only Minecraft types so the payload handler can reference it from common code.
 */
public final class CodexUnlocks {
    private static volatile int tribes, tablets;

    private CodexUnlocks() {}

    public static void accept(int tribeMask, int tabletMask) { tribes = tribeMask; tablets = tabletMask; }
    public static void reset() { accept(0, 0); }

    public static boolean tribeUnlocked(TribeDefinition tribe) { return (tribes & (1 << tribe.ordinal())) != 0; }
    public static boolean tabletUnlocked(int tablet) { return tablet >= 0 && tablet < LoreTabletBlock.TABLETS && (tablets & (1 << tablet)) != 0; }
    public static int tribesUnlocked() { return Integer.bitCount(tribes & ((1 << TribeDefinition.values().length) - 1)); }
    public static int tabletsUnlocked() { return Integer.bitCount(tablets & ((1 << LoreTabletBlock.TABLETS) - 1)); }

    /** True for a {@code tribe_<id>} page whose Mark the player holds or a {@code tablet_<n>} page they have read. */
    public static boolean unlocked(Entry entry) {
        String id = entry.id();
        if (id.startsWith("tribe_")) {
            for (TribeDefinition tribe : TribeDefinition.values())
                if (id.equals("tribe_" + tribe.id())) return tribeUnlocked(tribe);
            return false;
        }
        if (id.startsWith("tablet_")) {
            try { return tabletUnlocked(Integer.parseInt(id.substring(7))); } catch (NumberFormatException e) { return false; }
        }
        return false;
    }

    /** One-line progress hint for a category, or null. */
    public static String hint(String category) {
        return switch (category) {
            case "The Nine Tribes" -> tribesUnlocked() + " of " + TribeDefinition.values().length + " tribes met";
            case "Lore Tablets" -> tabletsUnlocked() + " of " + LoreTabletBlock.TABLETS + " tablets read";
            default -> null;
        };
    }
}
