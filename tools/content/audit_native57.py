#!/usr/bin/env python3
"""Review the authoritative odd-q source grid, not the wider axial storage.

Only --apply writes the eleven explicit ESTIMATED corrections. --check verifies
post-images, map/manifest digests, and six-neighbor evidence. No inferred mass fill.
"""
from pathlib import Path
from collections import Counter
import argparse, hashlib, json, re
ROOT = Path(__file__).resolve().parents[2]
MAP = ROOT / 'core/src/main/resources/maps/national-map-v056.properties'
PLAN = ROOT / 'data/map/reference-v057/terrain-corrections.json'
MANIFEST = ROOT / 'tools/content/map-release-manifest.json'

def digest(data): return hashlib.sha256(data).hexdigest()
def properties(text):
    return dict(line.split('=', 1) for line in text.splitlines() if '=' in line and not line.startswith('#'))
def neighbors(x, y):
    for xx, yy in ((x,y-1),(x,y+1),(x-1,y-(1-x%2)),(x-1,y+x%2),
                   (x+1,y-(1-x%2)),(x+1,y+x%2)):
        if 0 <= xx < 200 and 0 <= yy < 200: yield xx, yy

def components(grid, code):
    seen, result = set(), []
    for y in range(200):
        for x in range(200):
            if grid[y][x] != code or (x,y) in seen: continue
            todo, cells = [(x,y)], []
            seen.add((x,y))
            while todo:
                point=todo.pop(); cells.append(point)
                for xx,yy in neighbors(*point):
                    if grid[yy][xx]==code and (xx,yy) not in seen:
                        seen.add((xx,yy)); todo.append((xx,yy))
            ring = Counter(grid[yy][xx] for point in cells for xx,yy in neighbors(*point)
                           if grid[yy][xx] != code)
            result.append(dict(size=len(cells), touches_source_boundary=any(x in (0,199) or y in (0,199) for x,y in cells),
                               bounds=[min(x for x,y in cells),min(y for x,y in cells),max(x for x,y in cells),max(y for x,y in cells)],
                               sample=sorted(cells)[:3],neighbor_codes=dict(sorted(ring.items()))))
    return sorted(result,key=lambda c:(-c['size'],c['bounds']))

def main():
    parser=argparse.ArgumentParser(); parser.add_argument('--apply',action='store_true');parser.add_argument('--check',action='store_true');parser.add_argument('--report',type=Path);parser.add_argument('--map-file',type=Path)
    args=parser.parse_args();plan=json.loads(PLAN.read_text());raw=(args.map_file or MAP).read_bytes();text=raw.decode();p=properties(text)
    if args.map_file and args.apply:parser.error('--map-file is a read-only historical checkpoint')
    codes=dict(re.findall(r"case '([A-Z])' -> World\.Terrain\.([A-Z_]+)", (ROOT/'core/src/main/java/game/sanguo/core/TerrainCode.java').read_text()))
    # Current decoder may append Q; the checkpoint must still have all original 14 codes.
    if 'Q' in codes:assert codes.pop('Q')=='NON_NAVIGABLE_WATER'
    assert len(codes)==14 and codes['D']=='MOUNTAIN_PATH' and codes['H']=='DAM'
    assert p['columns']==p['rows']=='200'
    grid=[p[f'terrain.{y}'] for y in range(200)]
    assert all(len(row)==200 and set(row)<=codes.keys() for row in grid)
    if args.apply and digest(raw)==plan['before_sha256']:
        replacements={}
        for cell in plan['cells']:
            x,y=cell['source'];assert grid[y][x]==cell['before']
            assert cell['evidence']=='ESTIMATED'
            assert len(list(neighbors(x,y)))==6 and all(grid[yy][xx]==cell['after'] for xx,yy in neighbors(x,y))
            row=list(replacements.get(y,grid[y]));row[x]=cell['after'];replacements[y]=''.join(row)
        for y,row in replacements.items():text=text.replace('terrain.'+str(y)+'='+grid[y], 'terrain.'+str(y)+'='+row)
        text=text.replace('revision=56\n','revision=57\n')
        assert digest(text.encode())==plan['after_sha256']
        MAP.write_text(text);raw=MAP.read_bytes();grid=[properties(text)[f'terrain.{y}'] for y in range(200)]
        manifest=json.loads(MANIFEST.read_text());manifest['release']='0.57.0'
        entry=next(e for e in manifest['files'] if e['source_path']==str(MAP.relative_to(ROOT)))
        assert entry['sha256']==plan['before_sha256'];entry['sha256']=plan['after_sha256']
        MANIFEST.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
    assert digest(raw)==plan['after_sha256'], 'Unreviewed map change or missing explicit patch'
    assert properties(raw.decode())['revision']=='57'
    for cell in plan['cells']:
        x,y=cell['source'];assert grid[y][x]==cell['after']
        assert all(grid[yy][xx]==cell['after'] for xx,yy in neighbors(x,y))
    counts=Counter(''.join(grid));voids=components(grid,'V')
    report=dict(map_revision=57,map_sha256=digest(raw),source_grid=[200,200],axial_storage=[299,200],padding_cells=19800,
        code_mapping=codes,source_counts=dict(sorted(counts.items())),terrain_counts={codes[k]:v for k,v in sorted(counts.items())},
        natural_dam_source_cells=counts['H'],road_samples=[dict(source=[x,y],code=grid[y][x]) for x,y in [(77,76),(81,76)]],
        void_components=len(voids),interior_void_components=sum(not c['touches_source_boundary'] for c in voids),
        interior_void_cells=sum(c['size'] for c in voids if not c['touches_source_boundary']),void_regions=voids,
        estimated_fixes=len(plan['cells']),reference_available=False,
        unresolved='Remaining VOID regions and noisy road/plank digitization require reference-image review; boundary connectivity is not proof of correctness.')
    if args.report:args.report.parent.mkdir(parents=True,exist_ok=True);args.report.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print('NATIVE57 DATA PASS:',dict(counts),'DAM(H)=',counts['H'],'interior VOID cells=',report['interior_void_cells'],'estimated repairs=',len(plan['cells']))
if __name__=='__main__':main()
