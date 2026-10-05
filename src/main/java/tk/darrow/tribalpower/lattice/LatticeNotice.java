package tk.darrow.tribalpower.lattice;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import tk.darrow.tribalpower.api.pulse.PulseHandler;

/**
 * The lattice change, told once, and the one membership hook every block gets.
 *
 * <p>Pulse used to reach a machine from any generator within 8 blocks of it. Now it travels only along the
 * lattice (see {@link Weave}), so a base built the old way stops until a Lattice Conductor stands in reach of its
 * generators and machines. Every player who played before the change is told so once, on their next login; the
 * flag lives under {@link Player#PERSISTED_NBT_TAG} so death does not repeat it. A player with no play time yet
 * never had an old base, so they are only marked, not told.
 */
public final class LatticeNotice {
    /** Bumped only if a later change needs telling again. */
    public static final int VERSION = 1;
    public static final String KEY = "tribalpower_lattice_notice";

    private LatticeNotice() {}

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) greet(player);
    }

    /** Tells {@code player} about the lattice change if they have not been told; true when the message went out. */
    public static boolean greet(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.getInt(KEY) >= VERSION) return false;
        persisted.putInt(KEY, VERSION);
        root.put(Player.PERSISTED_NBT_TAG, persisted);
        if (player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) <= 0) return false;
        player.sendSystemMessage(Component.translatable("message.tribalpower.lattice_notice.title").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable("message.tribalpower.lattice_notice").withStyle(ChatFormatting.YELLOW));
        return true;
    }

    /**
     * Any Pulse block a player places (a third-party generator included) tells the lattice at once. Tribal Power's own
     * generators, cairns and totems also report themselves when they load or are set by a command.
     */
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) return;
        BlockEntity be = level.getBlockEntity(event.getPos());
        if (be instanceof PulseHandler) Weave.memberChanged(level, event.getPos());
    }
}
