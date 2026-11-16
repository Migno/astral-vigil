package media.hofferbert.astralvigil.item;

import media.hofferbert.astralvigil.compat.PatchouliCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
//? if >=26.1 {
import net.minecraft.world.InteractionResult;
//?} else {
/*import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
*///?}

/**
 * Codex Caelestis – the in-game guide. Rendered by Patchouli when installed (optional dependency); without Patchouli
 * the item explains how to get the book.
 */
public class CodexItem extends Item {
    public CodexItem(Properties properties) {
        super(properties);
    }

    private static void open(Level level, Player player) {
        if (level.isClientSide() || !(player instanceof ServerPlayer sp)) {
            return;
        }
        if (!PatchouliCompat.openCodex(sp)) {
            sp.sendSystemMessage(Component.translatable("message.astralvigil.codex.no_patchouli").withStyle(ChatFormatting.GRAY));
        }
    }

    //? if >=26.1 {
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        open(level, player);
        return InteractionResult.SUCCESS;
    }
    //?} else {
    /*@Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        open(level, player);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
    *///?}
}
