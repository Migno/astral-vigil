package media.hofferbert.astralvigil.firmament;

import java.util.Locale;
import media.hofferbert.astralvigil.AstralVigil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * The realms of the Firmament - one dimension per gate kind, no hub between them. Each realm is a void sky with its
 * own fixed atmosphere and one handcrafted template ({@code structure/realm/<id>.nbt}, from tools/dev/gen_realms.py)
 * holding the arrival shrine with the return gate, the trial and the scenery. Keep the numbers in sync with
 * gen_realms.py.
 */
public enum Realm {
    /** The Firmament wilds: floating starwood groves around a shrine; no trial. */
    FIRMAMENT(GateKind.ASTRAL, "firmament", 150, 0, 0),
    SUNSPIRE(GateKind.SOLAR, "sunspire", 80, 31, 14),
    MOONWELL(GateKind.LUNAR, "moonwell", 100, 43, 26),
    STARFORGE(GateKind.PRISM, "starforge", 100, 50, 32),
    ARCHIVE(GateKind.ARCHIVE, "archive", 100, 63, 44),
    UMBRAL_THRONE(GateKind.UMBRAL, "umbral_throne", 100, 51, 38);

    /** Template anchor: the pad centre sits at (ANCHOR, FLOOR_Y, ANCHOR) inside every realm template. */
    public static final int ANCHOR = 128;
    public static final int FLOOR_Y = 48;

    private final GateKind gate;
    private final String id;
    private final int padY;
    private final int trialOffset;
    private final int trialRadius;
    private final ResourceKey<Level> level;

    Realm(GateKind gate, String id, int padY, int trialOffset, int trialRadius) {
        this.gate = gate;
        this.id = id;
        this.padY = padY;
        this.trialOffset = trialOffset;
        this.trialRadius = trialRadius;
        this.level = ResourceKey.create(Registries.DIMENSION, AstralVigil.id(id));
    }

    public String id() {
        return id;
    }

    public GateKind gate() {
        return gate;
    }

    public ResourceKey<Level> level() {
        return level;
    }

    /** The Watcher whose trial lies in this realm (null for the wilds and the throne). */
    public Watcher watcher() {
        return gate.watcher();
    }

    /** Centre floor block of the arrival shrine (the Starlight Font, which marks the realm as built). */
    public BlockPos pad() {
        return new BlockPos(0, padY, 0);
    }

    /** Where travellers arrive: on the shrine, in front of the return gate, facing the trial (north). */
    public BlockPos arrival() {
        return pad().offset(0, 1, 2);
    }

    /** Centre floor block of the trial island (north of the pad). */
    public BlockPos trialCentre() {
        return pad().north(trialOffset);
    }

    public int trialRadius() {
        return trialRadius;
    }

    public boolean hasTrial() {
        return trialOffset > 0;
    }

    public String translationKey() {
        return "realm.astralvigil." + id;
    }

    public static Realm of(GateKind kind) {
        for (Realm r : values()) {
            if (r.gate == kind) {
                return r;
            }
        }
        throw new IllegalArgumentException(String.valueOf(kind));
    }

    public static Realm of(Watcher watcher) {
        return of(RealmBuilder.gateOf(watcher));
    }

    /** The realm a level belongs to, or null outside the Firmament. */
    public static Realm of(Level level) {
        return of(level.dimension());
    }

    public static Realm of(ResourceKey<Level> dimension) {
        for (Realm r : values()) {
            if (r.level == dimension) {
                return r;
            }
        }
        return null;
    }

    public static Realm byId(String id) {
        return valueOf(id.toUpperCase(Locale.ROOT));
    }
}
