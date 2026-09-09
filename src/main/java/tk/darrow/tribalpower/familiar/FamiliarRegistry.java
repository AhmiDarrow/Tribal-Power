package tk.darrow.tribalpower.familiar;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.*;

/** Bonded spirits (design 3.0 §5): the Bonding Charm, the Mossback saddlebag menu and the fox's invisible Spirit Light. */
public final class FamiliarRegistry {
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("tribalpower");
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("tribalpower");
    public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,"tribalpower");
    public static final DeferredItem<BondingCharmItem> BONDING_CHARM=ITEMS.register("bonding_charm",()->new BondingCharmItem(new Item.Properties().stacksTo(16)));
    /** Invisible, collision-free, replaceable light source (level 10) that a bonded Lantern Fox carries with it. No item, no loot. */
    public static final DeferredBlock<SpiritLightBlock> SPIRIT_LIGHT=BLOCKS.register("spirit_light",()->new SpiritLightBlock(BlockBehaviour.Properties.of()
            .replaceable().noCollission().noOcclusion().noLootTable().air().strength(-1F,3600000F).lightLevel(s->10).pushReaction(PushReaction.DESTROY)));
    public static final DeferredHolder<MenuType<?>,MenuType<MossbackMenu>> SADDLEBAG=MENUS.register("mossback_saddlebag",()->new MenuType<>(MossbackMenu::new,FeatureFlags.DEFAULT_FLAGS));
    public static void displayItems(CreativeModeTab.Output out) { out.accept(BONDING_CHARM.get()); }
    private FamiliarRegistry(){}
}
