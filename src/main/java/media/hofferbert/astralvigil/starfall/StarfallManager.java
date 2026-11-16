package media.hofferbert.astralvigil.starfall;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.compat.Celestial;
import media.hofferbert.astralvigil.compat.Keys;
import media.hofferbert.astralvigil.config.AVServerConfig;
import media.hofferbert.astralvigil.starlight.StarlightMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Starfall: at night a star may fall near a player. The fall is a short server-tracked animation (particle trail from
 * the sky) followed by an impact that carves a small crater with a Fallen Star in its centre.
 *
 * <p>Performance: one modulo check per level tick; rolls only every {@code checkIntervalTicks}; active falls are
 * a handful at most. Nothing iterates over blocks except the one-off crater carve.
 */
public final class StarfallManager {
    /** Length of the visible fall in ticks. */
    public static final int FALL_TICKS = 50;
    /** Height above the impact where the trail starts. */
    public static final int FALL_HEIGHT = 110;

    private static final int MAX_RECENT = 16;

    private static final Map<ResourceKey<Level>, List<Fall>> ACTIVE = new HashMap<>();
    private static final Map<ResourceKey<Level>, Deque<BlockPos>> RECENT = new HashMap<>();
    private static final Map<UUID, Long> PLAYER_COOLDOWN = new HashMap<>();

    private StarfallManager() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(StarfallManager::onLevelTick);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> clear());
    }

    static void clear() {
        ACTIVE.clear();
        RECENT.clear();
        PLAYER_COOLDOWN.clear();
    }

    /** A falling star on its way down. */
    record Fall(Vec3 from, BlockPos target, long startTick) {
        Vec3 positionAt(long gameTime) {
            double t = Mth.clamp((gameTime - startTick) / (double) FALL_TICKS, 0.0, 1.0);
            Vec3 to = Vec3.atCenterOf(target);
            return from.lerp(to, t);
        }

        boolean landed(long gameTime) {
            return gameTime - startTick >= FALL_TICKS;
        }
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        tickActive(level);

        if (!AVServerConfig.STARFALL_ENABLED.get() || !isAllowedDimension(level)) {
            return;
        }
        if (level.getGameTime() % AVServerConfig.STARFALL_CHECK_INTERVAL.get() != 0) {
            return;
        }
        if (!StarlightMath.isNight(Celestial.dayTime(level))) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            long readyAt = PLAYER_COOLDOWN.getOrDefault(player.getUUID(), Long.MIN_VALUE);
            if (level.getGameTime() < readyAt) {
                continue;
            }
            double chance = AVServerConfig.STARFALL_CHANCE.get();
            if (Celestial.moonPhase(level, player.blockPosition()) == 0) {
                chance *= AVServerConfig.STARFALL_FULL_MOON_MULTIPLIER.get();
            }
            if (level.getRandom().nextDouble() < chance) {
                trigger(level, player.blockPosition(), level.getRandom()).ifPresent(target -> PLAYER_COOLDOWN.put(
                        player.getUUID(), level.getGameTime() + AVServerConfig.STARFALL_PLAYER_COOLDOWN.get()));
            }
        }
    }

    static boolean isAllowedDimension(ServerLevel level) {
        String id = Keys.id(level.dimension()).toString();
        return AVServerConfig.STARFALL_DIMENSIONS.get().contains(id);
    }

    /**
     * Starts a starfall somewhere around {@code around}. Used by the natural roll, the debug command and GameTests.
     *
     * @return the impact column surface position, if a valid spot was found
     */
    public static Optional<BlockPos> trigger(ServerLevel level, BlockPos around, RandomSource random) {
        int min = AVServerConfig.STARFALL_MIN_DISTANCE.get();
        int max = Math.max(min + 1, AVServerConfig.STARFALL_MAX_DISTANCE.get());
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int dist = min + random.nextInt(max - min);
            int x = around.getX() + (int) Math.round(Math.cos(angle) * dist);
            int z = around.getZ() + (int) Math.round(Math.sin(angle) * dist);
            BlockPos column = new BlockPos(x, around.getY(), z);
            if (!level.isLoaded(column)) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos surface = new BlockPos(x, y, z);
            if (!level.getFluidState(surface.below()).isEmpty() || !level.canSeeSky(surface)) {
                continue;
            }
            start(level, surface, random);
            return Optional.of(surface);
        }
        return Optional.empty();
    }

    /** Starts a fall onto exactly this surface position. */
    public static void start(ServerLevel level, BlockPos surface, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2;
        Vec3 from = Vec3.atCenterOf(surface).add(Math.cos(angle) * 60, FALL_HEIGHT, Math.sin(angle) * 60);
        ACTIVE.computeIfAbsent(level.dimension(), k -> new ArrayList<>())
                .add(new Fall(from, surface, level.getGameTime()));
        announce(level, surface);
        AstralVigil.LOGGER.debug("Starfall started in {} at {}", Keys.id(level.dimension()), surface);
    }

    private static void tickActive(ServerLevel level) {
        List<Fall> falls = ACTIVE.get(level.dimension());
        if (falls == null || falls.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Fall> it = falls.iterator();
        while (it.hasNext()) {
            Fall fall = it.next();
            if (fall.landed(now)) {
                it.remove();
                land(level, fall.target());
                continue;
            }
            Vec3 p = fall.positionAt(now);
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 4, 0.25, 0.25, 0.25, 0.01);
            level.sendParticles(ParticleTypes.FIREWORK, p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.02);
            if ((now - fall.startTick()) % 10 == 0) {
                level.playSound(null, p.x, p.y, p.z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.AMBIENT, 6.0f, 0.5f);
            }
        }
    }

    private static void land(ServerLevel level, BlockPos surface) {
        if (!level.isLoaded(surface)) {
            return;
        }
        BlockPos star = StarfallCrater.impact(
                level,
                surface,
                AVServerConfig.STARFALL_CRATER_RADIUS.get(),
                AVServerConfig.STARFALL_TERRAIN_DAMAGE.get(),
                level.getRandom());
        Vec3 c = Vec3.atCenterOf(surface);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 1, c.z, 80, 1.5, 1.0, 1.5, 0.15);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0f, 0.7f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 3.0f, 0.6f);

        Deque<BlockPos> recent = RECENT.computeIfAbsent(level.dimension(), k -> new ArrayDeque<>());
        recent.addFirst(star.immutable());
        while (recent.size() > MAX_RECENT) {
            recent.removeLast();
        }
    }

    private static void announce(ServerLevel level, BlockPos surface) {
        int radius = AVServerConfig.STARFALL_ANNOUNCE_RADIUS.get();
        if (radius <= 0) {
            return;
        }
        double r2 = (double) radius * radius;
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(surface) <= r2) {
                Component dir = Component.translatable(
                        "direction.astralvigil." + Directions.cardinal(surface.getX() - player.getBlockX(), surface.getZ() - player.getBlockZ()));
                player.sendSystemMessage(Component.translatable("message.astralvigil.starfall.seen", dir));
            }
        }
    }

    /** Most recent fallen star (closest first) within range, if any. */
    public static Optional<BlockPos> nearestRecent(ServerLevel level, BlockPos from, int range) {
        Deque<BlockPos> recent = RECENT.get(level.dimension());
        if (recent == null) {
            return Optional.empty();
        }
        double r2 = (double) range * range;
        return recent.stream()
                .filter(p -> p.distSqr(from) <= r2)
                .min((a, b) -> Double.compare(a.distSqr(from), b.distSqr(from)));
    }

    public static int activeFalls(ServerLevel level) {
        List<Fall> falls = ACTIVE.get(level.dimension());
        return falls == null ? 0 : falls.size();
    }
}
