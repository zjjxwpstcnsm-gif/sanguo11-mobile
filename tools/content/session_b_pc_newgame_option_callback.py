#!/usr/bin/env python3
"""Original option button callbacks, real manager export and transfer block.
Declared UI buffers; not original GUI/default/full opening acceptance.
"""
import argparse,json,struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EIP
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output,source_index=0):
 output_guard(installation,output)
 if output.exists()or output.with_suffix('.failure.json').exists():raise ValueError('Preserve receipt')
 d,w,source,geo,_=prepare(installation,source_index);baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));rows=[];stage='labels'
 tables=[]
 for field,offset,control,label in [('difficulty',0x214,0x8b7e44,0x8b7e50),('death',0x218,0x8b7e5c,0x8b7e68),('life',0x22c,0x8b7edc,0x8b7ee8)]:
  ids=struct.unpack('<3i',w.u.mem_read(control,12));ptrs=struct.unpack('<3I',w.u.mem_read(label,12));texts=[]
  for p in ptrs:
   raw=bytes(w.u.mem_read(p,128)).split(b'\0')[0];texts.append(dict(pointer=hex(p),rawHex=raw.hex(),text=raw.decode('big5')))
  tables.append(dict(field=field,uiOffset=offset,controls=list(ids),labels=texts,controlSha=sha(bytes(w.u.mem_read(control,12))),labelTableSha=sha(bytes(w.u.mem_read(label,12)))))
 window=0x16000000;meta=window+0x20000;assert not any(a<=window+0x3ffff and b>=window for a,b,protection in w.u.mem_regions());w.u.mem_map(window,0x40000);originalMeta=json.loads(Path('docs/pc-data/scenario-metadata-audit.json').read_text())['sources'][source_index];assert originalMeta['sha256']==source['sourceSha256'];name=bytes.fromhex(originalMeta['decoded']['name_bytes']);assert len(name)<=16
 w.u.mem_write(meta,bytes(0x400));w.u.mem_write(meta+0x14,name+b'\0')
 try:
  for difficulty in range(3):
   for death in range(3):
    for life in range(3):
     w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,rng);w.u.mem_write(window,bytes(0x12000));w.u.mem_write(window+0x11c00,struct.pack('<i',1));selection=[difficulty,death,life]
     for t,index in zip(tables,selection):
      w.u.mem_write(window+t['uiOffset'],struct.pack('<i',-1));stage='button '+t['field'];w.call(0x545350,t['controls'][index],receiver=window,count=50000000);assert struct.unpack('<i',w.u.mem_read(window+t['uiOffset'],4))[0]==index
     vector=bytes(w.u.mem_read(window+0x214,56));stage='manager export';w.call(0x55c9f0,window+0x214,receiver=0x94109a0,count=50000000);assert vector==bytes(w.u.mem_read(0x9414964,56))
     beforeOptions=bytes(w.u.mem_read(w.root,0x60));fictionFlag=struct.unpack_from('<i',beforeOptions,0x18)[0];args=[0]*26;args[24]=meta;args[25]=0x9414964;stage='original option transfer';w.call(0x4a4354,*args,receiver=0x799895c,stop=0x4a446b,count=50000000)
     afterOptions=bytes(w.u.mem_read(w.root,0x60));actual=[struct.unpack_from('<i',afterOptions,offset)[0]for offset in [0x20,0x24,0x38]];assert actual==[difficulty,death,3 if fictionFlag else life];assert rng==bytes(w.u.mem_read(0x8a5d44,4))
     rows.append(dict(uiChoice=selection,uiVectorHex=vector.hex(),sourceFlag18=fictionFlag,beforeRootHex=beforeOptions.hex(),afterRootHex=afterOptions.hex(),actualRootDifficultyDeathLife=actual,rngPure=True));print('PASS original button/export/transfer',selection,actual,flush=True)
  w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,rng);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
  output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,tables=tables,rows=rows,wholeWorldAndRngRestored=True,limits=['Complete original545350 selected button handlers and full55c9f0 fourteen-field manager export','Original4a4354..4a446b option transfer block, not complete4a42d0 newgame startup or original GUI','Declared active UI/meta buffers; source0 metadata name retained, no source ability/identity/battle/result or RNG overrides','Sourceflag18 dependency and forced rawlife3 branch are separate from three displayed menu choices; PC defaults and other-source normal startup pending']),ensure_ascii=False,indent=2)+'\n');print('PASS original option callbacks',sha(output.read_bytes()),flush=True)
 except Exception as e:
  output.with_suffix('.failure.json').write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,stage=stage,rows=rows,error=repr(e),ip=hex(w.u.reg_read(UC_X86_REG_EIP)),invalid=w.invalid),indent=2)+'\n');raise
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--source-index',type=int,choices=range(16),default=0);a=p.parse_args();inspect(a.installation,a.output,a.source_index)
