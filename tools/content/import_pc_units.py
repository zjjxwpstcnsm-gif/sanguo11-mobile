#!/usr/bin/env python3
"""Reproducible map unit skin/curve conversion from the supplied installation.

Requires numpy/Pillow; source installation is read-only. EXE tables and filenames
are authoritative, not the resource IDs' apparent ordering. Runtime/PC visual
acceptance is independent of this converter's structural validation.
"""
import argparse
import gzip
import json
from pathlib import Path
import struct
import numpy as np
from pc_resources import Archive,sha,wftx
from pc_unit_formats import nested_link,fcvd,wkmd,pose

ROOT=Path(__file__).resolve().parents[2]
EXE_SHA='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
KINDS=('sword','spear','halberd','crossbow','cavalry','bowcavalry','ram','tower','catapult','juggernaut','boat','houseship','steamer','supply')


def source_name(exe,table,index):
    pointer=struct.unpack_from('<I',exe,table+index*4)[0]-0x400000
    if not 0<=pointer<len(exe):raise ValueError('EXE filename pointer')
    return exe[pointer:exe.index(b'\0',pointer)].decode('ascii')


def convert(installation,destination,report_path):
    exe=(installation/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA:raise ValueError('Unit EXE changed; inspect binding before converting')
    archive=Archive(installation/'Media/san11pkres.bin')
    try:
        model_table=struct.unpack_from('<388I',exe,0x363740)
        texture_table=struct.unpack_from('<88I',exe,0x363d50)
        texture_bases=struct.unpack_from('<388I',exe,0x363eb0)
        kind_models=struct.unpack_from('<14H',exe,0x369b94)
        motions=list(struct.iter_unpack('<8B',exe[0x369bb0:0x369bb0+112]))
        if sorted(kind_models)!=list(range(320,334)):raise ValueError('EXE unit model range')
        pack=archive.read(2236);raw_clips=nested_link(pack);clips=[fcvd(d) for d in raw_clips]
        payload=bytearray(b'PCUNIT02'+struct.pack('<4I',14,75,14,8))
        for model,states in zip(kind_models,motions):payload+=struct.pack('<9I',model-320,*states)
        formation_offsets=(0x447a70,0x447cd0,0x447f30,0x448190,0x4483f0,0x448650,0x4488b0,0x448b10)
        formations=b''.join(exe[o:o+76*8] for o in formation_offsets)
        if not np.isfinite(np.frombuffer(formations,'<f4')).all():raise ValueError('EXE formation floats')
        payload+=formations
        destination.mkdir(parents=True,exist_ok=True)
        sheets=[]
        report=dict(schema=1,source_archive='Media/san11pkres.bin',executable_sha256=sha(exe),
            source_policy='read-only; active supplied Media bytes authoritative; auxiliary MOD overrides remain unproven',
            tables=[dict(offset=hex(o),bytes=n,sha256=sha(exe[o:o+n])) for o,n in ((0x363740,388*4),(0x363d50,88*4),(0x363eb0,388*4),(0x369b94,28),(0x369bb0,112))],
            source_functions=[dict(va=hex(a+0x400000),bytes=b-a,sha256=sha(exe[a:b])) for a,b in ((0x11cf0,0x11da9),(0x11b40,0x11b90),(0x12270,0x12389),(0x4b740,0x4b9a0),(0x4bc50,0x4be46),(0x4ab40,0x4abdb),(0x4aff0,0x4b0da),(0x16dc70,0x16dc9f),(0x1648b0,0x1649f2))],
            bindings=[dict(source_kind=i,name=KINDS[i],model_index=kind_models[i],model_resource=model_table[kind_models[i]],motion_by_source_state=list(motions[i]),state_semantics='568e90 assigns state 1 to normal infantry/cavalry in its per-member formation controller and state 0 to ships/siege/supply; semantic event labels remain provisional') for i in range(14)],
            formation=dict(offsets=[hex(o) for o in formation_offsets],slots_per_layout=76,sha256=sha(formations),
                count='normal military foot/cavalry: min(76,24+min(troops,15000)//250); naval/siege/supply one model; status layouts resolver 5648b0',
                runtime_binding='PcUnitFormation: original 76-slot offsets / normal/status7 layouts / independent per-member terrain contact; other source layout transitions pending'),
            motion_pack=dict(resource=2236,format='nested LINK(75) / FCVD0022',bytes=len(pack),sha256=sha(pack),fps=60,
                conversion='retain all original upper frames and three float32 quadratic coefficients; global-frame polynomial; no quaternion normalization'),
            models=[],clips=[],renderer_coordinates=dict(horizontal=.05,vertical=.05,units='source PC world units uniformly to map scene',status='EXE415920/41c356 height and object translation verified; PC image comparison pending'),validation='strict binary, source EXE relationships and CPU pose checks; installed evidence tracked separately in units-validation-working.json; PC visual comparison pending')
        for i in range(14):
            source_index=320+i;resource=model_table[source_index];raw=archive.read(resource);model=wkmd(raw)
            texture_index=texture_bases[source_index];texture_resource=texture_table[texture_index];texture=archive.read(texture_resource)
            images=wftx(texture)
            if len(images)!=1:raise ValueError('unit texture variant count')
            im=images[0].convert('RGBA');w,h=im.size
            if w>256 or h>256:raise ValueError('unit source sheet size')
            sheet=destination/('unit-%02d.png'%i);im.save(sheet);sheets.append(sheet)
            a=model['attributes'].copy()
            palette=len(model['palette_nodes']);nv=len(a);ni=len(model['indices'])
            opaque_indices=sum(d['triangles']*3 for d in model['draws'] if not d['alpha_pass'])
            payload+=struct.pack('<6I',resource,model['nodes'],palette,nv,ni,opaque_indices)
            payload+=struct.pack('<'+'i'*model['nodes'],*[n['parent'] for n in model['hierarchy']])
            payload+=struct.pack('<'+'i'*palette,*model['palette_nodes'])+model['matrices'].astype('<f4').tobytes()
            payload+=a.astype('<f4').tobytes()+model['weights'].astype('<f4').tobytes()+model['bones'].astype('<u2').tobytes()+model['indices'].tobytes()
            # Independently verify inverse binds against the source rest hierarchy.
            globals=[]
            for j,node in enumerate(model['hierarchy']):
                m=model['source_matrices'][j].copy()
                if node['parent']>=0:m=m@globals[node['parent']]
                globals.append(m)
            inverse_error=max((float(np.max(np.abs(model['matrices'][j]@globals[node]-np.eye(4)))) for j,node in enumerate(model['palette_nodes'][:model['binds']]) if node>=0),default=0)
            if inverse_error>2e-5:raise ValueError('source inverse bind/hierarchy mismatch')
            bound=[c for states in motions for c in states if clips[c]['nodes']==model['nodes']]
            samples=[]
            for clip_id in sorted(set(bound)):
                for f in (0,.25,.5,.75,1):
                    p,n=pose(model,clips[clip_id],f*(clips[clip_id]['frames']-1))
                    samples.append(dict(clip=clip_id,frame=f*(clips[clip_id]['frames']-1),position_sha256=sha(p.tobytes()),normal_sha256=sha(n.tobytes())))
            report['models'].append(dict(model_index=source_index,resource=resource,format='WKMD0010',source_file=source_name(exe,0x4a07b0,source_index),sha256=sha(raw),
                texture_resource=texture_resource,texture_file=source_name(exe,0x4a0dc0,texture_index),texture_sha256=sha(texture),texture_size=[w,h],
                bones=model['nodes'],source_bind_matrices=model['binds'],runtime_palettes=palette,vertices=nv,triangles=ni//3,draws=model['draws'],inverse_bind_max_error=inverse_error,
                source_vertex_contract='original positions/normals/UV/BGRA and explicit weights retained; implicit last weight=1-sum; per-draw palette expanded; strip winding/degenerates handled',
                uv_policy='source float UV retained exactly; independent source RGBA sheets avoid packed atlas bleed; original PC sampler still requires runtime comparison',pose_crosschecks=samples,
                android_output=(destination/'units.pcz').relative_to(ROOT).as_posix(),runtime_binding='UnitVisual weapon/ship/mission -> PcUnits.kind/select -> original FCVD CPU skin -> SceneAssetQueue -> instanced original opaque/alpha GpuMesh; state semantics provisional',validation='CPU/source-structure verified; installed scope in units-validation-working.json; PC reference not accepted'))
        for i,(raw,clip) in enumerate(zip(raw_clips,clips)):
            payload+=struct.pack('<3I',clip['frames'],clip['nodes'],len(clip['channels']))
            for c in clip['channels']:
                payload+=struct.pack('<3I',c['node'],c['kind'],len(c['keys']))
                for upper,a,b,d in c['keys']:payload+=struct.pack('<I3f',upper,a,b,d)
            states=[dict(kind=k,state=s) for k,row in enumerate(motions) for s,c in enumerate(row) if c==i]
            report['clips'].append(dict(pack_resource=2236,nested_id=i,format='FCVD0022',bytes=len(raw),sha256=sha(raw),frames=clip['frames'],bones=clip['nodes'],channels=len(clip['channels']),source_states=states,runtime_binding='PcUnits source state table / normal visual journal; semantics provisional' if states else 'preserved unbound; no invented gameplay binding'))
        destination.mkdir(parents=True,exist_ok=True);(destination/'units.pcz').write_bytes(gzip.compress(payload,mtime=0))
        # No old atlas deletion: inherited/generated files remain recoverable until validation.
        report['outputs']=[dict(path=p.relative_to(ROOT).as_posix(),bytes=p.stat().st_size,sha256=sha(p.read_bytes())) for p in [destination/'units.pcz']+sheets]
        report['limits']=['PC screenshot/video comparison unavailable','Uniform source scale based on machine arithmetic; PC camera/contact visual comparison still pending','Original unit event-state mapping and installed GPU fidelity verification pending','Source opaque/alpha draw groups retained; installed transparent sorting still needs verification','Original army flags and terrain contact/formation status transitions pending']
        report_path.parent.mkdir(parents=True,exist_ok=True);report_path.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
        if destination.resolve()==(ROOT/'app/src/main/assets/3d/pc-units').resolve():
            manifest=ROOT/'tools/content/map-release-manifest.json';data=json.loads(manifest.read_text())
            for output in report['outputs']:
                entries=[e for e in data['files'] if e['source_path']==output['path']]
                if len(entries)!=1:raise ValueError('Expected one source-unit integrity entry: '+output['path'])
                entries[0]['sha256']=output['sha256']
            manifest.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
        print(json.dumps(dict(models=14,clips=75,decoded_bytes=len(payload),outputs=report['outputs'],inverse_bind_error=max(m['inverse_bind_max_error'] for m in report['models']))))
    finally:archive.close()


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('installation',type=Path)
    parser.add_argument('--output',type=Path,default=ROOT/'app/src/main/assets/3d/pc-units')
    parser.add_argument('--report',type=Path,default=ROOT/'docs/pc-visual/units-source.json')
    args=parser.parse_args();convert(args.installation,args.output,args.report)
