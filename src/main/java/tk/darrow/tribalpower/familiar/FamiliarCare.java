package tk.darrow.tribalpower.familiar;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import tk.darrow.tribalpower.camp.identity.CampStanding;
import tk.darrow.tribalpower.tribe.TribeRank;

/**
 * Feeding March creatures, the way you would a wolf. A wild one that is offered its favourite food bonds in time
 * (a third of the time for the gentle animals, a fifth for remnants, which still need their tribe's Voice); a
 * companion that is hurt is healed by it. Anything else falls through to love and breeding.
 */
public final class FamiliarCare {
    public static final float ANIMAL_TAME = 1F / 3F, REMNANT_TAME = 0.2F;
    public static final float ANIMAL_HEAL = 4F, REMNANT_HEAL = 6F;

    private FamiliarCare() {}

    private static void spendFood(Player player, ItemStack food) {
        if (player.getAbilities().instabuild) return;
        food.shrink(1);
        if (food.isEmpty()) player.getInventory().removeItem(food);
    }

    /** Returns the interaction result, or null when feeding does not apply here and the caller carries on. */
    public static InteractionResult feed(Familiar familiar, Player player, InteractionHand hand, ItemStack food) {
        var mob = familiar.asMob();
        boolean client = player.level().isClientSide;
        if (mob.isBaby()) return null;
        if (!familiar.isBonded()) {
            if (!FamiliarRoster.tameable(familiar.profile())) return null;
            var tribe = FamiliarRoster.voiceTribe(familiar.profile());
            if (tribe != null && (!(player instanceof ServerPlayer server) || CampStanding.effectiveStanding(server, tribe) < TribeRank.VOICE.threshold())) {
                if (!client) player.displayClientMessage(Component.translatable("message.tribalpower.familiar.need_voice",
                        tribe.displayNameComponent(), mob.getDisplayName()), true);
                return InteractionResult.sidedSuccess(client);
            }
            if (player.level() instanceof ServerLevel level) {
                spendFood(player, food);
                float chance = tribe == null ? ANIMAL_TAME : REMNANT_TAME;
                BondingCharmItem.conclude(level, player, familiar, ItemStack.EMPTY, level.random.nextFloat() < chance, false);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (familiar.isOwnedBy(player) && mob.getHealth() < mob.getMaxHealth()) {
            if (player.level() instanceof ServerLevel level) {
                spendFood(player, food);
                mob.heal(FamiliarRoster.voiceTribe(familiar.profile()) == null ? ANIMAL_HEAL : REMNANT_HEAL);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight() * 0.8, mob.getZ(),
                        5, 0.4, 0.3, 0.4, 0.02);
            }
            return InteractionResult.sidedSuccess(client);
        }
        return null;
    }
}
