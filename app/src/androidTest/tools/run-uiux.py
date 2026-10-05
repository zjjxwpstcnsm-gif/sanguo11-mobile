"""Record UI tests on the isolated UIUX AVD; recover the campaign via UI on failure.

Run from the UI checkout: python3 app/src/androidTest/tools/run-uiux.py RUN SUITE BASE [TIMEOUT_SECONDS]
Requires the installed UiUxInstrumentation and its known, dedicated manual3 fixture.
Never targets a user AVD, clears app data, or overwrites a saved slot.
"""
import hashlib
import pathlib
import subprocess
import sys
import time
import re
import threading
import json

run, suite, base = sys.argv[1:4]
test_timeout = int(sys.argv[4]) if len(sys.argv) == 5 else 170
if len(sys.argv) not in (4, 5) or not 30 <= test_timeout <= 900:
    raise SystemExit('Timeout must be between 30 and 900 seconds')
if not all(re.fullmatch(r'[a-zA-Z0-9_-]+', value) for value in (run, suite)):
    raise SystemExit('Run and suite must be simple identifiers')
dest = pathlib.Path(base) / run
dest.mkdir(parents=True, exist_ok=False)
adb = ['out/toolchain/android-sdk/platform-tools/adb', '-s', 'emulator-5580']
package = 'game.sanguo.mobile.dev'
baseline = '02ddb3d44d98fbebe763a82551b5cb5eb68da6e70b6087568c0a73943bde5d69'
private = '/data/user/0/' + package + '/files/'

def command(*args, **kw):
    return subprocess.run(adb + list(args), check=True, **kw)

avd = command('emu', 'avd', 'name', capture_output=True).stdout.decode().splitlines()
if not avd or avd[0].strip() != 'san11-uiux':
    raise SystemExit('Refusing test: emulator-5580 is not the isolated UIUX AVD')

for name in ('auto.sg11', 'manual3.sg11'):
    data = command('exec-out', 'cat', private + name, capture_output=True).stdout
    (dest / ('before-' + name)).write_bytes(data)
    if hashlib.sha256(data).hexdigest() != baseline:
        raise SystemExit('Refusing test: dedicated campaign or recovery slot differs from baseline')

def instrument(which, label, timeout):
    result = command('shell', 'am', 'instrument', '-w', '-e', 'suite', which,
                     '-e', 'run', label, '-e', 'reuseSlotSha', baseline,
                     package + '.test/game.sanguo.mobile.UiUxInstrumentation',
                     capture_output=True, timeout=timeout)
    return result.stdout + result.stderr

remote = '/sdcard/uiux-' + suite + '-' + run
screens_remote = '/sdcard/Android/data/' + package + '/files/uiux/' + suite + '-' + run
# A new local directory does not imply a new device directory across batches.
for evidence_path in (remote + '.mp4', screens_remote):
    exists = subprocess.run(adb + ['shell', 'test', '-e', evidence_path], capture_output=True)
    if exists.returncode == 0:
        raise SystemExit('Refusing to reuse device evidence; choose a globally unique RUN: ' + evidence_path)
    if exists.returncode != 1:
        raise SystemExit('Could not check device evidence path: ' + evidence_path)
passed = False
stopped = threading.Event()
started = threading.Event()
recordings, recorder_errors = [], []
recording_times = []
recording_start = time.monotonic()

def record_segments():
    try:
        while not stopped.is_set():
            suffix = '' if not recordings else '-part%02d' % (len(recordings) + 1)
            part = remote + suffix
            recordings.append((part, suffix))
            timing = {'file': 'interaction' + suffix + '.mp4', 'start_seconds': time.monotonic() - recording_start}
            recording_times.append(timing)
            with (dest / ('recorder' + suffix + '.log')).open('wb') as log:
                recorder = subprocess.Popen(adb + ['shell', 'echo $$ > ' + part + '.pid; exec screenrecord --time-limit 180 ' + part + '.mp4'], stdout=log, stderr=subprocess.STDOUT)
                started.set()
                while recorder.poll() is None and not stopped.wait(.25):
                    pass
                if recorder.poll() is None:
                    pid = command('shell', 'cat', part + '.pid', capture_output=True, timeout=10).stdout.decode().strip()
                    if pid.isdigit():
                        subprocess.run(adb + ['shell', 'kill', '-2', pid], check=False)
                    try:
                        recorder.wait(timeout=25)
                    except subprocess.TimeoutExpired:
                        recorder.kill()
                        recorder.wait()
                        recorder_errors.append('Recorder timed out after SIGINT: ' + part)
                elif recorder.returncode != 0:
                    raise RuntimeError('Recorder exited with error: ' + part)
                timing['end_seconds'] = time.monotonic() - recording_start
    except Exception as error:
        recorder_errors.append(str(error))
        started.set()

record_thread = threading.Thread(target=record_segments, daemon=True)
record_thread.start()
started.wait(10)
time.sleep(.7)
try:
    if recorder_errors or not recordings:
        raise RuntimeError('Recorder did not start')
    output = instrument(suite, suite + '-' + run, test_timeout)
    (dest / 'interaction.txt').write_bytes(output)
    passed = b'FAIL ' not in output and b'UIUX PASS ' in output
except (subprocess.TimeoutExpired, subprocess.CalledProcessError, RuntimeError) as error:
    (dest / 'interaction.txt').write_text(str(error))
finally:
    stopped.set()
    record_thread.join(35)
    if record_thread.is_alive():
        recorder_errors.append('Recorder cleanup is still running')
    if recorder_errors:
        (dest / 'recorder-errors.txt').write_text('\n'.join(recorder_errors))
        passed = False
    (dest / 'recordings.json').write_text(json.dumps(recording_times, indent=2))

try:
    for part, suffix in recordings:
        command('pull', part + '.mp4', str(dest / ('interaction' + suffix + '.mp4')))
    command('pull', screens_remote, str(dest / 'screens'))
finally:
    if not passed:
        # Evidence transport failure must not prevent campaign recovery.
        command('shell', 'am', 'force-stop', package)
        try:
            recovery = instrument('restore', 'restore-' + run, 80)
            (dest / 'recovery.txt').write_bytes(recovery)
            if b'UIUX PASS ' not in recovery or b'FAIL ' in recovery:
                command('shell', 'am', 'force-stop', package)
        except (subprocess.TimeoutExpired, subprocess.CalledProcessError) as error:
            # Killing the host adb client does not terminate device instrumentation.
            # End that expired recovery before checking files, without rewriting saves.
            (dest / 'recovery-error.txt').write_text(str(error))
            command('shell', 'am', 'force-stop', package)
        finally:
            recovery_remote = '/sdcard/Android/data/' + package + '/files/uiux/restore-' + run
            result = subprocess.run(adb + ['pull', recovery_remote, str(dest / 'recovery-screens')],
                                    capture_output=True)
            (dest / 'recovery-evidence-transfer.txt').write_bytes(result.stdout + result.stderr)

for name in ('auto.sg11', 'manual3.sg11'):
    data = command('exec-out', 'cat', private + name, capture_output=True).stdout
    (dest / ('after-' + name)).write_bytes(data)
    if hashlib.sha256(data).hexdigest() != baseline:
        raise SystemExit('Campaign recovery is not verified: ' + name)
print((dest / 'interaction.txt').read_text())
sys.exit(0 if passed else 1)
