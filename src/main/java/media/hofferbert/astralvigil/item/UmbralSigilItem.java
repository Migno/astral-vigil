package media.hofferbert.astralvigil.item;

import media.hofferbert.astralvigil.eclipse.EclipseEntity;
import media.hofferbert.astralvigil.firmament.Firmament;
import media.hofferbert.astralvigil.firmament.Watcher;
import media.hofferbert.astralvigil.registry.AVItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
//? if >=26.1 {
import net.minecraft.world.InteractionResult;
//?} else {
/*import net.minecraft.world.InteractionResultHolder;
*///?}

/** Umbral Sigil: in the Firmament, with all four Watchers' Marks, calls down the Eclipse. Consumed. */
public class UmbralSigilItem extends Item {
    public UmbralSigilItem(Properties properties) {
        super(properties);
    }

    /** @return true if the Eclipse was summoned */
    public static boolean trySummon(ServerPlayer player, ItemStack stack) {
        if (!Firmament.isFirmament(player.level())) {
            player.sendSystemMessage(Component.translatable("message.astralvigil.umbral.not_here").withStyle(ChatFormatting.DARK_PURPLE));
            return false;
        }
        for (Watcher w : Watcher.values()) {
            if (!player.getInventory().contains(new ItemStack(AVItems.MARKS.get(w).get()))) {
                player.sendSystemMessage(Component.translatable("message.astralvigil.umbral.marks").withStyle(ChatFormatting.DARK_PURPLE));
                return false;
            }
        }
        ServerLevel level = (ServerLevel) player.level();
        if (level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            player.sendSystemMessage(Component.translatable("message.astralvigil.umbral.peaceful").withStyle(ChatFormatting.DARK_PURPLE));
            return false;
        }
        if (!level.getEntitiesOfClass(EclipseEntity.class, player.getBoundingBox().inflate(96)).isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.astralvigil.umbral.present").withStyle(ChatFormatting.DARK_PURPLE));
            return false;
        }
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        EclipseEntity.summon(level, player.position().add(look.scale(8)).add(0, 3, 0));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return true;
    }

    //? if >=26.1 {
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            trySummon(sp, player.getItemInHand(hand));
        }
        return InteractionResult.SUCCESS;
    }
    //?} else {
    /*@Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            trySummon(sp, player.getItemInHand(hand));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
    *///?}
}
