#!/usr/bin/env python3
"""Offline ETC2 sRGB mip chains. pip install etcpak==0.9.15 Pillow==12.3.0 numpy==2.3.5 texture2ddecoder==1.0.6"""
import hashlib, json, math, struct, os
from pathlib import Path
import etcpak
import texture2ddecoder
import numpy as np
from PIL import Image
ROOT=Path(os.environ.get('ASSET_OUTPUT_ROOT',Path(__file__).resolve().parents[2]))

def mip(image, size):
    rgb=np.asarray(image.convert('RGB'),dtype=np.float32)/255
    linear=np.where(rgb<=.04045,rgb/12.92,((rgb+.055)/1.055)**2.4)
    resized=np.stack([np.asarray(Image.fromarray(linear[:,:,i]).resize(size,Image.Resampling.BOX)) for i in range(3)],axis=2)
    srgb=np.where(resized<=.0031308,resized*12.92,1.055*np.maximum(resized,0)**(1/2.4)-.055)
    return Image.fromarray(np.uint8(np.clip(srgb*255+.5,0,255)),'RGB')

reports=[]
for group,name in [('sites','atlas.png'),('field','atlas.png'),('field','unit-atlas.png')]:
    path=ROOT/'app/src/main/assets/3d'/group/name
    source=Image.open(path).convert('RGBA')
    assert source.getextrema()[3]==(255,255), 'opaque ETC2 RGB pipeline must not discard alpha'
    width,height=source.size;levels=1+int(math.log2(max(width,height)))
    output=bytearray(struct.pack('>4I',0x45544332,width,height,levels));raw_bytes=0;payload_bytes=0
    for level in range(levels):
        w,h=max(1,width>>level),max(1,height>>level)
        im=source.convert('RGB') if level==0 else mip(source,(w,h))
        pixels=np.asarray(im.convert('RGBA'));pad=np.pad(pixels,((0,(-h)%4),(0,(-w)%4),(0,0)),mode='edge')
        rgba=pad.copy()
        data=etcpak.compress_etc2_rgb(rgba.tobytes(),rgba.shape[1],rgba.shape[0])
        assert len(data)==((w+3)//4)*((h+3)//4)*8
        if level==0:
            decoded=Image.frombytes('RGBA',(w,h),texture2ddecoder.decode_etc2(data,w,h),'raw','BGRA').convert('RGB')
            mse=np.mean((np.asarray(decoded,dtype=float)-np.asarray(im,dtype=float))**2)
            psnr=float(10*np.log10(255**2/mse));assert psnr>28, 'channel order or compression regression'
        output.extend(struct.pack('>I',len(data)));output.extend(data)
        raw_bytes+=w*h*4;payload_bytes+=len(data)
    target=path.with_suffix('.etc2');target.write_bytes(output)
    reports.append(dict(asset=str(target.relative_to(ROOT)),source_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),sha256=hashlib.sha256(output).hexdigest(),width=width,height=height,levels=levels,format='ETC2_SRGB8',gpu_payload_bytes=payload_bytes,rgba_mip_bytes=raw_bytes,alpha='opaque',mip_filter='linear-light BOX',encoder='etcpak 0.9.15',level0_psnr_db=psnr,gpu_upload_verified=False))
(ROOT/'docs/3d/texture-compression.json').write_text(json.dumps(reports,indent=2)+'\n')
