package tk.darrow.tribalpower.healing;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.EffectCure;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.config.TribalConfig;

/**
 * The two spirit effects. Their attribute changes are read from the config when they take hold, rather than baked
 * in at registration, so a pack's numbers apply; and milk touches neither -- only shamanic remedies, the Healing
 * Circle and the Sweat Lodge lift Spirit Sickness.
 */
public class SpiritEffect extends MobEffect {
    public enum Kind { SICKNESS, BLESSING }

    private final Kind kind;
    /** One id per effect, so lifting the sickness never strips the blessing's health or the other way round. */
    private final ResourceLocation health, speed;

    public SpiritEffect(Kind kind) {
        super(kind == Kind.SICKNESS ? MobEffectCategory.HARMFUL : MobEffectCategory.BENEFICIAL, kind == Kind.SICKNESS ? 0x5B4A6E : 0x7EFFCB);
        this.kind = kind;
        String name = kind == Kind.SICKNESS ? "spirit_sickness" : "spirit_blessing";
        this.health = ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, name + "_health");
        this.speed = ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, name + "_speed");
    }

    @Override
    public void addAttributeModifiers(AttributeMap attributes, int amplifier) {
        super.addAttributeModifiers(attributes, amplifier);
        int levels = amplifier + 1;
        var maxHealth = attributes.getInstance(Attributes.MAX_HEALTH);
        if (kind == Kind.SICKNESS) {
            if (maxHealth != null) replace(maxHealth, health, -TribalConfig.sicknessHealthPerLevel() * levels, AttributeModifier.Operation.ADD_VALUE);
            var movement = attributes.getInstance(Attributes.MOVEMENT_SPEED);
            if (movement != null) replace(movement, speed, -TribalConfig.sicknessSlowPerLevel() * levels, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        } else if (maxHealth != null) {
            replace(maxHealth, health, TribalConfig.blessingHealth() * levels, AttributeModifier.Operation.ADD_VALUE);
        }
    }

    @Override
    public void removeAttributeModifiers(AttributeMap attributes) {
        super.removeAttributeModifiers(attributes);
        var maxHealth = attributes.getInstance(Attributes.MAX_HEALTH);
        if (maxHealth != null) maxHealth.removeModifier(health);
        var movement = attributes.getInstance(Attributes.MOVEMENT_SPEED);
        if (movement != null) movement.removeModifier(speed);
    }

    private static void replace(net.minecraft.world.entity.ai.attributes.AttributeInstance instance, ResourceLocation id, double amount,
                                AttributeModifier.Operation operation) {
        instance.removeModifier(id);
        // Saved with the player, as vanilla's own effect modifiers are, so a relog keeps the effect whole.
        if (amount != 0) instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
    }

    /** Neither is lifted by milk or honey. */
    @Override
    public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance instance) {}
}
