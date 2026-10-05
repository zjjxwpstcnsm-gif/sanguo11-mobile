#!/usr/bin/env python3
"""Check captured MP4 container metadata without claiming visual PC acceptance."""
import argparse,hashlib,json,struct
from pathlib import Path

def inspect(path):
 data=path.read_bytes();records=[]
 def boxes(start,end,parent=''):
  p=start
  while p<end:
   if p+8>end:raise ValueError('Truncated MP4 box')
   size,typ=struct.unpack_from('>I4s',data,p);header=8
   if size==1:size=struct.unpack_from('>Q',data,p+8)[0];header=16
   if size==0:size=end-p
   if size<header or p+size>end:raise ValueError('Invalid MP4 box bounds')
   kind=typ.decode('ascii');payload=p+header;record=dict(type=parent+'/'+kind,bytes=size)
   if kind in ('mvhd','mdhd'):
    version=data[payload];offset=20 if version else 12
    timescale=struct.unpack_from('>I',data,payload+offset)[0];duration=struct.unpack_from('>Q' if version else '>I',data,payload+offset+4)[0]
    record.update(duration_seconds=duration/timescale,timescale=timescale)
   if kind=='tkhd':record.update(width=struct.unpack_from('>I',data,p+size-8)[0]/65536,height=struct.unpack_from('>I',data,p+size-4)[0]/65536)
   if kind=='stsz':record['samples']=struct.unpack_from('>I',data,payload+8)[0]
   records.append(record)
   if kind in ('moov','trak','mdia','minf','stbl'):boxes(payload,p+size,record['type'])
   p+=size
 boxes(0,len(data))
 if not any(r['type']=='/moov' for r in records) or not any(r['type']=='/mdat' for r in records):raise ValueError('Incomplete recorded MP4')
 return dict(path=str(path),bytes=len(data),sha256=hashlib.sha256(data).hexdigest(),purpose='Android normal siege → original source damaged site geometry; battle particles remain legacy, PC-reference match not claimed',boxes=records)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('video',type=Path);p.add_argument('--output',type=Path);p.add_argument('--purpose',help='Explicit event coverage and acceptance limits');args=p.parse_args();result=inspect(args.video)
 if args.purpose:result['purpose']=args.purpose
 value=json.dumps(result,indent=2)+'\n'
 if args.output:args.output.write_text(value)
 print(json.dumps({k:v for k,v in result.items() if k!='boxes'}));print(json.dumps([r for r in result['boxes'] if any(k in r for k in ('duration_seconds','width','samples'))]))
