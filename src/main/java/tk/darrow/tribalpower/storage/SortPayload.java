package tk.darrow.tribalpower.storage;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Client to server: tidy one side of the menu the player has open. */
public record SortPayload(boolean container) implements CustomPacketPayload {
    public static final Type<SortPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("tribalpower", "sort_inventory"));
    public static final StreamCodec<ByteBuf, SortPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SortPayload::container, SortPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player) || player.isSpectator() || !player.containerMenu.stillValid(player)) return;
            InventorySorter.sort(player, payload.container());
        });
    }
}
