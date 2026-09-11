"""Author the Drum Circle music disc (design 3.1 §15).

A 96-second original: the four-beat wake, a circle of drums, a wind-harp pentatonic,
and a silence that the last chorus answers. Encodes OGG Vorbis with stream-friendly
bitrate. Do not ship a silent placeholder.

    python tools/generate_3_1_disc.py
"""
from __future__ import annotations

import json
import math
import sys
import wave
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_3_1_sounds import SR, ffmpeg_exe, kick, normalize, tone, noise, mix, env  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/tribalpower"
RECORDS = ASSETS / "sounds" / "records"
DATA = ROOT / "src/main/resources/data/tribalpower"
BPM = 90
BEAT = 60.0 / BPM
BARS = 36
SECONDS = BARS * 4 * BEAT  # 96.0
SONG_ID = "drum_circle"


def stereo(left: np.ndarray, right: np.ndarray) -> np.ndarray:
    n = max(len(left), len(right))
    L = np.zeros(n)
    R = np.zeros(n)
    L[: len(left)] = left
    R[: len(right)] = right
    return np.stack([L, R], axis=1)


def place(track: np.ndarray, part: np.ndarray, at: float, gain: float = 1.0) -> None:
    start = int(at * SR)
    if start >= len(track) or start < 0:
        return
    end = min(len(track), start + len(part))
    track[start:end] += part[: end - start] * gain


def lowpass(x: np.ndarray, k: int) -> np.ndarray:
    if k < 2:
        return x
    kernel = np.ones(k) / k
    return np.convolve(x, kernel, mode="same")


def harp_note(freq: float, seconds: float) -> np.ndarray:
    body = tone(freq, seconds, 0.004, seconds * 0.42, harmonics=((1, 1.0), (2, 0.42), (3, 0.16), (5, 0.07), (8, 0.03)))
    air = tone(freq * 2.01, seconds * 0.7, 0.01, seconds * 0.35, harmonics=((1, 0.35), (2, 0.08)))
    return normalize(mix(body, air * 0.4), peak=0.9)


def bell_note(freq: float, seconds: float) -> np.ndarray:
    return tone(freq, seconds, 0.006, seconds * 0.5, harmonics=((1, 1.0), (2.76, 0.38), (5.4, 0.12)))


def compose() -> tuple[np.ndarray, np.ndarray]:
    n = int(SECONDS * SR)
    left = np.zeros(n)
    right = np.zeros(n)
    rng = np.random.default_rng(0xD2C1E7)

    # Pentatonic around A3: A C D E G, two octaves. The Unsung's four-beat is 16–28 ticks (~1s).
    scale = [220.00, 261.63, 293.66, 329.63, 392.00, 440.00, 523.25, 587.33, 659.25, 783.99]
    melody_a = [0, 2, 4, 2, 3, 4, 7, 4, 5, 4, 2, 0, 4, 2, 0, -1]
    melody_b = [4, 5, 7, 5, 4, 2, 0, 2, 4, 7, 9, 7, 5, 4, 2, 0]
    bass = [0, 0, 3, 2, 0, 0, 4, 3]

    def beat_time(bar: int, beat: float) -> float:
        return (bar * 4 + beat) * BEAT

    # Four isolated wake beats — the Silent Drum's count-in.
    for i in range(4):
        hit = kick(68 + i, 0.34, 0.6, rng)
        t = beat_time(i, 0)
        place(left, hit, t, 0.95)
        place(right, hit, t, 0.95)
        if i == 3:
            chime = bell_note(659.25, 1.6)
            place(left, chime, t + 0.04, 0.35)
            place(right, chime, t + 0.04, 0.45)

    # Circle of drums from bar 4: kick on 1/3, slap on 2/4, a quiet 12-pillar tick.
    for bar in range(4, 32):
        accent = 1.0 if bar % 4 == 0 else 0.82
        for b, freq, punch, pan, g in (
            (0.0, 72, 0.55, 0.0, 0.85 * accent),
            (1.0, 96, 0.35, -0.35, 0.45),
            (2.0, 68, 0.5, 0.05, 0.78),
            (3.0, 90, 0.3, 0.35, 0.42),
        ):
            hit = kick(freq, 0.28, punch, rng)
            t = beat_time(bar, b)
            place(left, hit, t, g * (1 - pan) * 0.5 + g * 0.5)
            place(right, hit, t, g * (1 + pan) * 0.5 + g * 0.5)
        # Twelve faint pillar taps around the bar, one per pillar.
        if bar >= 8:
            for p in range(12):
                tap = kick(140 + (p % 5) * 8, 0.06, 0.12, rng) * 0.22
                t = beat_time(bar, p / 12.0)
                pan = math.sin(p * math.tau / 12)
                place(left, tap, t, 0.55 - 0.35 * pan)
                place(right, tap, t, 0.55 + 0.35 * pan)

    # Low ostinato under the drums.
    for bar in range(4, 32):
        degree = bass[bar % len(bass)]
        note = harp_note(scale[degree] / 2, BEAT * 3.6)
        t = beat_time(bar, 0)
        place(left, note, t, 0.22)
        place(right, note, t, 0.18)

    def phrase(start_bar: int, degrees: list[int], gain: float) -> None:
        for i, deg in enumerate(degrees):
            if deg < 0:
                continue
            freq = scale[min(deg, len(scale) - 1)]
            dur = BEAT * (1.8 if i % 4 == 3 else 0.95)
            note = harp_note(freq, dur)
            t = beat_time(start_bar, i * 0.5)
            pan = -0.25 if i % 2 == 0 else 0.25
            place(left, note, t, gain * (0.7 - pan * 0.3))
            place(right, note, t, gain * (0.7 + pan * 0.3))

    phrase(12, melody_a, 0.55)
    phrase(16, melody_b, 0.6)
    phrase(20, melody_a, 0.7)
    phrase(24, melody_b, 0.75)

    # Wake-bell answers in the chorus.
    for bar in (20, 24, 28):
        chime = bell_note(scale[7], 2.4)
        t = beat_time(bar, 0)
        place(left, chime, t, 0.28)
        place(right, chime, t, 0.38)
        echo = bell_note(scale[4], 1.8)
        place(left, echo, t + BEAT * 2, 0.22)
        place(right, echo, t + BEAT * 2, 0.18)

    # Silence phase: drums drop, one held harp, then the four-beat resync.
    for i in range(4):
        hit = kick(60, 0.4, 0.45, rng)
        t = beat_time(32, i)
        place(left, hit, t, 0.7)
        place(right, hit, t, 0.7)
    last = harp_note(scale[0], BEAT * 8)
    place(left, last, beat_time(32, 0), 0.5)
    place(right, last, beat_time(32, 0), 0.55)
    close = bell_note(scale[9], 3.5)
    place(left, close, beat_time(34, 0), 0.4)
    place(right, close, beat_time(34, 0), 0.5)

    # Air bed so the disc is never a dry click track.
    air = lowpass(noise(SECONDS, 0.4, SECONDS * 0.9, lowpass=24, rng=rng), 32) * 0.04
    fade = np.minimum(1.0, np.minimum(np.arange(n) / (0.4 * SR), np.arange(n)[::-1] / (4.0 * SR)))
    left += air[:n] * fade
    right += air[:n] * fade * 0.92
    left *= fade
    right *= fade
    peak = max(np.max(np.abs(left)), np.max(np.abs(right)), 1e-9)
    left *= 0.86 / peak
    right *= 0.86 / peak
    return left, right


def write_stereo_ogg(path: Path, left: np.ndarray, right: np.ndarray) -> None:
    import subprocess

    path.parent.mkdir(parents=True, exist_ok=True)
    wav = path.with_suffix(".wav")
    pcm = np.stack(
        [(np.clip(left, -1, 1) * 32767).astype(np.int16), (np.clip(right, -1, 1) * 32767).astype(np.int16)],
        axis=1,
    )
    with wave.open(str(wav), "wb") as w:
        w.setnchannels(2)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    subprocess.run(
        [ffmpeg_exe(), "-y", "-loglevel", "error", "-i", str(wav), "-c:a", "libvorbis", "-q:a", "5", str(path)],
        check=True,
    )
    wav.unlink()
    if path.stat().st_size < 20_000:
        raise SystemExit(f"{path} is too small to be a music disc ({path.stat().st_size} bytes)")
    print("wrote", path.relative_to(ROOT), path.stat().st_size, "bytes", f"{SECONDS:.1f}s")


def patch_jukebox_length() -> None:
    song = DATA / "jukebox_song" / f"{SONG_ID}.json"
    data = json.loads(song.read_text(encoding="utf-8"))
    data["length_in_seconds"] = float(SECONDS)
    song.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def generate() -> None:
    left, right = compose()
    write_stereo_ogg(RECORDS / "drum_circle.ogg", left, right)
    patch_jukebox_length()


if __name__ == "__main__":
    generate()
