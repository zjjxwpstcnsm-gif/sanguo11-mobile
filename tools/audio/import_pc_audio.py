#!/usr/bin/env python3
"""Lossless KOVS -> Ogg extraction, strict pages and actual PCM decode; bindings stay unknown."""
import argparse
import hashlib
import io
import json
from pathlib import Path
import struct
import sys
import wave
import av
import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'content'))
from pc_resources import Archive

DECODER_REFERENCE = 'https://github.com/vgmstream/vgmstream/blob/master/src/meta/ogg_vorbis.c'
CRC_TABLE = []
for seed in range(256):
    value = seed << 24
    for _ in range(8):
        value = ((value << 1) ^ (0x04c11db7 if value & 0x80000000 else 0)) & 0xffffffff
    CRC_TABLE.append(value)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def unwrap(raw):
    if len(raw) < 32 or raw[:4] != b'KOVS':
        raise ValueError('KOVS header')
    length, loop = struct.unpack_from('<Ii', raw, 4)
    if length != len(raw)-32 or loop < 0:
        raise ValueError('KOVS size/loop')
    data = bytearray(raw[32:])
    for index in range(min(256, len(data))):
        data[index] ^= index
    offset, sequence, serial, last, eos = 0, 0, None, 0, False
    while offset < len(data):
        if eos or data[offset:offset+4] != b'OggS' or len(data)-offset < 27 or data[offset+4] != 0:
            raise ValueError('Ogg page header/trailing bytes')
        flags = data[offset+5]
        granule, stream, seq = struct.unpack_from('<QII', data, offset+6)
        if seq != sequence or (serial is not None and stream != serial) or (sequence == 0 and not flags & 2):
            raise ValueError('Ogg sequence/serial/BOS')
        segments = data[offset+26]
        header = offset+27+segments
        if header > len(data):
            raise ValueError('Ogg lacing table')
        end = header+sum(data[offset+27:header])
        if end > len(data):
            raise ValueError('Ogg payload truncated')
        stored = struct.unpack_from('<I', data, offset+22)[0]
        page = bytearray(data[offset:end]);page[22:26] = bytes(4)
        crc = 0
        for byte in page:
            crc = ((crc << 8) & 0xffffffff) ^ CRC_TABLE[(crc >> 24) ^ byte]
        if crc != stored:
            raise ValueError('Ogg page CRC')
        if granule != 0xffffffffffffffff:
            last = granule
        eos = bool(flags & 4)
        offset, sequence, serial = end, sequence+1, stream
    if not eos:
        raise ValueError('Ogg missing EOS')
    if loop and loop >= last:
        raise ValueError('KOVS loop start outside original samples')
    return bytes(data), loop, last, sequence


def pcm_metadata(ogg, wavpath=None):
    digest = hashlib.sha256()
    count, square, peak, clips = 0, 0., 0, 0
    with av.open(io.BytesIO(ogg)) as source:
        if len(source.streams.audio) != 1:
            raise ValueError('Unexamined stream count')
        stream = source.streams.audio[0]
        rate = stream.codec_context.sample_rate
        channels = stream.codec_context.channels
        convert = av.AudioResampler(format='s16', layout=stream.codec_context.layout, rate=rate)
        sink = wave.open(str(wavpath), 'wb') if wavpath else None
        if sink:
            sink.setnchannels(channels);sink.setsampwidth(2);sink.setframerate(rate)
        def consume(frame):
            nonlocal count, square, peak, clips
            a = frame.to_ndarray().reshape(-1).astype('<i2', copy=False)
            data = a.tobytes();digest.update(data)
            if sink:sink.writeframesraw(data)
            x = a.astype(np.float64)
            count += len(a)//channels
            square += float(np.dot(x, x))
            peak = max(peak, int(np.abs(x).max(initial=0)))
            clips += int(np.count_nonzero((a == 32767) | (a == -32768)))
        try:
            for frame in source.decode(audio=0):
                for converted in convert.resample(frame):consume(converted)
            for converted in convert.resample(None):consume(converted)
        finally:
            if sink:sink.close()
    if not count:
        raise ValueError('Empty decoded PCM')
    rms = (square/(count*channels))**0.5/32768
    return dict(sampleRate=rate, channels=channels, decodedSamples=count, durationSeconds=count/rate,
                pcmS16leSha256=digest.hexdigest(), peakS16=peak, clippingSamples=clips,
                rmsDbfs=20*np.log10(rms) if rms else None,
                loudnessBasis='Original gain; decoded PCM RMS/peak only, not LUFS or loudness normalization')


def convert(installation, output, wav):
    installation, output = installation.resolve(), output.resolve()
    if output == installation or installation in output.parents or output.exists():
        raise ValueError('Fresh output outside read-only installation required')
    output.mkdir(parents=True)
    archive = Archive(installation/'Media/san11pkres.bin')
    rows = []
    try:
        for index, (offset, size) in enumerate(archive.entries):
            raw = archive.read(index)
            if raw[:4] != b'KOVS':continue
            ogg, loop, granule, pages = unwrap(raw)
            for suffix, data in [('kovs', raw), ('ogg', ogg)]:
                p = output/suffix/f'{index:04d}.{suffix}';p.parent.mkdir(exist_ok=True);p.write_bytes(data)
            wavpath = output/'wav'/f'{index:04d}.wav' if wav else None
            if wavpath:wavpath.parent.mkdir(exist_ok=True)
            metadata = pcm_metadata(ogg, wavpath)
            row = dict(resourceId=index, sourceOffset=offset, sourceBytes=size, sourceSha256=sha(raw),
                       oggSha256=sha(ogg), headerHex=raw[:32].hex(), oggPages=pages, endGranule=granule,
                       loopStartSample=loop or None, loopEndSample=granule if loop else None,
                       loopBasis='Original KOVS int32 at +8; end from original Ogg EOS granule',
                       status='DECODED_ORIGINAL_BINDING_UNKNOWN', eventId=None, officerId=None, voiceType=None,
                       asset=f'ogg/{index:04d}.ogg', original=f'kovs/{index:04d}.kovs', **metadata)
            if wavpath:row.update(wav=f'wav/{index:04d}.wav', wavSha256=sha(wavpath.read_bytes()))
            rows.append(row)
            if len(rows) % 100 == 0:print('decoded',len(rows),flush=True)
    finally:archive.close()
    manifest = dict(schema=1, sourcePolicy='READ_ONLY_NO_WINE', sourceFile='Media/san11pkres.bin',
                    sourceSha256=sha((installation/'Media/san11pkres.bin').read_bytes()),
                    decodedSamples=len(rows), verifiedEventBindings=0, effectiveOfficerVoiceCoverage=0,
                    decoderReference=DECODER_REFERENCE, pyavVersion=av.__version__,
                    libavVersions={k:list(v) for k,v in av.library_versions.items()}, entries=rows,
                    limits=['Duration, channels or listening do not prove BGM/event/actor identity.',
                            'No clean retail source supplied; installed MOD bytes preserved without assumed backup activation.',
                            'No event playback or installed APK acceptance claimed.'])
    (output/'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    print(json.dumps(dict(decodedSamples=len(rows), verifiedEventBindings=0)))


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation', type=Path);p.add_argument('--output', type=Path, required=True)
    p.add_argument('--wav', action='store_true')
    a = p.parse_args();convert(a.installation, a.output, a.wav)
