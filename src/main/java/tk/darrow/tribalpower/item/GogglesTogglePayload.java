package tk.darrow.tribalpower.item;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Client -> server: a sneak-click with an empty hand while a goggled hood is worn. Vanilla sends nothing for an
 * empty hand in the air, so the client asks for the toggle itself.
 */
public record GogglesTogglePayload() implements CustomPacketPayload {
    public static final Type<GogglesTogglePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("tribalpower", "goggles_toggle"));
    public static final StreamCodec<ByteBuf, GogglesTogglePayload> STREAM_CODEC = StreamCodec.unit(new GogglesTogglePayload());

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.isShiftKeyDown()) return;
            ItemStack hood = player.getItemBySlot(EquipmentSlot.HEAD);
            if (SpiritGear.goggles(hood)) SpiritGear.toggleGoggles(player, hood);
        });
    }
}
