package tk.darrow.tribalpower.item;
import java.util.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import tk.darrow.tribalpower.entity.*;
public final class CreatureItems {
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("tribalpower");
    public static final Map<CreatureProfile,DeferredItem<Item>> REAGENTS=new EnumMap<>(CreatureProfile.class);
    public static final Map<CreatureProfile,DeferredItem<DeferredSpawnEggItem>> EGGS=new EnumMap<>(CreatureProfile.class);
    static {
        for(var p:CreatureProfile.values()) {
            REAGENTS.put(p,ITEMS.registerSimpleItem(p.reagent));
            EGGS.put(p,ITEMS.register(p.id+"_spawn_egg",()->new DeferredSpawnEggItem(()->CreatureEntities.type(p),p.color,p.glow,new Item.Properties())));
        }
    }
}
