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
        final int[] offerings = new int[TRIBES];
        long killDay = -1;
        long offerDay = -1;
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

    /**
     * Standing still allowed today from hearth offerings, consuming what it grants.
     *
     * <p>Same idiom as the kill cap, and it exists for the same reason: without it, renewable ore feeds
     * the hearth that gates the pit that makes the ore (design 3.1 section 7.7).
     *
     * @return how much of {@code want} may actually be granted, 0 when the day is spent
     */
    public int allowOffering(UUID player, TribeDefinition tribe, long day, int cap, int want) {
        if (want <= 0) return 0;
        Record r = record(player);
        if (r.offerDay != day) { r.offerDay = day; Arrays.fill(r.offerings, 0); setDirty(); }
        int spent = r.offerings[tribe.ordinal()];
        int allowed = Math.max(0, Math.min(want, cap - spent));
        if (allowed > 0) { r.offerings[tribe.ordinal()] = spent + allowed; setDirty(); }
        return allowed;
    }

    /** Standing already taken from offerings today, for diagnostics and tests. */
    public int offeringsToday(UUID player, TribeDefinition tribe, long day) {
        Record r = records.get(player);
        return r == null || r.offerDay != day ? 0 : r.offerings[tribe.ordinal()];
    }

    public boolean hasMark(UUID player, TribeDefinition tribe) {
        Record r = records.get(player);
        return r != null && (r.marks & (1 << tribe.ordinal())) != 0;
    }

    /** Bitmask (bit = tribe ordinal) of the Tribe Marks this player has received; drives the Codex tribe pages. */
    public int marks(UUID player) {
        Record r = records.get(player);
        return r == null ? 0 : r.marks;
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
            int[] offerings = entry.getIntArray("Offerings");
            System.arraycopy(offerings, 0, r.offerings, 0, Math.min(TRIBES, offerings.length));
            r.killDay = entry.getLong("KillDay");
            r.offerDay = entry.contains("OfferDay") ? entry.getLong("OfferDay") : -1;
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
            entry.putIntArray("Offerings", e.getValue().offerings);
            entry.putLong("KillDay", e.getValue().killDay);
            entry.putLong("OfferDay", e.getValue().offerDay);
            entry.putInt("Marks", e.getValue().marks);
            players.add(entry);
        }
        tag.put("Players", players);
        return tag;
    }
}
