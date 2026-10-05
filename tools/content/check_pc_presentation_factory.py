#!/usr/bin/env python3
"""Execute the actual original presentation factory, not a guessed start.

The default camera is explicitly diagnostic. Source413d20 chooses457790,
457800 or457880 from the loaded emitter flags; callbacks execute unchanged.
No Wine, gameplay state, gameplay RNG or PC install writes are involved.
"""
import argparse,json,struct
from pathlib import Path
from pc_effect_machine import SourceEffectMachine,VerifiedSourceExecutable
from inspect_pc_effect_draw_packets import DrawPacketObserver
from pc_effect_gpu_observer import FinalQuadObserver
from pc_resources import Archive,sha

def evaluate(installation,output,steps,indices=(115,116,125,126)):
    verified=VerifiedSourceExecutable((installation/'san11pk.exe').read_bytes())
    archive=Archive(installation/'Media/san11pkres.bin');rows=[]
    try:
        for index in indices:
            if not 0<=index<244:raise ValueError('Original244 template boundary')
            resource=struct.unpack_from('<I',verified.data,0x37692c+index*12+4)[0]
            machine=SourceEffectMachine(verified);observer=DrawPacketObserver(machine,.001)
            final=FinalQuadObserver(machine,observer.camera,materials=True)
            try:
                manager=machine.HEAP+0x80000;root=machine.HEAP
                machine.instance=root;machine.data=machine.HEAP+0x1000
                machine.call(0x45a820,manager)
                if machine.call(0x45a620,manager,0,4096,244)!=1:raise ValueError('Source manager allocation')
                machine.u.mem_write(manager+0x2dc,struct.pack('<2I',0x795190,observer.camera))
                raw=archive.read(resource);machine.u.mem_write(machine.data,raw)
                for fn in [0x73bc80,0x73bd20]:machine.call(fn,0)
                machine.call(0x457c90,root);machine.call(0x457bd0,root,1,manager+0x2dc)
                if machine.call(0x457dd0,root,machine.data+20,0,0)!=machine.data+len(raw):raise ValueError('Exact KSEF loader boundary')
                machine.call(0x457900,root);machine.call(0x45a280,manager,index,root)
                # Already-loaded original root manager, no region-template remap.
                machine.u.mem_write(manager+0x314,struct.pack('<I',1))
                machine.u.mem_write(manager+0x33c,struct.pack('<I',9))
                identity=machine.call(0x443790,0)
                result=machine.call(0x413d20,manager,machine.STOP+0x600,index,identity)
                if result!=1:raise ValueError('Source413d20 start failed')
                frames=[]
                for dt in steps:
                    machine.update(dt);packets=observer.draw()
                    frames.append(dict(input_dt=dt,snapshot=machine.snapshot(),
                        quads=[dict(packet=p,final=final.evaluate(p))for p in packets]))
                rows.append(dict(effect_index=index,resource_id=resource,source_sha256=sha(raw),
                    factory_return=result,start_matrix_sha256=sha(bytes(machine.u.mem_read(identity,64))),frames=frames))
                print(json.dumps(dict(resource=resource,frames=len(frames),quads=sum(len(f['quads'])for f in frames))),flush=True)
            finally:final.close();observer.close();machine.close()
    finally:archive.close()
    report=dict(schema=1,goal_complete=False,status='ORIGINAL_FACTORY_AND_QUADS_EXECUTED_DIAGNOSTIC_CAMERA_ONLY',
        source_executable_sha256=sha(verified.data),factory='413d20;native4133d0/413420 branch and413770 callbacks',
        camera='diagnostic .001; not accepted source fullscreen camera',resources=rows,
        limits=['Original code/pixels required; no source PC instance or reference timing inferred',
                'No gameplay rule/RNG access, Wine or original installation writes',
                'This is source geometry evidence, not Android event integration'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,indent=2)+'\n')

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path)
    p.add_argument('--output',type=Path,required=True);p.add_argument('--steps',type=float,nargs='+',default=[1/60,.1,.25,.5,.5,.5])
    p.add_argument('--indices',type=int,nargs='+',default=[115,116,125,126])
    a=p.parse_args();evaluate(a.installation,a.output,a.steps,a.indices)
