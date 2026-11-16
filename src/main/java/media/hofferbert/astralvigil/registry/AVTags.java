package media.hofferbert.astralvigil.registry;

import media.hofferbert.astralvigil.AstralVigil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class AVTags {
    private AVTags() {}

    public static final class Blocks {
        /** Blocks a falling star never destroys (in addition to unbreakable blocks and block entities). */
        public static final TagKey<Block> STARFALL_IMMUNE = TagKey.create(Registries.BLOCK, AstralVigil.id("starfall_immune"));
        /** Blocks starlight beams pass through (glass etc.). */
        public static final TagKey<Block> BEAM_TRANSPARENT = TagKey.create(Registries.BLOCK, AstralVigil.id("beam_transparent"));

        private Blocks() {}
    }

    public static final class Items {
        public static final TagKey<Item> ASTRAL_SHARDS = TagKey.create(Registries.ITEM, AstralVigil.id("astral_shards"));
        /** Convention tag so packmakers/KubeJS can swap in other gems. */
        /** Paper accepted by the Star Chart recipe. */
        public static final TagKey<Item> CHART_PAPER = TagKey.create(Registries.ITEM, AstralVigil.id("chart_paper"));
        /** Books accepted by the Codex Caelestis recipe. */
        public static final TagKey<Item> CODEX_BOOKS = TagKey.create(Registries.ITEM, AstralVigil.id("codex_books"));

        public static final TagKey<Item> METEORIC_STONES = TagKey.create(Registries.ITEM, AstralVigil.id("meteoric_stones"));

        public static final TagKey<Item> GEMS_ASTRAL = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "gems/astral"));

        private Items() {}
    }
}
