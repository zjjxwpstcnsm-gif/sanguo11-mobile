#!/usr/bin/env python3
"""Read converted Java HPROF with mmap; count arrays and identify their fields.

Run SDK hprof-conv first. This is a post-GC live-object diagnostic, never a
claim about allocation stacks or the unperturbed application's heap peak.
"""
import argparse, collections, hashlib, json, mmap, pathlib, struct

p=argparse.ArgumentParser();p.add_argument('hprof',type=pathlib.Path);p.add_argument('--output',type=pathlib.Path,required=True);a=p.parse_args()
sizes={2:0,4:1,5:2,6:4,7:8,8:1,9:2,10:4,11:8}
names={4:'boolean',5:'char',6:'float',7:'double',8:'byte',9:'short',10:'int',11:'long'}
strings={};loaded={};classes={};arrays=collections.Counter();instances=collections.Counter();top=[];large_arrays={};holder_arrays=collections.defaultdict(set)
with a.hprof.open('rb') as f, mmap.mmap(f.fileno(),0,access=mmap.ACCESS_READ) as data:
    end=data.find(b'\0');id_size=struct.unpack_from('>I',data,end+1)[0];sizes[2]=id_size
    def num(pos,n):return int.from_bytes(data[pos:pos+n],'big')
    def records():
        pos=end+13
        while pos<len(data):
            tag=data[pos];length=num(pos+5,4);start=pos+9;yield tag,start,start+length;pos=start+length
    def heap(start,stop):
        pos=start
        while pos<stop:
            tag=data[pos];pos+=1;body=pos
            if tag in (0xff,0x05,0x07):pos+=id_size
            elif tag in (0x01,):pos+=id_size*2
            elif tag in (0x02,0x03,0x08):pos+=id_size+8
            elif tag in (0x04,0x06):pos+=id_size+4
            elif tag==0x20:
                cid=num(pos,id_size);pos+=id_size+4;sup=num(pos,id_size);pos+=id_size*6+4
                count=num(pos,2);pos+=2
                for _ in range(count):t=data[pos+2];pos+=3+sizes[t]
                count=num(pos,2);pos+=2
                for _ in range(count):t=data[pos+id_size];pos+=id_size+1+sizes[t]
                count=num(pos,2);pos+=2;fields=[]
                for _ in range(count):fields.append((num(pos,id_size),data[pos+id_size]));pos+=id_size+1
                classes[cid]=(sup,fields)
            elif tag==0x21:
                length=num(pos+id_size*2+4,4);pos+=id_size*2+8+length
            elif tag==0x22:
                count=num(pos+id_size+4,4);pos+=id_size*2+8+count*id_size
            elif tag==0x23:
                count=num(pos+id_size+4,4);t=data[pos+id_size+8];pos+=id_size+9+count*sizes[t]
            else:raise ValueError('Unknown converted heap tag '+hex(tag)+' at '+str(body-1))
            yield tag,body,pos
    for tag,start,stop in records():
        if tag==1:strings[num(start,id_size)]=data[start+id_size:stop].decode('utf-8','replace')
        elif tag==2:loaded[num(start+4,id_size)]=num(start+8+id_size,id_size)
        elif tag in (0x0c,0x1c):
            for t,b,e in heap(start,stop):
                if t==0x23:
                    count=num(b+id_size+4,4);kind=data[b+id_size+8];size=count*sizes[kind];arrays[(kind,'bytes')]+=size;arrays[(kind,'count')]+=1
                    if size>=4096:large_arrays[num(b,id_size)]=(kind,size)
                    if size>=65536:top.append((size,num(b,id_size),kind,count))
                elif t==0x21:instances[num(b+id_size+4,id_size)]+=1
    top.sort(reverse=True);top=top[:80];wanted={x[1] for x in top};owners=collections.defaultdict(list)
    def class_name(cid):return strings.get(loaded.get(cid,0),'class:'+hex(cid))
    def fields(cid):
        seen=set()
        while cid and cid not in seen:
            seen.add(cid);sup,fs=classes.get(cid,(0,[]));yield from fs;cid=sup
    for tag,start,stop in records():
        if tag not in (0x0c,0x1c):continue
        for t,b,e in heap(start,stop):
            if t==0x21:
                cid=num(b+id_size+4,id_size);pos=b+id_size*2+8
                for fid,ft in fields(cid):
                    if ft==2:
                        target=num(pos,id_size)
                        if target in large_arrays:holder_arrays[class_name(cid)+'.'+strings.get(fid,hex(fid))].add(target)
                        if target in wanted and len(owners[target])<8:owners[target].append(class_name(cid)+'.'+strings.get(fid,hex(fid)))
                    pos+=sizes[ft]
            elif t==0x22:
                cid=num(b+id_size+8,id_size);count=num(b+id_size+4,4);pos=b+id_size*2+8
                for i in range(count):
                    target=num(pos+i*id_size,id_size)
                    if target in wanted and len(owners[target])<8:owners[target].append(class_name(cid)+'['+str(i)+']')
    result={'input':str(a.hprof.resolve()),'bytes':len(data),'scope':'Post-GC live objects only; no allocation stacks or unperturbed peak; top80 arrays >=64KiB, up to8 direct field/array owners each','primitiveArrays':{names[t]:{'bytes':arrays[(t,'bytes')],'count':arrays[(t,'count')]} for t in names},'largestArrays':[{'bytes':n,'id':hex(i),'type':names[t],'elements':c,'directOwners':owners[i]} for n,i,t,c in top],'largeArrayHolderGroups':[{'holder':holder,'arrays':len(ids),'bytes':sum(large_arrays[i][1] for i in ids)} for holder,ids in sorted(holder_arrays.items(),key=lambda item:-sum(large_arrays[i][1] for i in item[1]))],'holderBoundary':'All arrays >=4096B; unique within each holder group, shared arrays can appear in multiple groups; group totals not additive. Static/GC root ownership not inferred.','instanceCounts':[{ 'class':class_name(cid),'count':n} for cid,n in instances.most_common()]}
a.output.write_text(json.dumps(result,indent=2)+'\n');print(json.dumps({k:v for k,v in result.items() if k not in ['largestArrays','instanceCounts']}))
