package tk.darrow.tribalpower.camp.identity;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Shared camps (design 3.0 §6): the Camp Charter. */
public final class CampIdentityRegistry {
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("tribalpower");
    public static final DeferredItem<CampCharterItem> CAMP_CHARTER=ITEMS.register("camp_charter",()->new CampCharterItem(new Item.Properties().stacksTo(1)));
    public static void displayItems(CreativeModeTab.Output out) { out.accept(CAMP_CHARTER.get()); }
    private CampIdentityRegistry(){}
}
