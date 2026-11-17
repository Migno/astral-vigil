package media.hofferbert.astralvigil.firmament;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
//? if >=26.1 {
import com.geckolib.animatable.manager.AnimatableManager;
//?} else {
/*import com.geckolib.animation.AnimatableManager;
*///?}

/**
 * A Watcher in its Firmament sanctum: floating, invulnerable, talks to players and starts their trial. Rendered with
 * GeckoLib: a looping idle plus triggerable clips - "cast" (a trial starts), "talk" (any other conversation),
 * "greet" (a player arrives nearby) and "bless" (a trial of this Watcher was completed, see {@link TrialManager}).
 */
public class WatcherEntity extends PathfinderMob implements GeoEntity {
    static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    static final RawAnimation CAST = RawAnimation.begin().thenPlay("cast");
    static final RawAnimation TALK = RawAnimation.begin().thenPlay("talk");
    static final RawAnimation GREET = RawAnimation.begin().thenPlay("greet");
    static final RawAnimation BLESS = RawAnimation.begin().thenPlay("bless");
    /** A player closer than this greets the Watcher; it has to leave beyond {@link #LEAVE_RANGE} to be greeted again. */
    static final double GREET_RANGE = 10.0;
    static final double LEAVE_RANGE = 16.0;
    static final int GREET_COOLDOWN = 200;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Watcher watcher;
    private boolean playerNear;
    private long nextGreet;
    private long busyUntil;

    public WatcherEntity(EntityType<? extends PathfinderMob> type, Level level, Watcher watcher) {
        super(type, level);
        this.watcher = watcher;
        setNoGravity(true);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public Watcher watcher() {
        return watcher;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer sp) {
            play(TrialManager.interact(sp, this) ? "cast" : "talk", 40);
        }
        return InteractionResult.SUCCESS;
    }

    /** Trial completed: wings flare, the gathered light is presented. */
    public void bless() {
        play("bless", 72);
    }

    private void play(String anim, int ticks) {
        triggerAnim("main", anim);
        busyUntil = level().getGameTime() + ticks;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || tickCount % 10 != 0) {
            return;
        }
        settle();
        long now = level().getGameTime();
        Player near = level().getNearestPlayer(this, playerNear ? LEAVE_RANGE : GREET_RANGE);
        boolean arrived = near != null && !near.isSpectator() && !playerNear;
        playerNear = near != null && !near.isSpectator();
        if (arrived && now >= nextGreet && now >= busyUntil) {
            nextGreet = now + GREET_COOLDOWN;
            play("greet", 52);
        }
    }

    /** Hover height of the hitbox above the ground: low enough to be spoken to (the model floats higher by itself). */
    static final double HOVER = 1.0;

    /** Sinks a Watcher that floats too high to be reached back down to its hover height (older realms spawned them higher). */
    private void settle() {
        net.minecraft.core.BlockPos.MutableBlockPos p = blockPosition().mutable();
        for (int i = 0; i < 12; i++) {
            p.move(net.minecraft.core.Direction.DOWN);
            if (!level().getBlockState(p).getCollisionShape(level(), p).isEmpty()) {
                double gap = getY() - (p.getY() + 1);
                if (gap > HOVER + 0.05) {
                    setPos(getX(), getY() - Math.min(gap - HOVER, 0.4), getZ());
                }
                return;
            }
        }
    }

    // ------------------------------------------------------------------ GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        //? if >=26.1 {
        controllers.add(new AnimationController<WatcherEntity>("main", 5, test -> test.setAndContinue(IDLE))
                .triggerableAnim("cast", CAST).triggerableAnim("talk", TALK)
                .triggerableAnim("greet", GREET).triggerableAnim("bless", BLESS));
        //?} else {
        /*controllers.add(new AnimationController<>(this, "main", 5, state -> state.setAndContinue(IDLE))
                .triggerableAnim("cast", CAST).triggerableAnim("talk", TALK)
                .triggerableAnim("greet", GREET).triggerableAnim("bless", BLESS));
        *///?}
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
