package media.hofferbert.astralvigil.firmament;

import java.util.Locale;

/**
 * The six gates into the Firmament. Each has its own frame block ({@code <id>_gate}) and portal block
 * ({@code <id>_portal}, the Astral one keeps the id {@code celestial_portal}) and leads to its own place: the Astral
 * Gate to the hub, each Watcher's gate straight to that Watcher's trial island, the Umbral Gate to the Eclipse's throne.
 * The islands are not connected - their gate is the only way in.
 */
public enum GateKind {
    ASTRAL(null, 0x8FEFFF),
    SOLAR(Watcher.SOLENNE, 0xF2C94C),
    LUNAR(Watcher.VAEL, 0xD8D8E6),
    PRISM(Watcher.ORRIN, 0x2FA8D0),
    ARCHIVE(Watcher.ILYRA, 0xB9F6FF),
    UMBRAL(null, 0xB57CFF);

    private final Watcher watcher;
    private final int colour;

    GateKind(Watcher watcher, int colour) {
        this.watcher = watcher;
        this.colour = colour;
    }

    /** The Watcher whose trial island this gate leads to, or null (hub / throne). */
    public Watcher watcher() {
        return watcher;
    }

    public int colour() {
        return colour;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String frameId() {
        return id() + "_gate";
    }

    public String portalId() {
        return this == ASTRAL ? "celestial_portal" : id() + "_portal";
    }
}
