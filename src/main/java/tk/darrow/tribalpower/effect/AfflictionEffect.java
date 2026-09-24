package tk.darrow.tribalpower.effect;

import java.util.Locale;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.EffectCure;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.config.TribalConfig;

/**
 * What the March leaves on you.
 * <ul>
 *   <li><b>Unsung Hush</b>: no song and no staff voice will answer. The Unsung's Silence bolts leave it.</li>
 *   <li><b>Ley Sickness</b>: the ground sways and your Pulse drains. Standing on a bare ley crossing in a surge.</li>
 *   <li><b>Frayed</b>: something is missing from you, and your health with it. Loom-torn creatures tear it out.</li>
 * </ul>
 * Milk lifts Hush and Ley Sickness; Frayed only lifts for a shaman's cleansing.
 */
public class AfflictionEffect extends MobEffect {
    public enum Kind {
        UNSUNG_HUSH(0x2E2440), LEY_SICKNESS(0x6A9C4C), FRAYED(0x8C5A72);

        public final int colour;

        Kind(int colour) { this.colour = colour; }

        public String id() { return name().toLowerCase(Locale.ROOT); }
    }

    public final Kind kind;
    private final ResourceLocation id;

    public AfflictionEffect(Kind kind) {
        super(MobEffectCategory.HARMFUL, kind.colour);
        this.kind = kind;
        this.id = ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, kind.id());
    }

    @Override
    public void addAttributeModifiers(AttributeMap attributes, int amplifier) {
        super.addAttributeModifiers(attributes, amplifier);
        if (kind != Kind.FRAYED) return;
        var health = attributes.getInstance(Attributes.MAX_HEALTH);
        if (health == null) return;
        health.removeModifier(id);
        double amount = -TribalConfig.frayedHealth() * (amplifier + 1);
        if (amount != 0) health.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    @Override
    public void removeAttributeModifiers(AttributeMap attributes) {
        super.removeAttributeModifiers(attributes);
        var health = attributes.getInstance(Attributes.MAX_HEALTH);
        if (health != null) health.removeModifier(id);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 40 == 0;
    }

    /** Ley Sickness: nausea, and Pulse leaking out of every cell you carry. */
    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide || kind != Kind.LEY_SICKNESS) return true;
        entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, true, false, false));
        if (entity instanceof Player player) tk.darrow.tribalpower.item.SpiritgearHelper.tryConsumePulse(player, TribalConfig.leySicknessDrain() * (amplifier + 1));
        return true;
    }

    /** Frayed is torn, not soured: milk does nothing for it. */
    @Override
    public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance instance) {
        if (kind != Kind.FRAYED) super.fillEffectCures(cures, instance);
    }
}
