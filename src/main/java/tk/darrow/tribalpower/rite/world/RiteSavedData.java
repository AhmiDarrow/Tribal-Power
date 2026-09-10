package tk.darrow.tribalpower.rite.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Overworld-persisted record of every timed world rite: blessed chunks (Green Blessing),
 * temporary wards (Still Night) and ley lines (Ley Binding). Entries carry an absolute expiry
 * game tick; game time is server-wide, so one store serves every dimension.
 */
public class RiteSavedData extends SavedData {
    public static final String FILE_ID = "tribalpower_rites";

    /** A temporary ward: hostile spawns suppressed within {@code radius} of {@code center}. */
    public record Ward(String dimension, BlockPos center, int radius, long expiry) {}
    /** A called spring: a Spirit Cistern that refills itself until {@code expiry}. */
    public record Spring(String dimension, BlockPos pos, long expiry) {}
    /** A temporary ley line joining two Resonance Totems. */
    public record LeyLine(String dimension, BlockPos a, BlockPos b, long expiry) {
        public boolean touches(BlockPos pos) { return a.equals(pos) || b.equals(pos); }
        public BlockPos other(BlockPos pos) { return a.equals(pos) ? b : a; }
    }

    private final Map<String, Map<Long, Long>> blessed = new HashMap<>();
    private final List<Ward> wards = new ArrayList<>();
    private final List<LeyLine> leyLines = new ArrayList<>();
    private final List<Spring> springs = new ArrayList<>();

    public static SavedData.Factory<RiteSavedData> factory() {
        return new SavedData.Factory<>(RiteSavedData::new, RiteSavedData::load);
    }

    public static RiteSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), FILE_ID);
    }

    public static String dimension(Level level) {
        return level.dimension().location().toString();
    }

    // ---- Green Blessing -------------------------------------------------------------------

    public void bless(ServerLevel level, ChunkPos chunk, long expiry) {
        blessed.computeIfAbsent(dimension(level), d -> new HashMap<>()).merge(chunk.toLong(), expiry, Math::max);
        setDirty();
    }

    public boolean isBlessed(ServerLevel level, ChunkPos chunk) {
        Map<Long, Long> chunks = blessed.get(dimension(level));
        if (chunks == null) return false;
        Long expiry = chunks.get(chunk.toLong());
        return expiry != null && expiry > level.getGameTime();
    }

    /** Live blessed chunks in a dimension; expired entries are dropped as they are met. */
    public List<ChunkPos> blessedChunks(ServerLevel level) {
        Map<Long, Long> chunks = blessed.get(dimension(level));
        if (chunks == null || chunks.isEmpty()) return List.of();
        List<ChunkPos> live = new ArrayList<>(chunks.size());
        long now = level.getGameTime();
        Iterator<Map.Entry<Long, Long>> it = chunks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Long> entry = it.next();
            if (entry.getValue() <= now) { it.remove(); setDirty(); }
            else live.add(new ChunkPos(entry.getKey()));
        }
        return live;
    }

    // ---- Still Night ------------------------------------------------------------------------

    public void ward(ServerLevel level, BlockPos center, int radius, long expiry) {
        wards.add(new Ward(dimension(level), center.immutable(), radius, expiry));
        setDirty();
    }

    public List<Ward> wards(ServerLevel level) {
        if (wards.isEmpty()) return List.of(); // hot path: consulted on every hostile spawn placement check
        prune(level.getGameTime());
        String dim = dimension(level);
        List<Ward> out = new ArrayList<>();
        for (Ward ward : wards) if (ward.dimension().equals(dim)) out.add(ward);
        return out;
    }

    // ---- Ley Binding ------------------------------------------------------------------------

    public void bind(ServerLevel level, BlockPos a, BlockPos b, long expiry) {
        String dim = dimension(level);
        leyLines.removeIf(line -> line.dimension().equals(dim) && line.touches(a) && line.touches(b));
        leyLines.add(new LeyLine(dim, a.immutable(), b.immutable(), expiry));
        setDirty();
    }

    /** Drop every ley line touching {@code pos} (a totem was broken). */
    public void unbind(ServerLevel level, BlockPos pos) {
        if (leyLines.isEmpty()) return;
        String dim = dimension(level);
        if (leyLines.removeIf(line -> line.dimension().equals(dim) && line.touches(pos))) setDirty();
    }

    public List<LeyLine> leyLines(ServerLevel level) {
        if (leyLines.isEmpty()) return List.of(); // hot path: consulted per node of every lattice walk
        prune(level.getGameTime());
        String dim = dimension(level);
        List<LeyLine> out = new ArrayList<>();
        for (LeyLine line : leyLines) if (line.dimension().equals(dim)) out.add(line);
        return out;
    }

    private void prune(long now) {
        if (wards.removeIf(w -> w.expiry() <= now)) setDirty();
        if (leyLines.removeIf(l -> l.expiry() <= now)) setDirty();
        if (springs.removeIf(s -> s.expiry() <= now)) setDirty();
    }

    // ---- Spring Calling ---------------------------------------------------------------------

    /** Marks a cistern a spring, extending rather than shortening whatever it already had. */
    public void spring(ServerLevel level, BlockPos pos, long expiry) {
        String dim = dimension(level);
        for (int i = 0; i < springs.size(); i++) {
            Spring existing = springs.get(i);
            if (!existing.dimension().equals(dim) || !existing.pos().equals(pos)) continue;
            springs.set(i, new Spring(dim, pos.immutable(), Math.max(existing.expiry(), expiry)));
            setDirty();
            return;
        }
        springs.add(new Spring(dim, pos.immutable(), expiry));
        setDirty();
    }

    public List<BlockPos> springs(ServerLevel level) {
        if (springs.isEmpty()) return List.of(); // hot path: consulted every second per dimension
        long now = level.getGameTime();
        if (springs.removeIf(spring -> spring.expiry() <= now)) setDirty();
        String dim = dimension(level);
        List<BlockPos> out = new ArrayList<>();
        for (Spring spring : springs) if (spring.dimension().equals(dim)) out.add(spring.pos());
        return out;
    }

    // ---- Serialisation ------------------------------------------------------------------------

    public static RiteSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        RiteSavedData data = new RiteSavedData();
        ListTag blessedList = tag.getList("Blessed", Tag.TAG_COMPOUND);
        for (int i = 0; i < blessedList.size(); i++) {
            CompoundTag entry = blessedList.getCompound(i);
            data.blessed.computeIfAbsent(entry.getString("Dim"), d -> new HashMap<>()).put(entry.getLong("Chunk"), entry.getLong("Expiry"));
        }
        ListTag wardList = tag.getList("Wards", Tag.TAG_COMPOUND);
        for (int i = 0; i < wardList.size(); i++) {
            CompoundTag entry = wardList.getCompound(i);
            data.wards.add(new Ward(entry.getString("Dim"), BlockPos.of(entry.getLong("Center")), entry.getInt("Radius"), entry.getLong("Expiry")));
        }
        ListTag lineList = tag.getList("LeyLines", Tag.TAG_COMPOUND);
        for (int i = 0; i < lineList.size(); i++) {
            CompoundTag entry = lineList.getCompound(i);
            data.leyLines.add(new LeyLine(entry.getString("Dim"), BlockPos.of(entry.getLong("A")), BlockPos.of(entry.getLong("B")), entry.getLong("Expiry")));
        }
        ListTag springList = tag.getList("Springs", Tag.TAG_COMPOUND);
        for (int i = 0; i < springList.size(); i++) {
            CompoundTag entry = springList.getCompound(i);
            data.springs.add(new Spring(entry.getString("Dim"), BlockPos.of(entry.getLong("Pos")), entry.getLong("Expiry")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag blessedList = new ListTag();
        for (Map.Entry<String, Map<Long, Long>> dim : blessed.entrySet()) {
            for (Map.Entry<Long, Long> chunk : dim.getValue().entrySet()) {
                CompoundTag entry = new CompoundTag();
                entry.putString("Dim", dim.getKey());
                entry.putLong("Chunk", chunk.getKey());
                entry.putLong("Expiry", chunk.getValue());
                blessedList.add(entry);
            }
        }
        tag.put("Blessed", blessedList);
        ListTag wardList = new ListTag();
        for (Ward ward : wards) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Dim", ward.dimension());
            entry.putLong("Center", ward.center().asLong());
            entry.putInt("Radius", ward.radius());
            entry.putLong("Expiry", ward.expiry());
            wardList.add(entry);
        }
        tag.put("Wards", wardList);
        ListTag lineList = new ListTag();
        for (LeyLine line : leyLines) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Dim", line.dimension());
            entry.putLong("A", line.a().asLong());
            entry.putLong("B", line.b().asLong());
            entry.putLong("Expiry", line.expiry());
            lineList.add(entry);
        }
        tag.put("LeyLines", lineList);
        ListTag springList = new ListTag();
        for (Spring spring : springs) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Dim", spring.dimension());
            entry.putLong("Pos", spring.pos().asLong());
            entry.putLong("Expiry", spring.expiry());
            springList.add(entry);
        }
        tag.put("Springs", springList);
        return tag;
    }
}
