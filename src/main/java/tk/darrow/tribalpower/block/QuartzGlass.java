package tk.darrow.tribalpower.block;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.echo.LatticeRecipe;
import tk.darrow.tribalpower.echo.ProcessingRecipes;
import tk.darrow.tribalpower.item.ModItems;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Quartz Glass: what the Glimmer Ridge's crystal becomes in a kiln. Clear and all sixteen dyes, as
 * blocks and as panes, and every one of them again as a <b>lit</b> pane that carries its own light.
 *
 * <p>Lighting it is a station job, not a crafting one: seat glass in an <b>Echo Attune</b> with
 * <b>glowstone dust</b> in the catalyst slot and the light is bound into the crystal. That formula is
 * built here rather than written as a recipe file because catalysts are a station concept — the same
 * road {@link tk.darrow.tribalpower.item.MachineRank} takes for ranking machines.
 *
 * <p>Unlike ordinary glass it survives being broken. A ridge crystal is stubborn stuff, and a building
 * set you cannot take back down again is a poor building set.
 *
 * <p>Models, loot, recipes and tags come from tools/generate_quartz_glass.py.
 */
public final class QuartzGlass {
    /** Every glass block and pane, in creative-tab order. */
    public static final Map<String, DeferredItem<? extends Item>> ITEMS = new LinkedHashMap<>();
    /** Unlit id to its lit twin, for the Echo Attune formula. */
    public static final Map<String, String> LIT_OF = new LinkedHashMap<>();
    /** Every id this class registers, for the generator and the tests to walk. */
    public static final List<String> ALL = new java.util.ArrayList<>();

    public static final int LIGHT = 15;
    /** Seconds and Pulse a second the station spends binding light into a pane. */
    public static final int SECONDS = 6, PULSE = 24;

    static {
        pair("quartz_glass");
        pair("quartz_glass_pane");
        for (DyeColor dye : DyeColor.values()) {
            pair(dye.getSerializedName() + "_quartz_glass");
            pair(dye.getSerializedName() + "_quartz_glass_pane");
        }
    }

    private QuartzGlass() {}

    public static void init() {}

    /** Registers an unlit block and its lit twin, and remembers the link between them. */
    private static void pair(String id) {
        boolean pane = id.endsWith("_pane");
        DyeColor dye = dyeOf(id);
        register(id, pane, dye, 0);
        register("lit_" + id, pane, dye, LIGHT);
        LIT_OF.put(id, "lit_" + id);
    }

    private static DyeColor dyeOf(String id) {
        for (DyeColor dye : DyeColor.values())
            if (id.startsWith(dye.getSerializedName() + "_")) return dye;
        return null;
    }

    private static void register(String id, boolean pane, DyeColor dye, int light) {
        Supplier<Block> factory;
        if (pane) factory = dye == null ? () -> new IronBarsBlock(glass(light))
                                        : () -> new StainedGlassPaneBlock(dye, glass(light));
        else factory = dye == null ? () -> new TransparentBlock(glass(light))
                                   : () -> new StainedGlassBlock(dye, glass(light));
        DeferredBlock<Block> block = ModBlocks.BLOCKS.register(id, factory);
        ITEMS.put(id, ModItems.ITEMS.registerSimpleBlockItem(id, block));
        ALL.add(id);
    }

    private static BlockBehaviour.Properties glass(int light) {
        BlockBehaviour.Properties p = BlockBehaviour.Properties.of()
                .strength(0.4F)
                .sound(SoundType.GLASS)
                .noOcclusion()
                .isValidSpawn((s, l, pos, e) -> false)
                .isRedstoneConductor((s, l, pos) -> false)
                .isSuffocating((s, l, pos) -> false)
                .isViewBlocking((s, l, pos) -> false);
        return light > 0 ? p.lightLevel(s -> light) : p;
    }

    /**
     * Echo Attune, with glowstone dust seated as the catalyst, binds light into a pane of quartz glass.
     * Returns null for anything that is not unlit quartz glass, so the station falls through as usual.
     */
    public static ProcessingRecipes.Formula litFormula(String station, ItemStack stack) {
        if (!"echo_attune".equals(station) || stack.isEmpty()) return null;
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key == null || !TribalPower.MOD_ID.equals(key.getNamespace())) return null;
        String lit = LIT_OF.get(key.getPath());
        if (lit == null) return null;
        Item result = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, lit));
        if (result == null || result == Items.AIR) return null;
        LatticeRecipe recipe = new LatticeRecipe("echo_attune", Ingredient.of(stack.getItem()),
                new ItemStack(result), SECONDS, PULSE, Attunement.FIRE);
        return new ProcessingRecipes.Formula(
                ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "quartz_glass/lit/" + key.getPath()),
                recipe, List.of(new ItemStack(Items.GLOWSTONE_DUST)));
    }
}
