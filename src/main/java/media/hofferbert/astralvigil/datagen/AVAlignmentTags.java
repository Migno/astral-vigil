package media.hofferbert.astralvigil.datagen;

import java.util.concurrent.CompletableFuture;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.worldgen.AVWorldgen;
import net.neoforged.neoforge.common.Tags;
import media.hofferbert.astralvigil.research.Alignment;
import media.hofferbert.astralvigil.research.AlignmentTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.data.tags.StructureTagsProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
//? if <26.1 {
/*import net.neoforged.neoforge.common.data.ExistingFileHelper;
*///?}

/**
 * Default members of the {@code #astralvigil:alignment/*} tags for creatures, biomes and structures (block alignments
 * live in {@link AVTagProviders.BlockTagProvider}). Only vanilla content that exists on both targets.
 */
public final class AVAlignmentTags {
    private AVAlignmentTags() {}

    public static class Entities extends EntityTypeTagsProvider {
        //? if >=26.1 {
        public Entities(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
            super(output, lookup, AstralVigil.MOD_ID);
        }
        //?} else {
        /*public Entities(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper efh) {
            super(output, lookup, AstralVigil.MOD_ID, efh);
        }
        *///?}

        @Override
        protected void addTags(HolderLookup.Provider registries) {
            tag(AlignmentTags.entities(Alignment.SOL))
                    .add(EntityType.BLAZE, EntityType.BEE, EntityType.MAGMA_CUBE, EntityType.STRIDER, EntityType.CAMEL, EntityType.SNIFFER);
            tag(AlignmentTags.entities(Alignment.LUNA))
                    .add(EntityType.WOLF, EntityType.FOX, EntityType.BAT, EntityType.PHANTOM, EntityType.POLAR_BEAR,
                            EntityType.CAT, EntityType.GLOW_SQUID, EntityType.AXOLOTL, EntityType.DROWNED);
            tag(AlignmentTags.entities(Alignment.STELLA))
                    .add(EntityType.ALLAY, EntityType.PARROT, EntityType.DOLPHIN, EntityType.SHULKER);
            tag(AlignmentTags.entities(Alignment.VOID))
                    .add(EntityType.ENDERMAN, EntityType.ENDERMITE, EntityType.WARDEN, EntityType.WITHER,
                            EntityType.WITHER_SKELETON, EntityType.ZOMBIE, EntityType.SKELETON, EntityType.HUSK,
                            EntityType.STRAY, EntityType.VEX, EntityType.EVOKER);
        }
    }

    public static class BiomeAlignments extends BiomeTagsProvider {
        //? if >=26.1 {
        public BiomeAlignments(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
            super(output, lookup, AstralVigil.MOD_ID);
        }
        //?} else {
        /*public BiomeAlignments(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper efh) {
            super(output, lookup, AstralVigil.MOD_ID, efh);
        }
        *///?}

        @Override
        protected void addTags(HolderLookup.Provider registries) {
            tag(AlignmentTags.biomes(Alignment.SOL))
                    .add(Biomes.DESERT, Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA,
                            Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS, Biomes.SUNFLOWER_PLAINS,
                            Biomes.NETHER_WASTES, Biomes.BASALT_DELTAS, Biomes.CRIMSON_FOREST);
            tag(AlignmentTags.biomes(Alignment.LUNA))
                    .add(Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES, Biomes.SNOWY_TAIGA, Biomes.FROZEN_OCEAN,
                            Biomes.DEEP_FROZEN_OCEAN, Biomes.FROZEN_RIVER, Biomes.SNOWY_BEACH, Biomes.SWAMP,
                            Biomes.MANGROVE_SWAMP, Biomes.GROVE);
            tag(AlignmentTags.biomes(Alignment.STELLA))
                    .add(Biomes.MEADOW, Biomes.CHERRY_GROVE, Biomes.JAGGED_PEAKS, Biomes.FROZEN_PEAKS, Biomes.STONY_PEAKS,
                            Biomes.THE_END, Biomes.END_HIGHLANDS, Biomes.END_MIDLANDS, Biomes.SMALL_END_ISLANDS);
            tag(AVWorldgen.HAS_FALLEN_OBSERVATORY)
                    .addTag(Tags.Biomes.IS_PLAINS)
                    .addTag(Tags.Biomes.IS_DESERT)
                    .addTag(Tags.Biomes.IS_SAVANNA)
                    .addTag(Tags.Biomes.IS_MOUNTAIN)
                    .addTag(Tags.Biomes.IS_BADLANDS)
                    .addTag(Tags.Biomes.IS_TAIGA)
                    .addTag(Tags.Biomes.IS_SNOWY_PLAINS);
            tag(AlignmentTags.biomes(Alignment.VOID))
                    .add(Biomes.DEEP_DARK, Biomes.SOUL_SAND_VALLEY, Biomes.WARPED_FOREST, Biomes.THE_VOID,
                            Biomes.END_BARRENS, Biomes.DARK_FOREST);
        }
    }

    public static class Structures extends StructureTagsProvider {
        //? if >=26.1 {
        public Structures(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
            super(output, lookup, AstralVigil.MOD_ID);
        }
        //?} else {
        /*public Structures(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper efh) {
            super(output, lookup, AstralVigil.MOD_ID, efh);
        }
        *///?}

        @Override
        protected void addTags(HolderLookup.Provider registries) {
            tag(AlignmentTags.structures(Alignment.SOL))
                    .add(BuiltinStructures.DESERT_PYRAMID, BuiltinStructures.BASTION_REMNANT, BuiltinStructures.FORTRESS,
                            BuiltinStructures.VILLAGE_DESERT, BuiltinStructures.VILLAGE_SAVANNA,
                            BuiltinStructures.RUINED_PORTAL_DESERT);
            tag(AlignmentTags.structures(Alignment.LUNA))
                    .add(BuiltinStructures.IGLOO, BuiltinStructures.OCEAN_RUIN_COLD, BuiltinStructures.SHIPWRECK,
                            BuiltinStructures.VILLAGE_SNOWY, BuiltinStructures.SWAMP_HUT);
            tag(AlignmentTags.structures(Alignment.STELLA))
                    .add(BuiltinStructures.END_CITY, BuiltinStructures.STRONGHOLD, BuiltinStructures.TRIAL_CHAMBERS,
                            BuiltinStructures.TRAIL_RUINS);
            tag(AlignmentTags.structures(Alignment.VOID))
                    .add(BuiltinStructures.ANCIENT_CITY, BuiltinStructures.WOODLAND_MANSION,
                            BuiltinStructures.PILLAGER_OUTPOST, BuiltinStructures.OCEAN_MONUMENT);
        }
    }
}
