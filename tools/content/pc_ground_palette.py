#!/usr/bin/env python3
"""Pack original ground pixels without resizing; retain source UV dimensions."""
import argparse,json
from pathlib import Path
import numpy as np
from PIL import Image
from pc_resources import Archive,sha

ROOT=Path(__file__).resolve().parents[2]

def pack_palette(images):
    if len(images)!=36:raise ValueError('Original36 ground palette images')
    atlas=Image.new('RGB',(1560,1560))
    dimensions=[]
    for i,im in enumerate(images):
        w,h=im.size
        if w not in (64,128,256) or h not in (64,128,256):raise ValueError('Reinspect ground dimensions')
        # Keep actual texels and wrap gutters. Smaller source images occupy
        # smaller rectangles inside fixed260 cells; shader UV uses their size.
        tile=Image.fromarray(np.pad(np.asarray(im.convert('RGB')),((2,2),(2,2),(0,0)),mode='wrap'))
        atlas.paste(tile,((i%6)*260,(i//6)*260))
        dimensions.append((w//32,h//32,0))
    return atlas,dimensions

def export(source):
    from import_pc_map import textures
    archive=Archive(source/'Media/san11pkres.bin');assets=ROOT/'app/src/main/assets/3d/pc-map'
    sizes=[];resources=[]
    try:
        for quarter in range(4):
            raw=archive.read(4800+quarter);images=textures(raw)
            atlas,dimensions=pack_palette(images);sizes.append(dimensions)
            target=assets/f'palette-{quarter}.png';atlas.save(target)
            resources.append(dict(id=4800+quarter,source_file='Media/san11pkres.bin',format='WFTX0010',sha256=sha(raw),
                images=[dict(index=i,width=im.width,height=im.height,rgb_sha256=sha(im.convert('RGB').tobytes()))for i,im in enumerate(images)],
                android_output=str(target.relative_to(ROOT)),output_sha256=sha(target.read_bytes())))
    finally:archive.close()
    target=assets/'palette-sizes.png';Image.fromarray(np.asarray(sizes,dtype=np.uint8)).save(target)
    outputs=[dict(path=r['android_output'],sha256=r['output_sha256'])for r in resources]+[dict(path=str(target.relative_to(ROOT)),sha256=sha(target.read_bytes()))]
    report=dict(schema=1,goal_complete=False,source_policy='read-only user supplied modded installation',resources=resources,outputs=outputs,
        converter='tools/content/pc_ground_palette.py; import_pc_map.py uses same pack_palette',
        sizes_contract='RGB36x4: width/32,height/32,zero; archive row order autumn,spring,summer,winter; numeric nearest sampling',
        atlas_contract='Original RGB pixels,260x260 cells,two wrapped texel gutters; no image resize',
        runtime_binding='FilamentMapView.pcPalette/pcPaletteSizes;pc-ground.mat per-source-dimension UV',
        validation='Original41dcb0/41dc00 and actual D3D9 opaque stream1 checked separately; installedv156 pending',
        limits=['Ground PBR/normal/fog/outline pipeline remains incomplete','Mip/edge behavior and whole PC pixel match still need live comparison'])
    (ROOT/'docs/pc-visual/ground-palette-source.json').write_text(json.dumps(report,indent=2)+'\n')
    manifest=ROOT/'tools/content/map-release-manifest.json';d=json.loads(manifest.read_text())
    for o in outputs:
        e=next((e for e in d['files'] if e['source_path']==o['path']),None)
        if e is None:
            e=dict(source_path=o['path'],apk_path='assets/'+str(Path(o['path']).relative_to('app/src/main/assets')));d['files'].append(e)
        e['sha256']=o['sha256']
    manifest.write_text(json.dumps(d,indent=2,ensure_ascii=False)+'\n')
    print('Original144 images packed without resizing; numeric36x4 size table written')

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);export(p.parse_args().installation)
