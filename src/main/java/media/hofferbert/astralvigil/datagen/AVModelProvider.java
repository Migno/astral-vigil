package media.hofferbert.astralvigil.datagen;

import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.registry.AVBlocks;
import media.hofferbert.astralvigil.registry.AVItems;
import net.minecraft.data.PackOutput;
//? if >=26.1 {
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.world.level.block.Block;
//?} else {
/*import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
*///?}

/** Block states, block models and item models. */
//? if >=26.1 {
public class AVModelProvider extends ModelProvider {
    public AVModelProvider(PackOutput output) {
        super(output, AstralVigil.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        blockModels.createTrivialCube(AVBlocks.FALLEN_STAR.get());
        blockModels.createTrivialCube(AVBlocks.METEORIC_STONE.get());
        blockModels.createTrivialCube(AVBlocks.CONSTELLATION_LOCK.get());
        blockModels.createTrivialCube(AVBlocks.VOID_CRYSTAL.get());
        for (Block b : new Block[] {AVBlocks.FIRMAMENT_STONE.get(), AVBlocks.STARLIGHT_FONT.get(), AVBlocks.TRIAL_RECEPTOR.get(),
            AVBlocks.TRIAL_GOAL.get(), AVBlocks.MOON_TARGET.get(), AVBlocks.ECLIPSE_SEAL.get()}) {
            blockModels.createTrivialCube(b);
        }
        AVBlocks.GATES.values().forEach(g -> blockModels.createTrivialCube(g.get()));

        // Gate portals: hand-made pane models (vanilla nether portal shape), one per axis
        AVBlocks.PORTALS.forEach((kind, portal) -> blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(portal.get())
                .with(PropertyDispatch.initial(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_AXIS)
                        .select(net.minecraft.core.Direction.Axis.X, BlockModelGenerators.plainVariant(media.hofferbert.astralvigil.AstralVigil.id("block/" + kind.portalId() + "_ns")))
                        .select(net.minecraft.core.Direction.Axis.Z, BlockModelGenerators.plainVariant(media.hofferbert.astralvigil.AstralVigil.id("block/" + kind.portalId() + "_ew"))))));

        // Device models are hand-built in Blockbench (art/bbmodel/block); only the block states are generated.
        facing(blockModels, AVBlocks.LENS.get(), BlockModelGenerators.ROTATION_FACING);
        facing(blockModels, AVBlocks.MIRROR.get(), BlockModelGenerators.ROTATION_FACING);
        facing(blockModels, AVBlocks.STARLIGHT_COLLECTOR.get(), BlockModelGenerators.ROTATION_HORIZONTAL_FACING);
        facing(blockModels, AVBlocks.PRISM.get(), BlockModelGenerators.ROTATION_HORIZONTAL_FACING);
        for (Block b : new Block[] {
            AVBlocks.STARLIGHT_RESERVOIR.get(), AVBlocks.PEDESTAL.get(), AVBlocks.CONVERGENCE_ALTAR.get(), AVBlocks.ASTRAL_FORGE.get(),
            AVBlocks.STAR_ANCHOR.get()
        }) {
            blockModels.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(
                    b, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(b))));
        }

        itemModels.generateFlatItem(AVItems.ASTRAL_SHARD.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(AVItems.ASTROLABE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(AVItems.STAR_CHART.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(AVItems.CODEX_CAELESTIS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(AVItems.STARMETAL_INGOT.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(AVItems.SUNSTEEL_INGOT.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(AVItems.MOONSILVER_INGOT.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(AVItems.ASTRAL_CORE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(AVItems.SIGIL_BLANK.get(), ModelTemplates.FLAT_ITEM);

        // Phase 5 – gear
        itemModels.generateFlatItem(AVItems.HALO_EDGE.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(AVItems.DAWNBREAKER.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateBow(AVItems.CRESCENT.get());
        itemModels.generateCrossbow(AVItems.TIDECALLER.get());
        // generateBow/generateCrossbow only create the frames; the standby model is hand-made in vanilla
        ModelTemplates.BOW.create(ModelLocationUtils.getModelLocation(AVItems.CRESCENT.get()),
                TextureMapping.layer0(AVItems.CRESCENT.get()), itemModels.modelOutput);
        ModelTemplates.CROSSBOW.create(ModelLocationUtils.getModelLocation(AVItems.TIDECALLER.get()),
                TextureMapping.layer0(AVItems.TIDECALLER.get()), itemModels.modelOutput);
        itemModels.generateFlatItem(AVItems.STARLIGHT_AEGIS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(AVItems.SERAPH_HELMET.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(AVItems.SERAPH_CHESTPLATE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(AVItems.SERAPH_LEGGINGS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(AVItems.SERAPH_BOOTS.get(), ModelTemplates.FLAT_ITEM);
        for (var set : new media.hofferbert.astralvigil.armor.ArmorSet[] {
                media.hofferbert.astralvigil.armor.ArmorSet.SOLAR, media.hofferbert.astralvigil.armor.ArmorSet.LUNAR}) {
            media.hofferbert.astralvigil.armor.ArmorSets.pieces(set).forEach(i -> itemModels.generateFlatItem(i.get(), ModelTemplates.FLAT_ITEM));
        }
        AVItems.SIGILS.values().forEach(s -> itemModels.generateFlatItem(s.get(), ModelTemplates.FLAT_ITEM));
        itemModels.generateFlatItem(AVItems.LORE_FRAGMENT.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(AVItems.VOID_SHARD.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(AVItems.ASCENSION_KEY.get(), ModelTemplates.FLAT_ITEM);
        AVItems.MARKS.values().forEach(m -> itemModels.generateFlatItem(m.get(), ModelTemplates.FLAT_ITEM));
        for (net.minecraft.world.item.Item i : new net.minecraft.world.item.Item[] {AVItems.UMBRAL_SIGIL.get(), AVItems.ECLIPSE_HEART.get(), AVItems.SERAPH_FEATHER.get(), AVItems.HALO_OF_DAWN.get()}) {
            itemModels.generateFlatItem(i, ModelTemplates.FLAT_ITEM);
        }
    }

    private static void facing(BlockModelGenerators blockModels, Block block, PropertyDispatch<VariantMutator> rotation) {
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(
                        block, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(block)))
                .with(rotation));
    }
}
//?} else {
/*public final class AVModelProvider {
    private AVModelProvider() {}

    public static class BlockStates extends BlockStateProvider {
        public BlockStates(PackOutput output, ExistingFileHelper efh) {
            super(output, AstralVigil.MOD_ID, efh);
        }

        @Override
        protected void registerStatesAndModels() {
            simpleBlockWithItem(AVBlocks.FALLEN_STAR.get(), cubeAll(AVBlocks.FALLEN_STAR.get()));
            simpleBlockWithItem(AVBlocks.METEORIC_STONE.get(), cubeAll(AVBlocks.METEORIC_STONE.get()));
            simpleBlockWithItem(AVBlocks.CONSTELLATION_LOCK.get(), cubeAll(AVBlocks.CONSTELLATION_LOCK.get()));
            simpleBlockWithItem(AVBlocks.VOID_CRYSTAL.get(), cubeAll(AVBlocks.VOID_CRYSTAL.get()));
            for (Block b : new Block[] {AVBlocks.FIRMAMENT_STONE.get(), AVBlocks.STARLIGHT_FONT.get(), AVBlocks.TRIAL_RECEPTOR.get(),
                AVBlocks.TRIAL_GOAL.get(), AVBlocks.MOON_TARGET.get(), AVBlocks.ECLIPSE_SEAL.get()}) {
                simpleBlockWithItem(b, cubeAll(b));
            }
            AVBlocks.GATES.values().forEach(g -> simpleBlockWithItem(g.get(), cubeAll(g.get())));

            AVBlocks.PORTALS.forEach((kind, portal) -> getVariantBuilder(portal.get())
                    .partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_AXIS, net.minecraft.core.Direction.Axis.X)
                    .modelForState().modelFile(models().getExistingFile(modLoc("block/" + kind.portalId() + "_ns"))).addModel()
                    .partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_AXIS, net.minecraft.core.Direction.Axis.Z)
                    .modelForState().modelFile(models().getExistingFile(modLoc("block/" + kind.portalId() + "_ew"))).addModel());

            // Device models are hand-built in Blockbench; only block states + item parents are generated.
            facingAll(AVBlocks.LENS.get());
            facingAll(AVBlocks.MIRROR.get());
            horizontalBlock(AVBlocks.STARLIGHT_COLLECTOR.get(), existing(AVBlocks.STARLIGHT_COLLECTOR.get()));
            horizontalBlock(AVBlocks.PRISM.get(), existing(AVBlocks.PRISM.get()));
            for (Block b : new Block[] {AVBlocks.STARLIGHT_RESERVOIR.get(), AVBlocks.PEDESTAL.get(), AVBlocks.CONVERGENCE_ALTAR.get(), AVBlocks.ASTRAL_FORGE.get(), AVBlocks.STAR_ANCHOR.get()}) {
                simpleBlock(b, existing(b));
            }
            for (Block b : new Block[] {AVBlocks.LENS.get(), AVBlocks.MIRROR.get(), AVBlocks.STARLIGHT_COLLECTOR.get(), AVBlocks.PRISM.get(), AVBlocks.STARLIGHT_RESERVOIR.get(), AVBlocks.PEDESTAL.get(), AVBlocks.CONVERGENCE_ALTAR.get(), AVBlocks.ASTRAL_FORGE.get(), AVBlocks.STAR_ANCHOR.get()}) {
                simpleBlockItem(b, existing(b));
            }
        }

        private ModelFile existing(Block block) {
            return models().getExistingFile(modLoc("block/" + BuiltInRegistries.BLOCK.getKey(block).getPath()));
        }

        // Same rotations as vanilla ROTATION_FACING: model front = north.
        private void facingAll(Block block) {
            ModelFile model = existing(block);
            getVariantBuilder(block).forAllStates(state -> {
                Direction d = state.getValue(BlockStateProperties.FACING);
                int x = d == Direction.DOWN ? 90 : d == Direction.UP ? 270 : 0;
                int y = switch (d) {
                    case SOUTH -> 180;
                    case WEST -> 270;
                    case EAST -> 90;
                    default -> 0;
                };
                return ConfiguredModel.builder().modelFile(model).rotationX(x).rotationY(y).build();
            });
        }
    }

    public static class ItemModels extends ItemModelProvider {
        public ItemModels(PackOutput output, ExistingFileHelper efh) {
            super(output, AstralVigil.MOD_ID, efh);
        }

        @Override
        protected void registerModels() {
            basicItem(AVItems.ASTRAL_SHARD.get());
            basicItem(AVItems.ASTROLABE.get());
            basicItem(AVItems.STAR_CHART.get());
            basicItem(AVItems.CODEX_CAELESTIS.get());
            basicItem(AVItems.STARMETAL_INGOT.get());
            basicItem(AVItems.SUNSTEEL_INGOT.get());
            basicItem(AVItems.MOONSILVER_INGOT.get());
            basicItem(AVItems.ASTRAL_CORE.get());
            basicItem(AVItems.SIGIL_BLANK.get());

            // Phase 5 – gear
            handheldItem(AVItems.HALO_EDGE.get());
            handheldItem(AVItems.DAWNBREAKER.get());
            bow(AVItems.CRESCENT.get());
            crossbow(AVItems.TIDECALLER.get());
            basicItem(AVItems.STARLIGHT_AEGIS.get());
            basicItem(AVItems.SERAPH_HELMET.get());
            basicItem(AVItems.SERAPH_CHESTPLATE.get());
            basicItem(AVItems.SERAPH_LEGGINGS.get());
            basicItem(AVItems.SERAPH_BOOTS.get());
            for (var set : new media.hofferbert.astralvigil.armor.ArmorSet[] {
                    media.hofferbert.astralvigil.armor.ArmorSet.SOLAR, media.hofferbert.astralvigil.armor.ArmorSet.LUNAR}) {
                media.hofferbert.astralvigil.armor.ArmorSets.pieces(set).forEach(i -> basicItem(i.get()));
            }
            AVItems.SIGILS.values().forEach(s -> basicItem(s.get()));
            basicItem(AVItems.LORE_FRAGMENT.get());
            basicItem(AVItems.VOID_SHARD.get());
            basicItem(AVItems.ASCENSION_KEY.get());
            AVItems.MARKS.values().forEach(m -> basicItem(m.get()));
            for (net.minecraft.world.item.Item i : new net.minecraft.world.item.Item[] {AVItems.UMBRAL_SIGIL.get(), AVItems.ECLIPSE_HEART.get(), AVItems.SERAPH_FEATHER.get(), AVItems.HALO_OF_DAWN.get()}) {
                basicItem(i);
            }
        }

        private String name(Item item) {
            return BuiltInRegistries.ITEM.getKey(item).getPath();
        }

        private ModelFile frame(String name, String parent) {
            return withExistingParent(name, mcLoc("item/" + parent)).texture("layer0", modLoc("item/" + name));
        }

        // Same override thresholds as the vanilla bow model.
        private void bow(Item item) {
            String n = name(item);
            ModelFile p0 = frame(n + "_pulling_0", "bow");
            ModelFile p1 = frame(n + "_pulling_1", "bow");
            ModelFile p2 = frame(n + "_pulling_2", "bow");
            withExistingParent(n, mcLoc("item/bow")).texture("layer0", modLoc("item/" + n))
                    .override().predicate(mcLoc("pulling"), 1).model(p0).end()
                    .override().predicate(mcLoc("pulling"), 1).predicate(mcLoc("pull"), 0.65f).model(p1).end()
                    .override().predicate(mcLoc("pulling"), 1).predicate(mcLoc("pull"), 0.9f).model(p2).end();
        }

        // Same override thresholds as the vanilla crossbow model.
        private void crossbow(Item item) {
            String n = name(item);
            ModelFile p0 = frame(n + "_pulling_0", "crossbow");
            ModelFile p1 = frame(n + "_pulling_1", "crossbow");
            ModelFile p2 = frame(n + "_pulling_2", "crossbow");
            ModelFile arrow = frame(n + "_arrow", "crossbow");
            ModelFile firework = frame(n + "_firework", "crossbow");
            withExistingParent(n, mcLoc("item/crossbow")).texture("layer0", modLoc("item/" + n))
                    .override().predicate(mcLoc("pulling"), 1).model(p0).end()
                    .override().predicate(mcLoc("pulling"), 1).predicate(mcLoc("pull"), 0.58f).model(p1).end()
                    .override().predicate(mcLoc("pulling"), 1).predicate(mcLoc("pull"), 1.0f).model(p2).end()
                    .override().predicate(mcLoc("charged"), 1).model(arrow).end()
                    .override().predicate(mcLoc("charged"), 1).predicate(mcLoc("firework"), 1).model(firework).end();
        }
    }
}
*///?}
