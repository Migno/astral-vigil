package media.hofferbert.astralvigil.gear;

import java.util.Optional;
import media.hofferbert.astralvigil.registry.AVItems;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
//? if >=26.1 {
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
//?} else {
/*import net.minecraft.world.entity.projectile.AbstractArrow;
*///?}

/** Gear item classes and the item → {@link GearKind} lookup. */
public final class GearItems {
    private GearItems() {}

    public static Optional<GearKind> kind(ItemStack stack) {
        Item item = stack.getItem();
        if (item == AVItems.HALO_EDGE.get()) {
            return Optional.of(GearKind.SWORD);
        }
        if (item == AVItems.DAWNBREAKER.get()) {
            return Optional.of(GearKind.AXE);
        }
        if (item == AVItems.CRESCENT.get()) {
            return Optional.of(GearKind.BOW);
        }
        if (item == AVItems.TIDECALLER.get()) {
            return Optional.of(GearKind.CROSSBOW);
        }
        if (item == AVItems.STARLIGHT_AEGIS.get()) {
            return Optional.of(GearKind.SHIELD);
        }
        if (item == AVItems.SERAPH_CHESTPLATE.get()) {
            return Optional.of(GearKind.ARMOR);
        }
        return Optional.empty();
    }

    public static boolean isSeraph(ItemStack stack) {
        Item i = stack.getItem();
        return i == AVItems.SERAPH_HELMET.get()
                || i == AVItems.SERAPH_CHESTPLATE.get()
                || i == AVItems.SERAPH_LEGGINGS.get()
                || i == AVItems.SERAPH_BOOTS.get();
    }

    /** Crescent – Moonsilver bow. Night (and awakening) strengthen its arrows. */
    public static class Crescent extends BowItem {
        public Crescent(Properties properties) {
            super(properties);
        }

        @Override
        public AbstractArrow customArrow(AbstractArrow arrow, ItemStack projectileStack, ItemStack weaponStack) {
            return GearManager.empowerArrow(super.customArrow(arrow, projectileStack, weaponStack), weaponStack);
        }
    }

    /** Tidecaller – Moonsilver crossbow. */
    public static class Tidecaller extends CrossbowItem {
        public Tidecaller(Properties properties) {
            super(properties);
        }

        @Override
        public AbstractArrow customArrow(AbstractArrow arrow, ItemStack projectileStack, ItemStack weaponStack) {
            return GearManager.empowerArrow(super.customArrow(arrow, projectileStack, weaponStack), weaponStack);
        }
    }

    /** Starlight Aegis – Starmetal shield (flat item model). */
    public static class Aegis extends ShieldItem {
        public Aegis(Properties properties) {
            super(properties);
        }
    }

    /** A sigil medallion. Socket it by sneak-using gear while holding the sigil in the off hand. */
    public static class SigilItem extends Item {
        private final Sigil sigil;

        public SigilItem(Properties properties, Sigil sigil) {
            super(properties);
            this.sigil = sigil;
        }

        public Sigil sigil() {
            return sigil;
        }
    }
}
