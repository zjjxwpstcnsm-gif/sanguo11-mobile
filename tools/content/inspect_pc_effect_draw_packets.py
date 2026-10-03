#!/usr/bin/env python3
"""Observe native evaluated effect packets before GPU calls.

Original457b00/child render methods execute, including source camera clipping.
A memory-recording callback replaces only the final draw-queue boundary.
Explicit diagnostic view/projection inputs are NOT a captured PC camera, and
these packets do not constitute Android integration or rasterization evidence.
"""
import argparse
import json
import math
from pathlib import Path
import struct
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ESP,UC_X86_REG_ECX,UC_X86_REG_EAX,UC_X86_REG_EIP
from pc_effect_machine import SourceEffectMachine,VerifiedSourceExecutable
from pc_resources import Archive,sha,effects

ROOT=Path(__file__).resolve().parents[2]

class DrawPacketObserver:
    def __init__(self,machine,scale,center=(0,0,0),native_queue=False):
        if not math.isfinite(scale) or not 0<scale<=1:raise ValueError('Diagnostic projection scale')
        self.machine=machine;u=machine.u
        # Separate mapped memory: never overwrite loaded/evaluated source state.
        self.base=0x31000000;u.mem_map(self.base,0x10000)
        self.queue=self.base;self.table=self.base+0x100;self.camera=self.base+0x1000
        self.callback=0x458650 if native_queue else machine.STOP+0x200;self.packets=[]
        u.mem_write(self.queue,struct.pack('<I',self.table))
        u.mem_write(self.table+4,struct.pack('<I',self.callback))
        identity=struct.pack('<16f',1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1)
        # Source renderer reads view+40 and projection+180; native billboard
        # consumers also read camera basis+140 and inverse-view+ c0.
        for offset in (0x40,0xc0,0x140):u.mem_write(self.camera+offset,identity)
        view=list(struct.unpack('<16f',identity));view[12:15]=[-float(v)for v in center]
        u.mem_write(self.camera+0x40,struct.pack('<16f',*view))
        inverse=list(struct.unpack('<16f',identity));inverse[12:15]=[float(v)for v in center]
        for offset in (0xc0,0x140):u.mem_write(self.camera+offset,struct.pack('<16f',*inverse))
        u.mem_write(self.camera,struct.pack('<4f',*center,1))
        # Native45a436 binds slot1 to the containing scene's camera provider.
        # Its source vtable795190/46d820 reads camera XYZ and view basis. Without
        # this binding, original radial/sector conditions compare world-space
        # particles to the origin and suppress valid SEFF placements before draw.
        self.provider=self.base+0x2000
        u.mem_write(self.provider,struct.pack('<2I',0x795190,self.camera))
        projection=struct.pack('<16f',scale,0,0,0,0,scale,0,0,0,0,scale,0,0,0,.5,1)
        u.mem_write(self.camera+0x180,projection)
        self.inputs=dict(view_hex=struct.pack('<16f',*view).hex(),projection_hex=projection.hex(),scale=scale,
            source_center=list(center),camera_provider='source795190/46d820; native457bd0 slot1',
            status='explicit diagnostic input, not captured PC view/projection')
        self.hook=u.hook_add(UC_HOOK_CODE,self._record)

    def _record(self,u,address,size,user):
        if address!=self.callback:return
        sp=u.reg_read(UC_X86_REG_ESP);ret,payload,depth_bits,flags=struct.unpack('<4I',u.mem_read(sp,16))
        if u.reg_read(UC_X86_REG_ECX)!=self.queue:raise ValueError('Unexamined queue callback owner')
        primitive=struct.unpack('<I',u.mem_read(payload,4))[0]
        if primitive>7:raise ValueError('Unexamined primitive packet')
        length=0x10 if primitive in (4,5) else 0x14 if primitive in (6,7) else 0x70
        raw=bytes(u.mem_read(payload,length))
        self.packets.append(dict(primitive=primitive,pointer=hex(payload),depth_bits=hex(depth_bits),flags=flags,
            sha256=sha(raw),raw_hex=raw.hex(),prefix_bytes=length,
            complete_quad_contract=primitive==2,
            selector=struct.unpack_from('<H',raw,8)[0],blend=struct.unpack_from('<H',raw,4)[0]))
        if self.callback!=0x458650:
            u.reg_write(UC_X86_REG_EAX,1);u.reg_write(UC_X86_REG_ESP,sp+16);u.reg_write(UC_X86_REG_EIP,ret)

    def draw(self):
        self.packets=[]
        self.machine.call(0x457b00,self.machine.instance,self.queue,self.camera)
        return list(self.packets)

    def close(self):self.machine.u.hook_del(self.hook)


def inspect(installation,resources,steps,scale,output,seff_slots=None):
    exe=(installation/'san11pk.exe').read_bytes();verified=VerifiedSourceExecutable(exe);archive=Archive(installation/'Media/san11pkres.bin');results=[]
    try:
        jobs=[(resource,None)for resource in resources]
        if seff_slots is not None:
            placements=effects(archive.read(4792));jobs=[]
            for slot in seff_slots:
                if not 0<=slot<len(placements):raise ValueError('SEFF source slot')
                placement=placements[slot]
                resource=struct.unpack_from('<I',exe,0x37692c+placement['effect']*12+4)[0]
                if not 125<=resource<=368:raise ValueError('Source SEFF effect-table resource')
                jobs.append((resource,placement))
        for resource,placement in jobs:
            raw=archive.read(resource);machine=SourceEffectMachine(verified);observer=None
            row=dict(resource_id=resource,source_sha256=sha(raw),frames=[],android_output=None,runtime_binding=None)
            try:
                matrix=None;center=(0,0,0)
                if placement is not None:
                    native_row=machine.STOP+0x500;destination=machine.STOP+0x600
                    machine.u.mem_write(native_row,struct.pack('<4f',placement['x'],placement['y'],placement['z'],placement['yaw']))
                    machine.call(0x413a80,0,destination,native_row)
                    matrix=bytes(machine.u.mem_read(destination,64));center=(placement['x'],placement['y'],placement['z'])
                    row.update(seff_placement=placement,start_matrix_hex=matrix.hex(),start_matrix_sha256=sha(matrix),source_placement_function='413a80')
                observer=DrawPacketObserver(machine,scale,center)
                machine.load(raw,scene_camera_provider=observer.provider);machine.start(matrix)
                for dt in steps:
                    machine.update(dt);before=machine.snapshot();packets=observer.draw();after=machine.snapshot()
                    # Drawing may update native scratch/queue pointers. Preserve
                    # both states instead of pretending it is always read-only.
                    row['frames'].append(dict(input_dt=dt,before=before,after=after,packets=packets))
                row.update(status='SOURCE_DRAW_PACKET_OBSERVED',camera_inputs=observer.inputs)
            except Exception as e:
                row.update(status='UNRESOLVED',reason=str(e),trace=list(machine.trace))
            finally:
                if observer is not None:observer.close()
                machine.close()
            results.append(row);print(json.dumps(dict(resource=resource,status=row['status'],
                frames=len(row['frames']),packets=sum(len(f['packets'])for f in row['frames']),reason=row.get('reason'))),flush=True)
    finally:archive.close()
    report=dict(schema=1,goal_complete=False,executable_sha256=sha(exe),resources=results,
        source_render_entry='457b00 ->457ab0 -> child vtable+18 -> node vtable+10',
        observation_boundary='native render queue vtable+4; only memory callback adapted',runtime_effects_added=0,
        limits=['Explicit diagnostic camera; original scene and wall-clock still unverified',
            'Primitive packets include original evaluated source fields, not accepted vertex/raster output',
            'No Android source renderer/event bindings or PC comparison'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,indent=2)+'\n')

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path)
    p.add_argument('--resources',type=int,nargs='+',default=[133,134,148])
    p.add_argument('--steps',type=float,nargs='+',default=[1/30,.1,.5,1,2])
    p.add_argument('--seff-slots',type=int,nargs='+',help='Exact resource4792 placement slots; replaces resource sweep')
    p.add_argument('--projection-scale',type=float,default=.001)
    p.add_argument('--output',type=Path,default=ROOT/'out/pc-visual/v148-effect-draw-packets-source.json')
    a=p.parse_args()
    if not a.steps or any(not math.isfinite(dt)or not 0<dt<=30 for dt in a.steps):p.error('Diagnostic steps in (0,30]')
    inspect(a.installation,a.resources,a.steps,a.projection_scale,a.output,a.seff_slots)
