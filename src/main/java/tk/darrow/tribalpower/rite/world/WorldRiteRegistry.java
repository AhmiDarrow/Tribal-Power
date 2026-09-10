package tk.darrow.tribalpower.rite.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;

import java.util.EnumMap;
import java.util.Map;

/**
 * World rites registry: the six Rite Tablets, the seal-keeping recipe serializer, the
 * {@code tribalpower:allowDawnRite} game rule and the per-level tick that drives Green Blessing
 * and ley-line particles. Call {@link #register(IEventBus)} from the mod constructor.
 */
public final class WorldRiteRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TribalPower.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, TribalPower.MOD_ID);
    public static final Map<WorldRite, DeferredItem<RiteTabletItem>> TABLETS = new EnumMap<>(WorldRite.class);

    static {
        for (WorldRite rite : WorldRite.values()) {
            TABLETS.put(rite, ITEMS.register(rite.tabletId(), () -> new RiteTabletItem(rite, new Item.Properties().stacksTo(16))));
        }
    }

    public static final DeferredHolder<RecipeSerializer<?>, SealKeepingRecipe.Serializer> SEAL_KEEPING_SERIALIZER =
            SERIALIZERS.register("seal_keeping_shapeless", SealKeepingRecipe.Serializer::new);

    /** Server-wide permission for Dawn Calling to advance time. Default true. */
    public static final GameRules.Key<GameRules.BooleanValue> ALLOW_DAWN_RITE =
            GameRules.register("tribalpower:allowDawnRite", GameRules.Category.MISC, GameRules.BooleanValue.create(true));

    private WorldRiteRegistry() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        SERIALIZERS.register(modBus);
        NeoForge.EVENT_BUS.addListener(WorldRiteRegistry::levelTick);
    }

    public static void displayItems(CreativeModeTab.Output out) {
        for (WorldRite rite : WorldRite.values()) out.accept(TABLETS.get(rite).get());
    }

    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        GreenBlessing.tick(level);
        LeyLines.tick(level);
        Springs.tick(level);
    }
}
