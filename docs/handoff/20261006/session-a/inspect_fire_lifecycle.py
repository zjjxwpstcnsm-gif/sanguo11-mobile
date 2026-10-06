#!/usr/bin/env python3
"""Read exact supplied executable functions; static evidence only, no rule execution."""
import pathlib,hashlib,json,struct
from capstone import Cs,CS_ARCH_X86,CS_MODE_32,CS_GRP_JUMP,CS_GRP_RET,CS_OP_IMM
ROOT=pathlib.Path(__file__).resolve().parents[4];source=pathlib.Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版/san11pk.exe');raw=source.read_bytes();assert hashlib.sha256(raw).hexdigest()=='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
md=Cs(CS_ARCH_X86,CS_MODE_32);md.detail=True;rows=[]
def reachable(entry):
 pending=[entry];seen={};unknown=[]
 while pending:
  address=pending.pop()
  if address in seen:continue
  if not entry<=address<entry+8192:raise ValueError('Unexamined function tail '+hex(address))
  decoded=list(md.disasm(raw[address-0x400000:address-0x400000+15],address,count=1))
  if not decoded:raise ValueError('Invalid source instruction '+hex(address))
  ins=decoded[0];seen[address]=ins
  if ins.group(CS_GRP_RET):continue
  if ins.group(CS_GRP_JUMP):
   if ins.operands and ins.operands[0].type==CS_OP_IMM:pending.append(ins.operands[0].imm)
   else:unknown.append(dict(va=hex(ins.address),op=ins.mnemonic,args=ins.op_str))
   if ins.mnemonic=='jmp':continue
  pending.append(address+ins.size)
 return [seen[x] for x in sorted(seen)],unknown

entries=[0x5a0770,0x5a06c0,0x5afbe0,0x483b40,0x483b20,0x59fea0,0x59fe00,0x414670,0x417880,0x413470,0x417770,0x413d20,0x4133d0,0x413420,0x413510,0x414090,0x413770]
for entry in entries:
 code,unknown=reachable(entry);end=max(i.address+i.size for i in code)-0x400000;at=entry-0x400000
 rows.append({'entry':hex(entry),'endExclusive':hex(end+0x400000),'sourceSha256':hashlib.sha256(raw[at:end]).hexdigest(),'instructions':[dict(va=hex(x.address),bytes=x.bytes.hex(),op=x.mnemonic,args=x.op_str) for x in code],'calls':[dict(va=hex(x.address),target=x.op_str) for x in code if x.mnemonic=='call'],'unresolvedIndirectBranches':unknown,'boundary':'reachable x86 CFG with calls excluded; returns stop, conditional targets/fallthrough followed; indirect jumps explicit'})

report={'source':str(source),'executableSha256':hashlib.sha256(raw).hexdigest(),'sourceReadOnly':True,'runtimeObserved':False,'entries':rows,'frozenWorkerBoundary':{'source':'tools/content/native/pc_effect_scene_probe.c:49','exactSceneTemplates':8,'exactScenePlacements':126,'streamCommands':'type/dt/camera only; no live fire create/remove'},'originalCellEmitterRestored':False,'completeGoal':False}
(ROOT/'docs/handoff/20261006/session-a/FIRE_LIFECYCLE_SOURCE.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
for r in rows:
 print(r['entry'],r['endExclusive']);print('\n'.join(x['va']+' '+x['op']+' '+x['args'] for x in r['instructions']))
