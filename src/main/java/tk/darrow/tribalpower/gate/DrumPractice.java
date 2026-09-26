package tk.darrow.tribalpower.gate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import tk.darrow.tribalpower.gate.Songbook.Difficulty;
import tk.darrow.tribalpower.gate.Songbook.Song;

/**
 * The Songkeeper Drum: a rhythm game on every song in the {@link Songbook}, at four difficulties, with a best score
 * per player and a board of the five best per song and difficulty. The client drums and judges its own timing (lag
 * never costs a note); the server holds each go to the song's real length and caps the score at what the chart
 * allows.
 *
 * <p>Two drums standing side by side make a duelling pair: a player at one challenges whoever stands at the other,
 * both play the same song at once, each sees the other's score climb, and the higher score takes the duel.
 */
public final class DrumPractice {
    private static final long SLACK_MS = 2500, EXPIRE_MS = 90_000, INVITE_MS = 30_000;
    public static final int BOARD = 5;
    /** How near a drum a player must stand to play it, and to be its duellist. */
    public static final double REACH = 6;
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<Integer, Duel> DUELS = new ConcurrentHashMap<>();
    private static final AtomicInteger NEXT_DUEL = new AtomicInteger(1);

    /** The seven tracks the drum knew before the Songbook, in their old order, so their scores carry over (as Hard). */
    private static final String[] LEGACY = {"waking_beat", "ember_walk", "reed_dance", "stone_circle", "storm_call", "looms_pull", "drum_circle"};

    private DrumPractice() {}

    private record Session(BlockPos pos, int song, Difficulty difficulty, long startMs, int duel) {}

    private static final class Duel {
        final int id;
        final UUID challenger, rival;
        final BlockPos challengerPos, rivalPos;
        final int song;
        final Difficulty difficulty;
        final long invitedMs;
        boolean started;
        final Map<UUID, Long> scores = new HashMap<>();
        final Map<UUID, Boolean> forfeits = new HashMap<>();

        Duel(int id, UUID challenger, UUID rival, BlockPos challengerPos, BlockPos rivalPos, int song, Difficulty difficulty) {
            this.id = id;
            this.challenger = challenger;
            this.rival = rival;
            this.challengerPos = challengerPos;
            this.rivalPos = rivalPos;
            this.song = song;
            this.difficulty = difficulty;
            this.invitedMs = Util.getMillis();
        }

        UUID other(UUID player) {
            return player.equals(challenger) ? rival : challenger;
        }
    }

    /** Only to a client that has agreed to the drum's packets (a mock player in a test, or a client without the mod, has not). */
    private static void send(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection != null && player.connection.hasChannel(payload)) PacketDistributor.sendToPlayer(player, payload);
    }

    private static String key(Song song, Difficulty difficulty) {
        return song.id() + "|" + difficulty.key();
    }

    public static boolean isDrum(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(tk.darrow.tribalpower.kit.KitRegistry.SONGKEEPER_DRUM.get());
    }

    /** The drums standing beside this one (within two blocks across and one up or down): its duelling partners. */
    public static List<BlockPos> partners(Level level, BlockPos pos) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos at : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 1, 2))) {
            if (!at.equals(pos) && isDrum(level, at)) out.add(at.immutable());
        }
        return out;
    }

    /** Who stands at a partner drum (nearer it than this one), free to be challenged. */
    public static List<ServerPlayer> rivals(ServerPlayer player, BlockPos pos) {
        List<ServerPlayer> out = new ArrayList<>();
        for (BlockPos other : partners(player.level(), pos)) {
            for (ServerPlayer candidate : player.serverLevel().players()) {
                if (candidate == player || out.contains(candidate) || SESSIONS.containsKey(candidate.getUUID())) continue;
                double there = candidate.distanceToSqr(other.getCenter()), here = candidate.distanceToSqr(pos.getCenter());
                if (there <= REACH * REACH && there < here) out.add(candidate);
            }
        }
        return out;
    }

    private static BlockPos drumOf(ServerPlayer player, BlockPos near) {
        for (BlockPos other : partners(player.level(), near))
            if (player.distanceToSqr(other.getCenter()) <= REACH * REACH) return other;
        return null;
    }

    private static boolean at(ServerPlayer player, BlockPos pos) {
        return player.distanceToSqr(pos.getCenter()) <= REACH * REACH && isDrum(player.level(), pos);
    }

    // ---- server: browsing and playing ---------------------------------------------------------------------------

    /** Opens the drum's song list for a player: their bests, their duel record, and who is at a partner drum. */
    public static void browse(ServerPlayer player, BlockPos pos) {
        Scores scores = Scores.get(player);
        List<Song> songs = Songbook.songs();
        int[] bests = new int[songs.size() * 4];
        byte[] stars = new byte[songs.size() * 4];
        for (Song song : songs) {
            for (Difficulty difficulty : Difficulty.values()) {
                Best mine = scores.best(key(song, difficulty), player.getUUID());
                int slot = song.index() * 4 + difficulty.ordinal();
                bests[slot] = mine == null ? 0 : mine.points;
                stars[slot] = (byte) (mine == null ? -1 : mine.stars + (mine.fullCombo ? 10 : 0));
            }
        }
        int[] record = scores.record(player.getUUID());
        List<String> rivals = rivals(player, pos).stream().map(p -> p.getGameProfile().getName()).toList();
        send(player, new Browse(pos, bests, stars, record[0], record[1], rivals, !partners(player.level(), pos).isEmpty()));
    }

    public static void board(ServerPlayer player, int songIndex, Difficulty difficulty) {
        Song song = Songbook.song(songIndex);
        if (song == null) return;
        List<Best> board = Scores.get(player).board(key(song, difficulty));
        send(player, new Board(songIndex, difficulty.ordinal(),
                board.stream().map(Best::name).toList(), board.stream().map(b -> b.points).toList()));
    }

    public static void play(ServerPlayer player, BlockPos pos, int songIndex, Difficulty difficulty) {
        Song song = Songbook.song(songIndex);
        if (song == null || !at(player, pos) || SESSIONS.containsKey(player.getUUID())) return;
        begin(player, pos, song, difficulty, 0, "");
    }

    private static void begin(ServerPlayer player, BlockPos pos, Song song, Difficulty difficulty, int duel, String opponent) {
        SESSIONS.put(player.getUUID(), new Session(pos.immutable(), song.index(), difficulty, Util.getMillis(), duel));
        send(player, new Start(pos, song.index(), difficulty.ordinal(), duel, opponent));
    }

    public static void loggedOut(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        Session session = SESSIONS.remove(id);
        if (session != null && session.duel != 0 && event.getEntity().getServer() != null) {
            Duel duel = DUELS.get(session.duel);
            if (duel != null) {
                duel.forfeits.put(id, true);
                duel.scores.putIfAbsent(id, 0L);
                settle(event.getEntity().getServer(), duel);
            }
        }
    }

    /** For tests: a go that began {@code ticksAgo} ticks ago. */
    public static void beginAt(ServerPlayer player, BlockPos pos, int songIndex, Difficulty difficulty, long ticksAgo) {
        SESSIONS.put(player.getUUID(), new Session(pos.immutable(), songIndex, difficulty, Util.getMillis() - ticksAgo * 50, 0));
    }

    /** For tests: pretend the player's go (duel or not) began {@code ticks} ticks earlier than it did. */
    public static void rewind(ServerPlayer player, long ticks) {
        SESSIONS.computeIfPresent(player.getUUID(), (id, s) -> new Session(s.pos, s.song, s.difficulty, s.startMs - ticks * 50, s.duel));
    }

    /** Settles a go. Returns the points recorded, or -1 when it was refused. */
    public static long finish(ServerPlayer player, Result result) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || !session.pos.equals(result.pos) || session.song != result.song
                || session.difficulty.ordinal() != result.difficulty) return -1;
        SESSIONS.remove(player.getUUID());
        Song song = Songbook.song(session.song);
        Duel duel = session.duel == 0 ? null : DUELS.get(session.duel);
        if (result.cancelled) {
            if (duel != null) {
                duel.forfeits.put(player.getUUID(), true);
                duel.scores.put(player.getUUID(), 0L);
                settle(player.getServer(), duel);
            }
            return -1;
        }
        long elapsed = Util.getMillis() - session.startMs;
        long needed = song.lengthMs() - SLACK_MS;
        boolean early = elapsed < needed && !result.failed;
        if (early || elapsed > song.lengthMs() + EXPIRE_MS || player.distanceToSqr(session.pos.getCenter()) > REACH * REACH) {
            player.displayClientMessage(Component.translatable("message.tribalpower.practice.refused"), false);
            if (duel != null) {
                duel.forfeits.put(player.getUUID(), true);
                duel.scores.put(player.getUUID(), 0L);
                settle(player.getServer(), duel);
            }
            return -1;
        }
        Songbook.Chart chart = Songbook.chart(song, session.difficulty);
        int total = chart.size();
        int hits = Math.max(0, Math.min(result.hits, total));
        long points = Math.max(0, Math.min(result.score, Songbook.maxScore(song, session.difficulty)));
        int accuracy = total == 0 ? 0 : (int) Math.round(100.0 * hits / total);
        boolean fullCombo = !result.failed && hits == total && result.bestStreak >= total;
        int stars = result.failed ? 0 : Songbook.stars(song, session.difficulty, points);
        if (duel != null) {
            duel.scores.put(player.getUUID(), points);
            settle(player.getServer(), duel);
        }
        if (result.failed) {
            player.displayClientMessage(Component.translatable("message.tribalpower.practice.failed", song.title(), points), false);
            return points;
        }
        String key = key(song, session.difficulty);
        Scores scores = Scores.get(player);
        Best before = scores.best(key, player.getUUID());
        boolean personal = before == null || points > before.points;
        if (personal) scores.record(key, player.getUUID(), new Best(player.getGameProfile().getName(), (int) Math.min(Integer.MAX_VALUE, points), accuracy, stars, fullCombo));
        int place = scores.place(key, player.getUUID());
        Component difficulty = Component.translatable("gui.tribalpower.songkeeper.difficulty." + session.difficulty.key());
        player.displayClientMessage(Component.translatable(personal ? "message.tribalpower.practice.best" : "message.tribalpower.practice.done",
                song.title(), difficulty, points, accuracy), false);
        if (personal && place >= 0 && place < BOARD)
            player.displayClientMessage(Component.translatable("message.tribalpower.practice.board", place + 1), false);
        return points;
    }

    // ---- server: duels ------------------------------------------------------------------------------------------

    /** A player at one drum of a pair challenges the named player at the other. */
    public static void challenge(ServerPlayer player, BlockPos pos, String rivalName, int songIndex, Difficulty difficulty) {
        Song song = Songbook.song(songIndex);
        if (song == null || !at(player, pos) || SESSIONS.containsKey(player.getUUID())) return;
        ServerPlayer rival = null;
        for (ServerPlayer candidate : rivals(player, pos))
            if (candidate.getGameProfile().getName().equals(rivalName)) rival = candidate;
        if (rival == null) {
            player.displayClientMessage(Component.translatable("message.tribalpower.duel.nobody"), true);
            return;
        }
        BlockPos rivalPos = drumOf(rival, pos);
        if (rivalPos == null) return;
        DUELS.values().removeIf(d -> !d.started && Util.getMillis() - d.invitedMs > INVITE_MS);
        int id = NEXT_DUEL.getAndIncrement();
        DUELS.put(id, new Duel(id, player.getUUID(), rival.getUUID(), pos.immutable(), rivalPos, songIndex, difficulty));
        send(rival, new Invite(id, player.getGameProfile().getName(), songIndex, difficulty.ordinal()));
        player.displayClientMessage(Component.translatable("message.tribalpower.duel.sent", rival.getGameProfile().getName(), song.title()), true);
    }

    /** The challenged player answers. On yes both drums start the song together. */
    public static void answer(ServerPlayer player, int id, boolean accept) {
        Duel duel = DUELS.get(id);
        if (duel == null || duel.started || !duel.rival.equals(player.getUUID())) return;
        ServerPlayer challenger = player.getServer().getPlayerList().getPlayer(duel.challenger);
        boolean stale = Util.getMillis() - duel.invitedMs > INVITE_MS || challenger == null
                || !at(challenger, duel.challengerPos) || !at(player, duel.rivalPos)
                || SESSIONS.containsKey(duel.challenger) || SESSIONS.containsKey(duel.rival);
        if (!accept || stale) {
            DUELS.remove(id);
            if (challenger != null) challenger.displayClientMessage(Component.translatable(stale ? "message.tribalpower.duel.expired"
                    : "message.tribalpower.duel.declined", player.getGameProfile().getName()), true);
            if (stale) player.displayClientMessage(Component.translatable("message.tribalpower.duel.expired", challenger == null ? "?" : challenger.getGameProfile().getName()), true);
            return;
        }
        duel.started = true;
        Song song = Songbook.song(duel.song);
        begin(challenger, duel.challengerPos, song, duel.difficulty, id, player.getGameProfile().getName());
        begin(player, duel.rivalPos, song, duel.difficulty, id, challenger.getGameProfile().getName());
        announce(player.serverLevel(), duel.challengerPos, Component.translatable("message.tribalpower.duel.begins",
                challenger.getGameProfile().getName(), player.getGameProfile().getName(), song.title()).withStyle(ChatFormatting.GOLD));
    }

    /** A duellist's running score, passed on to the other drum. */
    public static void progress(ServerPlayer player, Progress progress) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.duel == 0 || session.duel != progress.duel) return;
        Duel duel = DUELS.get(session.duel);
        if (duel == null) return;
        ServerPlayer other = player.getServer().getPlayerList().getPlayer(duel.other(player.getUUID()));
        if (other != null) send(other, new Rival(progress.duel, progress.score, progress.streak,
                progress.multiplier, progress.meter, progress.hits, progress.failed));
    }

    /** When both duellists are in (or one has walked away), the higher score wins. */
    private static void settle(net.minecraft.server.MinecraftServer server, Duel duel) {
        if (duel.scores.size() < 2) return;
        DUELS.remove(duel.id);
        long a = duel.scores.get(duel.challenger), b = duel.scores.get(duel.rival);
        boolean aOut = duel.forfeits.getOrDefault(duel.challenger, false), bOut = duel.forfeits.getOrDefault(duel.rival, false);
        UUID winner = aOut && !bOut ? duel.rival : bOut && !aOut ? duel.challenger : a > b ? duel.challenger : b > a ? duel.rival : null;
        ServerPlayer challenger = server.getPlayerList().getPlayer(duel.challenger), rival = server.getPlayerList().getPlayer(duel.rival);
        String aName = challenger == null ? "?" : challenger.getGameProfile().getName(), bName = rival == null ? "?" : rival.getGameProfile().getName();
        if (challenger != null) send(challenger, new Outcome(duel.id, winner == null ? 0 : winner.equals(duel.challenger) ? 1 : -1, a, b, bName));
        if (rival != null) send(rival, new Outcome(duel.id, winner == null ? 0 : winner.equals(duel.rival) ? 1 : -1, b, a, aName));
        ServerPlayer anyone = challenger != null ? challenger : rival;
        if (anyone != null) {
            Scores scores = Scores.get(anyone);
            if (winner != null) {
                scores.duel(winner, true);
                scores.duel(winner.equals(duel.challenger) ? duel.rival : duel.challenger, false);
            }
            String winnerName = winner == null ? null : winner.equals(duel.challenger) ? aName : bName;
            announce(anyone.serverLevel(), duel.challengerPos, winner == null
                    ? Component.translatable("message.tribalpower.duel.draw", aName, bName, a)
                    : Component.translatable("message.tribalpower.duel.won", winnerName, Math.max(a, b), Math.min(a, b),
                            winner.equals(duel.challenger) ? bName : aName).withStyle(ChatFormatting.GOLD));
        }
    }

    private static void announce(ServerLevel level, BlockPos pos, Component message) {
        for (ServerPlayer near : level.players())
            if (near.distanceToSqr(pos.getCenter()) < 24 * 24) near.displayClientMessage(message, false);
    }

    /** For tests: the duel ids waiting on an answer or in play. */
    public static java.util.Set<Integer> duels() {
        return java.util.Set.copyOf(DUELS.keySet());
    }

    // ---- scores -------------------------------------------------------------------------------------------------

    public record Best(String name, int points, int accuracy, int stars, boolean fullCombo) {}

    public static final class Scores extends SavedData {
        private final Map<String, Map<UUID, Best>> songs = new HashMap<>();
        private final Map<UUID, int[]> duels = new HashMap<>();

        public static Scores get(ServerPlayer player) {
            return player.getServer().overworld().getDataStorage()
                    .computeIfAbsent(new SavedData.Factory<>(Scores::new, Scores::load), "tribalpower_drum_scores");
        }

        public Best best(String key, UUID player) {
            return songs.getOrDefault(key, Map.of()).get(player);
        }

        public Best best(Song song, Difficulty difficulty, UUID player) {
            return best(key(song, difficulty), player);
        }

        void record(String key, UUID player, Best best) {
            songs.computeIfAbsent(key, t -> new HashMap<>()).put(player, best);
            setDirty();
        }

        /** Wins and losses in duels. */
        public int[] record(UUID player) {
            return duels.getOrDefault(player, new int[2]).clone();
        }

        void duel(UUID player, boolean won) {
            duels.computeIfAbsent(player, p -> new int[2])[won ? 0 : 1]++;
            setDirty();
        }

        public List<Best> board(String key) {
            return songs.getOrDefault(key, Map.of()).values().stream()
                    .sorted(Comparator.comparingInt(Best::points).reversed()).limit(BOARD).toList();
        }

        /** Where a player stands on a board, from 0, or -1. */
        public int place(String key, UUID player) {
            Best mine = best(key, player);
            if (mine == null) return -1;
            return (int) songs.getOrDefault(key, Map.of()).values().stream().filter(b -> b.points > mine.points).count();
        }

        public static Scores load(CompoundTag tag, HolderLookup.Provider registries) {
            Scores scores = new Scores();
            ListTag list = tag.getList("Scores", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                String key = entry.contains("Song") ? entry.getString("Song")
                        : entry.getInt("Track") >= 0 && entry.getInt("Track") < LEGACY.length ? LEGACY[entry.getInt("Track")] + "|hard" : null;
                if (key == null) continue;
                scores.songs.computeIfAbsent(key, t -> new HashMap<>()).put(entry.getUUID("Player"), new Best(entry.getString("Name"),
                        entry.getInt("Points"), entry.getInt("Accuracy"), entry.getInt("Stars"), entry.getBoolean("FullCombo")));
            }
            ListTag duels = tag.getList("Duels", Tag.TAG_COMPOUND);
            for (int i = 0; i < duels.size(); i++) {
                CompoundTag entry = duels.getCompound(i);
                scores.duels.put(entry.getUUID("Player"), new int[]{entry.getInt("Won"), entry.getInt("Lost")});
            }
            return scores;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            songs.forEach((key, bests) -> bests.forEach((player, best) -> {
                CompoundTag entry = new CompoundTag();
                entry.putString("Song", key);
                entry.putUUID("Player", player);
                entry.putString("Name", best.name);
                entry.putInt("Points", best.points);
                entry.putInt("Accuracy", best.accuracy);
                entry.putInt("Stars", best.stars);
                entry.putBoolean("FullCombo", best.fullCombo);
                list.add(entry);
            }));
            tag.put("Scores", list);
            ListTag duelList = new ListTag();
            duels.forEach((player, record) -> {
                CompoundTag entry = new CompoundTag();
                entry.putUUID("Player", player);
                entry.putInt("Won", record[0]);
                entry.putInt("Lost", record[1]);
                duelList.add(entry);
            });
            tag.put("Duels", duelList);
            return tag;
        }
    }

    // ---- network ------------------------------------------------------------------------------------------------

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tribalpower", path));
    }

    /** Server → client: the song list with this player's bests (4 per song), duel record and who is at a partner drum. */
    public record Browse(BlockPos pos, int[] bests, byte[] stars, int won, int lost, List<String> rivals, boolean paired) implements CustomPacketPayload {
        public static final Type<Browse> TYPE = payloadType("drum_practice_browse");
        public static final StreamCodec<FriendlyByteBuf, Browse> STREAM_CODEC = CustomPacketPayload.codec((v, b) -> {
            b.writeBlockPos(v.pos); b.writeVarIntArray(v.bests); b.writeByteArray(v.stars); b.writeVarInt(v.won); b.writeVarInt(v.lost);
            b.writeCollection(v.rivals, FriendlyByteBuf::writeUtf); b.writeBoolean(v.paired);
        }, b -> new Browse(b.readBlockPos(), b.readVarIntArray(), b.readByteArray(), b.readVarInt(), b.readVarInt(),
                b.readList(FriendlyByteBuf::readUtf), b.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client → server: the board for one song and difficulty, please. */
    public record BoardRequest(int song, int difficulty) implements CustomPacketPayload {
        public static final Type<BoardRequest> TYPE = payloadType("drum_practice_board_request");
        public static final StreamCodec<FriendlyByteBuf, BoardRequest> STREAM_CODEC = CustomPacketPayload.codec(
                (v, b) -> { b.writeVarInt(v.song); b.writeVarInt(v.difficulty); }, b -> new BoardRequest(b.readVarInt(), b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Board(int song, int difficulty, List<String> names, List<Integer> points) implements CustomPacketPayload {
        public static final Type<Board> TYPE = payloadType("drum_practice_board");
        public static final StreamCodec<FriendlyByteBuf, Board> STREAM_CODEC = CustomPacketPayload.codec((v, b) -> {
            b.writeVarInt(v.song); b.writeVarInt(v.difficulty); b.writeCollection(v.names, FriendlyByteBuf::writeUtf);
            b.writeCollection(v.points, FriendlyByteBuf::writeVarInt);
        }, b -> new Board(b.readVarInt(), b.readVarInt(), b.readList(FriendlyByteBuf::readUtf), b.readList(FriendlyByteBuf::readVarInt)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client → server: play this song at this difficulty. */
    public record Play(BlockPos pos, int song, int difficulty) implements CustomPacketPayload {
        public static final Type<Play> TYPE = payloadType("drum_practice_play");
        public static final StreamCodec<FriendlyByteBuf, Play> STREAM_CODEC = CustomPacketPayload.codec(
                (v, b) -> { b.writeBlockPos(v.pos); b.writeVarInt(v.song); b.writeVarInt(v.difficulty); },
                b -> new Play(b.readBlockPos(), b.readVarInt(), b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server → client: begin. {@code duel} is 0 for a go on your own, else the duel and who you face. */
    public record Start(BlockPos pos, int song, int difficulty, int duel, String opponent) implements CustomPacketPayload {
        public static final Type<Start> TYPE = payloadType("drum_practice_start");
        public static final StreamCodec<FriendlyByteBuf, Start> STREAM_CODEC = CustomPacketPayload.codec(
                (v, b) -> { b.writeBlockPos(v.pos); b.writeVarInt(v.song); b.writeVarInt(v.difficulty); b.writeVarInt(v.duel); b.writeUtf(v.opponent); },
                b -> new Start(b.readBlockPos(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readUtf()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client → server: how the go went. */
    public record Result(BlockPos pos, int song, int difficulty, long score, int hits, int perfects, int bestStreak,
                         int misses, int strays, boolean failed, boolean cancelled) implements CustomPacketPayload {
        public static final Type<Result> TYPE = payloadType("drum_practice_result");
        public static final StreamCodec<FriendlyByteBuf, Result> STREAM_CODEC = CustomPacketPayload.codec((v, b) -> {
            b.writeBlockPos(v.pos); b.writeVarInt(v.song); b.writeVarInt(v.difficulty); b.writeVarLong(v.score); b.writeVarInt(v.hits);
            b.writeVarInt(v.perfects); b.writeVarInt(v.bestStreak); b.writeVarInt(v.misses); b.writeVarInt(v.strays);
            b.writeBoolean(v.failed); b.writeBoolean(v.cancelled);
        }, b -> new Result(b.readBlockPos(), b.readVarInt(), b.readVarInt(), b.readVarLong(), b.readVarInt(), b.readVarInt(),
                b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client → server: challenge the named player at the partner drum. */
    public record Challenge(BlockPos pos, String rival, int song, int difficulty) implements CustomPacketPayload {
        public static final Type<Challenge> TYPE = payloadType("drum_duel_challenge");
        public static final StreamCodec<FriendlyByteBuf, Challenge> STREAM_CODEC = CustomPacketPayload.codec(
                (v, b) -> { b.writeBlockPos(v.pos); b.writeUtf(v.rival); b.writeVarInt(v.song); b.writeVarInt(v.difficulty); },
                b -> new Challenge(b.readBlockPos(), b.readUtf(), b.readVarInt(), b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server → client: someone at the partner drum challenges you. */
    public record Invite(int duel, String from, int song, int difficulty) implements CustomPacketPayload {
        public static final Type<Invite> TYPE = payloadType("drum_duel_invite");
        public static final StreamCodec<FriendlyByteBuf, Invite> STREAM_CODEC = CustomPacketPayload.codec(
                (v, b) -> { b.writeVarInt(v.duel); b.writeUtf(v.from); b.writeVarInt(v.song); b.writeVarInt(v.difficulty); },
                b -> new Invite(b.readVarInt(), b.readUtf(), b.readVarInt(), b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Answer(int duel, boolean accept) implements CustomPacketPayload {
        public static final Type<Answer> TYPE = payloadType("drum_duel_answer");
        public static final StreamCodec<FriendlyByteBuf, Answer> STREAM_CODEC = CustomPacketPayload.codec(
                (v, b) -> { b.writeVarInt(v.duel); b.writeBoolean(v.accept); }, b -> new Answer(b.readVarInt(), b.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client → server, a few times a second in a duel: where you stand. Meter is 0..100. */
    public record Progress(int duel, long score, int streak, int multiplier, int meter, int hits, boolean failed) implements CustomPacketPayload {
        public static final Type<Progress> TYPE = payloadType("drum_duel_progress");
        public static final StreamCodec<FriendlyByteBuf, Progress> STREAM_CODEC = CustomPacketPayload.codec((v, b) -> {
            b.writeVarInt(v.duel); b.writeVarLong(v.score); b.writeVarInt(v.streak); b.writeVarInt(v.multiplier); b.writeVarInt(v.meter);
            b.writeVarInt(v.hits); b.writeBoolean(v.failed);
        }, b -> new Progress(b.readVarInt(), b.readVarLong(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server → client: the other duellist's running score. */
    public record Rival(int duel, long score, int streak, int multiplier, int meter, int hits, boolean failed) implements CustomPacketPayload {
        public static final Type<Rival> TYPE = payloadType("drum_duel_rival");
        public static final StreamCodec<FriendlyByteBuf, Rival> STREAM_CODEC = CustomPacketPayload.codec((v, b) -> {
            b.writeVarInt(v.duel); b.writeVarLong(v.score); b.writeVarInt(v.streak); b.writeVarInt(v.multiplier); b.writeVarInt(v.meter);
            b.writeVarInt(v.hits); b.writeBoolean(v.failed);
        }, b -> new Rival(b.readVarInt(), b.readVarLong(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server → client: the duel's verdict. {@code result} 1 won, 0 draw, -1 lost. */
    public record Outcome(int duel, int result, long yours, long theirs, String opponent) implements CustomPacketPayload {
        public static final Type<Outcome> TYPE = payloadType("drum_duel_outcome");
        public static final StreamCodec<FriendlyByteBuf, Outcome> STREAM_CODEC = CustomPacketPayload.codec(
                (v, b) -> { b.writeVarInt(v.duel); b.writeVarInt(v.result); b.writeVarLong(v.yours); b.writeVarLong(v.theirs); b.writeUtf(v.opponent); },
                b -> new Outcome(b.readVarInt(), b.readVarInt(), b.readVarLong(), b.readVarLong(), b.readUtf()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToClient(Browse.TYPE, Browse.STREAM_CODEC, (payload, context) ->
                tk.darrow.tribalpower.client.SongkeeperScreen.open(payload));
        registrar.playToClient(Board.TYPE, Board.STREAM_CODEC, (payload, context) ->
                tk.darrow.tribalpower.client.SongkeeperScreen.board(payload));
        registrar.playToClient(Start.TYPE, Start.STREAM_CODEC, (payload, context) ->
                tk.darrow.tribalpower.client.SongkeeperPlayScreen.start(payload));
        registrar.playToClient(Invite.TYPE, Invite.STREAM_CODEC, (payload, context) ->
                tk.darrow.tribalpower.client.SongkeeperPlayScreen.invite(payload));
        registrar.playToClient(Rival.TYPE, Rival.STREAM_CODEC, (payload, context) ->
                tk.darrow.tribalpower.client.SongkeeperPlayScreen.rival(payload));
        registrar.playToClient(Outcome.TYPE, Outcome.STREAM_CODEC, (payload, context) ->
                tk.darrow.tribalpower.client.SongkeeperPlayScreen.outcome(payload));
        registrar.playToServer(Play.TYPE, Play.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) play(player, payload.pos(), payload.song(), Difficulty.of(payload.difficulty()));
        });
        registrar.playToServer(BoardRequest.TYPE, BoardRequest.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) board(player, payload.song(), Difficulty.of(payload.difficulty()));
        });
        registrar.playToServer(Result.TYPE, Result.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) finish(player, payload);
        });
        registrar.playToServer(Challenge.TYPE, Challenge.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player)
                challenge(player, payload.pos(), payload.rival(), payload.song(), Difficulty.of(payload.difficulty()));
        });
        registrar.playToServer(Answer.TYPE, Answer.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) answer(player, payload.duel(), payload.accept());
        });
        registrar.playToServer(Progress.TYPE, Progress.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) progress(player, payload);
        });
    }
}
