package media.hofferbert.astralvigil.registry;

import java.util.function.Function;
import java.util.function.UnaryOperator;
import media.hofferbert.astralvigil.AstralVigil;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import media.hofferbert.astralvigil.gear.AVMaterials;
import media.hofferbert.astralvigil.gear.GearItems;
import media.hofferbert.astralvigil.gear.Sigil;
import media.hofferbert.astralvigil.item.AstrolabeItem;
import media.hofferbert.astralvigil.item.CodexItem;
import media.hofferbert.astralvigil.item.StarChartItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.component.ChargedProjectiles;
//? if >=26.1 {
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.equipment.ArmorType;
//?} else {
/*import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.SwordItem;
*///?}
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

    // Phase 5 – living gear
    //? if >=26.1 {
    public static final DeferredItem<Item> HALO_EDGE = AVItems.<Item>register(
            "halo_edge", Item::new, p -> p.sword(AVMaterials.SUNSTEEL, 3.0f, -2.4f).rarity(Rarity.RARE));
    public static final DeferredItem<Item> DAWNBREAKER = AVItems.<Item>register(
            "dawnbreaker", p -> new AxeItem(AVMaterials.SUNSTEEL, 5.0f, -3.0f, p), p -> p.rarity(Rarity.RARE));
    public static final DeferredItem<Item> STARLIGHT_AEGIS = AVItems.<Item>register("starlight_aegis", GearItems.Aegis::new, p -> p
            .durability(700)
            .rarity(Rarity.RARE)
            .repairable(AVTags.Items.INGOTS_STARMETAL)
            .equippableUnswappable(EquipmentSlot.OFFHAND)
            .delayedComponent(DataComponents.BLOCKS_ATTACKS, ctx -> new BlocksAttacks(
                    0.25f,
                    1.0f,
                    List.of(new BlocksAttacks.DamageReduction(90.0f, Optional.empty(), 0.0f, 1.0f)),
                    new BlocksAttacks.ItemDamageFunction(3.0f, 1.0f, 1.0f),
                    Optional.of(ctx.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
                    Optional.of(SoundEvents.SHIELD_BLOCK),
                    Optional.of(SoundEvents.SHIELD_BREAK)))
            .component(DataComponents.BREAK_SOUND, SoundEvents.SHIELD_BREAK));
    public static final DeferredItem<Item> SERAPH_HELMET =
            AVItems.<Item>register("seraph_helmet", Item::new, p -> p.humanoidArmor(AVMaterials.SERAPH, ArmorType.HELMET).rarity(Rarity.RARE));
    public static final DeferredItem<Item> SERAPH_CHESTPLATE =
            AVItems.<Item>register("seraph_chestplate", Item::new, p -> p.humanoidArmor(AVMaterials.SERAPH, ArmorType.CHESTPLATE).rarity(Rarity.RARE));
    public static final DeferredItem<Item> SERAPH_LEGGINGS =
            AVItems.<Item>register("seraph_leggings", Item::new, p -> p.humanoidArmor(AVMaterials.SERAPH, ArmorType.LEGGINGS).rarity(Rarity.RARE));
    public static final DeferredItem<Item> SERAPH_BOOTS =
            AVItems.<Item>register("seraph_boots", Item::new, p -> p.humanoidArmor(AVMaterials.SERAPH, ArmorType.BOOTS).rarity(Rarity.RARE));
    // Celestial armour sets (mid tier): Solar Regalia (Sunsteel, Solenne) and Lunar Raiment (Moonsilver, Vael)
    public static final DeferredItem<Item> SOLAR_HELMET = AVItems.<Item>register("solar_helmet", Item::new,
            p -> p.humanoidArmor(AVMaterials.SOLAR, ArmorType.HELMET).rarity(Rarity.UNCOMMON).fireResistant());
    public static final DeferredItem<Item> SOLAR_CHESTPLATE = AVItems.<Item>register("solar_chestplate", Item::new,
            p -> p.humanoidArmor(AVMaterials.SOLAR, ArmorType.CHESTPLATE).rarity(Rarity.UNCOMMON).fireResistant());
    public static final DeferredItem<Item> SOLAR_LEGGINGS = AVItems.<Item>register("solar_leggings", Item::new,
            p -> p.humanoidArmor(AVMaterials.SOLAR, ArmorType.LEGGINGS).rarity(Rarity.UNCOMMON).fireResistant());
    public static final DeferredItem<Item> SOLAR_BOOTS = AVItems.<Item>register("solar_boots", Item::new,
            p -> p.humanoidArmor(AVMaterials.SOLAR, ArmorType.BOOTS).rarity(Rarity.UNCOMMON).fireResistant());
    public static final DeferredItem<Item> LUNAR_HELMET = AVItems.<Item>register("lunar_helmet", Item::new,
            p -> p.humanoidArmor(AVMaterials.LUNAR, ArmorType.HELMET).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> LUNAR_CHESTPLATE = AVItems.<Item>register("lunar_chestplate", Item::new,
            p -> p.humanoidArmor(AVMaterials.LUNAR, ArmorType.CHESTPLATE).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> LUNAR_LEGGINGS = AVItems.<Item>register("lunar_leggings", Item::new,
            p -> p.humanoidArmor(AVMaterials.LUNAR, ArmorType.LEGGINGS).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> LUNAR_BOOTS = AVItems.<Item>register("lunar_boots", Item::new,
            p -> p.humanoidArmor(AVMaterials.LUNAR, ArmorType.BOOTS).rarity(Rarity.UNCOMMON));
    //?} else {
    /*public static final DeferredItem<Item> HALO_EDGE = AVItems.<Item>register("halo_edge",
            p -> new SwordItem(AVMaterials.SUNSTEEL, p),
            p -> p.attributes(SwordItem.createAttributes(AVMaterials.SUNSTEEL, 3, -2.4f)).rarity(Rarity.RARE));
    public static final DeferredItem<Item> DAWNBREAKER = AVItems.<Item>register("dawnbreaker",
            p -> new AxeItem(AVMaterials.SUNSTEEL, p),
            p -> p.attributes(AxeItem.createAttributes(AVMaterials.SUNSTEEL, 5.0f, -3.0f)).rarity(Rarity.RARE));
    public static final DeferredItem<Item> STARLIGHT_AEGIS =
            AVItems.<Item>register("starlight_aegis", GearItems.Aegis::new, p -> p.durability(700).rarity(Rarity.RARE));
    public static final DeferredItem<Item> SERAPH_HELMET = armor("seraph_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<Item> SERAPH_CHESTPLATE = armor("seraph_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<Item> SERAPH_LEGGINGS = armor("seraph_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<Item> SERAPH_BOOTS = armor("seraph_boots", ArmorItem.Type.BOOTS);
    public static final DeferredItem<Item> SOLAR_HELMET = armor("solar_helmet", AVMaterials.SOLAR, ArmorItem.Type.HELMET, AVMaterials.SOLAR_DURABILITY_FACTOR, true);
    public static final DeferredItem<Item> SOLAR_CHESTPLATE = armor("solar_chestplate", AVMaterials.SOLAR, ArmorItem.Type.CHESTPLATE, AVMaterials.SOLAR_DURABILITY_FACTOR, true);
    public static final DeferredItem<Item> SOLAR_LEGGINGS = armor("solar_leggings", AVMaterials.SOLAR, ArmorItem.Type.LEGGINGS, AVMaterials.SOLAR_DURABILITY_FACTOR, true);
    public static final DeferredItem<Item> SOLAR_BOOTS = armor("solar_boots", AVMaterials.SOLAR, ArmorItem.Type.BOOTS, AVMaterials.SOLAR_DURABILITY_FACTOR, true);
    public static final DeferredItem<Item> LUNAR_HELMET = armor("lunar_helmet", AVMaterials.LUNAR, ArmorItem.Type.HELMET, AVMaterials.LUNAR_DURABILITY_FACTOR, false);
    public static final DeferredItem<Item> LUNAR_CHESTPLATE = armor("lunar_chestplate", AVMaterials.LUNAR, ArmorItem.Type.CHESTPLATE, AVMaterials.LUNAR_DURABILITY_FACTOR, false);
    public static final DeferredItem<Item> LUNAR_LEGGINGS = armor("lunar_leggings", AVMaterials.LUNAR, ArmorItem.Type.LEGGINGS, AVMaterials.LUNAR_DURABILITY_FACTOR, false);
    public static final DeferredItem<Item> LUNAR_BOOTS = armor("lunar_boots", AVMaterials.LUNAR, ArmorItem.Type.BOOTS, AVMaterials.LUNAR_DURABILITY_FACTOR, false);

    private static DeferredItem<Item> armor(String name, ArmorItem.Type type) {
        return AVItems.<Item>register(name, p -> new ArmorItem(AVMaterials.SERAPH, type, p),
                p -> p.durability(type.getDurability(AVMaterials.SERAPH_DURABILITY_FACTOR)).rarity(Rarity.RARE));
    }

    private static DeferredItem<Item> armor(String name, net.minecraft.core.Holder<net.minecraft.world.item.ArmorMaterial> material,
            ArmorItem.Type type, int factor, boolean fireResistant) {
        return AVItems.<Item>register(name, p -> new ArmorItem(material, type, p), p -> {
            Item.Properties props = p.durability(type.getDurability(factor)).rarity(Rarity.UNCOMMON);
            return fireResistant ? props.fireResistant() : props;
        });
    }
    *///?}
    public static final DeferredItem<Item> CRESCENT = AVItems.<Item>register(
            "crescent", GearItems.Crescent::new, p -> p.durability(640).rarity(Rarity.RARE));
    public static final DeferredItem<Item> TIDECALLER = AVItems.<Item>register("tidecaller", GearItems.Tidecaller::new, p -> p
            .stacksTo(1)
            .durability(700)
            .rarity(Rarity.RARE)
            .component(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY));

    public static final Map<Sigil, DeferredItem<Item>> SIGILS = sigils();

    private static Map<Sigil, DeferredItem<Item>> sigils() {
        Map<Sigil, DeferredItem<Item>> map = new EnumMap<>(Sigil.class);
        for (Sigil s : Sigil.values()) {
            map.put(s, AVItems.<Item>register(
                    "sigil_" + s.id(), p -> new GearItems.SigilItem(p, s), p -> p.stacksTo(16).rarity(Rarity.UNCOMMON)));
        }
        return map;
    }

    // Phase 6 – world
    public static final DeferredItem<BlockItem> STAR_ANCHOR = ITEMS.registerSimpleBlockItem(AVBlocks.STAR_ANCHOR);
    public static final DeferredItem<BlockItem> CONSTELLATION_LOCK = ITEMS.registerSimpleBlockItem(AVBlocks.CONSTELLATION_LOCK);
    public static final DeferredItem<BlockItem> VOID_CRYSTAL = ITEMS.registerSimpleBlockItem(AVBlocks.VOID_CRYSTAL);
    public static final DeferredItem<Item> LORE_FRAGMENT = AVItems.<Item>register(
            "lore_fragment", media.hofferbert.astralvigil.item.LoreFragmentItem::new, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> VOID_SHARD = register("void_shard", Item::new, p -> p.rarity(Rarity.UNCOMMON));

    // Phase 7 – Firmament
    public static final DeferredItem<BlockItem> FIRMAMENT_STONE = ITEMS.registerSimpleBlockItem(AVBlocks.FIRMAMENT_STONE);
    public static final java.util.Map<media.hofferbert.astralvigil.firmament.GateKind, DeferredItem<BlockItem>> GATES = gates();
    public static final DeferredItem<BlockItem> ASTRAL_GATE = GATES.get(media.hofferbert.astralvigil.firmament.GateKind.ASTRAL);

    private static java.util.Map<media.hofferbert.astralvigil.firmament.GateKind, DeferredItem<BlockItem>> gates() {
        var map = new java.util.EnumMap<media.hofferbert.astralvigil.firmament.GateKind, DeferredItem<BlockItem>>(media.hofferbert.astralvigil.firmament.GateKind.class);
        AVBlocks.GATES.forEach((k, b) -> map.put(k, ITEMS.registerSimpleBlockItem(b)));
        return map;
    }
    public static final DeferredItem<Item> ASCENSION_KEY = AVItems.<Item>register(
            "ascension_key", media.hofferbert.astralvigil.item.AscensionKeyItem::new, p -> p.stacksTo(1).rarity(Rarity.EPIC));
    public static final Map<media.hofferbert.astralvigil.firmament.Watcher, DeferredItem<Item>> MARKS = marks();

    private static Map<media.hofferbert.astralvigil.firmament.Watcher, DeferredItem<Item>> marks() {
        Map<media.hofferbert.astralvigil.firmament.Watcher, DeferredItem<Item>> map =
                new EnumMap<>(media.hofferbert.astralvigil.firmament.Watcher.class);
        for (var w : media.hofferbert.astralvigil.firmament.Watcher.values()) {
            map.put(w, register("mark_of_" + w.id(), Item::new, p -> p.stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
        }
        return map;
    }

    // Phase 8 – Endgame
    public static final DeferredItem<Item> UMBRAL_SIGIL = AVItems.<Item>register(
            "umbral_sigil", media.hofferbert.astralvigil.item.UmbralSigilItem::new, p -> p.stacksTo(16).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> ECLIPSE_HEART = register("eclipse_heart", Item::new, p -> p.stacksTo(16).rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<Item> SERAPH_FEATHER = AVItems.<Item>register(
            "seraph_feather", media.hofferbert.astralvigil.item.SeraphFeatherItem::new, p -> p.stacksTo(64).rarity(Rarity.RARE));
    public static final DeferredItem<Item> HALO_OF_DAWN = register("halo_of_dawn", Item::new, p -> p.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<BlockItem> ECLIPSE_SEAL = ITEMS.registerSimpleBlockItem(AVBlocks.ECLIPSE_SEAL);

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
