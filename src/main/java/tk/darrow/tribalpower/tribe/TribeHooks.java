package tk.darrow.tribalpower.tribe;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Server hooks for the tribes: hearth registry (kill-near-hearth standing), camp block breaking, first-meeting
 * flags and command registration. Register the three event methods on the NeoForge bus.
 */
public final class TribeHooks {
    private static final Map<ServerLevel, Map<BlockPos, TribeDefinition>> HEARTHS = new WeakHashMap<>();
    private static final String MET_KEY = "tribalpower_tribes_met";

    private TribeHooks() {}

    // ---- hearth registry ----

    public static void hearth(ServerLevel level, BlockPos pos, TribeDefinition tribe, boolean active) {
        Map<BlockPos, TribeDefinition> map = HEARTHS.computeIfAbsent(level, l -> new HashMap<>());
        if (active) map.put(pos.immutable(), tribe); else map.remove(pos);
    }

    /** Distinct tribes with a loaded hearth within {@code radius} of {@code target}. */
    public static Set<TribeDefinition> hearthsNear(ServerLevel level, BlockPos target, int radius) {
        Set<TribeDefinition> tribes = new HashSet<>();
        Map<BlockPos, TribeDefinition> map = HEARTHS.get(level);
        if (map == null) return tribes;
        map.entrySet().removeIf(e -> !level.hasChunkAt(e.getKey()) || !(level.getBlockEntity(e.getKey()) instanceof TribeHearthBlockEntity));
        for (var e : map.entrySet())
            if (e.getKey().distSqr(target) <= (double) radius * radius) tribes.add(e.getValue());
        return tribes;
    }

    // ---- first meeting ----

    /** Met flags live under {@link Player#PERSISTED_NBT_TAG} so they survive death and dimension changes. */
    public static boolean hasMet(ServerPlayer player, TribeDefinition tribe) {
        CompoundTag tag = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return (tag.getInt(MET_KEY) & (1 << tribe.ordinal())) != 0;
    }

    public static void markMet(ServerPlayer player, TribeDefinition tribe) {
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putInt(MET_KEY, persisted.getInt(MET_KEY) | (1 << tribe.ordinal()));
        data.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    // ---- events ----

    /** Killing a hostile within 24 blocks of a hearth: +1 standing with that hearth's tribe (capped daily). */
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Enemy) || !(event.getEntity().level() instanceof ServerLevel level)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        for (TribeDefinition tribe : hearthsNear(level, event.getEntity().blockPosition(), TribeStanding.KILL_RADIUS))
            TribeStanding.killGain(player, tribe);
    }

    /** Breaking camp blocks costs standing: hearth -40, banner -5. */
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) return;
        BlockState state = event.getState();
        if (state.is(TribeRegistry.TRIBE_HEARTH.get())) {
            if (event.getLevel().getBlockEntity(event.getPos()) instanceof TribeHearthBlockEntity hearth) {
                TribeStanding.add(player, hearth.tribe(), TribeStanding.LOSS_HEARTH);
                angerCamp(player.serverLevel(), event.getPos(), hearth.tribe(), player);
            }
        } else if (state.is(TribeRegistry.TRIBE_BANNER.get())) {
            TribeStanding.add(player, TribeDefinition.byOrdinal(state.getValue(TribeBannerBlock.TRIBE)), TribeStanding.LOSS_CAMP_BLOCK);
        }
    }

    public static void onCommands(RegisterCommandsEvent event) {
        TribeCommands.register(event.getDispatcher());
    }

    /** Anger every Hunter of the tribe near {@code pos} at {@code player} (used when camp blocks are broken). */
    public static void angerCamp(ServerLevel level, BlockPos pos, TribeDefinition tribe, Player player) {
        for (TribalKinEntity kin : level.getEntitiesOfClass(TribalKinEntity.class, TribalKinEntity.campBox(pos, 24), k -> k.tribe() == tribe))
            kin.setAngryAt(player, TribeStanding.HUNTER_ANGER_TICKS);
    }
}
