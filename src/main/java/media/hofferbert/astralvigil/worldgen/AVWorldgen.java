package media.hofferbert.astralvigil.worldgen;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.firmament.Realm;
import media.hofferbert.astralvigil.registry.AVBuilding;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

/**
 * Datapack worldgen (generated JSON): Fallen Observatory structure, its template pool and structure set. Disable or
 * retune it with a datapack that overrides {@code data/astralvigil/worldgen/structure_set/fallen_observatory.json}.
 */
public final class AVWorldgen {
    public static final ResourceKey<Structure> FALLEN_OBSERVATORY =
            ResourceKey.create(Registries.STRUCTURE, AstralVigil.id("fallen_observatory"));
    public static final ResourceKey<StructureTemplatePool> OBSERVATORY_POOL =
            ResourceKey.create(Registries.TEMPLATE_POOL, AstralVigil.id("fallen_observatory/start"));
    public static final ResourceKey<StructureSet> OBSERVATORY_SET =
            ResourceKey.create(Registries.STRUCTURE_SET, AstralVigil.id("fallen_observatory"));
    public static final TagKey<Biome> HAS_FALLEN_OBSERVATORY =
            TagKey.create(Registries.BIOME, AstralVigil.id("has_structure/fallen_observatory"));

    private AVWorldgen() {}

    public static RegistrySetBuilder builder() {
        return new RegistrySetBuilder()
                .add(Registries.TEMPLATE_POOL, AVWorldgen::pools)
                .add(Registries.STRUCTURE, AVWorldgen::structures)
                .add(Registries.STRUCTURE_SET, AVWorldgen::sets)
                .add(Registries.CONFIGURED_FEATURE, AVWorldgen::configuredFeatures)
                .add(Registries.PLACED_FEATURE, AVWorldgen::placedFeatures)
                .add(Registries.DIMENSION_TYPE, AVWorldgen::dimensionTypes)
                .add(Registries.BIOME, AVWorldgen::biomes)
                .add(Registries.LEVEL_STEM, AVWorldgen::dimensions);
    }

    static void pools(BootstrapContext<StructureTemplatePool> ctx) {
        HolderGetter<StructureTemplatePool> pools = ctx.lookup(Registries.TEMPLATE_POOL);
        ctx.register(OBSERVATORY_POOL, new StructureTemplatePool(
                pools.getOrThrow(Pools.EMPTY),
                List.of(
                        Pair.of(StructurePoolElement.single(AstralVigil.MOD_ID + ":fallen_observatory/halo"), 1),
                        Pair.of(StructurePoolElement.single(AstralVigil.MOD_ID + ":fallen_observatory/crescent"), 1),
                        Pair.of(StructurePoolElement.single(AstralVigil.MOD_ID + ":fallen_observatory/anvil"), 1),
                        Pair.of(StructurePoolElement.single(AstralVigil.MOD_ID + ":fallen_observatory/quill"), 1)),
                StructureTemplatePool.Projection.RIGID));
    }

    static void structures(BootstrapContext<Structure> ctx) {
        HolderSet<Biome> biomes = ctx.lookup(Registries.BIOME).getOrThrow(HAS_FALLEN_OBSERVATORY);
        ctx.register(FALLEN_OBSERVATORY, new JigsawStructure(
                new Structure.StructureSettings(biomes, Map.of(), GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.BEARD_THIN),
                ctx.lookup(Registries.TEMPLATE_POOL).getOrThrow(OBSERVATORY_POOL),
                Optional.empty(),
                1,
                ConstantHeight.of(VerticalAnchor.absolute(0)),
                false,
                Optional.of(Heightmap.Types.WORLD_SURFACE_WG),
                //? if >=26.1 {
                new JigsawStructure.MaxDistance(64),
                //?} else {
                /*64,
                *///?}
                List.of(),
                DimensionPadding.ZERO,
                LiquidSettings.APPLY_WATERLOGGING));
    }

    // ------------------------------------------------------------------ the realms of the Firmament

    public static final ResourceKey<net.minecraft.world.level.levelgen.placement.PlacedFeature> STARWOOD_TREES =
            ResourceKey.create(Registries.PLACED_FEATURE, AstralVigil.id("starwood_trees"));

    static ResourceKey<LevelStem> stem(Realm realm) {
        return ResourceKey.create(Registries.LEVEL_STEM, AstralVigil.id(realm.id()));
    }

    static ResourceKey<net.minecraft.world.level.dimension.DimensionType> type(Realm realm) {
        return ResourceKey.create(Registries.DIMENSION_TYPE, AstralVigil.id(realm.id()));
    }

    static ResourceKey<Biome> biome(Realm realm) {
        return ResourceKey.create(Registries.BIOME, AstralVigil.id(realm.id()));
    }

    /**
     * The fixed sky of a realm. {@code time121} is the 1.21.1 fixed day time (it also picks the moon phase there);
     * on 26.1 the same look comes from fixed environment attributes.
     */
    record Sky(long time121, boolean end, int sky, int fog, int water, float sunAngle, float stars, int moonPhase, int sunrise,
            float skyLight, float skyFactor, float ambient, net.minecraft.core.particles.ParticleOptions particle, float particleChance) {}

    static Sky sky(Realm realm) {
        return switch (realm) {
            // a moonlit night above the starwood groves
            case FIRMAMENT -> new Sky(18000, false, 0x0B0A1E, 0x1E2A5A, 0x3F76E4, 180f, 0.9f, 0, 0, 10f, 0.6f, 0.12f,
                    net.minecraft.core.particles.ParticleTypes.END_ROD, 0.0008f);
            // eternal dawn: the sun on the rim of the world, golden haze
            case SUNSPIRE -> new Sky(23300, false, 0x78A0E8, 0xF2B46B, 0xF2C94C, 262f, 0.12f, 0, 0xE9F2A04F, 13f, 0.9f, 0.05f,
                    net.minecraft.core.particles.ParticleTypes.WAX_ON, 0.003f);
            // a full moon over silver water
            case MOONWELL -> new Sky(18000, false, 0x10183A, 0x2A3B6E, 0x8FB8FF, 180f, 1.0f, 0, 0, 8f, 0.5f, 0.1f,
                    net.minecraft.core.particles.ParticleTypes.WHITE_ASH, 0.004f);
            // deep space around Orrin's forge, no moon, the stars at their brightest
            case STARFORGE -> new Sky(114000, false, 0x0A0614, 0x2A1450, 0x5B2A86, 180f, 1.0f, 4, 0, 5f, 0.3f, 0.1f,
                    net.minecraft.core.particles.ParticleTypes.END_ROD, 0.0015f);
            // the hush of the archive: a starless dark and drifting glyphs
            case ARCHIVE -> new Sky(114000, false, 0x07060F, 0x1B1426, 0x2C2A5E, 180f, 0.5f, 4, 0, 4f, 0.24f, 0.08f,
                    net.minecraft.core.particles.ParticleTypes.ENCHANT, 0.006f);
            // the Eclipse's sky: black, violet fog, rising motes of the void
            case UMBRAL_THRONE -> new Sky(6000, true, 0x000000, 0x1A0A2E, 0x5B2A86, 0f, 0f, 4, 0, 4f, 0f, 0.06f,
                    net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, 0.004f);
        };
    }

    static void configuredFeatures(BootstrapContext<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> ctx) {
        var tree = new net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration.TreeConfigurationBuilder(
                net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider.simple(AVBuilding.STARWOOD_LOG.get()),
                new net.minecraft.world.level.levelgen.feature.trunkplacers.CherryTrunkPlacer(7, 1, 0,
                        net.minecraft.util.valueproviders.UniformInt.of(1, 3), net.minecraft.util.valueproviders.UniformInt.of(2, 4),
                        net.minecraft.util.valueproviders.UniformInt.of(-4, -3), net.minecraft.util.valueproviders.UniformInt.of(-1, 0)),
                net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider.simple(AVBuilding.STARWOOD_LEAVES.get()),
                new net.minecraft.world.level.levelgen.feature.foliageplacers.CherryFoliagePlacer(
                        net.minecraft.util.valueproviders.ConstantInt.of(4), net.minecraft.util.valueproviders.ConstantInt.of(0),
                        net.minecraft.util.valueproviders.ConstantInt.of(5), 0.25f, 0.5f, 0.16666667f, 0.33333334f),
                new net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize(1, 0, 2)
                //? if >=26.2 {
                /*, net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration.defaultPlaceBelowTreeTrunkProvider(ctx.lookup(Registries.BIOME))
                *///?}
                ).ignoreVines().build();
        net.minecraft.data.worldgen.features.FeatureUtils.register(ctx, AVBuilding.STARWOOD_TREE,
                net.minecraft.world.level.levelgen.feature.Feature.TREE, tree);
    }

    static void placedFeatures(BootstrapContext<net.minecraft.world.level.levelgen.placement.PlacedFeature> ctx) {
        var tree = ctx.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(AVBuilding.STARWOOD_TREE);
        net.minecraft.data.worldgen.placement.PlacementUtils.register(ctx, STARWOOD_TREES, tree, List.of(
                net.minecraft.world.level.levelgen.placement.CountPlacement.of(net.minecraft.util.valueproviders.UniformInt.of(0, 2)),
                net.minecraft.world.level.levelgen.placement.InSquarePlacement.spread(),
                net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter.forMaxDepth(0),
                net.minecraft.data.worldgen.placement.PlacementUtils.HEIGHTMAP_OCEAN_FLOOR,
                net.minecraft.world.level.levelgen.placement.BiomeFilter.biome(),
                net.minecraft.world.level.levelgen.placement.BlockPredicateFilter.forPredicate(
                        net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate.wouldSurvive(
                                AVBuilding.STARWOOD_SAPLING.get().defaultBlockState(), net.minecraft.core.BlockPos.ZERO))));
    }

    /** One biome per realm: no rain, no mobs; the wilds grow starwood groves and meadow flowers. */
    static void biomes(BootstrapContext<Biome> ctx) {
        for (Realm realm : Realm.values()) {
            Sky s = sky(realm);
            //? if >=26.1 {
            var effects = new net.minecraft.world.level.biome.BiomeSpecialEffects.Builder().waterColor(s.water()).build();
            //?} else {
            /*var effects = new net.minecraft.world.level.biome.BiomeSpecialEffects.Builder().waterColor(s.water()).waterFogColor(s.fog())
                    .fogColor(s.fog()).skyColor(s.sky())
                    .ambientParticle(new net.minecraft.world.level.biome.AmbientParticleSettings(s.particle(), s.particleChance())).build();
            *///?}
            var generation = net.minecraft.world.level.biome.BiomeGenerationSettings.EMPTY;
            if (realm == Realm.FIRMAMENT) {
                var gen = new net.minecraft.world.level.biome.BiomeGenerationSettings.Builder(ctx.lookup(Registries.PLACED_FEATURE),
                        ctx.lookup(Registries.CONFIGURED_CARVER));
                gen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, STARWOOD_TREES);
                net.minecraft.data.worldgen.BiomeDefaultFeatures.addMeadowVegetation(gen);
                generation = gen.build();
            }
            ctx.register(biome(realm), new Biome.BiomeBuilder()
                    .hasPrecipitation(false)
                    .temperature(0.6f)
                    .downfall(0.0f)
                    .specialEffects(effects)
                    .mobSpawnSettings(new net.minecraft.world.level.biome.MobSpawnSettings.Builder().build())
                    .generationSettings(generation)
                    .build());
        }
    }

    /** Build ceiling of the wilds (the Sun Spire's realm needs room above its pad at y 80 as well). */
    public static final int FIRMAMENT_TOP = 448;

    static void dimensionTypes(BootstrapContext<net.minecraft.world.level.dimension.DimensionType> ctx) {
        for (Realm realm : Realm.values()) {
            Sky s = sky(realm);
            int minY = realm == Realm.FIRMAMENT ? -64 : 0;
            int height = FIRMAMENT_TOP - minY;
            //? if >=26.1 {
            var attributes = net.minecraft.world.attribute.EnvironmentAttributeMap.builder()
                    .set(net.minecraft.world.attribute.EnvironmentAttributes.SKY_COLOR, 0xFF000000 | s.sky())
                    .set(net.minecraft.world.attribute.EnvironmentAttributes.FOG_COLOR, 0xFF000000 | s.fog())
                    .set(net.minecraft.world.attribute.EnvironmentAttributes.SUN_ANGLE, s.sunAngle())
                    .set(net.minecraft.world.attribute.EnvironmentAttributes.MOON_ANGLE, (s.sunAngle() + 180f) % 360f)
                    .set(net.minecraft.world.attribute.EnvironmentAttributes.STAR_ANGLE, s.sunAngle())
                    .set(net.minecraft.world.attribute.EnvironmentAttributes.STAR_BRIGHTNESS, s.stars())
                    .set(net.minecraft.world.attribute.EnvironmentAttributes.MOON_PHASE, net.minecraft.world.level.MoonPhase.values()[s.moonPhase()])
                    .set(net.minecraft.world.attribute.EnvironmentAttributes.SUNRISE_SUNSET_COLOR, s.sunrise())
                    .set(net.minecraft.world.attribute.EnvironmentAttributes.SKY_LIGHT_LEVEL, s.skyLight())
                    .set(net.minecraft.world.attribute.EnvironmentAttributes.SKY_LIGHT_FACTOR, s.skyFactor())
                    .set(net.minecraft.world.attribute.EnvironmentAttributes.CLOUD_COLOR, realm == Realm.FIRMAMENT || realm == Realm.SUNSPIRE
                            ? 0xCCFFFFFF : 0x00FFFFFF)
                    .set(net.minecraft.world.attribute.EnvironmentAttributes.CLOUD_HEIGHT, realm == Realm.SUNSPIRE ? 40f : 120f)
                    .set(net.minecraft.world.attribute.EnvironmentAttributes.AMBIENT_PARTICLES,
                            List.of(new net.minecraft.world.attribute.AmbientParticle(s.particle(), s.particleChance())))
                    .set(net.minecraft.world.attribute.EnvironmentAttributes.BED_RULE, net.minecraft.world.attribute.BedRule.CAN_SLEEP_WHEN_DARK)
                    .set(net.minecraft.world.attribute.EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, false)
                    .build();
            ctx.register(type(realm), new net.minecraft.world.level.dimension.DimensionType(true, true, false, false, 1.0, minY, height, height,
                    //? if >=26.2 {
                    /*ctx.lookup(Registries.BLOCK).getOrThrow(net.minecraft.tags.BlockTags.INFINIBURN_OVERWORLD),
                    *///?} else {
                    net.minecraft.tags.BlockTags.INFINIBURN_OVERWORLD,
                    //?}
                    s.ambient(),
                    new net.minecraft.world.level.dimension.DimensionType.MonsterSettings(net.minecraft.util.valueproviders.ConstantInt.of(0), 0),
                    s.end() ? net.minecraft.world.level.dimension.DimensionType.Skybox.END : net.minecraft.world.level.dimension.DimensionType.Skybox.OVERWORLD,
                    net.minecraft.world.level.CardinalLighting.Type.DEFAULT, attributes, HolderSet.empty(), Optional.empty()));
            //?} else {
            /*ctx.register(type(realm), new net.minecraft.world.level.dimension.DimensionType(java.util.OptionalLong.of(s.time121()), true, false, false,
                    false, 1.0, true, false, minY, height, height, net.minecraft.tags.BlockTags.INFINIBURN_OVERWORLD,
                    s.end() ? BuiltinDimensionTypes.END_EFFECTS : BuiltinDimensionTypes.OVERWORLD_EFFECTS, s.ambient(),
                    new net.minecraft.world.level.dimension.DimensionType.MonsterSettings(false, false,
                            net.minecraft.util.valueproviders.ConstantInt.of(0), 0)));
            *///?}
        }
    }

    /** The wilds keep vanilla's floating-islands terrain; every trial realm is an empty void around its template. */
    static void dimensions(BootstrapContext<LevelStem> ctx) {
        for (Realm realm : Realm.values()) {
            var type = ctx.lookup(Registries.DIMENSION_TYPE).getOrThrow(type(realm));
            var biome = ctx.lookup(Registries.BIOME).getOrThrow(biome(realm));
            if (realm == Realm.FIRMAMENT) {
                ctx.register(stem(realm), new LevelStem(type, new NoiseBasedChunkGenerator(new FixedBiomeSource(biome),
                        ctx.lookup(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.FLOATING_ISLANDS))));
            } else {
                ctx.register(stem(realm), new LevelStem(type, new net.minecraft.world.level.levelgen.FlatLevelSource(
                        new net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings(Optional.empty(), biome, List.of()))));
            }
        }
    }

    static void sets(BootstrapContext<StructureSet> ctx) {
        ctx.register(OBSERVATORY_SET, new StructureSet(
                ctx.lookup(Registries.STRUCTURE).getOrThrow(FALLEN_OBSERVATORY),
                new RandomSpreadStructurePlacement(42, 14, RandomSpreadType.LINEAR, 1730517123)));
    }
}
