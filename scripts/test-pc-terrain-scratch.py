#!/usr/bin/env python3
"""Compare actual PC/legacy terrain buffers with an immutable pre-change source.

Usage: python3 scripts/test-pc-terrain-scratch.py /path/to/SceneMesh.java
The default is the v143 archive in this workspace. Output digests include every
vertex, index, material/tangent stream and grid. Allocation is desktop-only.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
p = argparse.ArgumentParser(description=__doc__)
p.add_argument('baseline', nargs='?', type=Path, default=ROOT/'out/pc-visual/v143/source-snapshot/app/src/main/java/game/sanguo/mobile/SceneMesh.java')
p.add_argument('--report', type=Path, default=ROOT/'out/pc-visual/v144-terrain-scratch-host.json')
a = p.parse_args()
baseline = a.baseline.resolve()
if not baseline.is_file():
    raise SystemExit('Provide the archived pre-change SceneMesh.java; no synthesized reference is used')
work = ROOT/'app/build/pc-terrain-scratch-check'
common = work/'common';common.mkdir(parents=True, exist_ok=True)
names = 'TileGeometry GridWorldTransform SceneCamera ScenePicking SceneMesh UnitVisual UnitMotion CombatVisual SiteVisual TerrainSurface WaterVisualField TerrainMaterialField MapSceneSnapshot FactionColors SiegeOverlay'.split()
sources = list((ROOT/'core/src/main/java').rglob('*.java'))+list((ROOT/'game-api/src/main/java').rglob('*.java'))
sources += [ROOT/f'app/src/main/java/game/sanguo/mobile/{n}.java' for n in names]
sources += [ROOT/'tools/content/PcTerrainScratchProbe.java']
argfile = work/'sources.txt';argfile.write_text('\n'.join(map(str,sources))+'\n')
subprocess.run(['javac','-encoding','UTF-8','--release','17','-d',str(common),'@'+str(argfile)],check=True)
refdir = work/'baseline';refdir.mkdir(exist_ok=True)
subprocess.run(['javac','-encoding','UTF-8','--release','17','-cp',str(common),'-d',str(refdir),str(baseline)],check=True)
outputs = {}
for name, first in [('baseline',refdir),('current',common)]:
    cp = ':'.join(map(str,[first,common,ROOT/'core/src/main/resources']))
    data = subprocess.check_output(['java','-Xmx700m','-cp',cp,'game.sanguo.mobile.PcTerrainScratchProbe'],text=True)
    (work/f'{name}.txt').write_text(data)
    outputs[name] = [line for line in data.splitlines() if ' sha256=' in line]
before,after = outputs['baseline'],outputs['current']
if len(before)!=16 or len(after)!=16:raise AssertionError('Expected all16 real source/legacy camera windows')
for old,new in zip(before,after):
    if old.split(' allocated=')[0]!=new.split(' allocated=')[0]:raise AssertionError(('Terrain buffer changed',old,new))
oldbytes = sum(int(s.rsplit('=',1)[1]) for s in before)
newbytes = sum(int(s.rsplit('=',1)[1]) for s in after)
if newbytes>=oldbytes:raise AssertionError('Worker allocation did not decrease')
report = dict(status='PASS',scope='Desktop CPU output/allocation only; not Android performance or PC image validation',
    baseline=str(baseline),baseline_sha256=hashlib.sha256(baseline.read_bytes()).hexdigest(),
    current_sha256=hashlib.sha256((ROOT/'app/src/main/java/game/sanguo/mobile/SceneMesh.java').read_bytes()).hexdigest(),
    windows=16,buffer_comparison='All vertices/indices/materials/tangents/grid byte-exact; shifted windows/cache reuse/publication independence',
    baseline_worker_allocated_bytes=oldbytes,current_worker_allocated_bytes=newbytes,
    saved_worker_allocated_bytes=oldbytes-newbytes,results=outputs)
a.report.parent.mkdir(parents=True,exist_ok=True);a.report.write_text(json.dumps(report,indent=2)+'\n')
print('PASS all16 real PC/legacy terrain windows buffer-exact; worker allocation %d -> %d (%d bytes saved); HOST ONLY'%(oldbytes,newbytes,oldbytes-newbytes))
