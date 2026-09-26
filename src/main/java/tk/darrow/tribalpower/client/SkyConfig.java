package tk.darrow.tribalpower.client;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class SkyConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED,OVERWORLD,MARCH,SORT_BUTTONS;
    public static final ModConfigSpec.DoubleValue INTENSITY;
    public static final ModConfigSpec.IntValue QUALITY, SONG_OFFSET_MS, SONG_DIFFICULTY;
    public static final ModConfigSpec.DoubleValue SONG_SPEED;
    public static final ModConfigSpec.BooleanValue SONG_HIT_SOUNDS;
    static {
        var b=new ModConfigSpec.Builder();
        ENABLED=b.comment("Enable the lightweight aurora sky layer. Disable when using a shader pack that replaces the sky.").define("auroraEnabled",true);
        OVERWORLD=b.define("overworldAurora",true);MARCH=b.define("marchAurora",true);
        INTENSITY=b.comment("Brightness multiplier; 0 disables the effect.").defineInRange("auroraIntensity",.7,0,1.5);
        SORT_BUTTONS=b.comment("Draw a tidy button above the player's pack and above plain storage (chests, barrels, shulkers, hoppers and Tribal Power caches).").define("sortButtons",true);
        QUALITY=b.comment("0: reduced (48 segments), 1: standard (96), 2: detailed (144). No world particles are spawned.").defineInRange("auroraQuality",1,0,2);
        b.push("songkeeper");
        SONG_OFFSET_MS=b.comment("Songkeeper Drum audio calibration in milliseconds. Raise it if notes reach the strike line before you hear them; lower it if after.").defineInRange("audioOffsetMs",0,-300,300);
        SONG_SPEED=b.comment("How fast notes travel down the Songkeeper highway: 1 is standard, 2 twice as fast.").defineInRange("highwaySpeed",1.0,0.5,2.5);
        SONG_HIT_SOUNDS=b.comment("A soft drum tap on every hit, on top of the song.").define("hitSounds",false);
        SONG_DIFFICULTY=b.comment("The difficulty the song list opens on (0 Easy .. 3 Expert).").defineInRange("lastDifficulty",1,0,3);
        b.pop();
        SPEC=b.build();
    }
}
