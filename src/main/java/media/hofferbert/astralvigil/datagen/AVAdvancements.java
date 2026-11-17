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
        lore(out, root);
        AdvancementHolder ascend = Advancement.Builder.advancement()
                .parent(heavens)
                .display(AVItems.ASCENSION_KEY.get(), title("ascension"), description("ascension"), null, AdvancementType.GOAL, true, true, false)
                .addCriterion("has_key", InventoryChangeTrigger.TriggerInstance.hasItems(AVItems.ASCENSION_KEY.get()))
                .save(out, "astralvigil:firmament/ascension");
        for (var w : media.hofferbert.astralvigil.firmament.Watcher.values()) {
            Advancement.Builder.advancement()
                    .parent(ascend)
                    .display(AVItems.MARKS.get(w).get(), title("trial_" + w.id()), description("trial_" + w.id()), null,
                            AdvancementType.CHALLENGE, true, true, false)
                    .addCriterion("trial", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                    .save(out, "astralvigil:firmament/trial_" + w.id());
        }
        AdvancementHolder umbral = Advancement.Builder.advancement()
                .parent(ascend)
                .display(AVItems.UMBRAL_SIGIL.get(), title("umbral_sigil"), description("umbral_sigil"), null, AdvancementType.GOAL, true, true, false)
                .addCriterion("has_sigil", InventoryChangeTrigger.TriggerInstance.hasItems(AVItems.UMBRAL_SIGIL.get()))
                .save(out, "astralvigil:endgame/umbral_sigil");
        AdvancementHolder defeated = Advancement.Builder.advancement()
                .parent(umbral)
                .display(AVItems.ECLIPSE_HEART.get(), title("eclipse_defeated"), description("eclipse_defeated"), null,
                        AdvancementType.CHALLENGE, true, true, false)
                .addCriterion("defeated", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                .save(out, "astralvigil:endgame/eclipse_defeated");
        Advancement.Builder.advancement()
                .parent(defeated)
                .display(AVItems.HALO_OF_DAWN.get(), title("halo_of_dawn"), description("halo_of_dawn"), null,
                        AdvancementType.CHALLENGE, true, true, false)
                .addCriterion("has_halo", InventoryChangeTrigger.TriggerInstance.hasItems(AVItems.HALO_OF_DAWN.get()))
                .save(out, "astralvigil:endgame/halo_of_dawn");
        Advancement.Builder.advancement()
                .parent(defeated)
                .display(AVItems.SERAPH_FEATHER.get(), title("seraph_wings"), description("seraph_wings"), null,
                        AdvancementType.TASK, true, true, false)
                .addCriterion("has_feather", InventoryChangeTrigger.TriggerInstance.hasItems(AVItems.SERAPH_FEATHER.get()))
                .save(out, "astralvigil:endgame/seraph_wings");
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

    static void lore(Consumer<AdvancementHolder> out, AdvancementHolder parent) {
        for (int i = 1; i <= 24; i++) {
            Advancement.Builder.advancement()
                    .parent(parent)
                    .addCriterion("read", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                    .save(out, "astralvigil:lore/fragment_" + i);
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
