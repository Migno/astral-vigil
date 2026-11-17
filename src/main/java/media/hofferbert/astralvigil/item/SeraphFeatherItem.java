package media.hofferbert.astralvigil.item;

import media.hofferbert.astralvigil.registry.AVAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
//? if >=26.1 {
import net.minecraft.world.InteractionResult;
//?} else {
/*import net.minecraft.world.InteractionResultHolder;
*///?}

/**
 * Seraph Feather (dropped by the Eclipse): the first use binds Seraph Wings to the player (cosmetic, visible to
 * everyone); every further use toggles them. Only the binding consumes a feather.
 */
public class SeraphFeatherItem extends Item {
    public static final int UNBOUND = 0;
    public static final int HIDDEN = 1;
    public static final int SHOWN = 2;

    public SeraphFeatherItem(Properties properties) {
        super(properties);
    }

    public static void toggle(ServerPlayer player, ItemStack stack) {
        int state = player.getData(AVAttachments.WINGS);
        int next = state == SHOWN ? HIDDEN : SHOWN;
        if (state == UNBOUND && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.setData(AVAttachments.WINGS, next);
        player.syncData(AVAttachments.WINGS);
        player.sendSystemMessage(Component.translatable(next == SHOWN
                ? "message.astralvigil.wings.shown" : "message.astralvigil.wings.hidden").withStyle(ChatFormatting.GOLD), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_ELYTRA.value(), SoundSource.PLAYERS, 1.0f, 1.3f);
    }

    //? if >=26.1 {
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            toggle(sp, player.getItemInHand(hand));
        }
        return InteractionResult.SUCCESS;
    }
    //?} else {
    /*@Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            toggle(sp, player.getItemInHand(hand));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
    *///?}
}
