package media.hofferbert.astralvigil.client.armor;

import java.util.List;
import media.hofferbert.astralvigil.armor.ArmorSet;
import media.hofferbert.astralvigil.armor.ArmorSets;
import media.hofferbert.astralvigil.armor.SetBonusEvents;
import media.hofferbert.astralvigil.armor.SetBonuses;
import media.hofferbert.astralvigil.armor.SetBonuses.Bonus;
import media.hofferbert.astralvigil.armor.SetBonuses.Tier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** "Set Bonus" section under every celestial armour piece: worn count and each bonus, lit when it applies. */
public final class ArmorTooltip {
    private ArmorTooltip() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ArmorTooltip::onTooltip);
    }

    private static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        ArmorSet set = ArmorSets.of(stack).orElse(null);
        if (set == null) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        int worn = player == null ? 0 : ArmorSets.worn(player, set);
        int level = player == null ? 0 : ArmorSets.level(player.getItemBySlot(EquipmentSlot.CHEST));
        boolean full = worn == 4;
        SetBonuses.Settings s = SetBonusEvents.settings();
        List<Component> tip = event.getToolTip();
        tip.add(Component.translatable("armor.astralvigil.set_bonus", Component.translatable(set.translationKey()), worn)
                .withStyle(full ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        for (Tier t : SetBonuses.tiers(set)) {
            boolean lit = full && level >= t.level();
            MutableComponent line = Component.literal(lit ? " ✦ " : " ✧ ").append(text(t.bonus(), s));
            if (t.level() > 0) {
                line.append(" ").append(Component.translatable("armor.astralvigil.requires",
                        Component.translatable("gear.astralvigil.level." + t.level())));
            }
            tip.add(line.withStyle(lit ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        }
    }

    private static Component text(Bonus bonus, SetBonuses.Settings s) {
        return switch (bonus) {
            case SOLAR_MIGHT -> Component.translatable(bonus.translationKey(), roman(s.solarStrength()));
            case MOONSTRIDE -> Component.translatable(bonus.translationKey(), roman(s.moonstride()));
            case MOONFALL -> Component.translatable(bonus.translationKey(), Math.round(s.moonfallReduction() * 100));
            case GLIDE_BURST -> Component.translatable(bonus.translationKey(), s.burstCost());
            default -> Component.translatable(bonus.translationKey());
        };
    }

    private static Component roman(int level) {
        return Component.translatable("enchantment.level." + Math.max(1, level));
    }
}
