package tk.darrow.tribalpower.client;

import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.world.structure.LoreTabletBlock;

/**
 * Client-side copy of the player's Codex unlocks, fed by {@code tribalpower:codex_unlocks} and cleared on disconnect.
 * Deliberately free of client-only Minecraft types so the payload handler can reference it from common code.
 */
public final class CodexUnlocks {
    private static volatile int tribes, tablets, fragments;

    private CodexUnlocks() {}

    public static void accept(int tribeMask, int tabletMask, int fragmentMask) { tribes = tribeMask; tablets = tabletMask; fragments = fragmentMask; }
    public static void reset() { accept(0, 0, 0); }
    public static boolean fragmentUnlocked(int fragment) { return fragment >= 0 && fragment < tk.darrow.tribalpower.lore.Chronicle.FRAGMENTS && (fragments & (1 << fragment)) != 0; }
    public static int fragmentsUnlocked() { return Integer.bitCount(fragments & ((1 << tk.darrow.tribalpower.lore.Chronicle.FRAGMENTS) - 1)); }

    public static boolean tribeUnlocked(TribeDefinition tribe) { return (tribes & (1 << tribe.ordinal())) != 0; }
    public static boolean tabletUnlocked(int tablet) { return tablet >= 0 && tablet < LoreTabletBlock.TABLETS && (tablets & (1 << tablet)) != 0; }
    public static int tribesUnlocked() { return Integer.bitCount(tribes & ((1 << TribeDefinition.values().length) - 1)); }
    public static int tabletsUnlocked() { return Integer.bitCount(tablets & ((1 << LoreTabletBlock.TABLETS) - 1)); }

    /** True when a Codex entry's {@code unlock} ("tribe:&lt;id&gt;" or "tablet:&lt;n&gt;") has been earned in the world. */
    public static boolean unlocked(String unlock) {
        if (unlock == null || unlock.isEmpty()) return false;
        if (unlock.startsWith("tribe:")) {
            for (TribeDefinition tribe : TribeDefinition.values())
                if (unlock.equals("tribe:" + tribe.id())) return tribeUnlocked(tribe);
            return false;
        }
        if (unlock.startsWith("tablet:")) {
            try { return tabletUnlocked(Integer.parseInt(unlock.substring(7))); } catch (NumberFormatException e) { return false; }
        }
        if (unlock.startsWith("fragment:")) {
            try { return fragmentUnlocked(Integer.parseInt(unlock.substring(9))); } catch (NumberFormatException e) { return false; }
        }
        return false;
    }
}
