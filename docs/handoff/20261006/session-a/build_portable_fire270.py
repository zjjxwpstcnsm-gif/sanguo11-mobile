#!/usr/bin/env python3
"""Compile the tracked instruction worker directly, preserving original JNI."""
from pathlib import Path
import argparse,json,hashlib,subprocess,shutil
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--toolchain',type=Path,default=Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain'));parser.add_argument('--output',type=Path,default=ROOT/'out/session-a/portable-fire-build270');args=parser.parse_args();out=args.output.resolve();assert not out.exists();out.mkdir(parents=True)
 before=json.loads((DOC/'INSTRUCTION_FIRE_CANDIDATE260.json').read_text());source=ROOT/'tools/content/native/session_a_cell_fire_instruction_worker.c';assert sha(source)=='435850bcdc505a3ce5397277e0ace178dcff82ef8ed57c137b390947c5a9ae35'
 for n,h in before['sourceSha256'].items():
  if n!='session_a_cell_fire_budget_worker.c':assert sha(ROOT/'tools/content/native'/n)==h
 sources=out/'sources';sources.mkdir();compiled_source=sources/'session_a_cell_fire_budget_worker.c';shutil.copy2(source,compiled_source)
 assert sha(compiled_source)==sha(source)
 for name in ['pc_effect_scene_probe.c','pc_effect_vm_probe.c']:shutil.copy2(ROOT/'tools/content/native'/name,sources/name)
 guards={n:h for n,h in before['protectedSixUnchanged'].items() if n.startswith('out/pc-native-runtime/')}
 for n,h in guards.items():assert sha(ROOT/n)==h
 ndk=args.toolchain/'android-ndk-r27d';unicorn=args.toolchain/'unicorn-2.1.4';bins=ndk/'toolchains/llvm/prebuilt/darwin-x86_64/bin';assert 'Pkg.Revision = 27.3.13750724' in (ndk/'source.properties').read_text();assert subprocess.check_output(['git','-C',str(unicorn),'rev-parse','HEAD'],text=True).strip()=='8028ec436f2d9376525352dd38ed9ed6b9f6be10';rows=[]
 for abi,target in [('x86_64','x86_64-linux-android26'),('arm64-v8a','aarch64-linux-android26')]:
  folder=out/'jniLibs'/abi;folder.mkdir(parents=True);lib=ROOT/'out/pc-native-runtime/jniLibs'/abi/'libunicorn.so';shutil.copy2(lib,folder/lib.name);worker=folder/'libpc_effect_fire_worker.so';cmd=[str(bins/'clang'),'--target='+target,'-std=c11','-O3','-fPIE','-pie','-Wall','-Wextra','-Werror','-I',str(unicorn/'include'),str(compiled_source),'-L',str(folder),'-Wl,-rpath,$ORIGIN','-lunicorn','-lm','-o',str(worker)]
  with (folder/'compile.log').open('w') as f:r=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT)
  assert r.returncode==0,(folder/'compile.log').read_text();expected=next(x['sha256'] for x in before['outputs'] if x['abi']==abi);assert sha(worker)==expected;rows.append({'abi':abi,'path':str(worker),'sha256':sha(worker),'compiledByteExact260':True,'command':cmd})
 for n,h in guards.items():assert sha(ROOT/n)==h
 report={'trackedSource':str(source),'trackedSourceSha256':sha(source),'outputs':rows,'originalFourUnchanged':guards,'usesTemporaryDiagnosticSource':False,'actualInstalled':False,'scope':'Direct tracked-source NDK build for both ABIs byte-identical to actual host-validated260 outputs. Output is a separate directory, never default current JNI. Shared two B native files included read-only with immutable hashes. No Android/ARM runtime/performance claim.','wholeGoalComplete':False};(DOC/'PORTABLE_FIRE_SOURCE270.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
if __name__=='__main__':main()
