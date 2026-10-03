#!/usr/bin/env python3
"""Independent original EXE climate/resolver execution and actual GPU match.

Uses read-only original archive plus captured city/province fields. Private
Unicorn memory is isolated from gameplay. Does not certify raster/LOD/timing.
"""
import argparse,gzip,json,struct
from pathlib import Path
from unicorn import Uc,UC_ARCH_X86,UC_MODE_32,UC_HOOK_MEM_INVALID,UC_HOOK_CODE,UC_PROT_READ,UC_PROT_WRITE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_ECX,UC_X86_REG_ESP,UC_X86_REG_EIP
from pc_resources import Archive,objects,sha
from inspect_pc_effect_bindings import EXE_SHA
ROOT=Path(__file__).resolve().parents[2]

def check(source,output,live_draw,sites=False):
    exe=(source/'san11pk.exe').read_bytes();assert sha(exe)==EXE_SHA
    reference=json.loads((ROOT/'docs/pc-visual/region-climate-live-source.json').read_text());snapshot=(ROOT/reference['snapshot']).read_bytes();assert sha(snapshot)==reference['sha256']
    u=Uc(UC_ARCH_X86,UC_MODE_32);u.mem_map(0x400000,0x500000);u.mem_write(0x400000,exe[:0x500000])
    def fault(engine,access,address,size,value,unused):
        print('Original climate VM invalid memory',hex(engine.reg_read(UC_X86_REG_EIP)),hex(address),size,flush=True);return False
    u.hook_add(UC_HOOK_MEM_INVALID,fault)
    u.mem_map(0x6fb0000,0xd0000);u.mem_map(0x7200000,0x80000)
    for i in range(42):u.mem_write(0x7201958+0x1d8+i*0x248,snapshot[32+i*32:32+(i+1)*32])
    u.mem_write(0x7201958+0x7984c,snapshot[32+42*32:])
    obj=0x10000000;stack=0x20000000;stop=0x30000000
    u.mem_map(obj,4096);u.mem_map(stack,0x10000);u.mem_map(stop,4096)
    # Only the original validator's two OS imports are supported. Execute all
    # source coordinate, province, vtable/type and model-selection code intact.
    pointer_imports={}
    for i,(iat,name,permission) in enumerate(((0x74e268,'IsBadReadPtr',UC_PROT_READ),(0x74e26c,'IsBadWritePtr',UC_PROT_WRITE))):
        offset=struct.unpack_from('<I',exe,iat-0x400000)[0]
        assert exe[offset+2:].split(b'\0',1)[0].decode('ascii')==name
        stub=stop+0x100+i*16;pointer_imports[stub]=(name,permission)
        u.mem_write(stub,b'\xc2\x08\x00');u.mem_write(iat,struct.pack('<I',stub))
    def platform(engine,address,size,unused):
        if address not in pointer_imports:return
        _,permission=pointer_imports[address];sp=engine.reg_read(UC_X86_REG_ESP)
        pointer,length=struct.unpack('<2I',engine.mem_read(sp+4,8))
        valid=length==0
        if length and pointer+length<=0x100000000:
            end=pointer+length;cursor=pointer
            for first,last,permissions in sorted(engine.mem_regions()):
                if first<=cursor<=last and permissions&permission:
                    cursor=min(end,last+1)
                    if cursor==end:valid=True;break
        engine.reg_write(UC_X86_REG_EAX,int(not valid))
    u.hook_add(UC_HOOK_CODE,platform)
    archive=Archive(source/'Media/san11pkres.bin')
    try:rows=[r for r in objects(archive.read(4805)) if (r['model']<8 if sites else r['model']in(46,47,48))];shex=archive.read(4791)
    finally:archive.close()
    asset='pc-sites/sites.pcz'if sites else'pc-scenery/scenery.pcz'
    packed=gzip.decompress((ROOT/'app/src/main/assets/3d'/asset).read_bytes());assert packed[:8]==(b'PCSIT004'if sites else b'PCSCN003');assert struct.unpack_from('<I',packed,8)[0]==len(rows)
    actual_climates=[];details=[]
    for i,row in enumerate(rows):
        # Populate only the region bits read by original41c3a0; the original
        # function performs all integer coordinates, parent and type checks.
        q=int((row['x']*2-112)/4);z=int((row['z']*2-112-2*(q&1))/4)
        if 0<=q<200 and 0<=z<200:
            region=shex[8+(q*200+z)*11+1]&127;u.mem_write(0x6fb0e6c+(q*200+z)*20,struct.pack('<I',region<<5))
        u.mem_write(obj+0x6e,struct.pack('<HH',row['x'],row['z']));u.mem_write(stack+0x8000,struct.pack('<I',stop));u.reg_write(UC_X86_REG_ECX,obj);u.reg_write(UC_X86_REG_ESP,stack+0x8000);u.emu_start(0x41c3a0,stop,count=10000)
        c=u.reg_read(UC_X86_REG_EAX);assert u.reg_read(UC_X86_REG_EIP)==stop and 0<=c<=5,(row['slot'],c)
        if sites:k,x,y,h,ax,az,yaw,converted=struct.unpack_from('<HHHHHHfH',packed,20+128+60+i*18)
        else:k,x,y,h,yaw,converted=struct.unpack_from('<HHHHfH',packed,16+i*14)
        assert (k,x,y,h)==(row['model'],row['x'],row['z'],row['height']) and struct.pack('<f',yaw)==struct.pack('<f',row['yaw']) and c==converted,(row['slot'],c,converted)
        actual_climates.append(c);details.append(dict(slot=row['slot'],kind=k,source_world=[x*10,y*10],climate=c))
    resolver=[]
    for kind in (range(8)if sites else(46,47,48)):
        for quarter in range(4):
            for climate in range(6):
                for state in(range(3 if kind<6 else 2)if sites else(0,)):
                  for lod in range(2):
                    args=(kind,quarter,climate,state,lod)
                    u.mem_write(stack+0x8000,struct.pack('<6I',stop,*args));u.reg_write(UC_X86_REG_ESP,stack+0x8000);u.emu_start(0x41bae0,stop,count=1000)
                    assert u.reg_read(UC_X86_REG_EIP)==stop
                    index=u.reg_read(UC_X86_REG_EAX);rid=struct.unpack_from('<I',exe,0x363740+index*4)[0]
                    entry=dict(kind=kind,quarter=quarter,climate=climate,lod=lod,resource_id=rid)
                    if sites:
                        u.mem_write(stack+0x8000,struct.pack('<6I',stop,*args));u.reg_write(UC_X86_REG_ESP,stack+0x8000);u.emu_start(0x41bbb0,stop,count=1000);assert u.reg_read(UC_X86_REG_EIP)==stop
                        entry.update(state=state,model_index=index,texture_delta=u.reg_read(UC_X86_REG_EAX))
                    resolver.append(entry)
    draw=json.loads(live_draw.read_text())if not sites else dict(records=[]);matched=[];seen=set();by_position={tuple(r['source_world']):r for r in details if r['kind']==47}
    for row in draw['records']:
        ids=[r['resource_id']for r in row['models']];w=row['world_c30'];position=(int(w[0][3]),int(w[2][3]))
        if any(4818<=rid<4830 for rid in ids) and position not in seen:
            seen.add(position);climate=by_position[position]['climate'];assert any(rid in (4818+2*climate,4819+2*climate)for rid in ids)
            assert all(abs(sum(w[r][col]**2 for r in range(3))-1)<1e-5 for col in range(3)),('Unexpected source scale',row['draw'])
            matched.append(dict(draw=row['draw'],position=list(position),climate=climate,resource_ids=ids,world=w))
    assert len(matched)==(0 if sites else 33),'Keep the actual33 reference tree assertions'
    report=dict(status='PASS_ORIGINAL_SITE_CLIMATE_AND_RESOLVERS'if sites else'PASS_ORIGINAL_EXECUTION_AND_ACTUAL_SOURCE_GPU',goal_complete=False,source_exe_sha256=EXE_SHA,source_climate_sha256=sha(snapshot),platform_imports=['IsBadReadPtr','IsBadWritePtr'],platform_boundary='Check private mapped ranges and access permissions only; original game algorithms unchanged',placements=len(details),resolver_cases=len(resolver),actual_tree_matches=len(matched),resolver=resolver,actual_trees=matched,limits=['Independent original code and raw placement binding; complete pixel/material/LOD/timing acceptance remains pending','Actual source33 tree GPU matching applies to scenery mode only','Other openings/MOD initialization not decoded'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:report[k]for k in ('status','placements','resolver_cases','actual_tree_matches')}))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--live-draw',type=Path,default=ROOT/'out/pc-visual/v153/source-draw-models.json');p.add_argument('--sites',action='store_true');a=p.parse_args();check(a.installation,a.output,a.live_draw,a.sites)
