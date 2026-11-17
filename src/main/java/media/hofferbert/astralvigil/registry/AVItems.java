package media.hofferbert.astralvigil.registry;

import java.util.function.Function;
import java.util.function.UnaryOperator;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.item.AstrolabeItem;
import media.hofferbert.astralvigil.item.CodexItem;
import media.hofferbert.astralvigil.item.StarChartItem;
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
    public static final DeferredItem<StarChartItem> STAR_CHART =
            register("star_chart", StarChartItem::new, p -> p.stacksTo(1));
    public static final DeferredItem<CodexItem> CODEX_CAELESTIS =
            register("codex_caelestis", CodexItem::new, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));

    public static final DeferredItem<BlockItem> FALLEN_STAR = ITEMS.registerSimpleBlockItem(AVBlocks.FALLEN_STAR);
    public static final DeferredItem<BlockItem> METEORIC_STONE = ITEMS.registerSimpleBlockItem(AVBlocks.METEORIC_STONE);
    public static final DeferredItem<BlockItem> STARLIGHT_COLLECTOR = ITEMS.registerSimpleBlockItem(AVBlocks.STARLIGHT_COLLECTOR);
    public static final DeferredItem<BlockItem> LENS = ITEMS.registerSimpleBlockItem(AVBlocks.LENS);
    public static final DeferredItem<BlockItem> MIRROR = ITEMS.registerSimpleBlockItem(AVBlocks.MIRROR);
    public static final DeferredItem<BlockItem> PRISM = ITEMS.registerSimpleBlockItem(AVBlocks.PRISM);
    public static final DeferredItem<BlockItem> STARLIGHT_RESERVOIR = ITEMS.registerSimpleBlockItem(AVBlocks.STARLIGHT_RESERVOIR);

    // Phase 4 – materials
    public static final DeferredItem<Item> STARMETAL_INGOT = register("starmetal_ingot", Item::new, p -> p);
    public static final DeferredItem<Item> SUNSTEEL_INGOT = register("sunsteel_ingot", Item::new, p -> p);
    public static final DeferredItem<Item> MOONSILVER_INGOT = register("moonsilver_ingot", Item::new, p -> p);
    public static final DeferredItem<Item> ASTRAL_CORE = register("astral_core", Item::new, p -> p.rarity(Rarity.RARE));
    public static final DeferredItem<Item> SIGIL_BLANK = register("sigil_blank", Item::new, p -> p.stacksTo(16));
    public static final DeferredItem<BlockItem> PEDESTAL = ITEMS.registerSimpleBlockItem(AVBlocks.PEDESTAL);
    public static final DeferredItem<BlockItem> CONVERGENCE_ALTAR = ITEMS.registerSimpleBlockItem(AVBlocks.CONVERGENCE_ALTAR);
    public static final DeferredItem<BlockItem> ASTRAL_FORGE = ITEMS.registerSimpleBlockItem(AVBlocks.ASTRAL_FORGE);

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
