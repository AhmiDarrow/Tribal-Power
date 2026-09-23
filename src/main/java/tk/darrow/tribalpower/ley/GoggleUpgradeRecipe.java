package tk.darrow.tribalpower.ley;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.item.SpiritGear;

/**
 * Fits a Ley Lens into a Spiritweave Hood. The lens is spent. Rank, voice and damage on the hood stay.
 * JSON type {@code tribalpower:goggle_upgrade}, with the fields of a shapeless recipe.
 */
public class GoggleUpgradeRecipe extends ShapelessRecipe {
    public GoggleUpgradeRecipe(ShapelessRecipe base) {
        super(base.getGroup(), base.category(), base.getResultItem(null), base.getIngredients());
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (!super.matches(input, level)) return false;
        for (int i = 0; i < input.size(); i++) {
            if (SpiritGear.goggles(input.getItem(i))) return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack hood = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.is(ModItems.SPIRITWEAVE_HOOD.get())) hood = stack;
        }
        if (hood.isEmpty() || SpiritGear.goggles(hood)) return ItemStack.EMPTY;
        ItemStack out = hood.copyWithCount(1);
        SpiritGear.setGoggles(out, true);
        return out;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return LeyRegistry.GOGGLES.get();
    }

    public static final class Serializer implements RecipeSerializer<GoggleUpgradeRecipe> {
        private final MapCodec<GoggleUpgradeRecipe> codec = RecipeSerializer.SHAPELESS_RECIPE.codec().xmap(GoggleUpgradeRecipe::new, r -> r);
        private final StreamCodec<RegistryFriendlyByteBuf, GoggleUpgradeRecipe> streamCodec =
                RecipeSerializer.SHAPELESS_RECIPE.streamCodec().map(GoggleUpgradeRecipe::new, r -> r);

        @Override public MapCodec<GoggleUpgradeRecipe> codec() { return codec; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, GoggleUpgradeRecipe> streamCodec() { return streamCodec; }
    }
}
