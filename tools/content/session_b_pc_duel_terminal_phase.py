#!/usr/bin/env python3
"""Untouched original terminal predicates/frame/manager, declared source teams."""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,corpus,output,boundary=False):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier receipt')
 raw=corpus.read_bytes();assert sha(raw)=='2bb4e40c692c1f0532fe31ebde2364d13c690c96aaf966f8f12248f9c368d426';r=json.loads(raw);d,w,src,geo,people=prepare(installation);assert src==r['source']and people==r['people']
 protection=[bool(w.call(0x4890f0,0x20,receiver=p['pointer']))for p in people]
 relations=[[bool(w.call(0x48d720,q['nativeId'],receiver=p['pointer'])or w.call(0x48bc10,q['nativeId'],receiver=p['pointer'])or w.call(0x4887d0,q['nativeId'],receiver=p['pointer']))for q in people]for p in people]
 settingsPointer=0x7201958;settingValid=bool(w.call(0x47a630,settingsPointer));setting=struct.unpack('<i',w.u.mem_read(0x720197c,4))[0]
 baseline=bytes(w.u.mem_read(0x7200000,0x300000));rows=[]
 for number,case in enumerate(r['cases']):
  for orientation in [0,1]:
   for winner in [-1,0,1]:
    for move in [-1,3]:
     for seed,declaredSetting in [(seed,settingValue)for seed in [0,23,42,0xffffffff]for settingValue in ([0,1,2]if boundary else[setting])]:
      b=bytearray.fromhex(case['trace'][-1]['modelHex']);struct.pack_into('<3i',b,4,12,-1,0);struct.pack_into('<i',b,0x1c,orientation);struct.pack_into('<2i',b,0x588,winner,1-winner if winner>=0 else -1);struct.pack_into('<i',b,0x580,move)
      w.u.mem_write(0x720197c,struct.pack('<i',declaredSetting));declaredBaseline=bytes(w.u.mem_read(0x7200000,0x300000));w.u.mem_write(d.fixture,bytes(b));w.u.mem_write(0x8b3740,bytes.fromhex(case['managerHex']));w.u.mem_write(0x8a5d44,struct.pack('<I',seed))
      for step in range(9):
       before=bytes(w.u.mem_read(d.fixture,0x59c));manager=bytes(w.u.mem_read(0x8b3740,0xd0));rngBefore=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0];value=w.call(0x505e60,1,receiver=d.fixture,count=10000000);after=bytes(w.u.mem_read(d.fixture,0x59c));managerAfter=bytes(w.u.mem_read(0x8b3740,0xd0));rngAfter=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]
       assert declaredBaseline==bytes(w.u.mem_read(0x7200000,0x300000));rows.append(dict(declaredSetting=declaredSetting,beforeHex=before.hex(),afterHex=after.hex(),managerBeforeHex=manager.hex(),managerAfterHex=managerAfter.hex(),seed=rngBefore,nativeRng=rngAfter,result=value,worldUnchanged=True))
       if value==1:break
      else:raise ValueError('Terminal never completed')
      w.u.mem_write(0x720197c,struct.pack('<i',setting));assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))
  print('PASS original terminal case',number,flush=True)
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=src,geography=geo,people=people,protection=protection,relations=relations,settingValid=settingValid,setting=setting,cases=rows,limits=['Declared six source teams, orientation/winner/move/seed fixtures; no rule or RNG replacement','Actual original fullphase12 and manager observed, sourceWorld unchanged','Normal admission, campaign XP/capture/unit callback, headed GUI and production Save/API/APK incomplete'],completeGoal=False),indent=2)+'\n');print('PASS original terminal',len(rows),'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('corpus',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--declared-settings-boundary',action='store_true');a=p.parse_args();inspect(a.installation,a.corpus,a.output,a.declared_settings_boundary)
