package tk.darrow.tribalpower.anvil;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringUtil;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.item.ModItems;

/**
 * The Spirit Anvil's menu type and its one rule beyond vanilla: Spiritgear (the {@code tribalpower:spiritgear} tag)
 * mends with Manifested Ingots, a quarter of its durability per ingot, the way a vanilla anvil mends a tool with its
 * own material. Everything else (renaming, books, combining, diamonds) is the vanilla anvil's own.
 */
public final class SpiritAnvil {
    private SpiritAnvil() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, TribalPower.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<SpiritAnvilMenu>> MENU =
            MENUS.register("spirit_anvil", () -> new MenuType<>(SpiritAnvilMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final TagKey<Item> SPIRITGEAR =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "spiritgear"));

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
        NeoForge.EVENT_BUS.addListener(SpiritAnvil::anvilUpdate);
    }

    /** A mend: the mended item, its level cost, and how many ingots it takes. */
    public record Repair(ItemStack result, int cost, int ingots) {}

    /** Mends {@code gear} with {@code ingots}, renaming it to {@code name} as an anvil does; null if this is no mend. */
    @Nullable
    public static Repair repair(ItemStack gear, ItemStack ingots, @Nullable String name) {
        if (gear.isEmpty() || !gear.is(SPIRITGEAR) || !gear.isDamageableItem() || !gear.isDamaged()
                || !ingots.is(ModItems.MANIFESTED_INGOT.get())) return null;
        ItemStack result = gear.copy();
        int used = 0;
        for (int step = Math.min(result.getDamageValue(), result.getMaxDamage() / 4); step > 0 && used < ingots.getCount();
                step = Math.min(result.getDamageValue(), result.getMaxDamage() / 4)) {
            result.setDamageValue(result.getDamageValue() - step);
            used++;
        }
        int prior = gear.getOrDefault(DataComponents.REPAIR_COST, 0);
        int cost = used + prior + ingots.getOrDefault(DataComponents.REPAIR_COST, 0);
        if (name != null && !StringUtil.isBlank(name)) {
            if (!name.equals(gear.getHoverName().getString())) {
                cost++;
                result.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            }
        } else if (gear.has(DataComponents.CUSTOM_NAME)) {
            cost++;
            result.remove(DataComponents.CUSTOM_NAME);
        }
        result.set(DataComponents.REPAIR_COST, AnvilMenu.calculateIncreasedRepairCost(prior));
        return new Repair(result, cost, used);
    }

    private static void anvilUpdate(AnvilUpdateEvent event) {
        if (event.getPlayer() == null || !(event.getPlayer().containerMenu instanceof SpiritAnvilMenu)) return;
        Repair repair = repair(event.getLeft(), event.getRight(), event.getName());
        if (repair == null) return;
        event.setOutput(repair.result());
        event.setCost(repair.cost());
        event.setMaterialCost(repair.ingots());
    }
}
