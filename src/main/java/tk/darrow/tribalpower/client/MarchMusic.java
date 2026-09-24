package tk.darrow.tribalpower.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import tk.darrow.tribalpower.boss.TheUnsungEntity;
import tk.darrow.tribalpower.entity.LatticeMonster;
import tk.darrow.tribalpower.event.MarchStatePayload;
import tk.darrow.tribalpower.event.MarchWeather;
import tk.darrow.tribalpower.guardian.GuardianEntity;
import tk.darrow.tribalpower.quest.QuestStatePayload;
import tk.darrow.tribalpower.sound.ModSounds;

/**
 * The client's ear for the March: a boss theme while a boss is near (the ordinary music steps aside), the finale's
 * theme when the Loom agrees, and a stinger when a festival, a surge or a weather begins.
 */
public final class MarchMusic {
    private static final double RANGE = 48;
    private static BossTheme playing;
    private static MarchStatePayload lastState = MarchStatePayload.EMPTY;
    private static boolean lastAgreed;
    private static boolean primed;

    private MarchMusic() {}

    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) { reset(); return; }
        if (mc.player.tickCount % 10 == 0) bosses(mc);
        stingers(mc);
    }

    private static void bosses(Minecraft mc) {
        LivingEntity boss = null;
        double best = RANGE * RANGE;
        for (LivingEntity entity : mc.level.getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(RANGE), MarchMusic::isBoss)) {
            double d = entity.distanceToSqr(mc.player);
            if (d < best) { best = d; boss = entity; }
        }
        if (boss == null) {
            if (playing != null) { playing.fade(); playing = null; }
            return;
        }
        SoundEvent theme = themeOf(boss);
        if (theme == null) return;
        if (playing == null || playing.theme != theme || playing.isStopped()) {
            if (playing != null) playing.fade();
            playing = new BossTheme(theme);
            mc.getSoundManager().play(playing);
        }
        mc.getMusicManager().stopPlaying();
    }

    private static boolean isBoss(LivingEntity entity) {
        return entity.isAlive() && (entity instanceof GuardianEntity || entity instanceof TheUnsungEntity
                || entity instanceof LatticeMonster monster && monster.profile().boss());
    }

    private static SoundEvent themeOf(LivingEntity boss) {
        if (boss instanceof GuardianEntity guardian) return ModSounds.BOSS_THEMES.get(guardian.guardian().id).get();
        if (boss instanceof TheUnsungEntity) return ModSounds.BOSS_THEMES.get("the_unsung").get();
        if (boss instanceof LatticeMonster monster) return ModSounds.BOSS_THEMES.get(monster.profile().id).get();
        return null;
    }

    private static void stingers(Minecraft mc) {
        MarchStatePayload state = MarchStatePayload.latest;
        boolean agreed = QuestStatePayload.latest.agreed();
        if (!primed) { lastState = state; lastAgreed = agreed; primed = true; return; }
        if (state != lastState) {
            boolean weather = false;
            for (MarchWeather kind : MarchWeather.values()) if (state.weather(kind) && !lastState.weather(kind)) weather = true;
            if (weather) play(mc, ModSounds.STINGER_WEATHER.get());
            if (state.surge() != null && lastState.surge() == null) play(mc, ModSounds.STINGER_SURGE.get());
            if (state.festival() != null && state.festival() != lastState.festival()) play(mc, ModSounds.STINGER_FESTIVAL.get());
            lastState = state;
        }
        if (agreed && !lastAgreed) {
            mc.getMusicManager().stopPlaying();
            mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.BOSS_THEMES.get("finale").get(), 1F, 1F));
        }
        lastAgreed = agreed;
    }

    private static void play(Minecraft mc, SoundEvent stinger) {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(stinger, 1F, 0.8F));
    }

    public static void reset() {
        if (playing != null) { playing.fade(); playing = null; }
        primed = false;
        lastState = MarchStatePayload.EMPTY;
        lastAgreed = false;
    }

    /** A looping theme that fades out over a second when told to. */
    private static final class BossTheme extends AbstractTickableSoundInstance {
        final SoundEvent theme;
        private int fading = -1;

        BossTheme(SoundEvent theme) {
            super(theme, SoundSource.MUSIC, SoundInstanceRandom.INSTANCE);
            this.theme = theme;
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            this.volume = 0.0F;
        }

        void fade() { if (fading < 0) fading = 20; }

        @Override
        public void tick() {
            if (fading >= 0) {
                volume = fading / 20F;
                if (--fading < 0) stop();
                return;
            }
            volume = Math.min(1F, volume + 0.05F);
        }
    }

    /** The sound system's random, held once. */
    private static final class SoundInstanceRandom {
        static final net.minecraft.util.RandomSource INSTANCE = net.minecraft.util.RandomSource.create();
    }

    public static List<String> themes() { return List.copyOf(ModSounds.BOSS_THEMES.keySet()); }
}
