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

        // Device models are hand-built in Blockbench (art/bbmodel/block); only the block states are generated.
        facing(blockModels, AVBlocks.LENS.get(), BlockModelGenerators.ROTATION_FACING);
        facing(blockModels, AVBlocks.MIRROR.get(), BlockModelGenerators.ROTATION_FACING);
        facing(blockModels, AVBlocks.STARLIGHT_COLLECTOR.get(), BlockModelGenerators.ROTATION_HORIZONTAL_FACING);
        facing(blockModels, AVBlocks.PRISM.get(), BlockModelGenerators.ROTATION_HORIZONTAL_FACING);
        blockModels.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(
                AVBlocks.STARLIGHT_RESERVOIR.get(),
                BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(AVBlocks.STARLIGHT_RESERVOIR.get()))));

        itemModels.generateFlatItem(AVItems.ASTRAL_SHARD.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(AVItems.ASTROLABE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(AVItems.STAR_CHART.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(AVItems.CODEX_CAELESTIS.get(), ModelTemplates.FLAT_ITEM);
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

            // Device models are hand-built in Blockbench; only block states + item parents are generated.
            facingAll(AVBlocks.LENS.get());
            facingAll(AVBlocks.MIRROR.get());
            horizontalBlock(AVBlocks.STARLIGHT_COLLECTOR.get(), existing(AVBlocks.STARLIGHT_COLLECTOR.get()));
            horizontalBlock(AVBlocks.PRISM.get(), existing(AVBlocks.PRISM.get()));
            simpleBlock(AVBlocks.STARLIGHT_RESERVOIR.get(), existing(AVBlocks.STARLIGHT_RESERVOIR.get()));
            for (Block b : new Block[] {AVBlocks.LENS.get(), AVBlocks.MIRROR.get(), AVBlocks.STARLIGHT_COLLECTOR.get(), AVBlocks.PRISM.get(), AVBlocks.STARLIGHT_RESERVOIR.get()}) {
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
        }
    }
}
*///?}
