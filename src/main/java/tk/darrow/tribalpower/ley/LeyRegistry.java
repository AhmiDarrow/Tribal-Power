package tk.darrow.tribalpower.ley;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;

/** Ley sight registry: the Ley Lens. Call {@link #register(IEventBus)} from the mod constructor. */
public final class LeyRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TribalPower.MOD_ID);
    public static final DeferredItem<LeyLensItem> LEY_LENS = ITEMS.register("ley_lens", () -> new LeyLensItem(new Item.Properties().stacksTo(1)));
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, TribalPower.MOD_ID);
    public static final DeferredHolder<RecipeSerializer<?>, GoggleUpgradeRecipe.Serializer> GOGGLES =
            SERIALIZERS.register("goggle_upgrade", GoggleUpgradeRecipe.Serializer::new);

    private LeyRegistry() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        SERIALIZERS.register(modBus);
    }

    public static void displayItems(CreativeModeTab.Output out) {
        out.accept(LEY_LENS.get());
    }
}
