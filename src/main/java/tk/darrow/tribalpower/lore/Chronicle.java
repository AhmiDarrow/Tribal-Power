package tk.darrow.tribalpower.lore;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import tk.darrow.tribalpower.tribe.CodexUnlocksPayload;

/**
 * The Loom's history in sixteen fragments, carved into the March: eight on stones in its ruins, eight on murals
 * at the guardians' grounds. Reading one adds it to the Codex's Chronicle, which assembles them in order.
 * What a player has read lives under {@link Player#PERSISTED_NBT_TAG} so it survives death.
 */
public final class Chronicle {
    public static final int FRAGMENTS = 16;
    public static final String READ_KEY = "TribalChronicle";

    private Chronicle() {}

    public static int readMask(Player player) {
        CompoundTag data = player.getPersistentData();
        return data.contains(Player.PERSISTED_NBT_TAG) ? data.getCompound(Player.PERSISTED_NBT_TAG).getInt(READ_KEY) : 0;
    }

    public static boolean hasRead(Player player, int fragment) { return (readMask(player) & (1 << Math.floorMod(fragment, FRAGMENTS))) != 0; }
    public static int readCount(Player player) { return Integer.bitCount(readMask(player) & ((1 << FRAGMENTS) - 1)); }

    public static String titleKey(int fragment) { return "lore.tribalpower.chronicle." + Math.floorMod(fragment, FRAGMENTS) + ".title"; }
    public static String textKey(int fragment) { return "lore.tribalpower.chronicle." + Math.floorMod(fragment, FRAGMENTS) + ".text"; }

    /** Records a fragment as read. Returns true the first time; the Codex learns of it and the last one is an advancement. */
    public static boolean markRead(Player player, int fragment) {
        int bit = 1 << Math.floorMod(fragment, FRAGMENTS);
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.contains(Player.PERSISTED_NBT_TAG) ? data.getCompound(Player.PERSISTED_NBT_TAG) : new CompoundTag();
        int mask = persisted.getInt(READ_KEY);
        if ((mask & bit) != 0) return false;
        persisted.putInt(READ_KEY, mask | bit);
        data.put(Player.PERSISTED_NBT_TAG, persisted);
        if (player instanceof ServerPlayer sp) {
            CodexUnlocksPayload.sync(sp);
            if (Integer.bitCount((mask | bit) & ((1 << FRAGMENTS) - 1)) == FRAGMENTS)
                tk.darrow.tribalpower.camp.CampHooks.award(sp.serverLevel(), sp.getUUID(), "march/chronicle");
        }
        return true;
    }

    /** Right-click on a carving: mark it, and say what it was. */
    public static void read(Player player, int fragment) {
        boolean first = markRead(player, fragment);
        player.displayClientMessage(Component.translatable(first ? "message.tribalpower.chronicle.found" : "message.tribalpower.chronicle.again",
                Component.translatable(titleKey(fragment)), readCount(player), FRAGMENTS).withStyle(first ? ChatFormatting.GOLD : ChatFormatting.GRAY), true);
    }
}
