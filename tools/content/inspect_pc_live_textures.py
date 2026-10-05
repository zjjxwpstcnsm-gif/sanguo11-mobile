#!/usr/bin/env python3
"""Decode read-only source D3D9 managed texture pixels; retain alpha and HRESULTs."""
import argparse,json,struct
from pathlib import Path
import numpy as np
from PIL import Image
from pc_resources import Archive,sha,wftx

def inspect(source,capture,output,presentations=False):
    data=capture.read_bytes();magic,version,start,limit=struct.unpack_from('<4I',data)
    assert magic==0x31544350 and version==1 and limit in (256,512)
    footer,count,end,reserved=struct.unpack_from('<4I',data,len(data)-16)
    assert footer==0x54444e45 and reserved==0 and count<=64
    reference={};unsupported=[];archive=Archive(source/'Media/san11pkres.bin')
    try:
        resources=[4800,4801,4802,4803,4804,4806,4807]
        if presentations:resources.extend([124,*range(369,716)])
        for rid in resources:
            raw=archive.read(rid)
            try: decoded=wftx(raw)
            except ValueError as error:
                unsupported.append(dict(resource=rid,sha256=sha(raw),magic_hex=raw[:8].hex(),error=str(error)))
                continue
            for index,im in enumerate(decoded):
                key=(im.width,im.height,sha(im.convert('RGB').tobytes()))
                reference.setdefault(key,[]).append(dict(resource=rid,image=index,resource_sha256=sha(raw),mode=im.mode,
                    rgba_sha256=sha(im.convert('RGBA').tobytes())))
    finally: archive.close()
    output.mkdir(parents=True,exist_ok=True);at=16;rows=[]
    for i in range(count):
        assert at+48<=len(data)-16
        row=struct.unpack_from('<12I',data,at);at+=48
        magic,draw,stage,pointer,fmt,pool,w,h,hr,pitch,n,zero=row
        assert magic==0x31584554 and zero==0 and fmt in (21,22) and pool==1 and 0<w<=limit and 0<h<=limit
        assert n in (0,w*h*4) and at+n<=len(data)-16
        raw=data[at:at+n];at+=n
        record=dict(index=i,first_draw=draw,first_stage=stage,pointer_hex=hex(pointer),format=fmt,width=w,height=h,
                    lock_hresult=hr,pitch=pitch,bytes=n,raw_sha256=sha(raw))
        if n:
            assert hr==0 and pitch>=w*4
            pixels=np.frombuffer(raw,dtype=np.uint8).reshape(h,w,4)[:,:,[2,1,0,3]].copy()
            if fmt==22: pixels[:,:,3]=255
            im=Image.fromarray(pixels);path=output/f'texture-{i:02d}-draw{draw}-stage{stage}.png';im.save(path)
            record.update(path=str(path),rgba_sha256=sha(im.tobytes()),rgb_sha256=sha(im.convert('RGB').tobytes()),
                          alpha_min=int(pixels[:,:,3].min()),alpha_max=int(pixels[:,:,3].max()),
                          known_source_rgb_matches=reference.get((w,h,sha(im.convert('RGB').tobytes())),[]))
        rows.append(record)
    assert at==len(data)-16
    report=dict(status='DECODED_ORIGINAL_MANAGED_TEXTURES',goal_complete=False,source_capture=str(capture),
                capture_sha256=sha(data),start_present=start,end_present=end,textures=rows,unsupported_archive_candidates=unsupported,
                texture_dimension_limit=limit,presentation_library_compared=presentations,
                limits=[f'Only managed format21/22 textures up to{limit} squared; per-frame pointer deduplication',
                        'Original GPU lock failures retained; source archive matches are exact full-image RGB comparisons only',
                        'Composed dynamic subrectangles do not match whole source images; no selector or gameplay cause inferred from absence of matches',
                        'No Android raster/material equivalence inferred'])
    (output/'manifest.json').write_text(json.dumps(report,indent=2)+'\n')
    print('Decoded',len(rows),'source textures;',sum(bool(r['bytes'])for r in rows),'readable')

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('capture',type=Path);p.add_argument('--output',type=Path,required=True)
    p.add_argument('--presentations',action='store_true',help='Also compare original common124 and347 dynamic texture source images')
    a=p.parse_args();inspect(a.installation,a.capture,a.output,a.presentations)
