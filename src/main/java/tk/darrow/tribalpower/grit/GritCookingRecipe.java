package tk.darrow.tribalpower.grit;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;

/**
 * The fire half of the metal craft, for metals nobody wrote a recipe for (design 3.1 section 1.4).
 *
 * <p>Vanilla metals get generated smelting and blasting JSON so JEI and the recipe book read correctly.
 * This recipe is what catches a modded metal in an ordinary furnace: it matches any mineral grit whose
 * material the tag scan knows, and resolves the ingot from the stack's own component at assemble time,
 * which is the only moment the result is knowable.
 *
 * <p>It reports itself special so the recipe book does not try to show one entry standing for every
 * metal at once; the per-material entries come from the scan instead.
 *
 * <p>The live objects are {@link Smelting} / {@link Blasting} so they are {@link SmeltingRecipe} and
 * {@link BlastingRecipe}. Mods that iterate {@link RecipeType#SMELTING} and cast (Mekanism 10.7.19
 * {@code IncompleteRecipeScanner}) crash if grit cooking is only {@link AbstractCookingRecipe}.
 */
public final class GritCookingRecipe {
    private GritCookingRecipe() {}

    public static AbstractCookingRecipe create(String group, CookingBookCategory category, Ingredient ingredient,
                                               float experience, int cookingTime, boolean blasting) {
        return blasting
                ? new Blasting(group, category, ingredient, experience, cookingTime)
                : new Smelting(group, category, ingredient, experience, cookingTime);
    }

    public static boolean blasting(AbstractCookingRecipe recipe) {
        return recipe.getType() == RecipeType.BLASTING;
    }

    public static final class Smelting extends SmeltingRecipe {
        public Smelting(String group, CookingBookCategory category, Ingredient ingredient,
                        float experience, int cookingTime) {
            super(group, category, ingredient, ItemStack.EMPTY, experience, cookingTime);
        }

        @Override
        public boolean matches(SingleRecipeInput input, Level level) {
            return super.matches(input, level) && !GritRegistry.ingotFor(input.item()).isEmpty();
        }

        @Override
        public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
            return GritRegistry.ingotFor(input.item());
        }

        @Override public boolean isSpecial() { return true; }

        @Override public RecipeSerializer<?> getSerializer() { return GritItems.GRIT_COOKING.get(); }
    }

    public static final class Blasting extends BlastingRecipe {
        public Blasting(String group, CookingBookCategory category, Ingredient ingredient,
                        float experience, int cookingTime) {
            super(group, category, ingredient, ItemStack.EMPTY, experience, cookingTime);
        }

        @Override
        public boolean matches(SingleRecipeInput input, Level level) {
            return super.matches(input, level) && !GritRegistry.ingotFor(input.item()).isEmpty();
        }

        @Override
        public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
            return GritRegistry.ingotFor(input.item());
        }

        @Override public boolean isSpecial() { return true; }

        @Override public RecipeSerializer<?> getSerializer() { return GritItems.GRIT_COOKING.get(); }
    }

    public static class Serializer implements RecipeSerializer<AbstractCookingRecipe> {
        public static final MapCodec<AbstractCookingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                com.mojang.serialization.Codec.STRING.optionalFieldOf("group", "").forGetter(AbstractCookingRecipe::getGroup),
                CookingBookCategory.CODEC.optionalFieldOf("category", CookingBookCategory.MISC).forGetter(AbstractCookingRecipe::category),
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(recipe -> recipe.getIngredients().getFirst()),
                com.mojang.serialization.Codec.FLOAT.optionalFieldOf("experience", 0.0F).forGetter(AbstractCookingRecipe::getExperience),
                com.mojang.serialization.Codec.INT.optionalFieldOf("cookingtime", 200).forGetter(AbstractCookingRecipe::getCookingTime),
                com.mojang.serialization.Codec.BOOL.optionalFieldOf("blasting", false).forGetter(GritCookingRecipe::blasting)
        ).apply(instance, GritCookingRecipe::create));

        public static final StreamCodec<RegistryFriendlyByteBuf, AbstractCookingRecipe> STREAM = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, AbstractCookingRecipe::getGroup,
                net.minecraft.network.codec.ByteBufCodecs.idMapper(i -> CookingBookCategory.values()[i], CookingBookCategory::ordinal), AbstractCookingRecipe::category,
                Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.getIngredients().getFirst(),
                ByteBufCodecs.FLOAT, AbstractCookingRecipe::getExperience,
                ByteBufCodecs.VAR_INT, AbstractCookingRecipe::getCookingTime,
                ByteBufCodecs.BOOL, GritCookingRecipe::blasting,
                GritCookingRecipe::create);

        @Override public MapCodec<AbstractCookingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, AbstractCookingRecipe> streamCodec() { return STREAM; }
    }
}
