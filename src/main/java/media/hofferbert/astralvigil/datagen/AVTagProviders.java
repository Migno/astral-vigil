package media.hofferbert.astralvigil.datagen;

import java.util.concurrent.CompletableFuture;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.registry.AVBlocks;
import media.hofferbert.astralvigil.registry.AVItems;
import media.hofferbert.astralvigil.registry.AVTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
//? if >=26.1 {
import net.neoforged.neoforge.common.data.ItemTagsProvider;
//?} else {
/*import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
*///?}

/** Block + item tags. Recipe inputs are always tags so packmakers can retarget them. */
public final class AVTagProviders {
    private AVTagProviders() {}

    public static class Blocks extends BlockTagsProvider {
        //? if >=26.1 {
        public Blocks(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
            super(output, lookup, AstralVigil.MOD_ID);
        }
        //?} else {
        /*public Blocks(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper efh) {
            super(output, lookup, AstralVigil.MOD_ID, efh);
        }
        *///?}

        @Override
        protected void addTags(HolderLookup.Provider registries) {
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(AVBlocks.FALLEN_STAR.get(), AVBlocks.METEORIC_STONE.get());
            tag(BlockTags.NEEDS_IRON_TOOL).add(AVBlocks.FALLEN_STAR.get());
            tag(BlockTags.NEEDS_STONE_TOOL).add(AVBlocks.METEORIC_STONE.get());
            tag(AVTags.Blocks.STARFALL_IMMUNE).addTag(BlockTags.FEATURES_CANNOT_REPLACE);
        }
    }

    public static class Items extends ItemTagsProvider {
        //? if >=26.1 {
        public Items(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
            super(output, lookup, AstralVigil.MOD_ID);
        }
        //?} else {
        /*public Items(
                PackOutput output,
                CompletableFuture<HolderLookup.Provider> lookup,
                CompletableFuture<TagsProvider.TagLookup<Block>> blockTags,
                ExistingFileHelper efh) {
            super(output, lookup, blockTags, AstralVigil.MOD_ID, efh);
        }
        *///?}

        @Override
        protected void addTags(HolderLookup.Provider registries) {
            tag(AVTags.Items.ASTRAL_SHARDS).add(AVItems.ASTRAL_SHARD.get());
            tag(AVTags.Items.GEMS_ASTRAL).add(AVItems.ASTRAL_SHARD.get());
            tag(Tags.Items.GEMS).addTag(AVTags.Items.GEMS_ASTRAL);
        }
    }
}
