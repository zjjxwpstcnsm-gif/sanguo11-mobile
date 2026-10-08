#!/usr/bin/env python3
"""Long source128 stop/restart and negative admission diagnostics, no JNI."""
from pathlib import Path
import json,hashlib,subprocess,struct,os,time,re
from probe_admitted_fire_budget246 import semantic
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';OUT=ROOT/'out/session-a/fire-budget-lifecycle249'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    assert not OUT.exists();OUT.mkdir(parents=True)
    binary=ROOT/'out/session-a/admitted-fire-budget246/host-probe';kernel=ROOT/'app/src/main/assets/3d/pc-effects/source-kernel.bin';scene=ROOT/'app/src/main/assets/3d/pc-effects/source-fire-scene.bin'
    guards={str(p):sha(p) for p in [binary,kernel,scene,ROOT/'tools/content/native/session_a_cell_fire_budget_worker.c']}
    original=(ROOT/'out/session-a/fire-capacity-matrix236/cells-128/commands.bin').read_bytes();camera=original[12:216];center=struct.unpack_from('<3f',camera)
    cells=[(x,y,0.) for x in range(150,158) for y in range(80,96)]
    def command(kind,serial,dt,active=None):
        raw=struct.pack('<IIf',kind,serial,dt)+camera
        if active is not None:raw+=struct.pack('<I',len(active))+b''.join(struct.pack('<IIf',*x) for x in active)
        return raw
    env=os.environ.copy();env['PC_VM_PROBE_BLOCK_BUDGET']='1'
    cases=[('long128',[(.1,cells)]*600+[(0.,cells)]+[(.1,[])]*40+[(.1,cells)]*120+[(.1,[])]*40),
           ('over128',[(.1,cells+[(158,80,0.)])]),('duplicate',[(.1,[cells[0],cells[0]])]),
           ('invalid-height',[(.1,[(150,80,129.)])]),('invalid-coordinate',[(.1,[(200,80,0.)])])]
    rows=[]
    for label,steps in cases:
        case=OUT/label;case.mkdir();payload=command(0,0,0.)+b''.join(command(4,i+1,dt,c) for i,(dt,c) in enumerate(steps))+command(3,len(steps)+1,0.);(case/'commands.bin').write_bytes(payload);began=time.monotonic()
        r=subprocess.run([str(binary),str(kernel),str(scene),*[str(x) for x in center],'--stream'],input=payload,capture_output=True,env=env,timeout=240)
        (case/'stdout.bin').write_bytes(r.stdout);(case/'stderr.txt').write_bytes(r.stderr);records,frames=semantic(r.stdout);log=r.stderr.decode();peak=re.search(r'PC_CELL_BUDGET_PEAK bytes=(\d+) highwater=(\d+) limit=(\d+)',log)
        row={'case':label,'exit':r.returncode,'framesCompleted':len(frames),'framesRequested':len(steps),'wallSeconds':time.monotonic()-began,'maximumPackets':max([x['packets'] for x in frames],default=0),'maximumCallBytes':int(peak[1]) if peak else None,'quotaHighwater':int(peak[2]) if peak else None,'quotaBytes':int(peak[3]) if peak else None,'semanticSha256':hashlib.sha256(records).hexdigest(),'stderr':log,'stdoutSha256':sha(case/'stdout.bin'),'stderrSha256':sha(case/'stderr.txt')}
        if label=='long128':
            row['transportLifecyclePassed']=r.returncode==0 and len(frames)==len(steps) and 'totalCreated=256 totalStopped=256' in log
            row['pauseExact']=len(frames)>600 and frames[599]['elapsed']==frames[600]['elapsed'] and frames[599]['packetSha256']==frames[600]['packetSha256']
            row['maximumEvaluatorMs']={k:max([x[k] for x in frames],default=0) for k in ['updateMs','drawMs','geometryMs']}
            (case/'frames.json').write_text(json.dumps(frames,indent=2)+'\n');row['framesSha256']=sha(case/'frames.json')
        else:row['invalidAdmissionRejectedBeforeAnyFrame']=r.returncode!=0 and not frames
        rows.append(row);print(json.dumps(row),flush=True)
    for p,digest in guards.items():assert sha(Path(p))==digest
    report={'cases':rows,'sourceGuardsBeforeAfter':guards,'scope':'Host-only fixed diagnostic terrain0/camera:128 warm60source seconds, exact pause,40stop steps,recreate128 for12source seconds,40stop; unchanged candidate20M maximum quota and original5s deadline,16MiB guest/32MiBTCG/32768packet. Negative129/duplicate/out-of-range-height/coordinate. Does not prove real Android128/position/view/FPS/GPU/ARM/JavaOOM; current sixJNI/APK/rules/save/RNG unchanged. Failure retained, no automatic enlarged quota or retry.','wholeGoalComplete':False};(DOC/'FIRE_BUDGET_LIFECYCLE249.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
if __name__=='__main__':main()
