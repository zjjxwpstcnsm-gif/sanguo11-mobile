#!/usr/bin/env python3
"""Execute pinned original debate capacity/card ordering/character talk flags.

Uses actual constructors/getters and original51e960 comparison. Synthetic input
states are explicit test fixtures. No replacement RNG/game rule and no Wine.
This does not certify a complete normal PC or Android debate flow.
"""
import argparse,gzip,json,struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_ESI
from inspect_pc_officer_fields import NativeFields
from audit_pc_restoration_sources import EXE_SHA,output_guard,json_bytes,sha

FUNCTIONS=[('hand_slots_including_reconsider',0x51d470,0x51d4ac),('person_talk_flag',0x489780,0x4897a8),('card_topic',0x51e260,0x51e28a),('card_size',0x51e2b0,0x51e2da),('card_comparison',0x51e960,0x51ea09),('speaker_health_set',0x51d890,0x51d8b6),('speaker_anger_set',0x51d8f0,0x51d90e),('damage',0x51ea10,0x51eae5)]

class DebateReader(NativeFields):
    def __init__(self,exe):
        super().__init__(exe);self.fixture=0xc100000;self.u.mem_map(self.fixture,0x10000)
        self.actor=self.root+0xc0bc;self.call(0x489f10,receiver=self.actor)
        self.u.mem_write(self.actor+0xa0,struct.pack('<I',0))
        if self.call(0x47a600,self.actor)!=1:raise ValueError('Original actor fixture invalid')
    def capacity(self,iq):
        self.u.mem_write(self.actor+0x172,bytes([iq]));self.u.reg_write(UC_X86_REG_ESI,self.actor)
        #51d470 is an ESI-register entry, not a thiscall ECX receiver.
        return self.call(0x51d470)
    def comparison(self,topic,left,right,left_temper=-1,left_fury=0,right_temper=-1,right_fury=0):
        self.u.mem_write(self.fixture,bytes(0x200))
        for offset,value in [(0x168,topic),(0x170,left),(0x174,right),(0x10+0x9c,left_temper),(0xb0+0x9c,right_temper),(0x10+0xc,left_fury),(0xb0+0xc,right_fury)]:
            self.u.mem_write(self.fixture+offset,struct.pack('<i',value))
        before=bytes(self.u.mem_read(self.fixture,0x200));rng=bytes(self.u.mem_read(0x8a5d44,4))
        result=self.call(0x51e960,receiver=self.fixture)
        expected=bytearray(before);struct.pack_into('<i',expected,0x178,result)
        if bytes(self.u.mem_read(self.fixture,0x200))!=bytes(expected)or rng!=bytes(self.u.mem_read(0x8a5d44,4)):
            raise ValueError('Original comparison wrote beyond recorded winner or RNG')
        return result

def inspect(installation,output):
    installation=installation.resolve();output_guard(installation,output);exe=(installation/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA:raise ValueError('Executable differs')
    d=DebateReader(exe);capacity=[];cards=[];comparisons=[];flags=[];clamps=[]
    for iq in range(101):capacity.append(dict(currentIntelligence=iq,slotsIncludingReconsider=d.capacity(iq)))
    for card in range(-1,16):cards.append(dict(nativeCard=card,topic=d.call(0x51e260,card),sizeZeroBased=d.call(0x51e2b0,card)))
    for topic in range(3):
        for left in range(15):
            for right in range(15):comparisons.append(dict(topic=topic,left=left,right=right,winner=d.comparison(topic,left,right)))
    # Isolate actual actor+124 bits, with each mask exercising the original
    #489780 ->472590 branch. Label order is from original8ad5c4/51da20.
    for mask in range(32):
        d.u.mem_write(d.actor+0x124,struct.pack('<I',mask<<3))
        actual=[d.call(0x489780,i,receiver=d.actor) for i in range(5)]
        if actual!=[(mask>>i)&1 for i in range(5)]:raise ValueError('Original talk-bit interpretation differs')
        flags.append(dict(nativeTalkMask=mask,originalFlags=actual))
    for value in [-1000,-101,-100,-1,0,1,99,100,999,1000,1001,5000]:
        d.call(0x51d890,value,receiver=d.fixture);health=struct.unpack('<i',d.u.mem_read(d.fixture+4,4))[0]
        d.call(0x51d8f0,value,receiver=d.fixture);anger=struct.unpack('<i',d.u.mem_read(d.fixture+8,4))[0]
        clamps.append(dict(input=value,health=health,anger=anger))
    fury_cases=[]
    for lt in range(4):
        for rt in range(4):
            for lf,rf in [(1,0),(0,1),(1,1)]:
                for left,right in [(1,9),(10,1),(11,9),(12,1),(13,14),(12,10)]:fury_cases.append(dict(leftTemper=lt,rightTemper=rt,leftFury=lf,rightFury=rf,left=left,right=right,topic=0,winner=d.comparison(0,left,right,lt,lf,rt,rf)))
    damage=[]
    damage_factors=list(struct.unpack('<3i',d.u.mem_read(0x8b52e0,12)))
    for topic in range(3):
        for card in range(1,15):
            for temper in range(4):
                for fury in (0,1):
                    for modifier in (50,100,176):
                        for seed in (0,1,0x12345678,0xffffffff):
                            d.u.mem_write(d.fixture,bytes(0x200))
                            for offset,value in [(0x168,topic),(0x10+0x9c,temper),(0x10+0xc,fury),(0x10+0x10,modifier)]:
                                d.u.mem_write(d.fixture+offset,struct.pack('<i',value))
                            d.u.mem_write(0x8a5d44,struct.pack('<I',seed))
                            before=bytes(d.u.mem_read(d.fixture,0x200))
                            result=d.call(0x51ea10,0,card,receiver=d.fixture)
                            after_rng=struct.unpack('<I',d.u.mem_read(0x8a5d44,4))[0]
                            expected_rng=(seed*0x6c078965+0x3039)&0xffffffff
                            if after_rng!=expected_rng or before!=bytes(d.u.mem_read(d.fixture,0x200)):
                                raise ValueError('Damage altered fixture or consumed unexpected original RNG')
                            damage.append(dict(topic=topic,card=card,temper=temper,fury=fury,modifier=modifier,seed=seed,damage=result,finalNativeRng=after_rng))
    functions=[dict(name=name,address=hex(start),endExclusive=hex(end),bytesHex=exe[start-0x400000:end-0x400000].hex(),sha256=sha(exe[start-0x400000:end-0x400000]))for name,start,end in FUNCTIONS]
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,functions=functions,capacity=capacity,cards=cards,comparison=comparisons,talkFlagMasks=flags,clamps=clamps,furyComparisons=fury_cases,damage=damage,damageSizeFactors=damage_factors,fixture='Original person489f10; valid status0; explicit runtime IQ+172; explicit synthetic model0xc100000',winnerEncoding='-1 tie;0 left;1 right',completeDebateFlow=False,limits=['No normal event/diplomacy/recruitment start or settlement executed','Capacity includes permanent nativecard0 reconsider; not all slots are playable topic cards','Damage function executed with explicit fixtures and original RNG; complete deck/AI/anger effects remain pending','No Android integration, media changes or PC writes'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0));print(json.dumps(dict(capacity=len(capacity),comparison=len(comparisons),furyComparisons=len(fury_cases),damage=len(damage),packedSha256=sha(output.read_bytes()),completeDebateFlow=False)))
    return report
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
