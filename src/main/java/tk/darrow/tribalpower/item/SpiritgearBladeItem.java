package tk.darrow.tribalpower.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.config.TribalConfig;

import java.util.List;

/** Spiritgear blade — Pulse fuels echo strikes; a linked totem voice adds a combat perk. */
public class SpiritgearBladeItem extends SwordItem {
    public SpiritgearBladeItem(Properties properties) {
        super(tk.darrow.tribalpower.item.SpiritGear.TIER, properties.attributes(SwordItem.createAttributes(tk.darrow.tribalpower.item.SpiritGear.TIER, 3, -2.4F)));
    }

    /** For the rest of the weapon family, which set their own numbers at runtime. */
    protected SpiritgearBladeItem(Properties properties, net.minecraft.world.item.component.ItemAttributeModifiers attributes) {
        super(tk.darrow.tribalpower.item.SpiritGear.TIER, properties.attributes(attributes));
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @org.jetbrains.annotations.Nullable T entity,
            java.util.function.Consumer<net.minecraft.world.item.Item> onBroken) {
        return SpiritGear.wear(stack, super.damageItem(stack, amount, entity, onBroken));
    }

    /** Cutting a cobweb or the like: paid as the break began, it wears nothing, like the tools. */
    @Override
    public boolean mineBlock(ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.core.BlockPos pos, LivingEntity entity) {
        return SpiritGear.mine(stack, level, state, pos, entity, () -> super.mineBlock(stack, level, state, pos, entity)).ok();
    }

    /** The line under the shared Spiritgear description. */
    protected String descKey() {
        return "item.tribalpower.spiritgear_blade.desc";
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
                || GearCell.spend(player, stack, SpiritGear.hitCost(stack));
        if (!paid) {
            // A starved blow wears the blade as a blow wears any sword, and then some (see SpiritGear.settle).
            int[] wear = {0};
            SpiritGear.held(stack, wear, () -> { super.postHurtEnemy(stack, target, attacker); return null; });
            SpiritGear.settle(player, stack, EquipmentSlot.MAINHAND, false, wear[0]);
            return;
        }
        target.invulnerableTime = 0;
        // every number here is the config's, and GearTooltips quotes the same ones
        boolean manifested = SpiritGear.rank(stack) >= 3;
        float echo = (float) TribalConfig.bladeEchoDamage();
        Attunement voice = SpiritGear.voice(stack).orElse(null);
        if (voice == Attunement.SPIRIT) echo += (float) (manifested ? TribalConfig.bladeSpiritEchoManifested() : TribalConfig.bladeSpiritEcho());
        target.hurt(player.damageSources().magic(), echo);
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, TribalConfig.bladeGlowTicks(), 0));
        if (voice == Attunement.EARTH) {
            target.knockback(TribalConfig.bladeEarthKnockback(), player.getX() - target.getX(), player.getZ() - target.getZ());
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TribalConfig.bladeEarthSlownessTicks(),
                    (manifested ? TribalConfig.bladeEarthSlownessLevelManifested() : TribalConfig.bladeEarthSlownessLevel()) - 1));
        } else if (voice == Attunement.FIRE) {
            target.igniteForSeconds(manifested ? TribalConfig.bladeFireSecondsManifested() : TribalConfig.bladeFireSeconds());
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().magic(), (float) (manifested ? TribalConfig.bladeFireDamageManifested() : TribalConfig.bladeFireDamage()));
        } else if (voice == Attunement.WATER) {
            player.heal((float) (manifested ? TribalConfig.bladeWaterHealManifested() : TribalConfig.bladeWaterHeal()));
        } else if (voice == Attunement.AIR) {
            float sweep = (float) (manifested ? TribalConfig.bladeAirSweepManifested() : TribalConfig.bladeAirSweep());
            AABB box = target.getBoundingBox().inflate(1.5, 0.25, 1.5);
            for (LivingEntity extra : player.level().getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != player && e != target && e.isAlive()
                            && tk.darrow.tribalpower.familiar.FamiliarRoster.hostile(e))) {
                extra.hurt(player.damageSources().playerAttack(player), sweep);
            }
        } else if (voice == Attunement.LOOM) {
            double pull = manifested ? TribalConfig.bladeLoomPullManifested() : TribalConfig.bladeLoomPull();
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
        tooltip.add(Component.translatable(descKey()));
        SpiritGear.appendTooltip(stack, tooltip, flag);
    }
}
