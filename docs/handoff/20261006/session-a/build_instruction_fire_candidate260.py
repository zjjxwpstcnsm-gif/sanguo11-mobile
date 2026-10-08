#!/usr/bin/env python3
"""Independent ABI candidates only: preserve current four plus two binaries."""
from pathlib import Path
import json,hashlib,shutil,subprocess
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';OUT=ROOT/'out/session-a/instruction-fire-candidate260'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 assert not OUT.exists();OUT.mkdir(parents=True)
 protected=json.loads((DOC/'SOURCE_CHECKPOINT252.json').read_text())['original4AndNew2Jni']
 for n,h in protected.items():assert sha(ROOT/n)==h
 proof=json.loads((DOC/'INSTRUCTION_BUDGET257.json').read_text());assert proof['lifecycle']['framesCompleted']==801 and proof['lifecycle']['exit0'] and proof['lifecycle']['first660FrameOriginalRecordsAndClockExact']
 boundary=json.loads((DOC/'INSTRUCTION_BOUNDARY259.json').read_text());assert len(boundary['cases'])==9 and all(x.get('allOriginalRecordsAndClockExact246',x.get('invalidAdmissionRejectedBeforeFrame',False)) for x in boundary['cases'])
 source=ROOT/'out/session-a/instruction-budget257';dest=OUT/'sources';dest.mkdir()
 for n in ['pc_effect_scene_probe.c','pc_effect_vm_probe.c','session_a_cell_fire_budget_worker.c']:shutil.copy2(source/n,dest/n)
 tool=Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain');ndk=tool/'android-ndk-r27d';bins=ndk/'toolchains/llvm/prebuilt/darwin-x86_64/bin';unicorn=tool/'unicorn-2.1.4';assert 'Pkg.Revision = 27.3.13750724' in (ndk/'source.properties').read_text();assert subprocess.check_output(['git','-C',str(unicorn),'rev-parse','HEAD'],text=True).strip()=='8028ec436f2d9376525352dd38ed9ed6b9f6be10'
 records=[]
 for abi,target in [('x86_64','x86_64-linux-android26'),('arm64-v8a','aarch64-linux-android26')]:
  output=OUT/'jniLibs'/abi;output.mkdir(parents=True);lib=ROOT/'out/pc-native-runtime/jniLibs'/abi/'libunicorn.so';shutil.copy2(lib,output/lib.name);worker=output/'libpc_effect_fire_worker.so';command=[str(bins/'clang'),'--target='+target,'-std=c11','-O3','-fPIE','-pie','-Wall','-Wextra','-Werror','-I',str(unicorn/'include'),str(dest/'session_a_cell_fire_budget_worker.c'),'-L',str(output),'-Wl,-rpath,$ORIGIN','-lunicorn','-lm','-o',str(worker)]
  with (output/'compile.log').open('w') as f:r=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT)
  assert r.returncode==0,(output/'compile.log').read_text()
  elf=subprocess.check_output([str(bins/'llvm-readelf'),'-h','-d',str(worker)],text=True);assert 'libunicorn.so' in elf;(output/'worker-elf.txt').write_text(elf);records.append({'abi':abi,'path':str(worker),'sha256':sha(worker),'bytes':worker.stat().st_size,'unicornSha256':sha(output/lib.name),'compileCommand':command,'compileLogSha256':sha(output/'compile.log'),'elfSha256':sha(output/'worker-elf.txt'),'actualRuntimeAccepted':False})
 for n,h in protected.items():assert sha(ROOT/n)==h
 report={'outputs':records,'sourceSha256':{p.name:sha(p) for p in dest.iterdir()},'protectedSixUnchanged':protected,'actualInstalled':False,'scope':'Separate unreferenced candidate ABI binaries using exactly host257 source. No current sixJNI overwrite or app/build/Gradle/source changes. Compilation/ELF dependency only, not device runtime/protocol/performance or ARM acceptance; serial final integration and fresh independent APK/backup/install required.','wholeGoalComplete':False};(DOC/'INSTRUCTION_FIRE_CANDIDATE260.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
if __name__=='__main__':main()
