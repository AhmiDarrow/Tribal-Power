package tk.darrow.tribalpower.grit;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * One item for every metal the tag scan finds beyond the three that shipped before 3.1 (design 3.1
 * section 1.3). The material rides in a {@code tribalpower:grit_material} data component, so the item
 * renders as "Tin Grit" from one greyscale sprite tinted per material and nothing else in the mod has to
 * know which metal a given stack holds.
 */
public class MineralGritItem extends Item {
    public MineralGritItem(Properties properties) {
        super(properties);
    }

    /** A grit stack for {@code material}. */
    public static ItemStack of(String material) {
        ItemStack stack = new ItemStack(GritItems.MINERAL_GRIT.get());
        stack.set(GritItems.GRIT_MATERIAL.get(), material);
        return stack;
    }

    /** The material this stack holds, or null when it is not mineral grit or carries no material. */
    public static String materialOf(ItemStack stack) {
        if (!stack.is(GritItems.MINERAL_GRIT.get())) return null;
        String material = stack.get(GritItems.GRIT_MATERIAL.get());
        return material == null || material.isBlank() ? null : material;
    }

    /**
     * A stable colour per material, so tin and lead do not look alike. Derived from the material name
     * rather than configured: a modded metal has to get a colour without anyone writing one down.
     */
    public static int tint(String material) {
        if (material == null) return 0xFFFFFF;
        // Keep the sprite readable: mid lightness, gentle saturation, hue from the name.
        return hsb(Math.floorMod(material.hashCode(), 360) / 360.0F, 0.32F, 0.78F);
    }

    /** Local HSB conversion: the server has no business loading AWT to pick a tint. */
    private static int hsb(float hue, float saturation, float value) {
        float h = (hue - (float) Math.floor(hue)) * 6.0F;
        float f = h - (float) Math.floor(h);
        float p = value * (1.0F - saturation);
        float q = value * (1.0F - saturation * f);
        float t = value * (1.0F - saturation * (1.0F - f));
        float r, g, b;
        switch ((int) h) {
            case 0 -> { r = value; g = t; b = p; }
            case 1 -> { r = q; g = value; b = p; }
            case 2 -> { r = p; g = value; b = t; }
            case 3 -> { r = p; g = q; b = value; }
            case 4 -> { r = t; g = p; b = value; }
            default -> { r = value; g = p; b = q; }
        }
        return (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
    }

    @Override
    public Component getName(ItemStack stack) {
        String material = materialOf(stack);
        if (material == null) return super.getName(stack);
        GritRegistry.Material known = GritRegistry.material(material);
        Component name = known != null ? known.displayName() : Component.literal(material);
        return Component.translatable("item.tribalpower.mineral_grit.named", name);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String material = materialOf(stack);
        if (material == null) {
            tooltip.add(Component.translatable("item.tribalpower.mineral_grit.unbound").withStyle(ChatFormatting.RED));
            return;
        }
        ItemStack ingot = GritRegistry.ingotFor(stack);
        if (ingot.isEmpty())
            tooltip.add(Component.translatable("item.tribalpower.mineral_grit.no_ingot").withStyle(ChatFormatting.YELLOW));
        else
            tooltip.add(Component.translatable("item.tribalpower.mineral_grit.fires_into", ingot.getHoverName())
                    .withStyle(ChatFormatting.GRAY));
    }
}
