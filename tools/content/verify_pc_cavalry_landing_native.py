#!/usr/bin/env python3
"""Execute original cavalry landing predicate; retain full-battle uncertainty.
The PC installation is read only. No rule callback or return is replaced.
"""
import argparse, hashlib, json, struct, sys
from collections import deque
from pathlib import Path

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--output',required=True,type=Path)
p.add_argument('--inactive-view-filter',action='store_true',help='Explicit predicate fixture input selector=-1; not a full runtime initialization')
a=p.parse_args();out=a.output.resolve()
source=Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版').resolve()
if out==source or source in out.parents:raise ValueError('PC installation is read-only')
out.mkdir(parents=True,exist_ok=False)
sys.path.insert(0,str(Path(__file__).resolve().parent))
import test_pc_city_action_costs as s
from pc_original_pe_data import load_original_data
from unicorn import UC_HOOK_MEM_WRITE,UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EIP

s.CityActionCostsTest.setUpClass();t=s.CityActionCostsTest();d=t.d;u=d.u
mapped=load_original_data(u)
# This grid lies inside original PE.data; preserve its already mapped bytes.
assert len(u.mem_read(0x6fb0000,0x100000))==0x100000
filter_before=struct.unpack('<i',u.mem_read(0x73f550c,4))[0]
if a.inactive_view_filter:u.mem_write(0x73f550c,struct.pack('<i',-1))
flags=[struct.unpack('<I',u.mem_read(0x8a5df8+4*n,4))[0] for n in range(32)]
writes=[];capture=False
trace=deque(maxlen=48)
def code_trace(machine,address,size,user):
    if 0x400000<=address<0x740000:trace.append(hex(address))
u.hook_add(UC_HOOK_CODE,code_trace)
def observe(machine,access,address,size,value,user):
    if capture and not d.stack-0x10000<=address<d.stack+0x10000:writes.append((address,size))
u.hook_add(UC_HOOK_MEM_WRITE,observe)
rows=[]
for x,y in [(0,0),(50,50),(51,50),(199,199),(-1,50),(200,50),(50,-1),(50,200)]:
    for terrain in range(32):
        for occupancy in range(4):
            inside=0<=x<200 and 0<=y<200
            cell=0x6fb0e68+(x*200+y)*20 if inside else None
            if cell is not None:
                original_cell=bytes(u.mem_read(cell,20))
                u.mem_write(cell,struct.pack('<II',occupancy,terrain)+bytes(12))
            before=bytes(u.mem_read(0x7200000,0x300000));grid=bytes(u.mem_read(0x6fb0000,0x100000));rng=bytes(u.mem_read(0x8a5d44,4))
            writes.clear();capture=True
            try:result=t.call(0x594650,((y&65535)<<16)|(x&65535))
            except Exception as error:
                (out/'failure.json').write_text(json.dumps(dict(x=x,y=y,terrain=terrain,occupancy=occupancy,eip=hex(u.reg_read(UC_X86_REG_EIP)),last_instructions=list(trace),terrain_flags=flags,error=str(error)),indent=2)+'\n')
                raise
            capture=False
            expected=int(inside and flags[terrain]!=0 and occupancy==0)
            assert result==expected,(x,y,terrain,occupancy,result,expected)
            assert not writes,('predicate writes',writes)
            assert before==bytes(u.mem_read(0x7200000,0x300000))
            assert grid==bytes(u.mem_read(0x6fb0000,0x100000)) and rng==bytes(u.mem_read(0x8a5d44,4))
            rows.append(dict(x=x,y=y,terrain=terrain,occupancy_low2=occupancy,allowed=result))
            if cell is not None:u.mem_write(cell,original_cell)
assert any(r['allowed'] for r in rows),'No positive landing: runtime terrain table may be uninitialized'
raw=(source/'san11pk.exe').read_bytes();digest=hashlib.sha256(raw).hexdigest();assert digest=='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
report=dict(exe_sha256=digest,entry='594650',source_caller='595724 within595630; native tactic11 dispatch5863aa ->595b90 pushes2',
            predicate_sha256=hashlib.sha256(raw[0x194650:0x1946e3]).hexdigest(),terrain_flag_table=flags,
            explicit_view_filter_input={'field':'73f550c','before_pe_value':filter_before,'value':struct.unpack('<i',u.mem_read(0x73f550c,4))[0],'native_predicate':'67f750 returns field!=-1','full_runtime_initialization':False},
            original_data_mappings=mapped,full_world_grid_rng_unchanged=True,nonstack_writes=0,rows=rows,
            limits=['Explicit selector=-1 input only; selector identity/full initialization and active callback remain unverified',
                    'No owner/friendly-city permission in this predicate; all nonzero occupancy low bits reject',
                    'Source grid population of all city cells, complete hit/damage/collision/failure and target admission remain unverified'])
(out/'native-results.json').write_text(json.dumps(report,indent=2)+'\n')
print('PASS',len(rows),'original cavalry landing predicate; exact world/grid/RNG and no writes; full battle pending')
