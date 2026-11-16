package media.hofferbert.astralvigil.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Small player API differences between 1.21.1 and 26.1. */
public final class PlayerCompat {
    private PlayerCompat() {}

    public static void actionBar(Player player, Component message) {
        //? if >=26.1 {
        player.sendOverlayMessage(message);
        //?} else {
        /*player.displayClientMessage(message, true);
        *///?}
    }

    public static void cooldown(Player player, ItemStack stack, int ticks) {
        if (ticks <= 0) {
            return;
        }
        //? if >=26.1 {
        player.getCooldowns().addCooldown(stack, ticks);
        //?} else {
        /*player.getCooldowns().addCooldown(stack.getItem(), ticks);
        *///?}
    }
}
