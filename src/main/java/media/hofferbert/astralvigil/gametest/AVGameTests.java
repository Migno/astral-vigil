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
import media.hofferbert.astralvigil.crafting.AstralForgeBlockEntity;
import media.hofferbert.astralvigil.crafting.ConvergenceAltarBlockEntity;
import media.hofferbert.astralvigil.crafting.PedestalBlockEntity;
import media.hofferbert.astralvigil.gear.GearData;
import media.hofferbert.astralvigil.gear.GearManager;
import media.hofferbert.astralvigil.gear.Sigil;
import media.hofferbert.astralvigil.starlight.Tint;
import net.minecraft.world.item.Item;
import media.hofferbert.astralvigil.starlight.network.Devices;
import media.hofferbert.astralvigil.starlight.network.StarlightBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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
            new Def("firmament_templates", PLATFORM, 40, false, AVGameTests::firmamentTemplates),
            new Def("trial_grants_mark_and_favour", PLATFORM, 40, false, AVGameTests::trialGrantsMarkAndFavour),
            new Def("eclipse_phase_transition", PLATFORM, 60, true, AVGameTests::eclipsePhaseTransition),
            new Def("eclipse_drops_heart", PLATFORM, 40, true, AVGameTests::eclipseDropsHeart),
            new Def("echo_expires", PLATFORM, 60, true, AVGameTests::echoExpires),
            new Def("eclipse_awards_advancement", PLATFORM, 40, false, AVGameTests::eclipseAwardsAdvancement),
            new Def("fallen_star_drops_shards", PLATFORM, 40, false, AVGameTests::fallenStarDropsShards),
            new Def("no_starlight_under_roof", PLATFORM, 40, false, AVGameTests::noStarlightUnderRoof),
            new Def("starfall_crater", CRATER_FIELD, 40, true, AVGameTests::starfallCrater),
            new Def("starfall_lands", CRATER_FIELD, StarfallManager.FALL_TICKS + 60, true, AVGameTests::starfallLands),
            new Def("alignment_tags_loaded", PLATFORM, 40, false, AVGameTests::alignmentTagsLoaded),
            new Def("scan_block_grants_knowledge", PLATFORM, 40, false, AVGameTests::scanBlockGrantsKnowledge),
            new Def("constellation_needs_attunement", PLATFORM, 40, false, AVGameTests::constellationNeedsAttunement),
            new Def("beam_fills_reservoir", CRATER_FIELD, 120, true, AVGameTests::beamFillsReservoir),
            new Def("beam_blocked_by_stone", CRATER_FIELD, 80, true, h -> beamThrough(h, Blocks.STONE, false)),
            new Def("beam_passes_glass", CRATER_FIELD, 120, true, h -> beamThrough(h, Blocks.GLASS, true)),
            new Def("prism_splits_light", CRATER_FIELD, 160, true, AVGameTests::prismSplitsLight),
            new Def("reservoir_feeds_lens", CRATER_FIELD, 140, true, AVGameTests::reservoirFeedsLens),
            new Def("forge_makes_starmetal", CRATER_FIELD, 200, true, AVGameTests::forgeMakesStarmetal),
            new Def("altar_symmetric_ritual", CRATER_FIELD, 300, true, AVGameTests::altarSymmetricRitual),
            new Def("altar_asymmetry_lowers_stability", CRATER_FIELD, 60, true, AVGameTests::altarAsymmetry),
            new Def("sigil_sockets_into_gear", PLATFORM, 40, false, AVGameTests::sigilSockets),
            new Def("undead_kill_by_day_is_a_deed", PLATFORM, 40, false, AVGameTests::undeadDeed),
            new Def("whirl_hits_nearby_mobs", CRATER_FIELD, 40, true, AVGameTests::whirlHits),
            new Def("anchor_ritual_eases_gaze", CRATER_FIELD, 40, true, AVGameTests::anchorRitual),
            new Def("constellation_lock_needs_sign", PLATFORM, 40, false, AVGameTests::constellationLock),
            new Def("observatory_templates_load", PLATFORM, 40, false, AVGameTests::observatoryTemplates));

    static void anchorRitual(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setData(AVAttachments.RESEARCH, ResearchData.EMPTY.withConstellation("halo"));
        player.setData(AVAttachments.GAZE, 500);
        BlockPos first = null;
        for (var p : media.hofferbert.astralvigil.research.AnchorPatterns.PATTERNS.get("halo")) {
            BlockPos rel = GT.pos(3 + p.x(), 4, 3 + p.z());
            helper.setBlock(rel, AVBlocks.STAR_ANCHOR.get());
            var anchor = (media.hofferbert.astralvigil.starlight.network.Devices.Anchor)
                    helper.getLevel().getBlockEntity(helper.absolutePos(rel));
            anchor.buffer().insert(Tint.PURE, 200, false);
            if (first == null) {
                first = helper.absolutePos(rel);
            }
        }
        player.setPos(first.getX() + 0.5, first.getY(), first.getZ() + 0.5);
        media.hofferbert.astralvigil.eclipse.AnchorRitual.tryActivate(helper.getLevel(), first, player);
        int gaze = player.getData(AVAttachments.GAZE);
        GT.check(helper, gaze < 500, "gaze not eased: " + gaze);
        var a = (media.hofferbert.astralvigil.starlight.network.Devices.Anchor) helper.getLevel().getBlockEntity(first);
        GT.check(helper, a.buffer().total() == 100, "anchor should have spent 100, holds " + a.buffer().total());
        helper.succeed();
    }

    static void constellationLock(GameTestHelper helper) {
        BlockPos rel = GT.pos(2, 1, 2);
        helper.setBlock(rel, AVBlocks.CONSTELLATION_LOCK.get().defaultBlockState()
                .setValue(media.hofferbert.astralvigil.block.ConstellationLockBlock.SIGN,
                        media.hofferbert.astralvigil.block.ConstellationLockBlock.Sign.CRESCENT));
        BlockPos abs = helper.absolutePos(rel);
        var hit = new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(abs), Direction.UP, abs, false);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setData(AVAttachments.RESEARCH, ResearchData.EMPTY);
        helper.getLevel().getBlockState(abs).useWithoutItem(helper.getLevel(), player, hit);
        GT.check(helper, helper.getLevel().getBlockState(abs).is(AVBlocks.CONSTELLATION_LOCK.get()), "lock opened without the sign");
        player.setData(AVAttachments.RESEARCH, ResearchData.EMPTY.withConstellation("crescent"));
        helper.getLevel().getBlockState(abs).useWithoutItem(helper.getLevel(), player, hit);
        GT.check(helper, helper.getLevel().getBlockState(abs).isAir(), "lock did not open for a player who knows the sign");
        helper.succeed();
    }

    static void observatoryTemplates(GameTestHelper helper) {
        var manager = helper.getLevel().getStructureManager();
        for (String sign : new String[] {"halo", "crescent", "anvil", "quill"}) {
            var template = manager.get(AstralVigil.id("fallen_observatory/" + sign));
            GT.check(helper, template.isPresent(), "missing template " + sign);
            GT.check(helper, template.get().getSize().getX() == 17, "unexpected size " + template.get().getSize());
        }
        helper.succeed();
    }

    static void firmamentTemplates(GameTestHelper helper) {
        var manager = helper.getLevel().getStructureManager();
        var settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings();
        java.util.Map<String, net.minecraft.world.level.block.Block> key = java.util.Map.of(
                "landing", media.hofferbert.astralvigil.registry.AVBlocks.STARLIGHT_FONT.get(),
                "solenne", media.hofferbert.astralvigil.registry.AVBlocks.TRIAL_GOAL.get(),
                "vael", media.hofferbert.astralvigil.registry.AVBlocks.MOON_TARGET.get(),
                "orrin", media.hofferbert.astralvigil.registry.AVBlocks.TRIAL_RECEPTOR.get(),
                "ilyra", media.hofferbert.astralvigil.registry.AVBlocks.TRIAL_GOAL.get());
        for (var e : key.entrySet()) {
            var template = manager.get(AstralVigil.id("firmament/" + e.getKey()));
            GT.check(helper, template.isPresent(), "missing template " + e.getKey());
            int found = template.get().filterBlocks(net.minecraft.core.BlockPos.ZERO, settings, e.getValue()).size();
            int expected = e.getKey().equals("vael") ? 6 : 1;
            GT.check(helper, found == expected, e.getKey() + ": expected " + expected + " key blocks, found " + found);
        }
        helper.succeed();
    }

    static void trialGrantsMarkAndFavour(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        var w = media.hofferbert.astralvigil.firmament.Watcher.SOLENNE;
        GT.check(helper, !media.hofferbert.astralvigil.firmament.TrialManager.complete(player, w), "completed without trial");
        player.getPersistentData().putString("astralvigil_trial", w.id());
        player.getPersistentData().putLong("astralvigil_trial_until", helper.getLevel().getGameTime() + 1000);
        GT.check(helper, media.hofferbert.astralvigil.firmament.TrialManager.complete(player, w), "trial not completed");
        GT.check(helper, player.getInventory().contains(new ItemStack(AVItems.MARKS.get(w).get())), "no mark");
        GT.check(helper, media.hofferbert.astralvigil.firmament.TrialManager.favour(player, w) == 50, "favour");
        GT.check(helper, media.hofferbert.astralvigil.firmament.TrialManager.favour(player, w.rival()) == -15, "rival favour");
        GT.check(helper, media.hofferbert.astralvigil.firmament.TrialManager.activeTrial(player).isEmpty(), "trial not cleared");
        helper.succeed();
    }

    private static media.hofferbert.astralvigil.eclipse.EclipseEntity eclipse(GameTestHelper helper) {
        var e = media.hofferbert.astralvigil.eclipse.EclipseEntity.summon(helper.getLevel(),
                net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(GT.pos(4, 6, 4))));
        GT.check(helper, e != null, "eclipse not summoned");
        return e;
    }

    static void eclipsePhaseTransition(GameTestHelper helper) {
        var e = eclipse(helper);
        GT.check(helper, e.phase() == 1, "starts in phase " + e.phase());
        helper.runAfterDelay(5, () -> e.setHealth(e.getMaxHealth() * 0.5f));
        helper.succeedWhen(() -> GT.check(helper, e.phase() == 2 && e.inTransition() && e.isInvulnerable(),
                "phase " + e.phase() + " transition " + e.inTransition()));
    }

    static void eclipseDropsHeart(GameTestHelper helper) {
        var e = eclipse(helper);
        e.kill(
                //? if >=26.1 {
                helper.getLevel()
                //?}
        );
        helper.succeedWhen(() -> GT.check(helper, !helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        e.getBoundingBox().inflate(8), i -> i.getItem().is(AVItems.ECLIPSE_HEART.get())).isEmpty(),
                "no Eclipse Heart dropped"));
    }

    static void eclipseAwardsAdvancement(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        media.hofferbert.astralvigil.firmament.TrialManager.award(player, "endgame/eclipse_defeated");
        var adv = helper.getLevel().getServer().getAdvancements().get(AstralVigil.id("endgame/eclipse_defeated"));
        GT.check(helper, adv != null && player.getAdvancements().getOrStartProgress(adv).isDone(), "advancement not granted");
        helper.succeed();
    }

    static void echoExpires(GameTestHelper helper) {
        var echo = media.hofferbert.astralvigil.eclipse.EclipseEcho.spawn(helper.getLevel(),
                net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(GT.pos(4, 5, 4))), 20);
        GT.check(helper, echo != null, "echo not spawned");
        helper.runAfterDelay(40, () -> {
            GT.check(helper, echo.isRemoved(), "echo still alive");
            helper.succeed();
        });
    }

    static void sigilSockets(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(AVItems.HALO_EDGE.get()));
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(AVItems.SIGILS.get(Sigil.DAWN).get()));
        GT.check(helper, GearManager.trySocket(player), "socketing not handled");
        GearData data = GearManager.data(player.getMainHandItem());
        GT.check(helper, data.sigils().equals(List.of("dawn")), "sigils " + data.sigils());
        GT.check(helper, data.activeSigil().orElse(null) == Sigil.DAWN, "active sigil " + data.activeSigil());
        helper.succeed();
    }

    static void undeadDeed(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack sword = new ItemStack(AVItems.HALO_EDGE.get());
        var zombie = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, GT.pos(2, 1, 2));
        boolean day = !media.hofferbert.astralvigil.starlight.StarlightMath.isNight(Celestial.dayTime(helper.getLevel()));
        GearManager.onKill(player, zombie, sword);
        int expected = day ? 1 : 0;
        GT.check(helper, GearManager.data(sword).deeds() == expected, "deeds " + GearManager.data(sword).deeds() + " expected " + expected);
        helper.succeed();
    }

    static void whirlHits(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos at = helper.absolutePos(GT.pos(6, 4, 6));
        player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        var pig = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.PIG, GT.pos(7, 4, 6));
        float before = pig.getHealth();
        GearManager.performForTest(helper.getLevel(), player, Sigil.WHIRL);
        GT.check(helper, pig.getHealth() < before, "pig was not hit (" + pig.getHealth() + ")");
        helper.succeed();
    }

    static void forgeMakesStarmetal(GameTestHelper helper) {
        AstralForgeBlockEntity forge = (AstralForgeBlockEntity) placeBe(helper, GT.pos(6, 4, 6), AVBlocks.ASTRAL_FORGE.get());
        forge.insert(new ItemStack(Items.IRON_INGOT));
        forge.insert(new ItemStack(AVItems.ASTRAL_SHARD.get()));
        forge.insert(new ItemStack(AVItems.ASTRAL_SHARD.get()));
        forge.buffer().insert(Tint.STELLA, 500, false);
        helper.succeedWhen(() -> GT.check(
                helper,
                forge.items().size() == 1 && forge.items().get(0).is(AVItems.STARMETAL_INGOT.get()),
                "forge holds " + forge.items()));
    }

    private static ConvergenceAltarBlockEntity altarWithPedestals(GameTestHelper helper, int cx, int cz, boolean symmetric) {
        ConvergenceAltarBlockEntity altar =
                (ConvergenceAltarBlockEntity) placeBe(helper, GT.pos(cx, 4, cz), AVBlocks.CONVERGENCE_ALTAR.get());
        int[][] spots = symmetric
                ? new int[][] {{-2, 0}, {2, 0}, {0, -2}, {0, 2}}
                : new int[][] {{-2, 0}, {2, 0}, {0, -2}, {-1, 2}};
        Item[] offerings = {Items.GOLD_NUGGET, Items.GOLD_NUGGET, Items.AMETHYST_SHARD, Items.AMETHYST_SHARD};
        for (int i = 0; i < spots.length; i++) {
            PedestalBlockEntity p = (PedestalBlockEntity)
                    placeBe(helper, GT.pos(cx + spots[i][0], 4, cz + spots[i][1]), AVBlocks.PEDESTAL.get());
            p.setItem(new ItemStack(offerings[i]));
        }
        altar.setCatalyst(new ItemStack(AVItems.STARMETAL_INGOT.get()));
        altar.buffer().insert(Tint.STELLA, 1000, false);
        return altar;
    }

    static void altarSymmetricRitual(GameTestHelper helper) {
        ConvergenceAltarBlockEntity altar = altarWithPedestals(helper, 6, 6, true);
        // aligned blocks lift stability to 100, so no random instability event can spoil the ritual
        for (int[] d : new int[][] {{-3, -3}, {3, 3}, {-3, 3}, {3, -3}, {-1, -1}, {1, 1}, {-1, 1}, {1, -1}}) {
            helper.setBlock(GT.pos(6 + d[0], 3, 6 + d[1]), Blocks.AMETHYST_BLOCK);
        }
        GT.check(helper, altar.tryStart(helper.getLevel()), "ritual did not start");
        GT.check(helper, altar.stability() == 100, "stability " + altar.stability());
        helper.succeedWhen(() -> GT.check(
                helper,
                altar.catalyst().is(AVItems.SIGIL_BLANK.get()) && altar.catalyst().getCount() == 2,
                "altar holds " + altar.catalyst()));
    }

    static void altarAsymmetry(GameTestHelper helper) {
        int sym = altarWithPedestals(helper, 3, 3, true)
                .computeStability(helper.getLevel(), java.util.Optional.of(Alignment.STELLA));
        int asym = altarWithPedestals(helper, 9, 9, false)
                .computeStability(helper.getLevel(), java.util.Optional.of(Alignment.STELLA));
        GT.check(helper, asym < sym, "asymmetric " + asym + " should be below symmetric " + sym);
        helper.succeed();
    }

    private static net.minecraft.world.level.block.entity.BlockEntity placeBe(GameTestHelper helper, BlockPos rel, Block block) {
        helper.setBlock(rel, block);
        var be = helper.getLevel().getBlockEntity(helper.absolutePos(rel));
        if (be instanceof StarlightBlockEntity device) {
            drive(helper, device);
        }
        return be;
    }

    /**
     * Ticks a device every test tick. GameTest chunks are not always block-ticking (seen on 1.21.1), so tests drive
     * their devices themselves; a device that also ticks naturally simply works twice as fast.
     */
    private static void drive(GameTestHelper helper, StarlightBlockEntity be) {
        helper.onEachTick(() -> {
            if (!be.isRemoved()) {
                StarlightBlockEntity.serverTick(helper.getLevel(), be.getBlockPos(), be.getBlockState(), be);
            }
        });
    }

    static void reservoirFeedsLens(GameTestHelper helper) {
        Devices.Reservoir source = device(helper, GT.pos(1, 4, 6), AVBlocks.STARLIGHT_RESERVOIR.get(), Direction.NORTH);
        source.buffer().insert(Tint.STELLA, 300, false);
        device(helper, GT.pos(2, 4, 6), AVBlocks.LENS.get(), Direction.EAST);
        Devices.Reservoir target = device(helper, GT.pos(6, 4, 6), AVBlocks.STARLIGHT_RESERVOIR.get(), Direction.NORTH);
        helper.succeedWhen(() -> GT.check(helper, target.buffer().get(Tint.STELLA) > 0, "no Stella light arrived via the lens"));
    }

    private AVGameTests() {}

    private static <T extends StarlightBlockEntity> T device(GameTestHelper helper, BlockPos rel, Block block, Direction facing) {
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        } else if (state.hasProperty(BlockStateProperties.FACING)) {
            state = state.setValue(BlockStateProperties.FACING, facing);
        }
        helper.setBlock(rel, state);
        @SuppressWarnings("unchecked")
        T be = (T) helper.getLevel().getBlockEntity(helper.absolutePos(rel));
        GT.check(helper, be != null, "no block entity at " + rel);
        drive(helper, be);
        return be;
    }

    private static Devices.Collector chargedCollector(GameTestHelper helper, BlockPos rel) {
        Devices.Collector collector = device(helper, rel, AVBlocks.STARLIGHT_COLLECTOR.get(), Direction.EAST);
        collector.buffer().insert(Tint.PURE, 300, false);
        return collector;
    }

    static void beamFillsReservoir(GameTestHelper helper) {
        chargedCollector(helper, GT.pos(1, 4, 6));
        Devices.Reservoir reservoir = device(helper, GT.pos(5, 4, 6), AVBlocks.STARLIGHT_RESERVOIR.get(), Direction.NORTH);
        helper.succeedWhen(() -> GT.check(helper, reservoir.buffer().get(Tint.PURE) > 0, "reservoir still empty"));
    }

    static void beamThrough(GameTestHelper helper, Block between, boolean expectTransfer) {
        chargedCollector(helper, GT.pos(1, 4, 6));
        helper.setBlock(GT.pos(3, 4, 6), between);
        Devices.Reservoir reservoir = device(helper, GT.pos(5, 4, 6), AVBlocks.STARLIGHT_RESERVOIR.get(), Direction.NORTH);
        if (expectTransfer) {
            helper.succeedWhen(() -> GT.check(helper, reservoir.buffer().total() > 0, "beam did not pass " + between));
        } else {
            helper.runAfterDelay(60, () -> {
                GT.check(helper, reservoir.buffer().total() == 0, "beam passed through " + between);
                helper.succeed();
            });
        }
    }

    static void prismSplitsLight(GameTestHelper helper) {
        chargedCollector(helper, GT.pos(1, 4, 6));
        device(helper, GT.pos(4, 4, 6), AVBlocks.PRISM.get(), Direction.EAST);
        Devices.Reservoir front = device(helper, GT.pos(7, 4, 6), AVBlocks.STARLIGHT_RESERVOIR.get(), Direction.NORTH);
        Devices.Reservoir left = device(helper, GT.pos(4, 4, 3), AVBlocks.STARLIGHT_RESERVOIR.get(), Direction.NORTH);
        Devices.Reservoir right = device(helper, GT.pos(4, 4, 9), AVBlocks.STARLIGHT_RESERVOIR.get(), Direction.NORTH);
        helper.succeedWhen(() -> {
            GT.check(helper, front.buffer().get(Tint.STELLA) > 0, "no Stella light in front");
            GT.check(helper, left.buffer().get(Tint.SOL) > 0, "no Sol light on the left");
            GT.check(helper, right.buffer().get(Tint.LUNA) > 0, "no Luna light on the right");
            GT.check(helper, front.buffer().get(Tint.PURE) == 0, "pure light leaked through the prism");
        });
    }

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
