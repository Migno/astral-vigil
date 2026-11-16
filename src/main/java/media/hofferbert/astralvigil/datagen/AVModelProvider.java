package media.hofferbert.astralvigil.datagen;

import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.registry.AVBlocks;
import media.hofferbert.astralvigil.registry.AVItems;
import net.minecraft.data.PackOutput;
//? if >=26.1 {
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
//?} else {
/*import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
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

        itemModels.generateFlatItem(AVItems.ASTRAL_SHARD.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(AVItems.ASTROLABE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(AVItems.STAR_CHART.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(AVItems.CODEX_CAELESTIS.get(), ModelTemplates.FLAT_ITEM);
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
