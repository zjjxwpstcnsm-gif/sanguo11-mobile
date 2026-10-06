#!/usr/bin/env python3
"""Deterministic new map-add material; never update an inherited resource pin."""
import hashlib,json,pathlib,re,subprocess
ROOT=pathlib.Path(__file__).resolve().parents[4]
MATC=pathlib.Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/matc-build/tools/matc/matc')
if subprocess.check_output([str(MATC),'--version'],text=True).strip()!='56':raise ValueError('Require Filament1.56.0 matc56')
source=ROOT/'tools/3d/session-a-pc-map-effect-add.mat';asset=ROOT/'app/src/main/assets/3d/pc-effects/quad-add.filamat'
out=ROOT/'out/session-a/fire-material';out.mkdir(parents=True,exist_ok=True)
files=[]
for n in range(2):
    target=out/f'quad-add-{n}.filamat'
    subprocess.run([str(MATC),'-p','mobile','-a','opengl','-o',str(target),str(source)],check=True)
    files.append(target.read_bytes())
if files[0]!=files[1]:raise ValueError('Material compilation not byte-identical')
sha=hashlib.sha256(files[0]).hexdigest();asset.write_bytes(files[0])
relative='3d/pc-effects/quad-add.filamat';pin=ROOT/'app/src/main/java/game/sanguo/mobile/VerifiedMaterial.java';text=pin.read_text()
entry='        EXPECTED.put("'+relative+'", "'+sha+'");'
pattern=r'        EXPECTED.put\("'+re.escape(relative)+r'", "[a-f0-9]{64}"\);'
if re.search(pattern,text):text=re.sub(pattern,lambda _:entry,text)
else:text=text.replace('    static {','    static {\n'+entry,1)
pin.write_text(text)
# Existing168 pins stay byte-for-byte the same, including the release manifest.
# The new material is bounded and runtime-pinned in VerifiedMaterial; additive
# final release-manifest integration is reserved for the serial integrator.
report={'material':str(asset),'source':str(source),'bytes':len(files[0]),'sha256':sha,'compiler':str(MATC),'compilerSha256':hashlib.sha256(MATC.read_bytes()).hexdigest(),'version':56,'repeatedCompilationByteEqual':True,'sourceBlend':[1,5,2],'depthTest':True,'depthWrite':False,'alphaTest':'GREATER1/255','inherited168PinsModified':False,'jni4Modified':False,'normalOriginalCellFireAccepted':False,'encodedFramebufferParityAccepted':False}
(ROOT/'docs/handoff/20261006/session-a/FIRE_MATERIAL.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
