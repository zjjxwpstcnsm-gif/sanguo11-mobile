"""Strict KSEF0131 graph boundaries confirmed against supplied EXE loaders.

Preserves unknown semantics. This is not yet an emitter/material/time evaluator.
"""
import math,struct
from pc_resources import sha

def graph(data):
    def uint(p):
        if p<0 or p+4>len(data):raise ValueError('KSEF u32 bounds')
        return struct.unpack_from('<I',data,p)[0]
    def matrix(p):
        if p<0 or p+64>len(data):raise ValueError('KSEF transform bounds')
        values=list(struct.unpack_from('<16f',data,p))
        if not all(math.isfinite(v)for v in values):raise ValueError('KSEF nonfinite transform')
        return values
    def records(base,size,count_offset,array_offset):
        count=uint(base+count_offset);array=uint(base+array_offset)
        if count>4096 or array+count*4>size:raise ValueError('KSEF record array bounds')
        result=[]
        for i in range(count):
            relative=uint(base+array+i*4);ptr=base+relative;length=uint(ptr)
            if relative<40 or length<40 or relative+length>size:raise ValueError('KSEF record bounds')
            result.append(dict(offset=ptr,bytes=length,sha256=sha(data[ptr:ptr+length])))
        return result
    active=set();visited=set()
    def renderer(record):
        ptr=record['offset'];size=record['bytes']
        if ptr in active or ptr in visited:raise ValueError('KSEF renderer cycle/duplicate ownership')
        active.add(ptr);visited.add(ptr)
        kind,relocated=struct.unpack_from('<HH',data,ptr+4)
        if relocated!=0 or kind not in(0,1,2,3,6):raise ValueError('Unexamined KSEF renderer type/state')
        result={**record,'type':kind,'type_semantics':'unresolved','header_words':list(struct.unpack_from('<12I',data,ptr))}
        if kind==3:
            if size<0x148:raise ValueError('KSEF composite renderer truncated')
            result['controllers']=records(ptr,size,0x114,0x118)
            result['transforms']=records(ptr,size,0x128,0x138)
            result['motions']=records(ptr,size,0x12c,0x13c)
            result['children']=[renderer(r)for r in records(ptr,size,0x130,0x140)]
            result['other_records']=records(ptr,size,0x134,0x144)
        active.remove(ptr);return result
    if data[:8]!=b'KSEF0131' or uint(8)!=len(data) or uint(12)!=1:raise ValueError('KSEF header/root contract')
    root=uint(16);transform=matrix(root);state=list(struct.unpack_from('<4I',data,root+64))
    count=uint(root+80)
    if count!=1:raise ValueError('KSEF root child contract changed')
    cursor=root+84;children=[]
    for _ in range(count):
        kind=uint(cursor)
        if kind!=1:raise ValueError('KSEF child factory type not examined')
        child_matrix=matrix(cursor+4);child_state=list(struct.unpack_from('<4I',data,cursor+68));base=cursor+84
        size=uint(base)
        if size<60 or base+size>len(data) or uint(base+4)!=0:raise ValueError('KSEF block bounds/state')
        groups=[records(base,size,8+4*i,24+4*i)for i in range(4)]
        children.append(dict(type=kind,transform=child_matrix,state_raw_words=child_state,block_offset=base,block_bytes=size,
                             transforms=groups[0],motions=groups[1],renderers=[renderer(r)for r in groups[2]],other_records=groups[3]))
        cursor=base+size
    if cursor!=len(data):raise ValueError('KSEF unexplained trailing bytes')
    return dict(root_transform=transform,root_state_raw_words=state,children=children,
                limits='Only graph boundaries, record hashes and EXE dispatch types confirmed; pixel/material/curve evaluation pending')
