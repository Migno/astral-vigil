package media.hofferbert.astralvigil.cosmetics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import media.hofferbert.astralvigil.cosmetics.Cosmetic.Slot;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** What a player wears: one cosmetic id per slot, empty string = nothing. Unknown ids are dropped by {@link #sanitized()}. */
public record Loadout(String head, String back, String wings, String aura) {
    public static final Loadout EMPTY = new Loadout("", "", "", "");

    public static final MapCodec<Loadout> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    Codec.STRING.optionalFieldOf("head", "").forGetter(Loadout::head),
                    Codec.STRING.optionalFieldOf("back", "").forGetter(Loadout::back),
                    Codec.STRING.optionalFieldOf("wings", "").forGetter(Loadout::wings),
                    Codec.STRING.optionalFieldOf("aura", "").forGetter(Loadout::aura))
            .apply(i, Loadout::new));

    public static final StreamCodec<ByteBuf, Loadout> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(32), Loadout::head,
            ByteBufCodecs.stringUtf8(32), Loadout::back,
            ByteBufCodecs.stringUtf8(32), Loadout::wings,
            ByteBufCodecs.stringUtf8(32), Loadout::aura,
            Loadout::new);

    public String raw(Slot slot) {
        return switch (slot) {
            case HEAD -> head;
            case BACK -> back;
            case WINGS -> wings;
            case AURA -> aura;
        };
    }

    public Optional<Cosmetic> get(Slot slot) {
        return Cosmetic.byId(slot, raw(slot));
    }

    public boolean wears(Cosmetic cosmetic) {
        return get(cosmetic.slot()).filter(c -> c == cosmetic).isPresent();
    }

    public Loadout with(Slot slot, Optional<Cosmetic> cosmetic) {
        String id = cosmetic.filter(c -> c.slot() == slot).map(Cosmetic::id).orElse("");
        return new Loadout(
                slot == Slot.HEAD ? id : head,
                slot == Slot.BACK ? id : back,
                slot == Slot.WINGS ? id : wings,
                slot == Slot.AURA ? id : aura);
    }

    /** Copy with every unknown or wrongly slotted id cleared. */
    public Loadout sanitized() {
        Loadout out = EMPTY;
        for (Slot slot : Slot.values()) {
            out = out.with(slot, get(slot));
        }
        return out;
    }

    public boolean isEmpty() {
        return sanitized().equals(EMPTY);
    }
}
