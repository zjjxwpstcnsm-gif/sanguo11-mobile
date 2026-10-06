#!/usr/bin/env python3
"""Read isolated original visual evaluator state; never a World or rule step."""
import pathlib,subprocess,struct,hashlib,json,os
ROOT=pathlib.Path(__file__).resolve().parents[4]
HOST=pathlib.Path('/Users/paopao/.codex/worktrees/map-ui-media-integration/sanguo11-mobile/out/session-a/pc-fire-runtime/host')
FIXTURE=pathlib.Path('/Users/paopao/workspace/sanguo11-mobile/out/pc-visual/v150-native-x86-source-rh-camera-final/commands.bin')
fixture=FIXTURE.read_bytes();assert hashlib.sha256(fixture).hexdigest()=='eee7a29a813eb094a3b9d189c096e219b392b65dcf4b265067e8ceb798902176'
camera=fixture[12:216];center=struct.unpack_from('<3f',camera);OUT=ROOT/'out/session-a/fire-init-rng';OUT.mkdir(exist_ok=True);rows=[]
for frames in [0,1]:
 for label,binary,scene in [('old',HOST/'legacy-probe','source-scene.bin'),('new9',HOST/'pc-effect-fire-probe','source-fire-scene.bin')]:
  path=OUT/(label+'-'+str(frames)+'.bin');env=dict(os.environ);env['PC_VM_PROBE_VISUAL_RNG_PATH']=str(path)
  command=lambda k,s,dt:struct.pack('<IIf',k,s,dt)+camera
  data=command(0,0,0.)+b''.join(command(1,i+1,.1) for i in range(frames))+command(3,frames+1,0.)
  result=subprocess.run([str(binary),str(ROOT/'app/src/main/assets/3d/pc-effects/source-kernel.bin'),str(ROOT/'app/src/main/assets/3d/pc-effects'/scene),*[str(v) for v in center],'--stream'],input=data,env=env,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=120)
  assert result.returncode==0,result.stderr.decode();state=path.read_bytes();assert len(state)==2504
  rows.append({'label':label,'afterFrames':frames,'bytes':len(state),'sha256':hashlib.sha256(state).hexdigest(),'path':str(path),'nativeWorkerSha256':hashlib.sha256(binary.read_bytes()).hexdigest(),'state':state})
for n in [0,1]:
 a,b=[x['state'] for x in rows if x['afterFrames']==n];print(n,'stateEqual',a==b,'differingBytes',sum(x!=y for x,y in zip(a,b)))
report={'scope':'isolated original evaluator RNG diagnostic only; state is never applied to authority or rewritten','cameraFixtureSha256':hashlib.sha256(fixture).hexdigest(),'initStateEqual':rows[0]['state']==rows[1]['state'],'afterOneFrameStateEqual':rows[2]['state']==rows[3]['state'],'rows':[{k:v for k,v in x.items() if k!='state'} for x in rows],'normalAndroid':False,'ARM':False,'originalPCFramebufferParity':False,'goalComplete':False}
(ROOT/'docs/handoff/20261006/session-a/FIRE_INIT_RNG.json').write_text(json.dumps(report,indent=2)+'\n')
