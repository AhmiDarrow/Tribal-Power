package tk.darrow.tribalpower.camp;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import tk.darrow.tribalpower.camp.identity.Camps;

import java.util.UUID;

/**
 * Shared owner-and-camp access for devices placed in a shared base (design 3.1 section 11).
 *
 * <p>Same rule the Summoning Cradle and Wayanchor already use: the placer, anyone sharing the placer's
 * camp, or everyone when the device was never claimed. Without this a shared base is griefable by its
 * own members.
 */
public final class Ownership {
    public static final String OWNER_KEY = "Owner";

    private Ownership() {}

    /** Owner recorded on placement, or null when the device was placed by something other than a player. */
    public static UUID of(LivingEntity placer) {
        return placer instanceof Player player ? player.getUUID() : null;
    }

    public static boolean canAccess(Level level, UUID owner, Player player) {
        if (owner == null || player == null) return true;
        if (player.getUUID().equals(owner)) return true;
        MinecraftServer server = level.getServer();
        return Camps.sameCamp(server == null ? ServerLifecycleHooks.getCurrentServer() : server, owner, player.getUUID());
    }

    /** Access check plus the refusal message, so callers do not each invent their own wording. */
    public static boolean check(Level level, UUID owner, Player player) {
        if (canAccess(level, owner, player)) return true;
        if (!level.isClientSide && player != null)
            player.displayClientMessage(Component.translatable("message.tribalpower.not_yours"), true);
        return false;
    }

    public static void save(CompoundTag tag, UUID owner) {
        if (owner != null) tag.putUUID(OWNER_KEY, owner);
    }

    public static UUID load(CompoundTag tag) {
        return tag.hasUUID(OWNER_KEY) ? tag.getUUID(OWNER_KEY) : null;
    }

    /** Whether {@code player} may break the device at {@code pos}, when it carries an owner. */
    public static boolean canBreak(Level level, BlockPos pos, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        return !(be instanceof Owned owned) || canAccess(level, owned.owner(), player);
    }

    /** A block entity that remembers who placed it. */
    public interface Owned {
        UUID owner();
        void setOwner(UUID owner);
    }
}
