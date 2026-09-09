package tk.darrow.tribalpower.tribe;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Overworld-persisted tribe standing: player UUID -> int[9], per-day kill-gain counters and Tribe Marks handed out.
 * Same malformed-record-preserving idiom as DeepCacheSavedData.
 */
public class TribeStandingSavedData extends SavedData {
    public static final String FILE_ID = "tribalpower_tribe_standing";
    private static final int TRIBES = TribeDefinition.values().length;

    private static final class Record {
        final int[] standing = new int[TRIBES];
        final int[] kills = new int[TRIBES];
        long killDay = -1;
        int marks; // bitmask of tribes whose Mark was granted
    }

    private final Map<UUID, Record> records = new HashMap<>();
    private final ListTag unreadable = new ListTag();

    public static SavedData.Factory<TribeStandingSavedData> factory() {
        return new SavedData.Factory<>(TribeStandingSavedData::new, TribeStandingSavedData::load);
    }

    public static TribeStandingSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), FILE_ID);
    }

    private Record record(UUID id) { return records.computeIfAbsent(id, k -> new Record()); }

    public int get(UUID player, TribeDefinition tribe) {
        Record r = records.get(player);
        return r == null ? 0 : r.standing[tribe.ordinal()];
    }

    public int[] all(UUID player) {
        Record r = records.get(player);
        return r == null ? new int[TRIBES] : Arrays.copyOf(r.standing, TRIBES);
    }

    /** Sets standing (clamped at 0) and returns the new value. */
    public int set(UUID player, TribeDefinition tribe, int value) {
        Record r = record(player);
        r.standing[tribe.ordinal()] = Math.max(0, value);
        setDirty();
        return r.standing[tribe.ordinal()];
    }

    /** Kill gains still allowed today for this player and tribe; consumes one when {@code consume}. */
    public boolean tryKillGain(UUID player, TribeDefinition tribe, long day, int cap) {
        Record r = record(player);
        if (r.killDay != day) { r.killDay = day; Arrays.fill(r.kills, 0); setDirty(); }
        if (r.kills[tribe.ordinal()] >= cap) return false;
        r.kills[tribe.ordinal()]++;
        setDirty();
        return true;
    }

    public boolean hasMark(UUID player, TribeDefinition tribe) {
        Record r = records.get(player);
        return r != null && (r.marks & (1 << tribe.ordinal())) != 0;
    }

    public void grantMark(UUID player, TribeDefinition tribe) {
        record(player).marks |= 1 << tribe.ordinal();
        setDirty();
    }

    public static TribeStandingSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TribeStandingSavedData data = new TribeStandingSavedData();
        ListTag players = tag.getList("Players", Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag entry = players.getCompound(i);
            if (!entry.hasUUID("Id")) { data.unreadable.add(entry.copy()); continue; }
            Record r = new Record();
            int[] standing = entry.getIntArray("Standing");
            System.arraycopy(standing, 0, r.standing, 0, Math.min(TRIBES, standing.length));
            int[] kills = entry.getIntArray("Kills");
            System.arraycopy(kills, 0, r.kills, 0, Math.min(TRIBES, kills.length));
            r.killDay = entry.getLong("KillDay");
            r.marks = entry.getInt("Marks");
            data.records.put(entry.getUUID("Id"), r);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag players = unreadable.copy();
        for (Map.Entry<UUID, Record> e : records.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", e.getKey());
            entry.putIntArray("Standing", e.getValue().standing);
            entry.putIntArray("Kills", e.getValue().kills);
            entry.putLong("KillDay", e.getValue().killDay);
            entry.putInt("Marks", e.getValue().marks);
            players.add(entry);
        }
        tag.put("Players", players);
        return tag;
    }
}
