#!/usr/bin/env python3
"""Host candidate quota experiment, exact small-case source-record comparison."""
from pathlib import Path
import json,hashlib,subprocess,struct,os,time,re
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';OUT=ROOT/'out/session-a/admitted-fire-budget246'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def semantic(raw):
    out=bytearray(raw[:16]);off=16;frames=[]
    while off+32<=len(raw):
        h=raw[off:off+32];assert h[:8]==b'PCFXFR01';serial,count=struct.unpack_from('<II',h,8);elapsed,update,draw,geometry=struct.unpack_from('<4f',h,16);off+=32
        records=raw[off:off+184*count];assert len(records)==184*count;off+=len(records);out+=h[:20]+records
        frames.append(dict(serial=serial,packets=count,elapsed=elapsed,updateMs=update,drawMs=draw,geometryMs=geometry,packetSha256=hashlib.sha256(records).hexdigest()))
    assert off==len(raw);return bytes(out),frames
def main():
    assert not OUT.exists();OUT.mkdir(parents=True)
    source=ROOT/'tools/content/native';files=['pc_effect_scene_probe.c','pc_effect_vm_probe.c','session_a_cell_fire_budget_worker.c'];before={n:sha(source/n) for n in files}
    lib=ROOT/'out/session-a/portrait-native-deps/unicorn/lib/libunicorn.a';include=Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/unicorn-2.1.4/include')
    cmd=['clang','-std=c11','-O3','-Wall','-Wextra','-Werror','-I',str(include),str(source/files[-1]),str(lib),'-lm','-lpthread','-o',str(OUT/'host-probe')]
    with (OUT/'compile.log').open('w') as f:r=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT)
    assert r.returncode==0,(OUT/'compile.log').read_text()
    env=os.environ.copy();env['PC_VM_PROBE_BLOCK_BUDGET']='1';rows=[]
    for count in [1,13,32,64,128]:
        old=ROOT/f'out/session-a/fire-capacity-matrix236/cells-{count:03d}';payload=(old/'commands.bin').read_bytes();center=struct.unpack_from('<3f',payload,12);case=OUT/f'cells-{count:03d}';case.mkdir();start=time.monotonic()
        r=subprocess.run([str(OUT/'host-probe'),str(ROOT/'app/src/main/assets/3d/pc-effects/source-kernel.bin'),str(ROOT/'app/src/main/assets/3d/pc-effects/source-fire-scene.bin'),*[str(x) for x in center],'--stream'],input=payload,capture_output=True,env=env,timeout=120)
        (case/'stdout.bin').write_bytes(r.stdout);(case/'stderr.txt').write_bytes(r.stderr);records,frames=semantic(r.stdout)
        equal=None
        if count<=32:
            original,_=semantic((old/'stdout.bin').read_bytes());equal=records==original;assert equal,'Source records or time changed'
        log=r.stderr.decode();peak=re.search(r'PC_CELL_BUDGET_PEAK bytes=(\d+) highwater=(\d+) limit=(\d+)',log)
        row={'controllers':count,'exit':r.returncode,'framesCompleted':len(frames),'framesRequested':71,'wallSeconds':time.monotonic()-start,'smallCaseSourceRecordAndTimeExact':equal,'semanticSha256':hashlib.sha256(records).hexdigest(),'maximumPackets':max([x['packets'] for x in frames],default=0),'maximumObservedCallBytes':int(peak[1]) if peak else None,'quotaHighwater':int(peak[2]) if peak else None,'quotaBytes':int(peak[3]) if peak else None,'maximumNativeEvaluatorMs':{k:max([x[k] for x in frames],default=0) for k in ['updateMs','drawMs','geometryMs']},'frames':frames,'stderr':log,'stderrSha256':sha(case/'stderr.txt'),'stdoutSha256':sha(case/'stdout.bin')}
        if r.returncode==0:
            assert len(frames)==71 and f'active=0 created=0 stopped={count}' in log
            row['pauseExact']=frames[29]['elapsed']==frames[30]['elapsed'] and frames[29]['packetSha256']==frames[30]['packetSha256']
        rows.append(row);print(json.dumps({k:v for k,v in row.items() if k not in ('frames','stderr')}),flush=True)
    for n in files:assert sha(source/n)==before[n]
    report={'sourceBeforeAfterExact':before,'candidateWorker':str(source/files[-1]),'compileCommand':cmd,'cases':rows,'scope':'Host-only candidate: max20M per-native-call block-byte quota derived from verified admitted controller highwater in32-cell batches, original5s/Java6s/guest16MiB/TCG32MiB/packet32768 remain. Same original objects/resources/order/visualRNG/time/protocol, exact source records compared1/13/32. Diagnostic height0/recorded camera, no Android/ARM/GPU/normal128 performance acceptance. Current original4/additional2JNI not rebuilt or changed; not an approved production fix or uniqueJavaOOM claim.','wholeGoalComplete':False};(DOC/'ADMITTED_FIRE_BUDGET246.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
if __name__=='__main__':main()
