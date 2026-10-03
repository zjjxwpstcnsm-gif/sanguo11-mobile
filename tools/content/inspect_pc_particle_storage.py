#!/usr/bin/env python3
"""Retain exact source particle slots for original evaluator/render porting.

Allocator reuse and lifetime semantics are unresolved. This does not label raw
words as XYZ/colors, export approximation artwork, or add Android effects.
"""
import argparse
import json
from pathlib import Path
from pc_effect_machine import SourceEffectMachine
from pc_resources import Archive,sha

ROOT=Path(__file__).resolve().parents[2]


def inspect(installation,resource,steps,output):
    archive=Archive(installation/'Media/san11pkres.bin')
    try:raw=archive.read(resource)
    finally:archive.close()
    machine=SourceEffectMachine((installation/'san11pk.exe').read_bytes())
    snapshots=[]
    try:
        machine.load(raw);machine.start()
        for dt in steps:
            machine.update(dt)
            slots=machine.particle_storage()
            snapshots.append(dict(input_dt=dt,source=machine.snapshot(),allocator_slots=slots))
            print(json.dumps(dict(dt=dt,slots=len(slots),source_allocations=machine.successes)),flush=True)
    finally:machine.close()
    report=dict(schema=1,goal_complete=False,resource_id=resource,source_sha256=sha(raw),
        status='RAW_SOURCE_SLOT_REFERENCE_NOT_RENDER_ACCEPTANCE',runtime_effects_added=0,
        functions=['45cf90->45cec0 allocation','native owner+2c vtable+14 stride'],snapshots=snapshots,
        limits=['Allocation reuse/active membership and field semantics require source consumers',
                'Diagnostic source time units are not accepted PC seconds or FPS',
                'No original scene transform, GPU rasterization, Android event binding or visual acceptance'])
    output.write_text(json.dumps(report,indent=2)+'\n')


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path)
    p.add_argument('--resource',type=int,default=148);p.add_argument('--steps',type=float,nargs='+',default=[1/30,.1,.5,1,2])
    p.add_argument('--output',type=Path,default=ROOT/'out/pc-visual/v147-particle-slots-source.json')
    a=p.parse_args();inspect(a.installation,a.resource,a.steps,a.output)
