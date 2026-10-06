#!/usr/bin/env python3
"""Readonly supplied EXE named fire/extinguish handlers; do not label raw queues as recovered emitters."""
import pathlib,hashlib,struct,json
from capstone import Cs,CS_ARCH_X86,CS_MODE_32
ROOT=pathlib.Path(__file__).resolve().parents[4];path=pathlib.Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版/san11pk.exe');raw=path.read_bytes();digest=hashlib.sha256(raw).hexdigest();assert digest=='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
md=Cs(CS_ARCH_X86,CS_MODE_32);rows=[]
for index,expected,branch,entry,end in [(0,'火計',0x593460,0x591270,0x5915e0),(1,'滅火',0x59347c,0x5915e0,0x5917d0)]:
 ptr=struct.unpack_from('<I',raw,0x4aecb4+index*4)[0];name=raw[ptr-0x400000:ptr-0x400000+64].split(b'\0')[0].decode('big5');assert name==expected
 dispatch=list(md.disasm(raw[branch-0x400000:branch-0x400000+32],branch));assert next(i for i in dispatch if i.mnemonic=='call').op_str==hex(entry)
 code=list(md.disasm(raw[entry-0x400000:end-0x400000],entry));calls=[]
 for i,ins in enumerate(code):
  if ins.mnemonic=='call':calls.append({'callVa':hex(ins.address),'target':ins.op_str,'argumentContext':[dict(va=hex(x.address),bytes=x.bytes.hex(),op=x.mnemonic,args=x.op_str) for x in code[max(0,i-12):i+1]]})
 rows.append({'sourcePlotId':index,'sourceName':name,'dispatchBranch':hex(branch),'entry':hex(entry),'endExclusive':hex(end),'handlerSha256':hashlib.sha256(raw[entry-0x400000:end-0x400000]).hexdigest(),'staticCalls':calls,'runtimeObserved':False})
helpers=[]
for entry,end in [(0x5887e0,0x58884a),(0x589810,0x589840),(0x588fc0,0x589004),(0x592440,0x592740)]:
 code=list(md.disasm(raw[entry-0x400000:end-0x400000],entry));helpers.append({'entry':hex(entry),'endExclusive':hex(end),'sha256':hashlib.sha256(raw[entry-0x400000:end-0x400000]).hexdigest(),'instructions':[dict(va=hex(x.address),bytes=x.bytes.hex(),op=x.mnemonic,args=x.op_str) for x in code],'semantics':'static raw helper, not recovered cell emitter'})
report={'helpers':helpers,'sourceExecutable':str(path),'sourceSha256':digest,'sourceReadOnly':True,'wineUsed':False,'rows':rows,'limits':['named normal command dispatch and static caller/arguments only','persistent cell-fire controller/instance/material/coordinates/lifetime not yet proved','no original emitter added or accepted by this source inspector'],'completeGoal':False}
(ROOT/'docs/handoff/20261006/session-a/PC_FIRE_CALLS.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({'sourceSha256':digest,'rows':[{'sourceName':x['sourceName'],'calls':[(c['callVa'],c['target']) for c in x['staticCalls']]} for x in rows]},ensure_ascii=False))
