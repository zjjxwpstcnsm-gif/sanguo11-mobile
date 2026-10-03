#!/usr/bin/env python3
"""Read EXE object/model lookup tables; do not infer variant state names.

This installation's resolver is documented in FORMAT_NOTES.md. These are
investigation results, not converted/accepted city assets. Standard library only.
"""
import argparse,json,struct
from pathlib import Path
from pc_resources import Archive,sha

ROOT=Path(__file__).resolve().parents[2]

def inspect(installation):
    exe=(installation/'san11pk.exe').read_bytes()
    archive=Archive(installation/'Media/san11pkres.bin')
    models=struct.unpack_from('<388I',exe,0x363740)
    if models[288:320]!=tuple(range(4808,4840)):
        raise ValueError('Executable model table differs; re-inspect resolver')
    textures=struct.unpack_from('<88I',exe,0x363d50)
    materials=struct.unpack_from('<388I',exe,0x363eb0)
    resources=[]
    for index,resource in enumerate(models):
        data=archive.read(resource)
        if not data.startswith(b'WKMD0010'):
            raise ValueError('Model table entry is not WKMD')
        resources.append(dict(model_index=index,resource_id=resource,sha256=sha(data),bytes=len(data),
            texture_base_index=materials[index],texture_resource=textures[materials[index]],geometry_conversion='see sites-source.json' if index<72 or 76<=index<84 else 'see scenery-source.json' if 288<=index<314 else 'unresolved'))
    bindings=[]
    for kind in list(range(46))+list(range(62,82)):
        offset=0x37a720+kind*8 if kind<46 else 0x37a890+(kind-62)*8
        indices=struct.unpack_from('<4H',exe,offset)
        if any(i+1>=len(models) for i in indices):
            raise ValueError('Object model index out of table')
        bindings.append(dict(object_kind=kind,source_table_offset=offset,
            variants=[dict(variant_index=i,near_model_index=n,far_model_index=n+1,
                near_resource=models[n],far_resource=models[n+1],state_name=('city-scale-by-completed-domestic-facilities' if kind<6 else 'gate-port-HP' if kind in (6,7) else 'not-yet-confirmed'),additional_wall_model_index=n+6 if kind<6 else None) for i,n in enumerate(indices)],
            runtime_binding='PcSites / FilamentMapView' if kind<8 else None,validation='source model/material/state references confirmed; installed evidence in validation.json' if kind<8 else 'EXE-table crosscheck only; material/state mapping pending'))
    archive.close()
    result=dict(schema=1,source_executable='san11pk.exe',executable_sha256=sha(exe),
        source_archive='Media/san11pkres.bin',resolver_va='0x41bae0',model_table_offset=0x363740,
        model_table_sha256=sha(exe[0x363740:0x363740+388*4]),
        function_evidence=[dict(va=hex(va),bytes=length,sha256=sha(exe[va-0x400000:va-0x400000+length]))
            for va,length in [(0x41bae0,198),(0x41bbb0,200),(0x4118c9,24),(0x41199d,64),(0x41cf70,400),(0x5a03e0,731),(0x47b630,107)]],
        model_resources=resources,object_bindings=bindings,texture_resources=[dict(texture_index=i,id=r,sha256=sha(ArchiveRead(installation,r))) for i,r in enumerate(textures)],
        texture_table_offset=0x363d50,model_texture_table_offset=0x363eb0,
        limits=['Other object kinds HP/construction transitions remain unresolved','Gate/port regional winter branch still requires climate table binding','Source visual comparison unavailable','Kinds46..61 use specialized resolver, not the generic four-variant table'])
    target=ROOT/'docs/pc-visual/object-bindings.json'
    target.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(dict(model_table_entries=len(models),generic_object_kinds=len(bindings),runtime_assets_added=0)))

def ArchiveRead(installation,index):
    a=Archive(installation/'Media/san11pkres.bin')
    try:return a.read(index)
    finally:a.close()

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('installation',type=Path)
    inspect(parser.parse_args().installation)
