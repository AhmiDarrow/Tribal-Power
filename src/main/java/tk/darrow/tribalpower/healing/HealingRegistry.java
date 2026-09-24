package tk.darrow.tribalpower.healing;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;

/** Shamanic healing: the spirit effects, the kettle and its remedies, remnants and the sweat lodge. */
public final class HealingRegistry {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, TribalPower.MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TribalPower.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TribalPower.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TribalPower.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, TribalPower.MOD_ID);

    public static final DeferredHolder<MobEffect, SpiritEffect> SPIRIT_SICKNESS =
            EFFECTS.register("spirit_sickness", () -> new SpiritEffect(SpiritEffect.Kind.SICKNESS));
    public static final DeferredHolder<MobEffect, SpiritEffect> SPIRIT_BLESSING =
            EFFECTS.register("spirit_blessing", () -> new SpiritEffect(SpiritEffect.Kind.BLESSING));

    public static final DeferredBlock<SpiritKettleBlock> SPIRIT_KETTLE = BLOCKS.register("spirit_kettle", () -> new SpiritKettleBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5F).sound(SoundType.LANTERN).noOcclusion()));
    public static final DeferredBlock<SweatStonesBlock> SWEAT_STONES = BLOCKS.register("sweat_stones", () -> new SweatStonesBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5F).sound(SoundType.STONE).noOcclusion()
                    .lightLevel(state -> state.getValue(SweatStonesBlock.HOT) ? 6 : 0)));

    public static final DeferredItem<net.minecraft.world.item.BlockItem> SPIRIT_KETTLE_ITEM = ITEMS.registerSimpleBlockItem("spirit_kettle", SPIRIT_KETTLE);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> SWEAT_STONES_ITEM = ITEMS.registerSimpleBlockItem("sweat_stones", SWEAT_STONES);
    public static final DeferredItem<RemedyItem> SPIRIT_TINCTURE = ITEMS.register("spirit_tincture",
            () -> new RemedyItem(Remedies.Form.TINCTURE, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<RemedyItem> SPIRIT_SALVE = ITEMS.register("spirit_salve",
            () -> new RemedyItem(Remedies.Form.SALVE, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<RemedyItem> SPIRIT_INCENSE = ITEMS.register("spirit_incense",
            () -> new RemedyItem(Remedies.Form.INCENSE, new Item.Properties()));
    public static final DeferredItem<SpiritRemnantItem> SPIRIT_REMNANT = ITEMS.register("spirit_remnant",
            () -> new SpiritRemnantItem(new Item.Properties().stacksTo(1).fireResistant()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpiritKettleBlockEntity>> KETTLE_ENTITY =
            BLOCK_ENTITIES.register("spirit_kettle", () -> BlockEntityType.Builder.of(SpiritKettleBlockEntity::new, SPIRIT_KETTLE.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<KettleMenu>> KETTLE_MENU =
            MENUS.register("spirit_kettle", () -> new MenuType<>(KettleMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private HealingRegistry() {}

    public static Item remedy(Remedies.Form form) {
        return switch (form) {
            case TINCTURE -> SPIRIT_TINCTURE.get();
            case SALVE -> SPIRIT_SALVE.get();
            case INCENSE -> SPIRIT_INCENSE.get();
        };
    }

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        NeoForge.EVENT_BUS.addListener(HealingHooks::salveOthers);
        // Last, so a death another mod cancels never leaves a remnant beside a living familiar.
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, HealingHooks::remnant);
        NeoForge.EVENT_BUS.addListener(HealingHooks::wake);
        NeoForge.EVENT_BUS.addListener(HealingHooks::levelTick);
    }

    /** Plain remedies in the creative tab: one tincture, salve and incense of every remedy. */
    public static void displayItems(CreativeModeTab.Output out) {
        out.accept(SPIRIT_KETTLE_ITEM.get());
        out.accept(SWEAT_STONES_ITEM.get());
        for (Remedy remedy : Remedy.values()) {
            var reagent = tk.darrow.tribalpower.song.Reagents.byNote().get(remedy.note);
            if (reagent == null || reagent.isEmpty()) continue;
            for (Remedies.Form form : Remedies.Form.values()) out.accept(Remedies.make(form, reagent.get(0), null, 1));
        }
        out.accept(SPIRIT_REMNANT.get());
    }
}
