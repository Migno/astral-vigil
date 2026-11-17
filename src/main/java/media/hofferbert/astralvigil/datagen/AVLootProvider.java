package media.hofferbert.astralvigil.datagen;

import java.util.Set;
import media.hofferbert.astralvigil.registry.AVBlocks;
import media.hofferbert.astralvigil.registry.AVItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/** Block loot tables. */
public class AVLootProvider extends BlockLootSubProvider {
    public AVLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        HolderLookup.RegistryLookup<Enchantment> enchantments = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        // Fallen Star: 2-4 Astral Shards (+Fortune), silk touch keeps the block.
        add(
                AVBlocks.FALLEN_STAR.get(),
                block -> createSilkTouchDispatchTable(
                        block,
                        applyExplosionDecay(
                                block,
                                LootItem.lootTableItem(AVItems.ASTRAL_SHARD.get())
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0f, 4.0f)))
                                        .apply(ApplyBonusCount.addOreBonusCount(
                                                enchantments.getOrThrow(Enchantments.FORTUNE))))));
        dropSelf(AVBlocks.METEORIC_STONE.get());
        dropSelf(AVBlocks.STARLIGHT_COLLECTOR.get());
        dropSelf(AVBlocks.LENS.get());
        dropSelf(AVBlocks.MIRROR.get());
        dropSelf(AVBlocks.PRISM.get());
        dropSelf(AVBlocks.STARLIGHT_RESERVOIR.get());
        dropSelf(AVBlocks.PEDESTAL.get());
        dropSelf(AVBlocks.CONVERGENCE_ALTAR.get());
        dropSelf(AVBlocks.ASTRAL_FORGE.get());
        dropSelf(AVBlocks.STAR_ANCHOR.get());
        add(AVBlocks.VOID_CRYSTAL.get(), block -> createSilkTouchDispatchTable(
                block,
                applyExplosionDecay(block, LootItem.lootTableItem(AVItems.VOID_SHARD.get())
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 2.0f))))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return AVBlocks.BLOCKS.getEntries().stream()
                .map(h -> (Block) h.value())
                .filter(b -> b != AVBlocks.CONSTELLATION_LOCK.get())
                .toList();
    }
}
