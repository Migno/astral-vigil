package media.hofferbert.astralvigil.item;

import media.hofferbert.astralvigil.firmament.Firmament;
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

/** Ascension Key: carries its holder up into the Firmament and back down again. */
public class AscensionKeyItem extends Item {
    public AscensionKeyItem(Properties properties) {
        super(properties);
    }

    private static void travel(Player player) {
        if (player instanceof ServerPlayer sp) {
            Firmament.travel(sp);
            sp.getCooldowns().addCooldown(
                    //? if >=26.1 {
                    sp.getMainHandItem(),
                    //?} else {
                    /*sp.getMainHandItem().getItem(),
                    *///?}
                    100);
        }
    }

    //? if >=26.1 {
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        travel(player);
        return InteractionResult.SUCCESS;
    }
    //?} else {
    /*@Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        travel(player);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
    *///?}
}
