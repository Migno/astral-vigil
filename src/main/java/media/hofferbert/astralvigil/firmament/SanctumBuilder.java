package media.hofferbert.astralvigil.firmament;

import java.util.Map;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.registry.AVBlocks;
import media.hofferbert.astralvigil.registry.AVEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Builds the Firmament hub once (first arrival): landing platform at {@link Firmament#LANDING}, four sanctum islands on
 * the cardinal axes, cloud-stone bridges between them, and spawns the Watchers. The Starlight Font in the centre of
 * the landing is the "already built" marker (it is unbreakable).
 */
public final class SanctumBuilder {
    /** Template floor height inside every firmament template (see tools/dev/gen_firmament.py). */
    static final int FLOOR = 6;
    public static final int DISTANCE = 44;

    static final Map<Watcher, BlockPos> CENTRES = Map.of(
            Watcher.SOLENNE, Firmament.LANDING.east(DISTANCE),
            Watcher.VAEL, Firmament.LANDING.west(DISTANCE),
            Watcher.ORRIN, Firmament.LANDING.south(DISTANCE),
            Watcher.ILYRA, Firmament.LANDING.north(DISTANCE));

    /** Where each Watcher floats, relative to its sanctum centre. */
    static final Map<Watcher, Vec3i> WATCHER_OFFSET = Map.of(
            Watcher.SOLENNE, new Vec3i(-5, 1, 0),
            Watcher.VAEL, new Vec3i(5, 1, 0),
            Watcher.ORRIN, new Vec3i(0, 1, -6),
            Watcher.ILYRA, new Vec3i(3, 1, 12));

    private SanctumBuilder() {}

    public static boolean isBuilt(ServerLevel level) {
        return level.getBlockState(Firmament.LANDING).is(AVBlocks.STARLIGHT_FONT.get());
    }

    public static void ensureBuilt(ServerLevel level) {
        if (isBuilt(level)) {
            return;
        }
        place(level, "landing", Firmament.LANDING);
        for (var e : CENTRES.entrySet()) {
            place(level, e.getKey().id(), e.getValue());
            bridge(level, Firmament.LANDING, e.getValue());
            spawnWatcher(level, e.getKey(), e.getValue().offset(WATCHER_OFFSET.get(e.getKey())));
        }
        AstralVigil.LOGGER.info("Firmament sanctums built");
    }

    /** Places a template so that its centre floor block lands on {@code centre}. */
    static void place(ServerLevel level, String name, BlockPos centre) {
        StructureTemplate template = level.getStructureManager()
                .get(AstralVigil.id("firmament/" + name))
                .orElseThrow(() -> new IllegalStateException("Missing firmament template " + name));
        Vec3i size = template.getSize();
        BlockPos origin = centre.offset(-(size.getX() / 2), -FLOOR, -(size.getZ() / 2));
        template.placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS);
    }

    /** A 3-wide cloud-stone walkway from the landing to a sanctum. */
    static void bridge(ServerLevel level, BlockPos from, BlockPos to) {
        int dx = Integer.signum(to.getX() - from.getX());
        int dz = Integer.signum(to.getZ() - from.getZ());
        int length = Math.max(Math.abs(to.getX() - from.getX()), Math.abs(to.getZ() - from.getZ()));
        for (int i = 6; i <= length - 8; i++) {
            BlockPos p = from.offset(dx * i, 0, dz * i);
            for (int w = -1; w <= 1; w++) {
                BlockPos q = dx != 0 ? p.offset(0, 0, w) : p.offset(w, 0, 0);
                if (level.getBlockState(q).isAir()) {
                    level.setBlock(q, AVBlocks.FIRMAMENT_STONE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    static void spawnWatcher(ServerLevel level, Watcher watcher, BlockPos at) {
        WatcherEntity entity = AVEntities.WATCHERS.get(watcher).get().create(level
                //? if >=26.1 {
                , net.minecraft.world.entity.EntitySpawnReason.STRUCTURE
                //?}
                );
        if (entity == null) {
            return;
        }
        entity.setPos(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
        level.addFreshEntity(entity);
    }
}
