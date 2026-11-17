package media.hofferbert.astralvigil.firmament;

import media.hofferbert.astralvigil.AstralVigil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
//? if >=26.1 {
import net.minecraft.world.level.portal.TeleportTransition;
//?} else {
/*import net.minecraft.world.level.portal.DimensionTransition;
*///?}

/** The Firmament dimension: reached through player-built gates (one per destination), low gravity, one-time construction. */
public final class Firmament {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, AstralVigil.id("firmament"));
    public static final BlockPos LANDING = new BlockPos(0, 290, 0);
    static final Identifier GRAVITY_ID = AstralVigil.id("firmament_gravity");
    static final Identifier FALL_ID = AstralVigil.id("firmament_fall");
    static final String RETURN_KEY = "astralvigil_return";

    private Firmament() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(Firmament::onPlayerTick);
    }

    public static boolean isFirmament(Level level) {
        return level.dimension() == LEVEL;
    }

    /** Where a player standing in a Celestial Gate arrives: in the Firmament next to the return gate, or back home. */
    record Target(ServerLevel level, Vec3 pos, boolean ascending) {}

    static Target target(ServerPlayer player, GateKind kind) {
        var server = player.level().getServer();
        if (server == null) {
            return null;
        }
        if (isFirmament(player.level())) {
            CompoundTag back = player.getPersistentData().getCompound(RETURN_KEY)
                    //? if >=26.1 {
                    .orElse(new CompoundTag())
                    //?}
                    ;
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION,
                    Identifier.tryParse(get(back, "dim", "minecraft:overworld")));
            ServerLevel target = server.getLevel(dim);
            if (target == null || !back.contains("x")) {
                target = server.overworld();
                int y = target.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, 0, 0);
                return new Target(target, new Vec3(0.5, y, 0.5), false);
            }
            return new Target(target, new Vec3(getD(back, "x", 0), getD(back, "y", 100), getD(back, "z", 0)), false);
        }
        ServerLevel firmament = server.getLevel(LEVEL);
        if (firmament == null) {
            player.sendSystemMessage(Component.translatable("message.astralvigil.firmament.missing").withStyle(ChatFormatting.RED));
            return null;
        }
        CompoundTag back = new CompoundTag();
        back.putString("dim", player.level().dimension()
                //? if >=26.1 {
                .identifier()
                //?} else {
                /*.location()
                *///?}
                .toString());
        back.putDouble("x", player.getX());
        back.putDouble("y", player.getY());
        back.putDouble("z", player.getZ());
        player.getPersistentData().put(RETURN_KEY, back);
        SanctumBuilder.ensureBuilt(firmament);
        return new Target(firmament, Vec3.atBottomCenterOf(SanctumBuilder.arrival(kind)), true);
    }

    /** Arrival spot on the hub, in front of the return gate. */
    public static final BlockPos ARRIVAL = LANDING.offset(0, 1, 3);

    //? if >=26.1 {
    public static TeleportTransition gateDestination(ServerPlayer player, GateKind kind) {
        Target t = target(player, kind);
        return t == null ? null : new TeleportTransition(t.level(), t.pos(), Vec3.ZERO, player.getYRot(), player.getXRot(),
                TeleportTransition.PLACE_PORTAL_TICKET.then(e -> arrived(e, t)));
    }
    //?} else {
    /*public static DimensionTransition gateDestination(ServerPlayer player, GateKind kind) {
        Target t = target(player, kind);
        return t == null ? null : new DimensionTransition(t.level(), t.pos(), Vec3.ZERO, player.getYRot(), player.getXRot(),
                DimensionTransition.PLACE_PORTAL_TICKET.then(e -> arrived(e, t)));
    }
    *///?}

    private static void arrived(net.minecraft.world.entity.Entity entity, Target t) {
        t.level().playSound(null, BlockPos.containing(t.pos()), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.6f);
        if (t.ascending() && entity instanceof ServerPlayer p) {
            p.sendSystemMessage(Component.translatable("message.astralvigil.firmament.arrive").withStyle(ChatFormatting.AQUA));
        }
    }

    private static String get(CompoundTag tag, String key, String fallback) {
        //? if >=26.1 {
        return tag.getStringOr(key, fallback);
        //?} else {
        /*return tag.contains(key) ? tag.getString(key) : fallback;
        *///?}
    }

    private static double getD(CompoundTag tag, String key, double fallback) {
        //? if >=26.1 {
        return tag.getDoubleOr(key, fallback);
        //?} else {
        /*return tag.contains(key) ? tag.getDouble(key) : fallback;
        *///?}
    }

    static void teleport(ServerPlayer player, ServerLevel level, Vec3 pos) {
        //? if >=26.1 {
        player.teleport(new TeleportTransition(level, pos, Vec3.ZERO, player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING));
        //?} else {
        /*player.changeDimension(new DimensionTransition(level, pos, Vec3.ZERO, player.getYRot(), player.getXRot(), DimensionTransition.DO_NOTHING));
        *///?}
        level.playSound(null, BlockPos.containing(pos), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.6f);
    }

    static final String CHECKPOINT_KEY = "astralvigil_checkpoint";

    /** Low gravity, softer landings, the safety net and the trial hazards while in the Firmament. */
    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        boolean inside = isFirmament(level);
        if (inside) {
            String trial = TrialManager.activeTrial(player);
            if ("solenne".equals(trial)) {
                solenneCheckpoint(player, level);
            } else if ("vael".equals(trial) && player.tickCount % 20 == 0) {
                vaelWaves(player, level);
            }
        }
        if (player.tickCount % 10 != 0) {
            return;
        }
        apply(player.getAttribute(Attributes.GRAVITY), GRAVITY_ID, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, inside);
        apply(player.getAttribute(Attributes.SAFE_FALL_DISTANCE), FALL_ID, 7.0, AttributeModifier.Operation.ADD_VALUE, inside);
        if (inside && shouldCatch(player.getX(), player.getY(), player.getZ(), player.getAbilities().flying)) {
            // the sky beneath the islands (and the void) catches falling players and sets them down at the nearest island
            teleport(player, level, Vec3.atBottomCenterOf(nearestAnchor(player.blockPosition())));
            player.fallDistance = 0;
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING, 100, 0, false, false));
        }
    }

    static BlockPos nearestAnchor(BlockPos from) {
        BlockPos best = ARRIVAL;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : SanctumBuilder.anchors()) {
            double d = p.distSqr(new BlockPos(from.getX(), p.getY(), from.getZ()));
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    /**
     * Sun Spire: the last gold pad stood on is a checkpoint. A fall of more than a few blocks below it is caught by
     * Solenne's light and the climber is set back onto it - the spire is hard, not lethal.
     */
    static void solenneCheckpoint(ServerPlayer player, ServerLevel level) {
        BlockPos centre = SanctumBuilder.centre(Watcher.SOLENNE);
        if (player.blockPosition().distSqr(new BlockPos(centre.getX(), player.getBlockY(), centre.getZ())) > 30 * 30) {
            return;
        }
        var data = player.getPersistentData();
        if (player.onGround() && level.getBlockState(player.blockPosition().below()).is(net.minecraft.world.level.block.Blocks.GOLD_BLOCK)
                && player.getY() > LANDING.getY() + 4) {
            data.putLong(CHECKPOINT_KEY, player.blockPosition().asLong());
            return;
        }
        //? if >=26.1 {
        long packed = data.getLongOr(CHECKPOINT_KEY, 0L);
        //?} else {
        /*long packed = data.getLong(CHECKPOINT_KEY);
        *///?}
        if (packed == 0L || player.fallDistance < 6) {
            return;
        }
        BlockPos checkpoint = BlockPos.of(packed);
        if (player.getY() < checkpoint.getY() - 6) {
            teleport(player, level, Vec3.atBottomCenterOf(checkpoint));
            player.fallDistance = 0;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
        }
    }

    /** Moon Arena: while Vael's trial runs, Eclipse Echoes keep rising out of the arena (at most six at a time). */
    static void vaelWaves(ServerPlayer player, ServerLevel level) {
        BlockPos centre = SanctumBuilder.centre(Watcher.VAEL);
        if (player.blockPosition().distSqr(new BlockPos(centre.getX(), player.getBlockY(), centre.getZ())) > 45 * 45
                || level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL || (player.tickCount / 20) % 12 != 0) {
            return;
        }
        var box = new net.minecraft.world.phys.AABB(centre).inflate(48, 30, 48);
        if (level.getEntitiesOfClass(media.hofferbert.astralvigil.eclipse.EclipseEcho.class, box).size() >= 6) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            double a = level.getRandom().nextDouble() * Math.PI * 2;
            Vec3 at = new Vec3(centre.getX() + 0.5 + Math.cos(a) * 12, centre.getY() + 1, centre.getZ() + 0.5 + Math.sin(a) * 12);
            media.hofferbert.astralvigil.eclipse.EclipseEcho.spawn(level, at, 20 * 60);
        }
    }

    /** Radius around the hub inside which falling below the islands returns you to the nearest island. */
    public static final int CATCH_RADIUS = 720;
    public static final int CATCH_DEPTH = 40;

    /** Pure: is a player at this position falling out of the islands (or into the void)? */
    public static boolean shouldCatch(double x, double y, double z, boolean flying) {
        if (y < -32) {
            return true;
        }
        return !flying && y < LANDING.getY() - CATCH_DEPTH && x * x + z * z < (double) CATCH_RADIUS * CATCH_RADIUS;
    }

    private static void apply(AttributeInstance attribute, Identifier id, double amount, AttributeModifier.Operation op, boolean on) {
        if (attribute == null) {
            return;
        }
        boolean has = attribute.getModifier(id) != null;
        if (on && !has) {
            attribute.addTransientModifier(new AttributeModifier(id, amount, op));
        } else if (!on && has) {
            attribute.removeModifier(id);
        }
    }
}
