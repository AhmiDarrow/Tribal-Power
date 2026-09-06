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

import java.util.List;

/**
 * Spiritgear blade — Pulse fuels echo strikes (bonus magic damage + reveal) and spares the edge.
 */
public class SpiritgearBladeItem extends SwordItem {
    public SpiritgearBladeItem(Properties properties) {
        super(Tiers.IRON, properties.attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F)));
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && !player.level().isClientSide && !player.getAbilities().instabuild) {
            if (SpiritgearHelper.tryConsumePulse(player, SpiritgearHelper.HIT_COST)) {
                target.invulnerableTime = 0;
                target.hurt(player.damageSources().magic(), 2.0F);
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 4, 0));
                SpiritgearHelper.notifyFueled(player);
                return;
            }
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            SpiritgearHelper.notifyStarved(player);
            return;
        }
        super.postHurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tribalpower.spiritgear.desc"));
        tooltip.add(Component.translatable("item.tribalpower.spiritgear_blade.desc"));
    }
}
