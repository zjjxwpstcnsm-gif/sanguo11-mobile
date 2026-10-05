#!/usr/bin/env python3
"""Execute original4cf160 governor ordering; no roster/admission replacement.

The original primary numeric key is48a4f0 effective command capacity, not
politics or the office enum. This tool records its observed result as an input.
"""
import argparse,gzip,itertools,json,struct
from pathlib import Path
from inspect_pc_debate_flow import NativeDebateFlow
from pc_original_pe_data import load_original_data
from audit_pc_restoration_sources import EXE_SHA,output_guard,json_bytes,sha

def inspect(installation,output):
    installation=installation.resolve();output_guard(installation,output)
    exe=(installation/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA:raise ValueError('Executable changed')
    shared=(installation/'Media/scenario/Scenario.s11').read_bytes()
    raw=(installation/'Media/scenario/Scen000.s11').read_bytes()
    w=NativeDebateFlow(installation,exe).world;load_original_data(w.u)
    w.load(shared,True);w.load(raw);w.call(0x73c500)
    for f,v in zip([0x4826e0,0x482700,0x482720],[184,1,1]):w.call(f,v,receiver=w.root)
    w.call(0x493400,receiver=w.root,count=50000000)
    baseline=bytes(w.u.mem_read(0x7200000,0x300000));seed=bytes(w.u.mem_read(0x8a5d44,4));cases=[]
    def pointer(i):return w.root+0xc0bc+i*0x190
    def person(i):
        p=pointer(i);b=bytes(w.u.mem_read(p,0x190))
        return dict(nativeId=i,allowed=bool(w.call(0x47a630,p)),status=struct.unpack_from('<i',b,0xa0)[0],
            commandCapacity=w.call(0x48a4f0,receiver=p)&65535,leadership=b[0x170],war=b[0x171],merit=struct.unpack_from('<H',b,0xae)[0])
    def earlier(a,b):
        if not a['allowed']or not b['allowed']or a['nativeId']==b['nativeId']:return False
        if (a['status']<=1 or b['status']<=1)and a['status']!=b['status']:return a['status']<b['status']
        for key in ['commandCapacity','leadership','war','merit']:
            if a[key]!=b[key]:return a[key]>b[key]
        return a['nativeId']<b['nativeId']
    def record(a,b,kind):
        left=person(a);right=person(b);actual=bool(w.call(0x4cf160,pointer(a),pointer(b)))
        if actual!=earlier(left,right):raise ValueError(f'Original ordering differs:{a}/{b}/{kind}')
        cases.append(dict(kind=kind,left=left,right=right,earlier=actual))
    active=[i for i in range(850)if w.call(0x47a630,pointer(i))]
    for index,a in enumerate(active):
        for b in [a,active[(index+1)%len(active)],active[(index+17)%len(active)]]:record(a,b,'actual-source')
    # Explicit comparator fixtures, not reconstructed scenario officers.
    for status_a,status_b,mode in itertools.product(range(6),range(6),range(7)):
        w.u.mem_write(0x7200000,baseline)
        for i,status in [(116,status_a),(222,status_b)]:
            p=pointer(i);w.u.mem_write(p+0xa0,struct.pack('<i',status));w.u.mem_write(p+0xa4,struct.pack('<i',80))
            w.u.mem_write(p+0x170,bytes([50,50]));w.u.mem_write(p+0xae,struct.pack('<H',1000))
        if mode==1:w.u.mem_write(pointer(116)+0x170,bytes([60]))
        if mode==2:w.u.mem_write(pointer(222)+0x170,bytes([60]))
        if mode==3:w.u.mem_write(pointer(116)+0x171,bytes([60]))
        if mode==4:w.u.mem_write(pointer(222)+0x171,bytes([60]))
        if mode==5:w.u.mem_write(pointer(116)+0xae,struct.pack('<H',2000))
        if mode==6:w.u.mem_write(pointer(222)+0xae,struct.pack('<H',2000))
        before=bytes(w.u.mem_read(0x7200000,0x300000))
        record(116,222,'controlled-comparator');record(222,116,'controlled-comparator')
        if before!=bytes(w.u.mem_read(0x7200000,0x300000)):raise ValueError('Comparator mutates authority')
    w.u.mem_write(0x7200000,baseline)
    if seed!=bytes(w.u.mem_read(0x8a5d44,4)):raise ValueError('Comparator consumes gameplay RNG')
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,sharedSha256=sha(shared),sourcePath='Media/scenario/Scen000.s11',sourceSha256=sha(raw),cases=cases,
        functions=[dict(start=hex(a),endExclusive=hex(b),sha256=sha(exe[a-0x400000:b-0x400000]))for a,b in [(0x4cf160,0x4cf270),(0x48a4f0,0x48a4fc),(0x49d420,0x49d59c)]],
        limits=['Ordering only; original candidate residency/district filtering and governor mutations remain separate',
                'Command capacity is the actual getter result; title/office/technique capacity calculation not replaced',
                'Controlled fixtures are not claimed as valid scenario records','No Android integration or APK evidence implied'],completeGoal=False)
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0))
    print(json.dumps(dict(actualOfficers=len(active),cases=len(cases),sha256=sha(output.read_bytes()))),flush=True)

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
