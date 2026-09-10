package tk.darrow.tribalpower.integration.jei;

import mezz.jei.api.*;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import tk.darrow.tribalpower.echo.LatticeRecipe;
import tk.darrow.tribalpower.item.ModItems;

/** Optional JEI integration; Tribal Power still runs without JEI. */
@JeiPlugin
public class TribalJeiPlugin implements IModPlugin {
    public static final RecipeType<LatticeRecipe> TYPE = RecipeType.create("tribalpower","lattice",LatticeRecipe.class);
    @Override public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath("tribalpower","jei"); }
    @Override public void registerCategories(IRecipeCategoryRegistration registration) { registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper())); }
    @Override public void registerRecipes(IRecipeRegistration registration) {
        var level=Minecraft.getInstance().level;
        if (level!=null) registration.addRecipes(TYPE,level.getRecipeManager().getAllRecipesFor(LatticeRecipe.TYPE.get()).stream().map(holder -> holder.value()).toList());
        // Shatterings the grit scan synthesised have no recipe file, so without this they are invisible
        // to players -- and a modded metal would look unsupported (design 3.1 section 1.4).
        registration.addRecipes(TYPE,tk.darrow.tribalpower.grit.GritRegistry.allFormulae().stream()
                .map(tk.darrow.tribalpower.echo.ProcessingRecipes.Formula::recipe).toList());
        registration.addIngredientInfo(ModItems.SPIRIT_STAFF.get(),Component.translatable("item.tribalpower.spirit_staff.desc",12));
        registration.addIngredientInfo(ModItems.LATTICE_TUNER.get(),Component.translatable("item.tribalpower.lattice_tuner.desc"));
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(TYPE,ModItems.ECHO_SHATTER.get(),ModItems.ECHO_ATTUNE.get(),ModItems.ECHO_BIND.get(),ModItems.ECHO_MANIFEST.get(),ModItems.ECHO_UNWEAVE.get());
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
        }
        @Override public void draw(LatticeRecipe recipe,IRecipeSlotsView slots,GuiGraphics g,double mouseX,double mouseY) {
            var font=Minecraft.getInstance().font;
            g.drawString(font,Component.translatable("block.tribalpower."+recipe.station()),3,2,0xFF526A61,false);
            g.fill(46,29,107,33,0xFF438F80);g.drawString(font,">",108,27,0xFF997445,false);
            g.drawString(font,Component.translatable("attunement.tribalpower."+recipe.attunement().getSerializedName()),3,48,0xFF526A61,false);
            g.drawString(font,Component.translatable("gui.tribalpower.recipe_cost",recipe.seconds(),recipe.seconds()*recipe.pulse()),3,63,0xFF526A61,false);
        }
    }
}
