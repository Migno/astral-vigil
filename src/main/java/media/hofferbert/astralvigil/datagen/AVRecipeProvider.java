package media.hofferbert.astralvigil.datagen;

import media.hofferbert.astralvigil.registry.AVBuilding;
import net.minecraft.world.level.ItemLike;

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
import net.minecraft.tags.ItemTags;
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

    /** Gate frames: astral shards around a themed centre, themed corners; 4 per craft. */
    private void gate(media.hofferbert.astralvigil.firmament.GateKind kind, net.minecraft.tags.TagKey<net.minecraft.world.item.Item> centre,
            net.minecraft.tags.TagKey<net.minecraft.world.item.Item> corners) {
        shaped(RecipeCategory.MISC, AVItems.GATES.get(kind).get(), 4)
                .define('S', AVTags.Items.ASTRAL_SHARDS)
                .define('I', centre)
                .define('M', corners)
                .pattern("MSM")
                .pattern("SIS")
                .pattern("MSM")
                .unlockedBy("has_astral_shard", has(AVTags.Items.ASTRAL_SHARDS))
                .save(output);
    }

    @Override
    protected void buildRecipes() {
        buildingRecipes(output);
        shaped(RecipeCategory.MISC, AVItems.STAR_ANCHOR.get(), 2)
                .define('S', AVTags.Items.ASTRAL_SHARDS)
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('M', AVTags.Items.METEORIC_STONES)
                .pattern(" S ")
                .pattern(" G ")
                .pattern("MMM")
                .unlockedBy("has_astral_shard", has(AVTags.Items.ASTRAL_SHARDS))
                .save(output);
        // Gate frames: same pattern; centre and corners are themed after where the gate leads
        gate(media.hofferbert.astralvigil.firmament.GateKind.ASTRAL, AVTags.Items.INGOTS_STARMETAL, AVTags.Items.METEORIC_STONES);
        gate(media.hofferbert.astralvigil.firmament.GateKind.SOLAR, AVTags.Items.INGOTS_SUNSTEEL, Tags.Items.STORAGE_BLOCKS_GOLD);
        gate(media.hofferbert.astralvigil.firmament.GateKind.LUNAR, AVTags.Items.INGOTS_MOONSILVER, Tags.Items.GEMS_QUARTZ);
        gate(media.hofferbert.astralvigil.firmament.GateKind.PRISM, AVTags.Items.INGOTS_STARMETAL, Tags.Items.STORAGE_BLOCKS_DIAMOND);
        gate(media.hofferbert.astralvigil.firmament.GateKind.ARCHIVE, Tags.Items.GEMS_AMETHYST, Tags.Items.BOOKSHELVES);
        gate(media.hofferbert.astralvigil.firmament.GateKind.UMBRAL, Tags.Items.OBSIDIANS_CRYING, Tags.Items.OBSIDIANS_NORMAL);
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

    // Gate frames: astral shards around a themed centre, themed corners; 4 per craft.
    private void gate(RecipeOutput output, media.hofferbert.astralvigil.firmament.GateKind kind,
            net.minecraft.tags.TagKey<net.minecraft.world.item.Item> centre, net.minecraft.tags.TagKey<net.minecraft.world.item.Item> corners) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AVItems.GATES.get(kind).get(), 4)
                .define('S', AVTags.Items.ASTRAL_SHARDS)
                .define('I', centre)
                .define('M', corners)
                .pattern("MSM")
                .pattern("SIS")
                .pattern("MSM")
                .unlockedBy("has_astral_shard", has(AVTags.Items.ASTRAL_SHARDS))
                .save(output);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        buildingRecipes(output);
        // Gate frames: same pattern; centre and corners are themed after where the gate leads
        gate(output, media.hofferbert.astralvigil.firmament.GateKind.ASTRAL, AVTags.Items.INGOTS_STARMETAL, AVTags.Items.METEORIC_STONES);
        gate(output, media.hofferbert.astralvigil.firmament.GateKind.SOLAR, AVTags.Items.INGOTS_SUNSTEEL, Tags.Items.STORAGE_BLOCKS_GOLD);
        gate(output, media.hofferbert.astralvigil.firmament.GateKind.LUNAR, AVTags.Items.INGOTS_MOONSILVER, Tags.Items.GEMS_QUARTZ);
        gate(output, media.hofferbert.astralvigil.firmament.GateKind.PRISM, AVTags.Items.INGOTS_STARMETAL, Tags.Items.STORAGE_BLOCKS_DIAMOND);
        gate(output, media.hofferbert.astralvigil.firmament.GateKind.ARCHIVE, Tags.Items.GEMS_AMETHYST, Tags.Items.BOOKSHELVES);
        gate(output, media.hofferbert.astralvigil.firmament.GateKind.UMBRAL, Tags.Items.OBSIDIANS_CRYING, Tags.Items.OBSIDIANS_NORMAL);
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

    // ------------------------------------------------------------------ celestial building blocks

    //? if >=26.1 {
    private net.minecraft.data.recipes.ShapedRecipeBuilder sh(ItemLike result, int count) {
        return shaped(RecipeCategory.BUILDING_BLOCKS, result, count);
    }

    private void cut(RecipeOutput output, ItemLike result, ItemLike base, int count) {
        stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, result, base, count);
    }

    private void crack(RecipeOutput output, ItemLike result, ItemLike base) {
        smeltingResultFromBase(result, base);
    }

    private void woodRecipes(RecipeOutput output) {
        planksFromLog(AVBuilding.STARWOOD_PLANKS.get(), AVBuilding.STARWOOD_LOGS, 4);
        woodFromLogs(AVBuilding.STARWOOD.get(), AVBuilding.STARWOOD_LOG.get());
        woodFromLogs(AVBuilding.STRIPPED_STARWOOD.get(), AVBuilding.STRIPPED_STARWOOD_LOG.get());
        AVBuildingModels.families().forEach(f -> generateRecipes(f, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
    }
    //?} else {
    /*private net.minecraft.data.recipes.ShapedRecipeBuilder sh(ItemLike result, int count) {
        return ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, result, count);
    }

    private void cut(RecipeOutput output, ItemLike result, ItemLike base, int count) {
        stonecutterResultFromBase(output, RecipeCategory.BUILDING_BLOCKS, result, base, count);
    }

    private void crack(RecipeOutput output, ItemLike result, ItemLike base) {
        smeltingResultFromBase(output, result, base);
    }

    private void woodRecipes(RecipeOutput output) {
        planksFromLog(output, AVBuilding.STARWOOD_PLANKS.get(), AVBuilding.STARWOOD_LOGS, 4);
        woodFromLogs(output, AVBuilding.STARWOOD.get(), AVBuilding.STARWOOD_LOG.get());
        woodFromLogs(output, AVBuilding.STRIPPED_STARWOOD.get(), AVBuilding.STRIPPED_STARWOOD_LOG.get());
        AVBuildingModels.families().forEach(f -> generateRecipes(output, f, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
    }
    *///?}

    private void ring(RecipeOutput output, ItemLike result, int count, ItemLike around, TagKey<Item> centre) {
        sh(result, count).define('A', around).define('C', centre).pattern("AAA").pattern("ACA").pattern("AAA")
                .unlockedBy("has_centre", has(centre)).save(output);
    }

    private void bricks(RecipeOutput output, ItemLike result, ItemLike base) {
        sh(result, 4).define('B', base).pattern("BB").pattern("BB").unlockedBy("has_base", has(base)).save(output);
        cut(output, result, base, 1);
    }

    private void lamp(RecipeOutput output, ItemLike result, ItemLike stone) {
        sh(result, 4).define('S', stone).define('L', Items.GLOWSTONE).pattern(" S ").pattern("SLS").pattern(" S ")
                .unlockedBy("has_stone", has(stone)).save(output);
    }

    private void buildingRecipes(RecipeOutput output) {
        woodRecipes(output);
        ring(output, AVBuilding.CELESTIAL_MARBLE.get(), 8, Items.CALCITE, AVTags.Items.ASTRAL_SHARDS);
        ring(output, AVBuilding.SUNSTONE.get(), 8, Items.SMOOTH_SANDSTONE, AVTags.Items.INGOTS_SUNSTEEL);
        ring(output, AVBuilding.MOONSTONE.get(), 8, Items.DIORITE, AVTags.Items.INGOTS_MOONSILVER);
        ring(output, AVBuilding.UMBRAL_STONE.get(), 8, Items.COBBLED_DEEPSLATE, AVTags.Items.ASTRAL_SHARDS);
        ring(output, AVBuilding.GILDED_STARWOOD.get(), 8, AVBuilding.STARWOOD_PLANKS.get(), Tags.Items.INGOTS_GOLD);
        ring(output, AVBuilding.GILDED_CELESTIAL_BRICKS.get(), 8, AVBuilding.CELESTIAL_BRICKS.get(), Tags.Items.INGOTS_GOLD);
        ring(output, AVBuilding.CONSTELLATION_TILES.get(), 8, Items.DEEPSLATE_TILES, AVTags.Items.ASTRAL_SHARDS);
        ring(output, AVBuilding.HALO_GLASS.get(), 8, Items.GLASS, Tags.Items.INGOTS_GOLD);
        ring(output, AVBuilding.NEBULA_GLASS.get(), 8, Items.GLASS, Tags.Items.GEMS_AMETHYST);
        ring(output, AVBuilding.STAR_LANTERN.get(), 1, Items.GOLD_NUGGET, AVTags.Items.ASTRAL_SHARDS);
        bricks(output, AVBuilding.CELESTIAL_BRICKS.get(), AVBuilding.CELESTIAL_MARBLE.get());
        bricks(output, AVBuilding.SUNSTONE_BRICKS.get(), AVBuilding.SUNSTONE.get());
        bricks(output, AVBuilding.MOONSTONE_BRICKS.get(), AVBuilding.MOONSTONE.get());
        bricks(output, AVBuilding.UMBRAL_BRICKS.get(), AVBuilding.UMBRAL_STONE.get());
        ItemLike marble = AVBuilding.CELESTIAL_MARBLE.get();
        sh(AVBuilding.CELESTIAL_PILLAR.get(), 2).define('M', marble).pattern("M").pattern("M")
                .unlockedBy("has_marble", has(marble)).save(output);
        cut(output, AVBuilding.CELESTIAL_PILLAR.get(), marble, 1);
        ring(output, AVBuilding.ECLIPSE_GLASS.get(), 8, Items.GLASS, Tags.Items.OBSIDIANS_CRYING);
        ItemLike umbralBricks = AVBuilding.UMBRAL_BRICKS.get();
        sh(AVBuilding.UMBRAL_PILLAR.get(), 2).define('M', umbralBricks).pattern("M").pattern("M")
                .unlockedBy("has_umbral_bricks", has(umbralBricks)).save(output);
        cut(output, AVBuilding.UMBRAL_PILLAR.get(), umbralBricks, 1);
        crack(output, AVBuilding.CRACKED_UMBRAL_BRICKS.get(), umbralBricks);
        crack(output, AVBuilding.CRACKED_CELESTIAL_BRICKS.get(), AVBuilding.CELESTIAL_BRICKS.get());
        lamp(output, AVBuilding.ASTRAL_LAMP.get(), marble);
        lamp(output, AVBuilding.SUNSTONE_LAMP.get(), AVBuilding.SUNSTONE.get());
        lamp(output, AVBuilding.MOONSTONE_LAMP.get(), AVBuilding.MOONSTONE.get());
        lamp(output, AVBuilding.UMBRAL_LAMP.get(), AVBuilding.UMBRAL_STONE.get());
        // stonecutting: every stone variant from its family base
        for (var f : AVBuildingModels.families()) {
            if (f.getBaseBlock() != AVBuilding.STARWOOD_PLANKS.get()) {
                f.getVariants().forEach((variant, block) -> cut(output, block, f.getBaseBlock(),
                        variant == net.minecraft.data.BlockFamily.Variant.SLAB ? 2 : 1));
            }
        }
    }

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
        // Solar Regalia (Sunsteel, Sol light) and Lunar Raiment (Moonsilver, Luna light): mid tier, iron armour as catalyst
        gear(output, "solar_helmet", AVItems.SOLAR_HELMET.get(), Items.IRON_HELMET, Tint.SOL, 500, Alignment.SOL,
                ing(AVTags.Items.INGOTS_SUNSTEEL), ing(AVTags.Items.INGOTS_SUNSTEEL),
                ing(Tags.Items.DUSTS_GLOWSTONE), ing(Tags.Items.DUSTS_GLOWSTONE));
        gear(output, "solar_chestplate", AVItems.SOLAR_CHESTPLATE.get(), Items.IRON_CHESTPLATE, Tint.SOL, 800, Alignment.SOL,
                ing(AVTags.Items.INGOTS_SUNSTEEL), ing(AVTags.Items.INGOTS_SUNSTEEL), ing(AVTags.Items.INGOTS_SUNSTEEL),
                ing(AVTags.Items.INGOTS_SUNSTEEL), ing(Tags.Items.RODS_BLAZE), ing(Tags.Items.RODS_BLAZE));
        gear(output, "solar_leggings", AVItems.SOLAR_LEGGINGS.get(), Items.IRON_LEGGINGS, Tint.SOL, 700, Alignment.SOL,
                ing(AVTags.Items.INGOTS_SUNSTEEL), ing(AVTags.Items.INGOTS_SUNSTEEL), ing(AVTags.Items.INGOTS_SUNSTEEL),
                ing(AVTags.Items.INGOTS_SUNSTEEL), ing(Tags.Items.DUSTS_GLOWSTONE), ing(Tags.Items.DUSTS_GLOWSTONE));
        gear(output, "solar_boots", AVItems.SOLAR_BOOTS.get(), Items.IRON_BOOTS, Tint.SOL, 500, Alignment.SOL,
                ing(AVTags.Items.INGOTS_SUNSTEEL), ing(AVTags.Items.INGOTS_SUNSTEEL),
                ing(Tags.Items.RODS_BLAZE), ing(Tags.Items.RODS_BLAZE));
        gear(output, "lunar_helmet", AVItems.LUNAR_HELMET.get(), Items.IRON_HELMET, Tint.LUNA, 500, Alignment.LUNA,
                ing(AVTags.Items.INGOTS_MOONSILVER), ing(AVTags.Items.INGOTS_MOONSILVER),
                ing(ItemTags.WOOL), ing(ItemTags.WOOL));
        gear(output, "lunar_chestplate", AVItems.LUNAR_CHESTPLATE.get(), Items.IRON_CHESTPLATE, Tint.LUNA, 800, Alignment.LUNA,
                ing(AVTags.Items.INGOTS_MOONSILVER), ing(AVTags.Items.INGOTS_MOONSILVER), ing(AVTags.Items.INGOTS_MOONSILVER),
                ing(AVTags.Items.INGOTS_MOONSILVER), ing(ItemTags.WOOL), Ingredient.of(Items.PHANTOM_MEMBRANE));
        gear(output, "lunar_leggings", AVItems.LUNAR_LEGGINGS.get(), Items.IRON_LEGGINGS, Tint.LUNA, 700, Alignment.LUNA,
                ing(AVTags.Items.INGOTS_MOONSILVER), ing(AVTags.Items.INGOTS_MOONSILVER), ing(AVTags.Items.INGOTS_MOONSILVER),
                ing(AVTags.Items.INGOTS_MOONSILVER), ing(ItemTags.WOOL), ing(ItemTags.WOOL));
        gear(output, "lunar_boots", AVItems.LUNAR_BOOTS.get(), Items.IRON_BOOTS, Tint.LUNA, 500, Alignment.LUNA,
                ing(AVTags.Items.INGOTS_MOONSILVER), ing(AVTags.Items.INGOTS_MOONSILVER),
                Ingredient.of(Items.PHANTOM_MEMBRANE), ing(Tags.Items.FEATHERS));

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
        convergence(output, "umbral_sigil", res(AVItems.UMBRAL_SIGIL.get(), 1), Ingredient.of(AVItems.VOID_SHARD.get()),
                Tint.STELLA, 3000, 400, Optional.empty(),
                Ingredient.of(AVItems.ASTRAL_CORE.get()), Ingredient.of(media.hofferbert.astralvigil.registry.AVBlocks.VOID_CRYSTAL.get()),
                Ingredient.of(media.hofferbert.astralvigil.registry.AVBlocks.VOID_CRYSTAL.get()), ing(Tags.Items.OBSIDIANS), ing(Tags.Items.OBSIDIANS),
                Ingredient.of(Items.ECHO_SHARD));
        convergence(output, "halo_of_dawn", res(AVItems.HALO_OF_DAWN.get(), 1), Ingredient.of(AVItems.ECLIPSE_HEART.get()),
                Tint.PURE, 5000, 600, Optional.of(Alignment.SOL),
                ing(AVTags.Items.INGOTS_SUNSTEEL), ing(AVTags.Items.INGOTS_SUNSTEEL), ing(AVTags.Items.INGOTS_MOONSILVER),
                ing(AVTags.Items.INGOTS_STARMETAL), Ingredient.of(AVItems.SERAPH_FEATHER.get()), Ingredient.of(AVItems.SERAPH_FEATHER.get()));
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
