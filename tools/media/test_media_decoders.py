#!/usr/bin/env python3
"""Reject truncated/corrupt source media and verify decoded originals, never fabricated fixtures."""
import argparse
import json
from pathlib import Path
import sys
import wave
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'audio'))
from import_pc_audio import unwrap

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('audio',type=Path);p.add_argument('banks',type=Path)
    a=p.parse_args();checks=0
    manifest=json.loads((a.audio/'manifest.json').read_text());row=manifest['entries'][0];raw=(a.audio/row['original']).read_bytes()
    ogg,loop,end,pages=unwrap(raw);assert ogg==(a.audio/row['asset']).read_bytes();checks+=1
    corrupt=bytearray(raw);corrupt[32+60]^=1
    for bad in [raw[:31],raw[:-1],bytes(corrupt),b'NOPE'+raw[4:]]:
        try:unwrap(bad)
        except ValueError:checks+=1
        else:raise AssertionError('Malformed original stream accepted')
    banks=json.loads((a.banks/'manifest.json').read_text());matched=0
    for r in banks['entries']:
        if 'asset' not in r:continue
        with wave.open(str(a.banks/r['asset']),'rb') as f:
            assert f.getframerate()==r['sampleRate'] and f.getnchannels()==r['channels'] and f.getnframes()==r['samples'];checks+=1
        if 33 in r['soundIds']:
            assert (r['bank'],r['slot'],r['headerResource'],r['waveResource'])==(1,19,2268,2267);matched+=1;checks+=1
    assert matched==1;checks+=1
    print('Media decoder PASS:',checks,'checks; original CRC, truncation and PCM headers; no installed playback claim')
