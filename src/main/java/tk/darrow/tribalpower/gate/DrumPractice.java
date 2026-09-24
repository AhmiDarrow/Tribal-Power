package tk.darrow.tribalpower.gate;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * The Songkeeper Drum: the Gate Rite as a game and nothing more. It plays any of the pack's music -- the six rite
 * tracks and the Drum Circle -- keeps each player's best score for each, and a board of the five best on the server.
 * The client drums and judges timing exactly as for the rite; the server holds each go to its real length.
 */
public final class DrumPractice {
    private static final long SLACK_MS = 2000, EXPIRE_MS = 60_000;
    public static final int BOARD = 5;
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private DrumPractice() {}

    private record Session(BlockPos pos, int track, long startMs) {}

    // ---- the tracks ---------------------------------------------------------------------------------------------

    /** Every track the drum knows: the rite tracks, then the Drum Circle. */
    public static int trackCount() {
        return GeneratedRiteTracks.TRACKS.length + 1;
    }

    public static DrumRite.Pattern pattern(int track) {
        if (track < GeneratedRiteTracks.TRACKS.length) return DrumRite.pattern(track);
        return drumCircle(track);
    }

    /**
     * The Drum Circle disc, charted from how it was composed (tools/generate_3_1_disc.py): 90 beats a minute, four
     * wake beats that serve as the count-in, then a circle of drums for 28 bars -- the deep drum on one, a slap on two
     * and four, the low drum on three, the rattle closing every fourth bar -- and the four-beat resync to finish.
     */
    private static DrumRite.Pattern drumCircle(int track) {
        double beat = 60000.0 / 90;
        List<DrumRite.Note> notes = new ArrayList<>();
        for (int bar = 4; bar < 32; bar++) {
            int[] lanes = {0, 2, 1, bar % 4 == 3 ? 3 : 2};
            for (int b = 0; b < 4; b++) notes.add(new DrumRite.Note(Math.round((bar * 4 + b) * beat), lanes[b]));
        }
        for (int b = 0; b < 4; b++) notes.add(new DrumRite.Note(Math.round((32 * 4 + b) * beat), b));
        long lead = Math.round(16 * beat);
        long play = Math.round((32 * 4 + 4) * beat) - lead;
        return new DrumRite.Pattern(track, "music_disc.drum_circle", 90, lead, play, List.copyOf(notes));
    }

    /** A go's points: every note landed, perfect ones more, a streak adds to it, a stray costs. */
    public static int points(int hits, int perfects, int bestCombo, int strays) {
        return Math.max(0, hits * 100 + perfects * 50 + bestCombo * 20 - strays * 50);
    }

    // ---- server -------------------------------------------------------------------------------------------------

    /** Opens the drum's track list for a player, with their bests and the board. */
    public static void browse(ServerPlayer player, BlockPos pos) {
        Scores scores = Scores.get(player);
        List<TrackScore> rows = new ArrayList<>();
        for (int track = 0; track < trackCount(); track++) {
            var board = scores.board(track);
            List<String> names = new ArrayList<>();
            List<Integer> points = new ArrayList<>();
            for (Best best : board) {
                names.add(best.name);
                points.add(best.points);
            }
            Best mine = scores.best(track, player.getUUID());
            rows.add(new TrackScore(track, mine == null ? 0 : mine.points, mine == null ? 0 : mine.accuracy, names, points));
        }
        PacketDistributor.sendToPlayer(player, new Browse(pos, rows));
    }

    public static void play(ServerPlayer player, BlockPos pos, int track) {
        if (track < 0 || track >= trackCount() || player.distanceToSqr(pos.getCenter()) > 8 * 8
                || !player.level().getBlockState(pos).is(tk.darrow.tribalpower.kit.KitRegistry.SONGKEEPER_DRUM.get())) return;
        SESSIONS.put(player.getUUID(), new Session(pos.immutable(), track, Util.getMillis()));
        PacketDistributor.sendToPlayer(player, new Start(pos, track));
    }

    public static void loggedOut(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }

    /** For tests: a go that began {@code ticksAgo} ticks ago. */
    public static void beginAt(ServerPlayer player, BlockPos pos, int track, long ticksAgo) {
        SESSIONS.put(player.getUUID(), new Session(pos.immutable(), track, Util.getMillis() - ticksAgo * 50));
    }

    /** Settles a go. Returns the points recorded, or -1 when it was refused. */
    public static int finish(ServerPlayer player, Result result) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null || !session.pos.equals(result.pos) || session.track != result.track || result.cancelled) return -1;
        DrumRite.Pattern pattern = pattern(session.track);
        long elapsed = Util.getMillis() - session.startMs;
        long needed = pattern.endMs() - SLACK_MS;
        if (elapsed < needed || elapsed > needed + EXPIRE_MS || player.distanceToSqr(session.pos.getCenter()) > 8 * 8) {
            player.displayClientMessage(Component.translatable("message.tribalpower.practice.refused"), false);
            return -1;
        }
        int total = pattern.notes().size();
        int hits = Math.min(result.hits, total);
        int perfects = Math.min(result.perfects, hits);
        int combo = Math.min(result.bestCombo, hits);
        int points = points(hits, perfects, combo, Math.max(0, result.strays));
        int accuracy = (int) Math.round(DrumRite.accuracy(hits, Math.max(0, result.strays), total) * 100);
        Scores scores = Scores.get(player);
        Best before = scores.best(session.track, player.getUUID());
        boolean personal = before == null || points > before.points;
        if (personal) scores.record(session.track, player.getUUID(), new Best(player.getGameProfile().getName(), points, accuracy));
        int place = scores.place(session.track, player.getUUID());
        player.displayClientMessage(Component.translatable(personal ? "message.tribalpower.practice.best" : "message.tribalpower.practice.done",
                Component.translatable("gui.tribalpower.practice.track." + session.track), points, accuracy), false);
        if (personal && place >= 0 && place < BOARD)
            player.displayClientMessage(Component.translatable("message.tribalpower.practice.board", place + 1), false);
        return points;
    }

    // ---- scores -------------------------------------------------------------------------------------------------

    public record Best(String name, int points, int accuracy) {}

    public static final class Scores extends SavedData {
        private final Map<Integer, Map<UUID, Best>> tracks = new HashMap<>();

        public static Scores get(ServerPlayer player) {
            return player.getServer().overworld().getDataStorage()
                    .computeIfAbsent(new SavedData.Factory<>(Scores::new, Scores::load), "tribalpower_drum_scores");
        }

        public Best best(int track, UUID player) {
            return tracks.getOrDefault(track, Map.of()).get(player);
        }

        void record(int track, UUID player, Best best) {
            tracks.computeIfAbsent(track, t -> new HashMap<>()).put(player, best);
            setDirty();
        }

        public List<Best> board(int track) {
            return tracks.getOrDefault(track, Map.of()).values().stream()
                    .sorted(Comparator.comparingInt(Best::points).reversed()).limit(BOARD).toList();
        }

        /** Where a player stands on a track's board, from 0, or -1. */
        public int place(int track, UUID player) {
            Best mine = best(track, player);
            if (mine == null) return -1;
            return (int) tracks.getOrDefault(track, Map.of()).values().stream().filter(b -> b.points > mine.points).count();
        }

        public static Scores load(CompoundTag tag, HolderLookup.Provider registries) {
            Scores scores = new Scores();
            ListTag list = tag.getList("Scores", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                scores.tracks.computeIfAbsent(entry.getInt("Track"), t -> new HashMap<>())
                        .put(entry.getUUID("Player"), new Best(entry.getString("Name"), entry.getInt("Points"), entry.getInt("Accuracy")));
            }
            return scores;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            tracks.forEach((track, bests) -> bests.forEach((player, best) -> {
                CompoundTag entry = new CompoundTag();
                entry.putInt("Track", track);
                entry.putUUID("Player", player);
                entry.putString("Name", best.name);
                entry.putInt("Points", best.points);
                entry.putInt("Accuracy", best.accuracy);
                list.add(entry);
            }));
            tag.put("Scores", list);
            return tag;
        }
    }

    // ---- network ------------------------------------------------------------------------------------------------

    public record TrackScore(int track, int best, int accuracy, List<String> names, List<Integer> points) {
        static final StreamCodec<ByteBuf, TrackScore> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, TrackScore::track, ByteBufCodecs.VAR_INT, TrackScore::best, ByteBufCodecs.VAR_INT, TrackScore::accuracy,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), TrackScore::names,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), TrackScore::points, TrackScore::new);
    }

    /** Server → client: the track list with scores. */
    public record Browse(BlockPos pos, List<TrackScore> tracks) implements CustomPacketPayload {
        public static final Type<Browse> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("tribalpower", "drum_practice_browse"));
        public static final StreamCodec<ByteBuf, Browse> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Browse::pos, TrackScore.CODEC.apply(ByteBufCodecs.list()), Browse::tracks, Browse::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client → server: play this track. */
    public record Play(BlockPos pos, int track) implements CustomPacketPayload {
        public static final Type<Play> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("tribalpower", "drum_practice_play"));
        public static final StreamCodec<ByteBuf, Play> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Play::pos, ByteBufCodecs.VAR_INT, Play::track, Play::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server → client: begin drumming this track. */
    public record Start(BlockPos pos, int track) implements CustomPacketPayload {
        public static final Type<Start> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("tribalpower", "drum_practice_start"));
        public static final StreamCodec<ByteBuf, Start> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Start::pos, ByteBufCodecs.VAR_INT, Start::track, Start::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client → server: how the go went. */
    public record Result(BlockPos pos, int track, int hits, int perfects, int bestCombo, int strays, boolean cancelled)
            implements CustomPacketPayload {
        public static final Type<Result> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("tribalpower", "drum_practice_result"));
        public static final StreamCodec<ByteBuf, Result> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public Result decode(ByteBuf buf) {
                return new Result(BlockPos.STREAM_CODEC.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.BOOL.decode(buf));
            }

            @Override
            public void encode(ByteBuf buf, Result result) {
                BlockPos.STREAM_CODEC.encode(buf, result.pos);
                ByteBufCodecs.VAR_INT.encode(buf, result.track);
                ByteBufCodecs.VAR_INT.encode(buf, result.hits);
                ByteBufCodecs.VAR_INT.encode(buf, result.perfects);
                ByteBufCodecs.VAR_INT.encode(buf, result.bestCombo);
                ByteBufCodecs.VAR_INT.encode(buf, result.strays);
                ByteBufCodecs.BOOL.encode(buf, result.cancelled);
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(Browse.TYPE, Browse.STREAM_CODEC, (payload, context) ->
                tk.darrow.tribalpower.client.SongkeeperScreen.open(payload.pos(), payload.tracks()));
        registrar.playToClient(Start.TYPE, Start.STREAM_CODEC, (payload, context) ->
                tk.darrow.tribalpower.client.DrumRiteScreen.practice(payload.pos(), payload.track()));
        registrar.playToServer(Play.TYPE, Play.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) play(player, payload.pos(), payload.track());
        });
        registrar.playToServer(Result.TYPE, Result.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) finish(player, payload);
        });
    }
}
