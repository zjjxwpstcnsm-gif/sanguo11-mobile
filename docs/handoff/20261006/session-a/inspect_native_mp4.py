#!/usr/bin/env python3
"""Decode actual screenrecord MP4 without rewriting it or forcing frame rate.

Use filter-input showinfo PTS, not rounded null-muxer timestamps. Pixel/PC/ARM
parity and unperturbed FPS are not proved by a successful recording decode.
"""
import argparse
import hashlib
import json
import pathlib
import re
import subprocess


def sha(path):
    h = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1048576), b''):
            h.update(chunk)
    return h.hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--video-manifest', type=pathlib.Path, required=True)
    parser.add_argument('--output', type=pathlib.Path, required=True)
    parser.add_argument('--ffmpeg', type=pathlib.Path, required=True)
    args = parser.parse_args()
    manifest = json.loads(args.video_manifest.read_text())
    args.output.mkdir(parents=True, exist_ok=False)
    result = {'scope': 'Actual raw native screenrecord every-frame decode and input PTS inspection only; no original PC pixels/timing, fixed FPS, unperturbed performance or ARM claim',
              'videoManifestSha256': sha(args.video_manifest),
              'ffmpegPath': str(args.ffmpeg.resolve()), 'ffmpegSha256': sha(args.ffmpeg),
              'ffmpegVersion': subprocess.check_output([str(args.ffmpeg), '-version'], text=True).splitlines()[0],
              'parts': []}
    for part in manifest['parts']:
        path = pathlib.Path(part['path'])
        digest = sha(path)
        if digest != part['sha256'] or digest != part['deviceSha256']:
            raise ValueError('Actual raw recording device/host SHA mismatch')
        command = [str(args.ffmpeg), '-hide_banner', '-copyts', '-i', str(path),
                   '-vf', 'showinfo', '-fps_mode', 'passthrough', '-f', 'null', '-']
        decoded = subprocess.run(command, capture_output=True, text=True, timeout=600)
        log = args.output/('part-'+str(part['part'])+'-decode.txt')
        log.write_text(decoded.stderr)
        timebase = re.search(r'config in time_base:\s*(\d+)/(\d+)', decoded.stderr)
        frames = [(int(n), int(pts), int(w), int(h)) for n, pts, w, h in
                  re.findall(r'\bn:\s*(\d+)\s+pts:\s*(-?\d+).*?\bs:(\d+)x(\d+)\b', decoded.stderr)]
        if decoded.returncode or not timebase or not frames:
            raise ValueError('Actual recording full decode unavailable: '+str(log))
        contiguous = [f[0] for f in frames] == list(range(len(frames)))
        increasing = all(a[1] < b[1] for a, b in zip(frames, frames[1:]))
        record = {'part': part['part'], 'path': str(path.resolve()), 'sha256': digest,
                  'decodedFrames': len(frames), 'frameIndexesContiguous': contiguous,
                  'nativeInputPtsStrictlyIncreasing': increasing,
                  'inputTimeBase': timebase.group(1)+'/'+timebase.group(2),
                  'firstPts': frames[0][1], 'lastPts': frames[-1][1],
                  'actualDecodedSizes': sorted({(f[2], f[3]) for f in frames}),
                  'sourceFramesUnmodified': True, 'decodeLogSha256': sha(log)}
        result['parts'].append(record)
        (args.output/'inspection.json').write_text(json.dumps(result, indent=2)+'\n')
        if not contiguous or not increasing:
            raise ValueError('Actual native input frame sequence failed inspection')
    result['passed'] = bool(result['parts'])
    (args.output/'inspection.json').write_text(json.dumps(result, indent=2)+'\n')
    print(json.dumps({'passed': result['passed'], 'parts': len(result['parts']),
                      'decodedFrames': sum(p['decodedFrames'] for p in result['parts'])}))


if __name__ == '__main__':
    main()
