#!/usr/bin/env python3
"""Compare actual original packet bytes and reject malformed visual admission."""
from pathlib import Path
import json,subprocess,struct,os,time,hashlib
from probe_admitted_fire_budget246 import semantic
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';OUT=ROOT/'out/session-a/instruction-boundary259'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 assert not OUT.exists();OUT.mkdir(parents=True)
 worker=ROOT/'out/session-a/instruction-budget257/host-probe';kernel=ROOT/'app/src/main/assets/3d/pc-effects/source-kernel.bin';scene=ROOT/'app/src/main/assets/3d/pc-effects/source-fire-scene.bin';guard={str(p):sha(p) for p in [worker,kernel,scene]};env=os.environ.copy();env['PC_VM_PROBE_BLOCK_BUDGET']='1';rows=[]
 cases=[(f'cells-{n:03d}',ROOT/f'out/session-a/fire-capacity-matrix236/cells-{n:03d}/commands.bin',ROOT/f'out/session-a/admitted-fire-budget246/cells-{n:03d}/stdout.bin') for n in [1,13,32,64,128]]
 cases += [(n,ROOT/f'out/session-a/fire-budget-lifecycle249/{n}/commands.bin',None) for n in ['over128','duplicate','invalid-height','invalid-coordinate']]
 for name,payload,expected in cases:
  case=OUT/name;case.mkdir();raw=payload.read_bytes();center=struct.unpack_from('<3f',raw,12);begin=time.monotonic();r=subprocess.run([str(worker),str(kernel),str(scene),*[str(v) for v in center],'--stream'],input=raw,capture_output=True,env=env,timeout=120);(case/'stdout.bin').write_bytes(r.stdout);(case/'stderr.txt').write_bytes(r.stderr);data,frames=semantic(r.stdout);row={'case':name,'exit':r.returncode,'frames':len(frames),'wallSeconds':time.monotonic()-begin,'stdoutSha256':sha(case/'stdout.bin'),'stderrSha256':sha(case/'stderr.txt')}
  if expected is not None:
   old,_=semantic(expected.read_bytes());assert r.returncode==0 and len(frames)==71 and data==old;row['allOriginalRecordsAndClockExact246']=True
  else:assert r.returncode!=0 and not frames;row['invalidAdmissionRejectedBeforeFrame']=True
  rows.append(row);print(json.dumps(row),flush=True)
 for p,h in guard.items():assert sha(Path(p))==h
 (DOC/'INSTRUCTION_BOUNDARY259.json').write_text(json.dumps({'cases':rows,'sourceGuardsBeforeAfter':guard,'scope':'Host257 actual instruction budget only; same original packet bytes/time vs246 all five71-frame cases; malformed129/duplicate/height/coordinate rejected before output. No JNI/APK/Android/normal path/ARM acceptance.','wholeGoalComplete':False},indent=2)+'\n')
if __name__=='__main__':main()
