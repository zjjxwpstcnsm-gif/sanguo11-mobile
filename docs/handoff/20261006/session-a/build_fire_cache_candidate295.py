#!/usr/bin/env python3
"""Separate ABI candidates after exact host source output; freeze current JNI."""
from pathlib import Path
import json,subprocess,shutil
from run_remaining_normal_media import sha
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
OUT=ROOT/'out/session-a/fire-cache-native295'
def main():
 assert not OUT.exists();proof=json.loads((DOC/'FIRE_CACHE_CANDIDATE294.json').read_text());assert len(proof['cases'])==2 and all(x['recordsAndSourceClockByteExact'] and x['metadataQueryRatio']<.005 for x in proof['cases'])
 old=json.loads((DOC/'INSTRUCTION_FIRE_CANDIDATE260.json').read_text());protected=old['protectedSixUnchanged']
 for p,h in protected.items():assert sha(ROOT/p)==h
 OUT.mkdir(parents=True);dest=OUT/'sources';dest.mkdir();source=Path(proof['candidateSource']);assert sha(source)==proof['candidateSourceSha256']
 for n in ['pc_effect_scene_probe.c','pc_effect_vm_probe.c',source.name]:shutil.copy2(source.parent/n,dest/n)
 tool=Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain');ndk=tool/'android-ndk-r27d';unicorn=tool/'unicorn-2.1.4';bins=ndk/'toolchains/llvm/prebuilt/darwin-x86_64/bin';assert 'Pkg.Revision = 27.3.13750724' in (ndk/'source.properties').read_text();assert subprocess.check_output(['git','-C',str(unicorn),'rev-parse','HEAD'],text=True).strip()=='8028ec436f2d9376525352dd38ed9ed6b9f6be10';rows=[]
 for abi,target in [('x86_64','x86_64-linux-android26'),('arm64-v8a','aarch64-linux-android26')]:
  folder=OUT/'jniLibs'/abi;folder.mkdir(parents=True);lib=ROOT/'out/pc-native-runtime/jniLibs'/abi/'libunicorn.so';shutil.copy2(lib,folder/lib.name);worker=folder/'libpc_effect_fire_worker.so'
  command=[str(bins/'clang'),'--target='+target,'-std=c11','-O3','-fPIE','-pie','-Wall','-Wextra','-Werror','-I',str(unicorn/'include'),str(dest/source.name),'-L',str(folder),'-Wl,-rpath,$ORIGIN','-lunicorn','-lm','-o',str(worker)]
  with (folder/'compile.log').open('w') as f:r=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT)
  assert r.returncode==0,(folder/'compile.log').read_text();elf=subprocess.check_output([str(bins/'llvm-readelf'),'-h','-d',str(worker)],text=True);assert 'libunicorn.so' in elf;(folder/'elf.txt').write_text(elf);rows.append({'abi':abi,'path':str(worker),'sha256':sha(worker),'bytes':worker.stat().st_size,'compileCommand':command,'compileLogSha256':sha(folder/'compile.log'),'unicornSha256':sha(folder/lib.name)})
 for p,h in protected.items():assert sha(ROOT/p)==h
 report={'outputs':rows,'sourceSha256':{p.name:sha(p) for p in dest.iterdir()},'protectedSixUnchanged':protected,'metadataCacheSlots':65536,'additionalReplacementBytes':16384,'native5sJava6sAndAllOtherLimitsUnchanged':True,'actualInstalled':False,'rootCauseProven':False,'scope':'Separate ABI compilation of exact host294 cache-only candidate. Original4 and current extra2 unchanged, no app/Bridge/Unity/rules/save/RNG rewrite. Host2/128 exact801 records/time/pause and dramatically reduced metadata queries; host wall smallcase regressed7%,largecase improved6%, not Android/FPS/root-cause proof. Requires independent full newAPK and actual normal fire/readback/cold/userSHA restore.','wholeGoalComplete':False}
 (DOC/'FIRE_CACHE_NATIVE_CANDIDATE295.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'outputs':rows,'actualInstalled':False}),flush=True)
if __name__=='__main__':main()
