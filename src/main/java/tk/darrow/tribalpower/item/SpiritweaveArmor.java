package tk.darrow.tribalpower.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FrostedIceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.api.pulse.Attunement;

import java.util.List;
import java.util.Map;

public class SpiritweaveArmor extends ArmorItem {
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, "tribalpower");
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> MATERIAL = MATERIALS.register("spiritweave", () ->
            new ArmorMaterial(Map.of(Type.HELMET, 3, Type.CHESTPLATE, 8, Type.LEGGINGS, 6, Type.BOOTS, 3, Type.BODY, 8),
                    20, SoundEvents.ARMOR_EQUIP_DIAMOND, () -> Ingredient.of(ModItems.SPIRITWEAVE.get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("tribalpower", "spiritweave"))),
                    2F, 0.05F));
    private static final ResourceLocation STEP = ResourceLocation.fromNamespaceAndPath("tribalpower", "spirit_step");

    public SpiritweaveArmor(Type type, Properties properties) {
        super(MATERIAL, type, properties.durability(type.getDurability(33)));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return SpiritGear.foil(stack) || super.isFoil(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof Player player)) return;

        var step = player.getAttribute(Attributes.STEP_HEIGHT);
        if (getType() == Type.LEGGINGS && step != null) {
            ItemStack worn = player.getItemBySlot(getEquipmentSlot());
            boolean spiritLegs = worn == stack && SpiritGear.voice(stack).orElse(null) == Attunement.SPIRIT;
            if (spiritLegs) {
                step.addOrUpdateTransientModifier(new AttributeModifier(STEP, 1.0, AttributeModifier.Operation.ADD_VALUE));
            } else if (worn != stack && (SpiritGear.voice(worn).orElse(null) != Attunement.SPIRIT
                    || !(worn.getItem() instanceof SpiritweaveArmor))) {
                step.removeModifier(STEP);
            }
        }
        if (player.getItemBySlot(getEquipmentSlot()) != stack) return;

        if (getType() == Type.BOOTS && SpiritGear.voice(stack).orElse(null) == Attunement.WATER) {
            freeze(player, SpiritGear.rank(stack) >= 3 ? 3 : 2);
        }

        if (getType() == Type.BOOTS) {
            if (player.fallDistance > 1 && SpiritGear.voice(stack).orElse(null) == null
                    && !player.hasEffect(MobEffects.SLOW_FALLING)
                    && SpiritgearHelper.tryConsumePulse(player, SpiritGear.armorCost(stack)))
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, true, false, true));
            if (SpiritGear.voice(stack).orElse(null) == Attunement.AIR && player.fallDistance > 1
                    && !player.hasEffect(MobEffects.SLOW_FALLING)
                    && SpiritgearHelper.tryConsumePulse(player, SpiritGear.armorCost(stack)))
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, true, false, true));
            return;
        }

        if (level.getGameTime() % 80 != 0) return;
        Attunement voice = SpiritGear.voice(stack).orElse(null);
        int cost = SpiritGear.armorCost(stack);
        boolean paid = SpiritgearHelper.tryConsumePulse(player, cost);
        if (!paid) return;
        if (voice == Attunement.LOOM && getType() == Type.CHESTPLATE
                && player.getRandom().nextFloat() < (SpiritGear.rank(stack) >= 3 ? 0.50F : 0.30F)) {
            PulseCellItem.insertPulse(findCell(player), cost, false);
        }
        apply(player, stack, voice);
    }

    private void apply(Player player, ItemStack stack, Attunement voice) {
        int rankAmp = SpiritGear.rank(stack) >= 3 ? 1 : 0;
        if (voice == null) {
            var effect = switch (getType()) {
                case HELMET -> MobEffects.NIGHT_VISION;
                case CHESTPLATE -> MobEffects.DAMAGE_RESISTANCE;
                case LEGGINGS -> MobEffects.MOVEMENT_SPEED;
                default -> null;
            };
            if (effect != null) player.addEffect(new MobEffectInstance(effect, getType() == Type.HELMET ? 300 : 100,
                    0, true, false, true));
            return;
        }
        switch (voice) {
            case EARTH -> {
                if (getType() == Type.CHESTPLATE)
                    effect(player, MobEffects.DAMAGE_RESISTANCE, 100, rankAmp);
            }
            case FIRE -> {
                if (getType() == Type.HELMET || getType() == Type.CHESTPLATE)
                    effect(player, MobEffects.FIRE_RESISTANCE, 120, 0);
                if (getType() == Type.LEGGINGS && (player.isInLava() || player.level().dimensionType().ultraWarm()))
                    effect(player, MobEffects.MOVEMENT_SPEED, 100, 0);
            }
            case WATER -> {
                if (getType() == Type.HELMET) effect(player, MobEffects.WATER_BREATHING, 220, 0);
                if (getType() == Type.LEGGINGS) effect(player, MobEffects.DOLPHINS_GRACE, 100, 0);
            }
            case AIR -> {
                if (getType() == Type.HELMET) effect(player, MobEffects.NIGHT_VISION, 300, 0);
                if (getType() == Type.LEGGINGS) effect(player, MobEffects.MOVEMENT_SPEED, 100, rankAmp);
            }
            case SPIRIT -> {
                if (getType() == Type.HELMET) {
                    effect(player, MobEffects.NIGHT_VISION, 300, 0);
                    glowHostiles(player, 12);
                }
                if (getType() == Type.CHESTPLATE) effect(player, MobEffects.DAMAGE_RESISTANCE, 100, 0);
                if (getType() == Type.LEGGINGS) effect(player, MobEffects.MOVEMENT_SPEED, 100, 0);
            }
            case LOOM -> {
                if (getType() == Type.HELMET) effect(player, MobEffects.LUCK, 120, 0);
            }
        }
    }

    private static void effect(Player player, net.minecraft.core.Holder<MobEffect> effect, int duration, int amp) {
        player.addEffect(new MobEffectInstance(effect, duration, amp, true, false, true));
    }

    private static void glowHostiles(Player player, int range) {
        for (LivingEntity mob : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range), tk.darrow.tribalpower.familiar.FamiliarRoster::hostile)) {
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
        }
    }

    private static ItemStack findCell(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof PulseCellItem) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static void freeze(Player player, int radius) {
        BlockPos origin = player.blockPosition();
        Level level = player.level();
        BlockPos.betweenClosed(origin.offset(-radius, -1, -radius), origin.offset(radius, -1, radius)).forEach(pos -> {
            if (pos.distManhattan(origin) > radius) return;
            BlockState state = level.getBlockState(pos);
            if (state.getFluidState().is(Fluids.WATER) && state.getFluidState().isSource()
                    && level.getBlockState(pos.above()).isAir()) {
                level.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState().setValue(FrostedIceBlock.AGE, 0));
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<net.minecraft.network.chat.Component> lines, TooltipFlag flag) {
        lines.add(net.minecraft.network.chat.Component.translatable("item.tribalpower.spiritweave_armor.desc"));
        SpiritGear.appendTooltip(stack, lines, flag);
    }
}
