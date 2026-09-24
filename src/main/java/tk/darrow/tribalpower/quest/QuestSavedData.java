package tk.darrow.tribalpower.quest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import tk.darrow.tribalpower.tribe.TribeDefinition;

/**
 * Every player's standing with the tribes' work: the request each tribe's Elder has open with them and how far
 * along it is, how many requests they have finished for each tribe, where they stand on each tribe's questline,
 * and which relics they have been given. Kept on the overworld beside tribe standing.
 */
public class QuestSavedData extends SavedData {
    public static final String FILE_ID = "tribalpower_quests";
    private static final int TRIBES = TribeDefinition.values().length;

    /** One tribe's request with one player: which template, from which day's rotation, and progress. */
    public record Request(String template, long day, int progress) {
        public Request advance(int by) { return new Request(template, day, progress + by); }
    }

    static final class Record {
        final Request[] requests = new Request[TRIBES];
        final int[] completed = new int[TRIBES];
        final int[] step = new int[TRIBES];
        final int[] stepProgress = new int[TRIBES];
        int relics, patterns;
    }

    private final Map<UUID, Record> records = new HashMap<>();

    public static QuestSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(QuestSavedData::new, QuestSavedData::load), FILE_ID);
    }

    Record record(UUID player) { return records.computeIfAbsent(player, k -> new Record()); }

    public Request request(UUID player, TribeDefinition tribe) {
        Record r = records.get(player);
        return r == null ? null : r.requests[tribe.ordinal()];
    }

    public void setRequest(UUID player, TribeDefinition tribe, Request request) {
        record(player).requests[tribe.ordinal()] = request;
        setDirty();
    }

    public int completed(UUID player, TribeDefinition tribe) {
        Record r = records.get(player);
        return r == null ? 0 : r.completed[tribe.ordinal()];
    }

    public void completedOne(UUID player, TribeDefinition tribe) {
        record(player).completed[tribe.ordinal()]++;
        setDirty();
    }

    public int step(UUID player, TribeDefinition tribe) {
        Record r = records.get(player);
        return r == null ? 0 : r.step[tribe.ordinal()];
    }

    public int stepProgress(UUID player, TribeDefinition tribe) {
        Record r = records.get(player);
        return r == null ? 0 : r.stepProgress[tribe.ordinal()];
    }

    public void setStep(UUID player, TribeDefinition tribe, int step, int progress) {
        Record r = record(player);
        r.step[tribe.ordinal()] = step;
        r.stepProgress[tribe.ordinal()] = progress;
        setDirty();
    }

    public boolean hasRelic(UUID player, TribeDefinition tribe) {
        Record r = records.get(player);
        return r != null && (r.relics & (1 << tribe.ordinal())) != 0;
    }

    public boolean hasPattern(UUID player, TribeDefinition tribe) {
        Record r = records.get(player);
        return r != null && (r.patterns & (1 << tribe.ordinal())) != 0;
    }

    public void grantPattern(UUID player, TribeDefinition tribe) {
        record(player).patterns |= 1 << tribe.ordinal();
        setDirty();
    }

    public void grantRelic(UUID player, TribeDefinition tribe) {
        record(player).relics |= 1 << tribe.ordinal();
        setDirty();
    }

    public static QuestSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        QuestSavedData data = new QuestSavedData();
        ListTag players = tag.getList("Players", Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag entry = players.getCompound(i);
            if (!entry.hasUUID("Id")) continue;
            Record r = data.record(entry.getUUID("Id"));
            int[] completed = entry.getIntArray("Completed"), step = entry.getIntArray("Step"), progress = entry.getIntArray("StepProgress");
            for (int t = 0; t < TRIBES; t++) {
                if (t < completed.length) r.completed[t] = completed[t];
                if (t < step.length) r.step[t] = step[t];
                if (t < progress.length) r.stepProgress[t] = progress[t];
            }
            r.relics = entry.getInt("Relics");
            r.patterns = entry.getInt("Patterns");
            ListTag requests = entry.getList("Requests", Tag.TAG_COMPOUND);
            for (int k = 0; k < requests.size(); k++) {
                CompoundTag request = requests.getCompound(k);
                int tribe = request.getInt("Tribe");
                if (tribe >= 0 && tribe < TRIBES) r.requests[tribe] = new Request(request.getString("Template"), request.getLong("Day"), request.getInt("Progress"));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag players = new ListTag();
        records.forEach((id, r) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", id);
            entry.putIntArray("Completed", r.completed);
            entry.putIntArray("Step", r.step);
            entry.putIntArray("StepProgress", r.stepProgress);
            entry.putInt("Relics", r.relics);
            entry.putInt("Patterns", r.patterns);
            ListTag requests = new ListTag();
            for (int t = 0; t < TRIBES; t++) {
                if (r.requests[t] == null) continue;
                CompoundTag request = new CompoundTag();
                request.putInt("Tribe", t);
                request.putString("Template", r.requests[t].template());
                request.putLong("Day", r.requests[t].day());
                request.putInt("Progress", r.requests[t].progress());
                requests.add(request);
            }
            entry.put("Requests", requests);
            players.add(entry);
        });
        tag.put("Players", players);
        return tag;
    }
}
