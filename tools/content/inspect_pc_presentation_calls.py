#!/usr/bin/env python3
"""Read-only source presentation call graph, with instruction boundary checks.

Static callers and immediate values are evidence of a queue/texture contract,
not guessed gameplay names, Android bindings or timing acceptance.
"""
import argparse, hashlib, json, struct
from pathlib import Path
from capstone import Cs, CS_ARCH_X86, CS_MODE_32
from pc_resources import Archive, wftx
from inspect_pc_effect_bindings import EXE_SHA
ROOT=Path(__file__).resolve().parents[2]
FUNCTIONS={0x414430:0x8a,0x4144c0:0x1a3,0x5888c0:0xfd,0x588de0:0x7b,
           0x589780:0x90,0x5898c0:0xb8}
EXPECTED={0x414430:[0x510f11,0x526e68,0x526ea4,0x58890a],
          0x4144c0:[0x58892b],0x5888c0:[0x588e4d,0x589488],
          0x588de0:[0x589797,0x589802,0x589970],
          0x589780:[0x584ee3,0x592153,0x592fb8,0x596145,0x596c8b,0x59804d,
                     0x59f6d3,0x59fc16,0x5a0976,0x5ce78b,0x5d7c1a]}
def inspect(installation,report):
 exe=(installation/'san11pk.exe').read_bytes()
 if hashlib.sha256(exe).hexdigest()!=EXE_SHA:raise ValueError('Presentation inspection requires fixed supplied EXE')
 md=Cs(CS_ARCH_X86,CS_MODE_32);md.detail=True
 def code(start,end):
  return [dict(va=hex(i.address),bytes=i.bytes.hex(),mnemonic=i.mnemonic,operands=i.op_str)
          for i in md.disasm(exe[start-0x400000:end-0x400000],start)]
 functions=[]
 for va,size in FUNCTIONS.items():
  raw=exe[va-0x400000:va-0x400000+size]
  functions.append(dict(va=hex(va),bytes=len(raw),sha256=hashlib.sha256(raw).hexdigest(),instructions=code(va,va+size)))
 callers={va:[] for va in FUNCTIONS}
 for at in range(0x1000,0x336000):
  if exe[at]!=0xe8:continue
  target=at+0x400005+struct.unpack_from('<i',exe,at+1)[0]
  if target not in callers:continue
  # Recover an aligned source entry using compiler INT3 padding; reject raw
  # byte matches unless sequential decoding reaches this exact CALL boundary.
  first=exe.rfind(b'\xcc\xcc\xcc',max(0x1000,at-0x2000),at)
  if first<0:raise ValueError('Unexamined caller entry')
  first+=3
  while exe[first]==0xcc:first+=1
  instructions=code(first+0x400000,at+0x400005)
  if not instructions or instructions[-1]['va']!=hex(at+0x400000) or instructions[-1]['mnemonic']!='call' or instructions[-1]['operands']!=hex(target):
   raise ValueError('CALL byte match lacks instruction-boundary proof at '+hex(at+0x400000))
  callers[target].append(dict(call_va=hex(at+0x400000),entry_candidate=hex(first+0x400000),
     caller_prefix_sha256=hashlib.sha256(exe[first:at+5]).hexdigest(),
     context=instructions[-28:],runtime_observed=False))
 for va,expected in EXPECTED.items():
  if [int(r['call_va'],16) for r in callers[va]]!=expected:raise ValueError('Source call graph changed '+hex(va))
 archive=Archive(installation/'Media/san11pkres.bin')
 try:
  rows=[]
  for texture in range(347):
   template,top=struct.unpack_from('<II',exe,0x3774e0+texture*8)
   raw=archive.read(369+texture);frames=wftx(raw)
   rows.append(dict(texture_index=texture,resource_id=369+texture,source_sha256=hashlib.sha256(raw).hexdigest(),
     effect_index=template,effect_resource_id=struct.unpack_from('<I',exe,0x37692c+template*12+4)[0],destination_top=top,
     rgba_sha256=[hashlib.sha256(im.convert('RGBA').tobytes()).hexdigest() for im in frames],
     android_binding=None,gameplay_cause='unresolved'))
 finally:archive.close()
 result=dict(schema=1,goal_complete=False,status='STATIC_QUEUE_AND_DYNAMIC_TEXTURE_CONTRACT_CONFIRMED',
   executable_sha256=EXE_SHA,source_policy='readonly original EXE/archive; no live code or authority patches',
   functions=functions,callers={hex(k):v for k,v in callers.items()},textures=rows,
   queue_contract=dict(builder='0x588de0',opcode=18,payload=dict(matrix='64 bytes copied from argument2 to payload+0',
      selector='argument1 to payload+0x40',dynamic='argument3 to payload+0x44',wait='argument4 to payload+0x48'),
      wrapper='0x589780: dynamic=1, selector and wait supplied by caller; original443790 supplies matrix',
      playback='0x5888c0: dynamic!=0 and selector<347 ->414430;347..406->4144c0;dynamic==0->414280',
      template='414430/4144b2 ->413d20 using presentation table[selector].first_word'),
   limits=['Immediate values and source call sites do not establish gameplay labels or actual trigger success',
     'Wait values are source arguments; exact complete presentation duration/camera ordering requires live reference',
     'No original fullscreen playback added to Android by this inspector',
     'Extended347..406 selectors use4144c0 substitutions; not direct archive369+selector'])
 report.parent.mkdir(parents=True,exist_ok=True);report.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
 print(json.dumps(dict(status=result['status'],functions=len(functions),verified_calls=sum(map(len,callers.values())),textures=len(rows))))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path)
 p.add_argument('--report',type=Path,default=ROOT/'docs/pc-visual/presentation-calls-source-working.json')
 a=p.parse_args();inspect(a.installation,a.report)
