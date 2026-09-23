package tk.darrow.tribalpower.bench;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;

/** The Tribal Bench's menu type. Its block, item and block entity live with the rest of theirs. */
public final class BenchRegistry {
    private BenchRegistry() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, TribalPower.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<BenchMenu>> MENU =
            MENUS.register("tribal_bench", () -> new MenuType<>(BenchMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
