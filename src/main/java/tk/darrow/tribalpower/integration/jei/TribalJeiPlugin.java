package tk.darrow.tribalpower.integration.jei;

import mezz.jei.api.*;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.gui.handlers.IScreenHandler;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IClickableIngredient;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.bench.BenchMenu;
import tk.darrow.tribalpower.bench.BenchRegistry;
import tk.darrow.tribalpower.client.BenchScreen;
import tk.darrow.tribalpower.client.SpiritCodexScreen;
import tk.darrow.tribalpower.echo.LatticeRecipe;
import tk.darrow.tribalpower.item.ModItems;

import java.util.Optional;

/** Optional JEI integration; Tribal Power still runs without JEI. */
@JeiPlugin
public class TribalJeiPlugin implements IModPlugin {
    public static final RecipeType<LatticeRecipe> TYPE = RecipeType.create("tribalpower","lattice",LatticeRecipe.class);
    /** One kettle brew: a remedy in one form. */
    public record KettleBrew(tk.darrow.tribalpower.healing.Remedy remedy, tk.darrow.tribalpower.healing.Remedies.Form form) {}
    public static final RecipeType<KettleBrew> KETTLE = RecipeType.create("tribalpower","spirit_kettle",KettleBrew.class);
    public static final RecipeType<tk.darrow.tribalpower.cuisine.HearthRecipe> HEARTH = RecipeType.create("tribalpower","hearth",tk.darrow.tribalpower.cuisine.HearthRecipe.class);
    public static final RecipeType<tk.darrow.tribalpower.integration.CompatDisplays.Rite> RITE = RecipeType.create("tribalpower","rite",tk.darrow.tribalpower.integration.CompatDisplays.Rite.class);
    public static final RecipeType<tk.darrow.tribalpower.integration.CompatDisplays.Call> CALL = RecipeType.create("tribalpower","guardian_call",tk.darrow.tribalpower.integration.CompatDisplays.Call.class);
    public static final RecipeType<tk.darrow.tribalpower.integration.CompatDisplays.Anoint> ANOINT = RecipeType.create("tribalpower","anointing",tk.darrow.tribalpower.integration.CompatDisplays.Anoint.class);
    private static IJeiRuntime runtime;
    @Override public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath("tribalpower","jei"); }
    @Override public void onRuntimeAvailable(IJeiRuntime value) { runtime = value; }
    @Override public void onRuntimeUnavailable() { runtime = null; }
    static boolean ready() { return runtime != null; }
    static boolean show(ItemStack stack, boolean uses) {
        if (runtime == null || stack == null || stack.isEmpty()) return false;
        try {
            var role = uses ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT;
            var focus = runtime.getJeiHelpers().getFocusFactory().createFocus(role, VanillaTypes.ITEM_STACK, stack);
            runtime.getRecipesGui().show(focus);
            return true;
        } catch (RuntimeException e) {
            TribalPower.LOGGER.warn("Could not open JEI from the Spirit Codex", e);
            return false;
        }
    }
    /** The bench's grid is a vanilla-shaped crafting grid: result 0, grid 1-9, then the 36 player slots. */
    @Override public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(BenchMenu.class, BenchRegistry.MENU.get(), RecipeTypes.CRAFTING,
                BenchMenu.GRID_START, BenchMenu.GRID_END - BenchMenu.GRID_START, BenchMenu.GRID_END, 36);
    }
    @Override public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(BenchScreen.class, 97, 35, 18, 12, RecipeTypes.CRAFTING);
        registration.addGuiScreenHandler(SpiritCodexScreen.class, new IScreenHandler<>() {
            @Override public IGuiProperties apply(SpiritCodexScreen screen) {
                return new BookGui(SpiritCodexScreen.class, screen.bookLeft(), screen.bookTop(),
                        screen.bookWidth(), screen.bookHeight(), screen.width, screen.height);
            }
            @Override public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(
                    mezz.jei.api.gui.builder.IClickableIngredientFactory factory, SpiritCodexScreen screen, double x, double y) {
                var hover = screen.itemHover(x, y);
                if (hover == null || hover.item().isEmpty()) return Optional.empty();
                return factory.createBuilder(hover.item()).buildWithArea(hover.x(), hover.y(), 16, 16);
            }
        });
    }
    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new KettleCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new HearthCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new RiteCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new CallCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new AnointCategory(registration.getJeiHelpers().getGuiHelper()));
    }
    @Override public void registerRecipes(IRecipeRegistration registration) {
        var level=Minecraft.getInstance().level;
        if (level!=null) registration.addRecipes(TYPE,level.getRecipeManager().getAllRecipesFor(LatticeRecipe.TYPE.get()).stream().map(holder -> holder.value()).toList());
        // Shatterings the grit scan synthesised have no recipe file, so without this they are invisible
        // to players -- and a modded metal would look unsupported (design 3.1 section 1.4).
        registration.addRecipes(TYPE,tk.darrow.tribalpower.grit.GritRegistry.allFormulae().stream()
                .map(tk.darrow.tribalpower.echo.ProcessingRecipes.Formula::recipe).toList());
        // Kiln firings the grit scan synthesised have no lattice file either.
        registration.addRecipes(TYPE,tk.darrow.tribalpower.grit.GritRegistry.allFireFormulae().stream()
                .map(tk.darrow.tribalpower.echo.ProcessingRecipes.Formula::recipe).toList());
        registration.addRecipes(TYPE,tk.darrow.tribalpower.item.SpiritGear.allRankFormulae().stream()
                .map(tk.darrow.tribalpower.echo.ProcessingRecipes.Formula::recipe).toList());
        registration.addRecipes(TYPE,tk.darrow.tribalpower.item.MachineRank.allRankFormulae().stream()
                .map(tk.darrow.tribalpower.echo.ProcessingRecipes.Formula::recipe).toList());
        var brews=new java.util.ArrayList<KettleBrew>();
        for(var remedy:tk.darrow.tribalpower.healing.Remedy.values())
            for(var form:tk.darrow.tribalpower.healing.Remedies.Form.values()) brews.add(new KettleBrew(remedy,form));
        registration.addRecipes(KETTLE,brews);
        if (level!=null) registration.addRecipes(HEARTH,level.getRecipeManager().getAllRecipesFor(tk.darrow.tribalpower.cuisine.CuisineRegistry.HEARTH_TYPE.get()).stream().map(h -> h.value()).toList());
        registration.addRecipes(RITE,tk.darrow.tribalpower.integration.CompatDisplays.rites());
        registration.addRecipes(CALL,tk.darrow.tribalpower.integration.CompatDisplays.calls());
        registration.addRecipes(ANOINT,tk.darrow.tribalpower.integration.CompatDisplays.anointments());
        for (var relic : tk.darrow.tribalpower.quest.QuestRegistry.RELICS.values()) registration.addIngredientInfo(relic.get(),Component.translatable("jei.tribalpower.relic"));
        for (var pattern : tk.darrow.tribalpower.lore.LoreRegistry.PATTERN_ITEMS.values()) registration.addIngredientInfo(pattern.get(),Component.translatable("jei.tribalpower.pattern"));
        registration.addIngredientInfo(tk.darrow.tribalpower.lore.LoreRegistry.CARVED_STONE_ITEM.get(),Component.translatable("jei.tribalpower.carving"));
        registration.addIngredientInfo(tk.darrow.tribalpower.lore.LoreRegistry.MURAL_ITEM.get(),Component.translatable("jei.tribalpower.carving"));
        registration.addIngredientInfo(tk.darrow.tribalpower.healing.HealingRegistry.SWEAT_STONES_ITEM.get(),Component.translatable("jei.tribalpower.sweat_lodge"));
        registration.addIngredientInfo(tk.darrow.tribalpower.healing.HealingRegistry.SPIRIT_REMNANT.get(),Component.translatable("jei.tribalpower.spirit_remnant"));
        registration.addIngredientInfo(tk.darrow.tribalpower.healing.HealingRegistry.SPIRIT_SALVE.get(),Component.translatable("jei.tribalpower.spirit_sickness"));
        var charmHint=Component.translatable("item.tribalpower.spirit_charm.hint");
        registration.addIngredientInfo(ModItems.SKY_CHARM.get(),charmHint);
        registration.addIngredientInfo(ModItems.EMBER_CHARM.get(),charmHint);
        registration.addIngredientInfo(ModItems.TIDE_CHARM.get(),charmHint);
        registration.addIngredientInfo(ModItems.ROOT_CHARM.get(),charmHint);
        registration.addIngredientInfo(ModItems.LANTERN_CHARM.get(),charmHint);
        registration.addIngredientInfo(ModItems.SPINDLE_CHARM.get(),charmHint);
        registration.addIngredientInfo(ModItems.WARD_CHARM.get(),charmHint);
        registration.addIngredientInfo(ModItems.HEARTH_CHARM.get(),charmHint);
        registration.addIngredientInfo(ModItems.VEIL_CHARM.get(),charmHint);
        registration.addIngredientInfo(ModItems.CHORUS_CHARM.get(),charmHint);
        registration.addIngredientInfo(ModItems.SPIRIT_STAFF.get(),Component.translatable("item.tribalpower.spirit_staff.jei"));
        registration.addIngredientInfo(ModItems.LATTICE_TUNER.get(),Component.translatable("item.tribalpower.lattice_tuner.desc"));
        registration.addIngredientInfo(ModItems.SPIRITGEAR_PICKAXE.get(),Component.translatable("item.tribalpower.spiritgear.hint"));
        registration.addIngredientInfo(ModItems.SPIRITWEAVE_HOOD.get(),Component.translatable("item.tribalpower.spiritweave_armor.hint"));
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(tk.darrow.tribalpower.healing.HealingRegistry.SPIRIT_KETTLE_ITEM.get()),KETTLE);
        registration.addRecipeCatalyst(new ItemStack(tk.darrow.tribalpower.cuisine.CuisineRegistry.HEARTH_POT_ITEM.get()),HEARTH);
        registration.addRecipeCatalyst(new ItemStack(ModItems.RITUAL_BRAZIER.get()),RITE);
        registration.addRecipeCatalyst(new ItemStack(tk.darrow.tribalpower.guardian.GuardianRegistry.ALTAR_ITEM.get()),CALL);
        registration.addRecipeCatalyst(new ItemStack(ModItems.SONG_BENCH.get()),ANOINT);
        registration.addRecipeCatalysts(TYPE,ModItems.ECHO_SHATTER.get(),ModItems.ECHO_ATTUNE.get(),ModItems.ECHO_BIND.get(),ModItems.ECHO_MANIFEST.get(),ModItems.ECHO_UNWEAVE.get(),ModItems.EMBER_KILN.get());
        for (var bench : tk.darrow.tribalpower.block.ModBlocks.allBenches())
            registration.addRecipeCatalyst(new ItemStack(bench), RecipeTypes.CRAFTING);
    }
    private static class Category implements IRecipeCategory<LatticeRecipe> {
        private final IDrawable icon;
        Category(IGuiHelper gui) { icon=gui.createDrawableItemLike(ModItems.ECHO_SHATTER.get()); }
        @Override public RecipeType<LatticeRecipe> getRecipeType() { return TYPE; }
        @Override public Component getTitle() { return Component.translatable("gui.tribalpower.lattice_recipes"); }
        @Override public IDrawable getIcon() { return icon; }
        @Override public int getWidth() { return 160; }
        @Override public int getHeight() { return 76; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder,LatticeRecipe recipe,IFocusGroup focus) {
            builder.addInputSlot(20,23).addIngredients(recipe.ingredient());
            builder.addOutputSlot(121,23).addItemStack(recipe.output());
            // Gear ranks also consume catalysts from the station's catalyst slots.
            var output = recipe.output();
            if (tk.darrow.tribalpower.item.SpiritGear.isGear(output)) {
                var catalysts = tk.darrow.tribalpower.item.SpiritGear.catalysts(tk.darrow.tribalpower.item.SpiritGear.rank(output));
                for (int i = 0; i < catalysts.size(); i++) builder.addInputSlot(62 + i * 18, 44).addItemStack(catalysts.get(i));
            }
        }
        @Override public void draw(LatticeRecipe recipe,IRecipeSlotsView slots,GuiGraphics g,double mouseX,double mouseY) {
            var font=Minecraft.getInstance().font;
            g.drawString(font,Component.translatable("block.tribalpower."+recipe.station()),3,2,0xFF526A61,false);
            g.fill(46,29,107,33,0xFF438F80);g.drawString(font,">",108,27,0xFF997445,false);
            g.drawString(font,Component.translatable("attunement.tribalpower."+recipe.attunement().getSerializedName()),3,48,0xFF526A61,false);
            g.drawString(font,Component.translatable("gui.tribalpower.recipe_cost",recipe.seconds(),recipe.seconds()*recipe.pulse()),3,63,0xFF526A61,false);
        }
    }
    /** Reagents of the remedy's Note, a herb and the form's base, brewed into the remedy. */
    private static class KettleCategory implements IRecipeCategory<KettleBrew> {
        private final IDrawable icon;
        KettleCategory(IGuiHelper gui) { icon=gui.createDrawableItemLike(tk.darrow.tribalpower.healing.HealingRegistry.SPIRIT_KETTLE_ITEM.get()); }
        @Override public RecipeType<KettleBrew> getRecipeType() { return KETTLE; }
        @Override public Component getTitle() { return Component.translatable("gui.tribalpower.kettle_recipes"); }
        @Override public IDrawable getIcon() { return icon; }
        @Override public int getWidth() { return 150; }
        @Override public int getHeight() { return 60; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder,KettleBrew brew,IFocusGroup focus) {
            var reagents=tk.darrow.tribalpower.song.Reagents.byNote().getOrDefault(brew.remedy().note,java.util.List.of());
            builder.addInputSlot(4,4).addItemStacks(reagents.stream().map(p->new ItemStack(tk.darrow.tribalpower.song.Reagents.item(p))).toList());
            builder.addInputSlot(4,22).addIngredients(net.minecraft.world.item.crafting.Ingredient.of(tk.darrow.tribalpower.healing.SpiritKettleBlockEntity.HERBS));
            var base=switch(brew.form()){ case TINCTURE->net.minecraft.world.item.Items.GLASS_BOTTLE; case SALVE->net.minecraft.world.item.Items.HONEYCOMB; case INCENSE->net.minecraft.world.item.Items.CHARCOAL; };
            builder.addInputSlot(4,40).addItemStack(new ItemStack(base));
            if(!reagents.isEmpty())builder.addOutputSlot(90,22).addItemStack(tk.darrow.tribalpower.healing.Remedies.make(brew.form(),reagents.get(0),null,
                    tk.darrow.tribalpower.healing.SpiritKettleBlockEntity.batchSize(brew.form())));
        }
        @Override public void draw(KettleBrew brew,IRecipeSlotsView slots,GuiGraphics g,double mouseX,double mouseY) {
            var font=Minecraft.getInstance().font;
            g.fill(28,28,84,32,0xFF438F80);g.drawString(font,">",85,26,0xFF997445,false);
            g.drawString(font,Component.translatable("remedy.tribalpower."+brew.remedy().id()),28,6,0xFF526A61,false);
            g.drawString(font,Component.translatable("gui.tribalpower.recipe_cost",tk.darrow.tribalpower.config.TribalConfig.kettleSeconds(),
                    tk.darrow.tribalpower.config.TribalConfig.kettlePulse()),28,46,0xFF526A61,false);
        }
    }
    /** Up to four ingredients and a bowl, simmered into a meal. */
    private static class HearthCategory implements IRecipeCategory<tk.darrow.tribalpower.cuisine.HearthRecipe> {
        private final IDrawable icon;
        HearthCategory(IGuiHelper gui) { icon=gui.createDrawableItemLike(tk.darrow.tribalpower.cuisine.CuisineRegistry.HEARTH_POT_ITEM.get()); }
        @Override public RecipeType<tk.darrow.tribalpower.cuisine.HearthRecipe> getRecipeType() { return HEARTH; }
        @Override public Component getTitle() { return Component.translatable("block.tribalpower.hearth_pot"); }
        @Override public IDrawable getIcon() { return icon; }
        @Override public int getWidth() { return 150; }
        @Override public int getHeight() { return 54; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder,tk.darrow.tribalpower.cuisine.HearthRecipe recipe,IFocusGroup focus) {
            for (int i=0;i<recipe.ingredients().size();i++) builder.addInputSlot(4+(i%2)*18,4+(i/2)*18).addIngredients(recipe.ingredients().get(i));
            recipe.container().ifPresent(c -> builder.addInputSlot(48,22).addIngredients(c));
            builder.addOutputSlot(120,22).addItemStack(recipe.result());
        }
        @Override public void draw(tk.darrow.tribalpower.cuisine.HearthRecipe recipe,IRecipeSlotsView slots,GuiGraphics g,double mouseX,double mouseY) {
            var font=Minecraft.getInstance().font;
            g.fill(72,28,112,32,0xFF438F80);g.drawString(font,">",113,26,0xFF997445,false);
            g.drawString(font,Component.translatable("gui.tribalpower.hearth_pot.seconds",recipe.seconds()),70,40,0xFF526A61,false);
        }
    }
    /** A world rite: its tablet and the seal its circle wants, and the Pulse it draws. */
    private static class RiteCategory implements IRecipeCategory<tk.darrow.tribalpower.integration.CompatDisplays.Rite> {
        private final IDrawable icon;
        RiteCategory(IGuiHelper gui) { icon=gui.createDrawableItemLike(ModItems.LOOM_SEAL.get()); }
        @Override public RecipeType<tk.darrow.tribalpower.integration.CompatDisplays.Rite> getRecipeType() { return RITE; }
        @Override public Component getTitle() { return Component.translatable("gui.tribalpower.rite_recipes"); }
        @Override public IDrawable getIcon() { return icon; }
        @Override public int getWidth() { return 160; }
        @Override public int getHeight() { return 44; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder,tk.darrow.tribalpower.integration.CompatDisplays.Rite rite,IFocusGroup focus) {
            builder.addInputSlot(4,14).addItemStack(rite.tablet());
            builder.addInputSlot(24,14).addItemStack(rite.seal());
        }
        @Override public void draw(tk.darrow.tribalpower.integration.CompatDisplays.Rite rite,IRecipeSlotsView slots,GuiGraphics g,double mouseX,double mouseY) {
            var font=Minecraft.getInstance().font;
            g.drawString(font,Component.translatable("item.tribalpower."+rite.rite().tabletId()),48,6,0xFF526A61,false);
            g.drawString(font,Component.translatable("gui.tribalpower.rite.circle",Component.translatable("attunement.tribalpower."+rite.rite().element().getSerializedName()),rite.cost()),48,22,0xFF526A61,false);
        }
    }
    /** A guardian's call: the reagents laid on its altar, and what rises. */
    private static class CallCategory implements IRecipeCategory<tk.darrow.tribalpower.integration.CompatDisplays.Call> {
        private final IDrawable icon;
        CallCategory(IGuiHelper gui) { icon=gui.createDrawableItemLike(tk.darrow.tribalpower.guardian.GuardianRegistry.ALTAR_ITEM.get()); }
        @Override public RecipeType<tk.darrow.tribalpower.integration.CompatDisplays.Call> getRecipeType() { return CALL; }
        @Override public Component getTitle() { return Component.translatable("gui.tribalpower.altar_recipes"); }
        @Override public IDrawable getIcon() { return icon; }
        @Override public int getWidth() { return 160; }
        @Override public int getHeight() { return 44; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder,tk.darrow.tribalpower.integration.CompatDisplays.Call call,IFocusGroup focus) {
            builder.addInputSlot(4,14).addItemStack(call.reagents());
            builder.addInputSlot(24,14).addItemStack(call.altar());
            builder.addOutputSlot(82,14).addItemStack(call.rises());
        }
        @Override public void draw(tk.darrow.tribalpower.integration.CompatDisplays.Call call,IRecipeSlotsView slots,GuiGraphics g,double mouseX,double mouseY) {
            var font=Minecraft.getInstance().font;
            g.fill(48,20,76,24,0xFF438F80);g.drawString(font,">",77,18,0xFF997445,false);
            g.drawString(font,Component.translatable(call.guardian().nameKey()),104,6,0xFF526A61,false);
            g.drawString(font,Component.translatable("biome.tribalpower."+call.guardian().biome),104,22,0xFF526A61,false);
        }
    }
    /** An anointment: reagents of one Note worked into a weapon at the Song Bench. */
    private static class AnointCategory implements IRecipeCategory<tk.darrow.tribalpower.integration.CompatDisplays.Anoint> {
        private final IDrawable icon;
        AnointCategory(IGuiHelper gui) { icon=gui.createDrawableItemLike(ModItems.SPIRITGEAR_BLADE.get()); }
        @Override public RecipeType<tk.darrow.tribalpower.integration.CompatDisplays.Anoint> getRecipeType() { return ANOINT; }
        @Override public Component getTitle() { return Component.translatable("gui.tribalpower.anoint_recipes"); }
        @Override public IDrawable getIcon() { return icon; }
        @Override public int getWidth() { return 160; }
        @Override public int getHeight() { return 44; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder,tk.darrow.tribalpower.integration.CompatDisplays.Anoint anoint,IFocusGroup focus) {
            builder.addInputSlot(4,14).addItemStacks(anoint.reagents());
            builder.addInputSlot(24,14).addItemStacks(anoint.weapons());
        }
        @Override public void draw(tk.darrow.tribalpower.integration.CompatDisplays.Anoint anoint,IRecipeSlotsView slots,GuiGraphics g,double mouseX,double mouseY) {
            var font=Minecraft.getInstance().font;
            g.drawString(font,Component.translatable(tk.darrow.tribalpower.integration.CompatDisplays.anointmentKey(anoint.anointment())),48,6,0xFF526A61,false);
            g.drawString(font,Component.translatable("gui.tribalpower.anoint.cost",tk.darrow.tribalpower.config.TribalConfig.anointReagentCost(),tk.darrow.tribalpower.config.TribalConfig.anointPulseCost()),48,22,0xFF526A61,false);
        }
    }
    private record BookGui(Class<? extends Screen> screenClass, int guiLeft, int guiTop, int guiXSize, int guiYSize,
                           int screenWidth, int screenHeight) implements IGuiProperties {}
}
