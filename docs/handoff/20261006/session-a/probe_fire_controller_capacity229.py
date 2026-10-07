#!/usr/bin/env python3
"""Original visual child capacity/lifecycle diagnostic, no authority or APK."""
from pathlib import Path
import hashlib,json,struct,subprocess,time,resource,platform,argparse,os
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--revision',type=int,choices=[229,230],default=229);parser.add_argument('--production-budget',action='store_true');args=parser.parse_args()
    OUT=ROOT/f'out/session-a/fire-controller-capacity{args.revision}'
    assert not OUT.exists();OUT.mkdir(parents=True)
    binary=ROOT/'out/session-a/pc-fire-runtime/host/pc-effect-fire-tcg32-probe'
    kernel=ROOT/'app/src/main/assets/3d/pc-effects/source-kernel.bin';scene=ROOT/'app/src/main/assets/3d/pc-effects/source-fire-scene.bin'
    fixture=Path('/Users/paopao/workspace/sanguo11-mobile/out/pc-visual/v150-native-x86-source-rh-camera-final/commands.bin')
    assert sha(fixture)=='eee7a29a813eb094a3b9d189c096e219b392b65dcf4b265067e8ceb798902176'
    camera=fixture.read_bytes()[12:216];center=struct.unpack_from('<3f',camera)
    cells=[(x,y,0.) for x in range(150,158) for y in range(80,96)]
    steps=[('warm',.1,cells)]*60+[('pause',0.,cells)]+[('stop',.1,[])]*40+[('restart',.1,cells)]*10+[('final-stop',.1,[])]*40
    def command(kind,serial,dt,active=None):
        raw=struct.pack('<IIf',kind,serial,dt)+camera
        if active is not None:raw+=struct.pack('<I',len(active))+b''.join(struct.pack('<IIf',*c) for c in active)
        return raw
    payload=command(0,0,0.)+b''.join(command(4,i+1,dt,c) for i,(_,dt,c) in enumerate(steps))+command(3,len(steps)+1,0.)
    (OUT/'commands.bin').write_bytes(payload)
    guards={str(p):sha(p) for p in [binary,kernel,scene,fixture]};started=time.monotonic()
    env=os.environ.copy();env.pop('PC_VM_PROBE_BLOCK_BUDGET',None)
    if args.production_budget:env['PC_VM_PROBE_BLOCK_BUDGET']='1'
    result=subprocess.run([str(binary),str(kernel),str(scene),*[str(v) for v in center],'--stream'],input=payload,capture_output=True,timeout=240,env=env)
    wall=time.monotonic()-started;(OUT/'stdout.bin').write_bytes(result.stdout);(OUT/'stderr.txt').write_bytes(result.stderr)
    if result.returncode!=0:
        for p,digest in guards.items():assert sha(Path(p))==digest
        report={'passedTransportAndLifecycle':False,'revision':args.revision,'productionBlockBudget':args.production_budget,'sourceGuards':guards,'exitCode':result.returncode,'wallSeconds':wall,'requestedVisualControllers':128,'stdoutBytes':len(result.stdout),'stderr':result.stderr.decode(errors='replace'),'stderrSha256':sha(OUT/'stderr.txt'),'stdoutSha256':sha(OUT/'stdout.bin'),'scope':'Isolated original evaluator128 diagnostic cells, height0. Child boundary failure retained. No Java OOM, World/rule/RNG mutation, Android/GPU/ARM or normal128 acceptance. Production is not modified or weakened.','wholeGoalComplete':False}
        (DOC/f'FIRE_CONTROLLER_CAPACITY{args.revision}.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(report,ensure_ascii=False));return

    assert result.stdout[:8]==b'PCFXRDY2';offset=16;frames=[]
    for i,(phase,dt,c) in enumerate(steps):
        header=result.stdout[offset:offset+32];assert header[:8]==b'PCFXFR01';serial,count=struct.unpack_from('<II',header,8);assert serial==i+1
        elapsed,update,draw,geometry=struct.unpack_from('<4f',header,16);offset+=32
        records=result.stdout[offset:offset+count*184];assert len(records)==count*184;offset+=len(records)
        frames.append(dict(serial=serial,phase=phase,controllerCount=len(c),packets=count,elapsed=elapsed,updateMs=update,drawMs=draw,geometryMs=geometry,packetSha256=hashlib.sha256(records).hexdigest()))
    assert offset==len(result.stdout)
    log=result.stderr.decode();assert 'active=128 created=128' in log and 'active=0 created=0 stopped=128' in log
    assert 'totalCreated=256 totalStopped=256' in log
    assert frames[59]['elapsed']==frames[60]['elapsed'] and frames[59]['packetSha256']==frames[60]['packetSha256'],'Pause changed visual records'
    assert frames[100]['packets']==0 and frames[-1]['packets']==0,'Stopped original effect did not drain'
    for p,digest in guards.items():assert sha(Path(p))==digest
    rss=resource.getrusage(resource.RUSAGE_CHILDREN).ru_maxrss
    report={'passedTransportAndLifecycle':True,'sourceGuards':guards,'recordedCameraSha256':hashlib.sha256(camera).hexdigest(),'visualDiagnosticHeight':0,'cells':cells,
            'frames':frames,'maximumPackets':max(x['packets'] for x in frames),'maximumPacketTransportBytes':max(x['packets'] for x in frames)*184,
            'wallSeconds':wall,'hostChildMaximumRssRaw':rss,'hostChildMaximumRssUnit':'bytes' if platform.system()=='Darwin' else 'KiB',
            'maximumOriginalEvaluatorMs':{k:max(x[k] for x in frames) for k in ('updateMs','drawMs','geometryMs')},
            'pauseExactPacketAndClock':True,'two128CreateStopCycles':True,'finalPacketsZero':True,'stderrPath':str(OUT/'stderr.txt'),'stderrSha256':sha(OUT/'stderr.txt'),'stdoutSha256':sha(OUT/'stdout.bin'),
            'scope':'Isolated original visual evaluator, diagnostic128 cells with chosen height0 and immutable recorded camera. No World/snapshot/rule/RNG injection; no game state. Host timing affected by live emulator and B build. Does not prove original terrain position, normal Android128-fire path, Java/native/GPU peak or frame budget, chains, ARM, or whole-goal acceptance.',
            'androidAccepted':False,'gpuAccepted':False,'armAccepted':False,'wholeGoalComplete':False}
    (DOC/f'FIRE_CONTROLLER_CAPACITY{args.revision}.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({k:v for k,v in report.items() if k not in ('sourceGuards','cells','frames')},ensure_ascii=False))
if __name__=='__main__':main()
