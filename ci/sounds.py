"""Synthesised, close-mic'd table sounds for Snakes & Ladders.

Everything is modelled rather than sampled: resin dice clicking in a cupped hand, a die landing on a
felt-covered wooden table, a lacquered piece tapping along the board, soft marimba notes for ladders and
a sliding hiss for snakes. Rendered at 44.1 kHz, 16-bit mono into app/src/main/res/raw.

    python3 ci/sounds.py app/src/main/res/raw
"""
import numpy as np, wave, sys, os
from scipy.signal import butter, lfilter

SR = 44100
rng = np.random.default_rng(23)

def t(dur): return np.arange(int(SR * dur)) / SR

def mode(freq, decay_ms, amp, dur, drift=0.0):
    """A damped sinusoid; drift < 0 lets the pitch sag as it dies (wood), > 0 rise."""
    x = t(dur)
    f = freq * (1 + drift * (1 - np.exp(-x * 1000 / decay_ms)))
    return amp * np.exp(-x * 1000 / decay_ms) * np.sin(2 * np.pi * np.cumsum(f) / SR)

def noise(dur, decay_ms, amp, lo=None, hi=None, r=rng):
    x = t(dur); n = r.standard_normal(len(x)) * np.exp(-x * 1000 / decay_ms) * amp
    if lo or hi: n = band(n, lo or 20, hi or 20000)
    return n

def band(x, lo, hi, order=2):
    b, a = butter(order, [lo, hi], btype='band', fs=SR); return lfilter(b, a, x)

def lowpass(x, cutoff, order=2):
    b, a = butter(order, cutoff, fs=SR); return lfilter(b, a, x)

def highpass(x, cutoff, order=2):
    b, a = butter(order, cutoff, btype='high', fs=SR); return lfilter(b, a, x)

def mix(*parts):
    n = max(len(p) for p in parts); out = np.zeros(n)
    for p in parts: out[:len(p)] += p
    return out

def delay(x, ms): return np.concatenate([np.zeros(int(SR * ms / 1000)), x])

def attack(x, ms=0.8):
    n = max(1, int(SR * ms / 1000)); env = np.ones(len(x)); env[:n] = np.linspace(0, 1, n); return x * env

def resin_click(amp=1.0, seed=0, bright=1.0):
    """Two resin dice knocking together: very short, bright, glassy-plastic."""
    r = np.random.default_rng(seed); j = lambda: 1 + 0.06 * r.standard_normal()
    modes = mix(*[mode(f * j() * bright, d, a, 0.03) for f, d, a in [(2400, 3.0, 0.5), (3900, 2.2, 0.4), (5600, 1.6, 0.25), (7800, 1.1, 0.12)]])
    body = mode(900 * j(), 6, 0.25, 0.04)
    return attack(mix(modes, body, noise(0.006, 0.8, 0.25, 2000, 12000, r)) * amp, 0.15)

def felt_thud(weight=1.0, seed=0, dur=0.4):
    """Something solid landing on felt over wood: warm, short, no ring."""
    r = np.random.default_rng(seed); j = lambda: 1 + 0.03 * r.standard_normal()
    wood = mix(*[mode(f * j(), d, a, dur) for f, d, a in [(140, 55, 0.7), (205, 45, 0.5), (290, 36, 0.35), (420, 28, 0.2)]])
    thump = mode(78 * j(), 70 * weight, 0.9 * weight, dur, drift=-0.15)
    contact = noise(0.02, 3, 0.35, 600, 5000, r)
    return attack(lowpass(mix(wood, thump, contact), 3200), 0.5)

def mallet(freq, amp=1.0, dur=1.2, soft=1.0):
    """A soft-mallet marimba bar: fundamental plus the bar's 4th and 10th partials, gently struck."""
    x = mix(mode(freq, 520, 0.8, dur), mode(freq * 3.93, 120, 0.22, dur), mode(freq * 9.2, 40, 0.06 * soft, dur))
    hit = noise(0.01, 1.5, 0.08, 500, 4000)
    return attack(lowpass(mix(x, hit), 5000) * amp, 2.5)

def write(name, x, peak=0.85):
    x = x / max(1e-9, np.max(np.abs(x))) * peak
    n = min(len(x), int(SR * 0.03)); x[-n:] *= np.linspace(1, 0, n)
    with wave.open(name, 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((x * 32767).astype('<i2').tobytes())

def note(semitones_from_a4): return 440.0 * 2 ** (semitones_from_a4 / 12)

out = sys.argv[1] if len(sys.argv) > 1 else '.'
os.makedirs(out, exist_ok=True)
P = lambda n: os.path.join(out, n)

# Shake: dice rattling in a cupped hand. Clicks cluster and thin out; skin brushes underneath.
r = np.random.default_rng(5)
shake = np.zeros(int(SR * 0.72))
tm = 0.0
for i in range(16):
    tm += 0.025 + r.random() * 0.045
    if tm > 0.66: break
    swell = np.sin(np.pi * tm / 0.7) ** 0.6
    c = delay(resin_click(0.35 + 0.65 * r.random() * swell, seed=100 + i, bright=0.85 + 0.3 * r.random()), tm * 1000)
    shake[:len(c)] += c[:len(shake)]
skin = band(r.standard_normal(len(shake)), 300, 1800) * np.sin(np.pi * t(0.72) / 0.72) ** 2 * 0.05
write(P('shake.wav'), mix(shake, skin), 0.7)

# Land: the die drops onto the felt, bounces twice, rattles to rest.
land = mix(felt_thud(1.0, 7), resin_click(0.5, 8, 0.8),
           delay(mix(felt_thud(0.45, 9, 0.25) * 0.4, resin_click(0.3, 10, 0.8)), 85),
           delay(mix(felt_thud(0.25, 11, 0.2) * 0.2, resin_click(0.18, 12, 0.85)), 140),
           delay(resin_click(0.08, 13), 175))
write(P('land.wav'), land, 0.85)

# Hop: a lacquered wooden piece set down on the board. Small, round, intimate.
hop = mix(*[mode(f, d, a, 0.22) for f, d, a in [(520, 24, 0.6), (780, 18, 0.35), (1320, 9, 0.18), (2650, 4, 0.08)]],
          mode(190, 30, 0.35, 0.22, drift=-0.1), noise(0.008, 1.2, 0.12, 1500, 8000))
write(P('hop.wav'), attack(lowpass(hop, 6000), 0.4), 0.55)

# Ladder: five soft marimba notes climbing a pentatonic scale, with an airy shimmer.
ladder = mix(*[delay(mallet(note(n), 0.55 + 0.1 * i, 1.0), i * 150) for i, n in enumerate([-5, -3, 0, 2, 4, 7])])
air = band(rng.standard_normal(int(SR * 1.4)), 5000, 11000) * np.sin(np.pi * t(1.4) / 1.4) ** 3 * 0.015
write(P('ladder.wav'), mix(ladder, air), 0.75)

# Snake: scales sliding over wood (a falling band of hiss) over a slow, sinking three-note line.
dur = 1.3
x = t(dur)
hiss = rng.standard_normal(len(x))
centre = 3200 * np.exp(-x * 1.6)
slide = np.zeros(len(x)); block = 1024
for s in range(0, len(x), block):
    c = float(centre[s]); slide[s:s + block] = band(hiss[max(0, s - 2048):s + block], c * 0.6, c * 1.5)[-len(slide[s:s + block]):]
slide *= np.sin(np.pi * x / dur) ** 1.2 * 0.12
line = mix(mallet(note(-10), 0.5, 1.4, 0.5), delay(mallet(note(-13), 0.45, 1.4, 0.4), 330), delay(mallet(note(-17), 0.5, 1.4, 0.3), 660))
write(P('snake.wav'), mix(slide, lowpass(line, 2500), delay(felt_thud(0.6, 21, 0.4) * 0.35, 1150)), 0.7)

# Turn: one soft kalimba-like note to hand the board over.
turn = mix(mode(note(7), 380, 0.7, 0.9), mode(note(7) * 5.4, 60, 0.12, 0.9), noise(0.005, 0.8, 0.05, 2000, 9000))
write(P('turn.wav'), attack(lowpass(turn, 6000), 1.5), 0.4)

# Win: a rising arpeggio that blooms into a warm chord, with a few high bells on top.
arp = mix(*[delay(mallet(note(n), 0.6 + 0.08 * i, 2.4), i * 120) for i, n in enumerate([-12, -5, 0, 4, 7, 12])])
chord = delay(mix(mallet(note(-12), 0.7, 2.6), mallet(note(-5), 0.5, 2.6), mallet(note(4), 0.45, 2.6), mallet(note(12), 0.35, 2.6)), 780)
bells = mix(*[delay(mode(note(n) * 2, 700, 0.12, 1.6), 900 + i * 180) for i, n in enumerate([19, 24, 16, 28])])
write(P('win.wav'), mix(arp, chord, bells), 0.8)

# Tap: a tiny dry click for buttons and swatches.
write(P('tap.wav'), mix(mode(1900, 4, 0.5, 0.05), mode(950, 7, 0.3, 0.05), noise(0.004, 0.6, 0.1, 2000, 9000)), 0.35)

# Blocked: two dull taps on felt, a gentle "not this time".
write(P('blocked.wav'), mix(felt_thud(0.6, 30, 0.25), delay(felt_thud(0.5, 31, 0.25) * 0.7, 120)), 0.5)
print("wrote", sorted(os.listdir(out)))
