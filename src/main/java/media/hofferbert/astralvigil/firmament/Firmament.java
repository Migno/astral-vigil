package media.hofferbert.astralvigil.firmament;

import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.registry.AVBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
//? if >=26.1 {
import net.minecraft.world.level.portal.TeleportTransition;
//?} else {
/*import net.minecraft.world.level.portal.DimensionTransition;
*///?}

/**
 * The Firmament: one realm dimension per gate kind ({@link Realm}), reached only through player-built gates. Low
 * gravity, a safety net under every realm (players and dropped items are set back onto the arrival shrine), realm
 * construction on the first visit, the trial hazards, and protection: trial realms cannot be dug, built on or skipped
 * with ender pearls, and the return gates and shrines cannot be broken anywhere.
 */
public final class Firmament {
    /** The Firmament wilds (Astral Gate). The trial realms have their own keys, see {@link Realm#level()}. */
    public static final ResourceKey<Level> LEVEL = Realm.FIRMAMENT.level();
    static final Identifier GRAVITY_ID = AstralVigil.id("firmament_gravity");
    static final Identifier FALL_ID = AstralVigil.id("firmament_fall");
    static final String RETURN_KEY = "astralvigil_return";

    private Firmament() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(Firmament::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Firmament::onLevelTick);
        NeoForge.EVENT_BUS.addListener(Firmament::onBreak);
        NeoForge.EVENT_BUS.addListener(Firmament::onPlace);
        NeoForge.EVENT_BUS.addListener(Firmament::onPearl);
        FloatingBlocks.register();
    }

    /** True in any realm of the Firmament. */
    public static boolean isFirmament(Level level) {
        return Realm.of(level) != null;
    }

    /** Where a player standing in a gate arrives: in the gate's realm in front of the return gate, or back home. */
    record Target(ServerLevel level, Vec3 pos, Realm realm) {}

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
            if (target == null || !back.contains("x") || Realm.of(dim) != null) {
                target = server.overworld();
                BlockPos spawn = worldSpawn(target);
                return new Target(target, Vec3.atBottomCenterOf(spawn), null);
            }
            return new Target(target, new Vec3(getD(back, "x", 0), getD(back, "y", 100), getD(back, "z", 0)), null);
        }
        Realm realm = Realm.of(kind);
        ServerLevel dest = server.getLevel(realm.level());
        if (dest == null) {
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
        RealmBuilder.ensureBuilt(dest, realm);
        return new Target(dest, Vec3.atBottomCenterOf(realm.arrival()), realm);
    }

    /** Debug/admin: builds a realm if needed and sends the player straight to its arrival shrine. */
    public static boolean visit(ServerPlayer player, Realm realm) {
        Target t = target(player, realm.gate());
        if (t == null || t.realm() == null) {
            // already in a realm: target() leads home; go to the requested realm directly instead
            ServerLevel dest = player.level().getServer().getLevel(realm.level());
            if (dest == null) {
                return false;
            }
            RealmBuilder.ensureBuilt(dest, realm);
            t = new Target(dest, Vec3.atBottomCenterOf(realm.arrival()), realm);
        }
        teleport(player, t.level(), t.pos());
        arrived(player, t);
        return true;
    }

    private static BlockPos worldSpawn(ServerLevel level) {
        //? if >=26.1 {
        BlockPos p = level.getRespawnData().pos();
        //?} else {
        /*BlockPos p = level.getSharedSpawnPos();
        *///?}
        return level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p);
    }

    //? if >=26.1 {
    public static TeleportTransition gateDestination(ServerPlayer player, GateKind kind) {
        Target t = target(player, kind);
        return t == null ? null : new TeleportTransition(t.level(), t.pos(), Vec3.ZERO, t.realm() != null ? 180f : player.getYRot(),
                player.getXRot(), TeleportTransition.PLACE_PORTAL_TICKET.then(e -> arrived(e, t)));
    }
    //?} else {
    /*public static DimensionTransition gateDestination(ServerPlayer player, GateKind kind) {
        Target t = target(player, kind);
        return t == null ? null : new DimensionTransition(t.level(), t.pos(), Vec3.ZERO, t.realm() != null ? 180f : player.getYRot(),
                player.getXRot(), DimensionTransition.PLACE_PORTAL_TICKET.then(e -> arrived(e, t)));
    }
    *///?}

    private static void arrived(net.minecraft.world.entity.Entity entity, Target t) {
        t.level().playSound(null, BlockPos.containing(t.pos()), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.6f);
        if (t.realm() != null && entity instanceof ServerPlayer p) {
            showTitle(p, Component.translatable(t.realm().translationKey()).withStyle(ChatFormatting.GOLD),
                    Component.translatable(t.realm().translationKey() + ".subtitle").withStyle(ChatFormatting.AQUA));
        }
    }

    /** Version-neutral action-bar message. */
    public static void actionBar(Player player, Component message) {
        //? if >=26.1 {
        player.sendOverlayMessage(message);
        //?} else {
        /*player.displayClientMessage(message, true);
        *///?}
    }

    public static void showTitle(ServerPlayer player, Component title, Component subtitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(15, 60, 20));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
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
    static final String CHECKPOINTS_KEY = "astralvigil_checkpoints";

    /** Low gravity, softer landings, the safety net and the trial hazards while in a realm. */
    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        Realm realm = Realm.of(level);
        boolean inside = realm != null;
        if (inside) {
            String trial = TrialManager.activeTrial(player);
            if (realm == Realm.SUNSPIRE) {
                if ("solenne".equals(trial)) {
                    solenneCheckpoint(player, level);
                }
                if (player.isFallFlying() && !player.getAbilities().instabuild) {
                    player.stopFallFlying(); // Solenne's sky does not carry wings of feathers and leather
                }
            } else if (realm == Realm.MOONWELL && "vael".equals(trial) && player.tickCount % 20 == 0) {
                vaelWaves(player, level);
            }
        }
        if (player.tickCount % 10 != 0) {
            return;
        }
        apply(player.getAttribute(Attributes.GRAVITY), GRAVITY_ID, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, inside);
        apply(player.getAttribute(Attributes.SAFE_FALL_DISTANCE), FALL_ID, 7.0, AttributeModifier.Operation.ADD_VALUE, inside);
        if (inside && shouldCatch(realm, player.getY(), player.getAbilities().flying)) {
            // the sky beneath the realm catches falling players and sets them down on the arrival shrine
            teleport(player, level, Vec3.atBottomCenterOf(realm.arrival()));
            player.fallDistance = 0;
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING, 100, 0, false, false));
        }
    }

    /** Dropped items that fall out of a realm (loot, death piles) float back up onto the arrival shrine. */
    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 20 != 0) {
            return;
        }
        Realm realm = Realm.of(level);
        if (realm == null) {
            return;
        }
        for (ItemEntity item : level.getEntities(EntityType.ITEM, e -> shouldCatch(realm, e.getY(), false))) {
            Vec3 to = Vec3.atBottomCenterOf(realm.arrival()).add(0, 0.5, 0);
            item.teleportTo(to.x, to.y, to.z);
            item.setDeltaMovement(Vec3.ZERO);
            item.setUnlimitedLifetime();
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, to.x, to.y, to.z, 8, 0.2, 0.3, 0.2, 0.02);
        }
    }

    /** Blocks that may never be broken in the Firmament: the gates that lead home and the realm markers. */
    static boolean isProtected(BlockState state) {
        return state.getBlock() instanceof media.hofferbert.astralvigil.block.CelestialPortalBlock
                || AVBlocks.GATES.values().stream().anyMatch(g -> state.is(g.get()))
                || state.is(AVBlocks.STARLIGHT_FONT.get());
    }

    /** Trial realms are sacred ground: no digging, no building. The return gates are protected everywhere. */
    //? if >=26.1 {
    private static void onBreak(net.neoforged.neoforge.event.level.block.BreakBlockEvent event) {
    //?} else {
    /*private static void onBreak(BlockEvent.BreakEvent event) {
    *///?}
        Player player = event.getPlayer();
        if (!(player.level() instanceof ServerLevel level) || player.getAbilities().instabuild) {
            return;
        }
        Realm realm = Realm.of(level);
        if (realm != null && (realm.hasTrial() || isProtected(event.getState()))) {
            event.setCanceled(true);
            actionBar(player, Component.translatable("message.astralvigil.firmament.sacred").withStyle(ChatFormatting.GOLD));
        }
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level) || player.getAbilities().instabuild) {
            return;
        }
        Realm realm = Realm.of(level);
        if (realm != null && realm.hasTrial()) {
            event.setCanceled(true);
            actionBar(player, Component.translatable("message.astralvigil.firmament.sacred").withStyle(ChatFormatting.GOLD));
        }
    }

    /** Ender pearls fizzle in the trial realms (the climb and the maze cannot be skipped). */
    private static void onPearl(EntityTeleportEvent.EnderPearl event) {
        Realm realm = Realm.of(event.getEntity().level());
        if (realm != null && realm.hasTrial()) {
            event.setCanceled(true);
        }
    }

    /**
     * Sun Spire: the last gold pad stood on is a checkpoint. A fall of more than a few blocks below it is caught by
     * Solenne's light and the climber is set back onto it - the spire is hard, not lethal. Every gold pad touched is
     * remembered; the crown only counts once all of them have been reached in the climb.
     */
    static void solenneCheckpoint(ServerPlayer player, ServerLevel level) {
        BlockPos centre = Realm.SUNSPIRE.trialCentre();
        if (player.blockPosition().distSqr(new BlockPos(centre.getX(), player.getBlockY(), centre.getZ())) > 30 * 30) {
            return;
        }
        var data = player.getPersistentData();
        if (player.onGround() && level.getBlockState(player.blockPosition().below()).is(net.minecraft.world.level.block.Blocks.GOLD_BLOCK)
                && player.getY() > centre.getY() + 4) {
            long here = player.blockPosition().asLong();
            //? if >=26.1 {
            long last = data.getLongOr(CHECKPOINT_KEY, 0L);
            int reached = data.getIntOr(CHECKPOINTS_KEY, 0);
            //?} else {
            /*long last = data.getLong(CHECKPOINT_KEY);
            int reached = data.getInt(CHECKPOINTS_KEY);
            *///?}
            if (last != here) {
                data.putLong(CHECKPOINT_KEY, here);
                data.putInt(CHECKPOINTS_KEY, reached + 1);
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.4f);
                actionBar(player, Component.translatable("message.astralvigil.trial.checkpoint", reached + 1).withStyle(ChatFormatting.GOLD));
            }
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

    /** Gold pads a climber has reached since the Sun Spire trial began. */
    public static int checkpointsReached(Player player) {
        //? if >=26.1 {
        return player.getPersistentData().getIntOr(CHECKPOINTS_KEY, 0);
        //?} else {
        /*return player.getPersistentData().getInt(CHECKPOINTS_KEY);
        *///?}
    }

    public static void resetCheckpoints(Player player) {
        player.getPersistentData().remove(CHECKPOINT_KEY);
        player.getPersistentData().remove(CHECKPOINTS_KEY);
    }

    /** Moon Arena: while Vael's trial runs, Eclipse Echoes keep rising out of the arena (at most six at a time). */
    static void vaelWaves(ServerPlayer player, ServerLevel level) {
        BlockPos centre = Realm.MOONWELL.trialCentre();
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

    /** How far below a realm's shrine the safety net catches (the wilds: below the lowest islands). */
    public static final int CATCH_DEPTH = 40;

    /** Pure: is something at height {@code y} in this realm falling out of it? */
    public static boolean shouldCatch(Realm realm, double y, boolean flying) {
        if (y < -32) {
            return true;
        }
        if (flying) {
            return false;
        }
        return realm == Realm.FIRMAMENT ? y < 0 : y < realm.pad().getY() - CATCH_DEPTH;
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
