#!/usr/bin/env python3
"""Original SENV and painting lookup conversion; source installation read-only."""
import argparse
import json
import math
from pathlib import Path
import struct
from pc_resources import Archive, sha, wftx_levels
from inspect_pc_effect_bindings import EXE_SHA

ROOT = Path(__file__).resolve().parents[2]


def environment(data):
    if len(data) != 968 or data[:8] != b'SENV0002':
        raise ValueError('SENV0002 4 seasons x 6 variants x 40 bytes')
    rows = []
    for i in range(24):
        offset = 8+i*40
        direction = struct.unpack_from('<3f', data, offset)
        ambient = struct.unpack_from('<3f', data, offset+12)
        if not all(math.isfinite(v) for v in direction+ambient) or sum(v*v for v in direction) < 1e-8:
            raise ValueError('SENV nonfinite/zero light vector')
        color = struct.unpack_from('<I', data, offset+24)[0]
        start, end, density, extra = data[offset+28:offset+32]
        rows.append(dict(season=i//6, variant=i%6, offset=offset,
            direction=list(direction), ambient=list(ambient), fog_bgra=hex(color),
            fog_start_percent=start, fog_end_percent=end, fog_density_percent=density,
            renderer_byte_1dd=extra, tail_colors_raw=[hex(x) for x in struct.unpack_from('<2I', data, offset+32)],
            sha256=sha(data[offset:offset+40])))
    return rows


def convert(installation):
    exe = (installation/'san11pk.exe').read_bytes()
    if sha(exe) != EXE_SHA:
        raise ValueError('Reinspect environment executable')
    archive = Archive(installation/'Media/san11pkres.bin')
    destination = ROOT/'app/src/main/assets/3d/pc-environment'
    destination.mkdir(parents=True, exist_ok=True)
    report = dict(schema=1, goal_complete=False, source_archive='Media/san11pkres.bin',
        executable_sha256=EXE_SHA, source_policy='read-only', resources=[], outputs=[],
        season_order=['spring','summer','autumn','winter'],
        season_binding='4825a0 month quarters -> 4d4010 -> 5a2ec0 stores SENV season; 5a2f04 uses 4788/4789/4787/4790',
        source_shader_va='0x77ae68',
        source_shader='vs.1.1 static model: color*c4, UV0, UV1.x=max(dot(world normal,c18),0), UV1.y=c2.y',
        draw_state='41b420: stage0/stage1 COLOROP MODULATE2X, stage1 UV index1; sampler1 address clamp; alpha test GREATER 0 plus SRCALPHA/INVSRCALPHA blending and depth writes',
        shader_constants='44c7d4 uploads 794884=(.5,.5,.5,.5) to c2; 41b624 uploads 794894=(1,1,1,1) to c4; 44cb44 uploads light first column to c18',
        fog_contract='5a2530: near+(far-near)*percent*float32(.01); density=min(255,trunc(percent*float32(2.55))); 4419f0 BGRA bytes',
        default_variant='4d43d4 -> 5a2e70(0); other variants and 1500ms interrupted transitions not yet bound',
        limits=['No PC rendered reference accepted','Source outline pass not yet ported','Static material and original ground variants are separate; unit source material remains incomplete','Ground base seasonal ambient/fog/fade bound in v157; static/unit source fog and weather transitions still pending','Current anisotropic world scale retained pending calibration','Original sampler filter/color-space/mip behavior and transparent blend target still require PC comparison','Original culling and intra/inter-model sorting not yet accepted'])
    try:
        raw = archive.read(4799)
        report['states'] = environment(raw)
        (destination/'environment.bin').write_bytes(raw)
        report['resources'].append(dict(id=4799,format='SENV0002',bytes=len(raw),sha256=sha(raw),
            android_output='app/src/main/assets/3d/pc-environment/environment.bin',runtime_binding='PcEnvironment base seasonal light; ground original ambient/c26 fog/c27 fade in FilamentMapView; other renderer pipelines still pending'))
        for resource, name in ((4806,'paint'),(4807,'silhouette')):
            raw = archive.read(resource)
            image, = wftx_levels(raw)
            if image['extra_mips'] != 0:
                raise ValueError('Reinspect source painting mips')
            pixels = image['levels'][0].convert('RGBA')
            pixels.save(destination/(name+'.png'))
            alias='media/stage/'+name+'.wft'
            overrides=[]
            for path in (installation/'Media'/'stage').glob('*'):
                if path.is_file() and path.name.casefold()==(name+'.wft').casefold():
                    overrides.append(dict(path=path.relative_to(installation).as_posix(),bytes=path.stat().st_size,
                        sha256=sha(path.read_bytes()),activation='unproven; source archive/loose loading branch requires running PC evidence'))
            report['resources'].append(dict(id=resource,format='WFTX0010',source_file=alias,
                potential_overrides=overrides,selection='supplied archive bytes; original loader also supports loose alias; PC active branch unproven',
                bytes=len(raw),sha256=sha(raw),width=image['width'],height=image['height'],pixel_sha256=sha(pixels.tobytes()),
                android_output='app/src/main/assets/3d/pc-environment/'+name+'.png',
                runtime_binding='FilamentMapView static original painting sampler1' if resource==4806 else None,
                validation='source decoded; installed and PC visual acceptance separate'))
        for path in sorted(destination.iterdir()):
            report['outputs'].append(dict(path=path.relative_to(ROOT).as_posix(),bytes=path.stat().st_size,sha256=sha(path.read_bytes())))
        (ROOT/'docs/pc-visual/environment-source-working.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
        inventory_path=ROOT/'docs/pc-visual/inventory.json'
        inventory=json.loads(inventory_path.read_text())
        for package in inventory['archives']:
            if package['path'].lower()!='media/san11pkres.bin': continue
            for entry in package['entries']:
                source=next((r for r in report['resources'] if r['id']==entry['id']),None)
                if source:
                    entry.update(conversion='tools/content/import_pc_environment.py',android_output=source['android_output'],runtime_binding=source['runtime_binding'],validation='source decoded; runtime integration candidate; PC comparison pending' if source['runtime_binding'] else 'source decoded and packaged only; runtime outline unbound')
        inventory_path.write_text(json.dumps(inventory,ensure_ascii=False,indent=2)+'\n')
        manifest_path=ROOT/'tools/content/map-release-manifest.json'
        manifest=json.loads(manifest_path.read_text())
        for output in report['outputs']:
            path=output['path']; entry=dict(source_path=path,apk_path='assets/'+str(Path(path).relative_to('app/src/main/assets')),sha256=output['sha256'])
            previous=next((r for r in manifest['files'] if r['source_path']==path),None)
            if previous is None:manifest['files'].append(entry)
            else:previous.update(entry)
        manifest_path.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
        print(json.dumps(dict(states=24,textures=2,outputs=len(report['outputs']))))
    finally:
        archive.close()


if __name__ == '__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('installation',type=Path)
    convert(parser.parse_args().installation)
