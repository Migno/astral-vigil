package media.hofferbert.astralvigil.crafting;

import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Block entities that show their items floating above the block (client renderer reads this). */
public interface ItemHolder {
    List<ItemStack> displayedItems();

    /** Height above the block's bottom at which items float. */
    default float displayHeight() {
        return 1.15f;
    }
}
