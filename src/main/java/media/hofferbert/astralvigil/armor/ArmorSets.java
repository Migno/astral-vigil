package media.hofferbert.astralvigil.armor;

import java.util.List;
import java.util.Optional;
import media.hofferbert.astralvigil.gear.GearManager;
import media.hofferbert.astralvigil.registry.AVItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;

/** Item ↔ {@link ArmorSet} lookups. Shared by the server bonuses and the client ornaments. */
public final class ArmorSets {
    /** Armour slots in head → feet order. */
    public static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private ArmorSets() {}

    /** The four pieces of a set, head → feet. */
    public static List<DeferredItem<Item>> pieces(ArmorSet set) {
        return switch (set) {
            case SERAPH -> List.of(AVItems.SERAPH_HELMET, AVItems.SERAPH_CHESTPLATE, AVItems.SERAPH_LEGGINGS, AVItems.SERAPH_BOOTS);
            case SOLAR -> List.of(AVItems.SOLAR_HELMET, AVItems.SOLAR_CHESTPLATE, AVItems.SOLAR_LEGGINGS, AVItems.SOLAR_BOOTS);
            case LUNAR -> List.of(AVItems.LUNAR_HELMET, AVItems.LUNAR_CHESTPLATE, AVItems.LUNAR_LEGGINGS, AVItems.LUNAR_BOOTS);
        };
    }

    public static Item piece(ArmorSet set, EquipmentSlot slot) {
        return pieces(set).get(SLOTS.indexOf(slot)).get();
    }

    /** The set this stack belongs to (any slot). */
    public static Optional<ArmorSet> of(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        Item item = stack.getItem();
        for (ArmorSet set : ArmorSet.values()) {
            for (DeferredItem<Item> piece : pieces(set)) {
                if (piece.get() == item) {
                    return Optional.of(set);
                }
            }
        }
        return Optional.empty();
    }

    /** The set of the piece worn in {@code slot}, only if it is that set's piece for this slot. */
    public static ArmorSet inSlot(ItemStack stack, EquipmentSlot slot) {
        ArmorSet set = of(stack).orElse(null);
        return set != null && piece(set, slot) == stack.getItem() ? set : null;
    }

    public static Optional<ArmorSet> fullSet(LivingEntity entity) {
        return SetBonuses.fullSet(
                inSlot(entity.getItemBySlot(EquipmentSlot.HEAD), EquipmentSlot.HEAD),
                inSlot(entity.getItemBySlot(EquipmentSlot.CHEST), EquipmentSlot.CHEST),
                inSlot(entity.getItemBySlot(EquipmentSlot.LEGS), EquipmentSlot.LEGS),
                inSlot(entity.getItemBySlot(EquipmentSlot.FEET), EquipmentSlot.FEET));
    }

    public static int worn(LivingEntity entity, ArmorSet set) {
        int n = 0;
        for (EquipmentSlot slot : SLOTS) {
            if (inSlot(entity.getItemBySlot(slot), slot) == set) {
                n++;
            }
        }
        return n;
    }

    /** Awakening level of a Seraph chestplate stack (0 for anything else). */
    public static int level(ItemStack chest) {
        return chest.is(AVItems.SERAPH_CHESTPLATE.get()) ? GearManager.data(chest).level() : 0;
    }
}
