#!/usr/bin/env python3
"""Read-only actual target thread CPU and adjacent progress; no signals/GC/UI."""
import argparse
import json
import os
import pathlib
import subprocess
import time
from device_session import ROOT, ADB, PACKAGE, digest


def read(*args):
    r=subprocess.run([ADB,'-s','emulator-5554',*args],capture_output=True,text=True,timeout=15)
    if r.returncode:raise RuntimeError(r.stderr+r.stdout)
    return r.stdout


def main():
    p=argparse.ArgumentParser()
    p.add_argument('--session',type=pathlib.Path,required=True)
    p.add_argument('--output',type=pathlib.Path,required=True)
    p.add_argument('--max-seconds',type=int,default=7200)
    a=p.parse_args();session=a.session.resolve();out=a.output.resolve()
    assert session.is_relative_to(ROOT/'out/session-a') and out.is_relative_to(ROOT/'out/session-a') and not out.exists()
    first=json.loads(session.read_text());assert first['root']==str(ROOT) and first['serial']=='emulator-5554'
    assert first['stage']=='installed-verified'
    assert all(digest(pathlib.Path(path))==sha for path,sha in first['apks'].items())
    out.parent.mkdir(parents=True,exist_ok=True);began=time.monotonic()
    with out.open('x') as f:
        while time.monotonic()-began<a.max_seconds:
            state=json.loads(session.read_text())
            assert state['apks']==first['apks'] and state['root']==str(ROOT)
            if state['stage']=='restored-verified':break
            sample=dict(hostPid=os.getpid(),hostUnixStart=time.time(),apks=first['apks'],stage=state['stage'],
                scope='Sequential pid/progress/top reads, CPU snapshot not allocation/Java stack/unique turn cause. Readback can perturb scheduling; no main+child peak sums or ARM claim.')
            try:
                raw=read('shell','pidof',PACKAGE).strip();assert raw.isdigit()
                sample['targetPid']=raw
                before=read('shell','cat','/proc/'+raw+'/cmdline');assert before.split('\0')[0]==PACKAGE
                sample['progressBefore']=read('shell','tail','-12','/sdcard/Android/data/'+PACKAGE+'/files/session-b/fieldworks/progress.txt')
                sample['topThreads']=read('shell','top','-H','-b','-n','1','-p',raw)
                assert read('shell','cat','/proc/'+raw+'/cmdline')==before
                sample['sameTargetPidCommandBeforeAfter']=True
                sample['progressAfter']=read('shell','tail','-12','/sdcard/Android/data/'+PACKAGE+'/files/session-b/fieldworks/progress.txt')
            except Exception as error:sample['unavailable']=repr(error)
            sample['hostUnixEnd']=time.time();f.write(json.dumps(sample)+'\n');f.flush()
            time.sleep(5)


if __name__=='__main__':main()
