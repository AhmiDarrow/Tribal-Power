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
    public static Formula find(Level level, String station, ItemStack stack) {
        if (level == null || stack.isEmpty()) return null;
        return level.getRecipeManager().getAllRecipesFor(LatticeRecipe.TYPE.get()).stream()
                .filter(holder -> holder.value().station().equals(station) && holder.value().ingredient().test(stack))
                .sorted(java.util.Comparator.comparing(holder -> holder.id().toString()))
                .map(holder -> new Formula(holder.id(),holder.value())).findFirst().orElse(null);
    }
}
