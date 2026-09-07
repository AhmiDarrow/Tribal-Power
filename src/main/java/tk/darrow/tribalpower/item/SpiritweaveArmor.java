package tk.darrow.tribalpower.item;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.*;
import java.util.*;

public class SpiritweaveArmor extends ArmorItem {
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, "tribalpower");
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> MATERIAL = MATERIALS.register("spiritweave", () ->
            new ArmorMaterial(Map.of(Type.HELMET, 2, Type.CHESTPLATE, 6, Type.LEGGINGS, 5, Type.BOOTS, 2, Type.BODY, 6),
                    20, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(ModItems.SPIRITWEAVE.get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("tribalpower", "spiritweave"))), 1F, 0F));
    public SpiritweaveArmor(Type type, Properties properties) { super(MATERIAL, type, properties.durability(type.getDurability(25))); }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof Player player)
                || player.getItemBySlot(getEquipmentSlot()) != stack) return;
        if (getType() == Type.BOOTS) {
            if (player.fallDistance > 1 && !player.hasEffect(MobEffects.SLOW_FALLING)
                    && SpiritgearHelper.tryConsumePulse(player, 2))
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, true, false, true));
            return;
        }
        if (level.getGameTime() % 80 != 0) return;
        // Only equipped pieces draw Pulse, and only when their benefit is useful.
        var effect = switch(getType()) {
            case HELMET -> MobEffects.NIGHT_VISION;
            case CHESTPLATE -> MobEffects.DAMAGE_RESISTANCE;
            case LEGGINGS -> MobEffects.MOVEMENT_SPEED;
            case BOOTS -> MobEffects.SLOW_FALLING;
            default -> null;
        };
        if (effect != null && (getType() != Type.BOOTS || player.fallDistance > 1)
                && SpiritgearHelper.tryConsumePulse(player, 2))
            player.addEffect(new MobEffectInstance(effect, getType() == Type.HELMET ? 300 : 100, 0, true, false, true));
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<net.minecraft.network.chat.Component> lines, TooltipFlag flag) {
        lines.add(net.minecraft.network.chat.Component.translatable("item.tribalpower.spiritweave_armor.desc"));
    }
}
