package media.hofferbert.astralvigil.gear;

import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.registry.AVTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
//? if >=26.1 {
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
//?} else {
/*import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?}

/**
 * Gear materials. Top tier ≈ Netherite (see docs/BALANCE.md): Sunsteel weapons match Netherite damage with less
 * durability; Seraph Plate matches Netherite defence with slightly better enchantability. The mid-tier Solar Regalia
 * (Sunsteel) and Lunar Raiment (Moonsilver) stay below diamond in raw defence and earn their keep with set bonuses.
 */
public final class AVMaterials {
    public static final int SERAPH_DURABILITY_FACTOR = 35; // netherite: 37
    public static final int SOLAR_DURABILITY_FACTOR = 30; // diamond: 33
    public static final int LUNAR_DURABILITY_FACTOR = 28;

    private AVMaterials() {}

    //? if >=26.1 {
    public static final ToolMaterial SUNSTEEL =
            new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 1800, 9.0f, 4.0f, 18, AVTags.Items.INGOTS_SUNSTEEL);
    public static final ResourceKey<EquipmentAsset> SERAPH_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID, AstralVigil.id("seraph"));
    public static final ArmorMaterial SERAPH = new ArmorMaterial(
            SERAPH_DURABILITY_FACTOR,
            Map.of(ArmorType.BOOTS, 3, ArmorType.LEGGINGS, 6, ArmorType.CHESTPLATE, 8, ArmorType.HELMET, 3, ArmorType.BODY, 11),
            18,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            3.0f,
            0.1f,
            AVTags.Items.INGOTS_STARMETAL,
            SERAPH_ASSET);
    public static final ResourceKey<EquipmentAsset> SOLAR_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID, AstralVigil.id("solar"));
    public static final ArmorMaterial SOLAR = new ArmorMaterial(
            SOLAR_DURABILITY_FACTOR,
            Map.of(ArmorType.BOOTS, 2, ArmorType.LEGGINGS, 6, ArmorType.CHESTPLATE, 7, ArmorType.HELMET, 3, ArmorType.BODY, 9),
            22,
            SoundEvents.ARMOR_EQUIP_GOLD,
            2.0f,
            0.0f,
            AVTags.Items.INGOTS_SUNSTEEL,
            SOLAR_ASSET);
    public static final ResourceKey<EquipmentAsset> LUNAR_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID, AstralVigil.id("lunar"));
    public static final ArmorMaterial LUNAR = new ArmorMaterial(
            LUNAR_DURABILITY_FACTOR,
            Map.of(ArmorType.BOOTS, 3, ArmorType.LEGGINGS, 5, ArmorType.CHESTPLATE, 6, ArmorType.HELMET, 3, ArmorType.BODY, 8),
            25,
            SoundEvents.ARMOR_EQUIP_CHAIN,
            1.5f,
            0.0f,
            AVTags.Items.INGOTS_MOONSILVER,
            LUNAR_ASSET);
    //?} else {
    /*public static final Tier SUNSTEEL = new SimpleTier(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 1800, 9.0f, 4.0f, 18, () -> Ingredient.of(AVTags.Items.INGOTS_SUNSTEEL));

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, AstralVigil.MOD_ID);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SERAPH = ARMOR_MATERIALS.register("seraph", () -> {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, 3);
        defense.put(ArmorItem.Type.LEGGINGS, 6);
        defense.put(ArmorItem.Type.CHESTPLATE, 8);
        defense.put(ArmorItem.Type.HELMET, 3);
        defense.put(ArmorItem.Type.BODY, 11);
        return new ArmorMaterial(
                defense,
                18,
                SoundEvents.ARMOR_EQUIP_NETHERITE,
                () -> Ingredient.of(AVTags.Items.INGOTS_STARMETAL),
                List.of(new ArmorMaterial.Layer(AstralVigil.id("seraph"))),
                3.0f,
                0.1f);
    });
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SOLAR = ARMOR_MATERIALS.register("solar",
            () -> material("solar", 2, 6, 7, 3, 9, 22, SoundEvents.ARMOR_EQUIP_GOLD, AVTags.Items.INGOTS_SUNSTEEL, 2.0f));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> LUNAR = ARMOR_MATERIALS.register("lunar",
            () -> material("lunar", 3, 5, 6, 3, 8, 25, SoundEvents.ARMOR_EQUIP_CHAIN, AVTags.Items.INGOTS_MOONSILVER, 1.5f));

    private static ArmorMaterial material(String name, int boots, int legs, int chest, int helmet, int body, int enchant,
            Holder<net.minecraft.sounds.SoundEvent> sound, net.minecraft.tags.TagKey<net.minecraft.world.item.Item> repair,
            float toughness) {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, boots);
        defense.put(ArmorItem.Type.LEGGINGS, legs);
        defense.put(ArmorItem.Type.CHESTPLATE, chest);
        defense.put(ArmorItem.Type.HELMET, helmet);
        defense.put(ArmorItem.Type.BODY, body);
        return new ArmorMaterial(defense, enchant, sound, () -> Ingredient.of(repair),
                List.of(new ArmorMaterial.Layer(AstralVigil.id(name))), toughness, 0.0f);
    }
    *///?}
}
