#!/usr/bin/env python3
"""Execute original derived debate model, native AI and frame transitions.

Inputs are explicit native fixtures; normal PC menus/event activation and final
campaign settlement remain separate. Original rules/AI/RNG are never replaced.
"""
import argparse,gzip,json,struct
from pathlib import Path
from inspect_pc_layered_scenario import NativeLayeredWorld
from pc_startup_platform import StartupPlatform
from audit_pc_restoration_sources import EXE_SHA,output_guard,json_bytes,sha

class NativeDebateFlow:
    def __init__(self,installation,exe):
        if sha(exe)!=EXE_SHA:raise ValueError('Changed original executable')
        self.world=NativeLayeredWorld(exe);self.platform=StartupPlatform(self.world,installation)
        self.world.call(0x73fd70)
        # The minimal serializer VM maps only the first5MiB of the original
        #image. Verify both omitted pages belong to the actual PE data section.
        pe=struct.unpack_from('<I',exe,60)[0];count=struct.unpack_from('<H',exe,pe+6)[0];optional=struct.unpack_from('<H',exe,pe+20)[0];sections=[]
        for i in range(count):
            offset=pe+24+optional+i*40;virtual,rva,stored,file_offset=struct.unpack_from('<4I',exe,offset+8)
            sections.append((0x400000+rva,stored,file_offset))
        self.original_pages=[]
        for address,size in [(0x900000,0x20000),(0x91ba000,0x1000)]:
            matches=[(start,n,off)for start,n,off in sections if start<=address and address+size<=start+n]
            if len(matches)!=1:raise ValueError('Omitted source page outside unique PE section')
            start,n,off=matches[0];raw=exe[off+address-start:off+address-start+size]
            if len(raw)!=size:raise ValueError('PE page truncated')
            self.world.u.mem_map(address,size);self.world.u.mem_write(address,raw)
            self.original_pages.append(dict(address=hex(address),bytes=size,sourceFileOffset=off+address-start,sha256=sha(raw)))
        self.fixture=0xc100000;self.world.u.mem_map(self.fixture,0x10000)
        self.people=[self.world.root+0xc0bc+i*0x190 for i in range(2)]
    def run(self,iqs,tempers,talk_masks,seed,frame_limit=2000,war_values=None,extended_fixture=False):
        w=self.world
        war_values=[0,0] if war_values is None else war_values
        if len(war_values)!=2 or any(not 0<=v<=(255 if extended_fixture else 100) for v in war_values):raise ValueError("Original unsigned-byte war inputs required")
        for actor,iq,temper,mask,war in zip(self.people,iqs,tempers,talk_masks,war_values):
            if not 0<=iq<=(255 if extended_fixture else 100) or not 0<=temper<4 or not 0<=mask<32:raise ValueError('Unexamined native input')
            w.call(0x489f10,receiver=actor);w.u.mem_write(actor+0xa0,struct.pack('<I',0))
            w.u.mem_write(actor+0x171,bytes([war]));w.u.mem_write(actor+0x172,bytes([iq]));w.u.mem_write(actor+0xfc,struct.pack('<I',temper));w.u.mem_write(actor+0x124,struct.pack('<I',mask<<3))
            if w.call(0x47a600,actor)!=1:raise ValueError('Original fixture actor invalid')
        w.u.mem_write(self.fixture,bytes(0x1000));w.call(0x51fcf0,receiver=self.fixture)
        w.u.mem_write(0x8b5214+0x28,struct.pack('<I',1)) # Explicit original input initializer pending gate.
        if w.call(0x51e220,*self.people,0,receiver=0x8b5214,count=10000000)!=1:raise ValueError('Original debate input rejected')
        w.u.mem_write(0x8a5d44,struct.pack('<I',seed))
        if w.call(0x51fd10,receiver=self.fixture,count=10000000)!=1:raise ValueError('Original derived initialization failed')
        start=bytes(w.u.mem_read(self.fixture,0x1b0));world_start=sha(bytes(w.u.mem_read(0x7200000,0x300000)));trace=[];previous=None
        for frame in range(frame_limit):
            try:w.call(0x51e300,1,receiver=self.fixture,count=10000000)
            except Exception as error:raise ValueError('Original frame failed '+str(frame)+' '+repr(w.invalid))from error
            state=bytes(w.u.mem_read(self.fixture,0x1b0));phase,sub=struct.unpack_from('<ii',state,8)
            hp=[struct.unpack_from('<i',state,0x14+0xa0*side)[0]for side in range(2)];anger=[struct.unpack_from('<i',state,0x18+0xa0*side)[0]for side in range(2)];fury=[struct.unpack_from('<i',state,0x1c+0xa0*side)[0]for side in range(2)]
            selected=list(struct.unpack_from('<2i',state,0x170));rng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0];key=(phase,sub,tuple(hp),tuple(anger),tuple(fury),tuple(selected),rng)
            if key!=previous:
                trace.append(dict(frame=frame,phase=phase,sub=sub,health=hp,anger=anger,fury=fury,selectedNativeCards=selected,nativeRng=rng,stateHex=state.hex()));previous=key
            if phase==9:break
        if phase!=9:raise ValueError('Original model did not reach terminal stage within bounded frames')
        return dict(currentIntelligence=iqs,nativePersonality=tempers,nativeTalkMasks=talk_masks,seed=seed,frameCount=frame+1,initialStateHex=start.hex(),terminalStateHex=state.hex(),finalNativeRng=rng,nativeWorldAfterInitializationSha256=world_start,nativeWorldAtTerminalSha256=sha(bytes(w.u.mem_read(0x7200000,0x300000))),trace=trace,originalModelReachedTerminal=True,campaignSettlementProven=False)

def inspect(installation,output):
    installation=installation.resolve();output_guard(installation,output);exe=(installation/'san11pk.exe').read_bytes();cases=[];pages=None
    for left in range(4):
        for right in range(4):
            d=NativeDebateFlow(installation,exe);case=d.run([90,82],[left,right],[31,31],23);cases.append(case);pages=d.original_pages
            print(json.dumps(dict(personality=[left,right],frames=case['frameCount'],health=case['trace'][-1]['health'],nativeRng=case['finalNativeRng'])),flush=True)
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,originalPages=pages,cases=cases,construct='51fcf0 derived ->51f850 base',initialize='51e220 input ->51fd10 ->51f8d0',frame='51e300 original vtable/state callbacks withdelta1',completePCNormalStart=False,androidIntegrated=False,limits=['Two explicitly constructed synthetic person actors with current IQ/personality/talk bits; not historical source identities','Original optional presentation pointers remain their exact zero PE defaults','Reached native terminalphase9; campaign event/settlement and UI rendering not certified','No AI/card/damage/RNG function interception; no Wine; PC installation read only'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0));print(json.dumps(dict(cases=len(cases),allOriginalModelsTerminal=True,sha256=sha(output.read_bytes()),androidIntegrated=False)))
    return report
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
