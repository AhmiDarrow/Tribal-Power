package tk.darrow.tribalpower.grit;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;

/**
 * Registrations for the metal-and-gem split (design 3.1 section 1). Call {@link #register} from the mod
 * constructor.
 */
public final class GritItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TribalPower.MOD_ID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, TribalPower.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, TribalPower.MOD_ID);

    /** Which metal a grit stack holds. The one thing that separates tin grit from lead grit. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> GRIT_MATERIAL =
            COMPONENTS.register("grit_material", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    public static final DeferredItem<MineralGritItem> MINERAL_GRIT =
            ITEMS.register("mineral_grit", () -> new MineralGritItem(new Item.Properties()));

    public static final DeferredHolder<RecipeSerializer<?>, GritCookingRecipe.Serializer> GRIT_COOKING =
            SERIALIZERS.register("grit_cooking", GritCookingRecipe.Serializer::new);

    private GritItems() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        COMPONENTS.register(modBus);
        SERIALIZERS.register(modBus);
        NeoForge.EVENT_BUS.addListener(GritRegistry::onTagsUpdated);
    }

    /**
     * Deliberately not in the creative tab. Tab contents are built before server tags are known in some
     * contexts, so enumerating discovered materials there would either be empty or wrong (design 3.1
     * section 1.4). The three vanilla metals keep their own tab entries; modded grit is reached through
     * JEI, the shatter itself, or {@code /give} with the component.
     */
    public static void displayItems(CreativeModeTab.Output out) {}
}
