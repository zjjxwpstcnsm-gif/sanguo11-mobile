#!/usr/bin/env python3
"""All16 independently serialized force tech bits via untouched4811e0."""
import argparse,gzip,json,struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_ECX,UC_X86_REG_ESP,UC_X86_REG_EIP
from inspect_pc_scenario_domains import NativeDomainDecoder
from audit_pc_restoration_sources import ROOT,EXE_SHA,sha,output_guard
def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier source receipt')
 exe=(installation/'san11pk.exe').read_bytes();assert sha(exe)==EXE_SHA
 domains_path=ROOT/'docs/pc-data/scenario-domains-native.json.gz';raw=domains_path.read_bytes();domains=json.loads(gzip.decompress(raw));assert domains['source_executable_sha256']==EXE_SHA
 manifest_raw=(ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes();manifest=json.loads(manifest_raw)['scenarios'];by_path={x['path']:x for x in domains['sources']};d=NativeDomainDecoder(exe);rows=[];checks=0
 for index,src in enumerate(manifest):
  source=(installation/src['sourcePath']).read_bytes();assert sha(source)==src['sourceSha256'];record=by_path[src['sourcePath']];assert record['sha256']==sha(source);forces=[]
  for native in range(47):
   f=next(x for x in record['records']if x['kind']=='force'and x['native_index']==native);assert sha(source[f['offset']:f['offset']+f['bytes']])==f['sha256'];actor=bytes.fromhex(f['actor_hex']);assert len(actor)==0x12c
   # Evidence is exact serializer bytes, not new-game controller activation.
   for off in [0x58,0x5c]:
    reads=[x for x in f['reads']if x['destination']=='actor+'+hex(off)];assert len(reads)==1 and reads[0]['bytes']==4 and source[reads[0]['offset']:reads[0]['offset']+4]==actor[off:off+4]
   pointer=d.root+0x7af8+native*0x12c;d.u.mem_write(pointer,actor);before=bytes(d.u.mem_read(pointer,0x12c));bits=struct.unpack_from('<Q',actor,0x58)[0];values=[]
   for tech in range(-1,37):
    d.u.reg_write(UC_X86_REG_ECX,pointer);d.u.reg_write(UC_X86_REG_ESP,d.stack);d.u.mem_write(d.stack,struct.pack('<II',d.stop,tech&0xffffffff));d.u.emu_start(0x4811e0,d.stop,count=10000);assert d.u.reg_read(UC_X86_REG_EIP)==d.stop;actual=d.u.reg_read(UC_X86_REG_EAX);assert actual==int(0<=tech<36 and bool(bits&(1<<tech)));assert before==bytes(d.u.mem_read(pointer,0x12c));values.append(actual);checks+=1
   forces.append(dict(nativeId=native,recordSha=f['sha256'],recordOffset=f['offset'],rawBits=str(bits),activeRuler=struct.unpack_from('<i',actor,4)[0],learned=[n for n in range(36)if values[n+1]],getterValues=values))
  rows.append(dict(sourceIndex=index,source=src,forces=forces));print('PASS original force tech source',index,checks,flush=True)
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,domainsSha=sha(raw),manifestSha=sha(manifest_raw),rows=rows,checks=checks,actorPure=True,limits=['Exact original serialized actor58 bits and untouched4811e0; not a full postload/event/controller initializer','47force slots retained; source valid ruler/player activation must remain separate','No original research fee/delayed completion/ordinary APK claim'],completeGoal=False),indent=2)+'\n');print('PASS original force technologies',checks,sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
