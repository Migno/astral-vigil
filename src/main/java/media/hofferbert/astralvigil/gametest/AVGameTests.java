package media.hofferbert.astralvigil.gametest;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.compat.Celestial;
import media.hofferbert.astralvigil.config.AVServerConfig;
import media.hofferbert.astralvigil.registry.AVAttachments;
import media.hofferbert.astralvigil.research.Alignment;
import media.hofferbert.astralvigil.research.AlignmentTags;
import media.hofferbert.astralvigil.research.ResearchData;
import media.hofferbert.astralvigil.research.ResearchManager;
import net.minecraft.server.level.ServerPlayer;
import media.hofferbert.astralvigil.registry.AVBlocks;
import media.hofferbert.astralvigil.registry.AVItems;
import media.hofferbert.astralvigil.starfall.StarfallCrater;
import media.hofferbert.astralvigil.starfall.StarfallManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;

/**
 * Version-independent catalogue of all Astral Vigil GameTests.
 * Registration differs per Minecraft version, see {@link GameTestRegistration}.
 */
public final class AVGameTests {
    /** 5x4x5 smooth-stone platform with open sky. */
    public static final String PLATFORM = "gametest/platform";
    /** 13x9x13 field, 4 layers of dirt (surface at relative y=4). */
    public static final String CRATER_FIELD = "gametest/crater_field";

    public record Def(String name, String structure, int maxTicks, boolean skyAccess, Consumer<GameTestHelper> body) {}

    public static final List<Def> ALL = List.of(
            new Def("mod_loaded", PLATFORM, 40, false, AVGameTests::modLoaded),
            new Def("fallen_star_drops_shards", PLATFORM, 40, false, AVGameTests::fallenStarDropsShards),
            new Def("no_starlight_under_roof", PLATFORM, 40, false, AVGameTests::noStarlightUnderRoof),
            new Def("starfall_crater", CRATER_FIELD, 40, true, AVGameTests::starfallCrater),
            new Def("starfall_lands", CRATER_FIELD, StarfallManager.FALL_TICKS + 60, true, AVGameTests::starfallLands),
            new Def("alignment_tags_loaded", PLATFORM, 40, false, AVGameTests::alignmentTagsLoaded),
            new Def("scan_block_grants_knowledge", PLATFORM, 40, false, AVGameTests::scanBlockGrantsKnowledge),
            new Def("constellation_needs_attunement", PLATFORM, 40, false, AVGameTests::constellationNeedsAttunement));

    private AVGameTests() {}

    static void alignmentTagsLoaded(GameTestHelper helper) {
        GT.check(helper, Blocks.GLOWSTONE.defaultBlockState().is(AlignmentTags.blocks(Alignment.SOL)), "glowstone not Sol");
        GT.check(helper, Blocks.SCULK.defaultBlockState().is(AlignmentTags.blocks(Alignment.VOID)), "sculk not Void");
        GT.check(
                helper,
                AVBlocks.FALLEN_STAR.get().defaultBlockState().is(AlignmentTags.blocks(Alignment.STELLA)),
                "fallen star not Stella");
        helper.succeed();
    }

    static void scanBlockGrantsKnowledge(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setData(AVAttachments.RESEARCH, ResearchData.EMPTY);
        BlockPos rel = GT.pos(2, 1, 2);
        helper.setBlock(rel, Blocks.GLOWSTONE);
        ResearchManager.scanBlock(player, helper.absolutePos(rel));
        ResearchData data = ResearchManager.get(player);
        int points = AVServerConfig.RESEARCH_POINTS_BLOCK.get();
        GT.check(helper, data.hasScanned("block/minecraft:glowstone"), "glowstone not recorded");
        GT.check(helper, data.knowledge(Alignment.SOL) == points, "expected " + points + " Sol, got " + data.knowledge(Alignment.SOL));
        ResearchManager.scanBlock(player, helper.absolutePos(rel)); // rescan gives nothing
        GT.check(helper, ResearchManager.get(player).knowledge(Alignment.SOL) == points, "rescan granted knowledge");
        helper.succeed();
    }

    static void constellationNeedsAttunement(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setData(AVAttachments.RESEARCH, ResearchData.EMPTY);
        ResearchManager.solveConstellation(player, "halo");
        GT.check(helper, !ResearchManager.get(player).hasConstellation("halo"), "halo learned without attunement");
        int threshold = AVServerConfig.RESEARCH_ATTUNEMENT.get();
        player.setData(AVAttachments.RESEARCH, ResearchData.EMPTY.withScan("test/sol", Set.of(Alignment.SOL), threshold));
        ResearchManager.solveConstellation(player, "halo");
        GT.check(helper, ResearchManager.get(player).hasConstellation("halo"), "halo not learned after attunement");
        helper.succeed();
    }

    static void modLoaded(GameTestHelper helper) {
        GT.check(helper, ModList.get().isLoaded(AstralVigil.MOD_ID), "Astral Vigil is not loaded");
        helper.assertBlockPresent(Blocks.SMOOTH_STONE, GT.pos(2, 0, 2));
        helper.succeed();
    }

    static void fallenStarDropsShards(GameTestHelper helper) {
        BlockPos rel = GT.pos(2, 1, 2);
        helper.setBlock(rel, AVBlocks.FALLEN_STAR.get());
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(rel);
        List<ItemStack> drops = Block.getDrops(
                level.getBlockState(abs), level, abs, null, null, new ItemStack(Items.IRON_PICKAXE));
        int shards = drops.stream()
                .filter(s -> s.is(AVItems.ASTRAL_SHARD.get()))
                .mapToInt(ItemStack::getCount)
                .sum();
        GT.check(helper, shards >= 2 && shards <= 4, "expected 2-4 astral shards, got " + shards + " " + drops);
        helper.succeed();
    }

    static void noStarlightUnderRoof(GameTestHelper helper) {
        helper.setBlock(GT.pos(2, 3, 2), Blocks.STONE);
        double starlight = Celestial.starlight(helper.getLevel(), helper.absolutePos(GT.pos(2, 1, 2)));
        GT.check(helper, starlight == 0.0, "starlight under a roof must be 0, was " + starlight);
        helper.succeed();
    }

    static void starfallCrater(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos surface = helper.absolutePos(GT.pos(6, 4, 6));
        BlockPos star = StarfallCrater.impact(level, surface, 3, true, level.getRandom());
        GT.check(helper, level.getBlockState(star).is(AVBlocks.FALLEN_STAR.get()), "no fallen star at " + star);
        GT.check(helper, star.getY() < surface.getY(), "fallen star should lie inside the crater");
        GT.check(helper, level.getBlockState(surface.below()).isAir() || star.equals(surface.below()), "crater not dug");
        GT.check(helper, countInField(helper, AVBlocks.METEORIC_STONE.get()) > 0, "no meteoric stone lining");
        helper.succeed();
    }

    static void starfallLands(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        StarfallManager.start(level, helper.absolutePos(GT.pos(6, 4, 6)), level.getRandom());
        helper.succeedWhen(() -> GT.check(
                helper, countInField(helper, AVBlocks.FALLEN_STAR.get()) == 1, "fallen star has not landed yet"));
    }

    private static int countInField(GameTestHelper helper, Block block) {
        int count = 0;
        for (BlockPos p : BlockPos.betweenClosed(helper.absolutePos(GT.pos(0, 0, 0)), helper.absolutePos(GT.pos(12, 8, 12)))) {
            if (helper.getLevel().getBlockState(p).is(block)) {
                count++;
            }
        }
        return count;
    }
}
