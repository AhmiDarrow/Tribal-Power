package tk.darrow.tribalpower.api;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.api.pulse.PulseHandler;
import tk.darrow.tribalpower.blockentity.DrumheartBlockEntity;
import tk.darrow.tribalpower.blockentity.LeyCollectorBlockEntity;
import tk.darrow.tribalpower.blockentity.PulseResonatorBlockEntity;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.lattice.LatticeNetwork;
import tk.darrow.tribalpower.ley.LeyMath;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Codex diagnostics: sneak-use the Spirit Codex on any Tribal block entity for a chat report.
 * The generic header works for every {@link PulseHandler}; blocks implementing {@link Diagnosable}
 * add their own lines. Wire {@link #onRightClickBlock} on the NeoForge event bus.
 */
public final class Diagnostics {
    public static final int RADIUS = LatticeNetwork.DEFAULT_RADIUS;

    private Diagnostics() {}

    /** Sneak + Spirit Codex + block entity → report (server) and swallow the click on both sides. */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!player.isShiftKeyDown() || !event.getItemStack().is(ModItems.SPIRIT_CODEX.get())) return;
        if (event.getLevel().getBlockEntity(event.getPos()) == null) return;
        if (event.getLevel() instanceof ServerLevel server) {
            for (Component line : report(server, event.getPos())) player.sendSystemMessage(line);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /** Full report: header, generic Pulse context, then the block's own diagnosis. */
    public static List<Component> report(ServerLevel level, BlockPos pos) {
        List<Component> lines = new ArrayList<>();
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            lines.add(Component.translatable("diag.tribalpower.no_block_entity").withStyle(ChatFormatting.RED));
            return lines;
        }
        lines.add(Component.translatable("diag.tribalpower.header", level.getBlockState(pos).getBlock().getName(),
                pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.AQUA));
        if (be instanceof PulseHandler handler) {
            lines.add(bullet(Component.translatable("diag.tribalpower.stored", handler.getPulseStored(), handler.getPulseCapacity())));
        } else if (be instanceof tk.darrow.tribalpower.camp.CampBlockEntity camp) {
            lines.add(bullet(Component.translatable("diag.tribalpower.stored", camp.pulse, tk.darrow.tribalpower.camp.CampBlockEntity.CAPACITY)));
            lines.add(bullet(camp.status()));
        }
        if (level.hasNeighborSignal(pos)) {
            lines.add(bullet(Component.translatable("diag.tribalpower.paused").withStyle(ChatFormatting.RED)));
        }
        List<Component> generators = generators(level, pos);
        if (generators.isEmpty()) lines.add(bullet(Component.translatable("diag.tribalpower.no_generators").withStyle(ChatFormatting.YELLOW)));
        else {
            lines.add(bullet(Component.translatable("diag.tribalpower.generators", generators.size())));
            for (Component line : generators) lines.add(Component.literal("    ").append(line));
        }
        int available = LatticeNetwork.extractPulseNearby(level, pos, RADIUS, Integer.MAX_VALUE / 2, true);
        lines.add(bullet(Component.translatable("diag.tribalpower.available", available)));
        Set<Attunement> voices = LatticeNetwork.collectAttunements(level, pos, RADIUS);
        if (voices.isEmpty()) lines.add(bullet(Component.translatable("diag.tribalpower.no_attunements").withStyle(ChatFormatting.YELLOW)));
        else {
            MutableComponent list = Component.empty();
            boolean first = true;
            for (Attunement voice : voices) {
                if (!first) list.append(", ");
                list.append(Component.translatable("attunement.tribalpower." + voice.getSerializedName()));
                first = false;
            }
            lines.add(bullet(Component.translatable("diag.tribalpower.attunements", list)));
        }
        if (be instanceof Diagnosable diagnosable) {
            for (Component line : diagnosable.diagnose(level, pos)) lines.add(bullet(line));
        }
        return lines;
    }

    /** Nearby generators with their output this second. */
    public static List<Component> generators(ServerLevel level, BlockPos origin) {
        List<Component> lines = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dy = -RADIUS; dy <= RADIUS; dy++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockEntity be = level.hasChunkAt(cursor) ? level.getBlockEntity(cursor) : null;
                    if (be == null) continue;
                    String perSecond;
                    if (be instanceof DrumheartBlockEntity) perSecond = "beat";
                    else if (be instanceof LeyCollectorBlockEntity) {
                        perSecond = level.hasNeighborSignal(cursor) ? "0" : String.format("%.1f", LeyMath.gain(level, cursor) * 20.0 / LeyCollectorBlockEntity.GAIN_INTERVAL);
                    } else if (be instanceof PulseResonatorBlockEntity resonator) perSecond = Integer.toString(resonator.getGain());
                    else if (be instanceof tk.darrow.tribalpower.api.pulse.PulseGenerator generator)
                        perSecond = Integer.toString(generator.currentOutput());
                    else continue;
                    PulseHandler handler = (PulseHandler) be;
                    lines.add(Component.translatable("diag.tribalpower.generator", be.getBlockState().getBlock().getName(),
                            (int) Math.round(Math.sqrt(origin.distSqr(cursor))), handler.getPulseStored(), handler.getPulseCapacity(), perSecond));
                }
            }
        }
        return lines;
    }

    private static Component bullet(Component body) {
        Component styled = body.getStyle().getColor() == null ? body.copy().withStyle(ChatFormatting.GRAY) : body;
        return Component.literal("  · ").withStyle(ChatFormatting.DARK_AQUA).append(styled);
    }
}
