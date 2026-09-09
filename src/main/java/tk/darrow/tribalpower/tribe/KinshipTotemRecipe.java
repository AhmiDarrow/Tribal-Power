package tk.darrow.tribalpower.tribe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import tk.darrow.tribalpower.item.ModItems;

/**
 * Kinship Totem: one Tribe Mark + the Resonance Totem of that tribe's voice + 2 Spiritweave, shapeless.
 * The result carries the mark's tribe.
 */
public class KinshipTotemRecipe extends CustomRecipe {
    public KinshipTotemRecipe(CraftingBookCategory category) { super(category); }

    private static TribeDefinition match(CraftingInput input) {
        TribeDefinition tribe = null;
        ItemStack totem = ItemStack.EMPTY;
        int weave = 0, other = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(TribeRegistry.TRIBE_MARK.get())) {
                if (tribe != null) return null;
                tribe = TribeDefinition.of(stack);
                if (tribe == null) return null;
            } else if (stack.is(ModItems.SPIRITWEAVE.get())) weave++;
            else if (totem.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.BlockItem bi
                    && bi.getBlock() instanceof tk.darrow.tribalpower.block.ResonanceTotemBlock) totem = stack;
            else other++;
        }
        if (tribe == null || weave != 2 || other != 0 || totem.isEmpty()) return null;
        return totem.is(tribe.totem()) ? tribe : null;
    }

    @Override public boolean matches(CraftingInput input, Level level) { return match(input) != null; }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        TribeDefinition tribe = match(input);
        return tribe == null ? ItemStack.EMPTY : tribe.stamped(TribeRegistry.KINSHIP_TOTEM_ITEM.get());
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 4; }
    @Override public RecipeSerializer<?> getSerializer() { return TribeRegistry.KINSHIP_RECIPE.get(); }
}
