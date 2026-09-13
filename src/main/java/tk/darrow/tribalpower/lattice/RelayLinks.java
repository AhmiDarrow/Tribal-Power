package tk.darrow.tribalpower.lattice;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tk.darrow.tribalpower.blockentity.WirelessRelayBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Relays that share the same link item are a pair. */
public final class RelayLinks {
    private record Handle(ResourceKey<Level> dim, BlockPos pos) {}
    private static final Map<String, List<Handle>> BY_KEY = new ConcurrentHashMap<>();

    private RelayLinks() {}

    public static String key(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + "|" + stack.getComponentsPatch();
    }

    public static void index(WirelessRelayBlockEntity be) {
        drop(be);
        String key = key(be.link());
        if (key.isEmpty() || be.getLevel() == null) return;
        BY_KEY.computeIfAbsent(key, k -> new ArrayList<>()).add(new Handle(be.getLevel().dimension(), be.getBlockPos().immutable()));
    }

    public static void drop(WirelessRelayBlockEntity be) {
        if (be.getLevel() == null) return;
        Handle self = new Handle(be.getLevel().dimension(), be.getBlockPos());
        BY_KEY.values().forEach(list -> list.removeIf(h -> h.equals(self)));
    }

    public static WirelessRelayBlockEntity partner(WirelessRelayBlockEntity be) {
        String key = key(be.link());
        if (key.isEmpty() || be.getLevel() == null) return null;
        List<Handle> list = BY_KEY.get(key);
        if (list == null) return null;
        WirelessRelayBlockEntity best = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos here = be.getBlockPos();
        for (Handle handle : List.copyOf(list)) {
            if (handle.pos.equals(here) && handle.dim == be.getLevel().dimension()) continue;
            var dest = be.getLevel().getServer() == null ? null : be.getLevel().getServer().getLevel(handle.dim);
            if (dest == null || !dest.hasChunkAt(handle.pos)) continue;
            if (!(dest.getBlockEntity(handle.pos) instanceof WirelessRelayBlockEntity other)) continue;
            if (!key.equals(key(other.link()))) continue;
            double dist = handle.dim == be.getLevel().dimension() ? here.distSqr(handle.pos) : 0;
            if (best == null || dist < bestDist) { best = other; bestDist = dist; }
        }
        return best;
    }
}
