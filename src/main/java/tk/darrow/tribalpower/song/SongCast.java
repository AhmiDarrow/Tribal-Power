package tk.darrow.tribalpower.song;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.effect.SpiritEffects;
import tk.darrow.tribalpower.familiar.FamiliarRoster;

/**
 * Plays one verse. A bolt, a ward, a step, a call, or a bind, then at most two riders from the
 * reagents that followed the lead.
 */
public final class SongCast {
    private SongCast() {}

    public static boolean play(ServerPlayer player, SongVerse verse) {
        ServerLevel level = player.serverLevel();
        LivingEntity target = look(player, verse.reach());
        if ((verse.shape() == SongShape.BOLT || verse.shape() == SongShape.BIND) && target == null) return false;
        switch (verse.shape()) {
            case BOLT -> strike(player, target, 3.0F + verse.power(), verse);
            case BIND -> hold(target, verse);
            case WARD -> ward(player, verse);
            case STEP -> step(player, verse);
            case CALL -> call(level, player, verse);
        }
        for (Note rider : verse.riders()) ride(player, target == null ? player : target, rider, verse);
        SpiritEffects.ring(level, player.position().add(0, 0.2, 0), verse.voice(), 0.8, 16);
        if (target != null && verse.shape() != SongShape.WARD && verse.shape() != SongShape.STEP) {
            SpiritEffects.beam(level, player.getEyePosition(), target.getBoundingBox().getCenter(), verse.voice());
        }
        return true;
    }

    /** What a verse arrow adds when the bolt lands. The bolt itself already dealt its damage. */
    public static void onArrow(ServerPlayer player, LivingEntity target, SongVerse verse) {
        hold(target, verse);
        for (Note rider : verse.riders()) ride(player, target, rider, verse);
        if (player.level() instanceof ServerLevel level) {
            SpiritEffects.ring(level, target.position().add(0, 1, 0), verse.voice(), 0.45, 8);
        }
    }

    private static void strike(ServerPlayer player, LivingEntity target, float damage, SongVerse verse) {
        target.hurt(player.damageSources().indirectMagic(player, player), damage);
        if (verse.voice() == Attunement.FIRE) target.igniteForSeconds(2 + verse.power());
        if (verse.voice() == Attunement.AIR) knock(target, player, 0.4F + verse.power() * 0.05F);
    }

    private static void hold(LivingEntity target, SongVerse verse) {
        int amplifier = Math.min(3, verse.power() - 1);
        if (verse.voice() == Attunement.WATER) amplifier = Math.min(4, amplifier + 1);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, verse.duration(), amplifier));
    }

    private static void ward(ServerPlayer player, SongVerse verse) {
        int duration = verse.duration();
        int mild = Math.min(1, verse.power() - 1);
        switch (verse.voice()) {
            case EARTH -> player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, mild));
            case FIRE -> player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0));
            case WATER -> {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, mild));
                player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, duration * 2, 0));
                player.clearFire();
            }
            case AIR -> {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, duration, 0));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, mild));
            }
            case SPIRIT -> player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration * 3, 0));
            case LOOM -> player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, mild));
        }
    }

    private static void step(ServerPlayer player, SongVerse verse) {
        Vec3 look = player.getLookAngle();
        double force = 0.55 + verse.power() * 0.08;
        player.push(look.x * force, verse.voice() == Attunement.AIR ? 0.35 : 0.12, look.z * force);
        player.hurtMarked = true;
        if (verse.voice() == Attunement.AIR || verse.voice() == Attunement.SPIRIT) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40 + verse.power() * 10, 0));
        }
        if (verse.voice() == Attunement.LOOM) {
            // A short stitch, never as far as the staff. The landing has to be open air.
            Vec3 flat = new Vec3(look.x, 0, look.z);
            if (flat.lengthSqr() < 1.0E-4) return;
            flat = flat.normalize();
            int reach = Math.min(4, 2 + verse.power() / 2);
            Vec3 landing = player.position().add(flat.scale(reach));
            var box = player.getBoundingBox().move(landing.subtract(player.position()));
            if (player.level().noCollision(player, box)) player.teleportTo(landing.x, landing.y, landing.z);
        }
    }

    private static void call(ServerLevel level, ServerPlayer player, SongVerse verse) {
        double radius = 3.0 + Math.max(0, verse.reagents().size() - SongVerse.MIN_SHEET) * 0.45;
        var box = player.getBoundingBox().inflate(radius, 1.5, radius);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, FamiliarRoster::hostile)) {
            strike(player, living, 2.0F + verse.power() * 0.5F, verse);
        }
        SpiritEffects.ring(level, player.position().add(0, 0.1, 0), verse.voice(), radius, 20);
    }

    private static void ride(ServerPlayer player, LivingEntity target, Note rider, SongVerse verse) {
        int duration = verse.duration();
        switch (rider) {
            case EMBER, BOLT -> target.igniteForSeconds(3);
            case CHILL, ROOT, WEAVE -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 1));
            case VENOM -> target.addEffect(new MobEffectInstance(MobEffects.POISON, duration, 0));
            case WEAKEN -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0));
            case GUST, SHOVE -> knock(target, player, rider == Note.SHOVE ? 0.9F : 0.55F);
            case HOUND -> target.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0));
            case QUIET -> player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0));
        }
    }

    private static void knock(LivingEntity target, ServerPlayer player, float strength) {
        Vec3 away = target.position().subtract(player.position());
        if (away.lengthSqr() < 1.0E-4) away = player.getLookAngle();
        target.knockback(strength, -away.x, -away.z);
        target.hurtMarked = true;
    }

    static LivingEntity look(ServerPlayer player, double reach) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(reach));
        var hit = ProjectileUtil.getEntityHitResult(player.level(), player, start, end,
                new AABB(start, end).inflate(1.0),
                entity -> entity instanceof LivingEntity living && living.isAlive() && FamiliarRoster.hostile(living));
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }
}
