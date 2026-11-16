package media.hofferbert.astralvigil.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Per-player research state (immutable; replace via {@code player.setData}).
 *
 * @param knowledge alignment id -> knowledge points
 * @param scanned subject keys already scanned ("block/minecraft:glowstone", "biome/minecraft:desert", ...)
 * @param constellations ids of solved constellations
 */
public record ResearchData(Map<String, Integer> knowledge, Set<String> scanned, Set<String> constellations) {
    public static final ResearchData EMPTY = new ResearchData(Map.of(), Set.of(), Set.of());

    public static final MapCodec<ResearchData> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    Codec.unboundedMap(Codec.STRING, Codec.INT)
                            .optionalFieldOf("knowledge", Map.of())
                            .forGetter(ResearchData::knowledge),
                    Codec.STRING.listOf()
                            .xmap(Set::copyOf, List::copyOf)
                            .optionalFieldOf("scanned", Set.of())
                            .forGetter(ResearchData::scanned),
                    Codec.STRING.listOf()
                            .xmap(Set::copyOf, List::copyOf)
                            .optionalFieldOf("constellations", Set.of())
                            .forGetter(ResearchData::constellations))
            .apply(i, ResearchData::new));

    /** Clients only need the knowledge and the solved constellations; scan count is enough. */
    public static final StreamCodec<ByteBuf, ResearchData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT),
            r -> new HashMap<>(r.knowledge()),
            ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.STRING_UTF8),
            r -> new HashSet<>(r.scanned()),
            ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.STRING_UTF8),
            r -> new HashSet<>(r.constellations()),
            (k, s, c) -> new ResearchData(Map.copyOf(k), Set.copyOf(s), Set.copyOf(c)));

    public ResearchData {
        knowledge = Map.copyOf(knowledge);
        scanned = Set.copyOf(scanned);
        constellations = Set.copyOf(constellations);
    }

    public int knowledge(Alignment alignment) {
        return knowledge.getOrDefault(alignment.id(), 0);
    }

    public boolean hasScanned(String subject) {
        return scanned.contains(subject);
    }

    public boolean hasConstellation(String id) {
        return constellations.contains(id);
    }

    public ResearchData withScan(String subject, Iterable<Alignment> alignments, int points) {
        Set<String> s = new HashSet<>(scanned);
        s.add(subject);
        Map<String, Integer> k = new HashMap<>(knowledge);
        for (Alignment a : alignments) {
            k.merge(a.id(), points, Integer::sum);
        }
        return new ResearchData(k, s, constellations);
    }

    public ResearchData withConstellation(String id) {
        Set<String> c = new HashSet<>(constellations);
        c.add(id);
        return new ResearchData(knowledge, scanned, c);
    }
}
