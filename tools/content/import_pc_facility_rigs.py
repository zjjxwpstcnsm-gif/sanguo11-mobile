#!/usr/bin/env python3
"""Restore authored catapult-platform skin/curves from exact PC call/table bindings.

The supplied installation stays read-only. Static construction variants and
rules remain independent; these original full-body rigs belong to real firing.
"""
import argparse,gzip,json,struct
from pathlib import Path
import numpy as np
from pc_resources import Archive,sha
from pc_unit_formats import wkmd,fcvd,nested_link
from import_pc_units import EXE_SHA,source_name
ROOT=Path(__file__).resolve().parents[2]

def convert(installation,output,report):
    output=output.resolve();report=report.resolve()
    exe=(installation/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA:raise ValueError('Reinspect changed source executable')
    model_ids=struct.unpack_from('<388I',exe,0x363740)
    texture_ids=struct.unpack_from('<88I',exe,0x363d50)
    texture_bases=struct.unpack_from('<388I',exe,0x363eb0)
    bindings=struct.unpack_from('<2I',exe,0x44926c)
    if bindings!=(73,74) or tuple(model_ids[i] for i in(286,287))!=(2234,2235):raise ValueError('Source animated platform binding changed')
    a=Archive(installation/'Media/san11pkres.bin')
    try:
        pack=a.read(2236);raw_clips=nested_link(pack);models=[];clips=[]
        payload=bytearray(b'PCFRIG01'+struct.pack('<4I',2,2,*bindings))
        for index in(286,287):
            raw=a.read(model_ids[index]);m=wkmd(raw);nv=len(m['attributes']);ni=len(m['indices']);opaque=sum(d['triangles']*3 for d in m['draws'] if not d['alpha_pass'])
            if m['nodes']!=6:raise ValueError('Source platform hierarchy changed')
            payload+=struct.pack('<6I',model_ids[index],m['nodes'],len(m['palette_nodes']),nv,ni,opaque)
            payload+=struct.pack('<'+'i'*m['nodes'],*[n['parent'] for n in m['hierarchy']])
            payload+=struct.pack('<'+'i'*len(m['palette_nodes']),*m['palette_nodes'])+m['matrices'].astype('<f4').tobytes()
            payload+=m['attributes'].astype('<f4').tobytes()+m['weights'].astype('<f4').tobytes()+m['bones'].astype('<u2').tobytes()+m['indices'].tobytes()
            tex=texture_bases[index]
            models.append(dict(model_index=index,resource=model_ids[index],source_file=source_name(exe,0x4a07b0,index),sha256=sha(raw),format='WKMD0010',bones=m['nodes'],vertices=nv,triangles=ni//3,draws=m['draws'],texture_base=tex,texture_resources=[dict(index=t,resource=texture_ids[t],sha256=sha(a.read(texture_ids[t])))for t in range(tex,tex+3)]))
        for clip_index in bindings:
            raw=raw_clips[clip_index];clip=fcvd(raw)
            if clip['nodes']!=6 or clip['frames']!=81:raise ValueError('Source platform curve changed')
            payload+=struct.pack('<3I',clip['frames'],clip['nodes'],len(clip['channels']))
            for c in clip['channels']:
                payload+=struct.pack('<3I',c['node'],c['kind'],len(c['keys']))
                for upper,x,y,z in c['keys']:payload+=struct.pack('<I3f',upper,x,y,z)
            clips.append(dict(pack_resource=2236,nested_id=clip_index,sha256=sha(raw),format='FCVD0022',frames=81,bones=6))
        output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(payload,mtime=0))
        info=dict(schema=1,executable_sha256=sha(exe),source_archive='Media/san11pkres.bin',source_policy='read-only active supplied Media bytes',object_kind=44,facility_id=10,models=models,clips=clips,
            motion_pack=dict(resource=2236,sha256=sha(pack)),
            source_binding=dict(model_selection_va='0x570f39..0x570f61',motion_table_offset='0x44926c',motion_table_sha256=sha(exe[0x44926c:0x449274]),motion_draw_va='0x56f61d..0x56f6ba',source_fps=60,source_delay_seconds=1,source_end_guard_frames=2),
            conversion='retain original hierarchy, palettes, weights, float UV/BGRA, opaque/alpha groups and polynomial coefficients; reuse PcUnits strict source math',
            android_output=output.relative_to(ROOT).as_posix(),output_sha256=sha(output.read_bytes()),decoded_bytes=len(payload),runtime_binding='PcFacilityRigs / actual FACILITY_ATTACK journal / FilamentMapView; construction remains static source variants',
            limits=['Source firing body and curve binding confirmed by EXE; original PC rendered timing and projectile synchronization not accepted','Source delay=1 second and end guard retained by presentation-only duration override; core event duration remains unchanged','Source atlas sampling, climate selection, source camera/contact and alpha order still need PC comparison','Source construction state2+ has no proven six-bone curve; keep original construction mesh'])
        report.parent.mkdir(parents=True,exist_ok=True);report.write_text(json.dumps(info,ensure_ascii=False,indent=2)+'\n')
        if output.resolve()==(ROOT/'app/src/main/assets/3d/pc-facilities/rigs.pcz').resolve():
            manifest=ROOT/'tools/content/map-release-manifest.json';data=json.loads(manifest.read_text())
            row=dict(source_path=info['android_output'],apk_path='assets/3d/pc-facilities/rigs.pcz',sha256=info['output_sha256'])
            entries=[r for r in data['files']if r['source_path']==row['source_path']]
            if len(entries)>1:raise ValueError('Duplicate platform rig integrity pin')
            if entries:entries[0].update(row)
            else:data['files'].append(row)
            manifest.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
        print(json.dumps(dict(models=2,clips=2,bytes=output.stat().st_size,sha256=info['output_sha256'])))
    finally:a.close()
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,default=ROOT/'app/src/main/assets/3d/pc-facilities/rigs.pcz');p.add_argument('--report',type=Path,default=ROOT/'docs/pc-visual/facility-rigs-source.json');a=p.parse_args();convert(a.installation,a.output,a.report)
