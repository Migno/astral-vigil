package media.hofferbert.astralvigil.compat.recipeviewer;

import java.util.List;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.client.ClientRecipes;
import media.hofferbert.astralvigil.crafting.StarlightRecipe;
import media.hofferbert.astralvigil.registry.AVItems;
import media.hofferbert.astralvigil.registry.AVRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ItemLike;
//? if >=26.1 {
import mezz.jei.api.recipe.types.IRecipeType;
//?} else {
/*import mezz.jei.api.recipe.RecipeType;
*///?}

/** JEI integration (optional, compile-only). Shows Convergence Altar and Astral Forge recipes. */
@JeiPlugin
public class AVJeiPlugin implements IModPlugin {
    //? if >=26.1 {
    static final IRecipeType<StarlightRecipe> CONVERGENCE = IRecipeType.create(AstralVigil.MOD_ID, "convergence", StarlightRecipe.class);
    static final IRecipeType<StarlightRecipe> FORGE = IRecipeType.create(AstralVigil.MOD_ID, "astral_forge", StarlightRecipe.class);
    //?} else {
    /*static final RecipeType<StarlightRecipe> CONVERGENCE = RecipeType.create(AstralVigil.MOD_ID, "convergence", StarlightRecipe.class);
    static final RecipeType<StarlightRecipe> FORGE = RecipeType.create(AstralVigil.MOD_ID, "astral_forge", StarlightRecipe.class);
    *///?}

    @Override
    public Identifier getPluginUid() {
        return AstralVigil.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new Category(CONVERGENCE, "recipe.astralvigil.convergence", gui.createDrawableItemLike(AVItems.CONVERGENCE_ALTAR.get())),
                new Category(FORGE, "recipe.astralvigil.astral_forge", gui.createDrawableItemLike(AVItems.ASTRAL_FORGE.get())));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(CONVERGENCE, ClientRecipes.of(AVRecipes.CONVERGENCE.get()));
        registration.addRecipes(FORGE, ClientRecipes.of(AVRecipes.FORGE.get()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        //? if >=26.1 {
        registration.addCraftingStation(CONVERGENCE, AVItems.CONVERGENCE_ALTAR.get(), AVItems.PEDESTAL.get());
        registration.addCraftingStation(FORGE, AVItems.ASTRAL_FORGE.get());
        //?} else {
        /*registration.addRecipeCatalysts(CONVERGENCE, new ItemLike[] {AVItems.CONVERGENCE_ALTAR.get(), AVItems.PEDESTAL.get()});
        registration.addRecipeCatalysts(FORGE, new ItemLike[] {AVItems.ASTRAL_FORGE.get()});
        *///?}
    }

    static final class Category implements IRecipeCategory<StarlightRecipe> {
        //? if >=26.1 {
        private final IRecipeType<StarlightRecipe> type;
        //?} else {
        /*private final RecipeType<StarlightRecipe> type;
        *///?}
        private final String title;
        private final IDrawable icon;

        //? if >=26.1 {
        Category(IRecipeType<StarlightRecipe> type, String title, IDrawable icon) {
        //?} else {
        /*Category(RecipeType<StarlightRecipe> type, String title, IDrawable icon) {
        *///?}
            this.type = type;
            this.title = title;
            this.icon = icon;
        }

        //? if >=26.1 {
        @Override
        public IRecipeType<StarlightRecipe> getRecipeType() {
            return type;
        }
        //?} else {
        /*@Override
        public RecipeType<StarlightRecipe> getRecipeType() {
            return type;
        }
        *///?}

        @Override
        public Component getTitle() {
            return Component.translatable(title);
        }

        @Override
        public int getWidth() {
            return RecipeLayout.WIDTH;
        }

        @Override
        public int getHeight() {
            return RecipeLayout.HEIGHT;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, StarlightRecipe recipe, IFocusGroup focuses) {
            recipe.center().ifPresent(c -> {
                //? if >=26.1 {
                builder.addInputSlot(RecipeLayout.CENTER_X, RecipeLayout.CENTER_Y).setStandardSlotBackground().add(c.display());
                //?} else {
                /*builder.addInputSlot(RecipeLayout.CENTER_X, RecipeLayout.CENTER_Y).setStandardSlotBackground().addIngredients(c);
                *///?}
            });
            List<RecipeLayout.Pos> slots = RecipeLayout.ingredientSlots(recipe);
            for (int i = 0; i < slots.size(); i++) {
                var slot = builder.addInputSlot(slots.get(i).x(), slots.get(i).y()).setStandardSlotBackground();
                //? if >=26.1 {
                slot.add(recipe.ingredients().get(i).display());
                //?} else {
                /*slot.addIngredients(recipe.ingredients().get(i));
                *///?}
            }
            builder.addOutputSlot(RecipeLayout.OUTPUT_X, RecipeLayout.OUTPUT_Y)
                    .setOutputSlotBackground()
                    .addItemStack(recipe.resultCopy());
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, StarlightRecipe recipe, IFocusGroup focuses) {
            List<FormattedText> lines = List.copyOf(RecipeLayout.info(recipe));
            builder.addText(lines, RecipeLayout.WIDTH, RecipeLayout.HEIGHT - RecipeLayout.TEXT_Y)
                    .setPosition(0, RecipeLayout.TEXT_Y)
                    .setColor(0xFF555566)
                    .setShadow(false);
        }
    }
}
