package tk.darrow.tribalpower.world;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.item.ModItems;

/**
 * The vanilla ores as they sit in March slate. Hardness, tool tier, XP and drops match the vanilla ore
 * (drops through loot tables, tiers through tags), so only the rock around the mineral is different.
 */
public final class MarchOres {
    public static final Map<String, DeferredBlock<Block>> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<BlockItem>> ITEMS = new LinkedHashMap<>();

    static {
        ore("coal", Blocks.COAL_ORE, UniformInt.of(0, 2));
        ore("iron", Blocks.IRON_ORE, ConstantInt.of(0));
        ore("copper", Blocks.COPPER_ORE, ConstantInt.of(0));
        ore("gold", Blocks.GOLD_ORE, ConstantInt.of(0));
        ore("redstone", Blocks.REDSTONE_ORE, null);
        ore("lapis", Blocks.LAPIS_ORE, UniformInt.of(2, 5));
        ore("diamond", Blocks.DIAMOND_ORE, UniformInt.of(3, 7));
        ore("emerald", Blocks.EMERALD_ORE, UniformInt.of(3, 7));
    }

    private MarchOres() {}

    /** Forces the static registrations before the registries fire. */
    public static void init() {}

    private static void ore(String mineral, Block vanilla, net.minecraft.util.valueproviders.IntProvider xp) {
        String id = "march_" + mineral + "_ore";
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(vanilla).mapColor(MapColor.TERRACOTTA_CYAN);
        DeferredBlock<Block> block = ModBlocks.BLOCKS.register(id, () -> vanilla == Blocks.REDSTONE_ORE
                ? new RedStoneOreBlock(properties)
                : new DropExperienceBlock(xp, properties));
        BLOCKS.put(mineral, block);
        ITEMS.put(mineral, ModItems.ITEMS.registerSimpleBlockItem(id, block));
    }
}
