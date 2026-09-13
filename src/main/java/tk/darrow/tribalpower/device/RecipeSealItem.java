package tk.darrow.tribalpower.device;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/** Blank tablet that remembers one crafting recipe after an imprint at the Seal Loom. */
public class RecipeSealItem extends Item {
    public static final String KEY = "Recipe";

    public RecipeSealItem(Properties properties) {
        super(properties);
    }

    public static boolean isBlank(ItemStack stack) {
        return recipeId(stack) == null;
    }

    public static ResourceLocation recipeId(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof RecipeSealItem)) return null;
        String id = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(KEY);
        return id.isEmpty() ? null : ResourceLocation.tryParse(id);
    }

    public static void imprint(ItemStack stack, ResourceLocation recipe) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(KEY, recipe.toString()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        ResourceLocation id = recipeId(stack);
        if (id == null) lines.add(Component.translatable("item.tribalpower.recipe_seal.blank").withStyle(ChatFormatting.GRAY));
        else lines.add(Component.translatable("item.tribalpower.recipe_seal.imprinted", id.toString()).withStyle(ChatFormatting.AQUA));
    }
}
