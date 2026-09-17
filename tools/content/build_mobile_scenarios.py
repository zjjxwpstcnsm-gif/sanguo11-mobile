#!/usr/bin/env python3
"""Reproducible playable sandboxes from the pinned catalog; NOT official openings.

Only names, stats, aptitudes and relative city positions come from the catalog.
Terrain, ownership, resources and rosters are explicit original game design.
All ages coexist; biographies, personalities and unambiguous relations are loaded
without historical appearance/death gates. No external downloads are required.
"""
import argparse
import csv
import hashlib
from national_geography import generate_geography
from strategic_sites import layout
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CONTENT = ROOT / 'core/src/main/resources/content'
SCENARIOS = ROOT / 'core/src/main/resources/scenarios'


def rows(name):
    with (CONTENT / name).open() as stream:
        return list(csv.DictReader(stream, delimiter='\t'))


officers = rows('officers.tsv')
aliases = {r['id']: r['alias'] for r in rows('aliases.tsv')}
cities = [r for r in rows('sites.tsv') if r['kind'] == 'city']
teams = [
    '劉備 關羽 張飛 趙雲 諸葛亮 黃忠 馬超 魏延 龐統 徐庶 法正 姜維 馬良 馬謖 關平 關興 張苞 蔣琬 費禕 孫乾 簡雍 糜竺',
    '曹操 夏侯惇 夏侯淵 張遼 荀彧 郭嘉 荀攸 曹仁 曹洪 許褚 典韋 徐晃 張郃 于禁 樂進 賈詡 司馬懿 曹丕 曹植 程昱 滿寵 李典',
    '孫權 周瑜 魯肅 甘寧 太史慈 呂蒙 陸遜 黃蓋 程普 韓當 周泰 蔣欽 朱然 凌統 徐盛 丁奉 孫策 孫堅 大喬 小喬 孫尚香 諸葛瑾',
]
named_owners = {name: side for side, names in enumerate(teams) for name in names.split()}
assert all(name in {o['name'] for o in officers} for name in named_owners), 'Roster name must exist in pinned catalog'


def generate(scenario_id, title, city_ids, people_count, check=False):
    sites = [c for c in cities if int(c['id']) in city_ids]
    full, all_parcels = generate_geography(cities)
    xmin=max(0,min(int(c['rawX']) for c in sites)-9)
    ymin=max(0,min(int(c['rawY']) for c in sites)-9)//2*2  # preserve odd-r row parity when cropping
    xmax=min(199,max(int(c['rawX']) for c in sites)+9)
    ymax=min(199,max(int(c['rawY']) for c in sites)+9)
    if len(sites)==42:xmin,ymin,xmax,ymax=0,0,199,199
    width,height=xmax-xmin+1,ymax-ymin+1
    positions={c['id']:(int(c['rawX'])-xmin,int(c['rawY'])-ymin) for c in sites}
    terrain=[row[xmin:xmax+1] for row in full[ymin:ymax+1]]
    parcels={c['id']:[(x-xmin,y-ymin) for x,y in all_parcels[c['id']]] for c in sites}
    # Three spheres of influence, deliberately a fictional setup.
    owner = {}
    for c in sites:
        x,y = int(c['rawX']),int(c['rawY'])
        owner[c['id']] = 0 if x<100 and y>100 else 2 if x>=100 and y>=100 else 1
    homes = {side:[c for c in sites if owner[c['id']]==side] for side in range(3)}
    assert all(homes.values())
    chosen = [o for o in officers if o['name'] in named_owners]
    # High ability unaffiliated officers make the smaller maps worthwhile too.
    pool = sorted((o for o in officers if o not in chosen), key=lambda o:(-max(map(int,o['stats'].split(','))),int(o['id'])))
    chosen += pool[:people_count-len(chosen)]
    chosen.sort(key=lambda o:int(o['id']))
    lines = ['# Self-made all-era sandbox. Never an official SAN11 opening.',
             'format=1',f'id={scenario_id}',f'name={title}','source=community-reference',
             'reference=rlu-officers','reference-details=1','reference-dates=0',
             'natural-deaths=0','revision=3','year=200','month=1','coordinates=odd-r',
             f'width={width}',f'height={height}','factions=3',
             'faction.0=刘备军','faction.1=曹操军','faction.2=孙权军']
    lines += [f'terrain.{y}={"".join(row)}' for y,row in enumerate(terrain)]
    extras=[(key,parent,x-xmin,y-ymin,kind) for key,parent,x,y,kind in layout(full,{c['id']:(int(c['rawX']),int(c['rawY'])) for c in cities}) if str(parent) in positions and xmin<=x<=xmax and ymin<=y<=ymax]
    source_sites={int(c['id']):c for c in rows('sites.tsv')}
    lines += [f'cities={len(sites)+len(extras)}']
    for i,c in enumerate(sites):
        x,y=positions[c['id']]
        lines += [f'city.{i}={c["id"]}|{c["name"]}|{x}|{y}|{owner[c["id"]]}|15000|90000|24000|95|90|{c["durability"]}|20000|20000|20000|20000']
    for i,(key,parent,x,y,kind) in enumerate(extras,len(sites)):
        d=source_sites[key];name=d['name']+('港' if kind=='PORT' else '' if d['name']=='劍閣' else '關')
        lines += [f'city.{i}={key}|{name}|{x}|{y}|{owner[str(parent)]}|1500|15000|3000|90|80|{d["durability"]}|3000|3000|3000|3000']
    lines += [f'site-kinds={len(extras)}']
    lines += [f'site-kind.{i}={key}|{kind}|{source_sites[key]["durability"]}' for i,(key,parent,x,y,kind) in enumerate(extras)]
    entries=[(c['id'],x,y) for c in sites for x,y in parcels[c['id']]]
    lines += [f'development-plots={len(entries)}']
    lines += [f'development-plot.{i}={city}|{x}|{y}' for i,(city,x,y) in enumerate(entries)]
    lines += [f'arsenals={len(sites)}']
    lines += [f'arsenal.{i}={c["id"]}|2|2|0|0|2|0' for i,c in enumerate(sites)]
    lines += [f'officers={len(chosen)}']
    counts=[0,0,0]
    for i,o in enumerate(chosen):
        side=named_owners.get(o['name'],-1)
        if side>=0:
            home=homes[side][counts[side]%len(homes[side])];counts[side]+=1
        else:
            home=sites[i%len(sites)]
        lines += [f'officer.{i}={o["id"]}|{aliases.get(o["id"],o["name"])}|{side}|{home["id"]}|{o["stats"].replace(",","|")}']
    lines += [f'aptitudes={len(chosen)}']
    for i,o in enumerate(chosen):
        lines += [f'aptitude.{i}={o["id"]}|'+ '|'.join(str('CBAS'.index(c)) for c in o['aptitudes'])]
    output='\n'.join(lines)+'\n'
    path=SCENARIOS / (scenario_id+'.properties')
    if check:assert path.read_text()==output, f'Stale generated scenario: {scenario_id}'
    else:path.write_text(output)
    return scenario_id+' '+hashlib.sha256(output.encode()).hexdigest()


def main():
    parser=argparse.ArgumentParser();parser.add_argument("--check",action="store_true");args=parser.parse_args()
    # Separate scenarios offer short regional sessions and a much larger campaign.
    entries=[generate('heroes-mobile-sandbox','群英汇聚 · 自制',range(20000,20042),670,args.check),
             generate('central-mobile-sandbox','中原竞逐 · 自制',list(range(20008,20019))+list(range(20022,20029)),180,args.check),
             generate('jingxiang-mobile-sandbox','荆襄群英 · 自制',list(range(20025,20037)),120,args.check)]
    index=SCENARIOS/'index.txt'
    ids={line.split()[0] for line in entries}
    old=[line for line in index.read_text().splitlines() if not line or line.split()[0] not in ids]
    output='\n'.join(old+entries)+'\n'
    if args.check:assert index.read_text()==output, 'Stale scenario index'
    else:index.write_text(output)


if __name__=='__main__':
    main()
