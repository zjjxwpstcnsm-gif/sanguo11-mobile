#!/usr/bin/env python3
"""Host transport/source-control proof; no Android/rule/normal-flow acceptance."""
import pathlib,subprocess,struct,json,hashlib,time
ROOT=pathlib.Path(__file__).resolve().parents[4]
OUT=ROOT/'out/session-a/pc-fire-runtime/host';ASSETS=ROOT/'app/src/main/assets/3d/pc-effects'
# Read-only recorded renderer camera, solely a host native transport fixture.
FIXTURE=pathlib.Path('/Users/paopao/workspace/sanguo11-mobile/out/pc-visual/v150-native-x86-source-rh-camera-final/commands.bin')
raw=FIXTURE.read_bytes();fixtureSha=hashlib.sha256(raw).hexdigest()
assert fixtureSha=='eee7a29a813eb094a3b9d189c096e219b392b65dcf4b265067e8ceb798902176'
assert len(raw)%216==0 and struct.unpack_from('<IIf',raw)==(0,0,0.)
camera=raw[12:216];center=struct.unpack_from('<3f',camera)

def command(kind,serial,dt,cells=None):
 data=struct.pack('<IIf',kind,serial,dt)+camera
 if cells is not None:data+=struct.pack('<I',len(cells))+b''.join(struct.pack('<IIf',*r) for r in cells)
 return data
def run(binary,scene,steps):
 stream=command(0,0,0.)+b''.join(command(kind,i+1,dt,cells) for i,(kind,dt,cells) in enumerate(steps))+command(3,len(steps)+1,0.)
 start=time.monotonic();p=subprocess.run([str(binary),str(ASSETS/'source-kernel.bin'),str(ASSETS/scene),*[str(v) for v in center],'--stream'],input=stream,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=180)
 if p.returncode:raise ValueError((p.returncode,p.stderr.decode(errors='replace')))
 dump=OUT/(binary.name+'-'+scene+'-'+str(len(steps))+'-stdout.bin');dump.write_bytes(p.stdout)
 ready=p.stdout[:16];cursor=16;frames=[]
 for i in range(len(steps)):
  header=p.stdout[cursor:cursor+32];assert header[:8]==b'PCFXFR01';serial,count=struct.unpack_from('<II',header,8);assert serial==i+1 and count<30000
  elapsed,update,draw,geometry=struct.unpack_from('<4f',header,16);cursor+=32;payload=p.stdout[cursor:cursor+count*184];assert len(payload)==count*184;cursor+=len(payload)
  frames.append({'count':count,'elapsed':elapsed,'recordsSha256':hashlib.sha256(payload).hexdigest(),'updateMs':update,'drawMs':draw,'geometryMs':geometry,'bytes':len(payload)})
 assert cursor==len(p.stdout)
 return {'stdoutPath':str(dump),'stdoutSha256':hashlib.sha256(p.stdout).hexdigest(),'ready':ready.hex(),'frames':frames,'wallSec':time.monotonic()-start,'stderr':p.stderr.decode(errors='replace')}
def identity(run):return [(r['count'],r['elapsed'],r['recordsSha256']) for r in run['frames']]
report={'passed':False,'scope':'host C transport and original controller only; chosen test terrainY0 is not original placement/normal game acceptance','AndroidAccepted':False,'ARMAccepted':False,'GPUAccepted':False}
try:
 steps=[(1,.1,None)]*10
 legacy=run(OUT/'legacy-probe','source-scene.bin',steps);old=run(OUT/'pc-effect-fire-probe','source-scene.bin',steps)
 assert identity(legacy)==identity(old),'old8 exact byte compatibility'
 new=run(OUT/'pc-effect-fire-probe','source-fire-scene.bin',steps)
 report.update(legacy=legacy,newNoFire=new,new9NoFireOld8ByteCompatibility=identity(old)==identity(new),old8ByteCompatibility=True)
 assert max(x['count'] for x in legacy['frames'])>0,'nonempty legacy native packet coverage'
 report.update(old8ByteCompatibility=True,cameraFixture={'path':str(FIXTURE),'sha256':fixtureSha,'scope':'recorded transport fixture only, not source/rule/Android acceptance'})
 # Immutable-set protocol exercise, independent of any gameplay rule/expiry.
 steps=[(4,.1,[(0,0,0.)])]*10+[(4,0.,[(0,0,0.)])]+[(4,.1,[(0,0,0.),(1,0,0.)])]*10+[(4,.1,[])]*40+[(4,.1,[(0,0,0.)])]*10
 dynamic=run(OUT/'pc-effect-fire-probe','source-fire-scene.bin',steps)
 assert dynamic['frames'][9]['elapsed']==dynamic['frames'][10]['elapsed'],'pause source time'
 report.update(passed=report['new9NoFireOld8ByteCompatibility'],dynamic=dynamic,creationRefreshPauseRemovalFadeReaddTransport=True,ruleRngHook='472150/4721d0 forbidden in native worker; a hit terminates child')
 if not report['passed']:report['error']='Nonempty old8 scene records differ under new9 manager; APK binding blocked pending source differential, old format unchanged'
except Exception as error:report['error']=repr(error)
(ROOT/'docs/handoff/20261006/session-a/FIRE_NATIVE_HOST.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k not in ['legacy','newNoFire','dynamic']}))
if not report['passed']:raise SystemExit(1)
