package tk.darrow.tribalpower.item;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.camp.identity.Camps;

/**
 * Client -> server: a setting changed from the Gear screen or a hotkey. {@code ARMOR} switches one worn Spiritweave
 * piece's abilities on or off ({@code slot} is the {@link EquipmentSlot} ordinal, {@code value} the new state);
 * the other actions are toggles and ignore {@code value}.
 */
public record GearSettingsPayload(int action, int slot, boolean value) implements CustomPacketPayload {
    public static final int ARMOR = 0, GOGGLES = 1, VAULT = 2, STAFF_VOICE = 3;
    public static final Type<GearSettingsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("tribalpower", "gear_settings"));
    public static final StreamCodec<ByteBuf, GearSettingsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GearSettingsPayload::action,
            ByteBufCodecs.VAR_INT, GearSettingsPayload::slot,
            ByteBufCodecs.BOOL, GearSettingsPayload::value,
            GearSettingsPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) apply(player, payload.action(), payload.slot(), payload.value());
        });
    }

    /** The server-side effect of one setting change; shared by the payload handler and the game tests. */
    public static void apply(ServerPlayer player, int action, int slot, boolean value) {
        switch (action) {
            case ARMOR -> {
                if (slot < 0 || slot >= EquipmentSlot.values().length) return;
                EquipmentSlot where = EquipmentSlot.values()[slot];
                if (!where.isArmor()) return;
                ItemStack piece = player.getItemBySlot(where);
                if (!(piece.getItem() instanceof SpiritweaveArmor)) return;
                SpiritGear.setAbilitiesOff(piece, !value);
                player.displayClientMessage(Component.translatable(value ? "message.tribalpower.gear.on" : "message.tribalpower.gear.off",
                        piece.getHoverName()), true);
            }
            case GOGGLES -> {
                ItemStack hood = player.getItemBySlot(EquipmentSlot.HEAD);
                if (SpiritGear.goggles(hood)) SpiritGear.toggleGoggles(player, hood);
                else player.displayClientMessage(Component.translatable("message.tribalpower.gear.no_goggles"), true);
            }
            case VAULT -> {
                var camp = Camps.campOf(player.server, player.getUUID());
                if (camp == null) { player.displayClientMessage(Component.translatable("message.tribalpower.camp.no_camp"), true); return; }
                boolean personal = !Camps.personalVault(player);
                Camps.setPersonalVault(player, personal);
                player.displayClientMessage(Component.translatable(personal ? "message.tribalpower.camp.vault_personal"
                        : "message.tribalpower.camp.vault_camp", camp.name), true);
            }
            case STAFF_VOICE -> {
                ItemStack staff = player.getMainHandItem();
                if (!(staff.getItem() instanceof SpiritStaffItem)) staff = player.getOffhandItem();
                if (!(staff.getItem() instanceof SpiritStaffItem)) return;
                Attunement next = SpiritStaffItem.cycle(staff);
                player.displayClientMessage(Component.translatable("message.tribalpower.staff.selected",
                        Component.translatable("spell.tribalpower." + next.getSerializedName())), true);
            }
            default -> { }
        }
    }
}
