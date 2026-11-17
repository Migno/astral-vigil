package media.hofferbert.astralvigil.worldgen;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import media.hofferbert.astralvigil.AstralVigil;
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

    public static final ResourceKey<LevelStem> FIRMAMENT_STEM =
            ResourceKey.create(Registries.LEVEL_STEM, AstralVigil.id("firmament"));

    public static final ResourceKey<net.minecraft.world.level.dimension.DimensionType> FIRMAMENT_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE, AstralVigil.id("firmament"));
    public static final ResourceKey<net.minecraft.world.level.biome.Biome> FIRMAMENT_BIOME =
            ResourceKey.create(Registries.BIOME, AstralVigil.id("firmament"));

    /**
     * The Firmament's biome: vanilla meadow (flowers, grass, colours) but without precipitation - above the clouds it
     * never rains, and at this altitude the meadow would otherwise snow onto every trial island.
     */
    static void biomes(BootstrapContext<net.minecraft.world.level.biome.Biome> ctx) {
        var meadow = net.minecraft.data.worldgen.biome.OverworldBiomes.meadowOrCherryGrove(
                ctx.lookup(Registries.PLACED_FEATURE), ctx.lookup(Registries.CONFIGURED_CARVER), false);
        var builder = new net.minecraft.world.level.biome.Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.5f)
                .downfall(0.8f)
                .specialEffects(meadow.getSpecialEffects())
                .mobSpawnSettings(meadow.getMobSettings())
                .generationSettings(meadow.getGenerationSettings());
        //? if >=26.1 {
        builder.putAttributes(meadow.getAttributes());
        //?}
        ctx.register(FIRMAMENT_BIOME, builder.build());
    }

    /** Build ceiling of the Firmament (the overworld's 320 is too low for the Sun Spire above the hub at y 290). */
    public static final int FIRMAMENT_TOP = 448;

    /** The overworld dimension type with a higher build limit (y -64 .. 447); everything else unchanged. */
    static void dimensionTypes(BootstrapContext<net.minecraft.world.level.dimension.DimensionType> ctx) {
        // vanilla registry values are unbound during datagen: run vanilla's own bootstrap and keep its overworld entry
        net.minecraft.world.level.dimension.DimensionType[] captured = new net.minecraft.world.level.dimension.DimensionType[1];
        net.minecraft.data.worldgen.DimensionTypes.bootstrap(new BootstrapContext<>() {
            @Override
            public net.minecraft.core.Holder.Reference<net.minecraft.world.level.dimension.DimensionType> register(
                    ResourceKey<net.minecraft.world.level.dimension.DimensionType> key, net.minecraft.world.level.dimension.DimensionType value,
                    com.mojang.serialization.Lifecycle lifecycle) {
                if (key == BuiltinDimensionTypes.OVERWORLD) {
                    captured[0] = value;
                }
                return null;
            }

            @Override
            public <S> net.minecraft.core.HolderGetter<S> lookup(ResourceKey<? extends net.minecraft.core.Registry<? extends S>> key) {
                return ctx.lookup(key);
            }
        });
        var o = captured[0];
        int height = FIRMAMENT_TOP - o.minY();
        //? if >=26.1 {
        ctx.register(FIRMAMENT_TYPE, new net.minecraft.world.level.dimension.DimensionType(o.hasFixedTime(), o.hasSkyLight(), o.hasCeiling(),
                o.hasEnderDragonFight(), o.coordinateScale(), o.minY(), height, height, o.infiniburn(), o.ambientLight(), o.monsterSettings(),
                o.skybox(), o.cardinalLightType(), o.attributes(), o.timelines(), o.defaultClock()));
        //?} else {
        /*ctx.register(FIRMAMENT_TYPE, new net.minecraft.world.level.dimension.DimensionType(o.fixedTime(), o.hasSkyLight(), o.hasCeiling(),
                o.ultraWarm(), o.natural(), o.coordinateScale(), o.bedWorks(), o.respawnAnchorWorks(), o.minY(), height, height,
                o.infiniburn(), o.effectsLocation(), o.ambientLight(), o.monsterSettings()));
        *///?}
    }

    /**
     * The Firmament: vanilla floating-islands terrain under the overworld sky (stars at night), a single meadow biome,
     * and its own dimension type (overworld copy with a build ceiling at {@link #FIRMAMENT_TOP}).
     */
    static void dimensions(BootstrapContext<LevelStem> ctx) {
        ctx.register(FIRMAMENT_STEM, new LevelStem(
                ctx.lookup(Registries.DIMENSION_TYPE).getOrThrow(FIRMAMENT_TYPE),
                new NoiseBasedChunkGenerator(
                        new FixedBiomeSource(ctx.lookup(Registries.BIOME).getOrThrow(FIRMAMENT_BIOME)),
                        ctx.lookup(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.FLOATING_ISLANDS))));
    }

    static void sets(BootstrapContext<StructureSet> ctx) {
        ctx.register(OBSERVATORY_SET, new StructureSet(
                ctx.lookup(Registries.STRUCTURE).getOrThrow(FALLEN_OBSERVATORY),
                new RandomSpreadStructurePlacement(42, 14, RandomSpreadType.LINEAR, 1730517123)));
    }
}
