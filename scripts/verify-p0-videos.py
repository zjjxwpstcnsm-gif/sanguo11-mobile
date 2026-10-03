#!/usr/bin/env python3
"""Validate raw captures without consuming a shell loop's filenames or rewriting media."""
import json
import subprocess
import sys
from pathlib import Path


def run(command, output):
    try:
        with output.open('wb') as stream:
            code = subprocess.run(command, stdin=subprocess.DEVNULL, stdout=stream,
                                  stderr=subprocess.STDOUT, timeout=300, check=False).returncode
    except subprocess.TimeoutExpired:
        code = 124
    except OSError as exc:
        output.write_text(str(exc), encoding='utf-8')
        code = 127
    output.with_suffix(output.suffix + '.exit').write_text(str(code) + '\n')
    return code


def main():
    root = Path(sys.argv[1] if len(sys.argv) > 1 else 'out/remediation')
    root.mkdir(parents=True, exist_ok=True)
    results = []
    # Materialize filenames first. Never share their stream with ffmpeg's interactive stdin.
    for video in sorted(root.rglob('*.mp4')):
        probe = run(['ffprobe', '-v', 'error', '-show_format', '-show_streams',
                     '-of', 'json', str(video)], Path(str(video) + '.ffprobe.json'))
        decode = run(['ffmpeg', '-nostdin', '-v', 'warning', '-i', str(video),
                      '-f', 'null', '-'], Path(str(video) + '.decode.log'))
        results.append({'path': str(video.relative_to(root)), 'bytes': video.stat().st_size,
                        'ffprobe_exit': probe, 'full_decode_exit': decode})
    code = 0 if results and all(r['ffprobe_exit'] == r['full_decode_exit'] == 0 for r in results) else 1
    (root / 'VIDEO_VALIDATION.json').write_text(json.dumps({
        'status': 'PARSE_AND_DECODE_PASS' if code == 0 else 'FAIL',
        'not_a_gameplay_or_visual_acceptance': True, 'videos': results
    }, indent=2) + '\n')
    (root / 'VIDEO_EXIT.txt').write_text(str(code) + '\n')
    return code


if __name__ == '__main__':
    sys.exit(main())
