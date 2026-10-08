#!/usr/bin/env python3
"""Portable exact cache-only worker build; explicit source root, frozen original4."""
from pathlib import Path
import argparse,json,hashlib,subprocess,shutil
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--source-root',type=Path,default=ROOT);parser.add_argument('--toolchain',type=Path,required=True);parser.add_argument('--output',type=Path,required=True);a=parser.parse_args();root=a.source_root.resolve();out=a.output.resolve();assert not out.exists()
 proof=json.loads((DOC/'FIRE_CACHE_NATIVE_CANDIDATE295.json').read_text());src=root/'tools/content/native'
 for n,h in proof['sourceSha256'].items():assert sha(src/n)==h,'Use exact296 source root or exported306 root: '+n
 original4={n:h for n,h in proof['protectedSixUnchanged'].items() if n.startswith('out/pc-native-runtime/')};assert len(original4)==4
 for n,h in original4.items():assert sha(root/n)==h
 ndk=a.toolchain/'android-ndk-r27d';unicorn=a.toolchain/'unicorn-2.1.4';assert 'Pkg.Revision = 27.3.13750724' in (ndk/'source.properties').read_text();assert subprocess.check_output(['git','-C',str(unicorn),'rev-parse','HEAD'],text=True).strip()=='8028ec436f2d9376525352dd38ed9ed6b9f6be10';bins=ndk/'toolchains/llvm/prebuilt/darwin-x86_64/bin'
 out.mkdir(parents=True);sources=out/'sources';sources.mkdir()
 for n in proof['sourceSha256']:shutil.copy2(src/n,sources/n)
 rows=[]
 for abi,target in [('x86_64','x86_64-linux-android26'),('arm64-v8a','aarch64-linux-android26')]:
  folder=out/'jniLibs'/abi;folder.mkdir(parents=True);shutil.copy2(root/'out/pc-native-runtime/jniLibs'/abi/'libunicorn.so',folder/'libunicorn.so');worker=folder/'libpc_effect_fire_worker.so'
  command=[str(bins/'clang'),'--target='+target,'-std=c11','-O3','-fPIE','-pie','-Wall','-Wextra','-Werror','-I',str(unicorn/'include'),str(sources/'session_a_cell_fire_instruction_worker.c'),'-L',str(folder),'-Wl,-rpath,$ORIGIN','-lunicorn','-lm','-o',str(worker)]
  with (folder/'compile.log').open('w') as f:r=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT)
  assert r.returncode==0,(folder/'compile.log').read_text();expected=next(x['sha256'] for x in proof['outputs'] if x['abi']==abi);assert sha(worker)==expected
  rows.append({'abi':abi,'path':str(worker),'sha256':sha(worker),'byteExact295AndPackaged296':True,'command':command,'compileLogSha256':sha(folder/'compile.log')})
 for n,h in original4.items():assert sha(root/n)==h
 report={'sourceRoot':str(root),'sourceSha256':proof['sourceSha256'],'outputs':rows,'originalFourUnchanged':original4,'independentOutput':True,'actualAndroidInstalledByThisRecipe':False,'armExecutionAccepted':False,'scope':'Explicit exact candidate296 or full306 source root; compile file basename matches295, same pinnedNDK27.3/unicorn8028 headers and included original libraries. Both complete ELF bytes exactly equal packaged296 cache workers, no original/current JNI overwrite. Not new runtime/performance/ARM or APK bit-repro acceptance.','wholeGoalComplete':False}
 (out/'portable-report.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report),flush=True)
if __name__=='__main__':main()
