package tk.darrow.tribalpower.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.effect.SpiritEffects;
import java.util.List;

/** Five readable, bounded spells. Ray spells stop at blocks and target hostiles, not teammates. */
public class SpiritStaffItem extends Item {
    public SpiritStaffItem(Properties properties) { super(properties); }
    public static Attunement element(ItemStack stack) {
        int mode = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("Attunement");
        return Attunement.values()[Math.floorMod(mode, 5)];
    }
    public static int cost(Attunement element) { return switch(element) { case EARTH -> 12; case FIRE -> 18; case WATER -> 24; case AIR -> 16; case SPIRIT -> 20; }; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack staff = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResultHolder.success(staff);
        Attunement element = element(staff);
        if (player.isShiftKeyDown()) {
            int next = (element.ordinal() + 1) % 5;
            CustomData.update(DataComponents.CUSTOM_DATA, staff, tag -> tag.putInt("Attunement", next));
            player.displayClientMessage(Component.translatable("message.tribalpower.staff.selected",
                    Component.translatable("spell.tribalpower." + Attunement.values()[next].getSerializedName())), true);
            return InteractionResultHolder.consume(staff);
        }
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(staff);
        Vec3 start = player.getEyePosition();
        Vec3 end = player.pick(18, 0, false).getLocation();
        var hit = ProjectileUtil.getEntityHitResult(level, player, start, end,
                player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1),
                entity -> entity instanceof Enemy && entity instanceof LivingEntity && entity.isAlive());
        LivingEntity target = hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
        if ((element == Attunement.FIRE || element == Attunement.EARTH) && target == null) {
            player.displayClientMessage(Component.translatable("message.tribalpower.staff.no_target"), true);
            return InteractionResultHolder.fail(staff);
        }
        if (!SpiritgearHelper.tryConsumePulse(player, cost(element))) {
            SpiritgearHelper.notifyStarved(player); return InteractionResultHolder.fail(staff);
        }
        switch(element) {
            case EARTH -> {
                target.hurt(player.damageSources().indirectMagic(player, player), 4);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
            }
            case FIRE -> {
                target.hurt(player.damageSources().indirectMagic(player, player), 8);
                target.igniteForSeconds(4);
            }
            case WATER -> {
                player.clearFire(); player.removeEffect(MobEffects.POISON);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
                player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 600, 0));
            }
            case AIR -> {
                Vec3 direction = player.getLookAngle();
                player.push(direction.x * 0.65, 0.30, direction.z * 0.65);
                player.hurtMarked = true;
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0));
            }
            case SPIRIT -> {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0));
                for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(12), e -> e instanceof Enemy))
                    enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, 240, 0));
            }
        }
        if (target != null && (element == Attunement.FIRE || element == Attunement.EARTH))
            SpiritEffects.beam(server, start.add(player.getLookAngle().scale(0.6)), target.getBoundingBox().getCenter(), element);
        SpiritEffects.ring(server, player.position().add(0, 0.2, 0), element, 0.75, 16);
        server.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 0.8F + element.ordinal() * 0.15F);
        player.getCooldowns().addCooldown(this, element == Attunement.WATER ? 160 : 40);
        return InteractionResultHolder.consume(staff);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        var element = element(stack);
        lines.add(Component.translatable("spell.tribalpower." + element.getSerializedName()));
        lines.add(Component.translatable("item.tribalpower.spirit_staff.desc", cost(element)));
    }
}
