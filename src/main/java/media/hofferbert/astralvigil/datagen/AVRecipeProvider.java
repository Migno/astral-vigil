package media.hofferbert.astralvigil.datagen;

import java.util.concurrent.CompletableFuture;
import media.hofferbert.astralvigil.registry.AVItems;
import media.hofferbert.astralvigil.registry.AVTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.neoforged.neoforge.common.Tags;
//? if <26.1 {
/*import net.minecraft.data.recipes.ShapedRecipeBuilder;
*///?}

/** Recipes. Every input is a tag (c: or astralvigil:) so KubeJS/CraftTweaker can retarget them. */
public class AVRecipeProvider extends RecipeProvider {
    //? if >=26.1 {
    protected AVRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    public static DataProvider create(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup) {
        return new Runner(out, lookup) {
            @Override
            protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
                return new AVRecipeProvider(registries, output);
            }

            @Override
            public String getName() {
                return "Astral Vigil recipes";
            }
        };
    }

    @Override
    protected void buildRecipes() {
        shaped(RecipeCategory.TOOLS, AVItems.ASTROLABE.get())
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('S', AVTags.Items.ASTRAL_SHARDS)
                .define('C', Tags.Items.INGOTS_COPPER)
                .pattern(" G ")
                .pattern("GSG")
                .pattern(" C ")
                .unlockedBy("has_astral_shard", has(AVTags.Items.ASTRAL_SHARDS))
                .save(output);
    }
    //?} else {
    /*public AVRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    public static DataProvider create(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup) {
        return new AVRecipeProvider(out, lookup);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, AVItems.ASTROLABE.get())
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('S', AVTags.Items.ASTRAL_SHARDS)
                .define('C', Tags.Items.INGOTS_COPPER)
                .pattern(" G ")
                .pattern("GSG")
                .pattern(" C ")
                .unlockedBy("has_astral_shard", has(AVTags.Items.ASTRAL_SHARDS))
                .save(output);
    }
    *///?}
}
