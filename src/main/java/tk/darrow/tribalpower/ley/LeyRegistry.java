package tk.darrow.tribalpower.ley;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;

/** Ley sight registry: the Ley Lens. Call {@link #register(IEventBus)} from the mod constructor. */
public final class LeyRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TribalPower.MOD_ID);
    public static final DeferredItem<LeyLensItem> LEY_LENS = ITEMS.register("ley_lens", () -> new LeyLensItem(new Item.Properties().stacksTo(1)));

    private LeyRegistry() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    public static void displayItems(CreativeModeTab.Output out) {
        out.accept(LEY_LENS.get());
    }
}
