package tk.darrow.tribalpower.world;

/** Pure March sky colours — unique day/night, complementary to {@link AuroraMath}. */
public final class MarchSkyMath {
    private MarchSkyMath() {}

    /** 1 at noon, 0 at midnight. Inverse of the aurora night window. */
    public static float dayness(long dayTime, float partial) {
        double time = (Math.floorMod(dayTime, 24000) + Math.clamp(partial, 0F, 1F)) / 24000.0;
        return (float) Math.clamp((Math.sin(time * Math.PI * 2) + 0.2) / 1.15, 0, 1);
    }

    public static float[] zenith(float dayness, float rain) {
        float r = lerp(dayness, 0.04F, 0.16F);
        float g = lerp(dayness, 0.06F, 0.40F);
        float b = lerp(dayness, 0.16F, 0.48F);
        return dim(r, g, b, rain);
    }

    public static float[] horizon(float dayness, float rain) {
        float r = lerp(dayness, 0.10F, 0.42F);
        float g = lerp(dayness, 0.14F, 0.62F);
        float b = lerp(dayness, 0.28F, 0.58F);
        return dim(r, g, b, rain);
    }

    public static float[] nadir(float dayness, float rain) {
        float r = lerp(dayness, 0.02F, 0.08F);
        float g = lerp(dayness, 0.03F, 0.16F);
        float b = lerp(dayness, 0.08F, 0.22F);
        return dim(r, g, b, rain);
    }

    public static float latticeAlpha(float dayness) {
        return 0.10F + dayness * 0.16F;
    }

    public static float sunAlpha(float dayness, float rain) {
        return dayness * (1F - Math.clamp(rain, 0F, 1F) * 0.7F);
    }

    public static float moonAlpha(float dayness, float rain) {
        return (1F - dayness) * (1F - Math.clamp(rain, 0F, 1F) * 0.5F);
    }

    private static float[] dim(float r, float g, float b, float rain) {
        float d = 1F - Math.clamp(rain, 0F, 1F) * 0.45F;
        return new float[] { r * d, g * d, b * d };
    }

    private static float lerp(float t, float a, float b) {
        return a + Math.clamp(t, 0F, 1F) * (b - a);
    }
}
