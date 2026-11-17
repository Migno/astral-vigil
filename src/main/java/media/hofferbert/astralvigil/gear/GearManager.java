package media.hofferbert.astralvigil.gear;

import java.util.List;
import java.util.Optional;
import media.hofferbert.astralvigil.compat.Celestial;
import media.hofferbert.astralvigil.compat.PlayerCompat;
import media.hofferbert.astralvigil.config.AVServerConfig;
import media.hofferbert.astralvigil.registry.AVComponents;
import media.hofferbert.astralvigil.registry.AVItems;
import media.hofferbert.astralvigil.research.Alignment;
import media.hofferbert.astralvigil.research.AlignmentTags;
import media.hofferbert.astralvigil.starlight.StarlightMath;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
//? if >=26.1 {
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
//?} else {
/*import net.minecraft.world.entity.projectile.AbstractArrow;
*///?}

/** Server-side living-gear logic: deeds, awakening, charge, sigil sockets, stances and abilities. */
public final class GearManager {
    public static final String TAG_LUNA = "astralvigil_luna";
    public static final String TAG_PRECISION = "astralvigil_precision";
    public static final String TAG_UNDERTOW = "astralvigil_undertow";
    /** Player persistent-data keys for "next hit/shot" empowerments. */
    static final String EMPOWER_KEY = "astralvigil_empower";
    static final String EMPOWER_UNTIL = "astralvigil_empower_until";

    private GearManager() {}

    // ------------------------------------------------------------------ data access

    public static GearData data(ItemStack stack) {
        GearData d = stack.get(AVComponents.GEAR.get());
        return d == null ? GearData.EMPTY : d;
    }

    public static void set(ItemStack stack, GearData data) {
        stack.set(AVComponents.GEAR.get(), data);
    }

    // ------------------------------------------------------------------ deeds

    /** Adds deeds and announces awakenings. */
    public static void addDeeds(ServerPlayer player, ItemStack stack, int amount) {
        if (amount <= 0) {
            return;
        }
        GearData before = data(stack);
        GearData after = before.withDeeds(before.deeds() + amount);
        set(stack, after);
        if (after.level() > before.level()) {
            player.sendSystemMessage(Component.translatable(
                            "message.astralvigil.gear.awakened",
                            stack.getHoverName(),
                            Component.translatable("gear.astralvigil.level." + after.level()))
                    .withStyle(ChatFormatting.GOLD));
            player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 1.2f);
        }
    }

    /** Called when a player kills something. Weapon = item that dealt the blow (may be the shooting bow). */
    public static void onKill(ServerPlayer player, LivingEntity victim, ItemStack weapon) {
        ServerLevel level = (ServerLevel) player.level();
        long time = Celestial.dayTime(level);
        boolean night = StarlightMath.isNight(time);
        GearItems.kind(weapon).ifPresent(kind -> {
            switch (kind) {
                case SWORD, AXE -> {
                    if (!night && victim.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD)) {
                        addDeeds(player, weapon, 1);
                    }
                }
                case BOW, CROSSBOW -> {
                    if (night) {
                        addDeeds(player, weapon, Celestial.moonPhase(level, player.blockPosition()) == 0 ? 3 : 1);
                    }
                }
                default -> {}
            }
        });
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (fullSeraph(player) && victim.getType().builtInRegistryHolder().is(AlignmentTags.entities(Alignment.VOID))) {
            addDeeds(player, chest, 1);
        }
    }

    public static void onShieldBlock(ServerPlayer player, ItemStack shield) {
        if (shield.is(AVItems.STARLIGHT_AEGIS.get())) {
            addDeeds(player, shield, 1);
        }
    }

    public static boolean fullSeraph(Player player) {
        return GearItems.isSeraph(player.getItemBySlot(EquipmentSlot.HEAD))
                && player.getItemBySlot(EquipmentSlot.CHEST).is(AVItems.SERAPH_CHESTPLATE.get())
                && GearItems.isSeraph(player.getItemBySlot(EquipmentSlot.LEGS))
                && GearItems.isSeraph(player.getItemBySlot(EquipmentSlot.FEET));
    }

    public static int armorLevel(Player player) {
        return fullSeraph(player) ? data(player.getItemBySlot(EquipmentSlot.CHEST)).level() : -1;
    }

    // ------------------------------------------------------------------ charge

    /** Passive recharge under the night sky; called every 40 ticks per player. */
    public static void recharge(ServerPlayer player) {
        double intensity = Celestial.starlight(player.level(), player.blockPosition());
        if (intensity <= 0) {
            return;
        }
        int amount = (int) Math.round(AVServerConfig.GEAR_CHARGE_RATE.get() * intensity);
        if (amount <= 0) {
            return;
        }
        int cap = AVServerConfig.GEAR_CHARGE_CAPACITY.get();
        for (ItemStack stack : gearInUse(player)) {
            GearData d = data(stack);
            if (d.charge() < cap) {
                set(stack, d.withCharge(Math.min(cap, d.charge() + amount)));
            }
        }
    }

    /** Main hand, off hand and chestplate – the pieces that can carry an active stance. */
    public static List<ItemStack> gearInUse(Player player) {
        return List.of(player.getMainHandItem(), player.getOffhandItem(), player.getItemBySlot(EquipmentSlot.CHEST)).stream()
                .filter(s -> GearItems.kind(s).isPresent())
                .toList();
    }

    /** Adds charge (e.g. from a reservoir); returns what was accepted. */
    public static int charge(ItemStack stack, int offered) {
        GearData d = data(stack);
        int cap = AVServerConfig.GEAR_CHARGE_CAPACITY.get();
        int take = Math.max(0, Math.min(offered, cap - d.charge()));
        if (take > 0) {
            set(stack, d.withCharge(d.charge() + take));
        }
        return take;
    }

    // ------------------------------------------------------------------ sockets + stances

    /** Sockets the sigil held in the off hand into the gear in the main hand. */
    public static boolean trySocket(ServerPlayer player) {
        ItemStack gear = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        Optional<GearKind> kind = GearItems.kind(gear);
        if (kind.isEmpty() || !(off.getItem() instanceof GearItems.SigilItem sigilItem)) {
            return false;
        }
        GearData d = data(gear);
        List<String> next = GearProgress.socket(d.sigils(), sigilItem.sigil(), kind.get(), d.level());
        if (next == null) {
            player.sendSystemMessage(Component.translatable("message.astralvigil.gear.socket_failed").withStyle(ChatFormatting.GRAY));
            return true;
        }
        set(gear, d.withSigils(next).withStance(next.size() - 1));
        if (!player.getAbilities().instabuild) {
            off.shrink(1);
        }
        player.sendSystemMessage(Component.translatable(
                "message.astralvigil.gear.socketed", Component.translatable(sigilItem.sigil().translationKey()), gear.getHoverName()));
        player.level().playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.4f);
        return true;
    }

    /** Picks the gear piece whose stance/ability the keys act on. */
    public static Optional<ItemStack> activeGear(Player player) {
        return gearInUse(player).stream().filter(s -> !data(s).sigils().isEmpty()).findFirst();
    }

    public static void cycleStance(ServerPlayer player) {
        activeGear(player).ifPresent(stack -> {
            GearData d = data(stack);
            GearData next = d.withStance(GearProgress.nextStance(d.stance(), d.sigils().size()));
            set(stack, next);
            next.activeSigil().ifPresent(s -> PlayerCompat.actionBar(player, Component.translatable(
                            "message.astralvigil.gear.stance", Component.translatable(s.translationKey()))
                    .withStyle(ChatFormatting.AQUA)));
        });
    }

    // ------------------------------------------------------------------ abilities

    public static void useAbility(ServerPlayer player) {
        Optional<ItemStack> gearOpt = activeGear(player);
        if (gearOpt.isEmpty()) {
            PlayerCompat.actionBar(player, Component.translatable("message.astralvigil.gear.no_stance").withStyle(ChatFormatting.GRAY));
            return;
        }
        ItemStack gear = gearOpt.get();
        GearData d = data(gear);
        Sigil sigil = d.activeSigil().orElse(null);
        if (sigil == null) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        if (now < d.cooldownUntil()) {
            PlayerCompat.actionBar(player, Component.translatable(
                            "message.astralvigil.gear.cooldown", (d.cooldownUntil() - now + 19) / 20)
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        int cost = GearProgress.scaled(sigil.baseCost(), AVServerConfig.ABILITY_COST_MULTIPLIER.get());
        if (d.charge() < cost) {
            PlayerCompat.actionBar(player, Component.translatable("message.astralvigil.gear.no_charge", cost)
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        int cooldown = GearProgress.scaled(sigil.baseCooldown(), AVServerConfig.ABILITY_COOLDOWN_MULTIPLIER.get());
        set(gear, d.withCharge(d.charge() - cost).withCooldownUntil(now + cooldown));
        perform(level, player, sigil, d.level());
    }

    static void perform(ServerLevel level, ServerPlayer player, Sigil sigil, int gearLevel) {
        Vec3 c = player.position().add(0, 1, 0);
        switch (sigil) {
            case WHIRL -> {
                double damage = player.getAttributeValue(Attributes.ATTACK_DAMAGE) * (0.6 + 0.1 * gearLevel);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(3.5),
                        e -> e != player && e.isAlive() && !(e instanceof Player))) {
                    e.hurt(level.damageSources().playerAttack(player), (float) damage);
                    Vec3 push = e.position().subtract(player.position()).normalize().scale(0.6);
                    e.push(push.x, 0.25, push.z);
                }
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 8, 1.5, 0.2, 1.5, 0);
                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.8f);
            }
            case SHIELDBREAKER, PRECISION, UNDERTOW -> {
                player.getPersistentData().putString(EMPOWER_KEY, sigil.id());
                player.getPersistentData().putLong(EMPOWER_UNTIL, level.getGameTime() + 200);
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, c.x, c.y, c.z, 20, 0.4, 0.5, 0.4, 0.1);
                level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.3f);
            }
            case DAWN -> {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6),
                        e -> e != player && e.isAlive())) {
                    if (e.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD)) {
                        e.igniteForSeconds(5);
                        e.hurt(level.damageSources().magic(), 4.0f + 2 * gearLevel);
                    } else if (!(e instanceof Player)) {
                        e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100));
                    }
                }
                level.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 80, 3, 1, 3, 0.05);
                level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.6f);
            }
            case FLASH -> {
                Vec3 look = player.getLookAngle();
                player.setDeltaMovement(look.x * 1.6, Math.max(0.15, look.y * 0.6), look.z * 1.6);
                player.hurtMarked = true;
                player.fallDistance = 0;
                level.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 30, 0.3, 0.5, 0.3, 0.02);
                level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0f, 1.4f);
            }
            case AEGIS -> {
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1 + Math.min(1, gearLevel)));
                //? if >=26.1 {
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 0));
                //?} else {
                /*player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0));
                *///?}
                level.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 40, 0.8, 1.0, 0.8, 0.01);
                level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.PLAYERS, 1.0f, 1.5f);
            }
        }
    }

    /** GameTest hook: performs an ability without cost or cooldown. */
    public static void performForTest(ServerLevel level, ServerPlayer player, Sigil sigil) {
        perform(level, player, sigil, 0);
    }

    /** Consumes a pending "next hit/shot" empowerment of the given kind. */
    public static boolean consumeEmpower(Player player, Sigil sigil) {
        var tag = player.getPersistentData();
        if (!sigil.id().equals(tag.getString(EMPOWER_KEY)
                //? if >=26.1 {
                .orElse("")
                //?}
                )) {
            return false;
        }
        long until = tag.getLong(EMPOWER_UNTIL)
                //? if >=26.1 {
                .orElse(0L)
                //?}
                ;
        tag.remove(EMPOWER_KEY);
        tag.remove(EMPOWER_UNTIL);
        return player.level().getGameTime() <= until;
    }

    // ------------------------------------------------------------------ combat hooks

    /** Called by the Luna bows when an arrow is created: tags it for night/precision/undertow bonuses. */
    public static AbstractArrow empowerArrow(AbstractArrow arrow, ItemStack weapon) {
        arrow.addTag(TAG_LUNA);
        if (arrow.getOwner() instanceof Player player) {
            if (consumeEmpower(player, Sigil.PRECISION)) {
                arrow.addTag(TAG_PRECISION);
            } else if (consumeEmpower(player, Sigil.UNDERTOW)) {
                arrow.addTag(TAG_UNDERTOW);
            }
        }
        return arrow;
    }

    /**
     * Damage multiplier for a hit. {@code direct} is the arrow/player that hit, {@code attacker} the player behind it,
     * {@code weapon} the gear that fired/struck.
     */
    public static float damageMultiplier(ServerLevel level, Entity direct, Player attacker, ItemStack weapon, LivingEntity target) {
        float m = 1.0f;
        int gearLevel = data(weapon).level();
        if (direct instanceof AbstractArrow arrow && hasTag(arrow, TAG_LUNA)) {
            if (StarlightMath.isNight(Celestial.dayTime(level))) {
                m += 0.15f;
            }
            m += 0.10f * gearLevel;
            if (hasTag(arrow, TAG_PRECISION)) {
                m += 0.6f;
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200));
            }
        } else if (direct == attacker) {
            if (GearItems.kind(weapon).map(k -> k == GearKind.SWORD || k == GearKind.AXE).orElse(false)
                    && target.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD)) {
                m += 0.15f * gearLevel;
            }
            if (consumeEmpower(attacker, Sigil.SHIELDBREAKER)) {
                m += 0.4f;
                level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1, target.getZ(), 20, 0.3, 0.3, 0.3, 0.2);
            }
        }
        return m;
    }

    static boolean hasTag(Entity entity, String tag) {
        //? if >=26.1 {
        return entity.entityTags().contains(tag);
        //?} else {
        /*return entity.getTags().contains(tag);
        *///?}
    }

    /** After an undertow arrow hits: pull the target towards the shooter. */
    public static void afterHit(Entity direct, Player attacker, LivingEntity target) {
        if (direct instanceof AbstractArrow arrow && hasTag(arrow, TAG_UNDERTOW)) {
            Vec3 pull = attacker.position().subtract(target.position()).normalize().scale(1.2);
            target.push(pull.x, 0.35, pull.z);
            target.hurtMarked = true;
        }
    }
}
