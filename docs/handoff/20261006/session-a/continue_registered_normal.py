#!/usr/bin/env python3
"""Own serial whole normal/media/audio/heap continuation after genuine39 proof."""
import argparse
import json
import os
import pathlib
import subprocess
import sys
import time
from run_picker_release_followup import command, restored
from run_remaining_normal_media import ROOT, HELPER, sha


def main():
    p=argparse.ArgumentParser();p.add_argument('--producer-pid',type=int,required=True)
    p.add_argument('--legacy-queue',type=pathlib.Path,required=True)
    p.add_argument('--output',type=pathlib.Path,required=True);a=p.parse_args()
    queue=a.legacy_queue.resolve();out=a.output.resolve()
    assert queue.is_relative_to(ROOT/'out/session-a') and out.is_relative_to(ROOT/'out/session-a') and not out.exists()
    producer=command(a.producer_pid)
    assert producer and 'continue_legacy39_registered.py' in producer and str(a.legacy_queue) in producer
    original=json.loads((queue/'queue.json').read_text());assert original['hostPid']==a.producer_pid and not original['deviceActionsStarted']
    default=HELPER.with_name('LEGACY39_REGISTERED_TEST_BUILD176.json')
    ordinary=HELPER.with_name('CURRENT_REGISTERED_ORDINARY_BUILD180.json')
    db=json.loads(default.read_text());ob=json.loads(ordinary.read_text())
    assert db['buildSuccessful'] and db['gameLargeHeap'] and ob['buildSuccessful'] and not ob['gameLargeHeap']
    assert original['apks']=={r['path']:r['sha256'] for r in db['apks']}
    assert all(sha(pathlib.Path(r['path']))==r['sha256'] for b in [db,ob] for r in b['apks'])
    out.mkdir(parents=True);report=dict(hostPid=os.getpid(),stage='waiting-for-genuine39-owner',producerPid=a.producer_pid,
        producerCommand=producer,legacyQueue=str(queue),deviceActionsStarted=False,childJobs=[],wholeGoalComplete=False,
        scope='Exact176 whole normal command/all16 caller/menu track gate and fresh180 ordinary/default heap stress. Child jobs serially gate full restored prior proof; no fabricated world or previous pair scores.')
    def save():(out/'queue.json').write_text(json.dumps(report,indent=2)+'\n')
    def spawn(name,args):
        log=(out/(name+'-driver.log')).open('w')
        proc=subprocess.Popen(args,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT)
        report['childJobs'].append(dict(name=name,pid=proc.pid,command=args));save();return proc
    def await_report(proc,path):
        deadline=time.monotonic()+60
        while not path.is_file():
            assert proc.poll() is None,'Child ended before own report; inspect driver log'
            if time.monotonic()>deadline:raise TimeoutError('Child report not observed; not restarted')
            time.sleep(.2)
        return json.loads(path.read_text())
    save()
    try:
        deadline=time.monotonic()+12*3600
        while True:
            current=command(a.producer_pid)
            if current is None:break
            assert current==producer,'PID reused; no device action'
            if time.monotonic()>deadline:raise TimeoutError('Live genuine39 observation timeout, not terminal proof')
            time.sleep(10)
        legacy=json.loads((queue/'queue.json').read_text())
        assert legacy['apks']==original['apks'] and legacy['stage']=='genuine39-normal-cold-fullrestore-accepted' and legacy['genuine39Accepted']
        actual=pathlib.Path(legacy['activeSession']);restored(actual,original['apks'])
        normal=out/'normal';audio=out/'audio';heap=out/'heap'
        report.update(stage='starting-current-normal',genuine39Accepted=True,deviceActionsStarted=True);save()
        normal_job=spawn('normal',[sys.executable,str(HELPER.with_name('run_picker_release_followup.py')),
            '--completed-legacy',str(actual),'--cohort-receipt',str(default),'--output',str(normal)])
        nr=await_report(normal_job,normal/'batch.json');assert nr['hostPid']==normal_job.pid
        audio_python='/Users/paopao/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3'
        audio_job=spawn('audio',[audio_python,str(HELPER.with_name('continue_latest_menu_audio.py')),
            '--producer-pid',str(normal_job.pid),'--normal-batch',str(normal),'--cohort-receipt',str(default),'--output',str(audio)])
        ar=await_report(audio_job,audio/'queue.json');assert ar['hostPid']==audio_job.pid and not ar['deviceActionsStarted']
        heap_job=spawn('heap',[sys.executable,str(HELPER.with_name('run_current_heap_regression.py')),
            '--producer-pid',str(audio_job.pid),'--audio-queue',str(audio),'--normal-batch',str(normal),
            '--ordinary-receipt',ordinary.name,'--default-receipt',default.name,'--output',str(heap)])
        hr=await_report(heap_job,heap/'batch.json');assert hr['hostPid']==heap_job.pid and not hr['deviceActionsStarted']
        report.update(stage='children-running-with-serial-gates',normalBatch=str(normal),audioQueue=str(audio),heapBatch=str(heap));save()
        statuses={}
        for name,proc in [('normal',normal_job),('audio',audio_job),('heap',heap_job)]:statuses[name]=proc.wait()
        report.update(childExitCodes=statuses,stage='children-terminal');save()
        assert all(code==0 for code in statuses.values()),'One or more actual stages failed; retain each report and restoration scope'
        report.update(stage='current-normal-menu-and-both-heaps-complete');save()
    except BaseException as error:
        report.update(stage='stopped-retain-actual-evidence',error=repr(error),needsActualRestorationInspection=report['deviceActionsStarted']);save();raise


if __name__=='__main__':main()
