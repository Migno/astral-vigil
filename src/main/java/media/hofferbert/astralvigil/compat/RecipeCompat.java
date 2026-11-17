package media.hofferbert.astralvigil.compat;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import media.hofferbert.astralvigil.crafting.ItemsInput;
import media.hofferbert.astralvigil.crafting.StarlightRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

/** Recipe lookups (26.1: ResourceKey ids + recipeAccess(); 1.21.1: ResourceLocation ids + getRecipeManager()). */
public final class RecipeCompat {
    private RecipeCompat() {}

    public record Found(String id, StarlightRecipe recipe) {}

    public static Optional<Found> find(ServerLevel level, RecipeType<StarlightRecipe> type, ItemsInput input) {
        //? if >=26.1 {
        return level.recipeAccess().getRecipeFor(type, input, level)
                .map(h -> new Found(h.id().identifier().toString(), h.value()));
        //?} else {
        /*return level.getRecipeManager().getRecipeFor(type, input, level)
                .map(h -> new Found(h.id().toString(), h.value()));
        *///?}
    }

    public static Optional<StarlightRecipe> byId(ServerLevel level, String id) {
        Identifier rl = Identifier.tryParse(id);
        if (rl == null) {
            return Optional.empty();
        }
        //? if >=26.1 {
        Optional<RecipeHolder<?>> holder = level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, rl));
        //?} else {
        /*Optional<RecipeHolder<?>> holder = level.getRecipeManager().byKey(rl);
        *///?}
        return holder.map(RecipeHolder::value)
                .filter(r -> r instanceof StarlightRecipe)
                .map(r -> (StarlightRecipe) r);
    }

    /** All recipes of a type known to the given collection of holders (used by JEI/EMI on the client). */
    public static List<StarlightRecipe> ofType(Collection<RecipeHolder<?>> holders, RecipeType<StarlightRecipe> type) {
        return holders.stream()
                .map(RecipeHolder::value)
                .filter(r -> r instanceof StarlightRecipe s && s.getType() == type)
                .map(r -> (StarlightRecipe) r)
                .toList();
    }
}
