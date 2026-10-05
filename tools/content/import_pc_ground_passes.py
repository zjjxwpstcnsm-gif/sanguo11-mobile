#!/usr/bin/env python3
"""Convert original ground normals, painting, outline and grid, preserving alpha.

Staging does not claim installed bindings. Use --output to choose a project
directory; the supplied PC installation remains read-only.
"""
import argparse,json
from pathlib import Path
import numpy as np
from PIL import Image
from pc_resources import Archive,sha,wftx_levels
ROOT=Path(__file__).resolve().parents[2]

def convert(source,output,install=False):
    output.mkdir(parents=True,exist_ok=True);archive=Archive(source/'Media/san11pkres.bin');rows=[]
    def save(im,name,rid,raw,index,role):
        path=output/name;im.save(path)
        rows.append(dict(resource=rid,source_file='Media/san11pkres.bin',resource_sha256=sha(raw),image=index,
                         role=role,width=im.width,height=im.height,mode=im.mode,pixel_sha256=sha(im.tobytes()),
                         output=str(path),output_sha256=sha(path.read_bytes()),runtime_binding=('FilamentMapView syncPcGround / source shaders; original grid unbound' if role.startswith('Original source ground grid') else 'FilamentMapView syncPcGround / pc-ground.mat, pc-ground-outline.mat') if install else 'pending; staged original ground pass',
                         validation='Source live ground24 pass checker and normal-prefix checker; no installed binding acceptance'))
    try:
        raw=archive.read(4793)
        if raw[:8]!=b'K3ST0006' or len(raw)!=8+1025**2*8+1024**2*8:raise ValueError('Original K3ST bounds')
        vertices=np.frombuffer(raw[8:8+1025**2*8],dtype=np.uint8).reshape(1025,1025,8)
        save(Image.fromarray(vertices[:,:,4:7].transpose(1,0,2)),'ground-normal.png',4793,raw,None,
             'Numeric normal bytes; decode float32(byte*(1/255))*2-1 without normalization; source columns transposed to Android x-fast')
        for quarter in range(4):
            rid=4800+quarter;raw=archive.read(rid);frames=wftx_levels(raw)
            if len(frames)!=36:raise ValueError('Original ground36 textures')
            frame=frames[35]
            if frame['extra_mips']!=0 or frame['bits']!=32:raise ValueError('Reinspect source ground paint RGBA/mips')
            save(frame['levels'][0].convert('RGBA'),f'ground-paint-{quarter}.png',rid,raw,35,
                 'Ground77ad28: per-vertex max(dot(quantized original normal,c18),0),.5; clamp; select texture RGB and source RGBA alpha')
        for rid,index,name,role in ((4807,0,'ground-outline.png','Original backface normal-extrusion silhouette pass'),
                                   (4804,1,'ground-grid.png','Original source ground grid-alpha pass; indexed WFTX palette/mip decoder')):
            raw=archive.read(rid);frame=wftx_levels(raw)[index]
            save(frame['levels'][0].convert('RGBA'),name,rid,raw,index,role)
    finally:archive.close()
    report=dict(schema=1,goal_complete=False,status='SOURCE_GROUND_PASSES_BOUND_INSTALL_VALIDATION_PENDING' if install else 'ORIGINAL_GROUND_PASSES_STAGED_NOT_INSTALLED',
                source_policy='read-only provided modded PC installation',resources=rows,
                source_evidence=['out/pc-visual/v156/ground-passes-source-check.json','out/pc-visual/v156/ground-normal-texture-capture-check.json'],
                limits=['Runtime binding, original pass sorting/depth/blending, fog/fade and installed PC raster comparison still required',
                        'Conversion preserves source pixels; Android mip generation/encoded alpha upload must be explicitly controlled'])
    ((ROOT/'docs/pc-visual/ground-passes-source.json') if install else (output/'ground-passes-source.json')).write_text(json.dumps(report,indent=2)+'\n')
    if install:
        path=ROOT/'tools/content/map-release-manifest.json';manifest=json.loads(path.read_text())
        for row in rows:
            file=Path(row['output']);relative=file.resolve().relative_to(ROOT).as_posix();entry=dict(source_path=relative,apk_path='assets/'+file.resolve().relative_to(ROOT/'app/src/main/assets').as_posix(),sha256=row['output_sha256'])
            old=next((e for e in manifest['files'] if e['source_path']==relative),None)
            if old is None:manifest['files'].append(entry)
            else:old.update(entry)
        path.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
    print('Converted',len(rows),'original ground textures; installed acceptance pending')

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True)
    p.add_argument('--install',action='store_true',help='Update project source record and APK integrity manifest; no installed acceptance claim')
    a=p.parse_args();convert(a.installation,a.output,a.install)
