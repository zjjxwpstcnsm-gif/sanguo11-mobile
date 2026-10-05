#!/usr/bin/env python3
"""Reproduce original fullscreen geometry timelines and original dynamic pixels.

No Wine, source writes, fonts, AI art, resampling or guessed emitter equations.
750ms is the Android default within the user's approved 500–1000ms range, not measured PC time.
"""
import argparse,gzip,json,struct
from pathlib import Path
from pc_resources import Archive,sha,wftx
from pc_effect_machine import VerifiedSourceExecutable
from pc_presentation_source import SourcePresentation

ROOT=Path(__file__).resolve().parents[2]

def convert(installation,templates,selectors):
    verified=VerifiedSourceExecutable((installation/'san11pk.exe').read_bytes())
    archive=Archive(installation/'Media/san11pkres.bin')
    output=ROOT/'app/src/main/assets/3d/pc-presentations';output.mkdir(parents=True,exist_ok=True)
    report_path=ROOT/'docs/pc-visual/presentations-source-working.json'
    previous=json.loads(report_path.read_text()) if report_path.exists() else {}
    if previous and previous['source_executable_sha256']!=sha(verified.data):raise ValueError('Previous presentation source differs')
    records=[r for r in previous.get('records',[]) if ('effect_index' in r and r['effect_index'] not in templates) or ('selector' in r and r['selector'] not in selectors)];pins=[]
    def pin(path):
        pins.append(dict(source_path=str(path.relative_to(ROOT)),apk_path='assets/3d/pc-presentations/'+path.name,sha256=sha(path.read_bytes())))
    try:
        for index in templates:
            source=SourcePresentation(verified,archive,index);frames=[];rejected=0
            try:
                for frame in range(125):
                    quads=source.frame(0 if frame==0 else 1/60)
                    rejected+=source.rejected
                    data=bytearray(struct.pack('<I',len(quads)))
                    for q in quads:
                        if q['blend_operation']!=1 or q['blend_source']!=5 or q['blend_destination']not in(2,6):raise ValueError('Unexamined fullscreen blend')
                        data.extend(struct.pack('<2I',q['texture_index'],q['blend_destination']))
                        data.extend(bytes.fromhex(q['quad_vb_hex']));data.extend(bytes.fromhex(q['quad_matrix_hex']))
                    frames.append(bytes(data))
                body=b'PCPSQ001'+struct.pack('<4I',index,source.resource,60,len(frames))+b''.join(frames)
                # AGP transparently expands/renames .gz asset files. Retain the
                # compressed container bytes with a dedicated .pcps extension.
                path=output/f'template-{index}.pcps';path.write_bytes(gzip.compress(body,mtime=0));pin(path)
                records.append(dict(effect_index=index,resource_id=source.resource,source_sha256=sha(source.raw),
                    output=path.name,output_sha256=sha(path.read_bytes()),decoded_sha256=sha(body),frames=len(frames),
                    maximum_quads=max(struct.unpack_from('<I',f)[0]for f in frames),original_sample_rate=60,
                    native_queue_rejected_packets=rejected,
                    source_factory='413d20: original4133d0/413420 branches;457790/457800/457880;413770 callback',
                    source_update='original45a2b0 reserves244 slots;45a530 updates registered children and source camera provider',
                    draw_order='original458650 depth insertion;4096 buckets descending, original linked order',
                    geometry='original442c00 final96-byte XYZ/BGRA/UV and64-byte matrix per quad'))
                print(json.dumps(dict(template=index,resource=source.resource,frames=len(frames),bytes=path.stat().st_size)),flush=True)
            finally:source.close()
        common=wftx(archive.read(124))[32].convert('RGBA')
        for selector in selectors:
            template,top=struct.unpack_from('<2I',verified.data,0x3774e0+selector*8)
            if template not in (115,121,122):raise ValueError('Unexamined dynamic presentation template')
            raw=archive.read(369+selector);images=wftx(raw)
            if len(images)!=1:raise ValueError('Single original dynamic image')
            image=images[0].convert('RGBA');atlas=common.copy()
            if top<0 or top+image.height>atlas.height or image.width>atlas.width:raise ValueError('Source4142b0 copy rectangle')
            atlas.paste(image,(0,top)) # Raw RGBA replacement, never alpha compositing.
            path=output/f'selector-{selector}.png';atlas.save(path,optimize=True);pin(path)
            records.append(dict(selector=selector,template=template,resource_id=369+selector,source_sha256=sha(raw),
                source_rgba_sha256=sha(image.tobytes()),destination=[0,top,image.width,top+image.height],
                output=path.name,rgba_sha256=sha(atlas.tobytes()),output_sha256=sha(path.read_bytes())))
    finally:archive.close()
    # Partial template reconstruction retains only verified existing outputs.
    for record in records:
        path=output/record['output']
        if sha(path.read_bytes())!=record['output_sha256']:raise ValueError('Presentation output changed outside converter: '+path.name)
        if not any(p['source_path']==str(path.relative_to(ROOT)) for p in pins):pin(path)
    records.sort(key=lambda r:(0,r['effect_index']) if 'effect_index' in r else (1,r['selector']))
    manifest=ROOT/'tools/content/map-release-manifest.json';d=json.loads(manifest.read_text())
    for p in pins:
        old=[x for x in d['files']if x['source_path']==p['source_path']]
        if len(old)>1:raise ValueError('Duplicate pin')
        if old:old[0].update(p)
        else:d['files'].append(p)
    manifest.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
    report=dict(schema=1,goal_complete=False,status='SOURCE_CONVERTED_RUNTIME_ACCEPTANCE_SEPARATE',
        source_executable_sha256=sha(verified.data),source_archive=str(installation/'Media/san11pkres.bin'),
        android_duration_millis=750,android_duration_range_millis=[500,1000],duration_basis='User accepts 0.5–1 second per critical; Android default 0.75 second',records=records,pins=pins,
        runtime_binding='PcPresentationPlan immutable journal cues → MapHost/TurnPlayback → PcPresentations original GPU packets',
        binding_scope=['Six canonical tactic critical actors: installed scenario identity/FCE/age lookup; scenario-portraits-source-working.json; template115',
            'Successful critical SORCERY:selector126/template121; original5933a0 plot7/name8aecb4 ->592050/592153',
            'Successful critical LIGHTNING:selector127/template122; original5933a0 plot8/name8aecb4 ->592ed0/592fb8',
            'Previous CONFUSE126 binding was incorrect and removed; other ordinary plot fullscreen calls remain unresolved'],
        common_texture_resource=124,common_dynamic_slot=32,
        camera='Original441ab0/441b80: identity RH view,30deg perspective,near1/far1000/aspect4:3, normalized by far; fullscreen PC lens parity remains pending',
        limits=['No Wine launched; supplied installation remains read-only',
            'Lens/fullscreen pixel/MOD parity remain unaccepted; external clips are visual reference only',
            'Fixed source visual RNG initialization during conversion, never gameplay RNG',
            'Only templates115/121/122 and the explicitly converted selectors; remaining officers and plot calls pending',
            'Successful noncritical magic presentation policy remains pending original caller argument semantics'])
    report_path.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path)
    p.add_argument('--templates',type=int,nargs='+',default=[115,121,122],choices=[115,121,122],help='Rebuild selected original timelines; verify and retain other existing outputs')
    p.add_argument('--selectors',type=int,nargs='+',default=[126,127,131,132,133,134,136,143,151,152,153,154,156,163],help='Copy original dynamic pixels; preserve other verified outputs')
    a=p.parse_args();convert(a.installation,a.templates,sorted(set(a.selectors)))
