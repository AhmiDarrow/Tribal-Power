package tk.darrow.tribalpower.client;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;
import net.neoforged.neoforge.client.event.sound.PlayStreamingSourceEvent;

/**
 * When a song really started. The sound engine opens a streamed song on its own thread, some time after it is asked
 * to; NeoForge fires these events on that thread right as the source begins to play. The Songkeeper Drum's clock
 * counts from that instant (plus the player's calibration), so the notes meet the strike line on the recording's
 * own beats however long the stream took to open.
 */
public final class SongClock {
    private static volatile SoundInstance watched;
    private static volatile long startedNanos = -1;

    private SongClock() {}

    /** Start watching for this sound; its start time is unknown until the engine plays it. */
    public static void watch(SoundInstance sound) {
        watched = sound;
        startedNanos = -1;
    }

    /** System.nanoTime() when the watched sound began, or -1 while it has not. */
    public static long started() {
        return startedNanos;
    }

    public static void streaming(PlayStreamingSourceEvent event) {
        if (event.getSound() == watched && startedNanos < 0) startedNanos = System.nanoTime();
    }

    public static void plain(PlaySoundSourceEvent event) {
        if (event.getSound() == watched && startedNanos < 0) startedNanos = System.nanoTime();
    }
}
