#!/usr/bin/env python3
"""Decode an owned recording completely and extract unmodified RGB frame samples.
Requires PyAV. No Pillow, encoder, source-video mutation or audio inference.
"""
import argparse,hashlib,json,struct,zlib
from pathlib import Path
import av

def chunk(kind,data):
    return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)

def frame_png(frame,path):
    rgb=frame.reformat(format='rgb24');plane=rgb.planes[0];data=bytes(plane)
    pixels=b''.join(b'\0'+data[y*plane.line_size:y*plane.line_size+rgb.width*3] for y in range(rgb.height))
    header=struct.pack('>IIBBBBB',rgb.width,rgb.height,8,2,0,0,0)
    path.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',header)+chunk(b'IDAT',zlib.compress(pixels))+chunk(b'IEND',b''))

def digest(path):
    h=hashlib.sha256()
    with path.open('rb') as f:
        for block in iter(lambda:f.read(1<<20),b''):h.update(block)
    return h.hexdigest()

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('video',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
    a.output.mkdir(parents=True,exist_ok=False)
    with av.open(str(a.video)) as c:
        if not c.streams.video:raise ValueError('No video stream')
        stream=c.streams.video[0]
        if stream.duration is None:raise ValueError('No bounded recording duration')
        duration=float(stream.duration*stream.time_base);targets=[duration*x for x in [.05,.2,.4,.6,.8,.95]]
        sample=count=0;first=last=None
        for frame in c.decode(video=0):
            if frame.pts is None:raise ValueError('Missing decoded frame timestamp')
            time=float(frame.pts*frame.time_base);count+=1
            if first is None:first=time
            if last is not None and time<last:raise ValueError('Nonmonotonic frame timestamp')
            last=time
            if sample<len(targets) and time>=targets[sample]:frame_png(frame,a.output/('sample-'+str(sample)+'.png'));sample+=1
        if count==0 or sample!=len(targets):raise ValueError('Incomplete decode/samples')
        result=dict(file=str(a.video.resolve()),sha256=digest(a.video),all_frames_decoded=True,frames=count,container_duration_seconds=duration,first_pts_seconds=first,last_pts_seconds=last,audio_streams=len(c.streams.audio),sample_frames=sample,scope='Decoded owned recording with sample RGB PNGs; not original-source pixel parity, GPU cadence, or audible-output proof')
        (a.output/'verification.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))

if __name__=='__main__':main()
