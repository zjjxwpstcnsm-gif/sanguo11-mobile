#!/usr/bin/env python3
"""Extract exact production control flow; stub only driver operations. Not GPU/phone timing."""
import os,subprocess,pathlib,json
root=pathlib.Path(__file__).resolve().parents[4]
os.chdir(root)
out=root/'out/r16/visibility';out.mkdir(parents=True,exist_ok=True)
source='app/src/main/java/game/sanguo/mobile/FilamentMapView.java'
def method(s,head):
 start=s.index(head);b=s.index('{',start);depth=1;i=b+1
 while depth:
  if s[i]=='{':depth+=1
  elif s[i]=='}':depth-=1
  i+=1
 return s[start:i]
for variant,ref in [('baseline','f355536d94d22f623fc44e33c91410f66938c7d0'),('candidate','HEAD')]:
 s=subprocess.check_output(['git','show',f'{ref}:{source}'],text=True)
 methods='\n'.join(method(s,h) for h in ['private void refreshPendingMeshes()','private void loadVisible()','private boolean inView('])
 methods=methods.replace('private boolean inView(float x,float z,float radius){','private boolean inView(float x,float z,float radius){checks++;')
 folder=out/variant;folder.mkdir(exist_ok=True)
 template=(pathlib.Path(__file__).parent/'visibility-harness.template').read_text();(folder/'VisibilityHarness.java').write_text(template.replace('/*PRODUCTION_METHODS*/',methods))
 cp=f'app/build/field-check:{os.environ["JSON_TEST_JAR"]}'
 subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-cp',cp,'-d',str(folder),str(folder/'VisibilityHarness.java'),'app/src/main/java/game/sanguo/mobile/TerrainMaterialLod.java'],check=True)
 with (out/f'{variant}.log').open('w') as log:subprocess.run(['java','-cp',f'{folder}:{cp}','game.sanguo.mobile.VisibilityHarness',variant],check=True,stdout=log)
a=(out/'baseline.log').read_text().splitlines();b=(out/'candidate.log').read_text().splitlines();assert a[1:]==b[1:],'visibility output mismatch'
print(a[0]);print(b[0]);print('PASS exact source control flow: same 20 camera-state outputs, replacement retirement, dynamic unit culling; GPU mocked')
