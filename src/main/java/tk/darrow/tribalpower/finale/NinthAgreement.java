package tk.darrow.tribalpower.finale;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.effect.ModEffects;
import tk.darrow.tribalpower.effect.SpiritEffects;
import tk.darrow.tribalpower.quest.QuestEvents;
import tk.darrow.tribalpower.quest.QuestRegistry;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.tribe.TribeRank;

/**
 * The one finale. With all nine relics carried into a Loom circle, the rite makes the Loom agree again for the
 * one who performs it: nine lights, every tribe's boon for a while, Kin with all nine from then on, and the
 * March's aurora answering every night. Performed once per player; the world remembers.
 */
public final class NinthAgreement {
    private NinthAgreement() {}

    /** Why the rite cannot take, or null when it can. */
    public static Component check(ServerLevel level, Player player) {
        if (!(player instanceof ServerPlayer performer)) return Component.translatable("message.tribalpower.rite.ninth_agreement.relics");
        if (AgreementSavedData.get(level.getServer()).agreed(performer.getUUID())) return null; // lights the circle again, costs the same
        for (TribeDefinition tribe : TribeDefinition.values())
            if (!QuestRegistry.carriesRelic(performer, tribe)) return Component.translatable("message.tribalpower.rite.ninth_agreement.relics");
        return null;
    }

    /** Whether a player has made the agreement. */
    public static boolean agreed(ServerPlayer player) {
        return AgreementSavedData.get(player.server).agreed(player.getUUID());
    }

    /** The standing every tribe grants an agreed player at the least. */
    public static int standingFloor(ServerPlayer player) {
        return agreed(player) ? TribeRank.KIN.threshold() : 0;
    }

    public static void perform(ServerLevel level, BlockPos pos, Player player) {
        if (!(player instanceof ServerPlayer performer)) return;
        AgreementSavedData data = AgreementSavedData.get(level.getServer());
        boolean first = !data.agreed(performer.getUUID());
        Vec3 centre = pos.getCenter();
        TribeDefinition[] tribes = TribeDefinition.values();
        for (int i = 0; i < tribes.length; i++) {
            double angle = i * Math.PI * 2 / tribes.length;
            Vec3 foot = centre.add(Math.cos(angle) * 4, 0, Math.sin(angle) * 4);
            SpiritEffects.beam(level, foot, foot.add(0, 24, 0), tribes[i].attunement());
        }
        SpiritEffects.ring(level, centre.add(0, 1, 0), Attunement.LOOM, 6, 36);
        level.sendParticles(ParticleTypes.END_ROD, centre.x, centre.y + 2, centre.z, 120, 3, 4, 3, 0.05);
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 2F, 0.6F);
        level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1F, 0.8F);
        for (TribeDefinition tribe : tribes) ModEffects.grantBoon(performer, tribe, TribalConfig.ninthAgreementBoonMinutes() * 1200, false);
        if (!first) {
            performer.displayClientMessage(Component.translatable("message.tribalpower.rite.ninth_agreement.already"), true);
            return;
        }
        data.agree(performer.getUUID());
        tk.darrow.tribalpower.camp.CampHooks.award(level, performer.getUUID(), "march/ninth_agreement");
        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("message.tribalpower.ninth_agreement.world", performer.getDisplayName()).withStyle(ChatFormatting.GOLD), false);
        QuestEvents.sync(performer);
        tk.darrow.tribalpower.tribe.CodexUnlocksPayload.sync(performer);
    }
}
