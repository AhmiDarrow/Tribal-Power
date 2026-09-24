package tk.darrow.tribalpower.kit;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.item.RitualChalkItem;

/**
 * A reagent ground in a bowl with a little chalk: a shapeless recipe that gives the bowl back and spends only one
 * mark of the chalk, and makes as much dye as the config says.
 */
public class GrindingRecipe extends ShapelessRecipe {
    public GrindingRecipe(ShapelessRecipe base) {
        super(base.getGroup(), base.category(), base.getResultItem(null), base.getIngredients());
    }

    /**
     * One stick of chalk at a time. A whole stack would hand back a part-used stick on every craft, each one its
     * own stack, and bury the inventory in them.
     */
    @Override
    public boolean matches(CraftingInput input, net.minecraft.world.level.Level level) {
        for (int i = 0; i < input.size(); i++)
            if (input.getItem(i).getItem() instanceof RitualChalkItem && input.getItem(i).getCount() > 1) return false;
        return super.matches(input, level);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack out = super.assemble(input, registries);
        out.setCount(Math.min(out.getMaxStackSize(), TribalConfig.dyeYield()));
        return out;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        ItemStack out = super.getResultItem(registries).copy();
        out.setCount(Math.min(out.getMaxStackSize(), TribalConfig.dyeYield()));
        return out;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = super.getRemainingItems(input);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.is(Items.BOWL)) remaining.set(i, new ItemStack(Items.BOWL));
            else if (stack.getItem() instanceof RitualChalkItem) {
                ItemStack chalk = stack.copyWithCount(1);
                RitualChalkItem.spend(chalk, null);
                remaining.set(i, chalk);
            }
        }
        return remaining;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return KitRegistry.GRINDING.get();
    }

    public static final class Serializer implements RecipeSerializer<GrindingRecipe> {
        private final MapCodec<GrindingRecipe> codec = RecipeSerializer.SHAPELESS_RECIPE.codec().xmap(GrindingRecipe::new, r -> r);
        private final StreamCodec<RegistryFriendlyByteBuf, GrindingRecipe> streamCodec =
                RecipeSerializer.SHAPELESS_RECIPE.streamCodec().map(GrindingRecipe::new, r -> r);

        @Override public MapCodec<GrindingRecipe> codec() { return codec; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, GrindingRecipe> streamCodec() { return streamCodec; }
    }
}
