#!/usr/bin/env python3
"""Compile own new child outputs against exact frozen Unicorn; never overwrite4 JNI."""
import pathlib,subprocess,hashlib,json,shutil
ROOT=pathlib.Path(__file__).resolve().parents[2]
TOOL=pathlib.Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain')
NDK=TOOL/'android-ndk-r27d';BIN=NDK/'toolchains/llvm/prebuilt/darwin-x86_64/bin';UNICORN=TOOL/'unicorn-2.1.4'
assert 'Pkg.Revision = 27.3.13750724' in (NDK/'source.properties').read_text()
assert subprocess.check_output(['git','-C',str(UNICORN),'rev-parse','HEAD'],text=True).strip()=='8028ec436f2d9376525352dd38ed9ed6b9f6be10'
assert not subprocess.check_output(['git','-C',str(UNICORN),'diff','--','include/unicorn'],text=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
base=json.loads((ROOT/'docs/handoff/20261006/session-a/INHERITANCE.json').read_text())['jni'];before={r['path']:sha(ROOT/r['path']) for r in base}
for r in base:assert before[r['path']]==r['sha256']
out=ROOT/'out/session-a/pc-fire-runtime';sources=out/'sources';sources.mkdir(parents=True,exist_ok=True)
for name in ['pc_effect_scene_probe.c','pc_effect_vm_probe.c']:shutil.copy2(ROOT/'tools/content/native'/name,sources/name)
records=[]
for abi,target in [('x86_64','x86_64-linux-android26'),('arm64-v8a','aarch64-linux-android26')]:
 dest=out/'jniLibs'/abi;dest.mkdir(parents=True,exist_ok=True);lib=ROOT/'out/pc-native-runtime/jniLibs'/abi/'libunicorn.so';shutil.copy2(lib,dest/lib.name)
 binary=dest/'libpc_effect_fire_worker.so';cmd=[str(BIN/'clang'),'--target='+target,'-std=c11','-O3','-fPIE','-pie','-Wall','-Wextra','-Werror','-I',str(UNICORN/'include'),str(sources/'pc_effect_scene_probe.c'),'-L',str(dest),'-Wl,-rpath,$ORIGIN','-lunicorn','-lm','-o',str(binary)]
 subprocess.run(cmd,check=True)
 elf=subprocess.check_output([str(BIN/'llvm-readelf'),'-h','-d',str(binary)],text=True);assert 'libunicorn.so' in elf
 (dest/'worker-elf.txt').write_text(elf);records.append({'abi':abi,'command':cmd,'worker':str(binary),'bytes':binary.stat().st_size,'sha256':sha(binary),'unicornSha256':sha(dest/lib.name),'runtimeAccepted':False,'apkAdditionalPath':'lib/'+abi+'/'+binary.name})
stage=out/'additional-jniLibs'
for record in records:
 abi=record['abi'];target=stage/abi;target.mkdir(parents=True,exist_ok=True);shutil.copy2(record['worker'],target/'libpc_effect_fire_worker.so')
for r in base:assert sha(ROOT/r['path'])==before[r['path']]
report={'outputs':records,'sourceSha256':{p.name:sha(p) for p in sources.iterdir()},'ndk':'27.3.13750724','unicornCommit':'8028ec436f2d9376525352dd38ed9ed6b9f6be10','frozen4JniSourceUntouched':True,'oldSceneInputUntouched':True,'newNativeArtifactScope':'independent compile only; not installed/defaultJNI/ARM/performance acceptance','goalComplete':False}
(ROOT/'docs/handoff/20261006/session-a/FIRE_NATIVE_BUILD.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
