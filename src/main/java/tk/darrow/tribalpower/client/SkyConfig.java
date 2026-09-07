package tk.darrow.tribalpower.client;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class SkyConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED,OVERWORLD,MARCH;
    public static final ModConfigSpec.DoubleValue INTENSITY;
    public static final ModConfigSpec.IntValue QUALITY;
    static {
        var b=new ModConfigSpec.Builder();
        ENABLED=b.comment("Enable the lightweight aurora sky layer. Disable when using a shader pack that replaces the sky.").define("auroraEnabled",true);
        OVERWORLD=b.define("overworldAurora",true);MARCH=b.define("marchAurora",true);
        INTENSITY=b.comment("Brightness multiplier; 0 disables the effect.").defineInRange("auroraIntensity",.7,0,1.5);
        QUALITY=b.comment("0: reduced (48 segments), 1: standard (96), 2: detailed (144). No world particles are spawned.").defineInRange("auroraQuality",1,0,2);
        SPEC=b.build();
    }
}
