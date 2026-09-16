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
    xmin = min(int(c['rawX']) for c in sites)
    ymin = min(int(c['rawY']) for c in sites)
    positions = {c['id']: ((int(c['rawX'])-xmin)//3+3, (int(c['rawY'])-ymin)//3+3) for c in sites}
    assert len(set(positions.values())) == len(sites)
    width = max(p[0] for p in positions.values())+4
    height = max(p[1] for p in positions.values())+4
    terrain = [['P' for _ in range(width)] for _ in range(height)]
    for y in range(height):
        for x in range(width):
            noise = (x*17+y*31+x*y*7) % 101
            terrain[y][x] = 'F' if noise < 23 else 'M' if x < width//4 and noise > 65 else 'P'
            if y == height*3//5 and x > width//5:
                terrain[y][x] = 'S' if x % 7 == 0 else 'W'
            if noise == 97:
                terrain[y][x] = 'X'  # poison spring; isolated from required roads
            if x < width//4 and noise == 93:
                terrain[y][x] = 'D'
            if x < width//4 and noise == 94:
                terrain[y][x] = 'B'
    # A connected trunk road with city clearings. Valleys and fords remain usable
    # without requiring technology just to leave an initial city.
    previous = None
    for x, y in sorted(positions.values()):
        if previous is not None:
            px, py = previous
            for xx in range(min(px,x), max(px,x)+1):
                terrain[py][xx] = 'P'
            for yy in range(min(py,y), max(py,y)+1):
                terrain[yy][x] = 'P'
        previous = (x,y)
    for x,y in positions.values():
        for yy in range(y-2,y+3):
            for xx in range(x-2,x+3):
                terrain[yy][xx] = 'P'
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
             'natural-deaths=0','revision=1','year=200','month=1','coordinates=odd-r',
             f'width={width}',f'height={height}','factions=3',
             'faction.0=刘备军','faction.1=曹操军','faction.2=孙权军']
    lines += [f'terrain.{y}={"".join(row)}' for y,row in enumerate(terrain)]
    lines += [f'cities={len(sites)}']
    for i,c in enumerate(sites):
        x,y=positions[c['id']]
        lines += [f'city.{i}={c["id"]}|{c["name"]}|{x}|{y}|{owner[c["id"]]}|15000|90000|24000|95|90|{c["durability"]}|20000|20000|20000|20000']
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
