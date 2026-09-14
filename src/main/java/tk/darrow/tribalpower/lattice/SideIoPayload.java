package tk.darrow.tribalpower.lattice;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Client → server: cycle one face of a {@link HasSideIo} machine. */
public record SideIoPayload(BlockPos pos, int face) implements CustomPacketPayload {
    public static final Type<SideIoPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("tribalpower", "side_io"));
    public static final StreamCodec<ByteBuf, SideIoPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SideIoPayload::pos,
            ByteBufCodecs.VAR_INT, SideIoPayload::face,
            SideIoPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (payload.face < 0 || payload.face >= Direction.values().length) return;
            var level = player.serverLevel();
            if (!level.hasChunkAt(payload.pos) || player.distanceToSqr(payload.pos.getCenter()) > 64) return;
            Direction face = Direction.from3DDataValue(payload.face);
            var be = level.getBlockEntity(payload.pos);
            HasSideIo.cycle(player, be, face);
        });
    }
}
