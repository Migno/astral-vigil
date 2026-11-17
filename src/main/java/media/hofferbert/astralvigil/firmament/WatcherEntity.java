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
 * GeckoLib (idle loop, triggerable "cast").
 */
public class WatcherEntity extends PathfinderMob implements GeoEntity {
    static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    static final RawAnimation CAST = RawAnimation.begin().thenPlay("cast");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Watcher watcher;

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
            if (TrialManager.interact(sp, this)) {
                triggerAnim("main", "cast");
            }
        }
        return InteractionResult.SUCCESS;
    }

    // ------------------------------------------------------------------ GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        //? if >=26.1 {
        controllers.add(new AnimationController<WatcherEntity>("main", 5, test -> test.setAndContinue(IDLE))
                .triggerableAnim("cast", CAST));
        //?} else {
        /*controllers.add(new AnimationController<>(this, "main", 5, state -> state.setAndContinue(IDLE))
                .triggerableAnim("cast", CAST));
        *///?}
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
