package tk.darrow.tribalpower.tribe;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import tk.darrow.tribalpower.client.CodexUnlocks;
import tk.darrow.tribalpower.world.structure.LoreTabletBlock;

/**
 * {@code tribalpower:codex_unlocks} (server → client): the Tribe Marks a player has received (bit = tribe ordinal,
 * from {@link TribeStandingSavedData}) and the Lore Tablets they have read ({@code PlayerPersisted.TribalTabletsRead}).
 * The Spirit Codex shows the matching tribe and tablet pages without the spoiler veil once they are unlocked.
 * Sent on login, respawn/clone, when a Mark is granted and when a tablet is first read.
 */
public record CodexUnlocksPayload(int tribes, int tablets) implements CustomPacketPayload {
    public static final Type<CodexUnlocksPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("tribalpower", "codex_unlocks"));
    public static final StreamCodec<ByteBuf, CodexUnlocksPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CodexUnlocksPayload::tribes,
            ByteBufCodecs.VAR_INT, CodexUnlocksPayload::tablets,
            CodexUnlocksPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Mod-bus listener. The handler body only runs on the client; {@link CodexUnlocks} holds no client-only types. */
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC, (payload, context) -> CodexUnlocks.accept(payload.tribes(), payload.tablets()));
    }

    public static CodexUnlocksPayload of(ServerPlayer player) {
        return new CodexUnlocksPayload(TribeStandingSavedData.get(player.server).marks(player.getUUID()), LoreTabletBlock.readMask(player));
    }

    /** Sends the player's current unlocks; silently skipped for connections without the channel (mock/test players). */
    public static void sync(ServerPlayer player) {
        if (player.connection == null || player.connection.getConnection().channel() == null
                || !NetworkRegistry.hasChannel(player.connection, TYPE.id())) return;
        PacketDistributor.sendToPlayer(player, of(player));
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    public static void onClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }
}
