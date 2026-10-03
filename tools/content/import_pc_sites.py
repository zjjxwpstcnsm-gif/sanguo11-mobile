#!/usr/bin/env python3
"""Reproduce source city, wall, gate and port geometry and source texture sheets.

Read-only installation. Requires pinned numpy/Pillow runtime used for scenery.
Table addresses are this installation's executable, not portable game guesses.
"""
import argparse,gzip,json,struct
from pathlib import Path
import numpy as np
from PIL import Image
from pc_scene_coordinates import WORLD_AXES, contract
from pc_resources import Archive,objects,wkmd_geometry,wftx,sha
from pc_region_climate import RegionClimate
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'app/src/main/assets/3d/pc-sites'
def convert(installation,climate_reference):
    exe=(installation/'san11pk.exe').read_bytes(); archive=Archive(installation/'Media/san11pkres.bin')
    models=struct.unpack_from('<388I',exe,0x363740)
    textures=struct.unpack_from('<88I',exe,0x363d50)
    materials=struct.unpack_from('<388I',exe,0x363eb0)
    bindings=[struct.unpack_from('<4H',exe,0x37a720+k*8) for k in range(8)]
    if models[:12]!=(4352,4353,4354,4355,4350,4351,4348,4349,4346,4347,4344,4345):raise ValueError('City model table changed')
    ids=list(range(72))+list(range(76,84)); texids=list(range(12))+list(range(15,18)); cells={n:i for i,n in enumerate(texids)}
    rows=[r for r in objects(archive.read(4805)) if r['model']<8]
    if len(rows)!=87 or any(r['tail'] for r in rows):raise ValueError('Site placement/state contract changed')
    climate=RegionClimate(exe,archive.read(4791),climate_reference)
    for r in rows:r['climate']=climate.resolve(r)['climate']
    anchors={}
    map_lines=(ROOT/'core/src/main/resources/maps/national-map-v056.properties').read_text().splitlines()
    for line in map_lines:
        if line.startswith('site.'):
            key,value=line.split('=',1);sid=int(key[5:]);x,z=map(int,value.split(','));anchors[sid]=(x,z)
    used=set()
    for r in rows:
        kind=r['model'];source_x=r['x']*.5-28.5;source_z=r['z']*.5-28.5
        candidates=[(abs(x-source_x)**2+abs(z+(x%2)*.5-source_z)**2,sid,x,z) for sid,(x,z) in anchors.items() if (sid-20000<42 if kind<6 else 42<=sid-20000<52 if kind==6 else 52<=sid-20000<87)]
        distance,sid,x,z=min(candidates)
        if distance>1.26 or sid in used:raise ValueError('Ambiguous site anchor')
        used.add(sid);r.update(site_id=sid,anchor_x=x,anchor_z=z,anchor_error_world=distance**.5)
    payload=bytearray(b'PCSIT004'+struct.pack('<III',len(rows),len(ids),len(texids)))
    for row in bindings:payload+=struct.pack('<4I',*row)
    for n in texids:payload+=struct.pack('<I',n)
    for r in rows:payload+=struct.pack('<HHHHHHfH',r['model'],r['x'],r['z'],r['height'],r['anchor_x'],r['anchor_z'],r['yaw'],r['climate'])
    evidence=dict(schema=1,source_archive='Media/san11pkres.bin',executable_sha256=sha(exe),objects_sha256=sha(archive.read(4805)),placements=rows,models=[],textures=[],bindings=bindings,
        resolver=dict(model='0x41bae0',material='0x41bbb0',city_two_components='0x41cf70',state_update='0x5a03e0'),
        states=dict(city_buildings='completed domestic facility count <=4: variant1; <=8: variant0; >8: variant2 (VA0x47b630 count)',city_walls='defense <500: variant2; <1000: variant1; otherwise variant0',gate_port='defense < min(baseDefense/2,500): variant1; otherwise variant0'),
        **climate.evidence(),climate_resolution=[climate.resolve(r)for r in rows],
        runtime_binding='PcSites / FilamentMapView; source body/wall states and original regional winter texture delta; installation pending',
        limits=['Original geographic climate snapshot from actual207 autumn; other MOD launch state unverified','Uniform source XYZ .05; machine arithmetic and camera verified, PC pixel comparison pending','Yangping gate authority remains inherited source26,84 while OBJS body is27,83.5; explicit visual anchor mapping, no save relocation','Original ownership flags not converted','Lighting/vertex-color and source opaque/alpha draw state remain subject to PC comparison'])
    for index in ids:
        data=archive.read(models[index]); header=struct.unpack_from('<8I',data,48)
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
        if np.any(length<.9):raise ValueError('Missing source normal')
        v[:,3:6]=norms/length[:,None]
        color=mesh['colors'][:,[2,1,0,3]].astype('<f4')/255
        attributes=np.concatenate([v,color],axis=1).astype('<f4');indices=np.asarray(triangles,dtype='<u4')
        if materials[index] not in cells:raise ValueError('Material absent')
        payload+=struct.pack('<IIII',index,materials[index],len(v),len(indices))+attributes.tobytes()+indices.tobytes()
        evidence['models'].append(dict(model_index=index,id=models[index],sha256=sha(data),texture_base_index=materials[index],vertices=len(v),triangles=len(indices)//3,draw_flags=hex(flags),source_draw_pass='opaque' if header[6] else 'alpha'))
    atlas=Image.new('RGBA',(1040,1040))
    for index in texids:
        data=archive.read(textures[index]);image,=wftx(data)
        if image.size!=(256,256):raise ValueError('Unexpected source site sheet')
        cell=cells[index];x=(cell%4)*260+2;y=(cell//4)*260+2
        padded=np.pad(np.asarray(image.convert('RGBA')),((2,2),(2,2),(0,0)),mode='edge')
        atlas.paste(Image.fromarray(padded),(x-2,y-2))
        evidence['textures'].append(dict(texture_index=index,id=textures[index],sha256=sha(data),cell=cell,source_size=[256,256]))
    archive.close();OUT.mkdir(parents=True,exist_ok=True)
    (OUT/'sites.pcz').write_bytes(gzip.compress(payload,mtime=0));atlas.save(OUT/'atlas.png')
    evidence['conversion']='FVF112 source positions/normals/UV/BGRA; strips→triangles; no geometry repaint; texture sheets exact RGBA with two replicated edge texels'
    evidence['outputs']=[dict(path=p.relative_to(ROOT).as_posix(),bytes=p.stat().st_size,sha256=sha(p.read_bytes())) for p in [OUT/'sites.pcz',OUT/'atlas.png']]
    evidence['coordinate_contract']=contract('PCSIT004')
    (ROOT/'docs/pc-visual/sites-source.json').write_text(json.dumps(evidence,ensure_ascii=False,indent=2)+'\n')
    inventory_path=ROOT/'docs/pc-visual/inventory.json'
    inventory=json.loads(inventory_path.read_text());model_ids={r['id'] for r in evidence['models']};texture_ids={r['id'] for r in evidence['textures']}
    for package in inventory['archives']:
        if package['path'].lower()!='media/san11pkres.bin':continue
        for entry in package['entries']:
            if entry['id'] not in model_ids|texture_ids:continue
            entry.update(conversion='tools/content/import_pc_sites.py',android_output='app/src/main/assets/3d/pc-sites/'+('atlas.png' if entry['id'] in texture_ids else 'sites.pcz'),runtime_binding='PcSites / FilamentMapView',validation='source-crosschecked; installed evidence pending')
    inventory_path.write_text(json.dumps(inventory,ensure_ascii=False,indent=2)+'\n')
    manifest_path=ROOT/'tools/content/map-release-manifest.json';manifest=json.loads(manifest_path.read_text())
    for output in evidence['outputs']:
        path=output['path'];entry=dict(apk_path='assets/'+str(Path(path).relative_to('app/src/main/assets')),source_path=path,sha256=output['sha256'])
        old=next((r for r in manifest['files'] if r['source_path']==path),None)
        if old is None:manifest['files'].append(entry)
        else:old.update(entry)
    manifest_path.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(dict(placements=len(rows),models=len(ids),textures=len(texids),compressed_bytes=(OUT/'sites.pcz').stat().st_size)))
if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('installation',type=Path);parser.add_argument('--climate-reference',type=Path,default=ROOT/'docs/pc-visual/region-climate-live-source.json');a=parser.parse_args();convert(a.installation,a.climate_reference)
