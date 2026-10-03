#!/usr/bin/env python3
"""Convert four exact source OBJS dams and opening-only authority coordinates."""
import argparse,gzip,json,struct,re
from pathlib import Path
from pc_resources import Archive,objects,sha
ROOT=Path(__file__).resolve().parents[2]
def convert(installation):
    exe=(installation/'san11pk.exe').read_bytes();proof=json.loads((ROOT/'docs/pc-visual/wall-connections-investigation.json').read_text())
    function=next(f for f in proof['functions'] if f['va']=='0x5a0160');off=int(function['va'],16)-0x400000
    if sha(exe)!=proof['executable_sha256'] or sha(exe[off:off+function['bytes']])!=function['sha256']:raise ValueError('Reinspect changed source facility coordinate function')
    archive=Archive(installation/'Media/san11pkres.bin');data=archive.read(4805);archive.close()
    rows=[r for r in objects(data) if r['model']==20]
    if len(rows)!=4 or any(r['tail']!=0 for r in rows):raise ValueError('Reinspect changed source dam placements')
    for r in rows:
        x=(r['x']-57)//2;y=(r['z']-57-(x&1))//2
        if r['x']!=x*2+57 or r['z']!=y*2+(x&1)+57 or not(0<=x<200 and 0<=y<200):raise ValueError('Noncanonical source dam cell')
        r['source_cell']=[x,y]
    raw=b'PCDAMS01'+struct.pack('<I',4)
    for r in rows:raw+=struct.pack('<HHHHBf',r['slot'],r['x'],r['z'],0,r['height'],r['yaw'])
    packed=bytearray(gzip.compress(raw,mtime=0));packed[9]=255
    output=ROOT/'app/src/main/assets/3d/pc-facilities/dams.pcz';output.write_bytes(packed)
    core=ROOT/'core/src/main/resources/maps/pc-dams-v065.properties'
    lines=['# Source OBJS4805 object20. Opening only; save load never seeds these.','format=PC_DAMS_1','mapRevision=65','sourceSha256='+sha(data),'count=4']
    lines+=[f"dam.{i}={r['source_cell'][0]},{r['source_cell'][1]}" for i,r in enumerate(rows)]
    core.write_text('\n'.join(lines)+'\n')
    java=ROOT/'core/src/main/java/game/sanguo/core/PcDamCatalog.java'
    java.write_text(re.sub(r'SHA256="[a-f0-9]{64}"','SHA256="'+sha(core.read_bytes())+'"',java.read_text()))
    facilities=json.loads((ROOT/'docs/pc-visual/facilities-source.json').read_text());binding=next(r for r in facilities['object_bindings'] if r['kind']==20)
    model_indices=set(binding['models'])|{n+1 for n in binding['models']};models=[m for m in facilities['models'] if m['model_index'] in model_indices];texture_indices={m['texture_base_index'] for m in models};textures=[t for t in facilities['textures'] if t['texture_index'] in texture_indices]
    report=dict(schema=1,source_archive='Media/san11pkres.bin',source_id=4805,source_sha256=sha(data),executable_sha256=sha(exe),source_coordinate_function=function,source_model_binding=binding,models=models,textures=textures,placements=rows,
        conversion='Exact half-grid placement, yaw, authored height; native facility-cell inverse from EXE5a0160',
        runtime_binding='PcDamCatalog opening-only → saved War.Structure DAM; PcDams / PcFacilities / FilamentMapView',
        outputs=[dict(path=str(p.relative_to(ROOT)),bytes=p.stat().st_size,sha256=sha(p.read_bytes())) for p in (output,core)],
        validation='conversion only; source body, opening/save behavior and normal attack/flood installation pending',
        limits=['Source ground remains wetland; never change source terrain to make an entity visible','Existing save entities are authoritative; no automatic dam respawn/migration','PC flood/collapse effects, timing and matched runtime captures pending'])
    (ROOT/'docs/pc-visual/dams-source.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    p=ROOT/'tools/content/map-release-manifest.json';m=json.loads(p.read_text())
    for out in report['outputs']:
        path=out['path'];entry=dict(source_path=path,apk_path='assets/3d/pc-facilities/dams.pcz' if path.startswith('app/') else 'maps/pc-dams-v065.properties',sha256=out['sha256']);old=next((e for e in m['files'] if e['source_path']==path),None)
        if old is None:m['files'].append(entry)
        else:old.update(entry)
    p.write_text(json.dumps(m,indent=2)+'\n');print(json.dumps(dict(dams=rows,outputs=report['outputs'])))
if __name__=='__main__':
    a=argparse.ArgumentParser(description=__doc__);a.add_argument('installation',type=Path);convert(a.parse_args().installation)
