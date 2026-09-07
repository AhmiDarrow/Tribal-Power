package tk.darrow.tribalpower.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Overworld-persisted per-player Deep Cache inventories + March visit flags.
 */
public class DeepCacheSavedData extends SavedData {
    public static final String FILE_ID = "tribalpower_deep_cache";
    public static final int SLOTS = 54;

    private final Map<UUID, NonNullList<ItemStack>> inventories = new HashMap<>();
    private final Set<UUID> visitedMarch = new HashSet<>();
    // Preserve unreadable records without letting one malformed owner disable every cache.
    private final ListTag unreadablePlayers = new ListTag();
    private final ListTag unreadableVisits = new ListTag();

    public static SavedData.Factory<DeepCacheSavedData> factory() {
        return new SavedData.Factory<>(DeepCacheSavedData::new, DeepCacheSavedData::load);
    }

    public static DeepCacheSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(factory(), FILE_ID);
    }

    public NonNullList<ItemStack> getOrCreateItems(UUID playerId) {
        return inventories.computeIfAbsent(playerId, id -> NonNullList.withSize(SLOTS, ItemStack.EMPTY));
    }

    public boolean hasVisitedMarch(UUID playerId) {
        return visitedMarch.contains(playerId);
    }

    public void markVisitedMarch(UUID playerId) {
        if (visitedMarch.add(playerId)) {
            setDirty();
        }
    }

    public static DeepCacheSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        DeepCacheSavedData data = new DeepCacheSavedData();
        ListTag players = tag.getList("Players", Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag entry = players.getCompound(i);
            if (!entry.hasUUID("Id")) { data.unreadablePlayers.add(entry.copy()); continue; }
            UUID id = entry.getUUID("Id");
            NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(entry, items, registries);
            data.inventories.put(id, items);
        }
        ListTag visits = tag.getList("Visited", Tag.TAG_COMPOUND);
        for (int i = 0; i < visits.size(); i++) {
            CompoundTag entry = visits.getCompound(i);
            if (!entry.hasUUID("Id")) { data.unreadableVisits.add(entry.copy()); continue; }
            data.visitedMarch.add(entry.getUUID("Id"));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag players = unreadablePlayers.copy();
        for (Map.Entry<UUID, NonNullList<ItemStack>> entry : inventories.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("Id", entry.getKey());
            ContainerHelper.saveAllItems(playerTag, entry.getValue(), registries);
            players.add(playerTag);
        }
        tag.put("Players", players);

        ListTag visits = unreadableVisits.copy();
        for (UUID id : visitedMarch) {
            CompoundTag visitTag = new CompoundTag();
            visitTag.putUUID("Id", id);
            visits.add(visitTag);
        }
        tag.put("Visited", visits);
        return tag;
    }
}
