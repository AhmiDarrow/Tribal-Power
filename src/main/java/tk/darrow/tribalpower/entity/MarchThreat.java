package tk.darrow.tribalpower.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * What makes the March dangerous beyond its spawn tables. Spirits that rise at night come stronger, and some
 * rise as elites: tougher, harder-hitting, named in gold so you see them coming, and worth more experience.
 */
public final class MarchThreat {
    private static final ResourceLocation NIGHT = ResourceLocation.fromNamespaceAndPath("tribalpower", "march_night");
    public static final ResourceLocation ELITE = ResourceLocation.fromNamespaceAndPath("tribalpower", "march_elite");

    private MarchThreat() {}

    /** Returns true when the monster rose as an elite. */
    public static boolean empower(Monster monster, ServerLevelAccessor level, DifficultyInstance difficulty) {
        var random = level.getRandom();
        boolean night = level.getLevel().isNight();
        if (night) {
            add(monster, NIGHT, 0.4, 0.25, 0);
        }
        float eliteChance = (night ? 0.10F : 0.05F) + (level.getDifficulty() == Difficulty.HARD ? 0.05F : 0)
                + difficulty.getSpecialMultiplier() * 0.05F;
        if (random.nextFloat() < eliteChance) {
            add(monster, ELITE, 1.0, 0.5, 4);
            monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1, 0, false, false));
            monster.setCustomName(Component.translatable("entity.tribalpower.elite", monster.getType().getDescription())
                    .withStyle(ChatFormatting.GOLD));
            monster.setHealth(monster.getMaxHealth());
            return true;
        }
        monster.setHealth(monster.getMaxHealth());
        return false;
    }

    private static void add(Monster monster, ResourceLocation id, double health, double damage, double armor) {
        var maxHealth = monster.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null && !maxHealth.hasModifier(id)) maxHealth.addPermanentModifier(new AttributeModifier(id, health, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        var attack = monster.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null && !attack.hasModifier(id)) attack.addPermanentModifier(new AttributeModifier(id, damage, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        var armour = monster.getAttribute(Attributes.ARMOR);
        if (armour != null && armor > 0 && !armour.hasModifier(id)) armour.addPermanentModifier(new AttributeModifier(id, armor, AttributeModifier.Operation.ADD_VALUE));
    }
}
