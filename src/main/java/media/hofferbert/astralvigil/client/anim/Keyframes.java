package media.hofferbert.astralvigil.client.anim;

import net.minecraft.util.Mth;

/**
 * Minimal keyframe player for vanilla model parts, matching GeckoLib's semantics: the easing stored on a keyframe
 * shapes the segment that ends at it. Easing ids follow {@code tools/dev/geckorig.py} EASINGS: 0 linear, then
 * Sine, Quad, Cubic, Quart, Back - each as In, Out, InOut.
 */
public final class Keyframes {
    private Keyframes() {}

    public static float ease(int id, float t) {
        if (id <= 0) {
            return t;
        }
        int family = (id - 1) / 3;
        int kind = (id - 1) % 3;
        return switch (kind) {
            case 0 -> in(family, t);
            case 1 -> 1f - in(family, 1f - t);
            default -> t < 0.5f ? in(family, 2f * t) / 2f : 1f - in(family, 2f * (1f - t)) / 2f;
        };
    }

    private static float in(int family, float t) {
        return switch (family) {
            case 0 -> 1f - Mth.cos(t * Mth.HALF_PI);
            case 1 -> t * t;
            case 2 -> t * t * t;
            case 3 -> t * t * t * t;
            default -> t * t * (2.70158f * t - 1.70158f);
        };
    }

    /** Rotation keys of one bone: times (s), xyz values (degrees) per key, easing id per key. */
    public record Track(float[] times, float[] values, byte[] easing) {
        public void sample(float t, float[] out) {
            int n = times.length;
            if (t <= times[0]) {
                copy(0, out);
                return;
            }
            for (int i = 1; i < n; i++) {
                if (t <= times[i]) {
                    float span = times[i] - times[i - 1];
                    float k = ease(easing[i], span <= 0f ? 1f : (t - times[i - 1]) / span);
                    for (int c = 0; c < 3; c++) {
                        float a = values[(i - 1) * 3 + c];
                        out[c] = a + (values[i * 3 + c] - a) * k;
                    }
                    return;
                }
            }
            copy(n - 1, out);
        }

        private void copy(int i, float[] out) {
            out[0] = values[i * 3];
            out[1] = values[i * 3 + 1];
            out[2] = values[i * 3 + 2];
        }
    }

    /** A clip: one (nullable) rotation track per bone. Looping clips wrap the time. */
    public record Clip(float length, boolean loop, Track[] tracks) {
        public void sample(int bone, float time, float[] out) {
            Track track = tracks[bone];
            if (track == null) {
                out[0] = out[1] = out[2] = 0f;
                return;
            }
            float t = loop ? time - length * Mth.floor(time / length) : Mth.clamp(time, 0f, length);
            track.sample(t, out);
        }
    }
}
