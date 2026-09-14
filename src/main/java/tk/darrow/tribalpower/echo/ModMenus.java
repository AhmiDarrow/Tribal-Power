package tk.darrow.tribalpower.echo;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.charm.CharmMenu;

/** Register every shared menu before registry events, independently of screen class loading. */
public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "tribalpower");
    public static final DeferredHolder<MenuType<?>, MenuType<StationMenu>> STATION =
            MENUS.register("echo_station", () -> new MenuType<>(StationMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<RelayMenu>> RELAY =
            MENUS.register("relay", () -> new MenuType<>(RelayMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<CacheMenu>> CACHE =
            MENUS.register("cache", () -> new MenuType<>(CacheMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<CharmMenu>> CHARMS =
            MENUS.register("charm_slots", () -> new MenuType<>(CharmMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {}
}
