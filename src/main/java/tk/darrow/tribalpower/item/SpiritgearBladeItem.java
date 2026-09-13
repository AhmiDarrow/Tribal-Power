package tk.darrow.tribalpower.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tk.darrow.tribalpower.api.pulse.Attunement;

import java.util.List;

/** Spiritgear blade — Pulse fuels echo strikes; a linked totem voice adds a combat perk. */
public class SpiritgearBladeItem extends SwordItem {
    public SpiritgearBladeItem(Properties properties) {
        super(Tiers.DIAMOND, properties.attributes(SwordItem.createAttributes(Tiers.DIAMOND, 3, -2.4F))
                .durability(SpiritGear.TOOL_DURABILITY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return SpiritGear.foil(stack) || super.isFoil(stack);
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker instanceof Player player) || player.level().isClientSide) {
            super.postHurtEnemy(stack, target, attacker);
            return;
        }
        boolean paid = player.getAbilities().instabuild
                || SpiritgearHelper.tryConsumePulse(player, SpiritGear.hitCost(stack));
        if (!paid) {
            if (!SpiritGear.skipStarveHurt(stack)) stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            SpiritgearHelper.notifyStarved(player);
            return;
        }
        target.invulnerableTime = 0;
        float echo = 2.0F;
        Attunement voice = SpiritGear.voice(stack).orElse(null);
        if (voice == Attunement.SPIRIT) echo += SpiritGear.rank(stack) >= 3 ? 4 : 2;
        target.hurt(player.damageSources().magic(), echo);
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 4, 0));
        if (voice == Attunement.EARTH) {
            target.knockback(1.2, player.getX() - target.getX(), player.getZ() - target.getZ());
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60,
                    SpiritGear.rank(stack) >= 3 ? 2 : 1));
        } else if (voice == Attunement.FIRE) {
            target.igniteForSeconds(SpiritGear.rank(stack) >= 3 ? 6 : 4);
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().magic(), SpiritGear.rank(stack) >= 3 ? 5 : 3);
        } else if (voice == Attunement.WATER) {
            player.heal(SpiritGear.rank(stack) >= 3 ? 4 : 2);
        } else if (voice == Attunement.AIR) {
            float sweep = SpiritGear.rank(stack) >= 3 ? 4 : 2;
            AABB box = target.getBoundingBox().inflate(1.5, 0.25, 1.5);
            for (LivingEntity extra : player.level().getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != player && e != target && e.isAlive())) {
                extra.hurt(player.damageSources().playerAttack(player), sweep);
            }
        } else if (voice == Attunement.LOOM) {
            double pull = SpiritGear.rank(stack) >= 3 ? 6 : 4;
            Vec3 delta = player.position().subtract(target.position());
            if (delta.lengthSqr() > 1) {
                Vec3 step = delta.normalize().scale(Math.min(pull, delta.length()));
                target.setDeltaMovement(target.getDeltaMovement().add(step.x, 0.15, step.z));
                target.hurtMarked = true;
            }
        }
        SpiritgearHelper.notifyFueled(player);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tribalpower.spiritgear.desc"));
        tooltip.add(Component.translatable("item.tribalpower.spiritgear_blade.desc"));
        SpiritGear.appendTooltip(stack, tooltip, flag);
    }
}
