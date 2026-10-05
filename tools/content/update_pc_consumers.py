#!/usr/bin/env python3
"""Rebuild shared PC-resource consumers without losing another renderer binding.

Run after resource converters. This changes provenance only, not game assets.
Every referenced source digest must equal the original archive inventory.
"""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[2]
DOC=ROOT/'docs/pc-visual'

def update():
    path=DOC/'inventory.json';inventory=json.loads(path.read_text())
    archive=next(a for a in inventory['archives'] if a['path'].lower()=='media/san11pkres.bin')
    entries={e['id']:e for e in archive['entries']};consumers={}
    def bind(resource,digest,tool,output,runtime,validation_file="validation.json"):
        entry=entries[resource]
        if entry['sha256']!=digest:raise ValueError(f'Source consumer digest differs: {resource}')
        row=dict(conversion='tools/content/'+tool,android_output=output,runtime_binding=runtime,
            validation='source-crosschecked; installed scope in '+validation_file+'; matched PC pixels pending')
        if row not in consumers.setdefault(resource,[]):consumers[resource].append(row)
    for filename,tool,folder,runtime in [
        ('scenery-source.json','import_pc_scenery.py','pc-scenery','PcScenery / FilamentMapView'),
        ('sites-source.json','import_pc_sites.py','pc-sites','PcSites / FilamentMapView'),
        ('facilities-source.json','import_pc_facilities.py','pc-facilities','PcFacilities / PcConstructibleWalls / FilamentMapView')]:
        source=json.loads((DOC/filename).read_text());base='app/src/main/assets/3d/'+folder+'/'
        for row in source['models']:bind(row['id'],row['sha256'],tool,base+{'pc-scenery':'scenery.pcz','pc-sites':'sites.pcz','pc-facilities':'facilities.pcz'}[folder],runtime)
        for row in source['textures']:bind(row['id'],row['sha256'],tool,base+'atlas.png',runtime)
        if 'objects_sha256' in source:bind(4805,source['objects_sha256'],tool,base+('scenery.pcz' if folder=='pc-scenery' else 'sites.pcz'),runtime)
        if 'objects_resource_sha256' in source:bind(4805,source['objects_resource_sha256'],tool,base+'sites.pcz',runtime)
    walls=json.loads((DOC/'cliff-walls-source.json').read_text());tool='import_pc_cliff_walls.py';runtime='PcCliffWalls / PcWallGeometry / FilamentMapView'
    bind(4805,walls['source_sha256'],tool,'app/src/main/assets/3d/pc-facilities/cliff-walls.pcz',runtime)
    for row in walls['models']:bind(row['id'],row['sha256'],tool,'app/src/main/assets/3d/pc-facilities/facilities.pcz',runtime)
    for row in walls['textures']:bind(row['id'],row['sha256'],tool,'app/src/main/assets/3d/pc-facilities/atlas.png',runtime)
    dams=json.loads((DOC/'dams-source.json').read_text())
    bind(dams['source_id'],dams['source_sha256'],'import_pc_dams.py','app/src/main/assets/3d/pc-facilities/dams.pcz; core/src/main/resources/maps/pc-dams-v065.properties','PcDamCatalog opening-only / PcDams / PcFacilities / FilamentMapView')
    for row in dams['models']:bind(row['id'],row['sha256'],'import_pc_facilities.py','app/src/main/assets/3d/pc-facilities/facilities.pcz','PcDams live placement / PcFacilities body/state/LOD / FilamentMapView')
    for row in dams['textures']:bind(row['id'],row['sha256'],'import_pc_facilities.py','app/src/main/assets/3d/pc-facilities/atlas.png','PcDams live placement / PcFacilities body/state/LOD / FilamentMapView')
    units_path=DOC/'units-source.json'
    if units_path.exists():
        units=json.loads(units_path.read_text());base='app/src/main/assets/3d/pc-units/'
        for index,row in enumerate(units['models']):
            bind(row['resource'],row['sha256'],'import_pc_units.py',base+'units.pcz','PcUnits original skin / PcUnitFormation / FilamentMapView; event semantics provisional','units-validation-working.json')
            bind(row['texture_resource'],row['texture_sha256'],'import_pc_units.py',base+f'unit-{index:02d}.png','FilamentMapView independent original RGBA sheet / opaque and alpha unit materials','units-validation-working.json')
        pack=units['motion_pack']
        bind(pack['resource'],pack['sha256'],'import_pc_units.py',base+'units.pcz','PcUnits FCVD curves / normal journal replay; original state semantic and PC timing comparison pending','units-validation-working.json')
    rigs_path=DOC/'facility-rigs-source.json'
    if rigs_path.exists():
        rigs=json.loads(rigs_path.read_text());output=rigs['android_output'];tool='import_pc_facility_rigs.py';runtime='PcFacilityRigs six-bone platform / real FACILITY_ATTACK / FilamentMapView'
        for row in rigs['models']:
            bind(row['resource'],row['sha256'],tool,output,runtime,'facility-rigs-validation-working.json')
            for texture in row['texture_resources']:bind(texture['resource'],texture['sha256'],tool,'app/src/main/assets/3d/pc-facilities/atlas.png',runtime,'facility-rigs-validation-working.json')
        bind(rigs['motion_pack']['resource'],rigs['motion_pack']['sha256'],tool,output,runtime,'facility-rigs-validation-working.json')
    worker_path=DOC/'worker-source-working.json'
    palette_path=DOC/'ground-palette-source.json'
    if palette_path.exists():
        palette=json.loads(palette_path.read_text())
        for row in palette['resources']:
            bind(row['id'],row['sha256'],'pc_ground_palette.py',row['android_output']+'; app/src/main/assets/3d/pc-map/palette-sizes.png',
                palette['runtime_binding'],'validation-v156-working.json')
    ground_path=DOC/'ground-passes-source.json'
    if ground_path.exists():
        ground=json.loads(ground_path.read_text())
        env=json.loads((DOC/'environment-source-working.json').read_text())
        row=next(r for r in env['resources']if r['id']==4799)
        bind(4799,row['sha256'],'import_pc_environment.py',row['android_output'],'PcEnvironment / FilamentMapView ground: base seasonal direction, ambient, original c26/c27 fog/fade; source weather variants/transitions unbound','validation-v157-working.json')
        for row in ground['resources']:
            if row['role'].startswith('Original source ground grid'):continue # packaged candidate, no runtime consumer
            bind(row['resource'],row['resource_sha256'],'import_pc_ground_passes.py',row['output'],row['runtime_binding'],'validation-v157-working.json')
    if worker_path.exists():
        worker=json.loads(worker_path.read_text());runtime=worker['runtime_binding']
        bind(4792,'0703ce051a133a6abd01188c90a713e27ffc95e750da7b5fe2904fa498637b2c','pack_pc_effect_scene.py',
             'app/src/main/assets/3d/pc-effects/source-scene.bin',runtime,'validation-v152-working.json')
        binding=json.loads((DOC/'effect-bindings-source.json').read_text())
        effects_by_resource={row['resource_id']:row for row in binding['effects']}
        for resource in worker['source_ksef_resources']:
            bind(resource,effects_by_resource[resource]['sha256'],'pack_pc_effect_scene.py',
                 'app/src/main/assets/3d/pc-effects/source-scene.bin',runtime,'validation-v152-working.json')
        textures=json.loads((DOC/'effect-textures-source.json').read_text())
        bind(124,textures['source_sha256'],'import_pc_effect_textures.py',
             'app/src/main/assets/3d/pc-effects/image-00.png..image-32.png',
             'original124 image index -> PcMapEffects.loadTexture / source ordered quads','validation-v152-working.json')
    for resource,rows in consumers.items():
        entry=entries[resource];entry['consumers']=rows
        # Retain a concise compatibility summary, but keep all bindings explicit.
        for key in ('conversion','android_output','runtime_binding'):entry[key]='; '.join(dict.fromkeys(row[key] for row in rows))
        entry['validation']=rows[0]['validation']
    effects_path=DOC/'effect-bindings-source.json'
    if effects_path.exists():
        # Investigation is separate from runtime consumers: diagnostic PNGs do
        # not enter the APK and unresolved source nodes must not imply integration.
        source=json.loads(effects_path.read_text())
        for row in source['effects']+source['textures']+source['extra_pk_effects']:
            entry=entries[row['resource_id']]
            if entry['sha256']!=row['sha256']:raise ValueError('Effect investigation digest differs')
            value=dict(tool='tools/content/inspect_pc_effect_bindings.py',record='effect-bindings-source.json',
                       source_file=row['source_file'],android_output=None,runtime_binding=None,
                       validation='source graph boundaries / exact texture pixels only; material/time/event semantics unresolved')
            previous=[r for r in entry.get('investigations',[])if r.get('tool')!=value['tool']]
            entry['investigations']=previous+[value]
    path.write_text(json.dumps(inventory,ensure_ascii=False,indent=2)+'\n')
    print(f'PASS {len(consumers)} source resources / {sum(len(v) for v in consumers.values())} explicit consumer bindings')

if __name__=='__main__':update()
