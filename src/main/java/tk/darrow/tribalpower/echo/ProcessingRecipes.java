package tk.darrow.tribalpower.echo;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tk.darrow.tribalpower.api.pulse.Attunement;

/** Station lookup over Minecraft's synced recipe manager, including external ingredient extensions. */
public final class ProcessingRecipes {
    private ProcessingRecipes() {}
    /**
     * A station's work for one input. {@code catalysts} are extra items that must sit in the station's catalyst
     * slots for the whole job and are used up when it finishes (gear ranks use them; written recipes do not).
     */
    public record Formula(ResourceLocation id, LatticeRecipe recipe, java.util.List<ItemStack> catalysts) {
        public Formula(ResourceLocation id, LatticeRecipe recipe) { this(id, recipe, java.util.List.of()); }
        public ItemStack result() { return recipe.output().copy(); }
        public int seconds() { return recipe.seconds(); }
        public int pulse() { return recipe.pulse(); }
        public Attunement attunement() { return recipe.attunement(); }
    }

    /**
     * The station's formula for this input. A written recipe always wins; when none exists, the grit scan
     * synthesises one for any metal or gem it discovered in the common tags (design 3.1 section 1.4).
     */
    public static Formula find(Level level, String station, ItemStack stack) {
        // A spent input stays its item at count zero. Ranking that ghost would mint pieces forever.
        if (stack == null || stack.isEmpty()) return null;
        Formula written = findWritten(level, station, stack);
        if (written != null) return written;
        Formula gear = tk.darrow.tribalpower.item.SpiritGear.rankFormula(station, stack);
        if (gear != null) return gear;
        Formula machine = tk.darrow.tribalpower.item.MachineRank.rankFormula(station, stack);
        if (machine != null) return machine;
        Formula glass = tk.darrow.tribalpower.block.QuartzGlass.litFormula(station, stack);
        if (glass != null) return glass;
        if ("ember_kiln".equals(station)) {
            Formula grit = tk.darrow.tribalpower.grit.GritRegistry.fire(stack);
            if (grit != null) return grit;
            return kiln(level, stack);
        }
        return tk.darrow.tribalpower.grit.GritRegistry.formula(station, stack);
    }

    /** Every furnace smelting recipe, including grit cooking whose result is only known at assemble. */
    public static Formula kiln(Level level, ItemStack stack) {
        if (level == null || stack.isEmpty()) return null;
        var input = new net.minecraft.world.item.crafting.SingleRecipeInput(stack);
        for (var holder : index(level).smeltingFor(stack.getItem())) {
            var cooking = holder.value();
            if (!cooking.matches(input, level)) continue;
            ItemStack output = cooking.assemble(input, level.registryAccess());
            if (output.isEmpty()) output = cooking.getResultItem(level.registryAccess()).copy();
            if (output.isEmpty()) continue;
            LatticeRecipe recipe = new LatticeRecipe("ember_kiln",
                    net.minecraft.world.item.crafting.Ingredient.of(stack.getItem()), output,
                    10, 32, Attunement.FIRE);
            return new Formula(holder.id(), recipe);
        }
        return null;
    }

    /** Datapack and KubeJS recipes only -- the authored half of the lookup. */
    public static Formula findWritten(Level level, String station, ItemStack stack) {
        if (level == null || stack.isEmpty()) return null;
        for (var holder : index(level).writtenFor(station, stack.getItem()))
            if (holder.value().ingredient().test(stack)) return new Formula(holder.id(), holder.value());
        return null;
    }

    /**
     * The same answer as {@link #findWritten}, by the full sorted scan with no index. Kept for the
     * checks that prove the index never changes a result.
     */
    public static Formula findWrittenUncached(Level level, String station, ItemStack stack) {
        if (level == null || stack.isEmpty()) return null;
        return level.getRecipeManager().getAllRecipesFor(LatticeRecipe.TYPE.get()).stream()
                .filter(holder -> holder.value().station().equals(station) && holder.value().ingredient().test(stack))
                .sorted(java.util.Comparator.comparing(holder -> holder.id().toString()))
                .map(holder -> new Formula(holder.id(), holder.value())).findFirst().orElse(null);
    }

    // ---- lookup index ------------------------------------------------------------------------

    /*
     * Hoppers ask a station "would you take this?" on every insert attempt, so the lookup above runs
     * many times a second. The index answers it from lists built once per recipe set:
     *
     *   - each station's written recipes, sorted by id once instead of on every call;
     *   - per item, only the recipes that could possibly take it. An ingredient that depends on the
     *     item alone is filtered by item up front; any other kind stays in every item's list and is
     *     still tested against the real stack, so components still count.
     *
     * Every candidate is tested again against the actual stack, in the same order as before, so the
     * answer is exactly what the full scan would give. The index is thrown away whenever the recipe
     * manager's recipe set is replaced (a reload, a datapack change, a sync to the client) and on
     * every tag reload, since tags decide what an ingredient takes.
     */

    private static volatile Index serverIndex;
    private static volatile Index clientIndex;

    /** Forget both sides' indexes; the next lookup rebuilds from the live recipe manager. */
    public static void invalidate() {
        serverIndex = null;
        clientIndex = null;
    }

    public static void onTagsUpdated(net.neoforged.neoforge.event.TagsUpdatedEvent event) {
        invalidate();
    }

    /** True when the index for this level's side is built and current; for the checks only. */
    public static boolean indexed(Level level) {
        Index index = level.isClientSide ? clientIndex : serverIndex;
        var manager = level.getRecipeManager();
        return index != null && index.manager == manager && index.stamp == manager.getRecipes();
    }

    private static Index index(Level level) {
        var manager = level.getRecipeManager();
        // The recipe manager swaps its whole map on every load or replace, so the map's own value
        // view is a stamp of the recipe set that costs nothing to compare.
        Object stamp = manager.getRecipes();
        boolean client = level.isClientSide;
        Index index = client ? clientIndex : serverIndex;
        if (index != null && index.manager == manager && index.stamp == stamp) return index;
        index = new Index(manager, stamp);
        if (client) clientIndex = index; else serverIndex = index;
        return index;
    }

    private static final class Index {
        final net.minecraft.world.item.crafting.RecipeManager manager;
        final Object stamp;
        final java.util.Map<String, java.util.List<net.minecraft.world.item.crafting.RecipeHolder<LatticeRecipe>>> byStation;
        final java.util.Map<String, java.util.Map<net.minecraft.world.item.Item, java.util.List<net.minecraft.world.item.crafting.RecipeHolder<LatticeRecipe>>>> writtenMemo =
                new java.util.concurrent.ConcurrentHashMap<>();
        final java.util.List<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.SmeltingRecipe>> smelting;
        final java.util.Map<net.minecraft.world.item.Item, java.util.List<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.SmeltingRecipe>>> smeltingMemo =
                new java.util.concurrent.ConcurrentHashMap<>();

        Index(net.minecraft.world.item.crafting.RecipeManager manager, Object stamp) {
            this.manager = manager;
            this.stamp = stamp;
            var sorted = new java.util.ArrayList<>(manager.getAllRecipesFor(LatticeRecipe.TYPE.get()));
            sorted.sort(java.util.Comparator.comparing(holder -> holder.id().toString()));
            var stations = new java.util.HashMap<String, java.util.List<net.minecraft.world.item.crafting.RecipeHolder<LatticeRecipe>>>();
            for (var holder : sorted)
                stations.computeIfAbsent(holder.value().station(), key -> new java.util.ArrayList<>()).add(holder);
            this.byStation = stations;
            this.smelting = manager.getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.SMELTING);
        }

        java.util.List<net.minecraft.world.item.crafting.RecipeHolder<LatticeRecipe>> writtenFor(String station, net.minecraft.world.item.Item item) {
            var all = byStation.get(station);
            if (all == null) return java.util.List.of();
            return writtenMemo.computeIfAbsent(station, key -> new java.util.concurrent.ConcurrentHashMap<>())
                    .computeIfAbsent(item, key -> {
                        ItemStack probe = new ItemStack(key);
                        var out = new java.util.ArrayList<net.minecraft.world.item.crafting.RecipeHolder<LatticeRecipe>>();
                        for (var holder : all) {
                            var ingredient = holder.value().ingredient();
                            if (!ingredient.isSimple() || ingredient.test(probe)) out.add(holder);
                        }
                        return java.util.List.copyOf(out);
                    });
        }

        java.util.List<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.SmeltingRecipe>> smeltingFor(net.minecraft.world.item.Item item) {
            return smeltingMemo.computeIfAbsent(item, key -> {
                ItemStack probe = new ItemStack(key);
                var out = new java.util.ArrayList<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.SmeltingRecipe>>();
                for (var holder : smelting) {
                    var recipe = holder.value();
                    // Only the plain furnace recipe is known to match on its one ingredient; any
                    // other kind keeps its own rules and is always asked.
                    boolean plain = recipe.getClass() == net.minecraft.world.item.crafting.SmeltingRecipe.class
                            && recipe.getIngredients().size() == 1;
                    if (!plain) { out.add(holder); continue; }
                    var ingredient = recipe.getIngredients().get(0);
                    if (!ingredient.isSimple() || ingredient.test(probe)) out.add(holder);
                }
                return java.util.List.copyOf(out);
            });
        }
    }
}
