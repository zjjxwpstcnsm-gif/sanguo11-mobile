#!/usr/bin/env python3
"""Convert the supplied PC facility catalog, retaining authored geometry/RGBA.

Uses the source labels/switch checked by inspect_pc_facilities.py. Runtime
climate selection remains separate from model conversion. No source writes.
"""
import argparse
import gzip
import json
import struct
from pathlib import Path
import numpy as np
from PIL import Image
from pc_scene_coordinates import WORLD_AXES, contract
from pc_resources import Archive, wkmd_geometry, wftx, sha

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'app/src/main/assets/3d/pc-facilities'


def texture_offset(kind,state,quarter,climate,model):
    if kind==71 and state<2 or kind in (73,78) and state<1:return quarter
    if kind in (74,79,76,81) and state<1:return 2 if quarter==3 else int(quarter==2)
    if kind==24 and state<2:return 4 if quarter==3 else quarter
    return 0 if quarter!=3 else 1 if climate in (0,2) or model==72 else 2


def convert(installation):
    exe=(installation/'san11pk.exe').read_bytes()
    definitions=(installation/'Media/scenario/Scenario.s11').read_bytes()
    catalog=json.loads((ROOT/'docs/pc-visual/facility-bindings.json').read_text())
    if catalog['executable_sha256']!=sha(exe) or catalog['definition_sha256']!=sha(definitions):raise ValueError('Re-inspect changed source catalog')
    models=struct.unpack_from('<388I',exe,0x363740);textures=struct.unpack_from('<88I',exe,0x363d50);materials=struct.unpack_from('<388I',exe,0x363eb0)
    bindings={r['object_kind']:[v['model_index'] for v in r['model_variants']] for r in catalog['facilities'] if r['model_variants']}
    # Native OBJS cliff walls are a distinct map object; include their source
    # model family without presenting them as the constructible stone wall.
    bindings[14]=list(struct.unpack_from('<4H',exe,0x37a720+14*8))
    # Source 41cf70/41c630 draw connected walls with independent pillars and
    # sheared segments. They are not extra guessed facility variants.
    connections={14:dict(pillars=[118,120],segment=114),17:dict(pillars=[136],segment=130),28:dict(pillars=[208],segment=196)}
    connection_models={n+lod for row in connections.values() for n in [*row['pillars'],row['segment']] for lod in (0,1)}
    ids=sorted({n+lod for row in bindings.values() for n in row for lod in (0,1)}|connection_models)
    texids=set()
    for kind,row in bindings.items():
        for state,model in enumerate(row):
            for quarter in range(4):
                for climate in range(6):
                    for lod in (0,1):texids.add(materials[model+lod]+texture_offset(kind,state,quarter,climate,model))
    for kind,row in connections.items():
        for model in [*row['pillars'],row['segment']]:
            for quarter in range(4):
                for climate in range(6):
                    for lod in (0,1):texids.add(materials[model+lod]+texture_offset(kind,0,quarter,climate,model))
    texids=sorted(texids)
    if any(n>=88 for n in texids):raise ValueError('Texture resolver outside table')
    archive=Archive(installation/'Media/san11pkres.bin')
    images={}
    for n in texids:
        image,=wftx(archive.read(textures[n]))
        if image.size not in ((128,128),(256,128),(256,256)):raise ValueError('Unexpected source facility texture')
        images[n]=image
    payload=bytearray(b'PCFAC002'+struct.pack('<III',len(bindings),len(ids),len(texids)))
    for kind,row in sorted(bindings.items()):payload+=struct.pack('<5I',kind,*row)
    for n in texids:payload+=struct.pack('<III',n,*images[n].size)
    evidence=dict(schema=1,source_archive='Media/san11pkres.bin',executable_sha256=sha(exe),
        definition_file=catalog['definition_file'],definition_sha256=sha(definitions),
        object_bindings=[dict(kind=k,models=v) for k,v in sorted(bindings.items())],wall_connection_bindings=connections,models=[],textures=[],
        conversion='Source FVF112 positions/normals/UV/BGRA; D3D strips to triangles; exact RGBA sheets with edge gutters',
        runtime_binding='PcFacilities / FilamentMapView',
        limits=['PC captured pixels unavailable','Regional climate default0 requires source region decoding',
                'Source opaque/alpha pass is recorded; current source material uses masked blending pending PC comparison',
                'Core supports a subset of the 54 source facility kinds; unsupported names are cataloged, not spawned',
                'Construction/upgrade authority differs from PC HP progression; no gameplay state is altered',
                'Ownership flags, flames, collapse and flood effects remain pending'])
    for index in ids:
        data=archive.read(models[index]);header=struct.unpack_from('<8I',data,48)
        if header[:6]!=(1,208,1,288,1,1) or sum(header[6:])!=1:raise ValueError('Unsupported draw group '+str(index))
        mesh,=wkmd_geometry(data);draw=struct.unpack_from('<I',data,160)[0]
        flags,tex=struct.unpack_from('<IH',data,draw)
        primitive,first,nv,start,primitives=struct.unpack_from('<5I',data,draw+24)
        if (tex,primitive,first,nv,start,primitives)!=(65535,5,0,len(mesh['vertices']),0,len(mesh['indices'])-2):raise ValueError('Draw bounds '+str(index))
        triangles=[];strip=mesh['indices']
        for i in range(len(strip)-2):
            tri=strip[i:i+3].tolist()
            if len(set(tri))!=3:continue
            if i&1:tri[0],tri[1]=tri[1],tri[0]
            triangles.extend(tri)
        v=mesh['vertices'].astype('<f4');v[:,:3]*=WORLD_AXES
        norms=v[:,3:6]/WORLD_AXES;length=np.linalg.norm(norms,axis=1)
        if np.any(length<.9):raise ValueError('Missing authored normal')
        v[:,3:6]=norms/length[:,None]
        color=mesh['colors'][:,[2,1,0,3]].astype('<f4')/255
        attributes=np.concatenate([v,color],axis=1).astype('<f4');indices=np.asarray(triangles,dtype='<u4')
        payload+=struct.pack('<4I',index,materials[index],len(v),len(indices))+attributes.tobytes()+indices.tobytes()
        evidence['models'].append(dict(model_index=index,id=models[index],sha256=sha(data),texture_base_index=materials[index],vertices=len(v),triangles=len(indices)//3,draw_flags=hex(flags),source_draw_pass='opaque' if header[6] else 'alpha'))
    # Eight columns, eight rows. 2px edge replication keeps mip sampling from
    # borrowing source pixels from unrelated buildings or seasons.
    if len(texids)>64:raise ValueError('Atlas budget')
    atlas=Image.new('RGBA',(2080,2080))
    for cell,index in enumerate(texids):
        data=archive.read(textures[index]);image=images[index]
        padded=np.pad(np.asarray(image.convert('RGBA')),((2,2),(2,2),(0,0)),mode='edge')
        atlas.paste(Image.fromarray(padded),((cell%8)*260,(cell//8)*260))
        evidence['textures'].append(dict(texture_index=index,id=textures[index],sha256=sha(data),cell=cell,source_size=list(image.size)))
    archive.close();OUT.mkdir(parents=True,exist_ok=True)
    (OUT/'facilities.pcz').write_bytes(gzip.compress(payload,mtime=0));atlas.save(OUT/'atlas.png')
    evidence['outputs']=[dict(path=p.relative_to(ROOT).as_posix(),bytes=p.stat().st_size,sha256=sha(p.read_bytes())) for p in [OUT/'facilities.pcz',OUT/'atlas.png']]
    evidence['coordinate_contract']=contract('PCFAC002')
    (ROOT/'docs/pc-visual/facilities-source.json').write_text(json.dumps(evidence,ensure_ascii=False,indent=2)+'\n')
    inventory_path=ROOT/'docs/pc-visual/inventory.json';inventory=json.loads(inventory_path.read_text())
    model_ids={r['id'] for r in evidence['models']};texture_ids={r['id'] for r in evidence['textures']}
    site_evidence=json.loads((ROOT/'docs/pc-visual/sites-source.json').read_text())
    shared_sites={r['id'] for r in site_evidence['textures']}
    for package in inventory['archives']:
        if package['path'].lower()!='media/san11pkres.bin':continue
        for entry in package['entries']:
            if entry['id'] not in model_ids|texture_ids:continue
            entry.update(conversion='tools/content/import_pc_facilities.py',android_output='app/src/main/assets/3d/pc-facilities/'+('atlas.png' if entry['id'] in texture_ids else 'facilities.pcz'),runtime_binding='PcFacilities / FilamentMapView',validation='source-crosschecked; installed evidence pending')
            if entry['id'] in shared_sites:
                entry['consumers']=[dict(conversion='tools/content/import_pc_sites.py',android_output='app/src/main/assets/3d/pc-sites/atlas.png',runtime_binding='PcSites / FilamentMapView',validation='v133 site evidence; regional alternate binding remains pending'),
                    dict(conversion=entry['conversion'],android_output=entry['android_output'],runtime_binding=entry['runtime_binding'],validation=entry['validation'])]
    inventory_path.write_text(json.dumps(inventory,ensure_ascii=False,indent=2)+'\n')
    manifest_path=ROOT/'tools/content/map-release-manifest.json';manifest=json.loads(manifest_path.read_text())
    for output in evidence['outputs']:
        path=output['path'];entry=dict(apk_path='assets/'+str(Path(path).relative_to('app/src/main/assets')),source_path=path,sha256=output['sha256'])
        old=next((r for r in manifest['files'] if r['source_path']==path),None)
        if old is None:manifest['files'].append(entry)
        else:old.update(entry)
    manifest_path.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(dict(object_kinds=len(bindings),models=len(ids),textures=len(texids),compressed_bytes=(OUT/'facilities.pcz').stat().st_size)))

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('installation',type=Path)
    convert(parser.parse_args().installation)
