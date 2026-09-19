#!/usr/bin/env python3
"""Build a private, offline, 42-city review atlas from the user's reference image.

The public repository stores only the coordinate index. The source image and
crops are never bundled with the Android application or committed by this tool.
Pillow is needed only for --image, not for reproducible --index-only generation.
"""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path
from refine_geography_v044 import read, distance

ROOT=Path(__file__).resolve().parents[2]


def index():
    old=json.loads((ROOT/'data/map/reference-v050.json').read_text())
    new=json.loads((ROOT/'data/map/reference-v051.json').read_text())
    p,t,sites=read(ROOT/'core/src/main/resources/scenarios/heroes-mobile-sandbox.properties')
    names={int(v.split('|')[0]):v.split('|')[1] for k,v in p.items() if k.startswith('city.')}
    reviewed={cid:r['name'] for r in new['regions'] for cid in r['reviewed_cities']}
    width,height=old['reference']['size'];a=old['reference']['calibration']['affine']
    cities=[]
    for anchor in old['reference']['calibration']['city_anchors']:
        cid=anchor['id'];x,y=sites[cid];px,py=anchor['px'];radius=620
        bounds=[max(0,int(px-radius)),max(0,int(py-radius)),min(width,int(px+radius)),min(height,int(py+radius))]
        nearby=[{'id':i,'name':names[i],'grid':list(h),'kind':'gate' if i<20052 else 'port'} for i,h in sites.items() if i>=20042 and distance((x,y),h)<=10]
        cities.append({'id':cid,'name':names[cid],'grid':[x,y],'source_center':[px,py],'pixel_bounds':bounds,'crop':f'cities/{cid}.jpg','review':'v051-regional' if cid in reviewed else 'v050-anchor-retained','region':reviewed.get(cid,'沿用v50据点配准，未在本轮逐格重审'),'nearby_sites':nearby})
    return {'schema':1,'release':'0.51.0','reference_sha256':new['reference_sha256'],'source_size':[width,height],'coordinate_frame':'100x100 odd-r; source coordinates shown before axial conversion','attribution':old['reference']['attribution'],'reviewed_city_count':len(reviewed),'cities':cities}


HTML='''<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>三国志11 · 逐城地图对照</title><style>
*{box-sizing:border-box}body{margin:0;background:#10181d;color:#e5e8e5;font:15px/1.65 system-ui,sans-serif}header{padding:20px 24px;border-bottom:1px solid #38454a}h1{font-size:23px;margin:0 0 4px}header p{margin:0;color:#aab7b8}main{display:grid;grid-template-columns:280px 1fr;max-width:1600px;margin:auto}aside{padding:16px;max-height:86vh;overflow:auto}input{width:100%;padding:12px;border:1px solid #526066;border-radius:8px;background:#1a262c;color:inherit}button{display:block;width:100%;text-align:left;padding:10px 12px;margin-top:6px;border:1px solid #38454a;border-radius:8px;background:#1a262c;color:inherit;cursor:pointer}button.active{border-color:#c2ad7a;background:#30352c}button small{display:block;color:#aab7b8}section{padding:20px;min-width:0}h2{margin:0 0 8px;font-size:23px}.meta{color:#b7c4c5;margin-bottom:14px}img{display:block;width:100%;height:auto;border:1px solid #39474b;border-radius:8px}.note{padding:12px;background:#263036;border-radius:8px;margin-top:14px;font-size:13px}a{color:#dbcb9e}@media(max-width:720px){main{grid-template-columns:1fr}aside{max-height:235px}section{padding:14px}header{padding:16px}h1{font-size:20px}}
</style><header><h1>三国志11 · 逐城地图对照</h1><p>v0.51 · 42城参考裁图 / 13城相关区域本轮精修 · 原图坐标可追溯</p></header><main><aside><input id="filter" placeholder="搜索城名 / 城市ID"><nav id="list"></nav></aside><section><h2 id="title"></h2><div id="meta" class="meta"></div><a id="full" target="_blank"><img id="map" alt="用户提供原图的局部裁图"></a><div id="near" class="note"></div><div class="note">图片是用户提供的参考图裁片，不是手机游戏截图。13城相关区域有本轮人工地形精修；其余城保留v0.50据点配准。100×100缩尺与原版逐格数据不同；白灰空带、湿地细类及全图支路仍须后续核验。新地形需要新开局，旧存档不搬城、不挪兵。来源：X GOD / 艾克軋德，2018-05-21；原图及裁片仅用于本次私人对照，不随公开仓库或APK发布。</div></section></main><script>
const DATA=__DATA__;let selected=DATA.cities[0].id;const el=id=>document.getElementById(id);function draw(){const q=el('filter').value.trim();el('list').replaceChildren();for(const c of DATA.cities.filter(c=>!q||c.name.includes(q)||String(c.id).includes(q))){const b=document.createElement('button');b.className=c.id===selected?'active':'';b.textContent=`${c.name} · ${c.grid.join(', ')}`;const s=document.createElement('small');s.textContent=c.review==='v051-regional'?'本轮相关区域精修':'沿用v0.50据点配准';b.append(s);b.onclick=()=>show(c.id);el('list').append(b)}}function show(id){selected=id;const c=DATA.cities.find(c=>c.id===id);el('title').textContent=c.name;el('meta').textContent=`${c.region} ｜ 地图 ${c.grid.join(', ')} ｜ 原图裁框 ${c.pixel_bounds.join(', ')}`;el('map').src=c.crop;el('full').href=c.crop;el('near').textContent='邻近港关（10格内）：'+(c.nearby_sites.map(s=>`${s.name} (${s.grid.join(', ')})`).join('；')||'无');draw()}el('filter').oninput=draw;show(selected);
</script></html>'''


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--index-only',action='store_true');p.add_argument('--image',type=Path);p.add_argument('--out',type=Path);args=p.parse_args()
    data=index();target=ROOT/'data/map/city-review-v051.json'
    if args.index_only:
        target.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n');print('42-city coordinate index written; no source images included');return
    if args.image is None or args.out is None:p.error('use --index-only, or --image <reference.jpg> --out <private-directory>')
    if hashlib.sha256(args.image.read_bytes()).hexdigest()!=data['reference_sha256']:p.error('reference image SHA-256 differs from the reviewed source')
    # Avoid accidentally writing source-derived imagery into tracked game assets.
    output=args.out.resolve()
    if output==ROOT or ROOT in output.parents:p.error('the private atlas output must be outside the repository')
    from PIL import Image
    im=Image.open(args.image)
    if list(im.size)!=data['source_size']:p.error('unexpected reference dimensions')
    (output/'cities').mkdir(parents=True,exist_ok=True)
    for c in data['cities']:im.crop(tuple(c['pixel_bounds'])).save(output/c['crop'],quality=94,subsampling=0)
    (output/'index.json').write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
    (output/'index.html').write_text(HTML.replace('__DATA__',json.dumps(data,ensure_ascii=False).replace('</','<\\/')))
    (output/'README.txt').write_text('打开 index.html。42份原分辨率局部裁片；点击图片可单独查看。不能将目录搬入公开仓库或APK资产目录。\n参考来源：X GOD / 艾克軋德，2018-05-21。\n')
    print(f'Private atlas ready: {len(data["cities"])} city crops, {data["reviewed_city_count"]} region-reviewed cities')

if __name__=='__main__':main()
