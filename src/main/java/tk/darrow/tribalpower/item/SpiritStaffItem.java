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
import tk.darrow.tribalpower.world.TravelSafety;
import java.util.List;

/** Six readable, bounded spells (the Sixfold Staff). Ray spells stop at blocks and target hostiles, not teammates. */
public class SpiritStaffItem extends Item {
    public SpiritStaffItem(Properties properties) { super(properties); }
    public static Attunement element(ItemStack stack) {
        int mode = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("Attunement");
        return Attunement.values()[Math.floorMod(mode, Attunement.values().length)];
    }
    public static int cost(Attunement element) { return switch(element) { case EARTH -> 12; case FIRE -> 18; case WATER -> 24; case AIR -> 16; case SPIRIT -> 20; case LOOM -> TETHER_COST; }; }
    public static final int TETHER_COST = 6;
    public static final int STITCH_COST = 10;
    public static final double TETHER_PULL = 8.0;
    public static final int STITCH_RANGE = 6;
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack staff = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResultHolder.success(staff);
        Attunement element = element(staff);
        if (player.isShiftKeyDown() && element == Attunement.LOOM && !player.getCooldowns().isOnCooldown(this) && findTarget(level, player) == null) {
            return stitch(server, player, staff);
        }
        if (player.isShiftKeyDown()) {
            int next = (element.ordinal() + 1) % Attunement.values().length;
            CustomData.update(DataComponents.CUSTOM_DATA, staff, tag -> tag.putInt("Attunement", next));
            player.displayClientMessage(Component.translatable("message.tribalpower.staff.selected",
                    Component.translatable("spell.tribalpower." + Attunement.values()[next].getSerializedName())), true);
            return InteractionResultHolder.consume(staff);
        }
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(staff);
        Vec3 start = player.getEyePosition();
        LivingEntity target = findTarget(level, player);
        if ((element == Attunement.FIRE || element == Attunement.EARTH || element == Attunement.LOOM) && target == null) {
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
            case LOOM -> tether(player, target);
        }
        if (target != null && (element == Attunement.FIRE || element == Attunement.EARTH || element == Attunement.LOOM))
            SpiritEffects.beam(server, start.add(player.getLookAngle().scale(0.6)), target.getBoundingBox().getCenter(), element);
        SpiritEffects.ring(server, player.position().add(0, 0.2, 0), element, 0.75, 16);
        server.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 0.8F + element.ordinal() * 0.15F);
        player.getCooldowns().addCooldown(this, element == Attunement.WATER ? 160 : 40);
        return InteractionResultHolder.consume(staff);
    }
    /** Hostile living entity along the player's look ray within 18 blocks, stopping at blocks. */
    static LivingEntity findTarget(Level level, Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 end = player.pick(18, 0, false).getLocation();
        var hit = ProjectileUtil.getEntityHitResult(level, player, start, end,
                player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1),
                entity -> entity instanceof Enemy && entity instanceof LivingEntity && entity.isAlive());
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }
    /** Tether: draw the target up to {@link #TETHER_PULL} blocks along the thread toward the caster. */
    static void tether(Player player, LivingEntity target) {
        Vec3 toCaster = player.position().subtract(target.position());
        double distance = toCaster.length();
        if (distance < 1.5) return;
        double pull = Math.min(TETHER_PULL, distance - 1.5);
        Vec3 step = toCaster.normalize().scale(pull);
        Vec3 landing = target.position().add(step);
        // Keep the pulled creature out of solid blocks: fall back to a shove if the landing is blocked.
        if (target.level().noCollision(target, target.getBoundingBox().move(step))) {
            target.teleportTo(landing.x, landing.y, landing.z);
        }
        target.setDeltaMovement(toCaster.normalize().scale(0.6));
        target.hurtMarked = true;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
    }
    /** Stitch: blink forward through open air. Refuses hazards, solids, and out-of-bounds landings. */
    private InteractionResultHolder<ItemStack> stitch(ServerLevel server, Player player, ItemStack staff) {
        Vec3 direction = player.getLookAngle();
        Vec3 flat = new Vec3(direction.x, 0, direction.z);
        if (flat.lengthSqr() < 1.0E-4) flat = Vec3.directionFromRotation(0, player.getYRot());
        flat = flat.normalize();
        Vec3 landing = null;
        for (int reach = STITCH_RANGE; reach >= 2; reach--) {
            Vec3 candidate = player.position().add(flat.scale(reach));
            net.minecraft.core.BlockPos feet = net.minecraft.core.BlockPos.containing(candidate);
            if (!TravelSafety.withinBounds(server, feet) || TravelSafety.hasHazard(server, feet)) continue;
            AABB box = player.getBoundingBox().move(candidate.subtract(player.position()));
            if (!server.noCollision(player, box)) continue;
            // Only stitch through air: the straight thread between here and there must be clear of blocks.
            var clip = server.clip(new net.minecraft.world.level.ClipContext(player.getEyePosition(),
                    candidate.add(0, player.getEyeHeight(), 0), net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE, player));
            if (clip.getType() != HitResult.Type.MISS) continue;
            landing = candidate; break;
        }
        if (landing == null) {
            player.displayClientMessage(Component.translatable("message.tribalpower.staff.stitch_blocked"), true);
            return InteractionResultHolder.fail(staff);
        }
        if (!SpiritgearHelper.tryConsumePulse(player, STITCH_COST)) {
            SpiritgearHelper.notifyStarved(player); return InteractionResultHolder.fail(staff);
        }
        Vec3 from = player.position();
        player.teleportTo(landing.x, landing.y, landing.z);
        player.fallDistance = 0;
        SpiritEffects.beam(server, from.add(0, 1, 0), landing.add(0, 1, 0), Attunement.LOOM);
        SpiritEffects.ring(server, landing.add(0, 0.2, 0), Attunement.LOOM, 0.75, 16);
        server.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.6F);
        player.getCooldowns().addCooldown(this, 30);
        return InteractionResultHolder.consume(staff);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        var element = element(stack);
        lines.add(Component.translatable("spell.tribalpower." + element.getSerializedName()));
        lines.add(Component.translatable("item.tribalpower.spirit_staff.desc", cost(element)));
        if (element == Attunement.LOOM) lines.add(Component.translatable("item.tribalpower.spirit_staff.stitch", STITCH_COST));
    }
}
