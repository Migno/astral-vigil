package media.hofferbert.astralvigil.registry;

import java.util.function.Function;
import java.util.function.UnaryOperator;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.item.AstrolabeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AVItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AstralVigil.MOD_ID);

    public static final DeferredItem<Item> ASTRAL_SHARD =
            register("astral_shard", Item::new, p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<AstrolabeItem> ASTROLABE =
            register("astrolabe", AstrolabeItem::new, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));

    public static final DeferredItem<BlockItem> FALLEN_STAR = ITEMS.registerSimpleBlockItem(AVBlocks.FALLEN_STAR);
    public static final DeferredItem<BlockItem> METEORIC_STONE = ITEMS.registerSimpleBlockItem(AVBlocks.METEORIC_STONE);

    private AVItems() {}

    static <I extends Item> DeferredItem<I> register(
            String name, Function<Item.Properties, I> factory, UnaryOperator<Item.Properties> props) {
        //? if >=26.1 {
        return ITEMS.registerItem(name, factory, props);
        //?} else {
        /*return ITEMS.registerItem(name, factory, props.apply(new Item.Properties()));
        *///?}
    }
}
