package tk.darrow.tribalpower.echo;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.*;
import tk.darrow.tribalpower.api.pulse.Attunement;

/** Standard Minecraft recipes: datapack/KubeJS extensible and automatically synced to clients. */
public record LatticeRecipe(String station, Ingredient ingredient, ItemStack output, int seconds, int pulse, Attunement attunement) implements Recipe<SingleRecipeInput> {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, "tribalpower");
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, "tribalpower");
    public static final DeferredHolder<RecipeType<?>,RecipeType<LatticeRecipe>> TYPE = TYPES.register("lattice", () -> new RecipeType<>() {});
    public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<LatticeRecipe>> SERIALIZER = SERIALIZERS.register("lattice", Serializer::new);
    private static final Codec<String> STATION = Codec.STRING.validate(value -> java.util.Set.of("echo_shatter","echo_attune","echo_bind","echo_manifest","echo_unweave").contains(value)
            ? DataResult.success(value) : DataResult.error(() -> "Unknown lattice station: " + value));
    public static final MapCodec<LatticeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            STATION.fieldOf("station").forGetter(LatticeRecipe::station),
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(LatticeRecipe::ingredient),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(LatticeRecipe::output),
            Codec.intRange(1,300).fieldOf("seconds").forGetter(LatticeRecipe::seconds),
            Codec.intRange(1,1000).fieldOf("pulse_per_second").forGetter(LatticeRecipe::pulse),
            Attunement.CODEC.fieldOf("attunement").forGetter(LatticeRecipe::attunement)
    ).apply(instance,LatticeRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf,LatticeRecipe> STREAM = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,LatticeRecipe::station,
            Ingredient.CONTENTS_STREAM_CODEC,LatticeRecipe::ingredient,
            ItemStack.STREAM_CODEC,LatticeRecipe::output,
            ByteBufCodecs.VAR_INT,LatticeRecipe::seconds,
            ByteBufCodecs.VAR_INT,LatticeRecipe::pulse,
            ByteBufCodecs.STRING_UTF8, r -> r.attunement().getSerializedName(),
            (station,ingredient,output,seconds,pulse,element) -> new LatticeRecipe(station,ingredient,output,seconds,pulse,Attunement.byName(element)));
    @Override public boolean matches(SingleRecipeInput input,Level level) { return ingredient.test(input.item()); }
    @Override public ItemStack assemble(SingleRecipeInput input,HolderLookup.Provider registries) { return output.copy(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return output; }
    @Override public boolean canCraftInDimensions(int width,int height) { return width*height>=1; }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY,ingredient); }
    @Override public RecipeType<?> getType() { return TYPE.get(); }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER.get(); }
    @Override public boolean isSpecial() { return true; }
    public static class Serializer implements RecipeSerializer<LatticeRecipe> {
        @Override public MapCodec<LatticeRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf,LatticeRecipe> streamCodec() { return STREAM; }
    }
}
