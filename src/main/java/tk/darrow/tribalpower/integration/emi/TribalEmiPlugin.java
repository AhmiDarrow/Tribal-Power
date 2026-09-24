package tk.darrow.tribalpower.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.cuisine.CuisineRegistry;
import tk.darrow.tribalpower.cuisine.HearthRecipe;
import tk.darrow.tribalpower.echo.LatticeRecipe;
import tk.darrow.tribalpower.guardian.GuardianRegistry;
import tk.darrow.tribalpower.healing.HealingRegistry;
import tk.darrow.tribalpower.healing.Remedies;
import tk.darrow.tribalpower.healing.Remedy;
import tk.darrow.tribalpower.healing.SpiritKettleBlockEntity;
import tk.darrow.tribalpower.integration.CompatDisplays;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.song.Reagents;

/** EMI: the same recipe families the JEI plugin shows, in EMI's own frame. Loaded only when EMI is present. */
@EmiEntrypoint
public class TribalEmiPlugin implements EmiPlugin {
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("tribalpower", path); }
    public static final EmiRecipeCategory LATTICE = new EmiRecipeCategory(id("lattice"), EmiStack.of(ModItems.ECHO_SHATTER.get()));
    public static final EmiRecipeCategory KETTLE = new EmiRecipeCategory(id("spirit_kettle"), EmiStack.of(HealingRegistry.SPIRIT_KETTLE_ITEM.get()));
    public static final EmiRecipeCategory HEARTH = new EmiRecipeCategory(id("hearth"), EmiStack.of(CuisineRegistry.HEARTH_POT_ITEM.get()));
    public static final EmiRecipeCategory RITES = new EmiRecipeCategory(id("rites"), EmiStack.of(ModItems.LOOM_SEAL.get()));
    public static final EmiRecipeCategory ALTARS = new EmiRecipeCategory(id("guardian_altar"), EmiStack.of(GuardianRegistry.ALTAR_ITEM.get()));
    public static final EmiRecipeCategory ANOINTING = new EmiRecipeCategory(id("anointing"), EmiStack.of(ModItems.SPIRITGEAR_BLADE.get()));

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(LATTICE);
        registry.addCategory(KETTLE);
        registry.addCategory(HEARTH);
        registry.addCategory(RITES);
        registry.addCategory(ALTARS);
        registry.addCategory(ANOINTING);
        for (var station : List.of(ModItems.ECHO_SHATTER, ModItems.ECHO_ATTUNE, ModItems.ECHO_BIND, ModItems.ECHO_MANIFEST, ModItems.ECHO_UNWEAVE, ModItems.EMBER_KILN))
            registry.addWorkstation(LATTICE, EmiStack.of(station.get()));
        registry.addWorkstation(KETTLE, EmiStack.of(HealingRegistry.SPIRIT_KETTLE_ITEM.get()));
        registry.addWorkstation(HEARTH, EmiStack.of(CuisineRegistry.HEARTH_POT_ITEM.get()));
        registry.addWorkstation(RITES, EmiStack.of(ModItems.RITUAL_BRAZIER.get()));
        registry.addWorkstation(ALTARS, EmiStack.of(GuardianRegistry.ALTAR_ITEM.get()));
        registry.addWorkstation(ANOINTING, EmiStack.of(ModItems.SONG_BENCH.get()));

        var level = Minecraft.getInstance().level;
        if (level != null) {
            for (var holder : level.getRecipeManager().getAllRecipesFor(LatticeRecipe.TYPE.get())) registry.addRecipe(new Lattice(holder.id(), holder.value()));
            for (var holder : level.getRecipeManager().getAllRecipesFor(CuisineRegistry.HEARTH_TYPE.get())) registry.addRecipe(new Hearth(holder.id(), holder.value()));
        }
        int n = 0;
        for (var formula : tk.darrow.tribalpower.grit.GritRegistry.allFormulae()) registry.addRecipe(new Lattice(id("grit_" + n++), formula.recipe()));
        for (var formula : tk.darrow.tribalpower.grit.GritRegistry.allFireFormulae()) registry.addRecipe(new Lattice(id("fire_" + n++), formula.recipe()));
        for (var formula : tk.darrow.tribalpower.item.SpiritGear.allRankFormulae()) registry.addRecipe(new Lattice(id("gear_" + n++), formula.recipe()));
        for (var formula : tk.darrow.tribalpower.item.MachineRank.allRankFormulae()) registry.addRecipe(new Lattice(id("rank_" + n++), formula.recipe()));
        for (Remedy remedy : Remedy.values()) for (Remedies.Form form : Remedies.Form.values()) registry.addRecipe(new Kettle(remedy, form));
        for (CompatDisplays.Rite rite : CompatDisplays.rites()) registry.addRecipe(new Rite(rite));
        for (CompatDisplays.Call call : CompatDisplays.calls()) registry.addRecipe(new Call(call));
        for (CompatDisplays.Anoint anoint : CompatDisplays.anointments()) registry.addRecipe(new Anoint(anoint));
    }

    private static final int INK = 0xFF526A61;

    private static List<EmiStack> stacks(List<ItemStack> stacks) {
        List<EmiStack> out = new ArrayList<>();
        for (ItemStack stack : stacks) out.add(EmiStack.of(stack));
        return out;
    }

    /** A station recipe: one ingredient in, one thing out, for a voice and a cost. */
    private record Lattice(ResourceLocation id, LatticeRecipe recipe) implements EmiRecipe {
        @Override public EmiRecipeCategory getCategory() { return LATTICE; }
        @Override public ResourceLocation getId() { return id; }
        @Override public List<EmiIngredient> getInputs() { return List.of(EmiIngredient.of(recipe.ingredient())); }
        @Override public List<EmiStack> getOutputs() { return List.of(EmiStack.of(recipe.output())); }
        @Override public int getDisplayWidth() { return 160; }
        @Override public int getDisplayHeight() { return 64; }
        @Override public void addWidgets(WidgetHolder widgets) {
            widgets.addText(Component.translatable("block.tribalpower." + recipe.station()), 2, 2, INK, false);
            widgets.addSlot(EmiIngredient.of(recipe.ingredient()), 20, 20);
            widgets.addTexture(EmiTexture.EMPTY_ARROW, 60, 22);
            widgets.addSlot(EmiStack.of(recipe.output()), 120, 20).recipeContext(this);
            widgets.addText(Component.translatable("gui.tribalpower.recipe_cost", recipe.seconds(), recipe.seconds() * recipe.pulse()), 2, 48, INK, false);
        }
    }

    private record Kettle(Remedy remedy, Remedies.Form form) implements EmiRecipe {
        private List<ItemStack> reagents() {
            List<ItemStack> out = new ArrayList<>();
            for (var profile : Reagents.byNote().getOrDefault(remedy.note, List.of())) out.add(new ItemStack(Reagents.item(profile)));
            return out;
        }
        private ItemStack base() {
            return new ItemStack(switch (form) { case TINCTURE -> net.minecraft.world.item.Items.GLASS_BOTTLE; case SALVE -> net.minecraft.world.item.Items.HONEYCOMB; case INCENSE -> net.minecraft.world.item.Items.CHARCOAL; });
        }
        @Override public EmiRecipeCategory getCategory() { return KETTLE; }
        @Override public ResourceLocation getId() { return id("kettle/" + remedy.id() + "_" + form.name().toLowerCase(java.util.Locale.ROOT)); }
        @Override public List<EmiIngredient> getInputs() {
            return List.of(EmiIngredient.of(stacks(reagents())), EmiIngredient.of(Ingredient.of(SpiritKettleBlockEntity.HERBS)), EmiStack.of(base()));
        }
        @Override public List<EmiStack> getOutputs() {
            var reagents = Reagents.byNote().getOrDefault(remedy.note, List.of());
            return reagents.isEmpty() ? List.of() : List.of(EmiStack.of(Remedies.make(form, reagents.get(0), null, SpiritKettleBlockEntity.batchSize(form))));
        }
        @Override public int getDisplayWidth() { return 150; }
        @Override public int getDisplayHeight() { return 60; }
        @Override public void addWidgets(WidgetHolder widgets) {
            var inputs = getInputs();
            for (int i = 0; i < inputs.size(); i++) widgets.addSlot(inputs.get(i), 4, 2 + i * 19);
            widgets.addTexture(EmiTexture.EMPTY_ARROW, 50, 22);
            widgets.addText(Component.translatable("remedy.tribalpower." + remedy.id()), 30, 4, INK, false);
            for (EmiStack out : getOutputs()) widgets.addSlot(out, 100, 20).recipeContext(this);
            widgets.addText(Component.translatable("gui.tribalpower.recipe_cost", TribalConfig.kettleSeconds(), TribalConfig.kettlePulse()), 30, 46, INK, false);
        }
    }

    private record Hearth(ResourceLocation id, HearthRecipe recipe) implements EmiRecipe {
        @Override public EmiRecipeCategory getCategory() { return HEARTH; }
        @Override public ResourceLocation getId() { return id; }
        @Override public List<EmiIngredient> getInputs() {
            List<EmiIngredient> out = new ArrayList<>();
            for (Ingredient ingredient : recipe.ingredients()) out.add(EmiIngredient.of(ingredient));
            recipe.container().ifPresent(c -> out.add(EmiIngredient.of(c)));
            return out;
        }
        @Override public List<EmiStack> getOutputs() { return List.of(EmiStack.of(recipe.result())); }
        @Override public int getDisplayWidth() { return 150; }
        @Override public int getDisplayHeight() { return 54; }
        @Override public void addWidgets(WidgetHolder widgets) {
            for (int i = 0; i < recipe.ingredients().size(); i++) widgets.addSlot(EmiIngredient.of(recipe.ingredients().get(i)), 2 + (i % 2) * 18, 2 + (i / 2) * 18);
            recipe.container().ifPresent(c -> widgets.addSlot(EmiIngredient.of(c), 46, 20));
            widgets.addTexture(EmiTexture.EMPTY_ARROW, 72, 20);
            widgets.addSlot(EmiStack.of(recipe.result()), 118, 20).recipeContext(this);
            widgets.addText(Component.translatable("gui.tribalpower.hearth_pot.seconds", recipe.seconds()), 70, 40, INK, false);
        }
    }

    private record Rite(CompatDisplays.Rite rite) implements EmiRecipe {
        @Override public EmiRecipeCategory getCategory() { return RITES; }
        @Override public ResourceLocation getId() { return id("rite/" + rite.rite().key()); }
        @Override public List<EmiIngredient> getInputs() { return List.of(EmiStack.of(rite.tablet()), EmiStack.of(rite.seal())); }
        @Override public List<EmiStack> getOutputs() { return List.of(); }
        @Override public boolean supportsRecipeTree() { return false; }
        @Override public int getDisplayWidth() { return 160; }
        @Override public int getDisplayHeight() { return 44; }
        @Override public void addWidgets(WidgetHolder widgets) {
            widgets.addSlot(EmiStack.of(rite.tablet()), 2, 12);
            widgets.addSlot(EmiStack.of(rite.seal()), 22, 12);
            widgets.addText(Component.translatable("item.tribalpower." + rite.rite().tabletId()), 46, 4, INK, false);
            widgets.addText(Component.translatable("gui.tribalpower.rite.circle", Component.translatable("attunement.tribalpower." + rite.rite().element().getSerializedName()), rite.cost()), 46, 18, INK, false);
        }
    }

    private record Call(CompatDisplays.Call call) implements EmiRecipe {
        @Override public EmiRecipeCategory getCategory() { return ALTARS; }
        @Override public ResourceLocation getId() { return id("call/" + call.guardian().id); }
        @Override public List<EmiIngredient> getInputs() { return List.of(EmiStack.of(call.reagents())); }
        @Override public List<EmiIngredient> getCatalysts() { return List.of(EmiStack.of(call.altar())); }
        @Override public List<EmiStack> getOutputs() { return List.of(EmiStack.of(call.rises())); }
        @Override public boolean supportsRecipeTree() { return false; }
        @Override public int getDisplayWidth() { return 160; }
        @Override public int getDisplayHeight() { return 44; }
        @Override public void addWidgets(WidgetHolder widgets) {
            widgets.addSlot(EmiStack.of(call.reagents()), 2, 12);
            widgets.addSlot(EmiStack.of(call.altar()), 22, 12);
            widgets.addTexture(EmiTexture.EMPTY_ARROW, 48, 14);
            widgets.addSlot(EmiStack.of(call.rises()), 80, 12);
            widgets.addText(Component.translatable(call.guardian().nameKey()), 102, 4, INK, false);
            widgets.addText(Component.translatable("biome.tribalpower." + call.guardian().biome), 102, 18, INK, false);
        }
    }

    private record Anoint(CompatDisplays.Anoint anoint) implements EmiRecipe {
        @Override public EmiRecipeCategory getCategory() { return ANOINTING; }
        @Override public ResourceLocation getId() { return id("anoint/" + anoint.anointment().name().toLowerCase(java.util.Locale.ROOT)); }
        @Override public List<EmiIngredient> getInputs() { return List.of(EmiIngredient.of(stacks(anoint.reagents())), EmiIngredient.of(stacks(anoint.weapons()))); }
        @Override public List<EmiStack> getOutputs() { return List.of(); }
        @Override public boolean supportsRecipeTree() { return false; }
        @Override public int getDisplayWidth() { return 160; }
        @Override public int getDisplayHeight() { return 44; }
        @Override public void addWidgets(WidgetHolder widgets) {
            widgets.addSlot(EmiIngredient.of(stacks(anoint.reagents())), 2, 12);
            widgets.addSlot(EmiIngredient.of(stacks(anoint.weapons())), 22, 12);
            widgets.addText(Component.translatable(CompatDisplays.anointmentKey(anoint.anointment())), 46, 4, INK, false);
            widgets.addText(Component.translatable("gui.tribalpower.anoint.cost", TribalConfig.anointReagentCost(), TribalConfig.anointPulseCost()), 46, 18, INK, false);
        }
    }
}
