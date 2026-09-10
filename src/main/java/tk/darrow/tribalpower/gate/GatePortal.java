package tk.darrow.tribalpower.gate;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.effect.SpiritEffects;
import tk.darrow.tribalpower.storage.DeepCacheManager;
import tk.darrow.tribalpower.world.ModDimensions;
import tk.darrow.tribalpower.world.TravelSafety;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The plane itself, and what happens when something walks into it (design 3.1 section 8).
 *
 * <p>A gate never terraforms: arrival is always inside the destination gate's own interior, and if that
 * interior will not hold a traveller the transit refuses and the Pulse is refunded. The destination is
 * held loaded for {@link #TICKET_TICKS} around the arrival, so an unloaded partner still works -- and if
 * that ticket cannot be granted, the transit refuses rather than dropping anyone into an ungenerated
 * chunk.
 */
public final class GatePortal {
    /** Long enough for the traveller to arrive and the chunk to settle, short enough not to be an anchor. */
    public static final int TICKET_TICKS = 200;

    public static final TicketType<ChunkPos> TICKET =
            TicketType.create("tribalpower_gate", Comparator.comparingLong(ChunkPos::toLong), TICKET_TICKS);

    /** Enough to stop a traveller bouncing back through the plane they just left. */
    public static final int WAY_COOLDOWN = 20;

    private GatePortal() {}

    // ---- the plane -------------------------------------------------------------------------

    /** The interior cells of a gate, in world space, honouring the rotation its frame was built in. */
    public static List<BlockPos> interior(Level level, GateKeystoneBlockEntity keystone,
                                          @Nullable GateKeystoneBlockEntity.Kind kind) {
        List<BlockPos> cells = new ArrayList<>();
        if (kind == null) return cells;
        Rotation rotation = keystone.match(level).rotation();
        BlockPos anchor = keystone.getBlockPos();
        int height = kind == GateKeystoneBlockEntity.Kind.FAR ? 4 : 3;
        for (int y = 1; y <= height; y++) {
            for (int across = -1; across <= 1; across++) {
                cells.add(anchor.offset(rotate(across, rotation), y, rotateZ(across, rotation)));
            }
        }
        return cells;
    }

    /** The written gate runs along X, so a local across-offset is (x, 0) before rotation. */
    private static int rotate(int across, Rotation rotation) {
        return switch (rotation) {
            case NONE -> across;
            case CLOCKWISE_180 -> -across;
            case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> 0;
        };
    }

    private static int rotateZ(int across, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> across;
            case COUNTERCLOCKWISE_90 -> -across;
            case NONE, CLOCKWISE_180 -> 0;
        };
    }

    public static void fill(ServerLevel level, GateKeystoneBlockEntity keystone, @Nullable GateKeystoneBlockEntity.Kind kind) {
        BlockState portal = GateRegistry.GATE_PORTAL.get().defaultBlockState();
        for (BlockPos cell : interior(level, keystone, kind)) {
            BlockState state = level.getBlockState(cell);
            if (state.isAir()) level.setBlock(cell, portal, 3);
        }
    }

    public static void clear(ServerLevel level, GateKeystoneBlockEntity keystone) {
        for (GateKeystoneBlockEntity.Kind kind : GateKeystoneBlockEntity.Kind.values())
            for (BlockPos cell : interior(level, keystone, kind))
                if (level.getBlockState(cell).is(GateRegistry.GATE_PORTAL.get())) level.removeBlock(cell, false);
    }

    /** The keystone that owns the plane at {@code pos}: always below and within one block across. */
    @Nullable
    public static GateKeystoneBlockEntity keystoneFor(Level level, BlockPos pos) {
        for (int dy = 1; dy <= 5; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos candidate = pos.offset(dx, -dy, dz);
                    if (!level.hasChunkAt(candidate)) continue;
                    if (level.getBlockEntity(candidate) instanceof GateKeystoneBlockEntity keystone) return keystone;
                }
            }
        }
        return null;
    }

    // ---- transit ---------------------------------------------------------------------------

    /** Walks {@code entity} through the plane at {@code pos}, if everything a gate needs is true. */
    public static void onEntityInside(ServerLevel level, BlockPos pos, Entity entity) {
        if (entity.isPassenger() || entity.isVehicle() || entity.isOnPortalCooldown()) return;
        GateKeystoneBlockEntity keystone = keystoneFor(level, pos);
        if (keystone == null || !keystone.lit() || keystone.stilled()) return;
        GateKeystoneBlockEntity.Kind kind = keystone.kind(level);
        if (kind == null) return;

        GateSavedData.Gate partner = keystone.partner(level);
        if (partner == null) {
            if (entity instanceof ServerPlayer player)
                player.displayClientMessage(Component.translatable("message.tribalpower.gate.thread_empty"), true);
            entity.setPortalCooldown(WAY_COOLDOWN);
            return;
        }

        ServerLevel destination = level.getServer().getLevel(
                ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                        ResourceLocation.parse(partner.dimension())));
        if (destination == null) return;
        if (kind == GateKeystoneBlockEntity.Kind.FAR && !tk.darrow.tribalpower.config.TribalConfig.farGatesEnabled()) return;

        if (!keystone.spendTravel(kind)) {
            if (entity instanceof ServerPlayer player)
                player.displayClientMessage(Component.translatable("message.tribalpower.gate.need_pulse",
                        kind.travelCost(), keystone.getPulseStored()), true);
            entity.setPortalCooldown(WAY_COOLDOWN);
            return;
        }

        BlockPos landing = arrival(destination, partner);
        if (landing == null) {
            keystone.refund(kind);
            entity.setPortalCooldown(WAY_COOLDOWN);
            if (entity instanceof ServerPlayer player)
                player.displayClientMessage(Component.translatable("message.tribalpower.gate.blocked"), true);
            return;
        }

        Vec3 target = new Vec3(landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5);
        Entity moved = entity;
        if (destination == level) {
            entity.teleportTo(target.x, target.y, target.z);
        } else {
            moved = entity.changeDimension(new DimensionTransition(destination, target, Vec3.ZERO,
                    entity.getYRot(), entity.getXRot(), DimensionTransition.DO_NOTHING));
            if (moved == null) {
                keystone.refund(kind);
                entity.setPortalCooldown(WAY_COOLDOWN);
                return;
            }
        }
        moved.setPortalCooldown(Math.max(WAY_COOLDOWN, kind.cooldownTicks()));

        keystone.markTransit();
        if (destination.getBlockEntity(partner.pos()) instanceof GateKeystoneBlockEntity far) far.markTransit();
        celebrate(level, pos);
        celebrate(destination, landing);

        // A Far Gate arriving in The March counts as having been there, exactly like the Gate Drum.
        if (moved instanceof ServerPlayer player) {
            if (destination.dimension().equals(ModDimensions.THE_MARCH)) DeepCacheManager.markVisited(player);
            if (destination != level)
                tk.darrow.tribalpower.camp.CampHooks.award(destination, player.getUUID(), "journey/long_thread");
        }
    }

    /**
     * A standing spot inside the destination gate, or null when there is none. Takes a chunk ticket first:
     * without it an unloaded partner would not answer, and if the ticket cannot be granted nobody travels.
     */
    @Nullable
    public static BlockPos arrival(ServerLevel destination, GateSavedData.Gate partner) {
        ChunkPos chunk = new ChunkPos(partner.pos());
        destination.getChunkSource().addRegionTicket(TICKET, chunk, 2, chunk);
        if (destination.getChunkSource().getChunk(chunk.x, chunk.z, true) == null) return null;
        if (!(destination.getBlockEntity(partner.pos()) instanceof GateKeystoneBlockEntity keystone)) return null;
        GateKeystoneBlockEntity.Kind kind = keystone.kind(destination);
        if (kind == null) return null;
        for (BlockPos cell : interior(destination, keystone, kind)) {
            BlockPos feet = cell;
            BlockPos head = cell.above();
            if (!TravelSafety.withinBounds(destination, feet)) continue;
            if (TravelSafety.hasHazard(destination, feet)) continue;
            if (!passable(destination, feet) || !passable(destination, head)) continue;
            return feet;
        }
        return null;
    }

    private static boolean passable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return (state.isAir() || state.is(GateRegistry.GATE_PORTAL.get()))
                && state.getCollisionShape(level, pos).isEmpty();
    }

    private static void celebrate(ServerLevel level, BlockPos pos) {
        SpiritEffects.ring(level, pos.getCenter(), Attunement.LOOM, 0.9, 16);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASEDRUM.value(),
                net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 0.6F);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.RESPAWN_ANCHOR_CHARGE,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.35F, 1.6F);
    }
}
