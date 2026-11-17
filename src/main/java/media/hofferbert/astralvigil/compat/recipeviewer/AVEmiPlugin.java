package media.hofferbert.astralvigil.compat.recipeviewer;

//? if <26.1 {
/*import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.ArrayList;
import java.util.List;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.crafting.StarlightRecipe;
import media.hofferbert.astralvigil.registry.AVItems;
import media.hofferbert.astralvigil.registry.AVRecipes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

// EMI integration (optional, compile-only; EMI has no 26.1 build yet, so this class only exists on 1.21.1).
@EmiEntrypoint
public class AVEmiPlugin implements EmiPlugin {
    static final EmiRecipeCategory CONVERGENCE = new EmiRecipeCategory(
            AstralVigil.id("convergence"), EmiStack.of(AVItems.CONVERGENCE_ALTAR.get()));
    static final EmiRecipeCategory FORGE = new EmiRecipeCategory(
            AstralVigil.id("astral_forge"), EmiStack.of(AVItems.ASTRAL_FORGE.get()));

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(CONVERGENCE);
        registry.addCategory(FORGE);
        registry.addWorkstation(CONVERGENCE, EmiStack.of(AVItems.CONVERGENCE_ALTAR.get()));
        registry.addWorkstation(CONVERGENCE, EmiStack.of(AVItems.PEDESTAL.get()));
        registry.addWorkstation(FORGE, EmiStack.of(AVItems.ASTRAL_FORGE.get()));
        for (RecipeHolder<StarlightRecipe> h : registry.getRecipeManager().getAllRecipesFor(AVRecipes.CONVERGENCE.get())) {
            registry.addRecipe(new Display(CONVERGENCE, h.id(), h.value()));
        }
        for (RecipeHolder<StarlightRecipe> h : registry.getRecipeManager().getAllRecipesFor(AVRecipes.FORGE.get())) {
            registry.addRecipe(new Display(FORGE, h.id(), h.value()));
        }
    }

    record Display(EmiRecipeCategory category, ResourceLocation id, StarlightRecipe recipe) implements EmiRecipe {
        @Override
        public EmiRecipeCategory getCategory() {
            return category;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public List<EmiIngredient> getInputs() {
            List<EmiIngredient> in = new ArrayList<>();
            recipe.center().ifPresent(c -> in.add(EmiIngredient.of(c)));
            recipe.ingredients().forEach(i -> in.add(EmiIngredient.of(i)));
            return in;
        }

        @Override
        public List<EmiStack> getOutputs() {
            return List.of(EmiStack.of(recipe.resultCopy()));
        }

        @Override
        public int getDisplayWidth() {
            return RecipeLayout.WIDTH;
        }

        @Override
        public int getDisplayHeight() {
            return RecipeLayout.HEIGHT;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            recipe.center().ifPresent(c -> widgets.addSlot(EmiIngredient.of(c), RecipeLayout.CENTER_X - 1, RecipeLayout.CENTER_Y - 1));
            List<RecipeLayout.Pos> slots = RecipeLayout.ingredientSlots(recipe);
            for (int i = 0; i < slots.size(); i++) {
                widgets.addSlot(EmiIngredient.of(recipe.ingredients().get(i)), slots.get(i).x() - 1, slots.get(i).y() - 1);
            }
            widgets.addSlot(EmiStack.of(recipe.resultCopy()), RecipeLayout.OUTPUT_X - 5, RecipeLayout.OUTPUT_Y - 5)
                    .large(true)
                    .recipeContext(this);
            int y = RecipeLayout.TEXT_Y;
            for (Component line : RecipeLayout.info(recipe)) {
                widgets.addText(line, 0, y, 0xFF555566, false);
                y += 9;
            }
        }
    }
}
*///?}
