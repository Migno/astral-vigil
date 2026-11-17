package media.hofferbert.astralvigil.item;

import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.registry.AVComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
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
 * One of 24 pages of Ilyra's lost chronicle ({@code astralvigil:fragment} component = 1..24). Reading it adds the
 * page to the Codex (advancement {@code astralvigil:lore/fragment_N}).
 */
public class LoreFragmentItem extends Item {
    public static final int COUNT = 24;

    public LoreFragmentItem(Properties properties) {
        super(properties);
    }

    public static int index(ItemStack stack) {
        Integer i = stack.get(AVComponents.FRAGMENT.get());
        return i == null ? 1 : Math.max(1, Math.min(COUNT, i));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.astralvigil.lore_fragment.numbered", index(stack));
    }

    private static void read(Level level, Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer sp)) {
            return;
        }
        int i = index(stack);
        sp.sendSystemMessage(Component.translatable("lore.astralvigil.fragment_" + i + ".title")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        sp.sendSystemMessage(Component.translatable("lore.astralvigil.fragment_" + i + ".text").withStyle(ChatFormatting.GRAY));
        var server = sp.level().getServer();
        if (server != null) {
            AdvancementHolder adv = server.getAdvancements().get(AstralVigil.id("lore/fragment_" + i));
            if (adv != null) {
                sp.getAdvancements().award(adv, "read");
            }
        }
        level.playSound(null, sp.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    //? if >=26.1 {
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        read(level, player, player.getItemInHand(hand));
        return InteractionResult.SUCCESS;
    }
    //?} else {
    /*@Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        read(level, player, player.getItemInHand(hand));
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
    *///?}
}
