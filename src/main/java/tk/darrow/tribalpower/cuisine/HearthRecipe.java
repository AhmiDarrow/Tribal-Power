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

    /**
     * Every ingredient is matched once, in any order, and a stack in one seat can stand for that many; every seated
     * stack must be used by something, and the container must be right.
     */
    @Override
    public boolean matches(Input input, Level level) {
        return plan(input) != null;
    }

    /**
     * How many to take from each ingredient seat for one meal, or null when the seats do not make this meal. A
     * seat holding two emberroot answers for "emberroot, emberroot"; a seat nothing asks for spoils the pot.
     */
    public int @Nullable [] plan(Input input) {
        List<ItemStack> seats = input.ingredients();
        int[] take = new int[seats.size()];
        if (!assign(seats, take, 0)) return null;
        for (int i = 0; i < seats.size(); i++) if (!seats.get(i).isEmpty() && take[i] == 0) return null;
        boolean containerRight = container.map(c -> c.test(input.container())).orElse(input.container().isEmpty());
        return containerRight ? take : null;
    }

    /**
     * Seats the wanted ingredients from {@code from} on, trying every seat that fits and backing out of a choice that
     * leaves a later ingredient with nothing (a tag that took the one seat a named ingredient needed). A seat not yet
     * drawn on is tried first, so two emberroot in two seats are both used before a stack is.
     */
    private boolean assign(List<ItemStack> seats, int[] take, int from) {
        if (from == ingredients.size()) return true;
        Ingredient wanted = ingredients.get(from);
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < seats.size(); i++) {
                ItemStack seat = seats.get(i);
                if (seat.isEmpty() || !wanted.test(seat) || seat.getCount() <= take[i]) continue;
                if ((pass == 0) == (take[i] > 0)) continue;
                take[i]++;
                if (assign(seats, take, from + 1)) return true;
                take[i]--;
            }
        }
        return false;
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
