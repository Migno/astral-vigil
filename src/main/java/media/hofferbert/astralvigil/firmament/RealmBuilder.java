package media.hofferbert.astralvigil.firmament;

import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.registry.AVBlocks;
import media.hofferbert.astralvigil.registry.AVBuilding;
import media.hofferbert.astralvigil.registry.AVEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

/**
 * Builds a realm on its first visit without stalling the server. The realm template is split into pieces
 * ({@code structure/realm/<id>/p0..pN.nbt}, see tools/dev/gen_realms.py): piece 0 - the arrival shrine with its return
 * gate - is placed at once, so the traveller always lands on solid ground; everything else is set block by block over
 * the following ticks within a small time budget per tick. The Starlight Font on the pad marks the realm as started,
 * a bedrock block hidden under the pad (set by the last piece) as finished; an unfinished realm resumes on the next visit
 * or server start. When the realm is complete the Watcher appears and fragments start to drift ({@link FloatingBlocks}).
 */
public final class RealmBuilder {
    /** Nanoseconds of block placement allowed per server tick and realm (keeps the tick well under 50 ms). */
    static final long BUDGET_NANOS = 8_000_000L;

    private static final java.util.Map<Realm, Job> JOBS = new java.util.EnumMap<>(Realm.class);

    private RealmBuilder() {}

    public static void register() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.LevelTickEvent.Post e) -> {
            if (e.getLevel() instanceof ServerLevel level) {
                Realm realm = Realm.of(level);
                if (realm != null) {
                    tick(level, realm);
                }
            }
        });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> JOBS.clear());
    }

    public static boolean isBuilt(ServerLevel level, Realm realm) {
        return level.getBlockState(realm.pad()).is(AVBlocks.STARLIGHT_FONT.get());
    }

    public static boolean isComplete(ServerLevel level, Realm realm) {
        return level.getBlockState(realm.pad().below(3)).is(Blocks.BEDROCK);
    }

    public static boolean isBuilding(Realm realm) {
        return JOBS.containsKey(realm);
    }

    /** Places the arrival pad right away and queues the rest; resumes an unfinished realm. */
    public static void ensureBuilt(ServerLevel level, Realm realm) {
        if (JOBS.containsKey(realm) || isBuilt(level, realm) && isComplete(level, realm)) {
            return;
        }
        boolean fresh = !isBuilt(level, realm);
        java.util.List<StructureTemplate> pieces = pieces(level, realm);
        if (pieces.isEmpty()) {
            throw new IllegalStateException("Missing realm template " + realm.id());
        }
        BlockPos origin = realm.pad().offset(-Realm.ANCHOR, -Realm.FLOOR_Y, -Realm.ANCHOR);
        if (realm == Realm.FIRMAMENT) {
            clearSky(level, realm.pad(), 12, 0); // the wilds: open the sky around the shrine first
        }
        pieces.get(0).placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS);
        Job job = new Job(level, realm, origin, pieces, fresh);
        JOBS.put(realm, job);
        AstralVigil.LOGGER.info("Firmament: building realm {} ({} pieces) in the background", realm.id(), pieces.size());
    }

    /** Builds (or finishes) a realm completely right now - GameTests and admin tooling only. */
    public static void buildNow(ServerLevel level, Realm realm) {
        ensureBuilt(level, realm);
        Job job = JOBS.get(realm);
        while (job != null && !job.step(Long.MAX_VALUE)) {
            // keep going
        }
        if (job != null) {
            finish(job);
        }
    }

    static java.util.List<StructureTemplate> pieces(ServerLevel level, Realm realm) {
        java.util.List<StructureTemplate> list = new java.util.ArrayList<>();
        for (int i = 0; ; i++) {
            var t = level.getStructureManager().get(AstralVigil.id("realm/" + realm.id() + "/p" + i));
            if (t.isEmpty()) {
                return list;
            }
            list.add(t.get());
        }
    }

    private static void tick(ServerLevel level, Realm realm) {
        Job job = JOBS.get(realm);
        if (job == null || job.level != level) {
            return;
        }
        if (job.step(BUDGET_NANOS)) {
            finish(job);
        }
    }

    private static void finish(Job job) {
        JOBS.remove(job.realm);
        // spawn what is missing (also after a build that was interrupted by a server stop)
        boolean watcherThere = job.realm.watcher() == null || !job.level.getEntitiesOfClass(WatcherEntity.class,
                new net.minecraft.world.phys.AABB(job.realm.pad()).inflate(48)).isEmpty();
        boolean floatersThere = !job.level.getEntities(net.minecraft.world.entity.EntityType.BLOCK_DISPLAY,
                d -> FloatingBlocks.tags(d).contains(FloatingBlocks.TAG)).isEmpty();
        {
            if (!watcherThere) {
                // floating right beside the bridge (within reach of the walkway), turned towards arriving travellers
                spawnWatcher(job.level, job.realm.watcher(), job.realm.pad().offset(3, 1, -10), Direction.SOUTH);
            }
            if (!floatersThere) {
                floaters(job.level, job.realm);
            }
        }
        AstralVigil.LOGGER.info("Firmament: realm {} complete", job.realm.id());
    }

    /** One realm under construction: a cursor through the clear-sky pass (wilds only) and the piece block lists. */
    static final class Job {
        final ServerLevel level;
        final Realm realm;
        final BlockPos origin;
        final java.util.List<StructureTemplate> pieces;
        final boolean fresh;
        int clearRing;
        int piece = 1;
        int block = 0;

        Job(ServerLevel level, Realm realm, BlockPos origin, java.util.List<StructureTemplate> pieces, boolean fresh) {
            this.level = level;
            this.realm = realm;
            this.origin = origin;
            this.pieces = pieces;
            this.fresh = fresh;
            this.clearRing = realm == Realm.FIRMAMENT ? 13 : Integer.MAX_VALUE;
        }

        /** Works until the budget is spent; true when everything is placed. */
        boolean step(long budget) {
            long end = budget == Long.MAX_VALUE ? Long.MAX_VALUE : System.nanoTime() + budget;
            while (clearRing <= CLEAR_RADIUS) {
                clearSky(level, realm.pad(), clearRing, clearRing - 1);
                clearRing++;
                if (System.nanoTime() > end) {
                    return false;
                }
            }
            while (piece < pieces.size()) {
                var blocks = pieces.get(piece).palettes.get(0).blocks();
                while (block < blocks.size()) {
                    var info = blocks.get(block++);
                    BlockPos at = origin.offset(info.pos());
                    level.setBlock(at, info.state(), Block.UPDATE_CLIENTS);
                    if (info.nbt() != null) {
                        var be = level.getBlockEntity(at);
                        if (be != null) {
                            //? if >=26.1 {
                            be.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(
                                    net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), info.nbt()));
                            //?} else {
                            /*be.loadWithComponents(info.nbt(), level.registryAccess());
                            *///?}
                        }
                    }
                    if ((block & 63) == 0 && System.nanoTime() > end) {
                        return false;
                    }
                }
                piece++;
                block = 0;
            }
            return true;
        }
    }

    static final int CLEAR_RADIUS = 34;

    /**
     * The wilds grow their own floating islands; the shrine needs open sky around it. Carves one ring
     * ({@code inner < horizontal distance <= outer}) of a flattened sphere of air (the shrine's own island and halo come
     * with the template) so the groves frame the shrine instead of burying it.
     */
    static void clearSky(ServerLevel level, BlockPos centre, int outer, int inner) {
        int r = CLEAR_RADIUS;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -outer; dx <= outer; dx++) {
            for (int dz = -outer; dz <= outer; dz++) {
                int ring = Math.max(Math.abs(dx), Math.abs(dz));
                if (ring > outer || ring <= inner && inner > 0) {
                    continue;
                }
                for (int dy = -18; dy <= 44; dy++) {
                    double ny = dy < 0 ? dy / 18.0 : dy / 44.0;
                    if ((dx * dx + dz * dz) / (double) (r * r) + ny * ny > 1.0) {
                        continue;
                    }
                    p.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    if (!level.getBlockState(p).isAir()) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    /** Orrin's puzzle back to its starting state: every mirror of the Starforge turned as the template has it. */
    public static void resetMirrors(ServerLevel level, Realm realm) {
        BlockPos origin = realm.pad().offset(-Realm.ANCHOR, -Realm.FLOOR_Y, -Realm.ANCHOR);
        for (StructureTemplate t : pieces(level, realm)) {
            for (var info : t.palettes.get(0).blocks()) {
                if (info.state().is(AVBlocks.MIRROR.get())) {
                    BlockPos at = origin.offset(info.pos());
                    if (level.getBlockState(at).is(AVBlocks.MIRROR.get()) && level.getBlockState(at) != info.state()) {
                        level.setBlock(at, info.state(), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    public static GateKind gateOf(Watcher w) {
        for (GateKind k : GateKind.values()) {
            if (k.watcher() == w) {
                return k;
            }
        }
        throw new IllegalArgumentException(w.toString());
    }

    /** Drifting fragments for each realm: what they are made of and where they hang. */
    static void floaters(ServerLevel level, Realm realm) {
        RandomSource rnd = RandomSource.create(realm.id().hashCode());
        BlockPos c = realm.hasTrial() ? realm.trialCentre() : realm.pad();
        BlockState[] palette = switch (realm) {
            case FIRMAMENT -> new BlockState[] {AVBuilding.CELESTIAL_MARBLE.get().defaultBlockState(), AVBuilding.ASTRAL_LAMP.get().defaultBlockState(),
                AVBuilding.STARWOOD_LEAVES.get().defaultBlockState(), Blocks.AMETHYST_BLOCK.defaultBlockState()};
            case SUNSPIRE -> new BlockState[] {AVBuilding.SUNSTONE.get().defaultBlockState(), AVBuilding.SUNSTONE_LAMP.get().defaultBlockState(),
                Blocks.GOLD_BLOCK.defaultBlockState(), AVBuilding.GILDED_CELESTIAL_BRICKS.get().defaultBlockState()};
            case MOONWELL -> new BlockState[] {AVBuilding.MOONSTONE.get().defaultBlockState(), AVBuilding.MOONSTONE_LAMP.get().defaultBlockState(),
                AVBuilding.MOONSTONE_BRICKS.get().defaultBlockState(), Blocks.CALCITE.defaultBlockState()};
            case STARFORGE -> new BlockState[] {Blocks.AMETHYST_BLOCK.defaultBlockState(), AVBuilding.NEBULA_GLASS.get().defaultBlockState(),
                AVBuilding.ASTRAL_LAMP.get().defaultBlockState(), AVBuilding.HALO_GLASS.get().defaultBlockState()};
            case ARCHIVE -> new BlockState[] {Blocks.BOOKSHELF.defaultBlockState(), Blocks.CHISELED_BOOKSHELF.defaultBlockState(),
                AVBuilding.GILDED_STARWOOD.get().defaultBlockState(), Blocks.LECTERN.defaultBlockState()};
            case UMBRAL_THRONE -> new BlockState[] {AVBuilding.UMBRAL_BRICKS.get().defaultBlockState(), AVBuilding.CRACKED_UMBRAL_BRICKS.get().defaultBlockState(),
                AVBuilding.UMBRAL_LAMP.get().defaultBlockState(), AVBuilding.ECLIPSE_GLASS.get().defaultBlockState(),
                AVBuilding.CHISELED_UMBRAL_BRICKS.get().defaultBlockState(), AVBlocks.VOID_CRYSTAL.get().defaultBlockState()};
        };
        int count = realm == Realm.UMBRAL_THRONE ? 70 : 36;
        int spread = realm.hasTrial() ? realm.trialRadius() + 8 : 30;
        for (int i = 0; i < count; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            double d = rnd.nextDouble() * spread;
            double y = c.getY() + 6 + rnd.nextDouble() * (realm == Realm.UMBRAL_THRONE ? 26 : 30);
            if (realm == Realm.UMBRAL_THRONE && i < 40) {
                // debris of the vault hangs inside the broken nave, above the fight
                a = 0;
                d = 0;
                double x = c.getX() + 0.5 + (rnd.nextDouble() - 0.5) * 30;
                double z = c.getZ() + 0.5 + (rnd.nextDouble() - 0.6) * 54;
                y = c.getY() + 9 + rnd.nextDouble() * 22;
                place(level, new Vec3(x, y, z), palette[rnd.nextInt(palette.length)], rnd);
                continue;
            }
            Vec3 at = new Vec3(c.getX() + 0.5 + Math.cos(a) * d, y, c.getZ() + 0.5 + Math.sin(a) * d);
            place(level, at, palette[rnd.nextInt(palette.length)], rnd);
        }
    }

    private static void place(ServerLevel level, Vec3 at, BlockState state, RandomSource rnd) {
        if (!level.getBlockState(BlockPos.containing(at)).isAir()) {
            return;
        }
        float scale = 0.45f + rnd.nextFloat() * (rnd.nextFloat() < 0.15f ? 1.6f : 0.7f);
        FloatingBlocks.spawn(level, at, state, scale, state.getLightEmission() > 0);
    }

    /** Spawns a Watcher floating at {@code at}, turned to face {@code facing}. */
    static void spawnWatcher(ServerLevel level, Watcher watcher, BlockPos at, Direction facing) {
        WatcherEntity entity = AVEntities.WATCHERS.get(watcher).get().create(level
                //? if >=26.1 {
                , net.minecraft.world.entity.EntitySpawnReason.STRUCTURE
                //?}
                );
        if (entity == null) {
            return;
        }
        float yaw = facing.toYRot();
        entity.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        entity.setYRot(yaw);
        entity.setYBodyRot(yaw);
        entity.setYHeadRot(yaw);
        entity.setPersistenceRequired();
        level.addFreshEntity(entity);
    }
}
