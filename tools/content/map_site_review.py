#!/usr/bin/env python3
"""Private 87-site comparison atlas; never distribute the supplied source image in the APK.

The raw reference uses staggered COLUMNS (odd-q), not the mobile map's odd-r.
Fit the reference with the explicit half-cell column offset. Keep the game's
current rules/data unchanged. Inferred port/gate crop centers are labeled as
inferred, never promoted to measured anchors or collision footprints.
"""
from __future__ import annotations
import argparse
import hashlib
import html
import json
import math
from pathlib import Path
from refine_geography_v044 import read

ROOT = Path(__file__).resolve().parents[2]
REFERENCE = ROOT / 'data/map/reference-v050.json'
SCENARIO = ROOT / 'core/src/main/resources/scenarios/heroes-mobile-sandbox.properties'


def solve(matrix, rhs):
    """Small pivoted Gaussian elimination; no numerical package needed for index generation."""
    n = len(rhs)
    a = [list(row) + [rhs[i]] for i, row in enumerate(matrix)]
    for i in range(n):
        pivot = max(range(i, n), key=lambda j: abs(a[j][i]))
        if abs(a[pivot][i]) < 1e-10:
            raise ValueError('Singular reference calibration')
        a[i], a[pivot] = a[pivot], a[i]
        d = a[i][i]
        a[i] = [v / d for v in a[i]]
        for j in range(n):
            if j != i:
                f = a[j][i]
                a[j] = [u - f * v for u, v in zip(a[j], a[i])]
    return [a[i][-1] for i in range(n)]


def native_cell(x, y):
    return [x, y + (int(x) & 1) * .5, 1.0]


def fit(anchors, staggered):
    rows = [native_cell(*a['raw']) if staggered else [*a['raw'], 1.] for a in anchors]
    gram = [[sum(r[i] * r[j] for r in rows) for j in range(3)] for i in range(3)]
    axes = [solve(gram, [sum(r[i] * a['px'][axis] for r, a in zip(rows, anchors)) for i in range(3)]) for axis in range(2)]
    errors = [math.dist([sum(v * m for v, m in zip(row, axis)) for axis in axes], a['px']) for row, a in zip(rows, anchors)]
    ordered = sorted(errors)
    return {'axes': axes, 'rms_pixels': math.sqrt(sum(e * e for e in errors) / len(errors)),
            'median_pixels': (ordered[20] + ordered[21]) / 2, 'max_pixels': max(errors),
            'anchor_errors': {str(a['id']): round(e, 5) for a, e in zip(anchors, errors)}}


def build_index():
    reference = json.loads(REFERENCE.read_text())
    anchors = reference['reference']['calibration']['city_anchors']
    rawfit, staggerfit = fit(anchors, False), fit(anchors, True)
    anchor_by_id = {a['id']: a for a in anchors}
    props, terrain, positions = read(SCENARIO)
    names = {int(v.split('|')[0]): v.split('|')[1] for k, v in props.items() if k.startswith('city.')}
    kinds = {int(v.split('|')[0]): v.split('|')[1] for k, v in props.items() if k.startswith('site-kind.')}
    sites = []
    for sid, (x, y) in positions.items():
        kind = kinds.get(sid, 'CITY')
        anchor = anchor_by_id.get(sid)
        if anchor:
            px, py = anchor['px']
            source_kind = 'measured_city_icon_center'
            native = anchor['raw']
        else:
            native = [x * 2, y * 2]
            row = native_cell(*native)
            px, py = [sum(v * m for v, m in zip(row, axis)) for axis in staggerfit['axes']]
            source_kind = 'inferred_from_mobile_center_not_verified'
        radius = 620 if kind == 'CITY' else 330
        width, height = reference['reference']['size']
        bounds = [max(0, int(px-radius)), max(0, int(py-radius)), min(width, int(px+radius)), min(height, int(py+radius))]
        near = sorted((math.hypot(x-v[0], y-v[1]), i) for i, v in positions.items() if i != sid)[:7]
        sites.append({'id': sid, 'name': names[sid], 'kind': kind, 'mobile_grid': [x, y],
                      'reference_center': [round(px, 3), round(py, 3)], 'center_evidence': source_kind,
                      'native_grid': native, 'native_grid_evidence': 'recorded_anchor' if anchor else 'estimate_only',
                      'crop_bounds': bounds, 'reference_crop': f'reference/{sid}.jpg', 'current_map': f'current/{sid}.svg',
                      'logical_footprint_in_current_code': 1, 'reference_collision_footprint': None,
                      'nearby': [{'id': i, 'name': names[i], 'grid': list(positions[i])} for _, i in near]})
    return {'schema': 1, 'baseline': '63a3d5e3f946145fb8b5e4a52b29d5d3cccaf140',
            'reference_sha256': reference['reference']['sha256'], 'size': reference['reference']['size'],
            'attribution': reference['reference']['attribution'],
            'reference_topology': 'column-staggered square appearance, six side-neighbors',
            'mobile_topology': 'row-staggered square presentation; existing odd-r six-neighbor rules unchanged',
            'reference_calibration_without_stagger': rawfit, 'reference_calibration_with_column_stagger': staggerfit,
            'scenario_sha256': hashlib.sha256(SCENARIO.read_bytes()).hexdigest(), 'sites': sites}, props, terrain


PALETTE={'P':'#8a9569','F':'#395c43','M':'#717774','W':'#427f99','O':'#244b6c','V':'#152331','D':'#97836a','S':'#6da6a7','B':'#5f6a61','X':'#65566f','Z':'#566158','H':'#a09079','A':'#c6b184'}


def data_svg(site, data, props, terrain):
    # This is a deterministic data inspector, not a fabricated Android screenshot.
    x, y = site['mobile_grid']; half = 10 if site['kind'] == 'CITY' else 6
    x0=max(0,x-half);x1=min(len(terrain[0])-1,x+half);y0=max(0,y-half);y1=min(len(terrain)-1,y+half)
    step=42;w=(x1-x0+2)*step;h=(y1-y0+2)*step
    out=[f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" role="img" aria-label="正式剧本地形数据示意，非APK截图"><rect width="100%" height="100%" fill="#14222a"/>']
    def point(a,b):return ((a-x0+.75+(b&1)*.5)*step,(b-y0+.75)*step)
    for r in range(y0,y1+1):
        for q in range(x0,x1+1):
            cx,cy=point(q,r);t=terrain[r][q]
            out.append(f'<rect x="{cx-step/2}" y="{cy-step/2}" width="{step}" height="{step}" fill="{PALETTE.get(t,"#b377ca")}" stroke="#173036" stroke-width=".6"><title>({q},{r}) {t}</title></rect>')
            out.append(f'<text x="{cx}" y="{cy+4}" fill="#dce5d8" opacity=".5" font-size="10" text-anchor="middle">{t}</text>')
    for k,v in props.items():
        if not k.startswith('development-plot.'):continue
        f=v.split('|');q,r=int(f[1]),int(f[2])
        if x0<=q<=x1 and y0<=r<=y1:
            cx,cy=point(q,r);out.append(f'<text x="{cx}" y="{cy+7}" fill="#ffe09b" font-size="22" text-anchor="middle">＋</text>')
    for other in data['sites']:
        q,r=other['mobile_grid']
        if not(x0<=q<=x1 and y0<=r<=y1):continue
        cx,cy=point(q,r);active=other['id']==site['id'];stroke='#ffdb88' if active else '#fff'
        out.append(f'<rect x="{cx-18}" y="{cy-18}" width="36" height="36" fill="#22353d" stroke="{stroke}" stroke-width="{3 if active else 1}"/>')
        out.append(f'<text x="{cx}" y="{cy+6}" fill="{stroke}" font-size="14" font-family="sans-serif" text-anchor="middle">{html.escape(other["name"])}</text>')
    out.append('</svg>');return ''.join(out)


PAGE='''<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>三国地图 · 87据点对照台</title><style>
*{box-sizing:border-box}body{margin:0;background:#101a20;color:#e2e9e6;font:15px/1.7 system-ui,sans-serif}header{padding:22px 28px;border-bottom:1px solid #34464c}h1{font-size:25px;margin:0}p{margin:5px 0;color:#b6c4c4}main{display:grid;grid-template-columns:265px 1fr}aside{padding:18px;position:sticky;top:0;max-height:100vh;overflow:auto}input,select{width:100%;margin-bottom:8px;background:#1e2b32;color:inherit;padding:11px;border:1px solid #48575a;border-radius:8px}button{display:block;text-align:left;width:100%;padding:9px 12px;margin:5px 0;background:#1b2930;color:inherit;border:1px solid #34464c;border-radius:7px;cursor:pointer}button.active{border-color:#ccb787;background:#323a33}button small{display:block;color:#afbebd}section{padding:22px;min-width:0}h2{margin:0}article{display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-top:16px}figure{margin:0;min-width:0}img{width:100%;height:auto;display:block;border:1px solid #415153;border-radius:8px}figcaption{padding:8px;color:#b6c4c4}.box{background:#23323a;padding:12px 16px;margin-top:12px;border-radius:8px}.warn{border-left:3px solid #dbb970}a{color:#e3c995}.foot{font-size:13px}@media(max-width:900px){main{grid-template-columns:1fr}aside{position:static;max-height:260px}article{grid-template-columns:1fr}section{padding:15px}}</style>
<header><h1>三国地图 · 87据点对照台</h1><p>42城 / 10关 / 35港 · 原分辨率局部图与正式剧本数据并排查看</p><p class="foot">原图：X GOD / 艾克軋德，2018-05-21。本目录仅用于私人核对，不能随公开仓库或APK发布。</p></header>
<main><aside><input id="search" placeholder="搜索地名或编号"><select id="kind"><option value="">全部据点</option><option value="CITY">城市</option><option value="GATE">关卡</option><option value="PORT">港口</option></select><nav id="list"></nav></aside><section><h2 id="name"></h2><p id="coords"></p><div id="evidence" class="box warn"></div><article><figure><a id="link" target="_blank"><img id="ref" alt="用户原图局部裁片"></a><figcaption>原图裁片，保持源分辨率；点击可单独放大。白灰区及图标遮挡不自动识别为地形。</figcaption></figure><figure><img id="current" alt="当前正式剧本数据示意"><figcaption>当前100×100剧本数据示意，并非Android截图。＋为开发位；字母为真实地形编码。不得当作原版逐格复刻。</figcaption></figure></article><div class="box" id="near"></div><div class="box warn">当前源码的据点逻辑占位仍是中心1格。图标跨格不等于已证实的碰撞占地。正式多格占地需同步点选、阻挡、城墙边缘射程、出征与进驻、港口转换、AI及存档；本图册不虚构已完成状态。</div><div class="box foot">地形字母：P平原 / F森林 / M山地 / W水面 / O海面 / S浅滩 / D山路（难所） / B栈道 / X毒泉 / Z湿地 / H水坝 / A沙地 / V地图外。原图使用错列方格（六向邻接），当前地图仍使用既有odd-r行错列规则；列错列原生数据迁移尚未执行。</div></section></main>
<script>const DATA=__DATA__;const $=id=>document.getElementById(id);let selected=20017;const names={CITY:'城',GATE:'关',PORT:'港'};function list(){const q=$('search').value.trim();$('list').replaceChildren();for(const s of DATA.sites.filter(s=>(!q||s.name.includes(q)||String(s.id).includes(q))&&(!$('kind').value||s.kind===$('kind').value))){let b=document.createElement('button');b.className=selected===s.id?'active':'';b.textContent=names[s.kind]+' · '+s.name;let small=document.createElement('small');small.textContent=s.center_evidence.startsWith('measured')?'有城市图标像素锚点':'裁图中心为估计，须目视定位';b.append(small);b.onclick=()=>show(s.id);$('list').append(b)}}function show(id){selected=id;const s=DATA.sites.find(s=>s.id===id);$('name').textContent=s.name+' · '+s.id;$('coords').textContent='当前格坐标 '+s.mobile_grid.join(', ')+' ｜ 原图裁框 '+s.crop_bounds.join(', ');$('evidence').textContent=s.center_evidence.startsWith('measured')?'裁片使用既有城市图标像素锚点；不把图标中心误差或图片大小当作碰撞占位。':'本港/关裁图中心由现行坐标反推，不是新测量点。先在裁片中确认名称、图标与河岸，再登记原生格位置。';$('ref').src=s.reference_crop;$('link').href=s.reference_crop;$('current').src=s.current_map;$('near').textContent='邻近据点：'+s.nearby.map(n=>n.name+' ('+n.grid.join(', ')+')').join('；');list()}$('search').oninput=list;$('kind').onchange=list;show(selected);</script></html>'''


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--index', type=Path)
    parser.add_argument('--image', type=Path)
    parser.add_argument('--out', type=Path)
    args=parser.parse_args()
    data, props, terrain = build_index()
    if args.index:
        args.index.parent.mkdir(parents=True, exist_ok=True)
        args.index.write_text(json.dumps(data, ensure_ascii=False, indent=2)+'\n')
    if args.image or args.out:
        if not(args.image and args.out):parser.error('--image and --out must be supplied together')
        out=args.out.resolve()
        if out==ROOT or ROOT in out.parents:parser.error('Reference image/crops may not be written inside the repository')
        if hashlib.sha256(args.image.read_bytes()).hexdigest()!=data['reference_sha256']:parser.error('Reference image fingerprint mismatch')
        from PIL import Image
        with Image.open(args.image) as im:
            if list(im.size)!=data['size']:parser.error('Unexpected image dimensions')
            for directory in ['reference','current']:(out/directory).mkdir(parents=True,exist_ok=True)
            for site in data['sites']:
                im.crop(site['crop_bounds']).save(out/site['reference_crop'],quality=89,subsampling=0)
                (out/site['current_map']).write_text(data_svg(site,data,props,terrain))
        (out/'index.html').write_text(PAGE.replace('__DATA__',json.dumps(data,ensure_ascii=False).replace('</','<\\/')))
        (out/'index.json').write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
        (out/'README.txt').write_text('解压后用浏览器打开 index.html。原图及裁片仅供本次私人核对，不能公开再分发。右图是正式剧本数据示意，不是Android截图。42城市参考中心使用既有像素锚点，45港关裁框位置为估计待复核。未宣称87据点或多格占地已全部还原。\n')
    if not(args.index or args.out):parser.error('Specify --index and/or --image plus --out')
    print(f"87 sites indexed; RMS {data['reference_calibration_without_stagger']['rms_pixels']:.3f} -> {data['reference_calibration_with_column_stagger']['rms_pixels']:.3f} px with explicit column stagger")

if __name__=='__main__':main()
