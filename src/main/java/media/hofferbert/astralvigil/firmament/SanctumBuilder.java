package media.hofferbert.astralvigil.firmament;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.registry.AVBlocks;
import media.hofferbert.astralvigil.registry.AVEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Builds the Firmament once (first arrival): the landing hub with its return gate at {@link Firmament#LANDING}, four
 * large trial islands 240 blocks out on the cardinal axes, the Umbral Throne far to the west (its bridge crosses Vael's
 * arena), long cloud-stone bridges and the Watchers floating beside each bridge end. The Starlight Font in the centre
 * of the landing is the "already built" marker (it is unbreakable).
 */
public final class SanctumBuilder {
    /** Template floor height inside every firmament template (see tools/dev/gen_firmament.py). */
    static final int FLOOR = 10;
    public static final int DISTANCE = 240;
    public static final int THRONE_DISTANCE = 520;

    /** Direction from the hub to each sanctum. */
    static final Map<Watcher, Direction> DIRECTION = Map.of(
            Watcher.SOLENNE, Direction.EAST,
            Watcher.VAEL, Direction.WEST,
            Watcher.ORRIN, Direction.SOUTH,
            Watcher.ILYRA, Direction.NORTH);

    /** Island radius of each sanctum (matches gen_firmament.py). */
    static final Map<Watcher, Integer> RADIUS = Map.of(
            Watcher.SOLENNE, 14,
            Watcher.VAEL, 26,
            Watcher.ORRIN, 22,
            Watcher.ILYRA, 37);

    public static final int THRONE_RADIUS = 34;
    public static final BlockPos THRONE = Firmament.LANDING.west(THRONE_DISTANCE);

    public static BlockPos centre(Watcher w) {
        return Firmament.LANDING.relative(DIRECTION.get(w), DISTANCE);
    }

    /** Point on the bridge just in front of a sanctum (where the Watcher waits and fallen players are set down). */
    public static BlockPos entrance(Watcher w) {
        return centre(w).relative(DIRECTION.get(w).getOpposite(), RADIUS.get(w) + 4).above();
    }

    /** All places the sky's safety net may return a falling player to. */
    public static List<BlockPos> anchors() {
        List<BlockPos> list = new ArrayList<>();
        list.add(Firmament.ARRIVAL);
        for (Watcher w : Watcher.values()) {
            list.add(entrance(w));
        }
        list.add(THRONE.east(THRONE_RADIUS + 4).above());
        return list;
    }

    private SanctumBuilder() {}

    public static boolean isBuilt(ServerLevel level) {
        return level.getBlockState(Firmament.LANDING).is(AVBlocks.STARLIGHT_FONT.get());
    }

    public static void ensureBuilt(ServerLevel level) {
        if (isBuilt(level)) {
            return;
        }
        place(level, "landing", Firmament.LANDING);
        for (Watcher w : Watcher.values()) {
            place(level, w.id(), centre(w));
            bridge(level, Firmament.LANDING, centre(w));
            Direction toHub = DIRECTION.get(w).getOpposite();
            BlockPos at = centre(w).relative(toHub, RADIUS.get(w) + 3).relative(toHub.getClockWise(), 5).above(3);
            spawnWatcher(level, w, at, toHub.getClockWise().getOpposite());
        }
        place(level, "throne", THRONE);
        bridge(level, centre(Watcher.VAEL), THRONE);
        AstralVigil.LOGGER.info("Firmament sanctums and the Umbral Throne built");
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

    /** A 3-wide cloud-stone walkway along an axis; only fills air, so it never cuts through an island's features. */
    static void bridge(ServerLevel level, BlockPos from, BlockPos to) {
        int dx = Integer.signum(to.getX() - from.getX());
        int dz = Integer.signum(to.getZ() - from.getZ());
        int length = Math.max(Math.abs(to.getX() - from.getX()), Math.abs(to.getZ() - from.getZ()));
        for (int i = 1; i < length; i++) {
            BlockPos p = from.offset(dx * i, 0, dz * i);
            for (int w = -1; w <= 1; w++) {
                BlockPos q = dx != 0 ? p.offset(0, 0, w) : p.offset(w, 0, 0);
                if (level.getBlockState(q).isAir()) {
                    boolean lamp = w != 0 && i % 16 == 0;
                    level.setBlock(q, (lamp ? net.minecraft.world.level.block.Blocks.SEA_LANTERN : AVBlocks.FIRMAMENT_STONE.get()).defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    /** Spawns a Watcher floating at {@code at}, turned to face {@code facing} (towards its bridge). */
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
