"""Original fullscreen factory, controllers, depth queue and final GPU writes.

All source instructions execute in an isolated visual VM, without Wine. The
identity view and broad diagnostic clipping are explicit conversion inputs;
Android's presentation lens remains a separately documented calibration.
"""
import math,struct
from pc_effect_machine import SourceEffectMachine
from inspect_pc_effect_draw_packets import DrawPacketObserver
from pc_effect_gpu_observer import FinalQuadObserver


class SourcePresentation:
    def __init__(self,verified,archive,index):
        if not 0<=index<244:raise ValueError('Original244 template index')
        self.machine=m=SourceEffectMachine(verified)
        self.observer=o=DrawPacketObserver(m,.001,native_queue=True)
        # Execute original camera builders.441b80 normalizes perspective by
        # far, and the source depth queue uses abs(w), not normalized z.
        # Orthographic diagnostic w=1 rejects all billboard layers.30deg is
        # confirmed for the captured PC map camera; fullscreen lens parity is
        # still pending. This is an explicit conversion lens, never hidden.
        m.u.mem_write(o.camera,bytes(0x200))
        m.u.mem_write(o.camera,struct.pack('<8f',0,0,0,1,0,0,-1,1))
        m.u.mem_write(o.camera+0x20,struct.pack('<5fI2f',0,math.pi/6,1,1000,4/3,0,0,0))
        up=m.STOP+0x680;m.u.mem_write(up,struct.pack('<4f',0,1,0,0))
        m.call(0x441ab0,0,o.camera,up);m.call(0x441b80,0,o.camera)
        m.u.mem_write(o.camera+0x140,bytes(m.u.mem_read(o.camera+0xc0,64)))
        self.final=FinalQuadObserver(m,o.camera,materials=True)
        self.manager=manager=m.HEAP+0x80000
        m.instance=root=m.HEAP;m.data=m.HEAP+0x1000
        m.call(0x45a820,manager)
        if m.call(0x45a620,manager,0,4096,244)!=1:raise ValueError('Original manager allocation')
        # Original45a320 reserves each loaded resource's slots with45a2b0.
        #45a280 binds a slot but does not grow the active range, so reserving
        #the original244-template range is required for parent45a530 updates.
        if m.call(0x45a2b0,manager,244)!=0 or m.uint(manager+0x2ec)!=244:
            raise ValueError('Original manager template slot reservation')
        m.u.mem_write(manager+0x2dc,struct.pack('<2I',0x795190,o.camera))
        # The original queue owns its native bucket/link/material order. The
        # observer only records the named insertion boundary without replacing it.
        o.queue=manager+0x220
        self.resource=struct.unpack_from('<I',verified.data,0x37692c+index*12+4)[0]
        self.raw=raw=archive.read(self.resource);m.u.mem_write(m.data,raw)
        for fn in (0x73bc80,0x73bd20):m.call(fn,0)
        m.call(0x457c90,root);m.call(0x457bd0,root,1,manager+0x2dc)
        if m.call(0x457dd0,root,m.data+20,0,0)!=m.data+len(raw):raise ValueError('Exact original KSEF boundary')
        m.call(0x457900,root);m.call(0x45a280,manager,index,root)
        m.u.mem_write(manager+0x314,struct.pack('<I',1));m.u.mem_write(manager+0x33c,struct.pack('<I',9))
        identity=m.call(0x443790,0)
        if m.call(0x413d20,manager,m.STOP+0x600,index,identity)!=1:raise ValueError('Original factory413d20')

    def frame(self,dt):
        m=self.machine
        if dt:m.call(0x45a530,self.manager,self.observer.camera,struct.unpack('<I',struct.pack('<f',dt))[0])
        # Original442800 clears exactly the original4096 buckets before render.
        m.call(0x442800,0,self.manager+0x224)
        packets=self.observer.draw();by_pointer={int(p['pointer'],16):p for p in packets}
        if len(by_pointer)!=len(packets):raise ValueError('Duplicate native draw packet')
        if m.uint(self.manager+0x22c)!=4096:raise ValueError('Original depth bucket count')
        ordered=[];seen=set();buckets=m.uint(self.manager+0x224)
        for i in range(4095,-1,-1):
            node=m.uint(buckets+4*i)
            while node:
                if node not in by_pointer or node in seen:raise ValueError('Native depth queue coverage/cycle')
                seen.add(node);ordered.append(by_pointer[node]);node=m.uint(node+0xc)
        if len(ordered)!=m.uint(self.manager+0x228):raise ValueError('Exact native queue coverage')
        # Source4420a0 may reject a packet outside its original[0,1] depth
        # interval; retain only nodes that the unchanged source would draw.
        self.rejected=len(packets)-len(ordered)
        return [self.final.evaluate(p)for p in ordered]

    def close(self):
        self.final.close();self.observer.close();self.machine.close()
