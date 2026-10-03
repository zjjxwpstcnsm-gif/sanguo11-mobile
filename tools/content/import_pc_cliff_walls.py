#!/usr/bin/env python3
"""Bind the supplied immutable OBJS kind14 walls to converted original models.

The read-only installation is the sole source of transforms. A wall is scenery;
this converter does not manufacture constructible facilities or blocking rules.
"""
import argparse,gzip,json,struct
from pathlib import Path
from pc_resources import Archive,objects,sha
from pc_scene_coordinates import contract
ROOT=Path(__file__).resolve().parents[2]
def convert(installation):
    archive=Archive(installation/'Media/san11pkres.bin')
    source=archive.read(4805);archive.close()
    rows=[r for r in objects(source) if r['model']==14]
    if len(rows)!=161 or any(r['tail']!=0 for r in rows):raise ValueError('Reinspect changed source wall catalog/state')
    facilities=json.loads((ROOT/'docs/pc-visual/facilities-source.json').read_text())
    binding=next(r for r in facilities['object_bindings'] if r['kind']==14)
    if binding['models']!=[114,116,114,74]:raise ValueError('Reinspect original wall models')
    exe=(installation/'san11pk.exe').read_bytes()
    proof=json.loads((ROOT/'docs/pc-visual/wall-connections-investigation.json').read_text())
    if sha(exe)!=proof['executable_sha256']:raise ValueError('Reinspect changed wall connection program')
    for f in proof['functions']:
        off=int(f['va'],16)-0x400000
        if sha(exe[off:off+f['bytes']])!=f['sha256']:raise ValueError('Wall connection function changed')
    neighbors=struct.unpack_from('<24h',exe,0x39c310)
    def cell(r):
        q=(r['x']*2-112)//4
        return q,(r['z']*2-112-2*(q&1))//4
    lookup={cell(r):r for r in rows}
    if len(lookup)!=len(rows):raise ValueError('Ambiguous original wall occupancy')
    edges=[]
    for r in rows:
        q,z=cell(r);mask=0
        for k in range(6):
            i=((q&1)*6+k)*2;n=lookup.get((q+neighbors[i],z+neighbors[i+1]))
            if n:
                mask|=1<<k
                if k>=3:edges.append(dict(start=r['slot'],end=n['slot'],direction=k))
        r.update(native_cell=[q,z],connection_mask=mask,body_model=114 if not mask else 120 if q&1 or z&1 else 118)
    if len(edges)!=154 or any(not r['connection_mask'] for r in rows):raise ValueError('Reinspect changed source wall topology')
    payload=b'PCWALL02'+struct.pack('<I',len(rows))
    for r in rows:payload+=struct.pack('<HHHBfBH',r['slot'],r['x'],r['z'],r['height'],r['yaw'],r['connection_mask'],r['body_model'])
    target=ROOT/'app/src/main/assets/3d/pc-facilities/cliff-walls.pcz'
    compressed=bytearray(gzip.compress(payload,mtime=0))
    # Python 3.9 and 3.12 otherwise write different gzip OS bytes (255/19)
    # for the same deterministic payload on macOS. Pin the portable header.
    compressed[9]=255
    target.write_bytes(compressed)
    models=[m for m in facilities['models'] if m['model_index'] in (114,115,118,119,120,121)]
    texture_indices={m['texture_base_index'] for m in models}
    textures=[t for t in facilities['textures'] if t['texture_index'] in texture_indices|{n+1 for n in texture_indices}|{n+2 for n in texture_indices}]
    result=dict(schema=2,source_archive='Media/san11pkres.bin',source_id=4805,source_sha256=sha(source),executable_sha256=sha(exe),object_kind=14,source_state=0,placements=rows,edges=edges,models=models,textures=textures,source_model_binding=binding,connection_evidence='wall-connections-investigation.json',conversion='Raw half-grid and height bytes retained; runtime XY *.5 minus28.5, height*.025 and yaw. EXE six-neighbor connection mask and parity-selected pillar; unique segment edges directions3..5. Original geometry/RGBA reused.',runtime_binding='PcCliffWalls.buildWindow / PcWallGeometry / FilamentMapView source facility instance',outputs=[dict(path=target.relative_to(ROOT).as_posix(),bytes=target.stat().st_size,sha256=sha(target.read_bytes()))],limits=['PC camera/scale/lighting/opacity match remains unverified','Scenery only: preserves core blocking/authority/save; does not represent constructible stone walls','Regional winter climate0 is provisional','Source damaged/construction wall states are converted but immutable source placements have state0','Source PC mutates some terrain vertex heights at wall initialization; corresponding visual patch is still under investigation'])
    result['coordinate_contract']=contract('PCWALL02; raw placements, PCFAC002 geometry')
    (ROOT/'docs/pc-visual/cliff-walls-source.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    p=ROOT/'tools/content/map-release-manifest.json';manifest=json.loads(p.read_text());entry=dict(apk_path='assets/3d/pc-facilities/cliff-walls.pcz',source_path=target.relative_to(ROOT).as_posix(),sha256=sha(target.read_bytes()))
    old=next((r for r in manifest['files'] if r['source_path']==entry['source_path']),None)
    if old is None:manifest['files'].append(entry)
    else:old.update(entry)
    p.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(dict(placements=len(rows),models=[m['id'] for m in models],bytes=target.stat().st_size)))
if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('installation',type=Path);convert(parser.parse_args().installation)
