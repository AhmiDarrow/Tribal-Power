package tk.darrow.tribalpower.integration.jei;

import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Spirit Codex talks to JEI only through this class. {@link #available()} is safe without JEI:
 * {@link TribalJeiPlugin} is not loaded until JEI is present and a lookup is attempted.
 */
public final class CodexJeiLinks {
    private CodexJeiLinks() {}

    public static boolean available() {
        return ModList.get().isLoaded("jei") && TribalJeiPlugin.ready();
    }

    public static boolean showRecipes(ItemStack stack) {
        return show(stack, false);
    }

    public static boolean showUses(ItemStack stack) {
        return show(stack, true);
    }

    public static boolean show(ItemStack stack, boolean uses) {
        return available() && TribalJeiPlugin.show(stack, uses);
    }
}
