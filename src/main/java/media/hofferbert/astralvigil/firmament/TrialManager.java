package media.hofferbert.astralvigil.firmament;

import java.util.HashMap;
import java.util.Map;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.registry.AVAttachments;
import media.hofferbert.astralvigil.registry.AVItems;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * Sanctum trials and Watcher favour. Talking to a Watcher starts (or reports) its trial; completing it grants the
 * Watcher's Mark once, favour with that Watcher and costs favour with the rival (Solenne ↔ Vael, Orrin ↔ Ilyra).
 */
public final class TrialManager {
    public static final int TRIAL_TICKS = 20 * 60 * 20;
    public static final int FAVOUR_TRIAL = 50;
    public static final int FAVOUR_RIVAL_LOSS = 15;
    static final String TRIAL_KEY = "astralvigil_trial";
    static final String TRIAL_UNTIL = "astralvigil_trial_until";

    private TrialManager() {}

    // ------------------------------------------------------------------ favour

    public static Map<String, Integer> favour(ServerPlayer player) {
        return player.getData(AVAttachments.FAVOUR);
    }

    public static int favour(ServerPlayer player, Watcher w) {
        return favour(player).getOrDefault(w.id(), 0);
    }

    public static void addFavour(ServerPlayer player, Watcher w, int amount) {
        Map<String, Integer> next = new HashMap<>(favour(player));
        next.merge(w.id(), amount, Integer::sum);
        next.put(w.id(), Math.max(-100, Math.min(1000, next.get(w.id()))));
        player.setData(AVAttachments.FAVOUR, Map.copyOf(next));
        player.syncData(AVAttachments.FAVOUR);
    }

    // ------------------------------------------------------------------ trials

    public static String activeTrial(ServerPlayer player) {
        var tag = player.getPersistentData();
        //? if >=26.1 {
        String id = tag.getStringOr(TRIAL_KEY, "");
        long until = tag.getLongOr(TRIAL_UNTIL, 0L);
        //?} else {
        /*String id = tag.getString(TRIAL_KEY);
        long until = tag.getLong(TRIAL_UNTIL);
        *///?}
        if (!id.isEmpty() && player.level().getGameTime() > until) {
            clear(player);
            player.sendSystemMessage(Component.translatable("message.astralvigil.trial.expired").withStyle(ChatFormatting.GRAY));
            return "";
        }
        return id;
    }

    static void clear(ServerPlayer player) {
        player.getPersistentData().remove(TRIAL_KEY);
        player.getPersistentData().remove(TRIAL_UNTIL);
    }

    public static boolean hasMark(ServerPlayer player, Watcher w) {
        ItemStack mark = new ItemStack(AVItems.MARKS.get(w).get());
        return player.getInventory().contains(mark) || hasAdvancement(player, "firmament/trial_" + w.id());
    }

    /** @return true if the Watcher should play its cast animation */
    public static boolean interact(ServerPlayer player, WatcherEntity entity) {
        Watcher w = entity.watcher();
        Component name = Component.translatable(w.translationKey());
        if (hasAdvancement(player, "firmament/trial_" + w.id())) {
            player.sendSystemMessage(Component.translatable("message.astralvigil.watcher.favour", name, favour(player, w))
                    .withStyle(ChatFormatting.GOLD));
            return false;
        }
        String active = activeTrial(player);
        if (w.id().equals(active)) {
            player.sendSystemMessage(Component.translatable("trial.astralvigil." + w.id() + ".hint").withStyle(ChatFormatting.AQUA));
            return false;
        }
        player.getPersistentData().putString(TRIAL_KEY, w.id());
        player.getPersistentData().putLong(TRIAL_UNTIL, player.level().getGameTime() + TRIAL_TICKS);
        player.sendSystemMessage(Component.translatable("trial.astralvigil." + w.id() + ".intro", name).withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable("trial.astralvigil." + w.id() + ".hint").withStyle(ChatFormatting.AQUA));
        return true;
    }

    /** Completes the player's trial if it is the given Watcher's. */
    public static boolean complete(ServerPlayer player, Watcher w) {
        if (!w.id().equals(activeTrial(player))) {
            return false;
        }
        clear(player);
        award(player, "firmament/trial_" + w.id());
        ItemStack mark = new ItemStack(AVItems.MARKS.get(w).get());
        if (!player.getInventory().add(mark)) {
            player.drop(mark, false);
        }
        addFavour(player, w, FAVOUR_TRIAL);
        addFavour(player, w.rival(), -FAVOUR_RIVAL_LOSS);
        player.sendSystemMessage(Component.translatable(
                        "message.astralvigil.trial.complete", Component.translatable(w.translationKey()))
                .withStyle(ChatFormatting.GOLD));
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
        return true;
    }

    /** Completes trials for all players near {@code pos} that run the given Watcher's trial. */
    public static void completeNear(net.minecraft.server.level.ServerLevel level, BlockPos pos, Watcher w, double radius) {
        for (ServerPlayer p : level.players()) {
            if (p.blockPosition().closerThan(pos, radius)) {
                complete(p, w);
            }
        }
    }

    static boolean hasAdvancement(ServerPlayer player, String path) {
        var server = player.level().getServer();
        if (server == null) {
            return false;
        }
        AdvancementHolder adv = server.getAdvancements().get(AstralVigil.id(path));
        return adv != null && player.getAdvancements().getOrStartProgress(adv).isDone();
    }

    public static void award(ServerPlayer player, String path) {
        var server = player.level().getServer();
        if (server == null) {
            return;
        }
        AdvancementHolder adv = server.getAdvancements().get(AstralVigil.id(path));
        if (adv != null) {
            for (String criterion : player.getAdvancements().getOrStartProgress(adv).getRemainingCriteria()) {
                player.getAdvancements().award(adv, criterion);
            }
        }
    }
}
