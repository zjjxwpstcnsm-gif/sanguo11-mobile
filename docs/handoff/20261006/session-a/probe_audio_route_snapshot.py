#!/usr/bin/env python3
"""Read-only exact current cohort preflight; never start projection/capture."""
import argparse
import json
import pathlib
import shlex
import time
import audio_capture_support as capture
import device_session as ds


def main():
    p=argparse.ArgumentParser()
    p.add_argument('--session',type=pathlib.Path,required=True)
    p.add_argument('--output',type=pathlib.Path,required=True)
    p.add_argument('--receipt',type=pathlib.Path,required=True)
    a=p.parse_args()
    session=a.session.resolve(); state=json.loads((session/'session.json').read_text())
    assert state['root']==str(ds.ROOT) and state['serial']=='emulator-5554'
    assert state['stage']=='installed-verified'
    assert a.output.resolve().is_relative_to(ds.ROOT/'out/session-a') and not a.output.exists()
    assert a.receipt.parent.resolve()==pathlib.Path(__file__).resolve().parent and not a.receipt.exists()
    for package,filename in ((ds.PACKAGE,'app-debug.apk'),(capture.TEST,'app-debug-androidTest.apk')):
        path,digest=next((path,digest) for path,digest in state['apks'].items() if pathlib.Path(path).name==filename)
        assert ds.digest(pathlib.Path(path))==digest
        remote=capture.shell('pm path '+package).removeprefix('package:')
        assert capture.shell('sha256sum '+shlex.quote(remote)).split()[0]==digest
    a.output.mkdir(parents=True)
    record={'run':'session_a_route_preflight106'}
    sample=capture.observe_input_route(a.output,state,record,'preflight-no-capture')
    assert not any(row['unavailable'] for row in record['inputRouteObservations'])
    assert all(sample[name]['text'].strip() for name in ('audioPolicy','audioFlinger','mediaProjection','recordAppOp','deviceUptime'))
    mixer={'run':'session_a_mixer_preflight107','seconds':1}
    capture.begin_mixer_measurement(a.output,mixer)
    deadline=time.monotonic()+20
    path=pathlib.Path(mixer['mixerMeasurementPath'])
    while time.monotonic()<deadline:
        if path.exists() and path.stat().st_size:break
        time.sleep(.1)
    capture.end_mixer_measurement(mixer)
    rows=[json.loads(line) for line in path.read_text().splitlines()]
    assert rows and all('unavailable' not in row and row['audioFlinger'].strip() for row in rows)
    assert mixer['mixerObserverJoined'] and ds.digest(path)==mixer['mixerMeasurementSha256']
    result=dict(scope='Actual exclusive5554 current APK pair readback and read-only route observer preflight. No permission/AppOp, playback, projection, input creation, UI or saves changed.',
        apks=state['apks'], routeObservations=record['inputRouteObservations'],
        captureStarted=False, inputCreationFailureReproduced=False, oldMinus22CauseClosed=False,
        futureCaptureHooksDynamicallyAccepted=False, wholeGoalComplete=False,
        supportToolSha256=ds.digest(pathlib.Path(capture.__file__)),
        sourceSession=str(session), actualSnapshotCommandsPassed=True,
        actualReadOnlyMixerObserverJoined=True,mixerObservation=mixer,mixerSampleCount=len(rows))
    a.receipt.write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(result))


if __name__=='__main__':main()
