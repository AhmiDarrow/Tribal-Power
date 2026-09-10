package tk.darrow.tribalpower.gate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The book of gates (design 3.1 section 8): every keystone in the world, where it stands, what it is
 * called, and which keystone it answers.
 *
 * <p>Kept on the overworld like every other Tribal Power store, and written with the same
 * malformed-record-preserving idiom as {@code DeepCacheSavedData}: one unreadable keystone must never
 * take the rest of a player's network down with it.
 */
public class GateSavedData extends SavedData {
    public static final String FILE_ID = "tribalpower_gates";

    /** One keystone. {@code partner} is the id of the keystone it answers, or null when it answers nothing. */
    public record Gate(UUID id, String dimension, BlockPos pos, String name, @Nullable UUID partner, @Nullable UUID owner) {
        public Gate withPartner(@Nullable UUID other) { return new Gate(id, dimension, pos, name, other, owner); }
        public Gate withName(String newName) { return new Gate(id, dimension, pos, newName, partner, owner); }
    }

    private final Map<UUID, Gate> gates = new LinkedHashMap<>();
    private final ListTag unreadable = new ListTag();

    public static SavedData.Factory<GateSavedData> factory() {
        return new SavedData.Factory<>(GateSavedData::new, GateSavedData::load);
    }

    public static GateSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), FILE_ID);
    }

    public static String dimension(Level level) { return level.dimension().location().toString(); }

    @Nullable public Gate gate(UUID id) { return id == null ? null : gates.get(id); }

    public List<Gate> all() { return List.copyOf(gates.values()); }

    /** Every gate this player placed, in the order they were placed. */
    public List<Gate> owned(UUID owner) {
        List<Gate> out = new ArrayList<>();
        for (Gate gate : gates.values()) if (owner.equals(gate.owner())) out.add(gate);
        return out;
    }

    @Nullable
    public Gate byName(UUID owner, String name) {
        for (Gate gate : gates.values())
            if (gate.name().equalsIgnoreCase(name) && (owner == null || owner.equals(gate.owner()))) return gate;
        return null;
    }

    @Nullable
    public Gate at(Level level, BlockPos pos) {
        String dim = dimension(level);
        for (Gate gate : gates.values()) if (gate.dimension().equals(dim) && gate.pos().equals(pos)) return gate;
        return null;
    }

    /** Records a keystone, or returns the record it already has. */
    public Gate record(Level level, BlockPos pos, String name, @Nullable UUID owner) {
        Gate existing = at(level, pos);
        if (existing != null) return existing;
        Gate gate = new Gate(UUID.randomUUID(), dimension(level), pos.immutable(), name, null, owner);
        gates.put(gate.id(), gate);
        setDirty();
        return gate;
    }

    public void rename(UUID id, String name) {
        Gate gate = gates.get(id);
        if (gate == null) return;
        gates.put(id, gate.withName(name));
        setDirty();
    }

    /** Links two keystones both ways, dropping whatever either answered before. */
    public void link(UUID a, UUID b) {
        Gate first = gates.get(a);
        Gate second = gates.get(b);
        if (first == null || second == null || a.equals(b)) return;
        unlink(a);
        unlink(b);
        gates.put(a, first.withPartner(b));
        gates.put(b, second.withPartner(a));
        setDirty();
    }

    /** Cuts the thread from both ends. Returns the partner that was let go, if there was one. */
    @Nullable
    public Gate unlink(UUID id) {
        Gate gate = gates.get(id);
        if (gate == null || gate.partner() == null) return null;
        Gate partner = gates.get(gate.partner());
        gates.put(id, gate.withPartner(null));
        if (partner != null) gates.put(partner.id(), partner.withPartner(null));
        setDirty();
        return partner;
    }

    /** Forgets a broken keystone, letting its partner go first. */
    @Nullable
    public Gate remove(UUID id) {
        Gate partner = unlink(id);
        if (gates.remove(id) != null) setDirty();
        return partner;
    }

    public static GateSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        GateSavedData data = new GateSavedData();
        ListTag list = tag.getList("Gates", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID("Id") || !entry.contains("Dim") || !entry.contains("Pos")) {
                data.unreadable.add(entry.copy());
                continue;
            }
            data.gates.put(entry.getUUID("Id"), new Gate(
                    entry.getUUID("Id"),
                    entry.getString("Dim"),
                    BlockPos.of(entry.getLong("Pos")),
                    entry.getString("Name"),
                    entry.hasUUID("Partner") ? entry.getUUID("Partner") : null,
                    entry.hasUUID("Owner") ? entry.getUUID("Owner") : null));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = unreadable.copy();
        for (Gate gate : gates.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", gate.id());
            entry.putString("Dim", gate.dimension());
            entry.putLong("Pos", gate.pos().asLong());
            entry.putString("Name", gate.name());
            if (gate.partner() != null) entry.putUUID("Partner", gate.partner());
            if (gate.owner() != null) entry.putUUID("Owner", gate.owner());
            list.add(entry);
        }
        tag.put("Gates", list);
        return tag;
    }
}
