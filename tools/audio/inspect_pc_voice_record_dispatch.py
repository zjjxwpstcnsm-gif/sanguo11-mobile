#!/usr/bin/env python3
"""Original record+50 -> renderer initialization routing, not rules execution.

Grid unit indices and renderer objects are explicit fixture inputs. Original
570490 reads them; refresh/pose synchronization and final renderer initializer
are bounded captures. Coordinate roles remain raw until upstream proof.
"""
import argparse,hashlib,json,struct,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'content'))
from inspect_pc_scenario_tail import NativeTailDecoder
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_ECX,UC_X86_REG_ESP,UC_X86_REG_EIP
EXE_SHA='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
INITIALIZERS={0x562f70:6,0x563040:7,0x563110:8,0x563e60:27}

def inspect(installation,output):
    if output.exists() or installation.resolve() in output.resolve().parents:raise ValueError('Fresh output outside readonly source required')
    raw=(installation/'san11pk.exe').read_bytes()
    if hashlib.sha256(raw).hexdigest()!=EXE_SHA:raise ValueError('Executable changed')
    d=NativeTailDecoder(raw);d.decode_tail((installation/'Media/scenario/Scenario.s11').read_bytes(),True);u=d.u;u.mem_map(0x6fb0000,0x100000);u.mem_map(0x9500000,0x2000000)
    manager=0x95499b0;record=d.stream+0x100;renderers=[d.stream+0x800,d.stream+0x900];points=[(10,20),(30,40)];calls=[]
    for i,(x,y) in enumerate(points):
        u.mem_write(0x6fb0e70+20*(x*200+y),struct.pack('<H',i));u.mem_write(manager+i*16,struct.pack('<I',renderers[i]))
    def capture(m,a,size,user):
        sp=m.reg_read(UC_X86_REG_ESP);ret=struct.unpack('<I',m.mem_read(sp,4))[0]
        if a==0x56fd60:consumed=0
        elif a==0x56f0c0:consumed=4
        else:
            if m.reg_read(UC_X86_REG_ECX)!=renderers[0] or struct.unpack('<I',m.mem_read(sp+4,4))[0]!=record:raise ValueError('Original source58 renderer/record binding differs')
            calls.append(dict(initializer=hex(a),rendererActionRaw=INITIALIZERS[a],rendererSlotRaw=0,recordPointer=hex(record)));consumed=4
        m.reg_write(UC_X86_REG_EAX,1);m.reg_write(UC_X86_REG_ESP,sp+4+consumed);m.reg_write(UC_X86_REG_EIP,ret)
    for a in [0x56fd60,0x56f0c0]:u.hook_add(UC_HOOK_CODE,capture,begin=a,end=a)
    def forbidden(m,a,size,user):raise ValueError('Forbidden random/command entry '+hex(a))
    for a in [0x402310,0x444150,0x472150,0x4721d0]:u.hook_add(UC_HOOK_CODE,forbidden,begin=a,end=a)
    # Independently execute just the original initializer field stores. Audio,
    # animation updates and any following state-entry callback are not entered.
    stores=[]
    ends={0x562f70:0x562f8b,0x563040:0x563069,0x563110:0x56312b,0x563e60:0x563e7b}
    for start,action in INITIALIZERS.items():
        u.reg_write(UC_X86_REG_ECX,renderers[0]);u.reg_write(UC_X86_REG_ESP,d.stack);u.mem_write(d.stack,struct.pack('<II',d.stop,record));u.emu_start(start,ends[start],count=100)
        if u.reg_read(UC_X86_REG_EIP)!=ends[start] or struct.unpack('<I',u.mem_read(renderers[0]+4,4))[0]!=action:raise ValueError('Original renderer action field store differs')
        stores.append(dict(initializer=hex(start),stop=hex(ends[start]),rendererActionRaw=action))
    for a in INITIALIZERS:u.hook_add(UC_HOOK_CODE,capture,begin=a,end=a)
    rows=[]
    for index in range(32):
        u.mem_write(record,bytes(0x700));u.mem_write(record+0x50,struct.pack('<I',index));u.mem_write(record+0x58,struct.pack('<hh',*points[0]));u.mem_write(record+0x5c,struct.pack('<hh',*points[1]));u.mem_write(record+0x600,struct.pack('<I',0));calls.clear()
        # Stop exactly before the second-coordinate renderer reaction; that
        # phase has additional displacement/effect inputs not supplied here.
        world=bytes(u.mem_read(0x7200000,0x300000));grid=bytes(u.mem_read(0x6fb0000,0x100000));rng=bytes(u.mem_read(0x8a5d44,4));request=bytes(u.mem_read(record,0x700))
        u.reg_write(UC_X86_REG_ECX,manager);u.reg_write(UC_X86_REG_ESP,d.stack);u.mem_write(d.stack,struct.pack('<4I',d.stop,record,0,0));u.emu_start(0x570490,0x57050c,count=100000)
        if u.reg_read(UC_X86_REG_EIP)!=0x57050c:raise ValueError('Source initialization boundary not reached')
        expected={0:6,1:7,2:8,17:27}.get(index)
        if [r['rendererActionRaw'] for r in calls]!=([] if expected is None else [expected]):raise ValueError('Original record index routing differs')
        if world!=bytes(u.mem_read(0x7200000,0x300000)) or grid!=bytes(u.mem_read(0x6fb0000,0x100000)) or rng!=bytes(u.mem_read(0x8a5d44,4)) or request!=bytes(u.mem_read(record,0x700)):raise ValueError('Readonly bounded record dispatch mutated inputs')
        rows.append(dict(record50Raw=index,record58Raw=points[0],record5cRaw=points[1],calls=calls.copy()))
    report=dict(sourceExecutableSha256=EXE_SHA,checks=len(rows)+len(stores),recordRoutes=len(rows),initializerStores=stores,rows=rows,codeSha256={hex(a):hashlib.sha256(bytes(u.mem_read(a,b-a))).hexdigest() for a,b in [(0x570490,0x57050c),*ends.items()]},
        established=['record+50 0/1/2/17 routes to initial renderer actions6/7/8/27; other0..31 indices do not initialize this side here.','Renderer selected from original grid unit slot at record+58; +5c is processed in the following phase, not silently interchanged.'],
        limits=['Stop57050c before second-coordinate reactions; not whole dispatcher or any native rule command.','Refresh/pose/initializer are explicit bounded sink captures; current profile table is independently proved inbatch22.','record50 raw index namespace and coordinate actor/target roles still require upstream record-generation evidence; source tactic-name similarity alone does not close it.','No normal Android voice binding, RNG, Wine or source write.'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,indent=2)+'\n');print('PASS bounded original record voice-init routes=',len(rows))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
