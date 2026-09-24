package tk.darrow.tribalpower.event;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import tk.darrow.tribalpower.api.pulse.Attunement;

/**
 * What the March is doing on its own: which weathers run and until when, which voice surges, when the last
 * rolls were, and who has joined which festival. Dirtied only when something changes.
 */
public class MarchEventsSavedData extends SavedData {
    public static final String FILE_ID = "tribalpower_march_events";
    private final long[] weatherUntil = new long[MarchWeather.values().length];
    private int surgeVoice = -1;
    private long surgeUntil, lastSurgeEnd, lastWeatherRoll, lastFestivalDay = -1;
    private final Map<UUID, Set<Long>> festivals = new HashMap<>();

    public static MarchEventsSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(MarchEventsSavedData::new, MarchEventsSavedData::load), FILE_ID);
    }

    // ---- weather ----
    public boolean weatherActive(MarchWeather weather, long now) { return weatherUntil[weather.ordinal()] > now; }
    public long weatherUntil(MarchWeather weather) { return weatherUntil[weather.ordinal()]; }
    public void setWeather(MarchWeather weather, long until) { weatherUntil[weather.ordinal()] = until; setDirty(); }
    public long lastWeatherRoll() { return lastWeatherRoll; }
    public void setLastWeatherRoll(long at) { lastWeatherRoll = at; setDirty(); }

    /** Drops weathers that have run out; true when any did. */
    public boolean expireWeather(long now) {
        boolean changed = false;
        for (int i = 0; i < weatherUntil.length; i++)
            if (weatherUntil[i] != 0 && weatherUntil[i] <= now) { weatherUntil[i] = 0; changed = true; }
        if (changed) setDirty();
        return changed;
    }

    // ---- surge ----
    public Attunement surgeVoice(long now) { return surgeVoice >= 0 && surgeUntil > now ? Attunement.values()[surgeVoice] : null; }
    public long surgeUntil() { return surgeUntil; }
    public long lastSurgeEnd() { return lastSurgeEnd; }
    public void setSurge(Attunement voice, long until) {
        surgeVoice = voice == null ? -1 : voice.ordinal();
        surgeUntil = until;
        setDirty();
    }
    /** Ends a surge that has run out; true when one did. */
    public boolean expireSurge(long now) {
        if (surgeVoice < 0 || surgeUntil > now) return false;
        surgeVoice = -1;
        lastSurgeEnd = now;
        setDirty();
        return true;
    }
    public void setLastSurgeEnd(long at) { lastSurgeEnd = at; setDirty(); }

    // ---- festivals ----
    public long lastFestivalDay() { return lastFestivalDay; }
    public void setLastFestivalDay(long day) { lastFestivalDay = day; setDirty(); }
    private static long festivalKey(int tribe, long day) { return (day << 4) | tribe; }
    public boolean joined(UUID player, int tribe, long day) {
        Set<Long> set = festivals.get(player);
        return set != null && set.contains(festivalKey(tribe, day));
    }
    public boolean join(UUID player, int tribe, long day) {
        Set<Long> set = festivals.computeIfAbsent(player, k -> new HashSet<>());
        boolean added = set.add(festivalKey(tribe, day));
        // keys older than a whole cycle can never be asked about again
        long oldest = day - Math.max(9, tk.darrow.tribalpower.config.TribalConfig.festivalCycleDays()) - 1;
        if (set.removeIf(key -> (key >> 4) < oldest)) added = added || false;
        if (added) setDirty();
        return added;
    }

    public static MarchEventsSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        MarchEventsSavedData data = new MarchEventsSavedData();
        long[] until = tag.getLongArray("Weather");
        System.arraycopy(until, 0, data.weatherUntil, 0, Math.min(until.length, data.weatherUntil.length));
        data.surgeVoice = tag.contains("SurgeVoice") ? tag.getInt("SurgeVoice") : -1;
        data.surgeUntil = tag.getLong("SurgeUntil");
        data.lastSurgeEnd = tag.getLong("LastSurgeEnd");
        data.lastWeatherRoll = tag.getLong("LastWeatherRoll");
        data.lastFestivalDay = tag.contains("LastFestivalDay") ? tag.getLong("LastFestivalDay") : -1;
        for (Tag entry : tag.getList("Festivals", Tag.TAG_COMPOUND)) {
            CompoundTag record = (CompoundTag) entry;
            Set<Long> set = new HashSet<>();
            for (long key : record.getLongArray("Joined")) set.add(key);
            data.festivals.put(NbtUtils.loadUUID(record.get("Player")), set);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLongArray("Weather", weatherUntil.clone());
        tag.putInt("SurgeVoice", surgeVoice);
        tag.putLong("SurgeUntil", surgeUntil);
        tag.putLong("LastSurgeEnd", lastSurgeEnd);
        tag.putLong("LastWeatherRoll", lastWeatherRoll);
        tag.putLong("LastFestivalDay", lastFestivalDay);
        ListTag list = new ListTag();
        for (var entry : festivals.entrySet()) {
            CompoundTag record = new CompoundTag();
            record.put("Player", NbtUtils.createUUID(entry.getKey()));
            record.putLongArray("Joined", entry.getValue().stream().mapToLong(Long::longValue).toArray());
            list.add(record);
        }
        tag.put("Festivals", list);
        return tag;
    }
}
