package tk.darrow.tribalpower.ley;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.List;

/**
 * The veins near one player, sent so the client can draw them. The braid's motion is not in the
 * packet: the client scrolls it from the game clock, so the ropes flow on every frame.
 */
public record LeyRopePayload(List<Rope> ropes) implements CustomPacketPayload {
    public static final int MAX = 6;
    public static final Type<LeyRopePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("tribalpower", "ley_ropes"));
    /** Ten floats. StreamCodec.composite stops at six, so the rope is written by hand. */
    public static final StreamCodec<ByteBuf, Rope> ROPE = new StreamCodec<>() {
        @Override public Rope decode(ByteBuf buf) {
            return new Rope(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
        }

        @Override public void encode(ByteBuf buf, Rope rope) {
            buf.writeFloat(rope.x());
            buf.writeFloat(rope.y());
            buf.writeFloat(rope.z());
            buf.writeFloat(rope.angle());
            buf.writeFloat(rope.amp());
            buf.writeFloat(rope.freq());
            buf.writeFloat(rope.phase());
            buf.writeFloat(rope.yAmp());
            buf.writeFloat(rope.yFreq());
            buf.writeFloat(rope.travel());
        }
    };
    public static final StreamCodec<ByteBuf, LeyRopePayload> STREAM_CODEC = StreamCodec.composite(
            ROPE.apply(ByteBufCodecs.list(MAX)), LeyRopePayload::ropes,
            LeyRopePayload::new);
    public static volatile LeyRopePayload latest = new LeyRopePayload(List.of());

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC, (payload, context) -> latest = payload);
    }

    public static LeyRopePayload capture(ServerPlayer player) {
        List<Rope> ropes = new java.util.ArrayList<>();
        for (LeyField.Rope rope : LeyField.ropes(player.serverLevel(), player.blockPosition())) {
            if (ropes.size() >= MAX) break;
            ropes.add(new Rope((float) rope.x(), (float) rope.y(), (float) rope.z(), (float) rope.angle(),
                    (float) rope.amp(), (float) rope.freq(), (float) rope.phase(), (float) rope.yAmp(),
                    (float) rope.yFreq(), (float) rope.travel()));
        }
        return new LeyRopePayload(List.copyOf(ropes));
    }

    public record Rope(float x, float y, float z, float angle, float amp, float freq, float phase,
                       float yAmp, float yFreq, float travel) {}
}
