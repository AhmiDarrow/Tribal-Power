package tk.darrow.tribalpower.rite.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import tk.darrow.tribalpower.blockentity.RitualBrazierBlockEntity;

/**
 * A shapeless recipe that hands elemental seals back after crafting — the seal only
 * carves the rite tablet, it is not spent. JSON type {@code tribalpower:seal_keeping_shapeless}
 * with exactly the {@code minecraft:crafting_shapeless} fields.
 */
public class SealKeepingRecipe extends ShapelessRecipe {
    public SealKeepingRecipe(ShapelessRecipe base) {
        super(base.getGroup(), base.category(), base.getResultItem(null), base.getIngredients());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = super.getRemainingItems(input);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (remaining.get(i).isEmpty() && RitualBrazierBlockEntity.element(stack) != null) remaining.set(i, stack.copyWithCount(1));
        }
        return remaining;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return WorldRiteRegistry.SEAL_KEEPING_SERIALIZER.get();
    }

    public static final class Serializer implements RecipeSerializer<SealKeepingRecipe> {
        private final MapCodec<SealKeepingRecipe> codec = RecipeSerializer.SHAPELESS_RECIPE.codec().xmap(SealKeepingRecipe::new, r -> r);
        private final StreamCodec<RegistryFriendlyByteBuf, SealKeepingRecipe> streamCodec =
                RecipeSerializer.SHAPELESS_RECIPE.streamCodec().map(SealKeepingRecipe::new, r -> r);

        @Override public MapCodec<SealKeepingRecipe> codec() { return codec; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, SealKeepingRecipe> streamCodec() { return streamCodec; }
    }
}
