package media.hofferbert.astralvigil.client;

import java.util.List;
import media.hofferbert.astralvigil.crafting.StarlightRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
//? if >=26.1 {
import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.common.NeoForge;
//?}

/**
 * Client-side view of Astral Vigil recipes for JEI/EMI. 26.1 clients only know recipe types the server explicitly
 * sends ({@code OnDatapackSyncEvent#sendRecipes}); they arrive through {@code RecipesReceivedEvent}.
 */
public final class ClientRecipes {
    //? if >=26.1 {
    private static RecipeMap recipes = RecipeMap.EMPTY;

    public static void register() {
        // before JEI reads them
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (RecipesReceivedEvent e) -> recipes = e.getRecipeMap());
    }
    //?} else {
    /*public static void register() {}
    *///?}

    private ClientRecipes() {}

    public static List<RecipeHolder<StarlightRecipe>> holders(RecipeType<StarlightRecipe> type) {
        //? if >=26.1 {
        return List.copyOf(recipes.byType(type));
        //?} else {
        /*var level = Minecraft.getInstance().level;
        return level == null ? List.of() : level.getRecipeManager().getAllRecipesFor(type);
        *///?}
    }

    public static List<StarlightRecipe> of(RecipeType<StarlightRecipe> type) {
        return holders(type).stream().map(RecipeHolder::value).toList();
    }

    @SuppressWarnings("unused")
    private static Minecraft mc() {
        return Minecraft.getInstance();
    }
}
