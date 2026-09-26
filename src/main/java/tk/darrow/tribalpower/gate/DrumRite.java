package tk.darrow.tribalpower.gate;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import tk.darrow.tribalpower.blockentity.GateDrumBlockEntity;
import tk.darrow.tribalpower.storage.DeepCacheManager;
import tk.darrow.tribalpower.world.ModDimensions;

/**
 * The Gate Rite: a Gate Drum opens for whoever beats out its rhythm, and the rhythm is all the power it needs.
 * The server picks a seed; the client plays the composed track it names, 20 to 30 seconds of drums, drone,
 * flute and chant, and the drum hits in that music are the A, S, D and F beats to strike. The server looks up
 * the same track to check the result. Timing is judged on the client, so lag never costs a note, while the
 * server holds the rite to its real length and to the player standing at the drum.
 */
public final class DrumRite {
    /** Share of notes that must land for the portal to open. Stray presses count a little against you. */
    public static final double PASS = 0.60;
    /** Wall-clock like the client, so a server below 20 TPS still accepts a rite that really took its length. */
    private static final long SLACK_MS = 2000, EXPIRE_MS = 60_000;
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private DrumRite() {}

    public record Note(long timeMs, int lane) {}

    /**
     * A rite's music and beat: which track plays, its tempo, the count-in, how long it lasts and every drum hit
     * to strike, all taken from the composed track (GeneratedRiteTracks) the seed picks.
     */
    public record Pattern(int track, String sound, int bpm, long leadMs, long playMs, List<Note> notes) {
        public long endMs() {
            return leadMs + playMs;
        }

        public long beatMs() {
            return 60000L / bpm;
        }
    }

    public static int trackCount() {
        return GeneratedRiteTracks.TRACKS.length;
    }

    public static Pattern pattern(long seed) {
        int index = Math.floorMod(seed, GeneratedRiteTracks.TRACKS.length);
        var track = GeneratedRiteTracks.TRACKS[index];
        List<Note> notes = new ArrayList<>(track.times().length);
        for (int i = 0; i < track.times().length; i++) notes.add(new Note(track.times()[i], track.lanes()[i]));
        return new Pattern(index, track.sound(), track.bpm(), track.leadMs(), track.playMs(), List.copyOf(notes));
    }

    /** How a finished rite scores: landed notes less a quarter note per stray press, over all notes. */
    public static double accuracy(int hits, int strays, int total) {
        return total <= 0 ? 0 : Math.max(0, hits - strays * 0.25) / total;
    }

    // ------------------------------------------------------------------ server

    private record Session(BlockPos pos, long seed, long startMs) {}

    /** Starts a rite at a full drum. */
    public static void begin(ServerPlayer player, BlockPos pos) {
        long seed = player.getRandom().nextLong();
        SESSIONS.put(player.getUUID(), new Session(pos.immutable(), seed, Util.getMillis()));
        player.serverLevel().playSound(null, pos, tk.darrow.tribalpower.sound.ModSounds.GATE_HUM.get(), SoundSource.BLOCKS, 0.8F, 0.8F);
        PacketDistributor.sendToPlayer(player, new Start(pos, seed));
    }

    /** For tests: a session that began {@code ticksAgo} ticks ago. */
    public static long beginAt(ServerPlayer player, BlockPos pos, long seed, long ticksAgo) {
        SESSIONS.put(player.getUUID(), new Session(pos.immutable(), seed, Util.getMillis() - ticksAgo * 50));
        return seed;
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }

    /** Settles a rite. Returns true when the portal opened. */
    public static boolean finish(ServerPlayer player, Result result) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null || !session.pos.equals(result.pos) || session.seed != result.seed) return false;
        if (result.cancelled) {
            player.displayClientMessage(Component.translatable("message.tribalpower.gate.rite_stopped"), true);
            return false;
        }
        var level = player.serverLevel();
        Pattern pattern = pattern(session.seed);
        long elapsed = Util.getMillis() - session.startMs;
        long needed = pattern.endMs() - SLACK_MS;
        if (elapsed < needed || elapsed > needed + EXPIRE_MS
                || player.distanceToSqr(session.pos.getCenter()) > 8 * 8 || !(level.getBlockEntity(session.pos) instanceof GateDrumBlockEntity)) {
            player.displayClientMessage(Component.translatable("message.tribalpower.gate.rite_refused"), false);
            return false;
        }
        int total = pattern.notes().size();
        double accuracy = accuracy(Math.min(result.hits, total), Math.max(0, result.strays), total);
        int percent = (int) Math.round(accuracy * 100);
        if (accuracy < PASS) {
            player.displayClientMessage(Component.translatable("message.tribalpower.gate.rite_failed", percent, (int) (PASS * 100)), false);
            return false;
        }
        if (!ModDimensions.travelThroughGate(player)) {
            player.displayClientMessage(Component.translatable("message.tribalpower.gate.fail"), true);
            return false;
        }
        DeepCacheManager.markVisited(player);
        player.displayClientMessage(Component.translatable("message.tribalpower.gate.rite_opened", percent), true);
        return true;
    }

    // ------------------------------------------------------------------ network

    /** Server → client: begin the rite for this drum with this seed. */
    public record Start(BlockPos pos, long seed) implements CustomPacketPayload {
        public static final Type<Start> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("tribalpower", "drum_rite_start"));
        public static final StreamCodec<ByteBuf, Start> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Start::pos, ByteBufCodecs.VAR_LONG, Start::seed, Start::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client → server: how the rite went. */
    public record Result(BlockPos pos, long seed, int hits, int strays, boolean cancelled) implements CustomPacketPayload {
        public static final Type<Result> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("tribalpower", "drum_rite_result"));
        public static final StreamCodec<ByteBuf, Result> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Result::pos, ByteBufCodecs.VAR_LONG, Result::seed, ByteBufCodecs.VAR_INT, Result::hits,
                ByteBufCodecs.VAR_INT, Result::strays, ByteBufCodecs.BOOL, Result::cancelled, Result::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(Start.TYPE, Start.STREAM_CODEC, (payload, context) ->
                tk.darrow.tribalpower.client.SongkeeperPlayScreen.rite(payload.pos(), payload.seed()));
        registrar.playToServer(Result.TYPE, Result.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) finish(player, payload);
        });
    }
}
