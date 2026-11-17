package media.hofferbert.astralvigil.eclipse;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import java.util.List;
import media.hofferbert.astralvigil.config.AVServerConfig;
import media.hofferbert.astralvigil.registry.AVEntities;
import media.hofferbert.astralvigil.registry.AVItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
//? if >=26.1 {
import com.geckolib.animatable.manager.AnimatableManager;
//?} else {
/*import com.geckolib.animation.AnimatableManager;
*///?}

/**
 * The Eclipse, fallen fifth Watcher. Three phases by health:
 * <ol>
 *   <li>melee sweeps and telegraphed Void Lances (dodge sideways);</li>
 *   <li>also summons Eclipse Echoes;</li>
 *   <li>also blinks next to its target and casts Umbral Novas – hide behind a block to avoid them.</li>
 * </ol>
 * Each phase change is an invulnerable transition with a shockwave. The phase is derived from health, so nothing has
 * to be saved. Drops (Eclipse Heart, Seraph Feathers) are config-driven, like the Wither's Nether Star.
 */
public class EclipseEntity extends Monster implements GeoEntity {
    static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("death");
    public static final int TRANSITION_TICKS = 50;
    public static final int DEATH_TICKS = 60;
    static final int LANCE_WINDUP = 20;
    static final int NOVA_WINDUP = 30;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossBar = createBossBar();
    private int phase;
    private int transition;
    private int attackCooldown = 40;
    private int lanceTimer = 80;
    private int lanceWindup;
    private Vec3 lanceTarget;
    private int summonTimer = 100;
    private int novaTimer = 160;
    private int novaWindup;
    private int blinkTimer = 120;

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
                .add(Attributes.MAX_HEALTH, 600.0)
                .add(Attributes.ARMOR, 14.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    /** 1, 2 or 3 – from the health fraction. */
    public static int phaseFor(float health, float max) {
        return EclipsePhase.of(health, max);
    }

    public int phase() {
        return phase == 0 ? phaseFor(getHealth(), getMaxHealth()) : phase;
    }

    public boolean inTransition() {
        return transition > 0;
    }

    // ------------------------------------------------------------------ server logic

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server) || isDeadOrDying()) {
            return;
        }
        bossBar.setProgress(getHealth() / getMaxHealth());
        int now = phaseFor(getHealth(), getMaxHealth());
        if (phase == 0) {
            phase = now;
        } else if (now > phase) {
            phase = now;
            startTransition(server);
        }
        if (transition > 0) {
            transition--;
            setDeltaMovement(getDeltaMovement().scale(0.5));
            if (transition == 0) {
                setInvulnerable(false);
            }
            return;
        }
        LivingEntity target = getTarget();
        hover(target);
        if (target == null || !target.isAlive()) {
            return;
        }
        getLookControl().setLookAt(target, 30f, 30f);
        tickMelee(server, target);
        tickLance(server, target);
        if (phase >= 2) {
            tickSummon(server, target);
        }
        if (phase >= 3) {
            tickNova(server);
            tickBlink(server, target);
        }
    }

    /** Hovers a few blocks above and around its target; drifts slowly when alone. */
    private void hover(LivingEntity target) {
        Vec3 goal;
        if (target != null) {
            double angle = tickCount * 0.02;
            goal = target.position().add(Math.cos(angle) * 5, 3.5, Math.sin(angle) * 5);
        } else {
            goal = position().add(0, Math.sin(tickCount * 0.05) * 0.1, 0);
        }
        Vec3 delta = goal.subtract(position());
        double speed = getAttributeValue(Attributes.MOVEMENT_SPEED) * (phase >= 3 ? 1.4 : 1.0);
        Vec3 move = delta.lengthSqr() > 1 ? delta.normalize().scale(speed) : delta.scale(0.1);
        setDeltaMovement(getDeltaMovement().scale(0.6).add(move.scale(0.4)));
    }

    private void tickMelee(ServerLevel level, LivingEntity target) {
        if (--attackCooldown > 0 || distanceToSqr(target) > 16) {
            return;
        }
        attackCooldown = 30;
        triggerAnim("main", "attack");
        hurt(level, target, damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
        Vec3 push = target.position().subtract(position()).normalize().scale(0.6);
        target.push(push.x, 0.3, push.z);
        level.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0f, 0.6f);
    }

    /** Void Lance: aims at where the target stands, fires {@link #LANCE_WINDUP} ticks later. */
    private void tickLance(ServerLevel level, LivingEntity target) {
        if (lanceWindup > 0) {
            if (--lanceWindup == 0) {
                fireLance(level);
            } else if (lanceWindup % 4 == 0) {
                Vec3 eye = getEyePosition();
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, eye.x, eye.y, eye.z, 6, 0.3, 0.3, 0.3, 0.02);
            }
            return;
        }
        if (--lanceTimer > 0) {
            return;
        }
        lanceTimer = phase >= 2 ? 60 : 90;
        lanceWindup = LANCE_WINDUP;
        lanceTarget = target.getEyePosition();
        triggerAnim("main", "cast");
        level.playSound(null, blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.5f, 0.5f);
    }

    private void fireLance(ServerLevel level) {
        if (lanceTarget == null) {
            return;
        }
        Vec3 from = getEyePosition();
        Vec3 dir = lanceTarget.subtract(from).normalize();
        Vec3 to = from.add(dir.scale(32));
        HitResult wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (wall.getType() != HitResult.Type.MISS) {
            to = wall.getLocation();
        }
        double length = from.distanceTo(to);
        for (double d = 0; d < length; d += 0.5) {
            Vec3 p = from.add(dir.scale(d));
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
            level.sendParticles(ParticleTypes.WITCH, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
        }
        float damage = AVServerConfig.ECLIPSE_LANCE_DAMAGE.get().floatValue();
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(from, to).inflate(1.0))) {
            if (player.getBoundingBox().inflate(0.3).clip(from, to).isPresent()) {
                hurt(level, player, damageSources().indirectMagic(this, this), damage);
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
            }
        }
        level.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.0f, 1.4f);
        lanceTarget = null;
    }

    private void tickSummon(ServerLevel level, LivingEntity target) {
        if (--summonTimer > 0) {
            return;
        }
        summonTimer = 220;
        List<EclipseEcho> existing = level.getEntitiesOfClass(EclipseEcho.class, getBoundingBox().inflate(32));
        int count = Math.min(AVServerConfig.ECLIPSE_ECHOES_PER_WAVE.get(), 6 - existing.size());
        for (int i = 0; i < count; i++) {
            EclipseEcho echo = EclipseEcho.spawn(level, position().add(random.nextInt(7) - 3, 1, random.nextInt(7) - 3), 0);
            if (echo != null) {
                echo.setTarget(target);
            }
        }
        if (count > 0) {
            triggerAnim("main", "cast");
            level.playSound(null, blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.5f, 0.6f);
        }
    }

    /** Umbral Nova: after a wind-up, hurts every player in range that has line of sight to the Eclipse. */
    private void tickNova(ServerLevel level) {
        if (novaWindup > 0) {
            if (--novaWindup == 0) {
                nova(level);
            } else {
                level.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + 2, getZ(), 4, 1.5, 1.5, 1.5, 0.01);
            }
            return;
        }
        if (--novaTimer > 0) {
            return;
        }
        novaTimer = 160;
        novaWindup = NOVA_WINDUP;
        triggerAnim("main", "cast");
        level.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.0f, 0.6f);
        for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(32))) {
            p.sendSystemMessage(Component.translatable("message.astralvigil.eclipse.nova"));
        }
    }

    private void nova(ServerLevel level) {
        Vec3 centre = getEyePosition();
        level.sendParticles(ParticleTypes.SONIC_BOOM, centre.x, centre.y, centre.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, centre.x, centre.y, centre.z, 200, 4, 2, 4, 0.2);
        float damage = AVServerConfig.ECLIPSE_NOVA_DAMAGE.get().floatValue();
        for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(12))) {
            HitResult cover = level.clip(new ClipContext(centre, p.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (cover.getType() != HitResult.Type.MISS) {
                continue; // hid behind a block
            }
            hurt(level, p, damageSources().indirectMagic(this, this), damage);
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
            Vec3 push = p.position().subtract(position()).normalize().scale(0.8);
            p.push(push.x, 0.2, push.z);
            p.hurtMarked = true;
        }
    }

    private void tickBlink(ServerLevel level, LivingEntity target) {
        if (--blinkTimer > 0 || novaWindup > 0) {
            return;
        }
        blinkTimer = 120 + random.nextInt(60);
        double angle = random.nextDouble() * Math.PI * 2;
        Vec3 to = target.position().add(Math.cos(angle) * 4, 2, Math.sin(angle) * 4);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 2, getZ(), 40, 0.5, 1.5, 0.5, 0.05);
        teleportTo(to.x, to.y, to.z);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, to.x, to.y + 2, to.z, 40, 0.5, 1.5, 0.5, 0.05);
        level.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 0.5f);
    }

    private void startTransition(ServerLevel level) {
        transition = TRANSITION_TICKS;
        lanceWindup = 0;
        novaWindup = 0;
        setInvulnerable(true);
        triggerAnim("main", "phase_transition");
        level.playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5f, 1.2f);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 2, getZ(), 1, 0, 0, 0, 0);
        for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(8))) {
            Vec3 push = p.position().subtract(position()).normalize().scale(0.9);
            p.push(push.x, 0.3, push.z);
            p.hurtMarked = true;
        }
        for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(48))) {
            p.sendSystemMessage(Component.translatable("message.astralvigil.eclipse.phase" + phase));
        }
    }

    /** Version-neutral damage helper. */
    static boolean hurt(ServerLevel level, Entity target, DamageSource source, float amount) {
        //? if >=26.1 {
        return target.hurtServer(level, source, amount);
        //?} else {
        /*return target.hurt(source, amount);
        *///?}
    }

    // ------------------------------------------------------------------ death, loot, boss bar

    @Override
    protected void tickDeath() {
        deathTime++;
        if (level() instanceof ServerLevel server) {
            if (deathTime % 5 == 0) {
                server.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 2, getZ(), 30, 1, 1.5, 1, 0.1);
            }
            if (deathTime >= DEATH_TICKS && !isRemoved()) {
                server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2, getZ(), 120, 1.5, 2, 1.5, 0.2);
                remove(RemovalReason.KILLED);
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server) {
            bossBar.removeAllPlayers();
            for (ServerPlayer p : server.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(48))) {
                p.sendSystemMessage(Component.translatable("message.astralvigil.eclipse.defeated"));
                media.hofferbert.astralvigil.firmament.TrialManager.award(p, "endgame/eclipse_defeated");
            }
            for (EclipseEcho echo : server.getEntitiesOfClass(EclipseEcho.class, getBoundingBox().inflate(48))) {
                echo.kill(
                        //? if >=26.1 {
                        server
                        //?}
                );
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        drop(level, new ItemStack(AVItems.ECLIPSE_HEART.get()));
        int feathers = AVServerConfig.ECLIPSE_FEATHERS.get();
        if (feathers > 0) {
            drop(level, new ItemStack(AVItems.SERAPH_FEATHER.get(), feathers + random.nextInt(3)));
        }
    }

    private void drop(ServerLevel level, ItemStack stack) {
        //? if >=26.1 {
        spawnAtLocation(level, stack);
        //?} else {
        /*spawnAtLocation(stack);
        *///?}
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
    public boolean causeFallDamage(
            //? if >=26.1 {
            double distance,
            //?} else {
            /*float distance,
            *///?}
            float multiplier, DamageSource source) {
        return false;
    }

    // ------------------------------------------------------------------ GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        //? if >=26.1 {
        controllers.add(new AnimationController<EclipseEntity>("main", 5, test -> test.setAndContinue(
                        test.animatable().isDeadOrDying() ? DEATH : IDLE))
                .triggerableAnim("attack", RawAnimation.begin().thenPlay("attack"))
                .triggerableAnim("cast", RawAnimation.begin().thenPlay("cast"))
                .triggerableAnim("phase_transition", RawAnimation.begin().thenPlay("phase_transition")));
        //?} else {
        /*controllers.add(new AnimationController<>(this, "main", 5, state -> state.setAndContinue(
                        state.getAnimatable().isDeadOrDying() ? DEATH : IDLE))
                .triggerableAnim("attack", RawAnimation.begin().thenPlay("attack"))
                .triggerableAnim("cast", RawAnimation.begin().thenPlay("cast"))
                .triggerableAnim("phase_transition", RawAnimation.begin().thenPlay("phase_transition")));
        *///?}
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** Summons the Eclipse at {@code pos} (Umbral Sigil, command, GameTests). */
    public static EclipseEntity summon(ServerLevel level, Vec3 pos) {
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
        level.addFreshEntity(e);
        level.playSound(null, e.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.0f, 0.7f);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 2, pos.z, 1, 0, 0, 0, 0);
        return e;
    }
}
