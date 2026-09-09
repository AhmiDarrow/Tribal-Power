package tk.darrow.tribalpower.echo;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.stream.Stream;

/**
 * {@code tribalpower:damaged}: wraps an inner ingredient and matches only stacks that have taken durability damage.
 * JSON: {@code {"type": "tribalpower:damaged", "base": {"tag": "tribalpower:spiritgear_tools"}}}. Used by Echo
 * Unweave salvage so only worn Spiritgear can be unwoven into a Manifested Ingot.
 */
public record DamagedIngredient(Ingredient base) implements ICustomIngredient {
    public static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES = DeferredRegister.create(NeoForgeRegistries.INGREDIENT_TYPES, "tribalpower");
    public static final MapCodec<DamagedIngredient> CODEC = RecordCodecBuilder.mapCodec(b -> b.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("base").forGetter(DamagedIngredient::base)).apply(b, DamagedIngredient::new));
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, DamagedIngredient> STREAM_CODEC =
            Ingredient.CONTENTS_STREAM_CODEC.map(DamagedIngredient::new, DamagedIngredient::base);
    public static final DeferredHolder<IngredientType<?>, IngredientType<DamagedIngredient>> TYPE =
            INGREDIENT_TYPES.register("damaged", () -> new IngredientType<>(CODEC, STREAM_CODEC));

    public static Ingredient of(Ingredient base) { return new DamagedIngredient(base).toVanilla(); }

    @Override
    public boolean test(ItemStack stack) {
        return stack.isDamageableItem() && stack.getDamageValue() > 0 && base.test(stack);
    }

    /** Display stacks are shown worn (one point of damage) so the Codex and JEI convey the durability rule. */
    @Override
    public Stream<ItemStack> getItems() {
        return Stream.of(base.getItems()).filter(ItemStack::isDamageableItem).map(s -> { ItemStack c = s.copy(); c.setDamageValue(1); return c; });
    }

    @Override
    public boolean isSimple() { return false; }

    @Override
    public IngredientType<?> getType() { return TYPE.get(); }
}
