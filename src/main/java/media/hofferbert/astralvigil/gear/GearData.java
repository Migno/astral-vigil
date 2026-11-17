package media.hofferbert.astralvigil.gear;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Living-gear state stored on the item stack ({@code astralvigil:gear} data component).
 *
 * @param deeds deeds performed (see {@link GearKind})
 * @param sigils socketed sigil ids
 * @param stance index of the active sigil
 * @param charge starlight charge used by abilities
 * @param cooldownUntil game time until which the ability is cooling down
 */
public record GearData(int deeds, List<String> sigils, int stance, int charge, long cooldownUntil) {
    public static final GearData EMPTY = new GearData(0, List.of(), 0, 0, 0L);

    public static final Codec<GearData> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Codec.INT.optionalFieldOf("deeds", 0).forGetter(GearData::deeds),
                    Codec.STRING.listOf().optionalFieldOf("sigils", List.of()).forGetter(GearData::sigils),
                    Codec.INT.optionalFieldOf("stance", 0).forGetter(GearData::stance),
                    Codec.INT.optionalFieldOf("charge", 0).forGetter(GearData::charge),
                    Codec.LONG.optionalFieldOf("cooldown_until", 0L).forGetter(GearData::cooldownUntil))
            .apply(i, GearData::new));

    public static final StreamCodec<ByteBuf, GearData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GearData::deeds,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), GearData::sigils,
            ByteBufCodecs.VAR_INT, GearData::stance,
            ByteBufCodecs.VAR_INT, GearData::charge,
            ByteBufCodecs.VAR_LONG, GearData::cooldownUntil,
            GearData::new);

    public GearData {
        sigils = List.copyOf(sigils);
    }

    public int level() {
        return GearProgress.level(deeds);
    }

    public Optional<Sigil> activeSigil() {
        if (sigils.isEmpty()) {
            return Optional.empty();
        }
        return Sigil.byId(sigils.get(Math.floorMod(stance, sigils.size())));
    }

    public GearData withDeeds(int value) {
        return new GearData(value, sigils, stance, charge, cooldownUntil);
    }

    public GearData withSigils(List<String> value) {
        return new GearData(deeds, new ArrayList<>(value), stance, charge, cooldownUntil);
    }

    public GearData withStance(int value) {
        return new GearData(deeds, sigils, value, charge, cooldownUntil);
    }

    public GearData withCharge(int value) {
        return new GearData(deeds, sigils, stance, Math.max(0, value), cooldownUntil);
    }

    public GearData withCooldownUntil(long value) {
        return new GearData(deeds, sigils, stance, charge, value);
    }
}
