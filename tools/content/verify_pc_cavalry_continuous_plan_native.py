#!/usr/bin/env python3
"""Execute continuous original cavalry landing and survival planning before commit.

This stops before hit/damage/authority/presentation commit. The building pointer
registry and occupied cells are controlled fixture inputs, not proof that the
original runtime registered all seven city cells. No instruction or callback is
replaced. PC files are read only.
"""
import argparse, gzip, hashlib, json, struct, sys
from collections import deque
from pathlib import Path

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--output',required=True,type=Path)
a=p.parse_args()
source=Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版').resolve()
out=a.output.resolve()
if out==source or source in out.parents:raise ValueError('PC installation is read only')
out.mkdir(parents=True,exist_ok=False)
sys.path.insert(0,str(Path(__file__).resolve().parent))
import test_pc_city_action_costs as support
from pc_original_pe_data import load_original_data
from unicorn import UC_HOOK_CODE, UC_HOOK_MEM_WRITE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_EDX,UC_X86_REG_ESI,UC_X86_REG_ESP,UC_X86_REG_ECX,UC_X86_REG_EIP,UC_X86_REG_EDI

support.CityActionCostsTest.setUpClass()
t,d=support.CityActionCostsTest(),support.CityActionCostsTest.d
u=d.u
mappings=load_original_data(u)
raw=(source/'san11pk.exe').read_bytes()
exe_sha=hashlib.sha256(raw).hexdigest()
assert exe_sha=='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
shared_sha=hashlib.sha256((source/'Media/scenario/Scenario.s11').read_bytes()).hexdigest()
assert shared_sha=='dfca0316e019454e77bab5805e3d725ae35e34408e1699e869e5419e2f5fad6f'
pack=lambda x,y:((y&65535)<<16)|(x&65535)
def coords(value):return list(struct.unpack('<hh',struct.pack('<I',value)))
def word(address,value):u.mem_write(address,struct.pack('<I',value&0xffffffff))
def read(address):return struct.unpack('<I',u.mem_read(address,4))[0]
# Same explicit inactive selector as the separately verified predicate fixture.
word(0x73f550c,-1)
flags=[read(0x8a5df8+4*n) for n in range(32)]
good=next(n for n,v in enumerate(flags) if v)
bad=next(n for n,v in enumerate(flags) if not v)
building=d.root+0x89730
u.reg_write(UC_X86_REG_ECX,building);t.call(0x4880a0);word(building+8,0)
unit=d.root+0x169730;officer=d.root+0xc0bc
u.reg_write(UC_X86_REG_ECX,officer);t.call(0x489f10);word(officer+0xa0,0)
u.reg_write(UC_X86_REG_ECX,unit);t.call(0x496d90);word(unit+8,1);word(unit+0xc,0)
assert t.call(0x47a630,unit)==1
# Actual 483b20 registry lookup reads this pointer; actual 487ab0 then reads the
# constructor-created building and source template. No predicate result stub.
registry_before=read(0x46483b4);word(0x46483b4,building)
frame=d.stack-0x400;context=d.stack+0x200;out_flag=d.stack+0x300;neighbor_out=d.stack+0x400
trace=deque(maxlen=48);capture=False;writes=[]
def tracing(machine,address,size,user):
    if 0x400000<=address<0x740000:trace.append(hex(address))
def writing(machine,access,address,size,value,user):
    if capture and not d.stack-0x10000<=address<d.stack+0x10000:writes.append([hex(address),size])
u.hook_add(UC_HOOK_CODE,tracing);u.hook_add(UC_HOOK_MEM_WRITE,writing)
def neighbor(hexagon,direction):
    t.call(0x483a50,neighbor_out,hexagon,direction);return read(neighbor_out)
def cell(hexagon):
    x,y=coords(hexagon);assert 0<=x<200 and 0<=y<200
    return 0x6fb0e68+(x*200+y)*20
rows=[]
for center in [(50,50),(51,50)]:
    for direction in range(6):
        chain=[pack(*center)]
        for _ in range(4):chain.append(neighbor(chain[-1],direction))
        for steps in [1,2]:
            for main_damage in [1000,5000]:
                for blocker,at in [('none',0),('unit',1),('unit',2),('city',1),('city',2),('terrain',1),('terrain',2)]:
                    original_cells={cell(h):bytes(u.mem_read(cell(h),20)) for h in chain}
                    for address in original_cells:u.mem_write(address,struct.pack('<II',0,good)+bytes(12))
                    if at:
                        occupancy={'unit':1,'city':2,'terrain':0}[blocker]
                        terrain=bad if blocker=='terrain' else good
                        u.mem_write(cell(chain[1+at]),struct.pack('<II',occupancy,terrain)+bytes(12))
                    u.mem_write(cell(chain[1]),struct.pack('<II',1,good)+bytes(12));u.mem_write(unit+0x18,struct.pack('<H',5000))
                    u.mem_write(context,bytes(0x80));word(context+0xc,main_damage);word(context+0x58,chain[0]);word(context+0x5c,chain[1])
                    u.mem_write(frame,bytes(0x80));word(frame+0x20,1);word(frame+0x38,steps);word(frame+0x3c,out_flag);word(out_flag,7)
                    # Entry 5956d6 follows the previous visibility predicate/local store.
                    # Explicit visibility local1 suppresses the presentation branch.
                    u.reg_write(UC_X86_REG_ESP,frame);u.reg_write(UC_X86_REG_ESI,context)
                    u.reg_write(UC_X86_REG_EAX,chain[0]);u.reg_write(UC_X86_REG_EDX,chain[1])
                    before=bytes(u.mem_read(0x7200000,0x300000));grid=bytes(u.mem_read(0x6fb0000,0x100000));rng=bytes(u.mem_read(0x8a5d44,4));request=bytes(u.mem_read(context,0x80))
                    writes.clear();trace.clear();capture=True
                    try:u.emu_start(0x5956d6,0x59588d,count=10000)
                    except Exception as error:
                        (out/'failure.json').write_text(json.dumps(dict(center=center,direction=direction,steps=steps,blocker=blocker,at=at,eip=hex(u.reg_read(UC_X86_REG_EIP)),last_instructions=list(trace),error=str(error)),indent=2)+'\n');raise
                    finally:capture=False
                    assert u.reg_read(UC_X86_REG_EIP)==0x59588d
                    moved=steps if not at or at>steps else at-1
                    defender=u.reg_read(UC_X86_REG_EDI);attacker=read(frame+0x10)
                    assert defender==chain[1+moved] and attacker==(chain[1] if at and at<=steps and main_damage>=5000 else chain[moved]),(center,direction,steps,blocker,at,coords(defender),coords(attacker),moved)
                    blocked=read(frame+0x28);assert blocked==int(bool(at and at<=steps))
                    assert read(out_flag)==7,'Ordinary city is not the special-building out-flag branch'
                    assert not writes and before==bytes(u.mem_read(0x7200000,0x300000))
                    assert grid==bytes(u.mem_read(0x6fb0000,0x100000)) and rng==bytes(u.mem_read(0x8a5d44,4))
                    expected_request=bytearray(request);struct.pack_into('<II',expected_request,0x74,attacker,defender)
                    assert bytes(expected_request)==bytes(u.mem_read(context,0x80)),'Only actual planned coordinate result fields written'
                    rows.append(dict(attacker=coords(chain[0]),defender=coords(chain[1]),direction=direction,steps=steps,blocker=blocker,block_at=at,actual_attacker_landing=coords(attacker),actual_defender_landing=coords(defender),blocked_local=blocked,actual_successful_steps=moved,explicit_main_damage=main_damage,target_troops=5000,lethal_follow_override=bool(at and at<=steps and main_damage>=5000),whole_world_grid_rng_unchanged=True))
                    for address,data in original_cells.items():u.mem_write(address,data)
report=dict(exe_sha256=exe_sha,shared_sha256=shared_sha,entry='5956d6',stop_before='59588d',block_sha256=hashlib.sha256(raw[0x1956d6:0x19588d]).hexdigest(),original_data_mappings=mappings,
            explicit_inputs=dict(inactive_view_selector=-1,visibility_local_suppresses_presentation=1,building_registry_address='46483b4',building_registry_before=registry_before,building_registry_fixture=hex(building),actual_building_constructor='4880a0',native_building_template=0,terrain_allowed_index=good,terrain_rejected_index=bad),
            rows=rows,planned_result_coordinates_written=True,nonstack_writes=0,limits=['Continuous geometry/landing/backstep and lethal-survival planning; stop59588d after planned result coordinate fields and before5b1870 authority application','Controlled single occupied city cells; separate R37 source seven-cell registration exists, not combined here','Main damage is an explicit input; original generation, application, full counter/chain/duel and RNG across whole command unverified','Visibility local1 is explicit and branches around original presentation; preceding5a06d0 not executed; no instructions or callbacks replaced'])
assert len(rows)==336
encoded=(json.dumps(report,indent=2)+'\n').encode('utf-8')
(out/'cavalry-continuous-plan-native.json').write_bytes(encoded)
with (out/'cavalry-continuous-plan-native.json.gz').open('wb') as file:
    with gzip.GzipFile(filename='',mode='wb',fileobj=file,mtime=0) as packed:packed.write(encoded)
print('PASS',len(rows),'original continuous landing/survival loop: both coordinate parities, six directions, one/two steps, unit/city/terrain stops; full world/grid/RNG unchanged; full command pending')
