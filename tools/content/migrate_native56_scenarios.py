#!/usr/bin/env python3
"""Replace embedded terrain with a shared native map reference without changing story fields."""
from __future__ import annotations
import hashlib,json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]

def props(path):
    return dict(line.split('=',1) for line in path.read_text(encoding='utf-8').splitlines() if '=' in line and not line.lstrip().startswith('#'))

def map_key(key):
    return key in {'map','map-crop','coordinates','width','height','development-plots'} or key.startswith(('terrain.','development-plot.'))

def run():
    national=props(ROOT/'core/src/main/resources/maps/national-map-v056.properties')
    directory=ROOT/'core/src/main/resources/scenarios';index=directory/'index.txt';ids=[line.split()[0] for line in index.read_text().splitlines() if line.strip() and not line.startswith('#')]
    report=[]
    for sid in ids:
        path=directory/(sid+'.properties');before=props(path);old_text=path.read_text()
        if any(k.startswith(('initial-unit.','initial-camp.')) for k in before):
            raise ValueError('Explicit initial entity coordinates need independently calibrated migration: '+sid)
        cities=[(k,v.split('|')) for k,v in before.items() if re.fullmatch(r'city\.\d+',k)]
        if len(cities)==87: crop=None
        else:
            points=[]
            for _,city in cities:
                points.append(tuple(map(int,national['site.'+city[0]].split(','))))
                points.extend(tuple(map(int,xy.split(','))) for xy in national.get('plots.'+city[0],'').split(';') if xy)
            x=max(0,min(q[0] for q in points)-4);x-=x%2;y=max(0,min(q[1] for q in points)-4)
            right=min(200,max(q[0] for q in points)+5);bottom=min(200,max(q[1] for q in points)+5)
            crop=(x,y,right-x,bottom-y)
        lines=[line for line in old_text.splitlines() if '=' not in line or not map_key(line.split('=',1)[0])]
        lines+=['map=national-map-v056']
        if crop:lines+=['map-crop='+','.join(map(str,crop))]
        path.write_text('\n'.join(lines)+'\n',encoding='utf-8');after=props(path)
        preserved={k:v for k,v in before.items() if not map_key(k)}
        assert preserved=={k:v for k,v in after.items() if not map_key(k)},sid
        report.append({'scenario':sid,'crop':crop,'preserved_non_map_fields':len(preserved),'sha256':hashlib.sha256(path.read_bytes()).hexdigest()})
    index.write_text('# scenario-id SHA-256 of exact UTF-8 bytes\n'+''.join(f'{d["scenario"]} {d["sha256"]}\n' for d in report))
    target=ROOT/'data/map/reference-v056';target.mkdir(parents=True,exist_ok=True)
    (target/'scenario-migration.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print('Shared-map references migrated:',len(report),'national:',sum(d['crop'] is None for d in report),'crops:',[(d['scenario'],d['crop']) for d in report if d['crop']])

if __name__=='__main__':run()
