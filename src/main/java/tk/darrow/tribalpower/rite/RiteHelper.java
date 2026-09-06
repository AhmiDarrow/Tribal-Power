package tk.darrow.tribalpower.rite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tk.darrow.tribalpower.item.ModItems;

/**
 * Seals & Rites — seal items on a Rite Pedestal grant short spirit effects.
 * Enchantment application hooks land in a later phase.
 */
public final class RiteHelper {
    private RiteHelper() {}

    public static boolean performSealRite(Level level, BlockPos pos, Player player, ItemStack seal) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }

        if (seal.is(ModItems.BLANK_SEAL.get())) {
            return false;
        }

        if (seal.is(ModItems.EARTH_SEAL.get())) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 30, 0));
        } else if (seal.is(ModItems.FIRE_SEAL.get())) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 45, 0));
        } else if (seal.is(ModItems.WATER_SEAL.get())) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 20 * 60, 0));
        } else if (seal.is(ModItems.AIR_SEAL.get())) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 40, 0));
        } else if (seal.is(ModItems.SPIRIT_SEAL.get())) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 20, 0));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 60, 0));
        } else {
            return false;
        }

        server.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                24, 0.35, 0.4, 0.35, 0.02);
        server.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.2F);
        return true;
    }
}
