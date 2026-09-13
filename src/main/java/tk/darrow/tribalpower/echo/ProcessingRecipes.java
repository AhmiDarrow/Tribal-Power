package tk.darrow.tribalpower.echo;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tk.darrow.tribalpower.api.pulse.Attunement;

/** Station lookup over Minecraft's synced recipe manager, including external ingredient extensions. */
public final class ProcessingRecipes {
    private ProcessingRecipes() {}
    public record Formula(ResourceLocation id, LatticeRecipe recipe) {
        public ItemStack result() { return recipe.output().copy(); }
        public int seconds() { return recipe.seconds(); }
        public int pulse() { return recipe.pulse(); }
        public Attunement attunement() { return recipe.attunement(); }
    }

    /**
     * The station's formula for this input. A written recipe always wins; when none exists, the grit scan
     * synthesises one for any metal or gem it discovered in the common tags (design 3.1 section 1.4).
     */
    public static Formula find(Level level, String station, ItemStack stack) {
        Formula written = findWritten(level, station, stack);
        if (written != null) return written;
        if ("ember_kiln".equals(station)) {
            Formula grit = tk.darrow.tribalpower.grit.GritRegistry.fire(stack);
            if (grit != null) return grit;
            return kiln(level, stack);
        }
        return tk.darrow.tribalpower.grit.GritRegistry.formula(station, stack);
    }

    /** Every furnace smelting recipe, including grit cooking whose result is only known at assemble. */
    public static Formula kiln(Level level, ItemStack stack) {
        if (level == null || stack.isEmpty()) return null;
        var input = new net.minecraft.world.item.crafting.SingleRecipeInput(stack);
        for (var holder : level.getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.SMELTING)) {
            var cooking = holder.value();
            if (!cooking.matches(input, level)) continue;
            ItemStack output = cooking.assemble(input, level.registryAccess());
            if (output.isEmpty()) output = cooking.getResultItem(level.registryAccess()).copy();
            if (output.isEmpty()) continue;
            LatticeRecipe recipe = new LatticeRecipe("ember_kiln",
                    net.minecraft.world.item.crafting.Ingredient.of(stack.getItem()), output,
                    10, 32, Attunement.FIRE);
            return new Formula(holder.id(), recipe);
        }
        return null;
    }

    /** Datapack and KubeJS recipes only -- the authored half of the lookup. */
    public static Formula findWritten(Level level, String station, ItemStack stack) {
        if (level == null || stack.isEmpty()) return null;
        return level.getRecipeManager().getAllRecipesFor(LatticeRecipe.TYPE.get()).stream()
                .filter(holder -> holder.value().station().equals(station) && holder.value().ingredient().test(stack))
                .sorted(java.util.Comparator.comparing(holder -> holder.id().toString()))
                .map(holder -> new Formula(holder.id(),holder.value())).findFirst().orElse(null);
    }
}
