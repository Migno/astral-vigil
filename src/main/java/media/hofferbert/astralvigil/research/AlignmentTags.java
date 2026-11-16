package media.hofferbert.astralvigil.research;

import java.util.EnumMap;
import java.util.Map;
import media.hofferbert.astralvigil.AstralVigil;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * {@code #astralvigil:alignment/<sol|luna|stella|void>} tags for blocks, entity types, biomes and structures. Packmakers
 * add or remove members with plain datapacks/KubeJS.
 */
public final class AlignmentTags {
    private static final Map<Alignment, TagKey<Block>> BLOCKS = new EnumMap<>(Alignment.class);
    private static final Map<Alignment, TagKey<EntityType<?>>> ENTITIES = new EnumMap<>(Alignment.class);
    private static final Map<Alignment, TagKey<Biome>> BIOMES = new EnumMap<>(Alignment.class);
    private static final Map<Alignment, TagKey<Structure>> STRUCTURES = new EnumMap<>(Alignment.class);

    static {
        for (Alignment a : Alignment.values()) {
            String path = "alignment/" + a.id();
            BLOCKS.put(a, TagKey.create(Registries.BLOCK, AstralVigil.id(path)));
            ENTITIES.put(a, TagKey.create(Registries.ENTITY_TYPE, AstralVigil.id(path)));
            BIOMES.put(a, TagKey.create(Registries.BIOME, AstralVigil.id(path)));
            STRUCTURES.put(a, TagKey.create(Registries.STRUCTURE, AstralVigil.id(path)));
        }
    }

    private AlignmentTags() {}

    public static TagKey<Block> blocks(Alignment a) {
        return BLOCKS.get(a);
    }

    public static TagKey<EntityType<?>> entities(Alignment a) {
        return ENTITIES.get(a);
    }

    public static TagKey<Biome> biomes(Alignment a) {
        return BIOMES.get(a);
    }

    public static TagKey<Structure> structures(Alignment a) {
        return STRUCTURES.get(a);
    }
}
