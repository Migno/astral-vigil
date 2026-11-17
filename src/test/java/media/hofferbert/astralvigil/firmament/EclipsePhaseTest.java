package media.hofferbert.astralvigil.firmament;

import static org.junit.jupiter.api.Assertions.assertEquals;

import media.hofferbert.astralvigil.eclipse.EclipsePhase;
import org.junit.jupiter.api.Test;

class EclipsePhaseTest {
    @Test
    void phasesByHealthFraction() {
        assertEquals(1, EclipsePhase.of(400, 400));
        assertEquals(1, EclipsePhase.of(270, 400));
        assertEquals(2, EclipsePhase.of(266, 400));
        assertEquals(2, EclipsePhase.of(134, 400));
        assertEquals(3, EclipsePhase.of(133, 400));
        assertEquals(3, EclipsePhase.of(0, 400));
    }
}
