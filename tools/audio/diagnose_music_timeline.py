#!/usr/bin/env python3
"""Measure fixed music windows in unchanged PCM. Diagnostic only, never acceptance.

Each declared reference window is matched independently. Offset jumps can locate
timeline discontinuities but cannot identify the responsible player/mixer/recorder.
No sample editing, alignment correction, or lowering of playback thresholds.
"""
import argparse
import hashlib
import json
from pathlib import Path
import wave
import numpy as np


def read(path):
    with wave.open(str(path)) as source:
        if source.getsampwidth() != 2:
            raise ValueError('PCM16 required')
        rate = source.getframerate()
        signal = np.frombuffer(source.readframes(source.getnframes()), '<i2')
        return rate, signal.reshape(-1, source.getnchannels()).mean(1) / 32768


def inspect(capture, reference, output):
    if output.exists():
        raise ValueError('Fresh diagnostic output required')
    rate, x = read(capture)
    reference_rate, y = read(reference)
    if rate != reference_rate or len(x) < len(y):
        raise ValueError('Equal sample rates and a full reference interval required')
    taps = np.arange(-32, 33)
    kernel = np.sinc(taps * .1) * np.hamming(len(taps))
    kernel /= kernel.sum()
    x, y = [np.convolve(signal, kernel, 'same') for signal in (x, y)]
    coarse = x[::8]
    prefix = np.r_[0, np.cumsum(coarse * coarse)]
    rows = []
    # Fixed before inspecting recordings. No selecting only successful windows.
    for second in range(5, 86, 5):
        template = y[second * rate:(second + 2) * rate]
        if len(template) != 2 * rate:
            raise ValueError('Reference must contain all declared windows')
        short = template[::8]
        size = 1 << (len(coarse) + len(short) - 2).bit_length()
        cross = np.fft.irfft(np.fft.rfft(coarse, size) *
                             np.fft.rfft(short[::-1], size), size)
        cross = cross[len(short) - 1:len(coarse)]
        energy = prefix[len(short):] - prefix[:-len(short)]
        scores = cross / np.sqrt(np.maximum(1e-20, energy * float(short @ short)))
        seed = int(np.argmax(scores)) * 8
        best = None
        for at in range(max(0, seed - 64), min(len(x) - len(template), seed + 64) + 1):
            window = x[at:at + len(template)]
            dot = float(window @ template)
            correlation = dot / np.sqrt(max(1e-20, float(window @ window) * float(template @ template)))
            candidate = (float(correlation), at, dot / max(1e-20, float(template @ template)))
            if best is None or candidate[0] > best[0]:
                best = candidate
        correlation, at, gain = best
        rows.append(dict(referenceSeconds=second, frames=len(template),
                         captureSample=at, offsetSamples=at - second * rate,
                         correlation=correlation, gain=gain,
                         reliableOffset=correlation >= .999 and gain > 0))
    report = dict(status='MEASURED_DIAGNOSTIC', sampleRate=rate,
                  scope='Independent fixed windows in unchanged PCM; no continuity pass or responsible component attribution',
                  captureSha256=hashlib.sha256(capture.read_bytes()).hexdigest(),
                  referenceSha256=hashlib.sha256(reference.read_bytes()).hexdigest(), rows=rows)
    output.write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(dict(status=report['status'], windows=len(rows),
                         offsets=[row['offsetSamples'] for row in rows])))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('capture', type=Path)
    parser.add_argument('reference', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    inspect(args.capture, args.reference, args.output)
