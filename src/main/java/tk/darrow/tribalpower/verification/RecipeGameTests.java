package tk.darrow.tribalpower.verification;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.TribalPower;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Two crafting recipes with the same shape and the same ingredients are not a harmless duplicate:
 * the recipe manager returns whichever it finds first and the other is silently uncraftable.
 * 3.7.0 shipped three of them. This reads the recipes the server actually loaded, so it also
 * catches one of ours colliding with a vanilla recipe, which no check over our own JSON would see.
 */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class RecipeGameTests {

    /** The set of items that can fill one slot, as a stable string. */
    private static String slot(Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) return "-";
        TreeSet<String> items = new TreeSet<>();
        for (ItemStack stack : ingredient.getItems()) {
            items.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        }
        return items.toString();
    }

    /** Width, height and every slot; compared against the mirror because the grid matches flipped. */
    private static String shapedSignature(ShapedRecipe recipe) {
        int width = recipe.getWidth();
        int height = recipe.getHeight();
        List<Ingredient> slots = recipe.getIngredients();

        StringBuilder forward = new StringBuilder(width + "x" + height + ":");
        StringBuilder mirrored = new StringBuilder(width + "x" + height + ":");
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                int index = row * width + column;
                int flipped = row * width + (width - 1 - column);
                forward.append(index < slots.size() ? slot(slots.get(index)) : "-").append('|');
                mirrored.append(flipped < slots.size() ? slot(slots.get(flipped)) : "-").append('|');
            }
        }
        String a = forward.toString();
        String b = mirrored.toString();
        return a.compareTo(b) <= 0 ? a : b;
    }

    private static String shapelessSignature(ShapelessRecipe recipe) {
        List<String> slots = new ArrayList<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            slots.add(slot(ingredient));
        }
        Collections.sort(slots);
        return "shapeless:" + slots;
    }

    @GameTest(template = "empty")
    public static void noTwoCraftingRecipesClaimTheSameGrid(GameTestHelper h) {
        Map<String, List<ResourceLocation>> byGrid = new HashMap<>();

        for (RecipeHolder<?> holder : h.getLevel().getRecipeManager().getRecipes()) {
            Recipe<?> recipe = holder.value();
            String signature;
            if (recipe instanceof ShapedRecipe shaped) {
                signature = shapedSignature(shaped);
            } else if (recipe instanceof ShapelessRecipe shapeless) {
                signature = shapelessSignature(shapeless);
            } else {
                continue;
            }
            byGrid.computeIfAbsent(signature, key -> new ArrayList<>()).add(holder.id());
        }

        List<String> clashes = new ArrayList<>();
        for (List<ResourceLocation> ids : byGrid.values()) {
            if (ids.size() < 2) continue;
            // Only our problem when one of ours is in the pile; two other mods clashing is theirs.
            if (ids.stream().noneMatch(id -> id.getNamespace().equals(TribalPower.MOD_ID))) continue;
            List<String> named = new ArrayList<>();
            for (ResourceLocation id : ids) named.add(id.toString());
            Collections.sort(named);
            clashes.add(String.join(" == ", named));
        }
        Collections.sort(clashes);

        h.assertTrue(clashes.isEmpty(),
                "These recipes share one grid, so one of each is uncraftable: " + String.join("; ", clashes));
        h.succeed();
    }
}
