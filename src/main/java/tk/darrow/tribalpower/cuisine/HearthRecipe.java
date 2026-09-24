package tk.darrow.tribalpower.cuisine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * A Hearth Pot meal: up to four ingredients, in any order, an optional container (a bowl, a bottle) that the meal
 * is served in, a result and how long it simmers. Data-driven, so a pack can add its own dishes.
 */
public record HearthRecipe(List<Ingredient> ingredients, Optional<Ingredient> container, ItemStack result, int seconds)
        implements Recipe<HearthRecipe.Input> {
    public static final int MAX_INGREDIENTS = 4;

    /** What is in the pot: the four ingredient seats and the container seat. */
    public record Input(List<ItemStack> ingredients, ItemStack container) implements RecipeInput {
        @Override public ItemStack getItem(int index) { return index < ingredients.size() ? ingredients.get(index) : container; }
        @Override public int size() { return ingredients.size() + 1; }
    }

    public static final MapCodec<HearthRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.listOf(1, MAX_INGREDIENTS).fieldOf("ingredients").forGetter(HearthRecipe::ingredients),
            Ingredient.CODEC_NONEMPTY.optionalFieldOf("container").forGetter(HearthRecipe::container),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(HearthRecipe::result),
            Codec.INT.optionalFieldOf("seconds", 12).forGetter(HearthRecipe::seconds)).apply(instance, HearthRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, HearthRecipe> STREAM = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), HearthRecipe::ingredients,
            ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC), HearthRecipe::container,
            ItemStack.STREAM_CODEC, HearthRecipe::result,
            ByteBufCodecs.VAR_INT, HearthRecipe::seconds, HearthRecipe::new);

    /** Every ingredient is matched once, in any order; nothing may be left over, and the container must be right. */
    @Override
    public boolean matches(Input input, Level level) {
        List<ItemStack> seated = new ArrayList<>();
        for (ItemStack stack : input.ingredients()) if (!stack.isEmpty()) seated.add(stack);
        if (seated.size() != ingredients.size()) return false;
        List<Ingredient> wanted = new ArrayList<>(ingredients);
        outer:
        for (ItemStack stack : seated) {
            for (int i = 0; i < wanted.size(); i++) {
                if (wanted.get(i).test(stack)) {
                    wanted.remove(i);
                    continue outer;
                }
            }
            return false;
        }
        return container.map(c -> c.test(input.container())).orElse(input.container().isEmpty());
    }

    @Override public ItemStack assemble(Input input, HolderLookup.Provider registries) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result; }
    @Override public RecipeSerializer<?> getSerializer() { return CuisineRegistry.HEARTH_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return CuisineRegistry.HEARTH_TYPE.get(); }
    @Override public boolean isSpecial() { return true; }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> all = NonNullList.create();
        all.addAll(ingredients);
        container.ifPresent(all::add);
        return all;
    }

    public static final class Serializer implements RecipeSerializer<HearthRecipe> {
        @Override public MapCodec<HearthRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, HearthRecipe> streamCodec() { return STREAM; }
    }
}
