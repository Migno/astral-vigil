package media.hofferbert.astralvigil.datagen;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.worldgen.AVWorldgen;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.data.event.GatherDataEvent;
//? if <26.1 {
/*import net.neoforged.neoforge.common.data.ExistingFileHelper;
*///?}

/**
 * Registers all data providers. Client-only class (model providers live in client packages on 26.1), registered
 * from {@link media.hofferbert.astralvigil.AstralVigilClient}.
 */
public final class AVDataGen {
    private AVDataGen() {}

    //? if >=26.1 {
    public static void gather(GatherDataEvent.Client event) {
        DataGenerator gen = event.getGenerator();
        PackOutput out = gen.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        gen.addProvider(true, new AVModelProvider(out));
        gen.addProvider(true, new AVEquipmentProvider(out));
        worldgen(event);
        common(gen, out, lookup);
        gen.addProvider(true, new AVTagProviders.BlockTagProvider(out, lookup));
        gen.addProvider(true, new AVTagProviders.ItemTagProvider(out, lookup));
        gen.addProvider(true, new AVAlignmentTags.Entities(out, lookup));
        gen.addProvider(true, new AVAlignmentTags.BiomeAlignments(out, lookup));
        gen.addProvider(true, new AVAlignmentTags.Structures(out, lookup));
    }
    //?} else {
    /*public static void gather(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput out = gen.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        ExistingFileHelper efh = event.getExistingFileHelper();

        worldgen(event);
        gen.addProvider(event.includeClient(), new AVModelProvider.BlockStates(out, efh));
        gen.addProvider(event.includeClient(), new AVModelProvider.ItemModels(out, efh));
        common(gen, out, lookup);
        var blockTags = gen.addProvider(event.includeServer(), new AVTagProviders.BlockTagProvider(out, lookup, efh));
        gen.addProvider(event.includeServer(), new AVTagProviders.ItemTagProvider(out, lookup, blockTags.contentsGetter(), efh));
        gen.addProvider(event.includeServer(), new AVAlignmentTags.Entities(out, lookup, efh));
        gen.addProvider(event.includeServer(), new AVAlignmentTags.BiomeAlignments(out, lookup, efh));
        gen.addProvider(event.includeServer(), new AVAlignmentTags.Structures(out, lookup, efh));
    }
    *///?}

    /** Worldgen registries (structure, template pool, structure set) as datapack JSON. */
    static void worldgen(GatherDataEvent event) {
        event.createDatapackRegistryObjects(AVWorldgen.builder());
    }

    private static void common(DataGenerator gen, PackOutput out, CompletableFuture<HolderLookup.Provider> lookup) {
        gen.addProvider(true, new AVLanguageProvider(out, "en_us"));
        gen.addProvider(true, new AVLanguageProvider(out, "de_de"));
        gen.addProvider(
                true,
                new LootTableProvider(
                        out,
                        Set.of(),
                        List.of(
                                new LootTableProvider.SubProviderEntry(AVLootProvider::new, LootContextParamSets.BLOCK),
                                new LootTableProvider.SubProviderEntry(AVChestLoot::new, LootContextParamSets.CHEST)),
                        lookup));
        gen.addProvider(true, AVRecipeProvider.create(out, lookup));
        gen.addProvider(true, new AdvancementProvider(out, lookup, List.of(new AVAdvancements())));
        gen.addProvider(true, new AVCodexProvider(out));
        AstralVigil.LOGGER.info("Astral Vigil data providers registered");
    }
}
