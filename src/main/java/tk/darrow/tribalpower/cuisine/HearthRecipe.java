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
import org.jetbrains.annotations.Nullable;

/**
 * A Hearth Pot meal: up to four ingredients, each in its own seat, an optional container (a bowl, a bottle) that the
 * meal is served in, a result and how long it simmers. Data-driven, so a pack can add its own dishes.
 *
 * <p>The seats are laid out two by two, and ingredient {@code i} belongs in seat {@code i}: top left, top right, bottom
 * left, bottom right, the way the pot's screen, JEI and EMI all draw a recipe.
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

    /** Every ingredient sits in its own seat, the seats past the last ingredient are empty, and the container is right. */
    @Override
    public boolean matches(Input input, Level level) {
        return plan(input) != null;
    }

    /**
     * How many to take from each ingredient seat for one meal (one from each seat the recipe uses), or null when the
     * seats do not make this meal: an ingredient in the wrong seat, a seat the recipe leaves empty holding something.
     */
    public int @Nullable [] plan(Input input) {
        List<ItemStack> seats = input.ingredients();
        if (ingredients.size() > seats.size()) return null;
        int[] take = new int[seats.size()];
        for (int i = 0; i < seats.size(); i++) {
            ItemStack seat = seats.get(i);
            if (i < ingredients.size()) {
                if (seat.isEmpty() || !ingredients.get(i).test(seat)) return null;
                take[i] = 1;
            } else if (!seat.isEmpty()) {
                return null;
            }
        }
        boolean containerRight = container.map(c -> c.test(input.container())).orElse(input.container().isEmpty());
        return containerRight ? take : null;
    }

    /**
     * Whether {@code stack} in seat {@code slot} could be part of this meal given what the other seats already hold:
     * the recipe wants it there, and every other filled seat holds what the recipe wants in it.
     */
    public boolean fits(List<ItemStack> seats, int slot, ItemStack stack) {
        if (slot >= ingredients.size() || !ingredients.get(slot).test(stack)) return false;
        for (int i = 0; i < seats.size(); i++) {
            if (i == slot || seats.get(i).isEmpty()) continue;
            if (i >= ingredients.size() || !ingredients.get(i).test(seats.get(i))) return false;
        }
        return true;
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
