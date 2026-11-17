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
import java.util.List;
import java.util.Optional;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.crafting.StarlightRecipe;
import media.hofferbert.astralvigil.gear.Sigil;
import net.minecraft.world.item.Items;
import media.hofferbert.astralvigil.research.Alignment;
import media.hofferbert.astralvigil.starlight.Tint;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
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
        shaped(RecipeCategory.MISC, AVItems.STAR_ANCHOR.get(), 2)
                .define('S', AVTags.Items.ASTRAL_SHARDS)
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('M', AVTags.Items.METEORIC_STONES)
                .pattern(" S ")
                .pattern(" G ")
                .pattern("MMM")
                .unlockedBy("has_astral_shard", has(AVTags.Items.ASTRAL_SHARDS))
                .save(output);
        shaped(RecipeCategory.MISC, AVItems.PEDESTAL.get(), 2)
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('M', AVTags.Items.METEORIC_STONES)
                .pattern(" G ")
                .pattern(" M ")
                .pattern("MMM")
                .unlockedBy("has_meteoric_stone", has(AVTags.Items.METEORIC_STONES))
                .save(output);
        shaped(RecipeCategory.MISC, AVItems.CONVERGENCE_ALTAR.get())
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('S', AVTags.Items.ASTRAL_SHARDS)
                .define('D', Tags.Items.GEMS_DIAMOND)
                .define('M', AVTags.Items.METEORIC_STONES)
                .pattern("GSG")
                .pattern("MDM")
                .pattern("MMM")
                .unlockedBy("has_astral_shard", has(AVTags.Items.ASTRAL_SHARDS))
                .save(output);
        shaped(RecipeCategory.MISC, AVItems.ASTRAL_FORGE.get())
                .define('I', Tags.Items.INGOTS_IRON)
                .define('S', AVTags.Items.ASTRAL_SHARDS)
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('M', AVTags.Items.METEORIC_STONES)
                .pattern("ISI")
                .pattern("MGM")
                .pattern("MMM")
                .unlockedBy("has_astral_shard", has(AVTags.Items.ASTRAL_SHARDS))
                .save(output);
        starlightRecipes(output);
        shaped(RecipeCategory.MISC, AVItems.STARLIGHT_COLLECTOR.get())
                .define('A', Tags.Items.GEMS_AMETHYST)
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('S', AVTags.Items.ASTRAL_SHARDS)
                .define('M', AVTags.Items.METEORIC_STONES)
                .pattern(" A ")
                .pattern("GSG")
                .pattern("MMM")
                .unlockedBy("has_astral_shard", has(AVTags.Items.ASTRAL_SHARDS))
                .save(output);
        shaped(RecipeCategory.MISC, AVItems.LENS.get(), 2)
                .define('N', Tags.Items.NUGGETS_GOLD)
                .define('S', AVTags.Items.ASTRAL_SHARDS)
                .define('P', Tags.Items.GLASS_PANES)
                .pattern("NPN")
                .pattern("PSP")
                .pattern("NPN")
                .unlockedBy("has_astral_shard", has(AVTags.Items.ASTRAL_SHARDS))
                .save(output);
        shaped(RecipeCategory.MISC, AVItems.MIRROR.get(), 2)
                .define('N', Tags.Items.NUGGETS_GOLD)
                .define('P', Tags.Items.GLASS_PANES)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('S', AVTags.Items.ASTRAL_SHARDS)
                .pattern("NPN")
                .pattern("NSN")
                .pattern(" I ")
                .unlockedBy("has_astral_shard", has(AVTags.Items.ASTRAL_SHARDS))
                .save(output);
        shaped(RecipeCategory.MISC, AVItems.PRISM.get())
                .define('S', AVTags.Items.ASTRAL_SHARDS)
                .define('A', Tags.Items.GEMS_AMETHYST)
                .define('N', Tags.Items.NUGGETS_GOLD)
                .pattern(" S ")
                .pattern("SAS")
                .pattern("NNN")
                .unlockedBy("has_astral_shard", has(AVTags.Items.ASTRAL_SHARDS))
                .save(output);
        shaped(RecipeCategory.MISC, AVItems.STARLIGHT_RESERVOIR.get())
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('P', Tags.Items.GLASS_BLOCKS)
                .define('S', AVTags.Items.ASTRAL_SHARDS)
                .pattern("GPG")
                .pattern("PSP")
                .pattern("GPG")
                .unlockedBy("has_astral_shard", has(AVTags.Items.ASTRAL_SHARDS))
                .save(output);
        shaped(RecipeCategory.TOOLS, AVItems.STAR_CHART.get())
                .define('G', Tags.Items.NUGGETS_GOLD)
                .define('S', AVTags.Items.ASTRAL_SHARDS)
                .define('P', AVTags.Items.CHART_PAPER)
                .pattern(" G ")
                .pattern("PSP")
                .pattern(" P ")
                .unlockedBy("has_astral_shard", has(AVTags.Items.ASTRAL_SHARDS))
                .save(output);
        shaped(RecipeCategory.MISC, AVItems.CODEX_CAELESTIS.get())
                .define('G', Tags.Items.NUGGETS_GOLD)
                .define('S', AVTags.Items.ASTRAL_SHARDS)
                .define('B', AVTags.Items.CODEX_BOOKS)
                .pattern(" G ")
                .pattern("SBS")
                .pattern(" G ")
                .unlockedBy("has_astral_shard", has(AVTags.Items.ASTRAL_SHARDS))
                .save(output);
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
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AVItems.STAR_ANCHOR.get(), 2)
                .define('S', AVTags.Items.ASTRAL_SHARDS)
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('M', AVTags.Items.METEORIC_STONES)
                .pattern(" S ")
                .pattern(" G ")
                .pattern("MMM")
                .unlockedBy("has_astral_shard", has(AVTags.Items.ASTRAL_SHARDS))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AVItems.PEDESTAL.get(), 2)
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('M', AVTags.Items.METEORIC_STONES)
                .pattern(" G ")
                .pattern(" M ")
                .pattern("MMM")
                .unlockedBy("has_meteoric_stone", has(AVTags.Items.METEORIC_STONES))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AVItems.CONVERGENCE_ALTAR.get())
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('S', AVTags.Items.ASTRAL_SHARDS)
                .define('D', Tags.Items.GEMS_DIAMOND)
                .define('M', AVTags.Items.METEORIC_STONES)
                .pattern("GSG")
                .pattern("MDM")
                .pattern("MMM")
                .unlockedBy("has_astral_shard", has(AVTags.Items.ASTRAL_SHARDS))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AVItems.ASTRAL_FORGE.get())
                .define('I', Tags.Items.INGOTS_IRON)
                .define('S', AVTags.Items.ASTRAL_SHARDS)
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('M', AVTags.Items.METEORIC_STONES)
                .pattern("ISI")
                .pattern("MGM")
                .pattern("MMM")
                .unlockedBy("has_astral_shard", has(AVTags.Items.ASTRAL_SHARDS))
                .save(output);
        starlightRecipes(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AVItems.STARLIGHT_COLLECTOR.get())
                .define('A', Tags.Items.GEMS_AMETHYST)
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('S', AVTags.Items.ASTRAL_SHARDS)
                .define('M', AVTags.Items.METEORIC_STONES)
                .pattern(" A ")
                .pattern("GSG")
                .pattern("MMM")
                .unlockedBy("has_astral_shard", has(AVTags.Items.ASTRAL_SHARDS))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AVItems.LENS.get(), 2)
                .define('N', Tags.Items.NUGGETS_GOLD)
                .define('S', AVTags.Items.ASTRAL_SHARDS)
                .define('P', Tags.Items.GLASS_PANES)
                .pattern("NPN")
                .pattern("PSP")
                .pattern("NPN")
                .unlockedBy("has_astral_shard", has(AVTags.Items.ASTRAL_SHARDS))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AVItems.MIRROR.get(), 2)
                .define('N', Tags.Items.NUGGETS_GOLD)
                .define('P', Tags.Items.GLASS_PANES)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('S', AVTags.Items.ASTRAL_SHARDS)
                .pattern("NPN")
                .pattern("NSN")
                .pattern(" I ")
                .unlockedBy("has_astral_shard", has(AVTags.Items.ASTRAL_SHARDS))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AVItems.PRISM.get())
                .define('S', AVTags.Items.ASTRAL_SHARDS)
                .define('A', Tags.Items.GEMS_AMETHYST)
                .define('N', Tags.Items.NUGGETS_GOLD)
                .pattern(" S ")
                .pattern("SAS")
                .pattern("NNN")
                .unlockedBy("has_astral_shard", has(AVTags.Items.ASTRAL_SHARDS))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AVItems.STARLIGHT_RESERVOIR.get())
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('P', Tags.Items.GLASS_BLOCKS)
                .define('S', AVTags.Items.ASTRAL_SHARDS)
                .pattern("GPG")
                .pattern("PSP")
                .pattern("GPG")
                .unlockedBy("has_astral_shard", has(AVTags.Items.ASTRAL_SHARDS))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, AVItems.STAR_CHART.get())
                .define('G', Tags.Items.NUGGETS_GOLD)
                .define('S', AVTags.Items.ASTRAL_SHARDS)
                .define('P', AVTags.Items.CHART_PAPER)
                .pattern(" G ")
                .pattern("PSP")
                .pattern(" P ")
                .unlockedBy("has_astral_shard", has(AVTags.Items.ASTRAL_SHARDS))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AVItems.CODEX_CAELESTIS.get())
                .define('G', Tags.Items.NUGGETS_GOLD)
                .define('S', AVTags.Items.ASTRAL_SHARDS)
                .define('B', AVTags.Items.CODEX_BOOKS)
                .pattern(" G ")
                .pattern("SBS")
                .pattern(" G ")
                .unlockedBy("has_astral_shard", has(AVTags.Items.ASTRAL_SHARDS))
                .save(output);
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

    // ------------------------------------------------------------------ starlight recipes (custom types)

    private void starlightRecipes(RecipeOutput output) {
        forge(output, "starmetal_ingot", res(AVItems.STARMETAL_INGOT.get(), 1), Tint.STELLA, 120, 100,
                ing(Tags.Items.INGOTS_IRON), ing(AVTags.Items.ASTRAL_SHARDS), ing(AVTags.Items.ASTRAL_SHARDS));
        forge(output, "sunsteel_ingot", res(AVItems.SUNSTEEL_INGOT.get(), 1), Tint.SOL, 120, 100,
                ing(Tags.Items.INGOTS_GOLD), ing(AVTags.Items.ASTRAL_SHARDS), ing(AVTags.Items.ASTRAL_SHARDS));
        forge(output, "moonsilver_ingot", res(AVItems.MOONSILVER_INGOT.get(), 1), Tint.LUNA, 120, 100,
                ing(Tags.Items.INGOTS_IRON), ing(AVTags.Items.ASTRAL_SHARDS), ing(Tags.Items.GEMS_QUARTZ));

        convergence(output, "astral_core", res(AVItems.ASTRAL_CORE.get(), 1), ing(Tags.Items.GEMS_DIAMOND),
                Tint.PURE, 1000, 200, Optional.of(Alignment.STELLA),
                ing(AVTags.Items.INGOTS_STARMETAL), ing(AVTags.Items.INGOTS_SUNSTEEL),
                ing(AVTags.Items.INGOTS_MOONSILVER), ing(AVTags.Items.ASTRAL_SHARDS));
        // Phase 5 – living gear (vanilla gear as catalyst, aligned light)
        gear(output, "halo_edge", AVItems.HALO_EDGE.get(), Items.DIAMOND_SWORD, Tint.SOL, 800, Alignment.SOL,
                ing(AVTags.Items.INGOTS_SUNSTEEL), ing(AVTags.Items.INGOTS_SUNSTEEL),
                ing(AVTags.Items.ASTRAL_SHARDS), ing(AVTags.Items.ASTRAL_SHARDS));
        gear(output, "dawnbreaker", AVItems.DAWNBREAKER.get(), Items.DIAMOND_AXE, Tint.SOL, 900, Alignment.SOL,
                ing(AVTags.Items.INGOTS_SUNSTEEL), ing(AVTags.Items.INGOTS_SUNSTEEL),
                ing(AVTags.Items.INGOTS_SUNSTEEL), ing(AVTags.Items.ASTRAL_SHARDS));
        gear(output, "crescent", AVItems.CRESCENT.get(), Items.BOW, Tint.LUNA, 800, Alignment.LUNA,
                ing(AVTags.Items.INGOTS_MOONSILVER), ing(AVTags.Items.INGOTS_MOONSILVER),
                ing(Tags.Items.STRINGS), ing(Tags.Items.STRINGS));
        gear(output, "tidecaller", AVItems.TIDECALLER.get(), Items.CROSSBOW, Tint.LUNA, 900, Alignment.LUNA,
                ing(AVTags.Items.INGOTS_MOONSILVER), ing(AVTags.Items.INGOTS_MOONSILVER),
                ing(Tags.Items.GEMS_PRISMARINE), ing(Tags.Items.GEMS_PRISMARINE));
        gear(output, "starlight_aegis", AVItems.STARLIGHT_AEGIS.get(), Items.SHIELD, Tint.STELLA, 800, Alignment.STELLA,
                ing(AVTags.Items.INGOTS_STARMETAL), ing(AVTags.Items.INGOTS_STARMETAL),
                ing(AVTags.Items.ASTRAL_SHARDS), ing(AVTags.Items.ASTRAL_SHARDS));
        gear(output, "seraph_helmet", AVItems.SERAPH_HELMET.get(), Items.DIAMOND_HELMET, Tint.STELLA, 700, Alignment.STELLA,
                ing(AVTags.Items.INGOTS_STARMETAL), ing(AVTags.Items.INGOTS_STARMETAL),
                ing(AVTags.Items.ASTRAL_SHARDS), ing(AVTags.Items.ASTRAL_SHARDS));
        gear(output, "seraph_chestplate", AVItems.SERAPH_CHESTPLATE.get(), Items.DIAMOND_CHESTPLATE, Tint.STELLA, 1200, Alignment.STELLA,
                ing(AVTags.Items.INGOTS_STARMETAL), ing(AVTags.Items.INGOTS_STARMETAL), ing(AVTags.Items.INGOTS_STARMETAL),
                ing(AVTags.Items.INGOTS_STARMETAL), ing(AVTags.Items.ASTRAL_SHARDS), ing(AVTags.Items.ASTRAL_SHARDS));
        gear(output, "seraph_leggings", AVItems.SERAPH_LEGGINGS.get(), Items.DIAMOND_LEGGINGS, Tint.STELLA, 1000, Alignment.STELLA,
                ing(AVTags.Items.INGOTS_STARMETAL), ing(AVTags.Items.INGOTS_STARMETAL), ing(AVTags.Items.INGOTS_STARMETAL),
                ing(AVTags.Items.INGOTS_STARMETAL), ing(AVTags.Items.ASTRAL_SHARDS), ing(AVTags.Items.ASTRAL_SHARDS));
        gear(output, "seraph_boots", AVItems.SERAPH_BOOTS.get(), Items.DIAMOND_BOOTS, Tint.STELLA, 700, Alignment.STELLA,
                ing(AVTags.Items.INGOTS_STARMETAL), ing(AVTags.Items.INGOTS_STARMETAL),
                ing(AVTags.Items.ASTRAL_SHARDS), ing(AVTags.Items.ASTRAL_SHARDS));

        sigil(output, Sigil.WHIRL, Tint.SOL, ing(Tags.Items.FEATHERS), ing(Tags.Items.FEATHERS),
                ing(AVTags.Items.INGOTS_SUNSTEEL), ing(AVTags.Items.INGOTS_SUNSTEEL));
        sigil(output, Sigil.SHIELDBREAKER, Tint.SOL, ing(Tags.Items.INGOTS_IRON), ing(Tags.Items.INGOTS_IRON),
                ing(Tags.Items.OBSIDIANS), ing(Tags.Items.OBSIDIANS));
        sigil(output, Sigil.DAWN, Tint.SOL, ing(Tags.Items.DUSTS_GLOWSTONE), ing(Tags.Items.DUSTS_GLOWSTONE),
                Ingredient.of(Items.SUNFLOWER), Ingredient.of(Items.SUNFLOWER));
        sigil(output, Sigil.FLASH, Tint.SOL, ing(Tags.Items.FEATHERS), ing(Tags.Items.FEATHERS),
                Ingredient.of(Items.SUGAR), Ingredient.of(Items.SUGAR));
        sigil(output, Sigil.PRECISION, Tint.LUNA, Ingredient.of(Items.SPIDER_EYE), Ingredient.of(Items.SPIDER_EYE),
                ing(AVTags.Items.INGOTS_MOONSILVER), ing(AVTags.Items.INGOTS_MOONSILVER));
        sigil(output, Sigil.UNDERTOW, Tint.LUNA, ing(Tags.Items.GEMS_PRISMARINE), ing(Tags.Items.GEMS_PRISMARINE),
                Ingredient.of(Items.KELP), Ingredient.of(Items.KELP));
        sigil(output, Sigil.AEGIS, Tint.STELLA, ing(AVTags.Items.INGOTS_STARMETAL), ing(AVTags.Items.INGOTS_STARMETAL),
                ing(Tags.Items.OBSIDIANS), ing(Tags.Items.OBSIDIANS));

        convergence(output, "ascension_key", res(AVItems.ASCENSION_KEY.get(), 1), Ingredient.of(AVItems.ASTRAL_CORE.get()),
                Tint.PURE, 2000, 300, Optional.of(Alignment.STELLA),
                ing(AVTags.Items.INGOTS_STARMETAL), ing(AVTags.Items.INGOTS_SUNSTEEL),
                ing(AVTags.Items.INGOTS_MOONSILVER), Ingredient.of(AVItems.VOID_SHARD.get()),
                ing(Tags.Items.FEATHERS), ing(Tags.Items.FEATHERS));
        convergence(output, "sigil_blank", res(AVItems.SIGIL_BLANK.get(), 2), ing(AVTags.Items.INGOTS_STARMETAL),
                Tint.STELLA, 400, 120, Optional.of(Alignment.STELLA),
                ing(Tags.Items.NUGGETS_GOLD), ing(Tags.Items.NUGGETS_GOLD),
                ing(Tags.Items.GEMS_AMETHYST), ing(Tags.Items.GEMS_AMETHYST));
    }

    private void forge(RecipeOutput output, String name, Result result, Tint tint, int starlight, int time, Ingredient... in) {
        save(output, "astral_forge/" + name, new StarlightRecipe(
                StarlightRecipe.Kind.FORGE, Optional.empty(), List.of(in), result.stack(), starlight, tint, time, Optional.empty()));
    }

    private void convergence(RecipeOutput output, String name, Result result, Ingredient center, Tint tint, int starlight,
            int time, Optional<Alignment> alignment, Ingredient... pedestals) {
        save(output, "convergence/" + name, new StarlightRecipe(
                StarlightRecipe.Kind.CONVERGENCE, Optional.of(center), List.of(pedestals), result.stack(), starlight, tint, time, alignment));
    }

    //? if >=26.1 {
    private static Result res(Item item, int count) {
        return new Result(new net.minecraft.world.item.ItemStackTemplate(item, count));
    }

    private record Result(net.minecraft.world.item.ItemStackTemplate stack) {}
    //?} else {
    /*private static Result res(Item item, int count) {
        return new Result(new ItemStack(item, count));
    }

    private record Result(ItemStack stack) {}
    *///?}

    private void gear(RecipeOutput output, String name, Item result, Item base, Tint tint, int starlight, Alignment alignment,
            Ingredient... pedestals) {
        convergence(output, name, res(result, 1), Ingredient.of(base), tint, starlight, 200, Optional.of(alignment), pedestals);
    }

    private void sigil(RecipeOutput output, Sigil sigil, Tint tint, Ingredient... pedestals) {
        convergence(output, "sigil_" + sigil.id(), res(AVItems.SIGILS.get(sigil).get(), 1),
                Ingredient.of(AVItems.SIGIL_BLANK.get()), tint, 300, 120, Optional.empty(), pedestals);
    }

    private static void save(RecipeOutput output, String path, StarlightRecipe recipe) {
        //? if >=26.1 {
        output.accept(ResourceKey.create(Registries.RECIPE, AstralVigil.id(path)), recipe, null);
        //?} else {
        /*output.accept(AstralVigil.id(path), recipe, null);
        *///?}
    }

    private Ingredient ing(TagKey<Item> tag) {
        //? if >=26.1 {
        return Ingredient.of(this.items.getOrThrow(tag));
        //?} else {
        /*return Ingredient.of(tag);
        *///?}
    }
}
