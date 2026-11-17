package media.hofferbert.astralvigil.datagen;

import java.util.function.BiConsumer;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.registry.AVComponents;
import media.hofferbert.astralvigil.registry.AVItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/** Chest loot of the Fallen Observatory (open chest + sealed vault). Lore fragments are spread over both. */
public class AVChestLoot implements LootTableSubProvider {
    public static final ResourceKey<LootTable> OBSERVATORY =
            ResourceKey.create(Registries.LOOT_TABLE, AstralVigil.id("chests/fallen_observatory"));
    public static final ResourceKey<LootTable> VAULT =
            ResourceKey.create(Registries.LOOT_TABLE, AstralVigil.id("chests/observatory_vault"));

    public AVChestLoot(HolderLookup.Provider registries) {}

    private static LootPool.Builder fragments(int rollsMin, int rollsMax) {
        LootPool.Builder pool = LootPool.lootPool().setRolls(UniformGenerator.between(rollsMin, rollsMax));
        for (int i = 1; i <= 24; i++) {
            pool.add(LootItem.lootTableItem(AVItems.LORE_FRAGMENT.get())
                    .apply(SetComponentsFunction.setComponent(AVComponents.FRAGMENT.get(), i)));
        }
        return pool;
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> out) {
        out.accept(OBSERVATORY, LootTable.lootTable()
                .withPool(fragments(1, 2))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2, 5))
                        .add(LootItem.lootTableItem(AVItems.ASTRAL_SHARD.get()).setWeight(4)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(4)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 8))))
                        .add(LootItem.lootTableItem(Items.PAPER).setWeight(3)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                        .add(LootItem.lootTableItem(Items.SPYGLASS).setWeight(1))
                        .add(LootItem.lootTableItem(Items.AMETHYST_SHARD).setWeight(3)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))));
        out.accept(VAULT, LootTable.lootTable()
                .withPool(fragments(2, 3))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2, 4))
                        .add(LootItem.lootTableItem(AVItems.STARMETAL_INGOT.get()).setWeight(3)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                        .add(LootItem.lootTableItem(AVItems.SUNSTEEL_INGOT.get()).setWeight(3)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                        .add(LootItem.lootTableItem(AVItems.MOONSILVER_INGOT.get()).setWeight(3)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                        .add(LootItem.lootTableItem(AVItems.SIGIL_BLANK.get()).setWeight(2))
                        .add(LootItem.lootTableItem(AVItems.ASTRAL_SHARD.get()).setWeight(4)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 5)))))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(AVItems.ASTRAL_CORE.get()).setWeight(1))
                        .add(net.minecraft.world.level.storage.loot.entries.EmptyLootItem.emptyItem().setWeight(9))));
    }
}
