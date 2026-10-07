#!/usr/bin/env python3
"""Freeze a completed raw-video prefix and decode all original rational PTS."""
import argparse
import hashlib
import json
import pathlib
import runpy
import subprocess

ROOT = pathlib.Path(__file__).resolve().parents[4]
OWN = pathlib.Path(__file__).resolve().parent
ADB = '/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk/platform-tools/adb'


def sha(path):
    value = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1048576), b''):
            value.update(block)
    return value.hexdigest()


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--session', type=pathlib.Path, required=True)
    p.add_argument('--video-index', type=pathlib.Path, required=True)
    p.add_argument('--parts', type=int, required=True)
    p.add_argument('--output', type=pathlib.Path, required=True)
    args = p.parse_args()
    session = args.session.resolve()
    state = json.loads(session.read_text())
    assert state['root'] == str(ROOT) and state['serial'] == 'emulator-5554'
    assert state['stage'] in ('installed-verified', 'restored-verified')
    assert all(sha(pathlib.Path(path)) == digest for path, digest in state['apks'].items())
    raw_index = args.video_index.read_bytes()
    index = json.loads(raw_index)
    assert index['session'] == str(session)
    assert 1 <= args.parts <= len(index['parts'])
    parts = index['parts'][:args.parts]
    assert [row['part'] for row in parts] == list(range(1, args.parts+1))
    out = args.output.resolve()
    out.mkdir(parents=True, exist_ok=False)
    (out/'original-video-index.json').write_bytes(raw_index)
    scanner = out/'verify-recorded-samples'
    with (out/'compile.log').open('w') as log:
        subprocess.run(['swiftc', '-O', str(OWN/'verify_recorded_samples.swift'), '-o', str(scanner)],
                       stdout=log, stderr=subprocess.STDOUT, check=True)
    records = []
    # Read the preserved container parser without executing its CLI or edits.
    # Its historical default purpose text is not evidence for this recording.
    inspect = runpy.run_path(str(ROOT/'tools/content/inspect_mp4_evidence.py'))['inspect']
    report = dict(session=str(session), sessionStageAtSnapshot=state['stage'], apks=state['apks'],
                  frozenIndexSha256=hashlib.sha256(raw_index).hexdigest(), requestedParts=args.parts,
                  completeRequestedPrefix=False, records=records, wholeGoalComplete=False,
                  scope='Only this exact APK cohort completed raw recording prefix. Whole host SHA reread; retained device parts reread now, removed own parts use recorded matching pre/post-pull SHA with current-device reread unavailable. Every decoded input sample and original rational PTS. Recording may perturb performance; segment gaps/start omission retained. No normal/cold/fullrestore completion, PC pixel/crop/timing, ARM or newer APK acceptance.')
    for row in parts:
        path = pathlib.Path(row['path'])
        assert 'error' not in row and sha(path) == row['sha256'] == row['deviceSha256']
        device = row['devicePath']
        assert device.startswith(index['deviceCaptureDirectory']+'/') and device.endswith('.mp4')
        device_now = 'whole-device-sha-reread'
        if row.get('verifiedDevicePartRemovedAfterPull'):
            assert row['deviceSha256AfterPull'] == row['sha256']
            device_now = 'own-device-part-removed-after-matching-pre/post-pull-sha; current-device-reread-unavailable'
        else:
            current = subprocess.check_output([ADB, '-s', 'emulator-5554', 'shell', 'sha256sum', device], text=True, timeout=40).split()[0]
            assert current == row['sha256']
        samples = out/('part-'+str(row['part']).zfill(2)+'-samples.json')
        subprocess.run([str(scanner), str(path), str(samples)], check=True)
        decoded = json.loads(samples.read_text())
        assert decoded['allSamplesDecoded'] and decoded['timestampsStrictlyIncreasing']
        container = inspect(path)
        encoded = [entry['samples'] for entry in container['boxes'] if 'samples' in entry]
        assert len(encoded) == 1 and encoded[0] == decoded['decodedFrames']
        assert sha(path) == row['sha256']
        records.append(dict(part=row['part'], video=str(path), sha256=row['sha256'],
                            actualBeginUnix=row['beginUnix'], actualEndUnix=row['endUnix'],
                            deviceVerification=device_now,
                            decodedFrames=decoded['decodedFrames'], trackTimescale=decoded['originalTrackTimescale'],
                            encodedSamples=encoded[0], encodedDecodedCountsExact=True,
                            samples=str(samples), samplesSha256=sha(samples)))
        (out/'report.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    report.update(completeRequestedPrefix=True, decodedFrames=sum(row['decodedFrames'] for row in records),
                  segmentHostGapsSeconds=[b['actualBeginUnix']-a['actualEndUnix'] for a,b in zip(records, records[1:])])
    (out/'report.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps({key: report[key] for key in ('requestedParts', 'completeRequestedPrefix', 'decodedFrames', 'segmentHostGapsSeconds')}))


if __name__ == '__main__':
    main()
