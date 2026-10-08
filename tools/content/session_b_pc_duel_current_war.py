#!/usr/bin/env python3
"""Original current duel WAR versus virtual ability getter with actualsource IDs.
Calendar/injury/extras flags are explicit boundary fixtures, not played years.
"""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve native prior evidence')
 d,w,src,geo,people=prepare(installation);ids=[98,144,185,395,432,515,660,116,163,222];rows=[]
 def one(native,year):
  actor=w.call(0x490b00,native,receiver=w.root);assert w.call(0x4883c0,receiver=actor)==native and w.call(0x47a600,actor)==1
  table=struct.unpack('<I',w.u.mem_read(actor,4))[0];getter=struct.unpack('<I',w.u.mem_read(table+0x4c,4))[0];w.call(0x4826e0,year,receiver=w.root);age=w.call(0x488a20,receiver=actor);option=struct.unpack('<i',w.u.mem_read(0x7201990,4))[0]
  # Warm original lazy ability cache first. Cache writes are not called pure.
  for injury in range(4):w.call(getter,1,injury,receiver=actor)
  baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4))
  for injury in range(4):
   base=w.call(getter,1,injury,receiver=actor)&255
   for extras in [0,1]:
    value=w.call(0x50c690,actor,injury,extras,count=10000000);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));rows.append(dict(nativeId=native,yearFixture=year,originalAge=age,rawRuleOption7201990=option,injuryFixture=injury,extraFlagFixture=extras,originalVirtualGetter=hex(getter),nativeAbilityLowByte=base,duelWar=value,originalWorldAfterCacheWarmAndRngUnchanged=True))
 for native in ids:one(native,src['date'][0])
 actor=w.call(0x490b00,185,receiver=w.root);w.call(0x4826e0,src['date'][0],receiver=w.root);initial_age=w.call(0x488a20,receiver=actor)
 for age in [59,60,64,65,69,70,79,80,89,90]:
  year=src['date'][0]+age-initial_age;one(185,year);assert rows[-1]['originalAge']==age
 report=dict(exeSha=EXE_SHA,source=src,geography=geo,cases=rows,limits=['Source immutable nativeIDs checked with original registry/getter/validity, no runtimeID arithmetic','Virtual currentability cached first, original50c690 executed foractualsource participants','Calendar/injury/extras fixtures do not mean campaignyears/ancient/NPC activation','Do not change globalbase/growth/XP/current from duel-specific bonus','Fullengine/normalcampaign/Save/API/APK stillrequired'],completeGoal=False)
 output.write_text(json.dumps(report,indent=2)+'\n');print('PASS original currentduelWAR',len(rows),'SHA',sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
