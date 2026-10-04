#!/usr/bin/env python3
"""Execute original4a75a0 on explicit loyalty/relationship memory fixtures.

No rule or RNG replacement. Source identities stay native222/365; fixture
values are not claimed to be historical records or complete opening facts.
"""
import argparse,gzip,itertools,json,struct
from pathlib import Path
from inspect_pc_debate_flow import NativeDebateFlow
from audit_pc_restoration_sources import ROOT,EXE_SHA,output_guard,json_bytes,sha

def inspect(installation,output):
    installation=installation.resolve();output_guard(installation,output)
    raw=(installation/'Media/scenario/Scen000.s11').read_bytes();shared=(installation/'Media/scenario/Scenario.s11').read_bytes()
    w=NativeDebateFlow(installation,(installation/'san11pk.exe').read_bytes()).world
    w.load(shared,True);loaded=w.load(raw);w.call(0x73c500)
    a=w.root+0xc0bc+222*0x190;r=w.root+0xc0bc+365*0x190;force=w.root+0x7af8+2*0x12c
    if w.call(0x491310,r,receiver=w.root)!=365 or struct.unpack('<I',w.u.mem_read(force+4,4))[0]!=365:raise ValueError('Source ruler identity differs')
    baseline=bytes(w.u.mem_read(0x7200000,0x300000));cases=[]
    numeric=[dict(current=80,gap=gap,honor=honor,ambition=ambition,charm=charm,weighted=weighted,
                  mode='neutral',sameHan=han,sameBirthplace=origin)for honor,gap,charm,weighted,han,origin,ambition in
        itertools.product(range(5),[0,1,24,49,50,51,75],[0,13,14,100],[False,True],[False,True],[False,True],[0,4])]
    relations=[dict(current=current,gap=50,honor=2,ambition=4,charm=100,weighted=weighted,mode=mode,sameHan=False,sameBirthplace=False)
        for mode,current,weighted in itertools.product(['ruler','spouse','sworn','liked','disliked','same-father','disliked-same-father'],[0,99,119,120,149,150,249,250,255],[False,True])]
    for index,c in enumerate(numeric+relations):
        w.u.mem_write(0x7200000,baseline)
        # Explicit memory inputs; original relation getters verify their effect.
        w.u.mem_write(a+0xa0,struct.pack('<i',0 if c['mode']=='ruler' else 3))
        w.u.mem_write(a+0xac,bytes([c['current']]))
        w.u.mem_write(a+0x60,struct.pack('<ii',365 if c['mode']=='spouse' else -1,365 if c['mode']=='sworn' else -1))
        w.u.mem_write(r+0x64,struct.pack('<i',365))
        w.u.mem_write(a+0x6c,struct.pack('<5i',*([365 if c['mode']=='liked' else -1]+[-1]*4)))
        w.u.mem_write(a+0x80,struct.pack('<5i',*([365 if c['mode'].startswith('disliked') else -1]+[-1]*4)))
        w.u.mem_write(a+0x54,struct.pack('<i',365 if 'same-father'in c['mode'] else -1));w.u.mem_write(r+0x54,struct.pack('<i',365))
        w.u.mem_write(a+0x69,bytes([c['gap']]));w.u.mem_write(r+0x69,b'\0')
        w.u.mem_write(a+0xf0,struct.pack('<ii',c['honor'],c['ambition']))
        w.u.mem_write(a+0x108,struct.pack('<i',1));w.u.mem_write(r+0x108,struct.pack('<i',1 if c['sameHan'] else 2))
        w.u.mem_write(a+0xe4,struct.pack('<i',1));w.u.mem_write(r+0xe4,struct.pack('<i',1 if c['sameBirthplace'] else 2))
        w.u.mem_write(r+0x174,bytes([c['charm']]))
        actual=dict(ruler=bool(w.call(0x488c00,receiver=a)),spouse=bool(w.call(0x488790,365,receiver=a)),
            sworn=bool(w.call(0x4887d0,365,receiver=a)),liked=bool(w.call(0x488910,365,receiver=a)),
            disliked=bool(w.call(0x4889e0,365,receiver=a)),sameFather=bool(w.call(0x48bb70,365,receiver=a)),
            gap=w.call(0x489f80,365,receiver=a)&255,rulerCharm=w.call(0x4890b0,receiver=r)&255)
        w.u.mem_write(0x8a5d44,struct.pack('<I',23));before=bytes(w.u.mem_read(0x7200000,0x300000))
        w.call(0x4a75a0,a,force,int(c['weighted']),receiver=0x799895c)
        result=bytes(w.u.mem_read(a+0xac,1))[0];expected=bytearray(before);expected[a-0x7200000+0xac]=result
        if bytes(expected)!=bytes(w.u.mem_read(0x7200000,0x300000))or struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]!=23:raise ValueError('Unexpected original loyalty side effect')
        cases.append(dict(inputs=c,originalFacts=actual,rawLoyaltyAfter=result))
        if index%500==0:print(json.dumps(dict(cases=index+1)),flush=True)
    records={x['native_index']:x for x in loaded['records']if x['kind']=='officer'}
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,sourcePath='Media/scenario/Scen000.s11',sourceSha256=sha(raw),sharedSha256=sha(shared),
        participantRecordSha256={str(i):records[i]['sha256']for i in [222,365]},cases=cases,
        functions=[dict(start=hex(a),endExclusive=hex(z),sha256=sha(bytes(w.u.mem_read(a,z-a))))for a,z in [(0x4a75a0,0x4a77b0),(0x489f80,0x489fda),(0x48bb70,0x48bbc5)]],
        fullWorldAndRngChecked=True,limits=['Explicit numeric and relationship VM fixtures; not historical edited records',
          'Original raw loyalty0..255; displayed loyalty getter remains separate','Only loyalty rule; complete allegiance/command admission/recovery are separate'],completeGoal=False)
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0));print(json.dumps(dict(cases=len(cases),sha256=sha(output.read_bytes()))),flush=True)
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
