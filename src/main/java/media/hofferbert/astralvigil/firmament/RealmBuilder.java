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
 * Builds a realm on its first visit: places {@code structure/realm/<id>.nbt} so its anchor lands on the realm's pad,
 * sets the Watcher beside the way to its trial and lets broken stone, crystals and lamps drift in the air
 * ({@link FloatingBlocks}). The Starlight Font in the middle of the pad marks the realm as built (it is unbreakable).
 */
public final class RealmBuilder {
    private RealmBuilder() {}

    public static boolean isBuilt(ServerLevel level, Realm realm) {
        return level.getBlockState(realm.pad()).is(AVBlocks.STARLIGHT_FONT.get());
    }

    public static void ensureBuilt(ServerLevel level, Realm realm) {
        if (isBuilt(level, realm)) {
            return;
        }
        long start = System.nanoTime();
        StructureTemplate template = level.getStructureManager()
                .get(AstralVigil.id("realm/" + realm.id()))
                .orElseThrow(() -> new IllegalStateException("Missing realm template " + realm.id()));
        if (realm == Realm.FIRMAMENT) {
            clearSky(level, realm.pad());
        }
        BlockPos origin = realm.pad().offset(-Realm.ANCHOR, -Realm.FLOOR_Y, -Realm.ANCHOR);
        template.placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS);
        if (realm.watcher() != null) {
            // floating beside the way in, turned towards arriving travellers
            spawnWatcher(level, realm.watcher(), realm.pad().offset(7, 4, -11), Direction.SOUTH);
        }
        floaters(level, realm);
        AstralVigil.LOGGER.info("Firmament: realm {} built in {} ms", realm.id(), (System.nanoTime() - start) / 1_000_000);
    }

    /**
     * The wilds grow their own floating islands; the shrine needs open sky around it. Carves a flattened sphere of air
     * (the shrine's own island and halo come with the template) so the groves frame the shrine instead of burying it.
     */
    static void clearSky(ServerLevel level, BlockPos centre) {
        int r = 34;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
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
