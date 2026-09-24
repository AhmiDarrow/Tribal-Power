package tk.darrow.tribalpower.finale;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Who has made the Ninth Agreement in this world. It is kept for good. */
public class AgreementSavedData extends SavedData {
    public static final String FILE_ID = "tribalpower_agreement";
    private final Set<UUID> agreed = new HashSet<>();

    public static AgreementSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(AgreementSavedData::new, AgreementSavedData::load), FILE_ID);
    }

    public boolean agreed(UUID player) { return agreed.contains(player); }

    public void agree(UUID player) {
        if (agreed.add(player)) setDirty();
    }

    public static AgreementSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        AgreementSavedData data = new AgreementSavedData();
        for (Tag id : tag.getList("Agreed", Tag.TAG_INT_ARRAY)) data.agreed.add(NbtUtils.loadUUID(id));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (UUID id : agreed) list.add(NbtUtils.createUUID(id));
        tag.put("Agreed", list);
        return tag;
    }
}
