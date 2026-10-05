#!/usr/bin/env python3
"""Bounded original force technique gain; no Wine, admission or UI emulation."""
from pathlib import Path
import argparse,hashlib,json,struct,sys
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',required=True,type=Path);a=p.parse_args();out=a.output.resolve()
source=Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版').resolve()
if out==source or source in out.parents:raise ValueError('PC installation is read-only')
out.mkdir(parents=True,exist_ok=False)
sys.path.insert(0,str(Path(__file__).resolve().parent))
import test_pc_city_action_costs as s
from pc_original_pe_data import load_original_data
from unicorn import UC_HOOK_MEM_WRITE
from unicorn.x86_const import UC_X86_REG_ECX,UC_X86_REG_EIP
s.CityActionCostsTest.setUpClass();t=s.CityActionCostsTest();d=t.d;u=d.u;load_original_data(u)
force=d.root+0x7af8;u.reg_write(UC_X86_REG_ECX,force);t.call(0x481830);u.mem_write(force+4,struct.pack('<i',0));u.mem_write(force+0x60,struct.pack('<i',-1))
base=bytes(u.mem_read(0x7200000,0x300000));writes=[];capture=False

def observe(machine,access,address,size,value,user):
 if capture and not d.stack-0x10000<=address<d.stack+0x10000:writes.append((address,size))
u.hook_add(UC_HOOK_MEM_WRITE,observe);rows=[]
for old in [0,1,99,5000,9999,10000]:
 for request in [-20000,-10000,-100,-1,0,1,2,3,9,10,11,100,10000,20000]:
  u.mem_write(0x7200000,base);u.mem_write(force+0xa2,struct.pack('<H',old));before=bytearray(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4));writes.clear();capture=True
  actual=t.call(0x4b6460,force,request&0xffffffff,0);capture=False
  applied=max(1,request//2) if request>0 else request;expected=max(0,min(10000,old+applied));delta=expected-old
  assert actual==delta&0xffffffff,(old,request,actual,delta)
  assert struct.unpack('<H',u.mem_read(force+0xa2,2))[0]==expected
  before[force+0xa2-0x7200000:force+0xa4-0x7200000]=struct.pack('<H',expected)
  assert bytes(before)==bytes(u.mem_read(0x7200000,0x300000)),(old,request,'unexpected world change')
  assert rng==bytes(u.mem_read(0x8a5d44,4));assert all(force+0xa2<=v and v+n<=force+0xa4 for v,n in writes),(old,request,writes)
  rows.append(dict(before=old,requested=request,after=expected,actual_delta=delta))
result=dict(source_exe_sha256=hashlib.sha256((source/'san11pk.exe').read_bytes()).hexdigest(),entry='4b6460',scope='original valid force rule function on a source-constructed force; absent live UI; production admission, full boot and HUD callbacks not executed',field='force+0xa2 WORD',full_3mib_guard=True,rng_unchanged=True,rows=rows)
assert result['source_exe_sha256']=='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
(out/'native-results.json').write_text(json.dumps(result,indent=2)+'\n');print('PASS',len(rows),'original signed technique gains, clamps, exact world delta and RNG')
