package media.hofferbert.astralvigil.datagen;

import java.util.concurrent.CompletableFuture;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.registry.AVBlocks;
import media.hofferbert.astralvigil.registry.AVItems;
import media.hofferbert.astralvigil.registry.AVTags;
import media.hofferbert.astralvigil.research.Alignment;
import media.hofferbert.astralvigil.research.AlignmentTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
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

    public static class BlockTagProvider extends BlockTagsProvider {
        //? if >=26.1 {
        public BlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
            super(output, lookup, AstralVigil.MOD_ID);
        }
        //?} else {
        /*public BlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper efh) {
            super(output, lookup, AstralVigil.MOD_ID, efh);
        }
        *///?}

        @Override
        protected void addTags(HolderLookup.Provider registries) {
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
                    AVBlocks.FALLEN_STAR.get(), AVBlocks.METEORIC_STONE.get(), AVBlocks.STARLIGHT_COLLECTOR.get(),
                    AVBlocks.LENS.get(), AVBlocks.MIRROR.get(), AVBlocks.PRISM.get(), AVBlocks.STARLIGHT_RESERVOIR.get(),
                    AVBlocks.PEDESTAL.get(), AVBlocks.CONVERGENCE_ALTAR.get(), AVBlocks.ASTRAL_FORGE.get(),
                    AVBlocks.STAR_ANCHOR.get(), AVBlocks.VOID_CRYSTAL.get(), AVBlocks.FIRMAMENT_STONE.get());
            tag(BlockTags.NEEDS_IRON_TOOL).add(AVBlocks.VOID_CRYSTAL.get());
            tag(AVTags.Blocks.BEAM_TRANSPARENT).addTag(Tags.Blocks.GLASS_BLOCKS).addTag(Tags.Blocks.GLASS_PANES);
            tag(BlockTags.NEEDS_IRON_TOOL).add(AVBlocks.FALLEN_STAR.get());
            tag(BlockTags.NEEDS_STONE_TOOL).add(AVBlocks.METEORIC_STONE.get());
            tag(AVTags.Blocks.STARFALL_IMMUNE).addTag(BlockTags.FEATURES_CANNOT_REPLACE).add(
                    AVBlocks.CONSTELLATION_LOCK.get(), AVBlocks.STARLIGHT_FONT.get(), AVBlocks.TRIAL_RECEPTOR.get(),
                    AVBlocks.TRIAL_GOAL.get(), AVBlocks.MOON_TARGET.get());

            // Celestial alignments (packmakers: extend via datapack/KubeJS)
            tag(AlignmentTags.blocks(Alignment.SOL))
                    .add(Blocks.GLOWSTONE, Blocks.SHROOMLIGHT, Blocks.SUNFLOWER, Blocks.GOLD_BLOCK, Blocks.RAW_GOLD_BLOCK,
                            Blocks.MAGMA_BLOCK, Blocks.HONEY_BLOCK, Blocks.HONEYCOMB_BLOCK, Blocks.TORCHFLOWER,
                            Blocks.CAMPFIRE, Blocks.SAND, Blocks.RED_SAND);
            tag(AlignmentTags.blocks(Alignment.LUNA))
                    .add(Blocks.SNOW_BLOCK, Blocks.SNOW, Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE,
                            Blocks.POWDER_SNOW, Blocks.SEA_LANTERN, Blocks.PRISMARINE, Blocks.LILY_PAD,
                            Blocks.SPORE_BLOSSOM, Blocks.WHITE_TULIP, Blocks.LILY_OF_THE_VALLEY);
            tag(AlignmentTags.blocks(Alignment.STELLA))
                    .add(Blocks.AMETHYST_BLOCK, Blocks.BUDDING_AMETHYST, Blocks.AMETHYST_CLUSTER,
                            Blocks.LARGE_AMETHYST_BUD, Blocks.END_STONE, Blocks.END_ROD, Blocks.BEACON,
                            Blocks.LODESTONE, Blocks.CALCITE, AVBlocks.FALLEN_STAR.get(), AVBlocks.METEORIC_STONE.get());
            tag(AlignmentTags.blocks(Alignment.VOID))
                    .add(Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.SCULK, Blocks.SCULK_CATALYST,
                            Blocks.SCULK_SHRIEKER, Blocks.SCULK_SENSOR, Blocks.REINFORCED_DEEPSLATE,
                            Blocks.END_PORTAL_FRAME, Blocks.RESPAWN_ANCHOR, Blocks.SOUL_SAND, Blocks.SOUL_SOIL,
                            AVBlocks.VOID_CRYSTAL.get());
        }
    }

    public static class ItemTagProvider extends ItemTagsProvider {
        //? if >=26.1 {
        public ItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
            super(output, lookup, AstralVigil.MOD_ID);
        }
        //?} else {
        /*public ItemTagProvider(
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
            tag(AVTags.Items.METEORIC_STONES).add(AVItems.METEORIC_STONE.get());
            tag(AVTags.Items.INGOTS_STARMETAL).add(AVItems.STARMETAL_INGOT.get());
            tag(AVTags.Items.INGOTS_SUNSTEEL).add(AVItems.SUNSTEEL_INGOT.get());
            tag(AVTags.Items.INGOTS_MOONSILVER).add(AVItems.MOONSILVER_INGOT.get());
            tag(Tags.Items.INGOTS)
                    .addTag(AVTags.Items.INGOTS_STARMETAL)
                    .addTag(AVTags.Items.INGOTS_SUNSTEEL)
                    .addTag(AVTags.Items.INGOTS_MOONSILVER);
            tag(ItemTags.SWORDS).add(AVItems.HALO_EDGE.get());
            tag(ItemTags.AXES).add(AVItems.DAWNBREAKER.get());
            tag(ItemTags.BOW_ENCHANTABLE).add(AVItems.CRESCENT.get());
            tag(ItemTags.CROSSBOW_ENCHANTABLE).add(AVItems.TIDECALLER.get());
            tag(ItemTags.DURABILITY_ENCHANTABLE).add(
                    AVItems.HALO_EDGE.get(), AVItems.DAWNBREAKER.get(), AVItems.CRESCENT.get(), AVItems.TIDECALLER.get(),
                    AVItems.STARLIGHT_AEGIS.get());
            tag(ItemTags.HEAD_ARMOR).add(AVItems.SERAPH_HELMET.get());
            tag(ItemTags.CHEST_ARMOR).add(AVItems.SERAPH_CHESTPLATE.get());
            tag(ItemTags.LEG_ARMOR).add(AVItems.SERAPH_LEGGINGS.get());
            tag(ItemTags.FOOT_ARMOR).add(AVItems.SERAPH_BOOTS.get());
            tag(Tags.Items.TOOLS_SHIELD).add(AVItems.STARLIGHT_AEGIS.get());
            tag(Tags.Items.TOOLS_BOW).add(AVItems.CRESCENT.get());
            tag(Tags.Items.TOOLS_CROSSBOW).add(AVItems.TIDECALLER.get());
            tag(AVTags.Items.CHART_PAPER).add(net.minecraft.world.item.Items.PAPER);
            tag(AVTags.Items.CODEX_BOOKS).add(net.minecraft.world.item.Items.BOOK);
        }
    }
}
