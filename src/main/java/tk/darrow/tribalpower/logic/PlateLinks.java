package tk.darrow.tribalpower.logic;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import tk.darrow.tribalpower.camp.Ownership;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.logic.LogicPlateBlockEntity.Ear;

/**
 * Syncing song plates with the Totem Wrench. Crouch-use a plate to hold its song on the wrench; use the wrench on
 * another plate to make that plate hear the held one, as if a wire ran into the ear on the half you clicked. Use it
 * again on the same pair to unsync. With nothing held, the wrench reads a plate's links and traces them in sparks.
 *
 * <p>Links are one-way (source to listener) and stored on the listener only, so a plate's inputs are always
 * visible where they are used, a broken source is noticed by its listeners, and nothing has to be kept in step
 * between two block entities.
 *
 * <p>Ownership: syncing or unsyncing writes to the listener, so it needs the listener's owner or camp. Holding a
 * plate's song needs that plate's owner or camp too, and so does the held source when a new sync is made. A wire
 * into a plate has to be laid where its owner can see it, but a sync reaches through walls from up to the full
 * range and lives only on the listener, so the source's owner could neither see it nor cut it. Reading a plate's
 * syncs, and letting go of a held song, stay open to everyone.
 */
public final class PlateLinks {
    /** Sparks drawn along one traced link, so a long link stays cheap to show. */
    private static final int MAX_SPARKS = 48;

    private PlateLinks() {}

    public static GlobalPos held(ItemStack wrench) {
        return wrench.get(LogicRegistry.HELD_PLATE.get());
    }

    /** Server side only. {@code spend} charges the wrench for a new link, the way a turn does. */
    public static InteractionResult useWrench(ServerLevel level, Player player, ItemStack wrench, LogicPlateBlockEntity plate,
                                              Vec3 hit, Runnable spend) {
        BlockPos pos = plate.getBlockPos();
        if (player.isShiftKeyDown()) {
            GlobalPos here = GlobalPos.of(level.dimension(), pos);
            if (here.equals(held(wrench))) {
                wrench.remove(LogicRegistry.HELD_PLATE.get());
                player.displayClientMessage(Component.translatable("message.tribalpower.plate_link.released"), true);
                return InteractionResult.CONSUME;
            }
            if (!Ownership.check(level, plate.owner(), player)) return InteractionResult.FAIL;
            wrench.set(LogicRegistry.HELD_PLATE.get(), here);
            player.displayClientMessage(Component.translatable("message.tribalpower.plate_link.held",
                    level.getBlockState(pos).getBlock().getName(), pos.getX(), pos.getY(), pos.getZ()), true);
            level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.BLOCKS, 0.6F, 1.4F);
            return InteractionResult.CONSUME;
        }

        GlobalPos held = held(wrench);
        if (held == null || held.pos().equals(pos) && held.dimension().equals(level.dimension())) {
            show(level, player, plate);
            return InteractionResult.CONSUME;
        }
        if (!Ownership.check(level, plate.owner(), player)) return InteractionResult.FAIL;
        if (!held.dimension().equals(level.dimension())) {
            player.displayClientMessage(Component.translatable("message.tribalpower.plate_link.other_world"), true);
            return InteractionResult.FAIL;
        }
        BlockPos from = held.pos();
        if (plate.linkFrom(from) != null) {
            plate.unlink(from);
            player.displayClientMessage(Component.translatable("message.tribalpower.plate_link.unlinked",
                    from.getX(), from.getY(), from.getZ()), true);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 0.7F, 0.8F);
            return InteractionResult.CONSUME;
        }
        int range = TribalConfig.plateLinkRange();
        int distance = (int) Math.ceil(Math.sqrt(from.distSqr(pos)));
        if (from.distSqr(pos) > (double) range * range) {
            player.displayClientMessage(Component.translatable("message.tribalpower.plate_link.too_far", distance, range), true);
            return InteractionResult.FAIL;
        }
        if (!level.isLoaded(from) || !(level.getBlockEntity(from) instanceof LogicPlateBlockEntity source)) {
            wrench.remove(LogicRegistry.HELD_PLATE.get());
            player.displayClientMessage(Component.translatable("message.tribalpower.plate_link.gone"), true);
            return InteractionResult.FAIL;
        }
        // asked again here: the held plate may since have been broken and another camp's set down in its place
        if (!Ownership.check(level, source.owner(), player)) return InteractionResult.FAIL;
        if (plate.links().size() >= TribalConfig.plateLinkMax()) {
            player.displayClientMessage(Component.translatable("message.tribalpower.plate_link.full", plate.links().size()), true);
            return InteractionResult.FAIL;
        }
        Ear ear = earFor(plate.kind(), plate.getBlockState().getValue(LogicPlateBlock.FACING), pos, hit);
        if (ear == null) {
            player.displayClientMessage(Component.translatable("message.tribalpower.plate_link.deaf"), true);
            return InteractionResult.FAIL;
        }
        plate.link(from, ear);
        spend.run();
        player.displayClientMessage(Component.translatable("message.tribalpower.plate_link.linked",
                level.getBlockState(from).getBlock().getName(), from.getX(), from.getY(), from.getZ(), ear.label()), true);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 1.2F);
        if (player instanceof ServerPlayer viewer) trace(level, viewer, from, pos);
        return InteractionResult.CONSUME;
    }

    /**
     * The ear a click lands in: the half of the plate toward its left side or its right side, as a wire from that
     * side would come in. A plate that only hears its back takes any click there; a Tally's right half is its back
     * (count) and its left half the reset. A Heartbeat hears nothing, so null.
     */
    public static Ear earFor(LogicKind kind, Direction facing, BlockPos pos, Vec3 hit) {
        if (kind == LogicKind.HEARTBEAT) return null;
        Direction left = LogicPlateBlockEntity.earSide(facing, Ear.LEFT);
        double toward = (hit.x - (pos.getX() + 0.5)) * left.getStepX()
                + (hit.y - (pos.getY() + 0.5)) * left.getStepY()
                + (hit.z - (pos.getZ() + 0.5)) * left.getStepZ();
        Ear side = toward > 0 ? Ear.LEFT : Ear.RIGHT;
        return LogicPlateBlockEntity.hearsWith(kind, side) ? side : Ear.BACK;
    }

    /** Lists a plate's links in chat and traces each loaded one in sparks for the reader alone. */
    public static void show(ServerLevel level, Player player, LogicPlateBlockEntity plate) {
        if (plate.links().isEmpty()) {
            player.displayClientMessage(Component.translatable("message.tribalpower.plate_link.none"), true);
            return;
        }
        BlockPos pos = plate.getBlockPos();
        player.sendSystemMessage(Component.translatable("message.tribalpower.plate_link.header",
                level.getBlockState(pos).getBlock().getName(), plate.links().size(), TribalConfig.plateLinkMax())
                .withStyle(ChatFormatting.AQUA));
        for (Component line : plate.linkLines(level))
            player.sendSystemMessage(Component.literal("  · ").withStyle(ChatFormatting.DARK_AQUA).append(line.copy().withStyle(ChatFormatting.GRAY)));
        if (!(player instanceof ServerPlayer viewer)) return;
        int range = TribalConfig.plateLinkRange();
        for (LogicPlateBlockEntity.Link link : plate.links()) {
            if (link.source().distSqr(pos) <= (double) range * range && level.isLoaded(link.source()))
                trace(level, viewer, link.source(), pos);
        }
    }

    /** A faint line of sparks from a source plate to its listener, sent only to the one player reading it. */
    private static void trace(ServerLevel level, ServerPlayer viewer, BlockPos from, BlockPos to) {
        Vec3 start = Vec3.atCenterOf(from);
        Vec3 span = Vec3.atCenterOf(to).subtract(start);
        int sparks = Math.clamp((int) Math.ceil(span.length() * 2), 2, MAX_SPARKS);
        for (int i = 0; i <= sparks; i++) {
            Vec3 at = start.add(span.scale(i / (double) sparks));
            level.sendParticles(viewer, ParticleTypes.ELECTRIC_SPARK, true, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }
}
