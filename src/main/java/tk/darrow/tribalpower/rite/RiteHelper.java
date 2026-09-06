package tk.darrow.tribalpower.rite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.item.PulseCellItem;
import tk.darrow.tribalpower.item.SpiritgearHelper;

/**
 * Seals & Rites — each elemental seal on a Rite Pedestal grants a distinct spirit working.
 */
public final class RiteHelper {
    private static final int RADIUS = 6;

    private RiteHelper() {}

    public static boolean performSealRite(Level level, BlockPos pos, Player player, ItemStack seal) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }

        if (seal.is(ModItems.BLANK_SEAL.get())) {
            return false;
        }

        boolean amplified = SpiritgearHelper.tryConsumePulse(player, 8);

        if (seal.is(ModItems.EARTH_SEAL.get())) {
            earthRite(server, pos, player, amplified);
        } else if (seal.is(ModItems.FIRE_SEAL.get())) {
            fireRite(server, pos, player, amplified);
        } else if (seal.is(ModItems.WATER_SEAL.get())) {
            waterRite(server, pos, player, amplified);
        } else if (seal.is(ModItems.AIR_SEAL.get())) {
            airRite(server, pos, player, amplified);
        } else if (seal.is(ModItems.SPIRIT_SEAL.get())) {
            spiritRite(server, pos, player, amplified);
        } else {
            return false;
        }

        server.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                amplified ? 40 : 24, 0.35, 0.4, 0.35, 0.02);
        server.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8F, amplified ? 1.4F : 1.2F);
        return true;
    }

    private static void earthRite(ServerLevel server, BlockPos pos, Player player, boolean amplified) {
        int resist = amplified ? 1 : 0;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * (amplified ? 50 : 35), resist));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 30, amplified ? 1 : 0));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 20 * 25, 0));

        // Root hostiles in place — earth bind.
        for (LivingEntity living : hostilesNear(server, pos)) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * (amplified ? 12 : 8), amplified ? 2 : 1));
        }

        // Firm the ground underfoot into dirt paths for footing.
        BlockPos.betweenClosedStream(pos.offset(-2, -1, -2), pos.offset(2, -1, 2)).forEach(p -> {
            BlockState state = server.getBlockState(p);
            if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)) {
                server.setBlock(p, Blocks.DIRT_PATH.defaultBlockState(), 3);
            }
        });
        server.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5,
                16, 1.2, 0.1, 1.2, 0.01);
    }

    private static void fireRite(ServerLevel server, BlockPos pos, Player player, boolean amplified) {
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * (amplified ? 75 : 50), 0));
        player.clearFire();

        for (LivingEntity living : hostilesNear(server, pos)) {
            living.igniteForSeconds(amplified ? 8 : 5);
        }

        // Melt snow / ice and light unlit campfires in range.
        BlockPos.betweenClosedStream(pos.offset(-RADIUS, -1, -RADIUS), pos.offset(RADIUS, 2, RADIUS)).forEach(p -> {
            BlockState state = server.getBlockState(p);
            if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) {
                server.destroyBlock(p, false);
            } else if (state.getBlock() instanceof CampfireBlock && !state.getValue(CampfireBlock.LIT)) {
                server.setBlock(p, state.setValue(CampfireBlock.LIT, true), 3);
            }
        });
        server.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                28, 0.8, 0.5, 0.8, 0.02);
    }

    private static void waterRite(ServerLevel server, BlockPos pos, Player player, boolean amplified) {
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 20 * (amplified ? 120 : 75), 0));
        player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 20 * (amplified ? 45 : 25), 0));
        player.clearFire();

        BlockPos.betweenClosedStream(pos.offset(-RADIUS, -1, -RADIUS), pos.offset(RADIUS, 2, RADIUS)).forEach(p -> {
            BlockState state = server.getBlockState(p);
            if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                server.removeBlock(p, false);
            } else if (state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT)) {
                server.setBlock(p, state.setValue(CampfireBlock.LIT, false), 3);
            } else if (state.is(Blocks.FARMLAND)) {
                server.setBlock(p, state.setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, 7), 3);
            } else if (amplified && state.getBlock() instanceof BonemealableBlock growable
                    && growable.isValidBonemealTarget(server, p, state)) {
                if (server.random.nextInt(3) == 0) {
                    growable.performBonemeal(server, server.random, p, state);
                }
            }
        });
        server.sendParticles(ParticleTypes.FALLING_WATER, pos.getX() + 0.5, pos.getY() + 1.4, pos.getZ() + 0.5,
                30, 1.0, 0.4, 1.0, 0.05);
    }

    private static void airRite(ServerLevel server, BlockPos pos, Player player, boolean amplified) {
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * (amplified ? 70 : 45), 0));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * (amplified ? 50 : 35), amplified ? 1 : 0));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 20 * 40, amplified ? 1 : 0));

        // Gust — shove hostiles outward from the pedestal.
        double strength = amplified ? 1.4 : 0.9;
        for (LivingEntity living : hostilesNear(server, pos)) {
            double dx = living.getX() - pos.getX() - 0.5;
            double dz = living.getZ() - pos.getZ() - 0.5;
            double dist = Math.max(0.35, Math.sqrt(dx * dx + dz * dz));
            living.push(dx / dist * strength, 0.35 + (amplified ? 0.25 : 0.0), dz / dist * strength);
            living.hurtMarked = true;
        }

        if (server.isRaining()) {
            server.setWeatherParameters(6000, 0, false, false);
        }
        server.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                24, 1.0, 0.6, 1.0, 0.04);
    }

    private static void spiritRite(ServerLevel server, BlockPos pos, Player player, boolean amplified) {
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * (amplified ? 35 : 22), amplified ? 1 : 0));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * (amplified ? 120 : 75), 0));
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WITHER);
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.HUNGER);

        float heal = amplified ? 6.0F : 3.0F;
        AABB area = new AABB(pos).inflate(RADIUS);
        for (Player nearby : server.getEntitiesOfClass(Player.class, area)) {
            nearby.heal(heal);
            nearby.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 10, 0));
        }

        for (LivingEntity living : hostilesNear(server, pos)) {
            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * (amplified ? 30 : 18), 0));
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 8, 0));
        }

        // Refund a little Pulse into a carried cell when amplified spirit answers.
        if (amplified) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() instanceof PulseCellItem && PulseCellItem.insertPulse(stack, 12, false) > 0) {
                    break;
                }
            }
        }
        server.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                26, 0.7, 0.5, 0.7, 0.01);
        server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5,
                10, 0.4, 0.4, 0.4, 0.02);
    }

    private static Iterable<LivingEntity> hostilesNear(ServerLevel server, BlockPos pos) {
        return server.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(RADIUS),
                e -> e.isAlive() && e instanceof Enemy);
    }
}
