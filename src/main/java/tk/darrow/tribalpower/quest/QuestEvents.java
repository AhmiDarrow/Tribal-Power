package tk.darrow.tribalpower.quest;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.tribe.TribeHooks;

/** The deeds the tribes notice. The rest of the mod calls in here; requests and questlines listen. */
public final class QuestEvents {
    private QuestEvents() {}

    /** A hostile fell to the player near a tribe's camp. */
    public static void slew(ServerPlayer player, LivingEntity fallen) {
        for (TribeDefinition tribe : TribeHooks.hearthsNear(player.serverLevel(), fallen.blockPosition(), Requests.CAMP_RADIUS)) {
            Requests.progress(player, tribe, Requests.Kind.SLAY, 1);
            Questline.progress(player, tribe, Questline.Kind.SLAY, 1);
        }
    }

    /** A rite was performed at a brazier or pedestal. Every tribe whose camp it stands in takes note. */
    public static void rite(ServerLevel level, BlockPos at, ServerPlayer player) {
        if (player == null) return;
        for (TribeDefinition tribe : TribeHooks.hearthsNear(level, at, Requests.CAMP_RADIUS)) {
            Requests.progress(player, tribe, Requests.Kind.RITE, 1);
            Questline.progress(player, tribe, Questline.Kind.RITE, 1);
            tk.darrow.tribalpower.event.Festivals.riteBonus(player, tribe);
        }
    }

    /** The tribe's guardian was overcome in its trial (Phase 4 calls this). */
    public static void trial(ServerPlayer player, TribeDefinition tribe) {
        Questline.progress(player, tribe, Questline.Kind.TRIAL, 1);
    }

    /** Standing rose: a RANK step may be met. Called from the standing change listener. */
    public static void standingChanged(ServerPlayer player, TribeDefinition tribe) {
        QuestSavedData data = QuestSavedData.get(player.server);
        // Kin may fly the tribe's crest: the banner pattern comes once, with the rank.
        if (!data.hasPattern(player.getUUID(), tribe)
                && tk.darrow.tribalpower.camp.identity.CampStanding.effectiveStanding(player, tribe) >= tk.darrow.tribalpower.tribe.TribeRank.KIN.threshold()) {
            data.grantPattern(player.getUUID(), tribe);
            tk.darrow.tribalpower.item.SpiritgearHelper.give(player, new net.minecraft.world.item.ItemStack(tk.darrow.tribalpower.lore.LoreRegistry.PATTERN_ITEMS.get(tribe).get()));
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.tribalpower.pattern.given", tribe.displayNameComponent())
                    .withStyle(tk.darrow.tribalpower.tribe.TribeStanding.colour(tribe)));
        }
        Questline.Step step = Questline.step(tribe, data.step(player.getUUID(), tribe));
        if (step != null && step.kind() == Questline.Kind.RANK && Questline.ready(player, tribe)) Questline.advance(player, tribe);
    }

    /** Sends the player's requests and stories to their Codex. */
    public static void sync(ServerPlayer player) {
        if (player.connection == null || player.connection.getConnection().channel() == null
                || !net.neoforged.neoforge.network.registration.NetworkRegistry.hasChannel(player.connection, QuestStatePayload.TYPE.id())) return;
        PacketDistributor.sendToPlayer(player, QuestStatePayload.of(player));
    }
}
