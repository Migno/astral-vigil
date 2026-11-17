package media.hofferbert.astralvigil.armor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import media.hofferbert.astralvigil.armor.SetBonuses.Bonus;
import media.hofferbert.astralvigil.compat.Celestial;
import media.hofferbert.astralvigil.config.AVServerConfig;
import media.hofferbert.astralvigil.gear.GearData;
import media.hofferbert.astralvigil.gear.GearManager;
import media.hofferbert.astralvigil.starlight.StarlightMath;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Applies full-set bonuses on the server. Status effects are granted as hidden ambient effects (no particles) and
 * refreshed every second; they are removed again when the set (or the time of day) no longer grants them.
 */
public final class SetBonusEvents {
    //? if >=26.1 {
    static final Holder<MobEffect> STRENGTH = MobEffects.STRENGTH;
    static final Holder<MobEffect> SPEED = MobEffects.SPEED;
    //?} else {
    /*static final Holder<MobEffect> STRENGTH = MobEffects.DAMAGE_BOOST;
    static final Holder<MobEffect> SPEED = MobEffects.MOVEMENT_SPEED;
    *///?}
    /** Night Vision flickers below 200 ticks, so it gets a longer duration than the others. */
    static final int DURATION = 60;
    static final int NIGHT_VISION_DURATION = 260;

    /** Transient per-player state (sneak edge for the glide burst, burst cooldown). */
    private static final Map<UUID, State> STATES = new HashMap<>();

    private static final class State {
        boolean sneakHeld;
        long burstReadyAt;
    }

    private SetBonusEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SetBonusEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(SetBonusEvents::onFall);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> STATES.remove(e.getEntity().getUUID()));
    }

    /** Current config, or the defaults before the server config is loaded. */
    public static SetBonuses.Settings settings() {
        if (!AVServerConfig.SPEC.isLoaded()) {
            return SetBonuses.Settings.DEFAULTS;
        }
        return new SetBonuses.Settings(
                AVServerConfig.SOLAR_FIRE_WARD.get(),
                AVServerConfig.SOLAR_STRENGTH.get(),
                AVServerConfig.LUNAR_NIGHT_VISION.get(),
                AVServerConfig.LUNAR_SPEED.get(),
                AVServerConfig.LUNAR_FALL_REDUCTION.get(),
                AVServerConfig.SERAPH_MENDING_AMOUNT.get(),
                AVServerConfig.SERAPH_BURST_COST.get());
    }

    public static SetBonuses.Sky sky(Level level, Player player) {
        if (!Celestial.hasSkyCycle(level)) {
            return SetBonuses.Sky.NONE;
        }
        boolean night = StarlightMath.isNight(Celestial.dayTime(level));
        return new SetBonuses.Sky(!night, night, Celestial.starlight(level, player.blockPosition()) > 0);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }
        ArmorSet set = ArmorSets.fullSet(player).orElse(null);
        int level = set == ArmorSet.SERAPH ? ArmorSets.level(player.getItemBySlot(EquipmentSlot.CHEST)) : 0;
        SetBonuses.Settings s = settings();
        if (set == ArmorSet.SERAPH) {
            glide(player, level, s);
        } else {
            STATES.computeIfAbsent(player.getUUID(), k -> new State()).sneakHeld = player.isShiftKeyDown();
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        Set<Bonus> active = applyEffects(player, set, level, s);
        if (active.contains(Bonus.STARLIGHT_MENDING) && player.tickCount % 40 == 0) {
            mend(player, s);
        }
    }

    /** Grants (or refreshes) a hidden ambient effect, or removes the one we granted earlier. */
    static void effect(ServerPlayer player, Holder<MobEffect> effect, boolean on, int amplifier, int duration) {
        MobEffectInstance current = player.getEffect(effect);
        boolean ours = current != null && current.isAmbient() && !current.isVisible() && current.getDuration() <= duration;
        if (!on) {
            if (ours) {
                player.removeEffect(effect);
            }
            return;
        }
        if (current == null || (ours && current.getDuration() < duration - 20) || (ours && current.getAmplifier() != amplifier)) {
            player.addEffect(new MobEffectInstance(effect, duration, Math.max(0, amplifier), true, false, true));
        }
    }

    /** Starlight Mending: heal under the night sky, paid with the chestplate's charge. */
    static void mend(ServerPlayer player, SetBonuses.Settings s) {
        if (player.getHealth() >= player.getMaxHealth()) {
            return;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        GearData d = GearManager.data(chest);
        int cost = AVServerConfig.SPEC.isLoaded() ? AVServerConfig.SERAPH_MENDING_COST.get() : 2;
        if (d.charge() < cost) {
            return;
        }
        GearManager.set(chest, d.withCharge(d.charge() - cost));
        player.heal((float) s.mendingAmount());
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1.2, player.getZ(), 4, 0.3, 0.5, 0.3, 0.01);
    }

    /** Seraph glide: sneak in mid-air to sail (Awakened); starting a glide flings you forward once per cooldown. */
    static void glide(ServerPlayer player, int level, SetBonuses.Settings s) {
        State state = STATES.computeIfAbsent(player.getUUID(), k -> new State());
        boolean sneaking = player.isShiftKeyDown();
        boolean pressed = sneaking && !state.sneakHeld;
        state.sneakHeld = sneaking;
        if (level < 1 || !sneaking || player.onGround() || player.getAbilities().flying || player.isInWater()
                || player.isPassenger() || player.getDeltaMovement().y > 0.05) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 10, 0, false, false));
        if (!pressed) {
            return;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        GearData d = GearManager.data(chest);
        long now = player.level().getGameTime();
        if (!SetBonuses.canBurst(level, d.charge(), now, state.burstReadyAt, s)) {
            return;
        }
        int cooldown = AVServerConfig.SPEC.isLoaded() ? AVServerConfig.SERAPH_BURST_COOLDOWN.get() : 160;
        double strength = AVServerConfig.SPEC.isLoaded() ? AVServerConfig.SERAPH_BURST_STRENGTH.get() : 1.3;
        state.burstReadyAt = now + cooldown;
        GearManager.set(chest, d.withCharge(d.charge() - s.burstCost()));
        media.hofferbert.astralvigil.eclipse.GazeManager.add(
                player, (int) Math.round(s.burstCost() * AVServerConfig.GAZE_PER_ABILITY_COST.get()));
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0e-4 ? Vec3.ZERO : flat.normalize().scale(strength);
        player.setDeltaMovement(flat.x, 0.45, flat.z);
        player.hurtMarked = true;
        player.fallDistance = 0;
        player.setData(media.hofferbert.astralvigil.registry.AVAttachments.WING_BURST, now); // Seraph Wings burst clip
        player.syncData(media.hofferbert.astralvigil.registry.AVAttachments.WING_BURST);
        ServerLevel server = (ServerLevel) player.level();
        server.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.6, 0.4, 0.6, 0.05);
        server.playSound(null, player.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0f, 1.3f);
        server.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 1.6f);
    }

    private static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player) {
            ArmorSets.fullSet(player).ifPresent(set -> {
                float m = SetBonuses.fallMultiplier(set, ArmorSets.level(player.getItemBySlot(EquipmentSlot.CHEST)), settings());
                if (m < 1f) {
                    event.setDamageMultiplier(event.getDamageMultiplier() * m);
                }
            });
        }
    }

    /** Grants/refreshes/removes the status-effect bonuses; returns the active bonuses. */
    static Set<Bonus> applyEffects(ServerPlayer player, ArmorSet set, int level, SetBonuses.Settings s) {
        Set<Bonus> active = set == null ? Set.of() : SetBonuses.active(set, level, sky(player.level(), player), s);
        effect(player, MobEffects.FIRE_RESISTANCE, active.contains(Bonus.FIRE_WARD), 0, DURATION);
        effect(player, STRENGTH, active.contains(Bonus.SOLAR_MIGHT), s.solarStrength() - 1, DURATION);
        effect(player, MobEffects.NIGHT_VISION, active.contains(Bonus.MOONSIGHT), 0, NIGHT_VISION_DURATION);
        effect(player, SPEED, active.contains(Bonus.MOONSTRIDE), s.moonstride() - 1, DURATION);
        return active;
    }

    /** GameTest hook: one bonus tick regardless of the tick counter; returns the active bonuses. */
    public static Set<Bonus> tickForTest(ServerPlayer player) {
        ArmorSet set = ArmorSets.fullSet(player).orElse(null);
        int level = set == ArmorSet.SERAPH ? ArmorSets.level(player.getItemBySlot(EquipmentSlot.CHEST)) : 0;
        return applyEffects(player, set, level, settings());
    }
}
