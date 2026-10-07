#!/usr/bin/env python3
"""Diagnostic SIGQUIT only after a fresh normal-flow foreground120s screenshot.

Exclusive A/5554 session and exact target process checked at each sample.
SIGQUIT pauses ART to dump stacks: never count this run as unperturbed timing.
No rules, World, RNG, save, heap dump, forced GC, or B source changes.
"""
import argparse
import hashlib
import json
import pathlib
import shlex
import subprocess
import time

ADB = '/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk/platform-tools/adb'
PACKAGE = 'game.sanguo.mobile.dev'
ROOT = pathlib.Path(__file__).resolve().parents[4]
LOCK = pathlib.Path('/tmp/sanguo11-emulator-5554-session-a.lock/owner.json')


def adb(*args, timeout=30):
    return subprocess.check_output([ADB, '-s', 'emulator-5554', *args], timeout=timeout)


def shell(command):
    return adb('shell', command).decode(errors='replace').strip()


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--session', type=pathlib.Path, required=True)
    parser.add_argument('--output', type=pathlib.Path, required=True)
    parser.add_argument('--watch', required=True, help='Exact actual fieldworks evidence directory on5554')
    args = parser.parse_args()
    session = args.session.resolve()
    assert args.watch == '/sdcard/Android/data/'+PACKAGE+'/files/session-b/fieldworks'
    owner = json.loads(LOCK.read_text())
    assert owner['root'] == str(ROOT) and owner['output'] == str(session.parent)
    args.output.mkdir(parents=True, exist_ok=False)
    started = time.time()
    report = {'scope': 'Actual normal foreground120s screenshot-triggered ART diagnostic; SIGQUIT changes timing; no unperturbed FPS/CPU or original rule cause claim',
              'session': str(session), 'startedUnix': started, 'captures': [], 'trigger': None}

    def state():
        current = json.loads(LOCK.read_text())
        assert current['root'] == str(ROOT) and current['output'] == str(session.parent)
        return json.loads(session.read_text())['stage']

    def own_process():
        pid = shell('pidof '+PACKAGE)
        if not pid.isdigit():
            raise RuntimeError('No single actual target PID')
        command = shell('cat /proc/'+pid+'/cmdline').replace('\0', ' ').strip()
        if command != PACKAGE:
            raise RuntimeError('Target executable changed: '+command)
        return pid

    try:
        while time.time()-started < 3600:
            if state() == 'restored-verified':
                break
            # Only the frozen normal helper creates this marker after its actual
            # foreground120s wait. Old markers and labels are not measurements.
            entries = shell('find '+shlex.quote(args.watch)+" -maxdepth 1 -name '*-foreground120s.png' -type f -exec stat -c '%Y %n' '{}' '+' 2>/dev/null || true")
            fresh = [(int(row.split(' ', 1)[0]), row.split(' ', 1)[1])
                     for row in entries.splitlines() if row.split(' ', 1)[0].isdigit()
                     and int(row.split(' ', 1)[0]) >= started]
            if not fresh:
                time.sleep(2)
                continue
            timestamp, marker = sorted(fresh)[0]
            report['trigger'] = {'devicePath': marker, 'modifiedUnix': timestamp,
                                 'systemHomeVerifiedByMarker': False, 'copyAttempts': []}
            marker_file = args.output/'actual-foreground120s.png'
            copied = False
            copy_deadline = time.monotonic()+30
            while time.monotonic() < copy_deadline:
                if state() == 'restored-verified':
                    break
                before_hash = shell('sha256sum '+shlex.quote(marker)).split()[0]
                raw = adb('exec-out', 'cat', marker)
                after_hash = shell('sha256sum '+shlex.quote(marker)).split()[0]
                host_hash = hashlib.sha256(raw).hexdigest()
                png_complete = raw.startswith(b'\x89PNG\r\n\x1a\n') and raw.endswith(b'\x00\x00\x00\x00IEND\xaeB\x60\x82')
                report['trigger']['copyAttempts'].append({'beforeSha256': before_hash,
                    'afterSha256': after_hash, 'hostSha256': host_hash,
                    'bytes': len(raw), 'completePngEnd': png_complete})
                if png_complete and before_hash == after_hash == host_hash:
                    marker_file.write_bytes(raw)
                    report['trigger']['sha256'] = host_hash
                    report['trigger']['completePngHostDeviceShaEqual'] = True
                    copied = True
                    break
                time.sleep(1)
            if not copied:
                raise ValueError('Fresh marker did not finish a complete stable PNG copy before diagnostic; no SIGQUIT sent')
            pid = own_process()
            for index in range(3):
                if state() == 'restored-verified' or own_process() != pid:
                    break
                before = shell("find /data/anr -maxdepth 1 -type f -exec stat -c '%Y %n' '{}' '+' 2>/dev/null || true")
                sample = args.output/('sample-'+str(index+1))
                sample.mkdir()
                for name, command in [('process-stat', 'cat /proc/'+pid+'/stat'),
                        ('thread-stat', "for f in /proc/"+pid+"/task/*/stat; do cat \"$f\"; done"),
                        ('process-status', 'cat /proc/'+pid+'/status'),
                        ('cpuinfo', 'dumpsys cpuinfo')]:
                    (sample/(name+'.txt')).write_text(shell(command)+'\n')
                captured = time.time()
                shell('kill -3 '+pid)
                time.sleep(3)
                record = {'pid': pid, 'signal': 'SIGQUIT', 'unix': captured,
                          'sameTargetPidAfter': own_process() == pid, 'artTraceFiles': []}
                after = shell("find /data/anr -maxdepth 1 -type f -exec stat -c '%Y %n' '{}' '+' 2>/dev/null || true")
                for line in after.splitlines():
                    if line in before.splitlines():
                        continue
                    stamp, remote = line.split(' ', 1)
                    if not stamp.isdigit() or int(stamp) < captured-1:
                        continue
                    # Preserve actual Android trace byte-for-byte. Other device
                    # processes may also create traces; inspect PID before attribution.
                    target = sample/pathlib.PurePosixPath(remote).name
                    target.write_bytes(adb('exec-out', 'cat', remote))
                    record['artTraceFiles'].append({'devicePath': remote, 'hostPath': str(target.resolve()),
                                                   'sha256': digest(target), 'bytes': target.stat().st_size})
                (sample/'logcat.txt').write_bytes(adb('logcat', '-d', '-t', '1200', '-v', 'threadtime', '--pid='+pid))
                report['captures'].append(record)
                (args.output/'report.json').write_text(json.dumps(report, indent=2)+'\n')
                time.sleep(2)
            break
    except Exception as error:
        report['observationError'] = type(error).__name__+': '+repr(error)
    report['endedUnix'] = time.time()
    report['actualTriggerObserved'] = report['trigger'] is not None
    report['causeProven'] = False
    (args.output/'report.json').write_text(json.dumps(report, indent=2)+'\n')
    print(json.dumps({'actualTriggerObserved': report['actualTriggerObserved'], 'captures': len(report['captures']),
                      'observationError': report.get('observationError'), 'causeProven': False}))


if __name__ == '__main__':
    main()
