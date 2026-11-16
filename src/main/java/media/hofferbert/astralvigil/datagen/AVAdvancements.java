package media.hofferbert.astralvigil.datagen;

import java.util.function.Consumer;
import media.hofferbert.astralvigil.registry.AVItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import media.hofferbert.astralvigil.research.Alignment;
import media.hofferbert.astralvigil.research.Constellation;
import media.hofferbert.astralvigil.research.Constellations;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
//? if >=26.1 {
import net.minecraft.advancements.criterion.ImpossibleTrigger;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
//?} else {
/*import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
*///?}

/** Advancement tree "Astral Vigil". */
public class AVAdvancements implements AdvancementSubProvider {
    //? if >=26.1 {
    static final Identifier BACKGROUND = Identifier.withDefaultNamespace("gui/advancements/backgrounds/end");
    //?} else {
    /*static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/gui/advancements/backgrounds/end.png");
    *///?}

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> out) {
        AdvancementHolder root = Advancement.Builder.advancement()
                .display(
                        AVItems.ASTRAL_SHARD.get(),
                        title("root"),
                        description("root"),
                        BACKGROUND,
                        AdvancementType.TASK,
                        false,
                        false,
                        false)
                .addCriterion("has_meteoric_stone", InventoryChangeTrigger.TriggerInstance.hasItems(AVItems.METEORIC_STONE.get()))
                .addCriterion("has_astral_shard", InventoryChangeTrigger.TriggerInstance.hasItems(AVItems.ASTRAL_SHARD.get()))
                .requirements(AdvancementRequirements.Strategy.OR)
                .save(out, "astralvigil:root");

        AdvancementHolder starstruck = Advancement.Builder.advancement()
                .parent(root)
                .display(
                        AVItems.ASTRAL_SHARD.get(),
                        title("starstruck"),
                        description("starstruck"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false)
                .addCriterion("has_astral_shard", InventoryChangeTrigger.TriggerInstance.hasItems(AVItems.ASTRAL_SHARD.get()))
                .save(out, "astralvigil:starstruck");

        AdvancementHolder heavens = Advancement.Builder.advancement()
                .parent(starstruck)
                .display(
                        AVItems.ASTROLABE.get(),
                        title("read_the_heavens"),
                        description("read_the_heavens"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false)
                .addCriterion("has_astrolabe", InventoryChangeTrigger.TriggerInstance.hasItems(AVItems.ASTROLABE.get()))
                .save(out, "astralvigil:read_the_heavens");

        // Research advancements: granted from code (impossible criterion "research").
        AdvancementHolder firstScan = research(out, heavens, "first_scan", AVItems.ASTROLABE.get(), AdvancementType.TASK);
        ItemLike[] icons = {Items.SUNFLOWER, Items.SNOW_BLOCK, Items.AMETHYST_CLUSTER, Items.CRYING_OBSIDIAN};
        for (Alignment a : Alignment.values()) {
            research(out, firstScan, "attuned_" + a.id(), icons[a.ordinal()], AdvancementType.GOAL);
        }
        ItemLike[] cIcons = {Items.SUNFLOWER, Items.SNOW_BLOCK, Items.ANVIL, Items.FEATHER};
        int i = 0;
        for (Constellation c : Constellations.all()) {
            research(out, firstScan, "constellation_" + c.id(), cIcons[i++ % cIcons.length], AdvancementType.CHALLENGE);
        }
    }

    private static AdvancementHolder research(
            Consumer<AdvancementHolder> out, AdvancementHolder parent, String id, ItemLike icon, AdvancementType type) {
        return Advancement.Builder.advancement()
                .parent(parent)
                .display(icon, title(id), description(id), null, type, true, type != AdvancementType.TASK, false)
                .addCriterion("research", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                .save(out, "astralvigil:research/" + id);
    }

    private static Component title(String id) {
        return Component.translatable("advancements.astralvigil." + id + ".title");
    }

    private static Component description(String id) {
        return Component.translatable("advancements.astralvigil." + id + ".description");
    }
}
