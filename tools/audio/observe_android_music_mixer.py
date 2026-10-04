#!/usr/bin/env python3
"""Read-only mixer observations during one exclusive normal-menu acceptance run.

Requires the existing install/restore wrapper's5582 lock and exact installed
APK pins. Does not install, launch, stop, broadcast, alter audio, or record PCM.
Observations can locate system counters; they do not attribute missed samples.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time

ROOT = Path(__file__).resolve().parents[2]


def observe(flow, output, wave):
    if output.exists():
        raise ValueError('Fresh observation output required')
    lock = Path('/tmp/codex-sanguo-media-emulator-5582.lock')
    owner = json.loads(lock.read_text())
    if owner['serial'] != 'emulator-5582' or Path(owner['workspace']).resolve() != ROOT:
        raise ValueError('Other device owner; no observation attempted')
    os.kill(owner['pid'], 0)
    report = json.loads((flow / 'results.json').read_text())
    if report['serial'] != 'emulator-5582' or report['runner'] != 'UiUxInstrumentation' or not {'suite=audio', 'menuMusic=1'}.issubset(report['arguments']):
        raise ValueError('Only the declared normal-menu run is observable')
    if report['stage'] != 'instrumentation':
        raise ValueError('Installed readback and instrumentation stage required')
    for key in ['installed_sha256', 'installed_test_sha256']:
        if report[key] not in report['apks'].values():
            raise ValueError('Installed APK not pinned')
    processes = subprocess.check_output(['ps', '-axo', 'pid,command']).decode().splitlines()
    emulators = [line.strip().split(None, 1) for line in processes
                 if 'qemu-system' in line and ' -port 5582 ' in line]
    if len(emulators) != 1 or str(wave.resolve()) not in subprocess.check_output(['lsof', '-p', emulators[0][0]]).decode():
        raise ValueError('Source WAV must belong to5582; other recorder untouched')
    recorder_identity = (wave.stat().st_dev, wave.stat().st_ino)
    output.mkdir(parents=True)
    adb = [str(ROOT / 'out/toolchain/android-sdk/platform-tools/adb'), '-s', 'emulator-5582']
    rows = []
    began = time.monotonic()
    for index in range(40):
        if not lock.exists() or json.loads(lock.read_text()) != owner:
            break
        current = json.loads((flow / 'results.json').read_text())
        if current['stage'] != 'instrumentation':
            break
        started = time.monotonic()
        if (wave.stat().st_dev, wave.stat().st_ino) != recorder_identity:
            raise ValueError('Recorder file replaced; observation stopped')
        cursor = wave.stat().st_size
        raw = subprocess.check_output(adb + ['shell', 'dumpsys', 'media.audio_flinger'], timeout=20)
        name = 'audio-flinger-%02d.txt' % index
        (output / name).write_bytes(raw)
        rows.append(dict(file=name, elapsedSeconds=started - began,
                         sourceWavByteCursor=cursor,
                         queryMillis=(time.monotonic() - started) * 1000,
                         sha256=hashlib.sha256(raw).hexdigest()))
        time.sleep(5)
    evidence = dict(scope='Read-only system mixer snapshots; observational overhead retained, no player/mixer/recorder responsibility assertion',
                    serial='emulator-5582', owner=owner, flow=str(flow.resolve()),
                    mainApkSha256=report['installed_sha256'], testApkSha256=report['installed_test_sha256'], rows=rows)
    evidence['recorder'] = dict(emulatorPid=emulators[0][0], source=str(wave.resolve()), identity=recorder_identity)
    (output / 'observations.json').write_text(json.dumps(evidence, indent=2) + '\n')
    print(json.dumps(dict(status='MEASURED_DIAGNOSTIC', snapshots=len(rows))))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--flow', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--wave', type=Path, required=True)
    args = parser.parse_args()
    observe(args.flow, args.output, args.wave)
