package media.hofferbert.astralvigil.eclipse;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import java.util.List;
import media.hofferbert.astralvigil.config.AVServerConfig;
import media.hofferbert.astralvigil.registry.AVEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
//? if >=26.1 {
import com.geckolib.animatable.manager.AnimatableManager;
//?} else {
/*import com.geckolib.animation.AnimatableManager;
*///?}

/**
 * Umbral Pylon: raised by the Eclipse in phase 2. While any pylon stands the Eclipse is shielded (and slowly healed);
 * destroy all of them to break the shield. Pylons are stationary, need no AI and crumble if their Eclipse is gone.
 */
public class UmbralPylon extends Mob implements GeoEntity {
    static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int orphanTicks;
    /** The Eclipse this pylon shields (transient: after a reload any nearby Eclipse claims it). */
    private java.util.UUID owner;

    public UmbralPylon(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    /** Whether this pylon belongs to {@code eclipse} (unowned pylons belong to any). */
    public boolean ownedBy(EclipseEntity eclipse) {
        return owner == null || owner.equals(eclipse.getUUID());
    }

    @Override
    public void tick() {
        setDeltaMovement(Vec3.ZERO);
        super.tick();
        if (level().isClientSide()) {
            clientEffects();
            return;
        }
        if (tickCount % 20 == 0 && level() instanceof ServerLevel server) {
            boolean owner = !server.getEntitiesOfClass(EclipseEntity.class, getBoundingBox().inflate(96)).isEmpty();
            orphanTicks = owner ? 0 : orphanTicks + 20;
            if (orphanTicks >= 100) {
                server.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 30, 0.3, 0.6, 0.3, 0.05);
                discard();
            }
        }
    }

    /** Thread of violet light from the crystal to the Eclipse it shields. */
    private void clientEffects() {
        double density = media.hofferbert.astralvigil.config.AVClientConfig.particleFactor();
        if (density <= 0 || tickCount % 2 != 0) {
            return;
        }
        List<EclipseEntity> owners = level().getEntitiesOfClass(EclipseEntity.class, getBoundingBox().inflate(48));
        Vec3 from = position().add(0, 1.45, 0);
        if (!owners.isEmpty() && owners.get(0).shielded()) {
            Vec3 to = owners.get(0).position().add(0, owners.get(0).getBbHeight() * 0.55, 0);
            Vec3 d = to.subtract(from);
            double len = d.length();
            int n = (int) Math.min(40, len * 0.6 * density);
            double phase = (tickCount * 0.07) % 1.0;
            for (int i = 0; i < n; i++) {
                double f = ((double) i / Math.max(1, n) + phase) % 1.0;
                Vec3 p = from.add(d.scale(f));
                level().addParticle(EclipseFx.dust(0xB57CFF, 0.9f), p.x, p.y, p.z, 0, 0, 0);
            }
        }
        level().addParticle(ParticleTypes.REVERSE_PORTAL, from.x, from.y, from.z, (random.nextDouble() - 0.5) * 0.1, 0.05,
                (random.nextDouble() - 0.5) * 0.1);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1.2, getZ(), 2, 0.3, 0.3, 0.3, 0);
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.2, getZ(), 60, 0.4, 0.8, 0.4, 0.15);
            server.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 2.0f, 0.5f);
            server.playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.5f, 0.7f);
        }
    }

    @Override
    protected void tickDeath() {
        if (++deathTime >= 8 && !isRemoved()) {
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {}

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

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        //? if >=26.1 {
        controllers.add(new AnimationController<UmbralPylon>("main", 0, test -> test.setAndContinue(IDLE)));
        //?} else {
        /*controllers.add(new AnimationController<>(this, "main", 0, state -> state.setAndContinue(IDLE)));
        *///?}
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** Raises a pylon at {@code pos} with the configured health. */
    public static UmbralPylon spawn(ServerLevel level, Vec3 pos, EclipseEntity owner) {
        UmbralPylon p = AVEntities.UMBRAL_PYLON.get().create(level
                //? if >=26.1 {
                , net.minecraft.world.entity.EntitySpawnReason.EVENT
                //?}
                );
        if (p == null) {
            return null;
        }
        double hp = AVServerConfig.ECLIPSE_PYLON_HEALTH.get();
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        p.setHealth((float) hp);
        p.setPos(pos.x, pos.y, pos.z);
        p.owner = owner == null ? null : owner.getUUID();
        level.addFreshEntity(p);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 1, pos.z, 40, 0.3, 1.0, 0.3, 0.1);
        level.playSound(null, p.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 1.5f, 0.6f);
        return p;
    }
}
