package media.hofferbert.astralvigil.eclipse;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.config.AVServerConfig;
import media.hofferbert.astralvigil.eclipse.EclipseMove.Stage;
import media.hofferbert.astralvigil.registry.AVEntities;
import media.hofferbert.astralvigil.registry.AVItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
//? if >=26.1 {
import com.geckolib.animatable.manager.AnimatableManager;
//?} else {
/*import com.geckolib.animation.AnimatableManager;
*///?}

/**
 * The Eclipse, fallen fifth Watcher – the Astral Vigil end boss.
 *
 * <p>A single move state machine ({@link EclipseBrain.Timer}) drives everything: an attack runs wind-up → active →
 * recovery and never overlaps another; specials (entrance, phase transitions, shield, enrage, stagger, grounded) are
 * moves too. Move and stage are synced to clients, which pick the matching GeckoLib animation and draw all telegraphs
 * (aim lines, markers, rays, shockwaves) themselves.
 *
 * <ol>
 *   <li><b>Fallen Light</b>: Umbral Sweep, Void Lance, Falling Stars.</li>
 *   <li><b>Shattered Halo</b>: raises a shield held by Umbral Pylons (destroy them → stagger); adds Echoes and the
 *   Shadow Dive.</li>
 *   <li><b>Total Eclipse</b>: raises cover walls, adds the Eclipse Ray, Umbral Nova and Blink; enrages at low health.</li>
 * </ol>
 * Heavy moves leave it grounded for a few seconds (the melee window). Leashed to its spawn point; resets when nobody is
 * near; drags hiding or fleeing targets back with the Umbral Grasp. Damage can never skip a phase.
 */
public class EclipseEntity extends Monster implements GeoEntity {
    public static final int DEATH_TICKS = 100;
    public static final double BEAM_LENGTH = 24;
    /** The ray emerges from the ground this far from the arena centre (clear of a dais) and hugs the floor from there. */
    public static final double BEAM_START = 6.5;
    public static final double WAVE_SPEED = 0.9;
    static final int LANCE_LOCK = 8;
    static final int DIVE_LOCK = 10;
    static final int SWEEP_LOCK = 6;
    static final int STAR_DELAY = 30;
    static final int WAVE_TICKS = 14;

    private static final EntityDataAccessor<Integer> DATA_MOVE = SynchedEntityData.defineId(EclipseEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STAGE = SynchedEntityData.defineId(EclipseEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_FLAGS = SynchedEntityData.defineId(EclipseEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> DATA_AIM_X = SynchedEntityData.defineId(EclipseEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_AIM_Y = SynchedEntityData.defineId(EclipseEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_AIM_Z = SynchedEntityData.defineId(EclipseEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_YAW = SynchedEntityData.defineId(EclipseEntity.class, EntityDataSerializers.FLOAT);
    static final byte FLAG_SHIELD = 1;
    static final byte FLAG_ENRAGED = 2;
    static final byte FLAG_TOTAL = 4;
    static final byte FLAG_COUNTERCLOCKWISE = 8;

    private static final Map<String, RawAnimation> ANIMATIONS = animations();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossBar = createBossBar();
    private final EclipseBrain.Timer timer = new EclipseBrain.Timer();
    private final int[] cooldowns = new int[EclipseMove.values().length];
    private final List<BlockPos> temporaryBlocks = new ArrayList<>();
    private final List<BlockPos> pendingCover = new ArrayList<>();
    private final Set<UUID> participants = new HashSet<>();
    private final List<Star> stars = new ArrayList<>();
    private final Set<UUID> waveHit = new HashSet<>();

    // saved
    private boolean initialised;
    private Vec3 home;
    private int phase;
    private int shield; // 0 not raised yet, 1 up, 2 broken
    private boolean coverRaised;
    private boolean enraged;
    private UUID summoner;

    // transient
    private double homeGround = Double.NaN;
    private EclipseMove lastMove = EclipseMove.NONE;
    private LivingEntity moveTarget;
    private int gap = 30;
    private int noPlayerTicks;
    private int cheeseTicks;
    private boolean cheese;
    private Vec3 aim = Vec3.ZERO;
    private float yaw;
    private float beamDir = 1;
    private Vec3 diveFrom;
    private int shieldFeedback;
    private String barKey = "";

    // client
    private int clientMove = -1;
    private int clientStage = -1;
    private int clientStageTicks;

    private record Star(Vec3 pos, int[] timer) {}

    public EclipseEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
        xpReward = 500;
    }

    private ServerBossEvent createBossBar() {
        //? if >=26.1 {
        ServerBossEvent bar = new ServerBossEvent(getUUID(), getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
        //?} else {
        /*ServerBossEvent bar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
        *///?}
        bar.setDarkenScreen(true);
        bar.setCreateWorldFog(true);
        return bar;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MOVE, 0);
        builder.define(DATA_STAGE, 0);
        builder.define(DATA_FLAGS, (byte) 0);
        builder.define(DATA_AIM_X, 0f);
        builder.define(DATA_AIM_Y, 0f);
        builder.define(DATA_AIM_Z, 0f);
        builder.define(DATA_YAW, 0f);
    }

    @Override
    protected void registerGoals() {
        // targeting is handled in tick(): only players inside the arena count
    }

    // ------------------------------------------------------------------ queries (also used by GameTests)

    /** 1, 2 or 3 – from the health fraction. */
    public static int phaseFor(float health, float max) {
        return EclipsePhase.of(health, max);
    }

    public int phase() {
        return phase == 0 ? phaseFor(getHealth(), getMaxHealth()) : phase;
    }

    public boolean inTransition() {
        EclipseMove m = currentMove();
        return m == EclipseMove.TRANSITION_2 || m == EclipseMove.TRANSITION_3;
    }

    /** The running move (synced, valid on both sides). */
    public EclipseMove currentMove() {
        return EclipseMove.byId(entityData.get(DATA_MOVE));
    }

    public Stage currentStage() {
        int s = entityData.get(DATA_STAGE);
        return s >= 0 && s < Stage.values().length ? Stage.values()[s] : Stage.NONE;
    }

    public boolean shielded() {
        return (entityData.get(DATA_FLAGS) & FLAG_SHIELD) != 0;
    }

    public boolean enraged() {
        return (entityData.get(DATA_FLAGS) & FLAG_ENRAGED) != 0;
    }

    public List<BlockPos> temporaryBlocks() {
        return List.copyOf(temporaryBlocks);
    }

    public Vec3 home() {
        return home == null ? position() : home;
    }

    Vec3 aim() {
        return new Vec3(entityData.get(DATA_AIM_X), entityData.get(DATA_AIM_Y), entityData.get(DATA_AIM_Z));
    }

    float syncedYaw() {
        return entityData.get(DATA_YAW);
    }

    // ------------------------------------------------------------------ server tick

    @Override
    public void tick() {
        noPhysics = true;
        super.tick();
        if (level().isClientSide()) {
            clientTick();
            return;
        }
        if (!(level() instanceof ServerLevel server) || isDeadOrDying()) {
            return;
        }
        if (!initialised) {
            initialise(server);
        }
        if (home == null) {
            home = position();
        }
        if (Double.isNaN(homeGround)) {
            double g = groundY(home.x, home.z, home.y + 2, 40);
            homeGround = Double.isNaN(g) ? home.y : g;
        }
        if (tickArena(server)) {
            return;
        }
        tickPhase(server);
        tickShield(server);
        tickHazards(server);
        for (int i = 0; i < cooldowns.length; i++) {
            if (cooldowns[i] > 0) {
                cooldowns[i]--;
            }
        }
        LivingEntity target = updateTarget(server);
        tickCheese(target);
        tickMove(server, target);
        tickMovement(target);
        setInvulnerable(shield == 1 || switch (timer.move()) {
            case ENTRANCE, TRANSITION_2, TRANSITION_3, SHIELD, ENRAGE -> true;
            default -> false;
        });
        if (shieldFeedback > 0) {
            shieldFeedback--;
        }
        updateBossBar();
        sync();
    }

    private void initialise(ServerLevel level) {
        initialised = true;
        home = position();
        int players = Math.max(1, players(level, 48).size());
        double health = AVServerConfig.ECLIPSE_HEALTH.get() * (1 + AVServerConfig.ECLIPSE_HEALTH_PER_PLAYER.get() * (players - 1));
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        getAttribute(Attributes.ARMOR).setBaseValue(AVServerConfig.ECLIPSE_ARMOR.get());
        setHealth((float) health);
        phase = 1;
        begin(level, EclipseMove.ENTRANCE, null);
    }

    private void sync() {
        entityData.set(DATA_MOVE, timer.move().ordinal());
        entityData.set(DATA_STAGE, timer.stage().ordinal());
        byte flags = (byte) ((shield == 1 ? FLAG_SHIELD : 0) | (enraged ? FLAG_ENRAGED : 0) | (phase >= 3 ? FLAG_TOTAL : 0)
                | (beamDir < 0 ? FLAG_COUNTERCLOCKWISE : 0));
        entityData.set(DATA_FLAGS, flags);
        entityData.set(DATA_AIM_X, (float) aim.x);
        entityData.set(DATA_AIM_Y, (float) aim.y);
        entityData.set(DATA_AIM_Z, (float) aim.z);
        entityData.set(DATA_YAW, yaw);
    }

    /** Players taking part: alive, not spectating, within {@code range} of the spawn point. */
    private List<ServerPlayer> players(ServerLevel level, double range) {
        Vec3 c = home();
        return level.getEntitiesOfClass(ServerPlayer.class, new AABB(c, c).inflate(range),
                p -> p.isAlive() && !p.isSpectator() && p.distanceToSqr(c) <= range * range);
    }

    /** @return true if the fight was reset this tick */
    private boolean tickArena(ServerLevel level) {
        if (players(level, AVServerConfig.ECLIPSE_RESET_RANGE.get()).isEmpty()) {
            noPlayerTicks++;
            if (EclipseBrain.shouldReset(noPlayerTicks, AVServerConfig.ECLIPSE_RESET_SECONDS.get() * 20)) {
                resetFight();
                return true;
            }
        } else {
            noPlayerTicks = 0;
        }
        return false;
    }

    /** Full reset: healed, back at its spawn point, summons and temporary blocks gone, phase 1 again. */
    public void resetFight() {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        timer.stop();
        stars.clear();
        pendingCover.clear();
        cleanup(level);
        shield = 0;
        coverRaised = false;
        enraged = false;
        phase = 1;
        Arrays.fill(cooldowns, 0);
        lastMove = EclipseMove.NONE;
        cheeseTicks = 0;
        cheese = false;
        noPlayerTicks = 0;
        gap = 40;
        setHealth(getMaxHealth());
        setTarget(null);
        Vec3 h = home();
        teleportTo(h.x, h.y, h.z);
        setDeltaMovement(Vec3.ZERO);
        setInvulnerable(false);
        participants.clear();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, h.x, h.y + 2, h.z, 80, 1, 2, 1, 0.1);
        AstralVigil.LOGGER.info("The Eclipse at {} reset (no players nearby)", BlockPos.containing(h));
        updateBossBar();
        sync();
    }

    private void tickPhase(ServerLevel level) {
        int now = phaseFor(getHealth(), getMaxHealth());
        if (phase == 0) {
            phase = now;
        } else if (now > phase && !inTransitionMove()) {
            phase++; // one phase at a time, even after burst damage
            interrupt();
            begin(level, phase == 2 ? EclipseMove.TRANSITION_2 : EclipseMove.TRANSITION_3, null);
            return;
        }
        if (timer.running()) {
            return;
        }
        // catch-up after loading or a transition: shield in phase 2, cover in phase 3, enrage near the end
        if (phase >= 2 && shield == 0) {
            if (AVServerConfig.ECLIPSE_PYLON_COUNT.get() > 0) {
                begin(level, EclipseMove.SHIELD, null);
                return;
            }
            shield = 2;
        }
        if (phase >= 3 && !coverRaised) {
            planCover(level);
            flushCover(level);
        }
        if (phase >= 3 && !enraged && getHealth() / getMaxHealth() < AVServerConfig.ECLIPSE_ENRAGE_THRESHOLD.get()) {
            enraged = true;
            begin(level, EclipseMove.ENRAGE, null);
        }
    }

    private boolean inTransitionMove() {
        return timer.move() == EclipseMove.TRANSITION_2 || timer.move() == EclipseMove.TRANSITION_3;
    }

    private void tickShield(ServerLevel level) {
        if (shield != 1 || tickCount % 10 != 0) {
            return;
        }
        List<UmbralPylon> pylons = pylons(level);
        if (pylons.isEmpty()) {
            shield = 2;
            interrupt();
            begin(level, EclipseMove.STAGGER, null);
            return;
        }
        float heal = (float) (pylons.size() * AVServerConfig.ECLIPSE_PYLON_HEAL.get() / 2.0);
        float cap = getMaxHealth() * 2f / 3f - 1f; // pylons never lift it back into phase 1
        if (heal > 0 && getHealth() < cap) {
            setHealth(Math.min(cap, getHealth() + heal));
        }
    }

    private List<UmbralPylon> pylons(ServerLevel level) {
        Vec3 c = home();
        return level.getEntitiesOfClass(UmbralPylon.class, new AABB(c, c).inflate(AVServerConfig.ECLIPSE_LEASH.get() + 8),
                p -> p.isAlive() && p.ownedBy(this));
    }

    private LivingEntity updateTarget(ServerLevel level) {
        LivingEntity t = getTarget();
        double range = AVServerConfig.ECLIPSE_RESET_RANGE.get();
        if (t instanceof Player p && (!p.isAlive() || p.isSpectator() || p.isCreative() || p.distanceToSqr(home()) > range * range)) {
            t = null;
        } else if (t != null && !(t instanceof Player)) {
            t = null;
        }
        if (t == null || tickCount % 40 == 0) {
            ServerPlayer best = null;
            double bestD = Double.MAX_VALUE;
            for (ServerPlayer p : players(level, AVServerConfig.ECLIPSE_LEASH.get() + 8)) {
                if (p.isCreative()) {
                    continue;
                }
                double d = p.distanceToSqr(this);
                if (d < bestD) {
                    bestD = d;
                    best = p;
                }
            }
            if (t == null || best != null && bestD + 36 < t.distanceToSqr(this)) {
                t = best;
            }
        }
        setTarget(t);
        return t;
    }

    /** Anti-cheese: hiding out of sight or fleeing past the leash for too long earns an Umbral Grasp. */
    private void tickCheese(LivingEntity target) {
        if (target == null || timer.move() == EclipseMove.GRASP) {
            cheeseTicks = 0;
            return;
        }
        boolean away = horizontal(target.position(), home()) > AVServerConfig.ECLIPSE_LEASH.get();
        boolean hidden = !hasLineOfSight(target);
        cheeseTicks = away || hidden ? cheeseTicks + 1 : Math.max(0, cheeseTicks - 2);
        if (cheeseTicks >= AVServerConfig.ECLIPSE_CHEESE_SECONDS.get() * 20) {
            cheese = true;
            cheeseTicks = 0;
        }
    }

    // ------------------------------------------------------------------ the move state machine

    private void tickMove(ServerLevel level, LivingEntity target) {
        if (timer.running()) {
            during(level, timer.move(), timer.stage(), target());
            EclipseMove before = timer.move();
            if (timer.tick()) {
                if (timer.running()) {
                    enter(level, timer.move(), timer.stage(), target());
                } else {
                    finish(level, before);
                }
            }
            return;
        }
        if (target == null) {
            return;
        }
        if (cheese && cooldowns[EclipseMove.GRASP.ordinal()] <= 0) {
            cheese = false;
            begin(level, EclipseMove.GRASP, target);
            return;
        }
        if (--gap > 0) {
            return;
        }
        EclipseMove next = EclipseBrain.pick(phase(), shield == 1, distanceTo(target), cooldowns, lastMove, random::nextInt);
        if (next == EclipseMove.NONE) {
            gap = 10;
        } else {
            begin(level, next, target);
        }
    }

    /** Starts a move now (GameTests use it to drive the state machine). */
    public void forceMove(EclipseMove move) {
        if (level() instanceof ServerLevel level) {
            interrupt();
            begin(level, move, getTarget());
            sync();
        }
    }

    private void begin(ServerLevel level, EclipseMove move, LivingEntity target) {
        moveTarget = target;
        timer.start(move);
        if (move.kind() == EclipseMove.Kind.ATTACK || move.kind() == EclipseMove.Kind.FORCED) {
            lastMove = move;
            cooldowns[move.ordinal()] = EclipseBrain.cooldown(move, phase(), enraged, AVServerConfig.ECLIPSE_ENRAGE_TEMPO.get());
        }
        if (timer.running()) {
            enter(level, timer.move(), timer.stage(), target());
        }
    }

    /** Cancels the running move and pending hazards (transitions: no attacks in flight). */
    private void interrupt() {
        timer.stop();
        stars.clear();
        waveHit.clear();
    }

    private LivingEntity target() {
        if (moveTarget != null && moveTarget.isAlive() && !moveTarget.isRemoved()) {
            return moveTarget;
        }
        return getTarget();
    }

    private void finish(ServerLevel level, EclipseMove move) {
        gap = EclipseBrain.gap(AVServerConfig.ECLIPSE_MOVE_GAP.get(), phase(), enraged, AVServerConfig.ECLIPSE_ENRAGE_TEMPO.get());
        switch (move) {
            case SHIELD -> raiseShield(level);
            case TRANSITION_3 -> flushCover(level);
            case BLINK -> {
                gap = 4;
                cooldowns[EclipseMove.SWEEP.ordinal()] = 0;
            }
            case DIVE, NOVA, BEAM -> begin(level, EclipseMove.GROUNDED, null);
            case LANCE -> {
                if (shield != 1 && random.nextBoolean()) {
                    begin(level, EclipseMove.GROUNDED, null);
                }
            }
            default -> {}
        }
    }

    private void enter(ServerLevel level, EclipseMove move, Stage stage, LivingEntity target) {
        switch (move) {
            case SWEEP -> {
                if (stage == Stage.WINDUP) {
                    cue(level, move);
                    if (target != null) {
                        yaw = yawTo(target.position());
                    }
                    sound(level, SoundEvents.PHANTOM_SWOOP, 2.0f, 0.5f);
                } else if (stage == Stage.ACTIVE) {
                    sweep(level);
                }
            }
            case LANCE -> {
                if (stage == Stage.WINDUP) {
                    cue(level, move);
                    if (target != null) {
                        aim = target.getEyePosition();
                    }
                    sound(level, SoundEvents.BEACON_POWER_SELECT, 2.0f, 0.5f);
                } else if (stage == Stage.ACTIVE) {
                    fireLance(level);
                }
            }
            case STARFALL -> {
                if (stage == Stage.WINDUP) {
                    cue(level, move);
                    sound(level, SoundEvents.ILLUSIONER_CAST_SPELL, 2.0f, 0.6f);
                } else if (stage == Stage.ACTIVE) {
                    placeStars(level, target);
                }
            }
            case SUMMON -> {
                if (stage == Stage.WINDUP) {
                    cue(level, move);
                    sound(level, SoundEvents.EVOKER_PREPARE_SUMMON, 2.0f, 0.6f);
                } else if (stage == Stage.ACTIVE) {
                    summonEchoes(level, target);
                }
            }
            case DIVE -> {
                if (stage == Stage.WINDUP) {
                    cue(level, move);
                    aim = groundUnder(target != null ? target.position() : position());
                    sound(level, SoundEvents.ENDER_DRAGON_FLAP, 3.0f, 0.5f);
                } else if (stage == Stage.ACTIVE) {
                    diveFrom = position();
                    sound(level, SoundEvents.PHANTOM_SWOOP, 3.0f, 0.4f);
                } else if (stage == Stage.RECOVERY) {
                    diveImpact(level);
                }
            }
            case BEAM -> {
                if (stage == Stage.WINDUP) {
                    cue(level, move);
                    beamDir = random.nextBoolean() ? 1 : -1;
                    float toTarget = target != null ? yawTo(target.position(), home()) : 0;
                    yaw = Mth.wrapDegrees(toTarget - beamDir * 100f);
                    aim = new Vec3(home().x, homeGround + 0.55, home().z);
                    sound(level, SoundEvents.BEACON_ACTIVATE, 3.0f, 0.5f);
                    sound(level, SoundEvents.WARDEN_SONIC_CHARGE, 2.0f, 0.5f);
                } else if (stage == Stage.ACTIVE) {
                    sound(level, SoundEvents.WARDEN_SONIC_BOOM, 3.0f, 0.5f);
                } else if (stage == Stage.RECOVERY) {
                    sound(level, SoundEvents.BEACON_DEACTIVATE, 3.0f, 0.5f);
                }
            }
            case NOVA -> {
                if (stage == Stage.WINDUP) {
                    for (ServerPlayer p : players(level, 48)) {
                        EclipseFx.title(p, name("attack.astralvigil.eclipse.nova", ChatFormatting.DARK_PURPLE),
                                Component.translatable("message.astralvigil.eclipse.nova").withStyle(ChatFormatting.LIGHT_PURPLE));
                        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
                    }
                    sound(level, SoundEvents.WARDEN_SONIC_CHARGE, 3.0f, 0.4f);
                } else if (stage == Stage.ACTIVE) {
                    nova(level);
                }
            }
            case BLINK -> {
                if (stage == Stage.WINDUP) {
                    sound(level, SoundEvents.ENDERMAN_TELEPORT, 1.5f, 0.4f);
                } else if (stage == Stage.ACTIVE) {
                    blink(level, target);
                }
            }
            case GRASP -> {
                if (stage == Stage.WINDUP) {
                    if (target instanceof ServerPlayer sp) {
                        EclipseFx.actionBar(sp, Component.translatable("message.astralvigil.eclipse.grasp").withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                    cue(level, move);
                    sound(level, SoundEvents.SCULK_SHRIEKER_SHRIEK, 2.0f, 0.5f);
                } else if (stage == Stage.ACTIVE) {
                    grasp(level, target);
                }
            }
            case ENTRANCE -> {
                sound(level, SoundEvents.END_PORTAL_SPAWN, 3.0f, 0.5f);
                sound(level, SoundEvents.WARDEN_EMERGE, 3.0f, 0.6f);
                for (ServerPlayer p : players(level, 64)) {
                    p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
                    EclipseFx.title(p, getDisplayName().copy().withStyle(ChatFormatting.DARK_PURPLE),
                            Component.translatable("boss.astralvigil.eclipse.phase.1").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
            case TRANSITION_2, TRANSITION_3 -> transition(level, move == EclipseMove.TRANSITION_3);
            case SHIELD -> sound(level, SoundEvents.RESPAWN_ANCHOR_CHARGE, 3.0f, 0.5f);
            case ENRAGE -> {
                sound(level, SoundEvents.WARDEN_ROAR, 3.0f, 0.6f);
                for (ServerPlayer p : players(level, 64)) {
                    EclipseFx.title(p, Component.translatable("boss.astralvigil.eclipse.phase.enraged").withStyle(ChatFormatting.DARK_RED),
                            Component.translatable("message.astralvigil.eclipse.enraged").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
            case STAGGER -> {
                sound(level, SoundEvents.GLASS_BREAK, 3.0f, 0.5f);
                sound(level, SoundEvents.WITHER_BREAK_BLOCK, 2.0f, 0.6f);
                level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 3, getZ(), 120, 2, 2.5, 2, 0.15);
                for (ServerPlayer p : players(level, 64)) {
                    EclipseFx.actionBar(p, Component.translatable("message.astralvigil.eclipse.shield_broken").withStyle(ChatFormatting.GOLD));
                }
            }
            case GROUNDED -> {
                sound(level, SoundEvents.MACE_SMASH_GROUND_HEAVY, 2.0f, 0.6f);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.3, getZ(), 20, 1.2, 0.2, 1.2, 0.02);
                for (ServerPlayer p : players(level, 48)) {
                    EclipseFx.actionBar(p, Component.translatable("message.astralvigil.eclipse.grounded").withStyle(ChatFormatting.GOLD));
                }
            }
            default -> {}
        }
    }

    private void during(ServerLevel level, EclipseMove move, Stage stage, LivingEntity target) {
        switch (move) {
            case SWEEP -> {
                if (stage == Stage.WINDUP && target != null && timer.left() > SWEEP_LOCK) {
                    yaw = yawTo(target.position());
                } else if (stage == Stage.WINDUP && timer.left() == SWEEP_LOCK) {
                    sound(level, SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0f, 0.5f);
                }
            }
            case LANCE -> {
                if (stage == Stage.WINDUP && target != null && timer.left() > LANCE_LOCK) {
                    aim = target.getEyePosition();
                } else if (stage == Stage.WINDUP && timer.left() == LANCE_LOCK) {
                    sound(level, SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0f, 1.6f);
                }
            }
            case DIVE -> {
                if (stage == Stage.WINDUP && target != null && timer.left() > DIVE_LOCK) {
                    aim = groundUnder(target.position());
                } else if (stage == Stage.WINDUP && timer.left() == DIVE_LOCK) {
                    sound(level, SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0f, 0.8f);
                } else if (stage == Stage.ACTIVE && diveFrom != null) {
                    double f = (timer.elapsed() + 1.0) / EclipseMove.DIVE.ticks(Stage.ACTIVE);
                    Vec3 p = diveFrom.lerp(aim, Math.min(1, f * f));
                    setPos(p.x, p.y, p.z);
                    setDeltaMovement(Vec3.ZERO);
                } else if (stage == Stage.RECOVERY) {
                    tickShockwave(level, timer.elapsed());
                }
            }
            case BEAM -> {
                if (stage == Stage.ACTIVE) {
                    float prev = yaw;
                    yaw = Mth.wrapDegrees(yaw + beamDir * 360f / EclipseMove.BEAM.ticks(Stage.ACTIVE));
                    beamHits(level, prev, yaw);
                    if (timer.elapsed() % 20 == 0) {
                        sound(level, SoundEvents.BEACON_AMBIENT, 3.0f, 0.5f);
                    }
                }
            }
            case GRASP -> {
                if (stage == Stage.WINDUP && target != null) {
                    aim = target.position().add(0, target.getBbHeight() * 0.5, 0);
                }
            }
            case NOVA -> {
                if (stage == Stage.WINDUP && timer.elapsed() % 10 == 0) {
                    sound(level, SoundEvents.WARDEN_HEARTBEAT, 3.0f, 0.6f);
                }
            }
            case TRANSITION_3 -> {
                if (tickCount % 3 == 0) {
                    placeCover(level, 2);
                }
            }
            case ENTRANCE -> {
                if (timer.elapsed() == 50) {
                    sound(level, SoundEvents.WITHER_SPAWN, 2.5f, 0.7f);
                    level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 3, getZ(), 1, 0, 0, 0, 0);
                }
            }
            default -> {}
        }
    }

    // ------------------------------------------------------------------ movement

    private void tickMovement(LivingEntity target) {
        EclipseMove m = timer.move();
        Stage s = timer.stage();
        Vec3 goal = null;
        double speed = getAttributeValue(Attributes.MOVEMENT_SPEED) * (phase() >= 3 ? 1.25 : 1.0) * (enraged ? 1.2 : 1.0);
        Vec3 h = home();
        switch (m) {
            case NONE -> {
                if (shield == 1) {
                    goal = new Vec3(h.x, homeGround + 7, h.z);
                } else if (target != null) {
                    goal = orbit(target, 3.6, 0.1);
                } else {
                    goal = new Vec3(h.x, homeGround + 1.5, h.z);
                }
            }
            case SWEEP -> {
                if (s == Stage.WINDUP && target != null) {
                    goal = towards(target, 2.8, 0.0);
                    speed *= 1.6;
                } else if (s == Stage.ACTIVE) {
                    goal = position().add(dir(yaw).scale(2.5));
                    speed *= 2.5;
                }
            }
            case LANCE -> {
                if (s == Stage.WINDUP && target != null) {
                    goal = towards(target, 9, 3);
                }
            }
            case STARFALL -> {
                if (s == Stage.WINDUP) {
                    goal = new Vec3(getX(), groundY(getX(), getZ(), getY() + 2, 24) + 3, getZ());
                }
            }
            case DIVE -> {
                if (s == Stage.WINDUP) {
                    goal = new Vec3(getX(), Math.max(homeGround, aim.y) + 11, getZ());
                    speed *= 1.5;
                } else {
                    return; // ACTIVE is scripted, RECOVERY sits in the crater
                }
            }
            case BEAM, NOVA -> {
                if (s == Stage.WINDUP) {
                    goal = new Vec3(h.x, homeGround + (m == EclipseMove.BEAM ? 0.1 : 0.4), h.z);
                    speed *= 2;
                }
            }
            case GROUNDED, STAGGER -> goal = new Vec3(getX(), groundY(getX(), getZ(), getY() + 2, 24) + 0.05, getZ());
            case TRANSITION_2, TRANSITION_3, ENRAGE, SHIELD -> goal = new Vec3(getX(), groundY(getX(), getZ(), getY() + 2, 24) + 2, getZ());
            default -> {}
        }
        // facing
        if (m == EclipseMove.SWEEP && s != Stage.WINDUP || m == EclipseMove.SWEEP && timer.left() <= SWEEP_LOCK
                || m == EclipseMove.BEAM && s == Stage.ACTIVE) {
            face(yaw, 40);
        } else if (target != null && m != EclipseMove.STAGGER && m != EclipseMove.GROUNDED) {
            face(yawTo(target.position()), 12);
        }
        if (goal == null || Double.isNaN(goal.y)) {
            setDeltaMovement(getDeltaMovement().scale(0.5));
            return;
        }
        goal = leash(goal);
        Vec3 delta = goal.subtract(position());
        double len = delta.length();
        Vec3 want = len > 0.05 ? delta.scale(Math.min(speed, len * 0.35) / len) : Vec3.ZERO;
        setDeltaMovement(getDeltaMovement().scale(0.45).add(want.scale(0.55)));
    }

    /** Keeps a movement goal inside the leash and never above the void. */
    private Vec3 leash(Vec3 goal) {
        Vec3 h = home();
        double max = AVServerConfig.ECLIPSE_LEASH.get() - 2;
        double d = horizontal(goal, h);
        if (d > max) {
            Vec3 off = new Vec3(goal.x - h.x, 0, goal.z - h.z).scale(max / d);
            goal = new Vec3(h.x + off.x, goal.y, h.z + off.z);
        }
        for (int i = 0; i < 4 && Double.isNaN(groundY(goal.x, goal.z, Math.max(goal.y, homeGround) + 4, 48)); i++) {
            goal = new Vec3((goal.x + h.x) / 2, goal.y, (goal.z + h.z) / 2);
        }
        return goal;
    }

    private Vec3 orbit(LivingEntity target, double radius, double height) {
        double a = Math.atan2(getZ() - target.getZ(), getX() - target.getX()) + 0.035;
        return new Vec3(target.getX() + Math.cos(a) * radius, target.getY() + height, target.getZ() + Math.sin(a) * radius);
    }

    private Vec3 towards(LivingEntity target, double distance, double height) {
        Vec3 off = new Vec3(getX() - target.getX(), 0, getZ() - target.getZ());
        off = off.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : off.normalize();
        return target.position().add(off.scale(distance)).add(0, height, 0);
    }

    private void face(float targetYaw, float step) {
        float y = getYRot() + Mth.clamp(Mth.wrapDegrees(targetYaw - getYRot()), -step, step);
        setYRot(y);
        yBodyRot = y;
        yHeadRot = y;
    }

    static Vec3 dir(float yaw) {
        double r = Math.toRadians(yaw);
        return new Vec3(-Math.sin(r), 0, Math.cos(r));
    }

    private float yawTo(Vec3 p) {
        return yawTo(p, position());
    }

    static float yawTo(Vec3 p, Vec3 from) {
        return (float) (Math.toDegrees(Math.atan2(p.z - from.z, p.x - from.x)) - 90.0);
    }

    static double horizontal(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Top of the first collidable block at or below {@code fromY}, or NaN (void) within {@code depth} blocks. */
    double groundY(double x, double z, double fromY, int depth) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(Mth.floor(x), Mth.floor(fromY), Mth.floor(z));
        for (int i = 0; i < depth; i++) {
            BlockState s = level().getBlockState(p);
            if (!s.getCollisionShape(level(), p).isEmpty()) {
                return p.getY() + 1;
            }
            p.move(0, -1, 0);
        }
        return Double.NaN;
    }

    private Vec3 groundUnder(Vec3 pos) {
        double g = groundY(pos.x, pos.z, pos.y + 1, 16);
        return new Vec3(pos.x, Double.isNaN(g) ? pos.y : g, pos.z);
    }

    // ------------------------------------------------------------------ attacks

    private float damage(ModConfigDouble value) {
        return (float) (value.get() * (enraged ? AVServerConfig.ECLIPSE_ENRAGE_DAMAGE.get() : 1.0));
    }

    @FunctionalInterface
    private interface ModConfigDouble {
        double get();
    }

    private void sweep(ServerLevel level) {
        Vec3 c = position();
        float dmg = damage(AVServerConfig.ECLIPSE_SWEEP_DAMAGE::get);
        for (ServerPlayer p : players(level, 9)) {
            double d = horizontal(p.position(), c);
            double dy = p.getY() - getY();
            float diff = Math.abs(Mth.wrapDegrees(yawTo(p.position()) - yaw));
            if (d <= 7.5 && dy > -2.5 && dy < 5.5 && (diff <= 65 || d < 1.5)) {
                hurt(level, p, damageSources().mobAttack(this), dmg);
                Vec3 push = new Vec3(p.getX() - c.x, 0, p.getZ() - c.z).normalize().scale(0.9);
                p.push(push.x, 0.35, push.z);
                p.hurtMarked = true;
            }
        }
        for (int a = -60; a <= 60; a += 20) {
            Vec3 at = c.add(dir(yaw + a).scale(4.5)).add(0, 1.2, 0);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
        sound(level, SoundEvents.PLAYER_ATTACK_SWEEP, 2.5f, 0.5f);
        sound(level, SoundEvents.WITHER_SHOOT, 1.0f, 0.6f);
    }

    private void fireLance(ServerLevel level) {
        Vec3 from = getEyePosition();
        Vec3 d = aim.subtract(from);
        if (d.lengthSqr() < 1e-4) {
            return;
        }
        Vec3 dir = d.normalize();
        Vec3 to = from.add(dir.scale(40));
        HitResult wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (wall.getType() != HitResult.Type.MISS) {
            to = wall.getLocation();
        }
        float dmg = damage(AVServerConfig.ECLIPSE_LANCE_DAMAGE::get);
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(from, to).inflate(1.0))) {
            if (player.getBoundingBox().inflate(0.3).clip(from, to).isPresent()) {
                hurt(level, player, damageSources().indirectMagic(this, this), dmg);
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION, to.x, to.y, to.z, 1, 0, 0, 0, 0);
        sound(level, SoundEvents.WARDEN_SONIC_BOOM, 2.0f, 1.4f);
    }

    private void placeStars(ServerLevel level, LivingEntity target) {
        int count = Math.min(6, 2 + phase());
        List<Vec3> spots = new ArrayList<>();
        for (ServerPlayer p : players(level, AVServerConfig.ECLIPSE_LEASH.get())) {
            if (!p.isCreative() && spots.size() < 4) {
                spots.add(groundUnder(p.position()));
            }
        }
        Vec3 centre = target != null ? target.position() : home();
        for (int i = 0; spots.size() < count && i < count * 3; i++) {
            Vec3 at = centre.add(random.nextInt(13) - 6, 1, random.nextInt(13) - 6);
            double g = groundY(at.x, at.z, at.y + 3, 12);
            if (!Double.isNaN(g)) {
                spots.add(new Vec3(at.x, g, at.z));
            }
        }
        for (Vec3 s : spots) {
            stars.add(new Star(s, new int[] {STAR_DELAY}));
        }
        sound(level, SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0f, 0.4f);
    }

    private void tickHazards(ServerLevel level) {
        for (int i = stars.size() - 1; i >= 0; i--) {
            Star s = stars.get(i);
            int t = --s.timer()[0];
            Vec3 p = s.pos();
            if (t % 3 == 0) {
                double r = 2.5;
                for (int k = 0; k < 14; k++) {
                    double a = k * Math.PI * 2 / 14 + t * 0.05;
                    level.sendParticles(EclipseFx.dust(t < 10 ? EclipseFx.PALE : EclipseFx.VIOLET, 1.3f),
                            p.x + Math.cos(a) * r, p.y + 0.15, p.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
                }
            }
            if (t < 14) {
                level.sendParticles(ParticleTypes.END_ROD, p.x, p.y + t * 1.2, p.z, 2, 0.15, 0.3, 0.15, 0);
            }
            if (t <= 0) {
                stars.remove(i);
                level.sendParticles(ParticleTypes.EXPLOSION, p.x, p.y + 0.5, p.z, 2, 0.6, 0.2, 0.6, 0);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x, p.y + 0.5, p.z, 40, 1.2, 0.3, 1.2, 0.15);
                level.playSound(null, BlockPos.containing(p), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0f, 1.4f);
                float dmg = damage(AVServerConfig.ECLIPSE_STAR_DAMAGE::get);
                for (ServerPlayer pl : level.getEntitiesOfClass(ServerPlayer.class, new AABB(p, p).inflate(3, 4, 3))) {
                    if (horizontal(pl.position(), p) <= 2.5 && pl.getY() - p.y > -1.5 && pl.getY() - p.y < 3) {
                        hurt(level, pl, damageSources().indirectMagic(this, this), dmg);
                    }
                }
            }
        }
    }

    private void summonEchoes(ServerLevel level, LivingEntity target) {
        List<EclipseEcho> existing = level.getEntitiesOfClass(EclipseEcho.class, getBoundingBox().inflate(32));
        int count = Math.min(AVServerConfig.ECLIPSE_ECHOES_PER_WAVE.get(), 6 - existing.size());
        for (int i = 0; i < count; i++) {
            EclipseEcho echo = EclipseEcho.spawn(level, position().add(random.nextInt(7) - 3, 1.5, random.nextInt(7) - 3), 0);
            if (echo != null && target != null) {
                echo.setTarget(target);
            }
        }
    }

    private void diveImpact(ServerLevel level) {
        setPos(aim.x, aim.y, aim.z);
        setDeltaMovement(Vec3.ZERO);
        waveHit.clear();
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, aim.x, aim.y + 0.5, aim.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, aim.x, aim.y + 0.5, aim.z, 80, 2, 0.4, 2, 0.3);
        level.playSound(null, BlockPos.containing(aim), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0f, 0.6f);
        sound(level, SoundEvents.MACE_SMASH_GROUND_HEAVY, 3.0f, 0.5f);
        float dmg = damage(AVServerConfig.ECLIPSE_DIVE_DAMAGE::get);
        for (ServerPlayer p : players(level, 8)) {
            if (horizontal(p.position(), aim) <= 3.5 && Math.abs(p.getY() - aim.y) < 3) {
                hurt(level, p, damageSources().mobAttack(this), dmg);
                waveHit.add(p.getUUID());
            }
        }
    }

    /** The dive's shockwave: a ring rolling outward along the ground; airborne players are safe. */
    private void tickShockwave(ServerLevel level, int elapsed) {
        if (elapsed > WAVE_TICKS) {
            return;
        }
        double r = elapsed * WAVE_SPEED;
        float dmg = damage(AVServerConfig.ECLIPSE_SHOCKWAVE_DAMAGE::get);
        for (ServerPlayer p : players(level, r + 2)) {
            double d = horizontal(p.position(), aim);
            if (Math.abs(d - r) < 0.9 && p.onGround() && Math.abs(p.getY() - aim.y) < 1.5 && waveHit.add(p.getUUID())) {
                hurt(level, p, damageSources().indirectMagic(this, this), dmg);
                Vec3 push = new Vec3(p.getX() - aim.x, 0, p.getZ() - aim.z).normalize().scale(0.6);
                p.push(push.x, 0.45, push.z);
                p.hurtMarked = true;
            }
        }
    }

    /** Height of the ground-hugging Eclipse Ray at (x, z): half a block above the local floor (NaN over the void). */
    double beamHeight(double x, double z, double fromY) {
        double g = groundY(x, z, fromY, 10);
        return Double.isNaN(g) ? g : g + 0.55;
    }

    /** Eclipse Ray: hits players the ray swept over this tick, unless they jumped over it or hid behind a block. */
    private void beamHits(ServerLevel level, float prev, float now) {
        float step = Mth.wrapDegrees(now - prev);
        float dmg = damage(AVServerConfig.ECLIPSE_BEAM_DAMAGE::get);
        for (ServerPlayer p : players(level, BEAM_LENGTH + 1)) {
            double d = horizontal(p.position(), aim);
            if (d > BEAM_LENGTH) {
                continue;
            }
            double y = beamHeight(p.getX(), p.getZ(), p.getY() + 0.5);
            if (Double.isNaN(y)) {
                y = p.getY() + 0.55;
            }
            AABB box = p.getBoundingBox();
            if (box.minY > y + 0.3 || box.maxY < y - 0.3) {
                continue; // jumped over it
            }
            float margin = (float) Math.toDegrees(Math.atan2(0.5, Math.max(0.5, d)));
            float toPlayer = yawTo(p.position(), aim);
            float delta = Mth.wrapDegrees(toPlayer - prev);
            boolean swept = step >= 0 ? delta >= -margin && delta <= step + margin : delta <= margin && delta >= step - margin;
            if (!swept && d > 1.5) {
                continue;
            }
            if (d > BEAM_START + 0.5) {
                Vec3 s0 = aim.add(dir(toPlayer).scale(BEAM_START));
                double sy = beamHeight(s0.x, s0.z, aim.y + 2);
                Vec3 start = new Vec3(s0.x, Double.isNaN(sy) ? y : sy, s0.z);
                Vec3 at = new Vec3(p.getX(), y, p.getZ());
                HitResult wall = level.clip(new ClipContext(start, at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                if (wall.getType() != HitResult.Type.MISS && wall.getLocation().distanceToSqr(start) < at.distanceToSqr(start) - 0.25) {
                    continue; // behind cover
                }
            }
            if (hurt(level, p, damageSources().indirectMagic(this, this), dmg)) {
                Vec3 push = dir(now + beamDir * 90).scale(0.5);
                p.push(push.x, 0.3, push.z);
                p.hurtMarked = true;
            }
        }
    }

    private void nova(ServerLevel level) {
        Vec3 centre = getEyePosition();
        level.sendParticles(ParticleTypes.SONIC_BOOM, centre.x, centre.y, centre.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, centre.x, centre.y, centre.z, 250, 5, 2, 5, 0.3);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, centre.x, centre.y, centre.z, 1, 0, 0, 0, 0);
        sound(level, SoundEvents.WARDEN_SONIC_BOOM, 3.0f, 0.5f);
        float dmg = damage(AVServerConfig.ECLIPSE_NOVA_DAMAGE::get);
        for (ServerPlayer p : players(level, 20)) {
            if (p.distanceToSqr(centre) > 20 * 20) {
                continue;
            }
            HitResult cover = level.clip(new ClipContext(centre, p.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (cover.getType() != HitResult.Type.MISS) {
                continue; // hid behind a block
            }
            hurt(level, p, damageSources().indirectMagic(this, this), dmg);
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
            Vec3 push = p.position().subtract(position()).normalize().scale(0.8);
            p.push(push.x, 0.2, push.z);
            p.hurtMarked = true;
        }
    }

    private void blink(ServerLevel level, LivingEntity target) {
        if (target == null) {
            return;
        }
        Vec3 look = target.getLookAngle();
        Vec3 back = new Vec3(-look.x, 0, -look.z);
        back = back.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : back.normalize();
        Vec3 to = target.position().add(back.scale(3.5));
        if (Double.isNaN(groundY(to.x, to.z, to.y + 2, 12))) {
            to = target.position().add(back.scale(-3.5)); // never blink over the void
        }
        to = leash(to);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 2.5, getZ(), 60, 0.6, 1.8, 0.6, 0.08);
        teleportTo(to.x, target.getY() + 0.1, to.z);
        face(yawTo(target.position()), 180);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, to.x, target.getY() + 2.5, to.z, 60, 0.6, 1.8, 0.6, 0.08);
        sound(level, SoundEvents.ENDERMAN_TELEPORT, 2.0f, 0.5f);
    }

    /** Umbral Grasp: drags a hiding or fleeing target back in front of the Eclipse; otherwise just yanks it closer. */
    private void grasp(ServerLevel level, LivingEntity target) {
        if (target == null) {
            return;
        }
        boolean stillCheesing = !hasLineOfSight(target) || horizontal(target.position(), home()) > AVServerConfig.ECLIPSE_LEASH.get();
        Vec3 front = position().add(dir(getYRot()).scale(3));
        double g = groundY(front.x, front.z, getY() + 3, 16);
        if (stillCheesing) {
            Vec3 to = Double.isNaN(g) ? new Vec3(home().x, homeGround, home().z) : new Vec3(front.x, g, front.z);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY() + 1, target.getZ(), 60, 0.4, 0.9, 0.4, 0.1);
            target.teleportTo(to.x, to.y + 0.05, to.z);
        } else {
            Vec3 pull = position().subtract(target.position());
            double len = Math.max(1, pull.length());
            pull = pull.scale(Math.min(2.2, len * 0.25) / len);
            target.push(pull.x, 0.35, pull.z);
            target.hurtMarked = true;
        }
        hurt(level, target, damageSources().indirectMagic(this, this), damage(AVServerConfig.ECLIPSE_GRASP_DAMAGE::get));
        //? if >=26.1 {
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
        //?} else {
        /*target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        *///?}
        sound(level, SoundEvents.ELDER_GUARDIAN_CURSE, 1.0f, 0.6f);
    }

    // ------------------------------------------------------------------ phases, shield, cover

    private void transition(ServerLevel level, boolean total) {
        sound(level, SoundEvents.WITHER_SPAWN, 2.0f, total ? 0.8f : 1.2f);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 3, getZ(), 1, 0, 0, 0, 0);
        for (ServerPlayer p : players(level, 10)) {
            Vec3 push = p.position().subtract(position()).normalize().scale(1.0);
            p.push(push.x, 0.35, push.z);
            p.hurtMarked = true;
        }
        for (ServerPlayer p : players(level, 64)) {
            EclipseFx.title(p, Component.translatable("boss.astralvigil.eclipse.phase." + (total ? 3 : 2)).withStyle(ChatFormatting.DARK_PURPLE),
                    Component.translatable("message.astralvigil.eclipse.phase" + (total ? 3 : 2)).withStyle(ChatFormatting.LIGHT_PURPLE));
            if (total) {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
            }
        }
        if (total) {
            planCover(level);
        }
    }

    private void raiseShield(ServerLevel level) {
        int count = AVServerConfig.ECLIPSE_PYLON_COUNT.get();
        double radius = AVServerConfig.ECLIPSE_PYLON_RADIUS.get();
        Vec3 h = home();
        double start = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < count; i++) {
            double a = start + i * Math.PI * 2 / count;
            Vec3 spot = null;
            for (double r = radius; r >= 2 && spot == null; r -= 1.5) {
                double x = h.x + Math.cos(a) * r;
                double z = h.z + Math.sin(a) * r;
                double g = groundY(x, z, homeGround + 6, 16);
                if (!Double.isNaN(g)) {
                    spot = new Vec3(x, g, z);
                }
            }
            if (spot == null) {
                spot = new Vec3(h.x + Math.cos(a) * 2, homeGround, h.z + Math.sin(a) * 2);
            }
            UmbralPylon.spawn(level, spot, this);
        }
        shield = 1;
        for (ServerPlayer p : players(level, 64)) {
            EclipseFx.actionBar(p, Component.translatable("message.astralvigil.eclipse.shield").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** Plans the cover walls of phase 3: short walls facing the arena centre, so the Ray and the Nova can be hidden from. */
    private void planCover(ServerLevel level) {
        coverRaised = true;
        pendingCover.clear();
        int count = AVServerConfig.ECLIPSE_COVER_PILLARS.get();
        Vec3 h = home();
        for (int i = 0; i < count; i++) {
            double a = (i + 0.5) * Math.PI * 2 / count;
            for (double r = 9.5; r >= 3.5; r -= 1) {
                double cx = h.x + Math.cos(a) * r;
                double cz = h.z + Math.sin(a) * r;
                double g = groundY(cx, cz, homeGround + 4, 10);
                if (Double.isNaN(g)) {
                    continue;
                }
                double tx = -Math.sin(a);
                double tz = Math.cos(a);
                for (int y = 0; y < 4; y++) {
                    for (int w = -1; w <= 1; w++) {
                        pendingCover.add(BlockPos.containing(cx + tx * w, g + y, cz + tz * w));
                    }
                }
                break;
            }
        }
        pendingCover.sort((x, y) -> Integer.compare(x.getY(), y.getY()));
    }

    private void placeCover(ServerLevel level, int max) {
        for (int i = 0; i < max && !pendingCover.isEmpty(); i++) {
            BlockPos p = pendingCover.remove(0);
            BlockState state = level.getBlockState(p);
            if ((state.isAir() || state.canBeReplaced()) && !temporaryBlocks.contains(p)) {
                level.setBlock(p, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
                temporaryBlocks.add(p.immutable());
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.05);
                if (i == 0) {
                    level.playSound(null, p, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 0.6f, 0.5f);
                }
            }
        }
    }

    private void flushCover(ServerLevel level) {
        placeCover(level, pendingCover.size());
    }

    /** Removes everything the fight left behind: temporary blocks, pylons, echoes. */
    private void cleanup(ServerLevel level) {
        for (BlockPos p : temporaryBlocks) {
            if (level.getBlockState(p).is(Blocks.CRYING_OBSIDIAN)) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.05);
            }
        }
        temporaryBlocks.clear();
        pendingCover.clear();
        for (UmbralPylon p : pylons(level)) {
            p.discard();
        }
        Vec3 c = home();
        for (EclipseEcho echo : level.getEntitiesOfClass(EclipseEcho.class, new AABB(c, c).inflate(AVServerConfig.ECLIPSE_LEASH.get() + 16))) {
            echo.discard();
        }
    }

    // ------------------------------------------------------------------ damage

    /** Version-neutral damage helper. */
    public static boolean hurt(ServerLevel level, Entity target, DamageSource source, float amount) {
        //? if >=26.1 {
        return target.hurtServer(level, source, amount);
        //?} else {
        /*return target.hurt(source, amount);
        *///?}
    }

    /** Shield/transition immunity, vulnerability windows and the phase floor. @return the damage to apply (0 = ignore) */
    private float adjustIncoming(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return amount;
        }
        Entity attacker = source.getEntity();
        if (isInvulnerable()) {
            if (attacker instanceof Player && shieldFeedback == 0) {
                shieldFeedback = 10;
                sound(level, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5f, 1.8f);
                level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + getBbHeight() * 0.5, getZ(), 12, 1.2, 1.5, 1.2, 0.05);
                if (shield == 1 && attacker instanceof ServerPlayer sp) {
                    EclipseFx.actionBar(sp, Component.translatable("message.astralvigil.eclipse.shield").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
            return 0;
        }
        if (attacker instanceof ServerPlayer sp) {
            participants.add(sp.getUUID());
        }
        if (timer.move() == EclipseMove.STAGGER || timer.move() == EclipseMove.GROUNDED) {
            amount *= AVServerConfig.ECLIPSE_VULNERABLE_MULTIPLIER.get().floatValue();
        }
        float floor = EclipseBrain.healthFloor(phase(), getMaxHealth());
        if (getHealth() - amount < floor) {
            amount = Math.max(0, getHealth() - floor);
        }
        return amount;
    }

    //? if >=26.1 {
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        float a = adjustIncoming(level, source, amount);
        return a > 0 && super.hurtServer(level, source, a);
    }
    //?} else {
    /*@Override
    public boolean hurt(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel level)) {
            return super.hurt(source, amount);
        }
        float a = adjustIncoming(level, source, amount);
        return a > 0 && super.hurt(source, a);
    }
    *///?}

    // ------------------------------------------------------------------ death, loot, removal

    @Override
    protected void tickDeath() {
        deathTime++;
        if (level() instanceof ServerLevel server) {
            double y = getY() + 2.5 - deathTime * 0.02;
            if (deathTime % 4 == 0) {
                server.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), y, getZ(), 20 + deathTime / 2, 1.2, 1.8, 1.2, 0.12);
                server.sendParticles(ParticleTypes.SQUID_INK, getX(), y, getZ(), 4, 1, 1.5, 1, 0.02);
            }
            if (deathTime == 25 || deathTime == 60) {
                sound(server, SoundEvents.WITHER_DEATH, 2.0f, 0.5f + deathTime / 100f);
            }
            if (deathTime == 75) {
                sound(server, SoundEvents.GLASS_BREAK, 3.0f, 0.4f);
                server.sendParticles(ParticleTypes.END_ROD, getX(), y + 1, getZ(), 60, 0.5, 0.5, 0.5, 0.35);
            }
            if (deathTime >= DEATH_TICKS && !isRemoved()) {
                server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2, getZ(), 150, 1.5, 2, 1.5, 0.25);
                server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 2, getZ(), 1, 0, 0, 0, 0);
                sound(server, SoundEvents.ENDER_DRAGON_DEATH, 1.0f, 1.6f);
                remove(RemovalReason.KILLED);
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel server) {
            for (ServerPlayer p : players(server, AVServerConfig.ECLIPSE_LEASH.get() + 8)) {
                participants.add(p.getUUID());
            }
        }
        super.die(source);
        timer.stop();
        stars.clear();
        if (level() instanceof ServerLevel server) {
            bossBar.removeAllPlayers();
            for (ServerPlayer p : players(server, AVServerConfig.ECLIPSE_RESET_RANGE.get())) {
                p.sendSystemMessage(Component.translatable("message.astralvigil.eclipse.defeated").withStyle(ChatFormatting.GOLD));
                media.hofferbert.astralvigil.firmament.TrialManager.award(p, "endgame/eclipse_defeated");
            }
            cleanup(server);
            Vec3 c = home();
            for (EclipseEcho echo : server.getEntitiesOfClass(EclipseEcho.class, new AABB(c, c).inflate(48))) {
                echo.kill(
                        //? if >=26.1 {
                        server
                        //?}
                );
            }
        }
    }

    /** Rewards land on the dais (its spawn point): one Heart + feathers per participant, claimable only by them. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        List<UUID> owners = new ArrayList<>(participants);
        if (owners.isEmpty()) {
            owners.add(null);
        }
        int feathers = AVServerConfig.ECLIPSE_FEATHERS.get();
        for (UUID owner : owners) {
            reward(level, new ItemStack(AVItems.ECLIPSE_HEART.get()), owner);
            if (feathers > 0) {
                reward(level, new ItemStack(AVItems.SERAPH_FEATHER.get(), feathers + random.nextInt(3)), owner);
            }
        }
    }

    private void reward(ServerLevel level, ItemStack stack, UUID owner) {
        Vec3 h = home();
        double g = groundY(h.x, h.z, h.y + 1, 16);
        ItemEntity item = new ItemEntity(level, h.x, (Double.isNaN(g) ? h.y : g) + 0.5, h.z, stack);
        item.setDeltaMovement(Vec3.ZERO);
        item.setUnlimitedLifetime();
        item.setInvulnerable(true);
        if (owner != null) {
            item.setTarget(owner);
        }
        level.addFreshEntity(item);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && level() instanceof ServerLevel server) {
            bossBar.removeAllPlayers();
            cleanup(server);
        }
        super.remove(reason);
    }

    /** Peaceful: the Eclipse withdraws and the Umbral Sigil returns to whoever spent it. */
    @Override
    public void checkDespawn() {
        if (level().getDifficulty() == Difficulty.PEACEFUL && level() instanceof ServerLevel server) {
            if (summoner != null) {
                ItemStack sigil = new ItemStack(AVItems.UMBRAL_SIGIL.get());
                ServerPlayer p = server.getServer().getPlayerList().getPlayer(summoner);
                if (p != null) {
                    if (!p.getInventory().add(sigil)) {
                        reward(server, sigil, summoner);
                    }
                    p.sendSystemMessage(Component.translatable("message.astralvigil.eclipse.peaceful_refund").withStyle(ChatFormatting.DARK_PURPLE));
                } else {
                    reward(server, sigil, summoner);
                }
                summoner = null;
            }
            discard();
            return;
        }
        super.checkDespawn();
    }

    // ------------------------------------------------------------------ boss bar, sound, cues

    private void updateBossBar() {
        bossBar.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0f, 1f));
        String key = enraged ? "enraged" : shield == 1 ? "shield" : String.valueOf(phase());
        if (!key.equals(barKey)) {
            barKey = key;
            bossBar.setName(Component.translatable("boss.astralvigil.eclipse", getDisplayName(),
                    Component.translatable("boss.astralvigil.eclipse.phase." + key)));
            bossBar.setColor(switch (key) {
                case "shield" -> BossEvent.BossBarColor.WHITE;
                case "2" -> BossEvent.BossBarColor.BLUE;
                case "3", "enraged" -> BossEvent.BossBarColor.RED;
                default -> BossEvent.BossBarColor.PURPLE;
            });
        }
    }

    private void sound(ServerLevel level, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, blockPosition(), sound, SoundSource.HOSTILE, volume, pitch);
    }

    private static Component name(String key, ChatFormatting colour) {
        return Component.translatable(key).withStyle(colour, ChatFormatting.BOLD);
    }

    /** Names the attack above the hotbar of everyone in the arena. */
    private void cue(ServerLevel level, EclipseMove move) {
        Component c = name("attack.astralvigil.eclipse." + move.animation(Stage.NONE), ChatFormatting.LIGHT_PURPLE);
        for (ServerPlayer p : players(level, 48)) {
            EclipseFx.actionBar(p, c);
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(
            //? if >=26.1 {
            double distance,
            //?} else {
            /*float distance,
            *///?}
            float multiplier, DamageSource source) {
        return false;
    }

    // ------------------------------------------------------------------ save data

    private int[] packTemporary() {
        int[] out = new int[temporaryBlocks.size() * 3];
        for (int i = 0; i < temporaryBlocks.size(); i++) {
            BlockPos p = temporaryBlocks.get(i);
            out[i * 3] = p.getX();
            out[i * 3 + 1] = p.getY();
            out[i * 3 + 2] = p.getZ();
        }
        return out;
    }

    private void unpackTemporary(int[] data) {
        temporaryBlocks.clear();
        for (int i = 0; i + 2 < data.length; i += 3) {
            temporaryBlocks.add(new BlockPos(data[i], data[i + 1], data[i + 2]));
        }
    }

    private void readSummoner(String s) {
        try {
            summoner = s.isEmpty() ? null : UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            summoner = null;
        }
    }

    //? if >=26.1 {
    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("AVInit", initialised);
        out.putBoolean("AVHasHome", home != null);
        if (home != null) {
            out.putDouble("AVHomeX", home.x);
            out.putDouble("AVHomeY", home.y);
            out.putDouble("AVHomeZ", home.z);
        }
        out.putInt("AVPhase", phase);
        out.putInt("AVShield", shield);
        out.putBoolean("AVCover", coverRaised);
        out.putBoolean("AVEnraged", enraged);
        out.putIntArray("AVTemp", packTemporary());
        out.putString("AVSummoner", summoner == null ? "" : summoner.toString());
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in) {
        super.readAdditionalSaveData(in);
        initialised = in.getBooleanOr("AVInit", false);
        if (in.getBooleanOr("AVHasHome", false)) {
            home = new Vec3(in.getDoubleOr("AVHomeX", getX()), in.getDoubleOr("AVHomeY", getY()), in.getDoubleOr("AVHomeZ", getZ()));
        }
        phase = in.getIntOr("AVPhase", 0);
        shield = in.getIntOr("AVShield", 0);
        coverRaised = in.getBooleanOr("AVCover", false);
        enraged = in.getBooleanOr("AVEnraged", false);
        unpackTemporary(in.getIntArray("AVTemp").orElse(new int[0]));
        readSummoner(in.getStringOr("AVSummoner", ""));
    }
    //?} else {
    /*@Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("AVInit", initialised);
        out.putBoolean("AVHasHome", home != null);
        if (home != null) {
            out.putDouble("AVHomeX", home.x);
            out.putDouble("AVHomeY", home.y);
            out.putDouble("AVHomeZ", home.z);
        }
        out.putInt("AVPhase", phase);
        out.putInt("AVShield", shield);
        out.putBoolean("AVCover", coverRaised);
        out.putBoolean("AVEnraged", enraged);
        out.putIntArray("AVTemp", packTemporary());
        out.putString("AVSummoner", summoner == null ? "" : summoner.toString());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag in) {
        super.readAdditionalSaveData(in);
        initialised = in.getBoolean("AVInit");
        if (in.getBoolean("AVHasHome")) {
            home = new Vec3(in.getDouble("AVHomeX"), in.getDouble("AVHomeY"), in.getDouble("AVHomeZ"));
        }
        phase = in.getInt("AVPhase");
        shield = in.getInt("AVShield");
        coverRaised = in.getBoolean("AVCover");
        enraged = in.getBoolean("AVEnraged");
        unpackTemporary(in.getIntArray("AVTemp"));
        readSummoner(in.getString("AVSummoner"));
    }
    *///?}

    // ------------------------------------------------------------------ client: telegraphs and ambience

    private void clientTick() {
        int m = entityData.get(DATA_MOVE);
        int s = entityData.get(DATA_STAGE);
        if (m != clientMove || s != clientStage) {
            clientMove = m;
            clientStage = s;
            clientStageTicks = 0;
        } else {
            clientStageTicks++;
        }
        double density = media.hofferbert.astralvigil.config.AVClientConfig.particleFactor();
        if (density <= 0 || isDeadOrDying()) {
            return;
        }
        EclipseMove move = EclipseMove.byId(m);
        Stage stage = currentStage();
        Vec3 aim = aim();
        Level level = level();
        // ambience: void motes drip from the wings and robe
        if (random.nextDouble() < 0.6 * density) {
            level.addParticle(ParticleTypes.REVERSE_PORTAL, getX() + (random.nextDouble() - 0.5) * 4, getY() + random.nextDouble() * 4.5,
                    getZ() + (random.nextDouble() - 0.5) * 4, 0, -0.03, 0);
        }
        if (enraged() && random.nextDouble() < 0.8 * density) {
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, getX() + (random.nextDouble() - 0.5) * 2.5, getY() + random.nextDouble() * 5,
                    getZ() + (random.nextDouble() - 0.5) * 2.5, 0, 0.04, 0);
        }
        if (shielded()) {
            Vec3 c = position().add(0, getBbHeight() * 0.5, 0);
            for (int i = 0; i < 6 * density; i++) {
                double u = random.nextDouble() * Math.PI * 2;
                double v = Math.acos(2 * random.nextDouble() - 1);
                double r = 3.4;
                level.addParticle(EclipseFx.dust(i % 3 == 0 ? EclipseFx.PALE : EclipseFx.VIOLET, 0.8f), c.x + r * Math.sin(v) * Math.cos(u),
                        c.y + r * Math.cos(v) * 1.1, c.z + r * Math.sin(v) * Math.sin(u), 0, 0, 0);
            }
        }
        switch (move) {
            case SWEEP -> {
                if (stage == Stage.WINDUP && clientStageTicks % 2 == 0) {
                    boolean locked = EclipseMove.SWEEP.ticks(Stage.WINDUP) - clientStageTicks <= SWEEP_LOCK;
                    float base = syncedYaw();
                    for (int a = -65; a <= 65; a += 10) {
                        for (double r = 2.5; r <= 7.5; r += 2.5) {
                            Vec3 p = position().add(dir(base + a).scale(r));
                            level.addParticle(EclipseFx.dust(locked ? EclipseFx.PALE : EclipseFx.DEEP, 1.0f), p.x, getY() + 0.2, p.z, 0, 0, 0);
                        }
                    }
                }
            }
            case LANCE -> {
                Vec3 from = getEyePosition();
                if (stage == Stage.WINDUP) {
                    boolean locked = EclipseMove.LANCE.ticks(Stage.WINDUP) - clientStageTicks <= LANCE_LOCK;
                    line(from, aim, locked ? 0.5 : 1.2, EclipseFx.dust(locked ? EclipseFx.PALE : EclipseFx.VIOLET, locked ? 1.0f : 0.6f), 40);
                } else if (stage == Stage.ACTIVE && clientStageTicks == 0) {
                    Vec3 to = clip(from, from.add(aim.subtract(from).normalize().scale(40)));
                    line(from, to, 0.35, ParticleTypes.END_ROD, 120);
                    line(from, to, 0.5, EclipseFx.dust(EclipseFx.VIOLET, 1.6f), 80);
                }
            }
            case STARFALL -> {
                if (stage == Stage.WINDUP) {
                    level.addParticle(ParticleTypes.END_ROD, getX(), getY() + 6.5, getZ(), (random.nextDouble() - 0.5) * 0.2, 0.25,
                            (random.nextDouble() - 0.5) * 0.2);
                }
            }
            case DIVE -> {
                if (stage == Stage.WINDUP || stage == Stage.ACTIVE) {
                    boolean locked = stage == Stage.ACTIVE || EclipseMove.DIVE.ticks(Stage.WINDUP) - clientStageTicks <= DIVE_LOCK;
                    ring(aim.add(0, 0.15, 0), 3.5, 20, EclipseFx.dust(locked ? EclipseFx.PALE : EclipseFx.VIOLET, 1.2f), clientStageTicks * 0.1);
                    ring(aim.add(0, 0.15, 0), 1.2, 8, EclipseFx.dust(EclipseFx.DEEP, 1.0f), -clientStageTicks * 0.2);
                } else if (stage == Stage.RECOVERY && clientStageTicks <= WAVE_TICKS) {
                    double r = clientStageTicks * WAVE_SPEED;
                    ring(aim.add(0, 0.2, 0), r, (int) (8 + r * 5), EclipseFx.dust(EclipseFx.PALE, 1.4f), 0);
                    ring(aim.add(0, 0.5, 0), r, (int) (4 + r * 2), ParticleTypes.REVERSE_PORTAL, 0.3);
                }
            }
            case BEAM -> {
                if (stage == Stage.WINDUP && clientStageTicks % 2 == 0) {
                    drawRay(aim, syncedYaw(), 1.0, EclipseFx.dust(EclipseFx.DEEP, 0.9f), false);
                    for (int i = 1; i <= 3; i++) { // arrows show which way it will turn
                        Vec3 p = aim.add(dir(syncedYaw() + beamDirHint() * i * 12).scale(BEAM_START + 2));
                        double y = beamHeight(p.x, p.z, aim.y + 2);
                        level.addParticle(EclipseFx.dust(EclipseFx.VIOLET, 1.2f), p.x, Double.isNaN(y) ? aim.y : y + 0.3, p.z, 0, 0, 0);
                    }
                } else if (stage == Stage.ACTIVE) {
                    drawRay(aim, syncedYaw(), 0.4, EclipseFx.dust(EclipseFx.PALE, 1.5f), true);
                    drawRay(aim, syncedYaw(), 1.2, ParticleTypes.END_ROD, false);
                }
            }
            case NOVA -> {
                if (stage == Stage.WINDUP) {
                    Vec3 c = getEyePosition();
                    for (int i = 0; i < 6 * density; i++) {
                        Vec3 off = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).normalize().scale(10);
                        level.addParticle(ParticleTypes.REVERSE_PORTAL, c.x + off.x, c.y + off.y, c.z + off.z, -off.x * 0.09, -off.y * 0.09, -off.z * 0.09);
                    }
                }
            }
            case GRASP -> {
                if (stage == Stage.WINDUP) {
                    line(position().add(0, 2.5, 0), aim, 0.8, EclipseFx.dust(EclipseFx.DEEP, 1.2f), 60);
                }
            }
            case ENTRANCE -> {
                double t = clientStageTicks * 0.15;
                for (int i = 0; i < 4; i++) {
                    double a = t + i * Math.PI / 2;
                    double r = Math.max(0.5, 6 - clientStageTicks * 0.08);
                    level.addParticle(ParticleTypes.REVERSE_PORTAL, getX() + Math.cos(a) * r, getY() + 0.2 + clientStageTicks * 0.05,
                            getZ() + Math.sin(a) * r, 0, 0.02, 0);
                }
            }
            case STAGGER, GROUNDED -> {
                if (random.nextDouble() < 0.3) {
                    level.addParticle(ParticleTypes.LARGE_SMOKE, getX() + (random.nextDouble() - 0.5) * 2, getY() + 0.3,
                            getZ() + (random.nextDouble() - 0.5) * 2, 0, 0.02, 0);
                }
            }
            default -> {}
        }
    }

    /** Turning direction of the Eclipse Ray (synced). */
    private float beamDirHint() {
        return (entityData.get(DATA_FLAGS) & FLAG_COUNTERCLOCKWISE) != 0 ? -1f : 1f;
    }

    /** Client: the ground-hugging ray from the arena centre outward, stopping at walls and over the void. */
    private void drawRay(Vec3 centre, float rayYaw, double step, net.minecraft.core.particles.ParticleOptions particle, boolean source) {
        Vec3 d = dir(rayYaw);
        double prev = Double.NaN;
        Vec3 first = null;
        double density = media.hofferbert.astralvigil.config.AVClientConfig.particleFactor();
        double stride = step / Math.max(0.25, density);
        for (double r = BEAM_START + random.nextDouble() * stride; r <= BEAM_LENGTH; r += stride) {
            double x = centre.x + d.x * r;
            double z = centre.z + d.z * r;
            double y = beamHeight(x, z, (Double.isNaN(prev) ? centre.y : prev) + 2);
            if (Double.isNaN(y) || !Double.isNaN(prev) && y > prev + 1.1) {
                if (source && !Double.isNaN(prev)) {
                    level().addParticle(ParticleTypes.SONIC_BOOM, x - d.x * stride, prev, z - d.z * stride, 0, 0, 0);
                }
                break;
            }
            if (first == null) {
                first = new Vec3(x, y, z);
            }
            prev = y;
            level().addParticle(particle, x, y, z, 0, 0, 0);
        }
        if (source && first != null) {
            line(position().add(0, getBbHeight() * 0.45, 0), first, 0.5, EclipseFx.dust(EclipseFx.PALE, 1.2f), 20);
        }
    }

    private Vec3 clip(Vec3 from, Vec3 to) {
        HitResult hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
    }

    private void line(Vec3 from, Vec3 to, double step, net.minecraft.core.particles.ParticleOptions particle, int max) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 1e-3) {
            return;
        }
        double density = media.hofferbert.astralvigil.config.AVClientConfig.particleFactor();
        int n = (int) Math.min(max * density, len / step);
        Vec3 u = d.scale(1 / len);
        double off = random.nextDouble() * step;
        for (int i = 0; i < n; i++) {
            Vec3 p = from.add(u.scale(off + i * step));
            level().addParticle(particle, p.x, p.y, p.z, 0, 0, 0);
        }
    }

    private void ring(Vec3 c, double r, int n, net.minecraft.core.particles.ParticleOptions particle, double spin) {
        double density = media.hofferbert.astralvigil.config.AVClientConfig.particleFactor();
        int count = Math.max(4, (int) (n * density));
        for (int i = 0; i < count; i++) {
            double a = spin + i * Math.PI * 2 / count;
            level().addParticle(particle, c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r, 0, 0, 0);
        }
    }

    // ------------------------------------------------------------------ GeckoLib

    private static Map<String, RawAnimation> animations() {
        Map<String, RawAnimation> map = new HashMap<>();
        for (EclipseMove m : EclipseMove.values()) {
            for (Stage s : Stage.values()) {
                if (m != EclipseMove.NONE && s != Stage.NONE && m.ticks(s) > 0) {
                    String name = m.animation(s);
                    map.put(name, EclipseMove.loops(name) ? RawAnimation.begin().thenLoop(name) : RawAnimation.begin().thenPlayAndHold(name));
                }
            }
        }
        for (String idle : new String[] {"idle", "idle_shielded", "idle_total", "idle_enraged"}) {
            map.put(idle, RawAnimation.begin().thenLoop(idle));
        }
        map.put("death", RawAnimation.begin().thenPlayAndHold("death"));
        return map;
    }

    /** All animation names the entity can request (for tests and the generator). */
    public static Set<String> animationNames() {
        return ANIMATIONS.keySet();
    }

    RawAnimation currentAnimation() {
        if (isDeadOrDying()) {
            return ANIMATIONS.get("death");
        }
        EclipseMove m = currentMove();
        Stage s = currentStage();
        if (m != EclipseMove.NONE && s != Stage.NONE) {
            RawAnimation a = ANIMATIONS.get(m.animation(s));
            if (a != null) {
                return a;
            }
        }
        byte f = entityData.get(DATA_FLAGS);
        if ((f & FLAG_SHIELD) != 0) {
            return ANIMATIONS.get("idle_shielded");
        }
        if ((f & FLAG_ENRAGED) != 0) {
            return ANIMATIONS.get("idle_enraged");
        }
        return ANIMATIONS.get((f & FLAG_TOTAL) != 0 ? "idle_total" : "idle");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        //? if >=26.1 {
        controllers.add(new AnimationController<EclipseEntity>("main", 3, test -> test.setAndContinue(test.animatable().currentAnimation())));
        //?} else {
        /*controllers.add(new AnimationController<>(this, "main", 3, state -> state.setAndContinue(state.getAnimatable().currentAnimation())));
        *///?}
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ------------------------------------------------------------------ summoning

    /** Summons the Eclipse at {@code pos} (command, GameTests). */
    public static EclipseEntity summon(ServerLevel level, Vec3 pos) {
        return summon(level, pos, null);
    }

    /**
     * Summons the Eclipse at {@code pos}; {@code summoner} (may be null) gets the Umbral Sigil back if the world turns
     * peaceful before the fight ends.
     */
    public static EclipseEntity summon(ServerLevel level, Vec3 pos, ServerPlayer summoner) {
        EclipseEntity e = AVEntities.ECLIPSE.get().create(level
                //? if >=26.1 {
                , net.minecraft.world.entity.EntitySpawnReason.EVENT
                //?}
                );
        if (e == null) {
            return null;
        }
        e.setPos(pos.x, pos.y, pos.z);
        e.setYRot(Mth.wrapDegrees(level.getRandom().nextFloat() * 360f));
        e.summoner = summoner == null ? null : summoner.getUUID();
        level.addFreshEntity(e);
        level.playSound(null, e.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.0f, 0.5f);
        return e;
    }
}
