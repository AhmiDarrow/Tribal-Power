package tk.darrow.tribalpower.quest;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import tk.darrow.tribalpower.tribe.TribeDefinition;

/**
 * Server to client: where the player stands with every tribe's work, for the Codex. One entry per tribe, in
 * ordinal order: the open request (empty when none) and its progress, requests finished, the story step and its
 * progress, and whether the relic is held.
 */
public record QuestStatePayload(List<TribeState> tribes) implements CustomPacketPayload {
    public static final Type<QuestStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("tribalpower", "quest_state"));

    public record TribeState(String request, int requestProgress, int completed, int step, int stepProgress, boolean relic) {
        static final StreamCodec<ByteBuf, TribeState> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, TribeState::request, ByteBufCodecs.VAR_INT, TribeState::requestProgress,
                ByteBufCodecs.VAR_INT, TribeState::completed, ByteBufCodecs.VAR_INT, TribeState::step,
                ByteBufCodecs.VAR_INT, TribeState::stepProgress, ByteBufCodecs.BOOL, TribeState::relic, TribeState::new);
    }

    public static final StreamCodec<ByteBuf, QuestStatePayload> STREAM_CODEC = StreamCodec.composite(
            TribeState.CODEC.apply(ByteBufCodecs.list()), QuestStatePayload::tribes, QuestStatePayload::new);

    /** What the client last heard; read by the Codex. */
    public static volatile QuestStatePayload latest = new QuestStatePayload(List.of());

    public static QuestStatePayload of(ServerPlayer player) {
        QuestSavedData data = QuestSavedData.get(player.server);
        List<TribeState> out = new ArrayList<>();
        for (TribeDefinition tribe : TribeDefinition.values()) {
            QuestSavedData.Request request = data.request(player.getUUID(), tribe);
            out.add(new TribeState(request == null ? "" : request.template(), request == null ? 0 : request.progress(),
                    data.completed(player.getUUID(), tribe), data.step(player.getUUID(), tribe), data.stepProgress(player.getUUID(), tribe),
                    data.hasRelic(player.getUUID(), tribe)));
        }
        return new QuestStatePayload(List.copyOf(out));
    }

    public TribeState of(TribeDefinition tribe) {
        return tribe.ordinal() < tribes.size() ? tribes.get(tribe.ordinal()) : new TribeState("", 0, 0, 0, 0, false);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
