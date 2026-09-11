"""Original short device hits for Tribal Power 3.1 (design §15).

Synthesises WAV with numpy, encodes OGG Vorbis with ffmpeg. Not a music disc: these are
one-shot hits for the Drumheart, the five new voices, the mesh, the font, chalk, and the gates.

    python tools/generate_3_1_sounds.py
"""
from __future__ import annotations

import json
import subprocess
import wave
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/tribalpower"
SOUNDS = ASSETS / "sounds"
SR = 44100


def env(n: int, attack: float, decay: float, sr: int = SR) -> np.ndarray:
    t = np.arange(n) / sr
    a = np.clip(t / max(attack, 1e-4), 0, 1)
    d = np.exp(-t / max(decay, 1e-4))
    return a * d


def tone(freq: float, seconds: float, attack=0.005, decay=0.3, harmonics=((1, 1.0),), sr: int = SR) -> np.ndarray:
    n = int(seconds * sr)
    t = np.arange(n) / sr
    out = np.zeros(n)
    for mult, amp in harmonics:
        out += amp * np.sin(2 * np.pi * freq * mult * t)
    return out * env(n, attack, decay, sr)


def noise(seconds: float, attack=0.001, decay=0.05, lowpass=0.0, rng=None, sr: int = SR) -> np.ndarray:
    n = int(seconds * sr)
    rng = np.random.default_rng(7) if rng is None else rng
    x = rng.standard_normal(n)
    if lowpass > 0:
        k = int(lowpass)
        x = np.convolve(x, np.ones(k) / k, mode="same")
    return x * env(n, attack, decay, sr)


def mix(*parts: np.ndarray, offsets=None) -> np.ndarray:
    offsets = offsets or [0] * len(parts)
    n = max(len(p) + o for p, o in zip(parts, offsets))
    out = np.zeros(n)
    for p, o in zip(parts, offsets):
        out[o:o + len(p)] += p
    return out


def normalize(x: np.ndarray, peak=0.85) -> np.ndarray:
    m = np.max(np.abs(x)) or 1.0
    return x / m * peak


def ffmpeg_exe() -> str:
    from shutil import which
    found = which("ffmpeg")
    if found:
        return found
    import imageio_ffmpeg
    return imageio_ffmpeg.get_ffmpeg_exe()


def write_ogg(name: str, data: np.ndarray, sr: int = SR) -> Path:
    SOUNDS.mkdir(parents=True, exist_ok=True)
    wav = SOUNDS / f"{name}.wav"
    ogg = SOUNDS / f"{name}.ogg"
    pcm = (np.clip(data, -1, 1) * 32767).astype(np.int16)
    with wave.open(str(wav), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(sr)
        w.writeframes(pcm.tobytes())
    subprocess.run(
        [ffmpeg_exe(), "-y", "-loglevel", "error", "-i", str(wav),
         "-c:a", "libvorbis", "-q:a", "4", "-map_metadata", "-1", "-fflags", "+bitexact", str(ogg)],
        check=True,
    )
    wav.unlink()
    if ogg.stat().st_size < 400:
        raise SystemExit(f"{ogg} is too small to be real audio ({ogg.stat().st_size} bytes)")
    print("wrote", ogg.relative_to(ROOT), ogg.stat().st_size, "bytes")
    return ogg


def kick(freq: float, seconds: float, punch: float, rng) -> np.ndarray:
    n = int(seconds * SR)
    t = np.arange(n) / SR
    sweep = freq * np.exp(-t / 0.05)
    body = np.sin(2 * np.pi * np.cumsum(sweep) / SR) * env(n, 0.001, 0.09)
    click = noise(0.04, 0.0005, 0.008, lowpass=4, rng=rng) * punch
    return mix(body, click)


def drumheart_tempo() -> np.ndarray:
    rng = np.random.default_rng(31)
    return normalize(mix(kick(72, 0.32, 0.55, rng), tone(48, 0.22, 0.001, 0.08, harmonics=((1, 0.6), (2, 0.2))) * 0.4))


def drumheart_off_tempo() -> np.ndarray:
    rng = np.random.default_rng(32)
    return normalize(mix(kick(54, 0.28, 0.25, rng), tone(40, 0.18, 0.001, 0.06, harmonics=((1, 0.5),)) * 0.3), peak=0.7)


def ember_horn_roar() -> np.ndarray:
    rng = np.random.default_rng(41)
    whoosh = noise(0.45, 0.02, 0.18, lowpass=12, rng=rng)
    n = len(whoosh)
    t = np.arange(n) / SR
    flame = np.sin(2 * np.pi * (90 + 40 * np.sin(2 * np.pi * 8 * t)) * t) * env(n, 0.01, 0.16)
    crackle = noise(0.45, 0.001, 0.05, lowpass=2, rng=rng) * 0.35
    return normalize(mix(whoosh * 0.7, flame * 0.45, crackle))


def wind_harp_string() -> np.ndarray:
    pluck = tone(392, 0.9, 0.002, 0.28, harmonics=((1, 1.0), (2, 0.45), (3, 0.18), (5, 0.08)))
    air = tone(784, 0.7, 0.004, 0.22, harmonics=((1, 0.4), (2.01, 0.12)))
    return normalize(mix(pluck, air * 0.55))


def wave_drum_slap() -> np.ndarray:
    rng = np.random.default_rng(51)
    skin = kick(90, 0.22, 0.7, rng)
    splash = noise(0.16, 0.001, 0.04, lowpass=8, rng=rng) * 0.55
    return normalize(mix(skin, splash))


def wake_bell_toll() -> np.ndarray:
    bell = tone(330, 1.4, 0.004, 0.55, harmonics=((1, 1.0), (2.76, 0.45), (5.4, 0.18), (8.2, 0.08)))
    strike = tone(220, 0.3, 0.001, 0.08, harmonics=((1, 0.6), (3, 0.2)))
    return normalize(mix(strike, bell, offsets=[0, int(0.02 * SR)]))


def gate_hum() -> np.ndarray:
    n = int(1.8 * SR)
    t = np.arange(n) / SR
    hum = (np.sin(2 * np.pi * 55 * t) + 0.55 * np.sin(2 * np.pi * 55.7 * t)
           + 0.25 * np.sin(2 * np.pi * 110.2 * t))
    fade = np.minimum(1, np.minimum(t / 0.12, (1.8 - t) / 0.18))
    return normalize(hum * fade, 0.55)


def gate_transit() -> np.ndarray:
    rng = np.random.default_rng(61)
    drum = kick(64, 0.28, 0.4, rng)
    whoosh = noise(0.5, 0.01, 0.16, lowpass=18, rng=rng)
    n = len(whoosh)
    t = np.arange(n) / SR
    sweep = np.sin(2 * np.pi * (180 + 420 * t / 0.5) * t) * env(n, 0.01, 0.18)
    chime = tone(660, 0.7, 0.004, 0.3, harmonics=((1, 1.0), (2.0, 0.3), (2.76, 0.15)))
    return normalize(mix(drum, whoosh * 0.45, sweep * 0.35, chime * 0.5, offsets=[0, 0, int(0.04 * SR), int(0.08 * SR)]))


def mesh_sift() -> np.ndarray:
    rng = np.random.default_rng(71)
    grit = noise(0.32, 0.004, 0.09, lowpass=5, rng=rng)
    pebbles = mix(*[kick(140 + i * 18, 0.08, 0.2, rng) * 0.35 for i in range(5)],
                  offsets=[int(i * 0.035 * SR) for i in range(5)])
    return normalize(mix(grit * 0.8, pebbles))


def font_form() -> np.ndarray:
    rng = np.random.default_rng(81)
    return normalize(mix(kick(110, 0.18, 0.35, rng), tone(180, 0.16, 0.001, 0.05, harmonics=((1, 0.5), (2, 0.2))) * 0.4))


def chalk_draw() -> np.ndarray:
    rng = np.random.default_rng(91)
    scratch = noise(0.18, 0.004, 0.05, lowpass=3, rng=rng)
    n = len(scratch)
    t = np.arange(n) / SR
    scrape = np.sin(2 * np.pi * (1400 + 600 * t / 0.18) * t) * env(n, 0.003, 0.05) * 0.25
    return normalize(mix(scratch, scrape), peak=0.65)


HITS = {
    "drumheart_tempo": ("Drumheart on tempo", drumheart_tempo),
    "drumheart_off_tempo": ("Drumheart off tempo", drumheart_off_tempo),
    "ember_horn_roar": ("Ember Horn roar", ember_horn_roar),
    "wind_harp_string": ("Wind Harp string", wind_harp_string),
    "wave_drum_slap": ("Wave Drum slap", wave_drum_slap),
    "wake_bell_toll": ("Wake Bell toll", wake_bell_toll),
    "gate_hum": ("Gate hum", gate_hum),
    "gate_transit": ("Gate transit", gate_transit),
    "mesh_sift": ("Resonance Mesh sift", mesh_sift),
    "font_form": ("Stone Font form", font_form),
    "chalk_draw": ("Ritual Chalk draw", chalk_draw),
}


def sounds_json() -> dict:
    out = {}
    for name, (subtitle, _) in HITS.items():
        out[name] = {
            "subtitle": f"subtitles.tribalpower.{name}",
            "sounds": [f"tribalpower:{name}"],
        }
        _ = subtitle
    out["music_disc.drum_circle"] = {
        "sounds": [{"name": "tribalpower:records/drum_circle", "stream": True}],
    }
    return out


def generate():
    for name, (_, fn) in HITS.items():
        write_ogg(name, fn())
    path = ASSETS / "sounds.json"
    path.write_text(json.dumps(sounds_json(), indent=2) + "\n", encoding="utf-8")
    print("wrote", path.relative_to(ROOT), len(HITS), "events")


if __name__ == "__main__":
    generate()
