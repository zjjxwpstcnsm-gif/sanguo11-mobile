#!/usr/bin/env python3
"""Compare exact source records/time across translator budgets, not normal APK acceptance."""
import pathlib,struct,subprocess,json,hashlib,time
ROOT=pathlib.Path(__file__).resolve().parents[4];OUT=ROOT/'out/session-a/fire-tcg32';OUT.mkdir(exist_ok=True)
BASE=pathlib.Path('/Users/paopao/.codex/worktrees/map-ui-media-integration/sanguo11-mobile/out/session-a/pc-fire-runtime/host/pc-effect-fire-probe')
NEW=ROOT/'out/session-a/pc-fire-runtime/host/pc-effect-fire-tcg32-probe'
FIXTURE=pathlib.Path('/Users/paopao/workspace/sanguo11-mobile/out/pc-visual/v150-native-x86-source-rh-camera-final/commands.bin');raw=FIXTURE.read_bytes();assert hashlib.sha256(raw).hexdigest()=='eee7a29a813eb094a3b9d189c096e219b392b65dcf4b265067e8ceb798902176';camera=raw[12:216];center=struct.unpack_from('<3f',camera)
def cmd(kind,serial,dt,cells=None):
 b=struct.pack('<IIf',kind,serial,dt)+camera
 if cells is not None:b+=struct.pack('<I',len(cells))+b''.join(struct.pack('<IIf',*x) for x in cells)
 return b
def run(binary,scene,steps):
 data=cmd(0,0,0.)+b''.join(cmd(k,i+1,dt,c) for i,(k,dt,c) in enumerate(steps))+cmd(3,len(steps)+1,0.)
 began=time.monotonic();p=subprocess.run([str(binary),str(ROOT/'app/src/main/assets/3d/pc-effects/source-kernel.bin'),str(ROOT/'app/src/main/assets/3d/pc-effects'/scene),*[str(v) for v in center],'--stream'],input=data,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=240)
 assert p.returncode==0,p.stderr.decode();off=16;semantic=bytearray(p.stdout[:16]);counts=[];metrics=[]
 for i in range(len(steps)):
  header=p.stdout[off:off+32];assert header[:8]==b'PCFXFR01';serial,count=struct.unpack_from('<II',header,8);assert serial==i+1;semantic+=header[:20];metrics.append(struct.unpack_from('<3f',header,20));off+=32;semantic+=p.stdout[off:off+count*184];off+=count*184;counts.append(count)
 assert off==len(p.stdout);return semantic,{'frames':len(steps),'semanticSha256':hashlib.sha256(semantic).hexdigest(),'counts':counts,'maxPackets':max(counts),'wallSec':time.monotonic()-began,'stderr':p.stderr.decode(),'maximumNativeMs':{'update':max(x[0] for x in metrics),'draw':max(x[1] for x in metrics),'geometry':max(x[2] for x in metrics)},'binarySha256':hashlib.sha256(binary.read_bytes()).hexdigest()}
report={'passed':False,'requestedTcgBytes':33554432,'scope':'isolated recorded-camera transport source records; ignores host timing; no normal Android/GPU/ARM acceptance','Android':False,'GPU':False,'ARM':False,'goalComplete':False}
try:
 cases=[('legacy8','source-scene.bin',[(1,.1,None)]*50),('source9','source-fire-scene.bin',[(4,.1,[(0,0,0.)])]*10+[(4,0.,[(0,0,0.)])]+[(4,.1,[(0,0,0.),(1,0,0.)])]*10+[(4,.1,[])]*40+[(4,.1,[(0,0,0.)])]*10)]
 report['cases']={}
 for label,scene,steps in cases:
  a,old=run(BASE,scene,steps);b,new=run(NEW,scene,steps);assert a==b,(label,'translator budget changed source records/time');assert 'PC_TCG_BUDGET requested=33554432 actual=33554432' in new['stderr'];report['cases'][label]={'before':old,'after':new,'exactSourceRecordBytesAndTimeEqual':True}
 report['passed']=True
except Exception as error:report['error']=repr(error)
(ROOT/'docs/handoff/20261006/session-a/FIRE_TCG_BUDGET.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k!='cases'}))
if not report['passed']:raise SystemExit(1)
