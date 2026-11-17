package media.hofferbert.astralvigil.gear;

import media.hofferbert.astralvigil.starlight.Tint;
import media.hofferbert.astralvigil.starlight.network.Devices;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Game-bus hooks for living gear. */
public final class GearEvents {
    private GearEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(GearEvents::onDeath);
        NeoForge.EVENT_BUS.addListener(GearEvents::onDamage);
        NeoForge.EVENT_BUS.addListener(GearEvents::onShieldBlock);
        NeoForge.EVENT_BUS.addListener(GearEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(GearEvents::onFall);
        NeoForge.EVENT_BUS.addListener(GearEvents::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(GearEvents::onRightClickBlock);
    }

    private static void onDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            ItemStack weapon = event.getSource().getWeaponItem();
            GearManager.onKill(player, event.getEntity(), weapon == null ? ItemStack.EMPTY : weapon);
        }
    }

    private static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)
                || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        Entity direct = event.getSource().getDirectEntity();
        ItemStack weapon = event.getSource().getWeaponItem();
        if (weapon == null || GearItems.kind(weapon).isEmpty()) {
            return;
        }
        float multiplier = GearManager.damageMultiplier(level, direct, attacker, weapon, event.getEntity());
        if (multiplier != 1.0f) {
            event.setAmount(event.getAmount() * multiplier);
        }
        GearManager.afterHit(direct, attacker, event.getEntity());
    }

    private static void onShieldBlock(LivingShieldBlockEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getBlocked()) {
            GearManager.onShieldBlock(player, player.getUseItem());
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 40 == 0) {
            GearManager.recharge(player);
        }
        // Seraph Plate, awakened: glide (slow falling) while sneaking in the air
        if (player.isShiftKeyDown() && !player.onGround() && player.getDeltaMovement().y < -0.1
                && GearManager.armorLevel(player) >= 1) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 10, 0, false, false));
        }
    }

    private static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && GearManager.armorLevel(player) >= 2) {
            event.setDamageMultiplier(0f);
        }
    }

    /** Sneak + use gear with a sigil in the off hand → socket it. */
    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                && player.isShiftKeyDown()
                && player.getOffhandItem().getItem() instanceof GearItems.SigilItem
                && GearItems.kind(player.getMainHandItem()).isPresent()
                && GearManager.trySocket(player)) {
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    /** Use gear on a Starlight Reservoir → recharge it from the reservoir's light. */
    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // Socketing works whether the player aims at a block or into the air.
        if (event.getEntity() instanceof ServerPlayer socketing
                && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                && socketing.isShiftKeyDown()
                && socketing.getOffhandItem().getItem() instanceof GearItems.SigilItem
                && GearItems.kind(socketing.getMainHandItem()).isPresent()
                && GearManager.trySocket(socketing)) {
            event.setCanceled(true);
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level().getBlockEntity(event.getPos()) instanceof Devices.Reservoir reservoir)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (GearItems.kind(stack).isEmpty()) {
            return;
        }
        int moved = 0;
        for (Tint t : Tint.values()) {
            int offered = reservoir.buffer().get(t);
            if (offered > 0) {
                int taken = GearManager.charge(stack, offered);
                reservoir.buffer().extract(t, taken, false);
                moved += taken;
            }
        }
        if (moved > 0) {
            reservoir.syncNow();
            player.level().playSound(null, event.getPos(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.8f);
        }
        event.setCanceled(true);
    }
}
