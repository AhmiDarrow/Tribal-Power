package tk.darrow.tribalpower.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.config.TribalConfig;

/**
 * A voice's blessing, one per Attunement.
 * <ul>
 *   <li><b>Earth</b>: digs faster, stands firm against knockback.</li>
 *   <li><b>Fire</b>: blows set the target alight; at level II, fire cannot touch you.</li>
 *   <li><b>Water</b>: swims faster and mends while wet.</li>
 *   <li><b>Air</b>: falls softly and jumps higher.</li>
 *   <li><b>Spirit</b>: hostiles nearby show through walls.</li>
 *   <li><b>Loom</b>: a little of the Pulse gear spends comes back.</li>
 * </ul>
 * The attribute halves live here; the moment-to-moment halves live in {@link EffectHooks}.
 */
public class VoiceBlessingEffect extends MobEffect {
    public final Attunement voice;
    private final ResourceLocation id;

    private static int rgb(org.joml.Vector3f c) {
        return ((int) (c.x * 255) << 16) | ((int) (c.y * 255) << 8) | (int) (c.z * 255);
    }

    public VoiceBlessingEffect(Attunement voice) {
        super(MobEffectCategory.BENEFICIAL, rgb(SpiritEffects.color(voice)));
        this.voice = voice;
        this.id = ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, voice.getSerializedName() + "_voice_blessing");
    }

    @Override
    public void addAttributeModifiers(AttributeMap attributes, int amplifier) {
        super.addAttributeModifiers(attributes, amplifier);
        int levels = amplifier + 1;
        switch (voice) {
            case EARTH -> set(attributes, Attributes.KNOCKBACK_RESISTANCE, TribalConfig.earthBlessingKnockback() * levels, AttributeModifier.Operation.ADD_VALUE);
            case WATER -> set(attributes, net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED, TribalConfig.waterBlessingSwim() * levels, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            case AIR -> set(attributes, Attributes.JUMP_STRENGTH, TribalConfig.airBlessingJump() * levels, AttributeModifier.Operation.ADD_VALUE);
            default -> {}
        }
    }

    private void set(AttributeMap attributes, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double amount,
                     AttributeModifier.Operation operation) {
        var instance = attributes.getInstance(attribute);
        if (instance == null) return;
        instance.removeModifier(id);
        if (amount != 0) instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
    }

    @Override
    public void removeAttributeModifiers(AttributeMap attributes) {
        super.removeAttributeModifiers(attributes);
        for (var attribute : java.util.List.of(Attributes.KNOCKBACK_RESISTANCE, net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED, Attributes.JUMP_STRENGTH)) {
            var instance = attributes.getInstance(attribute);
            if (instance != null) instance.removeModifier(id);
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    /** Once a second: Water mends the wet, Fire II shrugs off fire, Spirit lights up what hunts you. */
    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return true;
        switch (voice) {
            case WATER -> {
                if (entity.isInWaterOrRain() && entity.getHealth() < entity.getMaxHealth()) entity.heal((float) TribalConfig.waterBlessingRegen());
            }
            case FIRE -> {
                if (amplifier >= 1) entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, true, false, false));
            }
            case SPIRIT -> {
                int reach = TribalConfig.spiritBlessingSight();
                for (LivingEntity other : entity.level().getEntitiesOfClass(LivingEntity.class, new AABB(entity.blockPosition()).inflate(reach),
                        e -> e != entity && e instanceof Enemy && e.isAlive()))
                    other.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, false, false));
            }
            default -> {}
        }
        return true;
    }
}
