package tk.darrow.tribalpower.lore;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BannerPatternItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.tribe.TribeDefinition;

/** The world telling its own story: carved stones and murals for the Chronicle, and each tribe's banner pattern. */
public final class LoreRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TribalPower.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TribalPower.MOD_ID);

    public static final DeferredBlock<CarvedStoneBlock> CARVED_STONE = BLOCKS.registerBlock("carved_stone", CarvedStoneBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2F, 6F).sound(SoundType.DEEPSLATE_TILES).noOcclusion());
    public static final DeferredItem<BlockItem> CARVED_STONE_ITEM = ITEMS.registerSimpleBlockItem("carved_stone", CARVED_STONE);
    public static final DeferredBlock<MuralBlock> MURAL = BLOCKS.registerBlock("mural", MuralBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2F, 6F).sound(SoundType.DEEPSLATE_TILES).noOcclusion());
    public static final DeferredItem<BlockItem> MURAL_ITEM = ITEMS.registerSimpleBlockItem("mural", MURAL);

    /** Each tribe's crest as a banner pattern: the pattern item unlocks the tag that holds the one pattern. */
    public static final Map<TribeDefinition, DeferredItem<Item>> PATTERN_ITEMS = new EnumMap<>(TribeDefinition.class);
    public static final Map<TribeDefinition, TagKey<BannerPattern>> PATTERN_TAGS = new EnumMap<>(TribeDefinition.class);

    static {
        for (TribeDefinition tribe : TribeDefinition.values()) {
            TagKey<BannerPattern> tag = TagKey.create(Registries.BANNER_PATTERN, ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "pattern_item/" + tribe.id()));
            PATTERN_TAGS.put(tribe, tag);
            PATTERN_ITEMS.put(tribe, ITEMS.register(tribe.id() + "_banner_pattern", () -> new BannerPatternItem(tag, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON))));
        }
    }

    private LoreRegistry() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    public static void displayItems(CreativeModeTab.Output out) {
        out.accept(CARVED_STONE_ITEM.get());
        out.accept(MURAL_ITEM.get());
        PATTERN_ITEMS.values().forEach(item -> out.accept(item.get()));
    }
}
