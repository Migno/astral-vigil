package media.hofferbert.astralvigil.compat.recipeviewer;

import java.util.ArrayList;
import java.util.List;
import media.hofferbert.astralvigil.crafting.StarlightRecipe;
import net.minecraft.network.chat.Component;

/** Shared layout for the JEI and EMI recipe displays (slot positions in a 150×94 panel). */
public final class RecipeLayout {
    public static final int WIDTH = 150;
    public static final int HEIGHT = 94;
    public static final int CENTER_X = 30;
    public static final int CENTER_Y = 24;
    public static final int OUTPUT_X = 120;
    public static final int OUTPUT_Y = 24;
    public static final int TEXT_Y = 64;

    private RecipeLayout() {}

    public record Pos(int x, int y) {}

    /** Ingredient slot positions: a ring around the centre (convergence) or a 2×2 grid (forge). */
    public static List<Pos> ingredientSlots(StarlightRecipe recipe) {
        List<Pos> out = new ArrayList<>();
        int n = recipe.ingredients().size();
        if (recipe.kind() == StarlightRecipe.Kind.CONVERGENCE) {
            for (int i = 0; i < n; i++) {
                double a = -Math.PI / 2 + Math.PI * 2 * i / n;
                out.add(new Pos(CENTER_X + (int) Math.round(Math.cos(a) * 24), CENTER_Y + (int) Math.round(Math.sin(a) * 19)));
            }
        } else {
            for (int i = 0; i < n; i++) {
                out.add(new Pos(20 + (i % 2) * 20, 14 + (i / 2) * 20));
            }
        }
        return out;
    }

    public static List<Component> info(StarlightRecipe recipe) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(
                "recipe.astralvigil.starlight", recipe.starlight(), Component.translatable(recipe.tint().translationKey())));
        lines.add(Component.translatable("recipe.astralvigil.time", recipe.time() / 20));
        recipe.alignment().ifPresent(a -> lines.add(
                Component.translatable("recipe.astralvigil.alignment", Component.translatable(a.translationKey()))));
        return lines;
    }
}
