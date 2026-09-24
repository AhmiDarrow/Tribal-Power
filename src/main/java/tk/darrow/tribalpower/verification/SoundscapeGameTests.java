package tk.darrow.tribalpower.verification;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.guardian.Guardian;
import tk.darrow.tribalpower.sound.ModSounds;

/** The soundscape: every March biome plays its own music, every boss has a theme, every guardian a voice, and the stingers exist. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public final class SoundscapeGameTests {
    private SoundscapeGameTests() {}

    @GameTest(template = "empty")
    public static void everyMarchBiomeHasItsOwnMusic(GameTestHelper h) {
        var biomes = h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for (String id : ModSounds.MARCH_BIOMES) {
            var biome = biomes.get(ResourceLocation.fromNamespaceAndPath("tribalpower", id));
            h.assertTrue(biome != null, id + " exists");
            var music = biome.getBackgroundMusic();
            h.assertTrue(music.isPresent(), id + " has music");
            var event = music.get().getEvent().value();
            h.assertTrue(event == ModSounds.BIOME_MUSIC.get(id).get(), id + " plays its own track, got " + event.getLocation());
            h.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(event.getLocation()), id + " music is a registered sound");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void everyBossHasAThemeAndEveryGuardianAVoice(GameTestHelper h) {
        for (String boss : new String[] {"the_unsung", "colossus_warden", "finale"})
            h.assertTrue(ModSounds.BOSS_THEMES.containsKey(boss) && ModSounds.BOSS_THEMES.get(boss).get() != null, boss + " has a theme");
        for (Guardian guardian : Guardian.values()) {
            h.assertTrue(ModSounds.BOSS_THEMES.get(guardian.id).get() != null, guardian.id + " has a theme");
            h.assertTrue(ModSounds.GUARDIAN_VOICES.get(guardian.id).get() != null, guardian.id + " has a voice");
        }
        h.assertTrue(ModSounds.STINGER_FESTIVAL.get() != null && ModSounds.STINGER_SURGE.get() != null && ModSounds.STINGER_WEATHER.get() != null, "Three stingers");
        h.succeed();
    }
}
