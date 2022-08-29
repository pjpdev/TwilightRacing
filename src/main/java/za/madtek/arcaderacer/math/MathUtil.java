package za.madtek.arcaderacer.math;

public class MathUtil {

    public static final int UINT32_MAX = 0xFFFFFFFF;
    public static final long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;

    public static int clamp(int min, int max, int value) {
        return Math.max(min, Math.min(max, value));
    }
}
