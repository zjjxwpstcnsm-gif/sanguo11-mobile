#!/usr/bin/env python3
"""Capture a controlled, already-running scenario. Requires an explicit 3D Surface layer.
Run separately for original-2d / current-2d / current-3d on the SAME phone and route.
SurfaceFlinger latency is optional evidence, not a substitute for trace FrameTimeline analysis.
"""
import argparse,json,subprocess,time,statistics
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--mode',choices=['original-2d','current-2d','current-3d'],required=True);p.add_argument('--layer',required=True);p.add_argument('--seconds',type=int,default=1200);p.add_argument('--output',type=Path,required=True);p.add_argument('--manifest',type=Path,required=True);a=p.parse_args()
manifest=json.loads(a.manifest.read_text())
for key in ['sha','apk_sha256','abi','quality','frame_cap','internal_resolution','save_sha256','seed','route','ambient_temperature_c','build_type']:
 if key not in manifest:raise SystemExit('Missing controlled input: '+key)
a.output.mkdir(parents=True,exist_ok=False)
def adb(*args,timeout=30):
 r=subprocess.run(['adb',*args],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=timeout);return r.stdout
if 'device' not in adb('get-state'):raise SystemExit('No authorized device')
for name,args in {'device':['getprop'],'gpu':['dumpsys','SurfaceFlinger'],'display':['wm','size'],'battery-before':['dumpsys','battery'],'thermal-before':['dumpsys','thermalservice'],'layers':['dumpsys','SurfaceFlinger','--list'],'installed':['dumpsys','package','game.sanguo.mobile.dev']}.items():
 (a.output/(name+'.txt')).write_text(adb('shell',*args))
layers=(a.output/'layers.txt').read_text().splitlines()
if a.layer not in layers:raise SystemExit('Select an exact layer from SurfaceFlinger --list; never silently use UI instead of 3D Surface')
(a.output/'manifest.json').write_text(json.dumps(dict(manifest,mode=a.mode,duration_seconds=a.seconds,layer=a.layer),indent=2))
trace='/data/misc/perfetto-traces/s08.perfetto-trace'
traceproc=subprocess.Popen(['adb','shell','perfetto','-o',trace,'-t',str(a.seconds)+'s','-b','64mb','sched','freq','idle','am','wm','gfx','view','binder_driver','dalvik'],stdout=open(a.output/'perfetto.log','w'),stderr=subprocess.STDOUT)
start=time.monotonic();present=set();samples=[]
with (a.output/'samples.jsonl').open('w') as out:
 while time.monotonic()-start<a.seconds:
  now=time.monotonic()-start;latency=adb('shell','dumpsys','SurfaceFlinger','--latency',a.layer)
  row=dict(elapsed=now,surface_latency_raw=latency)
  for line in latency.splitlines():
   fields=line.split()
   if len(fields)==3 and all(x.isdigit() for x in fields):
    value=int(fields[1])
    if 0<value<9223372036854775807:present.add(value)
  if len(samples)%10==0:
   row.update(memory=adb('shell','dumpsys','meminfo','game.sanguo.mobile.dev'),thermal=adb('shell','dumpsys','thermalservice'),battery=adb('shell','dumpsys','battery'))
  out.write(json.dumps(row)+'\n');out.flush();samples.append(now);time.sleep(1)
try:traceproc.wait(timeout=30)
except subprocess.TimeoutExpired:traceproc.terminate()
(a.output/'trace-pull.txt').write_text(adb('pull',trace,str(a.output/'trace.perfetto-trace'),timeout=60))
# Report raw presentation deltas only. Idle frames, dropped ring history and refresh rate
# require trace review; do not label these GPU execution durations.
stamps=sorted(present);deltas=[(y-x)/1e6 for x,y in zip(stamps,stamps[1:])]
def stats(values):
 s=sorted(values)
 return {str(p):s[min(len(s)-1,int((len(s)-1)*p))] if s else None for p in [.5,.95,.99]}
mid=len(deltas)//2
summary=dict(unique_present_timestamps=len(stamps),present_interval_ms=stats(deltas),first_half=stats(deltas[:mid]),second_half=stats(deltas[mid:]),gpu_execution_ms=None,battery_energy=None,pss_budget_mib=800,interpretation='SurfaceFlinger optional data; validate coverage and timing in Perfetto before acceptance')
(a.output/'summary.json').write_text(json.dumps(summary,indent=2))
