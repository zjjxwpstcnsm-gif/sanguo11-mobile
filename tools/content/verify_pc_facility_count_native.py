#!/usr/bin/env python3
"""Execute original facility matching/count queries; no construction/UI claim."""
import argparse,gzip,hashlib,json,struct,sys
from pathlib import Path

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',required=True,type=Path);a=p.parse_args()
 root=Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版').resolve();out=a.output.resolve()
 if out==root or root in out.parents:raise ValueError('PC source is read only')
 out.mkdir(parents=True,exist_ok=False)
 sys.path.insert(0,str(Path(__file__).resolve().parent))
 from test_pc_city_action_costs import CityActionCostsTest
 from pc_original_pe_data import load_original_data
 from unicorn.x86_const import UC_X86_REG_ECX
 CityActionCostsTest.setUpClass();t=CityActionCostsTest();d=t.d;u=d.u
 mappings=load_original_data(u);building,city,district,facility=t.actors()
 word=lambda addr,value:u.mem_write(addr,struct.pack('<I',value&0xffffffff))
 read=lambda addr:struct.unpack('<I',u.mem_read(addr,4))[0]
 base=0x7200000;size=0x300000;complete=d.stack+0x400;incomplete=complete+4
 audit=json.loads((Path(__file__).resolve().parents[2]/'docs/pc-data/facility-costs-native.json').read_text())
 exe=(root/'san11pk.exe').read_bytes();shared=(root/'Media/scenario/Scenario.s11').read_bytes()
 assert hashlib.sha256(exe).hexdigest()==audit['executable_sha256']
 assert hashlib.sha256(shared).hexdigest()==audit['source_sha256']
 groups={31:{31,50,51},32:{32,52,53},33:{33,54,55},34:{34,56,57},35:{35,58,59}}
 matches=[];counts=[]
 def checked(address,*args):
  before=bytes(u.mem_read(base,size));rng=bytes(u.mem_read(0x8a5d44,4));value=t.call(address,*args)
  assert before==bytes(u.mem_read(base,size)),hex(address)
  assert rng==bytes(u.mem_read(0x8a5d44,4)),hex(address)
  return value
 for actual in range(64):
  word(facility+8,actual)
  for requested in range(64):
   value=checked(0x49dd30,facility,requested)
   expected=int(actual in groups.get(requested,{requested}))
   assert value==expected,(actual,requested,value,expected)
   matches.append(dict(actual=actual,requested=requested,match=bool(value)))
 # Exercise every native ID, both query modes and all declared field14 inputs.
 word(city+0xe8,30)
 for actual in range(64):
  word(facility+8,actual)
  requested=next((key for key,values in groups.items() if actual in values),actual)
  for field14 in [0,1,1000,0xffffffff]:
   word(facility+0x14,field14)
   for grouped in [0,1]:
    u.mem_write(city+0xf4,bytes(30*8));word(city+0xf4,facility)
    word(complete,0xdeadbeef);word(incomplete,0xdeadbeef)
    value=checked(0x4c0b60,city,requested,complete,incomplete,grouped)
    expected=int(actual==requested or grouped and actual in groups.get(requested,{requested}))
    assert (value,read(complete),read(incomplete))==(expected,expected if field14 else 0,0 if field14 else expected)
    assert checked(0x49dab0,city,requested)==int(actual==requested and field14!=0)
    counts.append(dict(actual=actual,requested=requested,field14=field14,grouped=bool(grouped),slot=0,total=value,nonzero14=read(complete),zero14=read(incomplete),exact_presence=actual==requested and field14!=0))
 # Presence query scans30 slots; count query scans exactly city+e8 entries.
 word(facility+8,41);word(facility+0x14,1)
 for slot in range(30):
  u.mem_write(city+0xf4,bytes(30*8));word(city+0xf4+8*slot,facility)
  for count in [slot,slot+1,30]:
   word(city+0xe8,count)
   value=checked(0x4c0b60,city,41,complete,incomplete,0)
   assert (value,read(complete),read(incomplete))==(int(slot<count),int(slot<count),0)
   assert checked(0x49dab0,city,41)==1
   counts.append(dict(actual=41,requested=41,field14=1,grouped=False,slot=slot,city_count=count,total=value,nonzero14=read(complete),zero14=read(incomplete),presence_scans_all30=True))
 # Three distinct valid actors, mixed field14, empty slots, null outputs.
 actors=[]
 for index,field14 in enumerate([0,1,1000]):
  actor=building+(90+index)*0x38;u.reg_write(UC_X86_REG_ECX,actor);t.call(0x4880a0)
  word(actor+8,41);word(actor+0x14,field14);actors.append(actor)
 u.mem_write(city+0xf4,bytes(30*8));word(city+0xe8,30)
 for slot,actor in zip([0,15,29],actors):word(city+0xf4+slot*8,actor)
 for outputs in [(complete,incomplete),(0,incomplete),(complete,0),(0,0)]:
  assert checked(0x4c0b60,city,41,*outputs,0)==3
  if outputs[0]:assert read(complete)==2
  if outputs[1]:assert read(incomplete)==1
 assert len(matches)==4096 and len(counts)==602
 report=dict(executable_sha256=audit['executable_sha256'],shared_sha256=audit['source_sha256'],source_records=audit['records'],original_data_mappings=mappings,
  functions=[dict(start=hex(start),end=hex(end),bytes_hex=exe[start-0x400000:end-0x400000].hex()) for start,end in [(0x49dd30,0x49dded),(0x4c0b60,0x4c0c2e),(0x49dab0,0x49db0e)]],
  matches=matches,counts=counts,mixed_null_output_cases=4,whole_world_readonly=True,rng_unchanged=True,
  limits=['field14 zero/nonzero is an explicit input; construction transition and full durability semantics unverified','Count query uses city+e8, presence query scans30; valid fixture actors only','No construction admission/progress, Android integration, UI, official opening or active external patch claim'])
 raw=(json.dumps(report,ensure_ascii=False,indent=2)+'\n').encode();(out/'facility-count-native.json').write_bytes(raw)
 with (out/'facility-count-native.json.gz').open('wb') as f:
  with gzip.GzipFile(filename='',fileobj=f,mode='wb',mtime=0) as z:z.write(raw)
 print('PASS facility matching4096, count602, mixed/null4; original queries;3MiB/RNG unchanged')

if __name__=='__main__':main()
