#!/usr/bin/env python3
"""Run complete original debate frames with unsigned-byte IQ/war fixtures.

Explicit synthetic input actors, not historical edits. Uses the same original
constructors, deck, AI, damage, frame and RNG functions as the prior flow audit.
"""
import argparse,gzip,json,itertools
from pathlib import Path
from inspect_pc_debate_flow import NativeDebateFlow
from audit_pc_restoration_sources import ROOT,EXE_SHA,output_guard,json_bytes,sha

def inspect(installation,output):
    installation=installation.resolve();output_guard(installation,output)
    if output.exists():raise ValueError('Preserve earlier evidence; choose a new output')
    exe=(installation/'san11pk.exe').read_bytes();cases=[]
    inputs=[([90,82],[0,0]),([101,82],[255,0]),([255,100],[0,255]),([255,255],[255,255]),([0,255],[255,0]),([200,200],[101,200])]
    for (iq,war),temper in itertools.product(inputs,[(0,3),(1,2),(2,1),(3,0)]):
        d=NativeDebateFlow(installation,exe);result=d.run(iq,list(temper),[31,31],23,frame_limit=4000,war_values=war,extended_fixture=True)
        result['currentWar']=war;cases.append(result)
        print(json.dumps(dict(case=len(cases)-1,iq=iq,war=war,temper=temper,frames=result['frameCount'],seed=result['finalNativeRng'])),flush=True)
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,toolSha256=sha(Path(__file__).read_bytes()),flowToolSha256=sha((ROOT/'tools/content/inspect_pc_debate_flow.py').read_bytes()),cases=cases,
        limits=['Explicit original actor0/1 IQ0..255/war0..255 fixture bytes, not historical source changes',
                'Complete original model and AI/RNG execute to phase9; optional presentation remains exact PE defaults',
                'Null-UI fixture model only; production headed input/campaign rewards/installed APK need separate evidence'],completeGoal=False)
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0));print(json.dumps(dict(cases=len(cases),sha256=sha(output.read_bytes()))),flush=True)
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
