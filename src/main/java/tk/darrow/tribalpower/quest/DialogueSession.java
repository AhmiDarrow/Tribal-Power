package tk.darrow.tribalpower.quest;

import com.google.gson.JsonElement;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import tk.darrow.tribalpower.camp.identity.CampStanding;
import tk.darrow.tribalpower.tribe.TribalKinEntity;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.tribe.TribeRank;
import tk.darrow.tribalpower.tribe.TribeStanding;

/**
 * A conversation with an Elder. The server holds the tree and judges every condition; the client only shows the
 * node it is sent and says which choice was taken. Choices the player cannot take are not sent at all.
 */
public final class DialogueSession {
    private DialogueSession() {}

    /** Server → client: show this node, with these choices open. */
    public record Open(int kin, int tribe, String node, List<String> lines, List<String> choices) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("tribalpower", "dialogue_open"));
        public static final StreamCodec<ByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::kin, ByteBufCodecs.VAR_INT, Open::tribe, ByteBufCodecs.STRING_UTF8, Open::node,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Open::lines,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Open::choices, Open::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client → server: the player took this choice (by its text key, so a stale screen cannot pick a hidden one). */
    public record Choose(int kin, String node, String choice) implements CustomPacketPayload {
        public static final Type<Choose> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("tribalpower", "dialogue_choose"));
        public static final StreamCodec<ByteBuf, Choose> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Choose::kin, ByteBufCodecs.STRING_UTF8, Choose::node, ByteBufCodecs.STRING_UTF8, Choose::choice, Choose::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, (payload, context) ->
                tk.darrow.tribalpower.client.DialogueScreen.open(payload));
        registrar.playToClient(QuestStatePayload.TYPE, QuestStatePayload.STREAM_CODEC, (payload, context) -> QuestStatePayload.latest = payload);
        registrar.playToServer(Choose.TYPE, Choose.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) choose(player, payload);
        });
    }

    // ---- flow ----------------------------------------------------------------------------------------------------

    /** Begins at the first root whose conditions hold. Returns false when the tribe has nothing to say. */
    public static boolean begin(ServerPlayer player, TribalKinEntity kin) {
        Dialogue.Tree tree = Dialogue.tree(kin.tribe());
        if (tree == null) return false;
        for (Dialogue.Root root : tree.roots())
            if (holds(player, kin.tribe(), root.conditions())) return show(player, kin, tree, root.node());
        return false;
    }

    private static boolean show(ServerPlayer player, TribalKinEntity kin, Dialogue.Tree tree, String nodeId) {
        Dialogue.Node node = tree.node(nodeId);
        if (node == null) return false;
        List<String> choices = new ArrayList<>();
        for (Dialogue.Choice choice : node.choices()) if (holds(player, kin.tribe(), choice.conditions())) choices.add(choice.text());
        if (player.connection == null || player.connection.getConnection().channel() == null
                || !net.neoforged.neoforge.network.registration.NetworkRegistry.hasChannel(player.connection, Open.TYPE.id())) return false;
        PacketDistributor.sendToPlayer(player, new Open(kin.getId(), kin.tribe().ordinal(), node.id(), node.text(), List.copyOf(choices)));
        return true;
    }

    private static void choose(ServerPlayer player, Choose payload) {
        if (!(player.level().getEntity(payload.kin()) instanceof TribalKinEntity kin) || player.distanceToSqr(kin) > 64) return;
        Dialogue.Tree tree = Dialogue.tree(kin.tribe());
        if (tree == null) return;
        Dialogue.Node node = tree.node(payload.node());
        if (node == null) return;
        for (Dialogue.Choice choice : node.choices()) {
            if (!choice.text().equals(payload.choice()) || !holds(player, kin.tribe(), choice.conditions())) continue;
            boolean stay = true;
            for (Dialogue.Action action : choice.actions()) stay &= act(player, kin, action);
            if (stay && !choice.next().isEmpty()) show(player, kin, tree, choice.next());
            return;
        }
    }

    // ---- conditions ------------------------------------------------------------------------------------------------

    public static boolean holds(ServerPlayer player, TribeDefinition tribe, List<Dialogue.Condition> conditions) {
        for (Dialogue.Condition condition : conditions) if (!holds(player, tribe, condition)) return false;
        return true;
    }

    private static boolean holds(ServerPlayer player, TribeDefinition tribe, Dialogue.Condition condition) {
        QuestSavedData data = QuestSavedData.get(player.server);
        String value = condition.value();
        return switch (condition.kind()) {
            case "rank" -> CampStanding.effectiveStanding(player, tribe) >= TribeRank.valueOf(value.toUpperCase(java.util.Locale.ROOT)).threshold();
            case "below_rank" -> CampStanding.effectiveStanding(player, tribe) < TribeRank.valueOf(value.toUpperCase(java.util.Locale.ROOT)).threshold();
            case "advancement" -> {
                var advancement = player.getServer().getAdvancements().get(ResourceLocation.parse(value));
                yield advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
            }
            case "holding" -> holding(player, value);
            case "time" -> value.equals("night") == player.serverLevel().isNight();
            case "quest_step" -> data.step(player.getUUID(), tribe) == Integer.parseInt(value);
            case "quest_min_step" -> data.step(player.getUUID(), tribe) >= Integer.parseInt(value);
            case "quest_done" -> (data.step(player.getUUID(), tribe) >= Questline.STEPS) == Boolean.parseBoolean(value);
            case "quest_ready" -> Questline.ready(player, tribe) == Boolean.parseBoolean(value);
            case "request" -> switch (value) {
                case "none" -> data.request(player.getUUID(), tribe) == null;
                case "active" -> data.request(player.getUUID(), tribe) != null && !Requests.ready(player, tribe);
                case "ready" -> Requests.ready(player, tribe);
                default -> false;
            };
            case "relic" -> data.hasRelic(player.getUUID(), tribe) == Boolean.parseBoolean(value);
            case "completed_min" -> data.completed(player.getUUID(), tribe) >= Integer.parseInt(value);
            default -> true;
        };
    }

    private static boolean holding(ServerPlayer player, String id) {
        ItemStack held = player.getMainHandItem();
        if (id.startsWith("#")) return held.is(ItemTags.create(ResourceLocation.parse(id.substring(1))));
        return held.is(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    // ---- actions ---------------------------------------------------------------------------------------------------

    /** Does one thing. Returns whether the conversation goes on to the choice's next node. */
    private static boolean act(ServerPlayer player, TribalKinEntity kin, Dialogue.Action action) {
        TribeDefinition tribe = kin.tribe();
        JsonElement value = action.value();
        switch (action.kind()) {
            case "close" -> { return false; }
            case "trade" -> {
                kin.openTrades(player);
                return false;
            }
            case "standing" -> TribeStanding.add(player, tribe, value.getAsInt());
            case "give" -> tk.darrow.tribalpower.item.SpiritgearHelper.give(player, stack(value));
            case "take" -> {
                ItemStack wanted = stack(value);
                int left = wanted.getCount();
                for (int i = 0; i < player.getInventory().items.size() && left > 0; i++) {
                    ItemStack stack = player.getInventory().items.get(i);
                    if (!ItemStack.isSameItem(stack, wanted)) continue;
                    int taken = Math.min(left, stack.getCount());
                    stack.shrink(taken);
                    left -= taken;
                }
            }
            case "offer_request" -> {
                Requests.Template offered = Requests.offer(player, tribe);
                if (offered != null) player.sendSystemMessage(Component.translatable("message.tribalpower.request.offered", tribe.displayNameComponent(), offered.name())
                        .append(": ").append(offered.describe()).withStyle(TribeStanding.colour(tribe)));
            }
            case "turn_in_request" -> Requests.turnIn(player, tribe);
            case "advance_quest" -> {
                if (Questline.ready(player, tribe)) Questline.handIn(player, tribe);
            }
            case "advancement" -> tk.darrow.tribalpower.camp.CampHooks.award(player.serverLevel(), player.getUUID(), value.getAsString().replace("tribalpower:", ""));
            case "say" -> player.sendSystemMessage(Component.translatable(value.getAsString()).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
            default -> {}
        }
        return true;
    }

    private static ItemStack stack(JsonElement value) {
        var json = value.getAsJsonObject();
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(json.get("id").getAsString()));
        return new ItemStack(item, json.has("count") ? json.get("count").getAsInt() : 1);
    }
}
