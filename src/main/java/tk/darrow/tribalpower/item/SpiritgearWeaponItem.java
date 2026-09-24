package tk.darrow.tribalpower.item;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.config.TribalConfig;

/**
 * The rest of the Spiritgear weapon family. Each is a Blade underneath -- it ranks at the Echo stations, spends
 * Pulse on echo strikes, takes a totem voice and, Manifested, drinks from the blow -- with its own reach, weight
 * and one move of its own ({@link WeaponKind}). Every number comes from the {@code weapons} config section.
 */
public class SpiritgearWeaponItem extends SpiritgearBladeItem {
    private static final ResourceLocation REACH = id("weapon_reach"), SWEEP = id("weapon_sweep"), WEIGHT = id("weapon_weight");

    private final WeaponKind kind;

    public SpiritgearWeaponItem(WeaponKind kind, Properties properties) {
        super(properties, shipped(kind));
        this.kind = kind;
    }

    public WeaponKind kind() {
        return kind;
    }

    @Override
    protected String descKey() {
        return "item.tribalpower." + kind.itemId() + ".desc";
    }

    /** The shipped numbers, baked in so the item is whole even before the config loads. */
    private static ItemAttributeModifiers shipped(WeaponKind kind) {
        var builder = ItemAttributeModifiers.builder();
        for (var entry : modifiers(kind, kind.damage, kind.speed, kind.reach, kind.trait, kind.weight))
            builder.add(entry.attribute(), entry.modifier(), EquipmentSlotGroup.MAINHAND);
        return builder.build();
    }

    private record Entry(Holder<Attribute> attribute, AttributeModifier modifier) {}

    private static java.util.List<Entry> modifiers(WeaponKind kind, double damage, double speed, double reach, double trait, double weight) {
        var out = new java.util.ArrayList<Entry>();
        // The player's own base is 1 damage and 4 swings a second; the tooltip adds them back.
        out.add(new Entry(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damage - 1, AttributeModifier.Operation.ADD_VALUE)));
        out.add(new Entry(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, speed - 4, AttributeModifier.Operation.ADD_VALUE)));
        if (reach != 0) out.add(new Entry(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(REACH, reach, AttributeModifier.Operation.ADD_VALUE)));
        if (kind.sweeps && trait != 0)
            out.add(new Entry(Attributes.SWEEPING_DAMAGE_RATIO, new AttributeModifier(SWEEP, trait, AttributeModifier.Operation.ADD_VALUE)));
        if (weight != 0)
            out.add(new Entry(Attributes.MOVEMENT_SPEED, new AttributeModifier(WEIGHT, weight, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)));
        return out;
    }

    /** Swaps the baked numbers for the configured ones, so a pack's rebalance reaches every weapon already made. */
    public static void attributes(ItemAttributeModifierEvent event) {
        if (!(event.getItemStack().getItem() instanceof SpiritgearWeaponItem weapon)) return;
        WeaponKind kind = weapon.kind;
        for (var entry : modifiers(kind, kind.damage, kind.speed, kind.reach, kind.trait, kind.weight))
            event.removeModifier(entry.attribute(), entry.modifier().id());
        for (var entry : modifiers(kind, TribalConfig.weaponDamage(kind), TribalConfig.weaponSpeed(kind),
                TribalConfig.weaponReach(kind), TribalConfig.weaponTrait(kind), TribalConfig.weaponWeight(kind)))
            event.addModifier(entry.attribute(), entry.modifier(), EquipmentSlotGroup.MAINHAND);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        if (ability == ItemAbilities.SWORD_SWEEP) return kind.sweeps;
        return super.canPerformAction(stack, ability);
    }

    @Override
    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
        return kind.breaksShields || super.canDisableShield(stack, shield, entity, attacker);
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (kind == WeaponKind.SCYTHE && attacker instanceof Player player && !player.level().isClientSide) reap(player, target);
        super.postHurtEnemy(stack, target, attacker);
    }

    /** The scythe's swing carries on through every hostile around the one it struck. */
    private void reap(Player player, LivingEntity struck) {
        float amount = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * TribalConfig.weaponTrait(kind));
        if (amount <= 0) return;
        double radius = 2.0 + Math.max(0, TribalConfig.weaponReach(kind));
        for (LivingEntity other : player.level().getEntitiesOfClass(LivingEntity.class, struck.getBoundingBox().inflate(radius, 0.5, radius),
                e -> e != player && e != struck && e.isAlive() && tk.darrow.tribalpower.familiar.FamiliarRoster.hostile(e))) {
            other.hurt(player.damageSources().playerAttack(player), amount);
        }
        if (player.level() instanceof net.minecraft.server.level.ServerLevel server)
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK, struck.getX(), struck.getY(0.5), struck.getZ(), 3, radius * 0.4, 0.1, radius * 0.4, 0);
    }

    /** The spear's charge, the dagger's back-stab, the warhammer's armour-breaking weight and the trident's bite on wet prey. */
    public static void incomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player) || event.getSource().getDirectEntity() != player
                || !event.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)) return;
        if (!(player.getMainHandItem().getItem() instanceof SpiritgearWeaponItem weapon)) return;
        LivingEntity target = event.getEntity();
        double trait = TribalConfig.weaponTrait(weapon.kind);
        switch (weapon.kind) {
            case SPEAR -> { if (player.isSprinting()) event.setAmount((float) (event.getAmount() * (1 + trait))); }
            case DAGGER -> { if (behind(player, target)) event.setAmount((float) (event.getAmount() * Math.max(1, trait))); }
            case WARHAMMER -> event.setAmount((float) (event.getAmount() + target.getArmorValue() * trait));
            case TRIDENT -> { if (target.isInWaterOrRain()) event.setAmount((float) (event.getAmount() * (1 + trait))); }
            default -> {}
        }
    }

    /** Whether the striker stands in the half behind where the target is facing. */
    static boolean behind(LivingEntity striker, LivingEntity target) {
        Vec3 facing = Vec3.directionFromRotation(0, target.getYHeadRot());
        Vec3 toStriker = new Vec3(striker.getX() - target.getX(), 0, striker.getZ() - target.getZ());
        return toStriker.lengthSqr() > 1.0E-4 && facing.dot(toStriker.normalize()) < -0.2;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, path);
    }
}
