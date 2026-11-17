package media.hofferbert.astralvigil.eclipse;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import media.hofferbert.astralvigil.registry.AVEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
//? if >=26.1 {
import com.geckolib.animatable.manager.AnimatableManager;
//?} else {
/*import com.geckolib.animation.AnimatableManager;
*///?}

/** Eclipse Echo: a small floating wraith. Summoned by high Eclipse Gaze (short-lived) and by the Eclipse (phase 2+). */
public class EclipseEcho extends Monster implements GeoEntity {
    static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("death");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Remaining life in ticks; 0 = unlimited (not saved: echoes are transient). */
    private int life;
    private int attackCooldown;

    public EclipseEcho(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
        xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    public void setLife(int ticks) {
        life = ticks;
    }

    @Override
    public void tick() {
        noPhysics = false;
        super.tick();
        if (!(level() instanceof ServerLevel server) || isDeadOrDying()) {
            return;
        }
        if (life > 0 && --life == 0) {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 0.8, getZ(), 20, 0.3, 0.5, 0.3, 0.05);
            discard();
            return;
        }
        LivingEntity target = getTarget();
        Vec3 goal = target != null && target.isAlive()
                ? target.position().add(0, target.getBbHeight() * 0.5, 0)
                : position().add(Math.sin(tickCount * 0.1) * 0.3, Math.sin(tickCount * 0.07) * 0.1, Math.cos(tickCount * 0.1) * 0.3);
        Vec3 delta = goal.subtract(position());
        Vec3 move = delta.lengthSqr() > 0.25 ? delta.normalize().scale(getAttributeValue(Attributes.MOVEMENT_SPEED)) : Vec3.ZERO;
        setDeltaMovement(getDeltaMovement().scale(0.7).add(move.scale(0.3)));
        if (target != null) {
            getLookControl().setLookAt(target, 30f, 30f);
            if (--attackCooldown <= 0 && distanceToSqr(target) < 2.5) {
                attackCooldown = 20;
                triggerAnim("main", "attack");
                EclipseEntity.hurt(server, target, damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
            }
        }
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

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        //? if >=26.1 {
        controllers.add(new AnimationController<EclipseEcho>("main", 3, test -> test.setAndContinue(
                        test.animatable().isDeadOrDying() ? DEATH : IDLE))
                .triggerableAnim("attack", RawAnimation.begin().thenPlay("attack")));
        //?} else {
        /*controllers.add(new AnimationController<>(this, "main", 3, state -> state.setAndContinue(
                        state.getAnimatable().isDeadOrDying() ? DEATH : IDLE))
                .triggerableAnim("attack", RawAnimation.begin().thenPlay("attack")));
        *///?}
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** Spawns an echo with a lifetime in ticks (0 = unlimited). */
    public static EclipseEcho spawn(ServerLevel level, Vec3 pos, int life) {
        EclipseEcho echo = AVEntities.ECLIPSE_ECHO.get().create(level
                //? if >=26.1 {
                , net.minecraft.world.entity.EntitySpawnReason.EVENT
                //?}
                );
        if (echo == null) {
            return null;
        }
        echo.setPos(pos.x, pos.y, pos.z);
        echo.setLife(life);
        level.addFreshEntity(echo);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 0.8, pos.z, 30, 0.4, 0.6, 0.4, 0.05);
        level.playSound(null, echo.blockPosition(), SoundEvents.VEX_CHARGE, SoundSource.HOSTILE, 1.0f, 0.6f);
        return echo;
    }
}
