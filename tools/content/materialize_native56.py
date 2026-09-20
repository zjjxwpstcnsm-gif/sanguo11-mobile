#!/usr/bin/env python3
"""Recover complete source files, preserve the checkpoint API, and materialize real v056 assets.

The old six-part XZ transport is truncated. We deliberately accept ONLY its first
48 complete git-diff sections, whose entire bytes and every resulting blob prefix
were independently checked locally. No incomplete file or reference image is used.
This one-shot importer is not called by the game or normal builds.
"""
from __future__ import annotations
import argparse,base64,collections,hashlib,json,lzma,re,subprocess,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools/content'))
from migrate_native56_scenarios import props,run as migrate
from build_city_atlas_v056 import generate as art
from map_site_review import fit,native_cell
BASE='bb8e58ebe32e96ddf453ee68fd23e21509f71247'
RAW_SHA='cc28b9c4508726b49208999b606a83dda0354f0071f42ff54cc5b2055880e0c8'
PATCH_SHA='219720bb79f277c0e5d0413613ab6f983b1dbfe310a60acf3ba314165d90f32c'
REF_SHA='a5a4e8c7f9785b6fbadc8487508ff6c937dcef2d782b099f2e876e48d167d4e0'
CORE=Path('core/src/main/java/game/sanguo/core'); TEST=Path('core/src/test/java/game/sanguo/core')
def git(*args):return subprocess.check_output(['git',*args],cwd=ROOT)
def write(path,text):
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding='utf-8')
def output(name,data):write(Path('data/map/reference-v056')/name,json.dumps(data,ensure_ascii=False,indent=2)+'\n')
def recover(base):
    parts=sorted((ROOT/'.ci').glob('native56-??.b64'))
    if not parts:raise SystemExit('Missing verified original six transport files; no changes applied')
    assert len(parts)==6
    raw=base64.b64decode(''.join(p.read_text().strip() for p in parts),validate=True)
    assert hashlib.sha256(raw).hexdigest()==RAW_SHA,'Original transport was changed'
    decoder=lzma.LZMADecompressor();decoded=decoder.decompress(raw,max_length=2_000_000)
    assert not decoder.eof,'Unexpectedly complete transport: re-audit instead of silently accepting different input'
    sections=re.split(rb'(?=^diff --git )',decoded,flags=re.M);sections=[s for s in sections if s.startswith(b'diff --git ')]
    patch=b''.join(sections[:48]);assert hashlib.sha256(patch).hexdigest()==PATCH_SHA
    legacy=(ROOT/TEST/'MapCoordinateTest.java').read_text().replace('MapCoordinateTest','LegacyMapCoordinateCheckpointTest')
    write(TEST/'LegacyMapCoordinateCheckpointTest.java',legacy)
    # These are the only three paths changed by both the recovered increment and checkpoint.
    (ROOT/CORE/'MapCoordinates.java').write_bytes(git('show',base+':'+str(CORE/'MapCoordinates.java')))
    for path in [CORE/'SourceGridCoord.java',TEST/'MapCoordinateTest.java']:(ROOT/path).unlink()
    process=subprocess.run(['git','apply','--unidiff-zero','-'],input=patch,cwd=ROOT,check=True)
    verified=[]
    for section in sections[:48]:
        name=re.search(rb'^diff --git a/(.+) b/(.+)$',section,re.M).group(2).decode()
        wanted=re.search(rb'^index [0-9a-f]+\.\.([0-9a-f]+)',section,re.M).group(1).decode()
        actual=git('hash-object',name).decode().strip();assert actual.startswith(wanted),(name,actual,wanted)
        verified.append({'path':name,'git_blob':actual})
    output('source-recovery.json',{'base':base,'transport_sha256':RAW_SHA,'complete_patch_sha256':PATCH_SHA,'complete_files':verified,'discarded':'Truncated 49th calibration.json section; never applied','original_stream_complete':False})
    # Keep the checkpoint public API and exhaustive legacy topology tests, not a regression.
    path=ROOT/CORE/'MapCoordinates.java';text=path.read_text();needle='    private MapCoordinates(){}'
    addition='''
    /** Legacy odd-r source API; native map conversion must use the World overload. */
    public static Hex toAxial(SourceGridCoord s,int rows){
        Objects.requireNonNull(s,"source");checkRows(rows);return axial(s.x,s.y,rows);
    }
    public static SourceGridCoord toSource(Hex h,int rows){
        Objects.requireNonNull(h,"axial");checkRows(rows);Hex s=source(h,rows);return new SourceGridCoord(s.q,s.r);
    }
    private static void checkRows(int rows){if(rows<1||rows>200)throw new IllegalArgumentException("源地图行数必须在1..200之间");}
'''
    assert needle in text;path.write_text(text.replace(needle,needle+addition))
    path=ROOT/CORE/'SourceGridCoord.java';text=path.read_text();idx=text.index('    public Hex toNativeAxial')
    text=text[:idx]+'''    public boolean inBounds(int columns,int rows){return columns>0&&rows>0&&x>=0&&y>=0&&x<columns&&y<rows;}
    public SourceGridCoord requireWithin(int columns,int rows){if(!inBounds(columns,rows))throw new IllegalArgumentException("源格坐标越界: "+this);return this;}
'''+text[idx:];path.write_text(text)
    path=ROOT/TEST/'NativeMap56Test.java';text=path.read_text();text=text.replace('MapCoordinateTest.main(args);','LegacyMapCoordinateCheckpointTest.main(args);MapCoordinateTest.main(args);');path.write_text(text)
    for p in parts:p.unlink()

def evidence(base):
    ref=json.loads((ROOT/'data/map/reference-v050.json').read_text());anchors=ref['reference']['calibration']['city_anchors'];cal=fit(anchors,True)
    national=props(ROOT/'core/src/main/resources/maps/national-map-v056.properties');oldtext=git('show',base+':core/src/main/resources/scenarios/heroes-mobile-sandbox.properties').decode()
    old=dict(l.split('=',1) for l in oldtext.splitlines() if '=' in l and not l.startswith('#'))
    oldsites={int(v.split('|')[0]):v.split('|') for k,v in old.items() if re.fullmatch(r'city\.\d+',k)}
    anchor={a['id']:a for a in anchors};prior={a['id']:a for a in json.loads((ROOT/'data/map/review-v055.json').read_text())['anchors']}
    sites=[]
    for sid in range(20000,20087):
        x,y=map(int,national['site.'+str(sid)].split(','));a=anchor.get(sid);p=prior.get(sid);row=native_cell(x,y)
        pixel=p['pixel'] if p else a['px'] if a else [round(sum(m*v for m,v in zip(axis,row)),3) for axis in cal['axes']]
        level='MULTI_ANCHOR_CALIBRATED' if p or a else 'ESTIMATED'
        note='Existing independently measured v055 landmark retained; not a newly measured collision boundary.' if p else 'Recorded 42-city pixel anchor; same-anchor residual is NOT independent accuracy.' if a else 'Prior reviewed mobile location and bank/pass topology; this pixel is fitted, not a measured landmark.'
        sites.append({'id':sid,'name':oldsites[sid][1],'kind':'CITY' if sid<20042 else 'GATE' if sid<20052 else 'PORT','old_coord':list(map(int,oldsites[sid][2:4])),'source_coord':[x,y],'axial_coord':[y-x//2+99,x],'reference_pixel':pixel,'evidence_level':level,'note':note})
    regions=[('guanluo',[43,61,99,87]),('northwest',[0,0,69,88]),('jinyang-huguan-ye',[69,20,120,66]),('northeast',[115,0,200,64]),('central',[89,60,163,111]),('jingxiang',[50,89,115,146]),('shudao',[0,74,65,121]),('bashu',[0,110,70,159]),('jianghuai',[119,77,179,128]),('jiangdong',[138,89,200,177]),('jingnan',[61,138,143,200]),('nanzhong',[0,151,76,200])]
    reviewed=[]
    for name,box in regions:
        x0,y0,x1,y1=box;inside=[s for s in sites if x0<=s['source_coord'][0]<x1 and y0<=s['source_coord'][1]<y1]
        reviewed.append({'region':name,'source_bounds':box,'sites':[s['id'] for s in inside],'evidence_counts':dict(collections.Counter(s['evidence_level'] for s in inside)),'status':'NATIVE_DATA_PRESENT_WITH_ESTIMATES','cell_by_cell_visual_review_complete':False})
    rows=[national[f'terrain.{y}'] for y in range(200)];different=sum(rows[y][x]!=old[f'terrain.{y//2}'][x//2] for y in range(200) for x in range(200))
    heterogeneous=sum(len({rows[y+j][x+i] for j in (0,1) for i in (0,1)})>1 for y in range(0,200,2) for x in range(0,200,2))
    output('calibration.json',{'schema':1,'reference':{'file':'MAP_SAN11.jpg','width':7200,'height':6752,'bytes':34925627,'sha256':REF_SHA,'distributed':False},'source_grid':{'columns':200,'rows':200,'topology':'odd-q with six neighbors'},'fit':cal,'anchor_count':42,'fit_evaluation':'In-sample residual, not held-out validation','reference_pixel_to_source':'Fit inverse -> nearest staggered cell; do not confuse with Android projection','reference_pixel_anchors':anchors,'global_visual_accuracy_certified':False})
    output('site-review.json',{'map_id':'san11-national','revision':56,'sites':sites,'counts':dict(collections.Counter(s['kind'] for s in sites)),'evidence_counts':dict(collections.Counter(s['evidence_level'] for s in sites))})
    output('region-review.json',{'regions':reviewed,'terrain_counts':dict(collections.Counter(''.join(rows))),'diff_from_old_nearest_neighbor_cells':different,'heterogeneous_2x2_blocks':heterogeneous,'all_87_sites_have_region':all(any(s['id'] in r['sites'] for r in reviewed) for s in sites),'caution':'Structural nationwide coverage does not certify every source cell against the illustration.'})
    output('uncertain.json',{'unverified_sites':[s['id'] for s in sites if s['evidence_level']=='ESTIMATED'],'terrain':'Recovered pixel digitization has noisy mountain/forest/road classifications. Full manual per-cell review remains incomplete. Do not present this as 100% original map fidelity.','small_features':'shoals, dams and isolated plank-road spans require further reference review','stream_recovery':'48 complete files verified; original source generator/calibration tail was not recovered'})

def manifest():
    path=ROOT/'tools/content/map-release-manifest.json';data=json.loads(path.read_text());data['release']='0.56.0';seen=set()
    for entry in data['files']:
        src=entry['source_path'];seen.add(src);sha=hashlib.sha256((ROOT/src).read_bytes()).hexdigest()
        if src.startswith('core/src/main/resources/scenarios/'):entry['sha256']=sha
        else:assert sha==entry['sha256'],'Unrelated existing artwork was modified: '+src
    paths=[ROOT/'core/src/main/resources/maps/national-map-v056.properties',*(ROOT/'app/src/main/assets/map/cities-v056').glob('*')]
    for path in sorted(paths):
        src=path.relative_to(ROOT).as_posix()
        if src in seen:continue
        apk=src.removeprefix('core/src/main/resources/') if src.startswith('core/') else src.removeprefix('app/src/main/')
        data['files'].append({'source_path':src,'apk_path':apk,'sha256':hashlib.sha256(path.read_bytes()).hexdigest()})
    write('tools/content/map-release-manifest.json',json.dumps(data,indent=2)+'\n')

def docs():
    write('version.properties','versionName=0.56.0\nversionCode=56\n')
    write('scripts/test-native56.sh','''#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p core/build/native56
find core/src/main/java core/src/test/java core/src/testFixtures/java -name '*.java' > core/build/native56/sources.txt
javac -encoding UTF-8 --release 17 -d core/build/native56 @core/build/native56/sources.txt
java -Xmx1500m -cp core/build/native56:core/src/main/resources:core/src/test/resources game.sanguo.core.NativeMap56Test
javac -encoding UTF-8 --release 17 -cp core/build/native56 -d core/build/native56 app/src/main/java/game/sanguo/mobile/TileGeometry.java app/src/main/java/game/sanguo/mobile/MapCamera.java app/src/test/java/game/sanguo/mobile/AndroidProjectionTest.java
java -cp core/build/native56:core/src/main/resources game.sanguo.mobile.AndroidProjectionTest
java -Xmx1g -cp core/build/native56:core/src/main/resources game.sanguo.core.MarchScale56Benchmark
''')
    write('scripts/test-map56-coordinates.sh','''#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p core/build/map56-coordinates
find core/src/main/java -name '*.java' > core/build/map56-coordinates/sources.txt
for name in LegacyMapCoordinateCheckpointTest MapCoordinateTest Native56Checks; do echo core/src/test/java/game/sanguo/core/$name.java >> core/build/map56-coordinates/sources.txt; done
javac -encoding UTF-8 --release 17 -d core/build/map56-coordinates @core/build/map56-coordinates/sources.txt
java -cp core/build/map56-coordinates:core/src/main/resources game.sanguo.core.LegacyMapCoordinateCheckpointTest
java -cp core/build/map56-coordinates:core/src/main/resources game.sanguo.core.MapCoordinateTest
''')
    write('docs/MARCH_SCALE_V056.md','''# Native-grid march scale (engineering calibration)

Measured using the same `MarchScale56Benchmark`, 10,000 spear troops, all route sites made friendly, no water shortcuts, no terrain edits. A shortest route terminates at any of seven goal-city cells. Baseline main bb8e58e was compiled separately, not simulated by halving new results. One turn is ten days; food is 1,000 units/turn for this army. Results are parameter checks, not historical-original formulas.

| Route | old cells/cost | new cells/cost | old/new move budget | old/new turns | old/new food |
|---|---:|---:|---:|---:|---:|
| Luoyang-Changan | 13/13 | 25/25 | 4/8 | 4/4 | 4000/4000 |
| Xiangyang-Jiangling | 9/9 | 21/21 | 4/8 | 3/3 | 3000/3000 |
| Hanzhong-Chengdu | 29/32 | 51/51 | 4/8 | 8/7 | 8000/7000 |

`MarchScale.base` recalibrates only retired mobile base movement budgets for the new national map and its crops. Six-neighbor topology, terrain costs, ranges, seven-cell footprints, ration formulas and skill/technology additive bonuses are not doubled. Local engineering fixtures retain old budgets. Ship/transport base budgets use the same geographic scale; their individual campaign route benchmarks remain pending. These results do not certify nationwide road fidelity.
''')
    write('docs/NATIVE_MAP_V056.md','''# v0.56 native map and original city atlas — validation boundaries

Formal ScenarioData attaches `maps/national-map-v056.properties` before coordinate normalization. Seven national scenarios use 200 columns by 200 rows; two regional scenarios reference the same resource with explicit even-column crop offsets. Story/resource/officer/diplomacy fields are unchanged. The rectangular axial storage contains padding, which is VOID and is not an additional source cell.

The original image is column-staggered; MapCoordinates explicitly converts source odd-q coordinates into internal Hex. TileGeometry, MapCamera, MapOverview and MapView project them back to geographic screen axes. Legacy odd-r APIs and their exhaustive tests remain. Source/axial/screen roundtrip, edges, culling and target hit testing use the same formal functions.

National IDs: san11-national / native-200 / revision56. SaveCodec31 retains map identity/crop information and rejects prior versions; old files are retained for the user rather than silently relocating cities or armies. Real seven-cell cities, explicit garrison versus ordinary traversal, footprint attack range and entity-level AoE remain in place.

42 cities bind CityArtCatalog; Luoyang and Changan are distinct. CityAtlas loads 30 original procedural RGBA PNGs (NEAR/MID/FAR for eight city variants plus gate/port), with explicit pivot, bounds, atlas rectangle, hashes and alpha audit. Far views still render buildings; armies draw later. Old buildings atlas remains solely for non-city facilities, not a city fallback. Art is stylized procedural isometric artwork, not copied original SAN11 textures or a claim of photorealism.

MapView culls visible source ranges and entity buckets, and separates overview/static rendering from dynamic selection. AndroidProjectionTest visits 353 candidates in its near-view test, rather than 40,000. Decoded city atlas is 13,475,840 bytes. Actual pan/zoom frame intervals, Android memory, installed screenshots and APK identity must come from the Native56Probe Actions artifact; no fabricated screenshots are supplied.

Reference identity was reverified from the user's archive. Existing 42-city pixel anchors and independently observed v055 Guan-Luo landmarks provide calibrated positions. Other ports/gates remain ESTIMATED. All 87 sites and 12 overlapping regional groups have records. The recovered native terrain is not nearest-neighbor 2x old terrain, but classification noise and unreviewed small features remain. The truncated original transport omitted its generator/calibration tail; 48 complete files were hash-verified, and the incomplete 49th file was discarded. **This checkpoint does not certify 100% nationwide geographic fidelity.**

Local native contracts and the v055 movement/garrison/combat/AI/save regression passed. The old all-core suite has a baseline failure in ContentIntegrationTest (also reproduced against unmodified main); it is not labelled passed. Android build/install status is recorded separately in progress.md and CI evidence.
''')
    write('progress.md','''# v0.56 native200 + city atlas — IN PROGRESS

Branch `agent/native-map-200-city-art-v056`, draft PR #44; base main bb8e58ebe32e96ddf453ee68fd23e21509f71247. Previous complete history is retained in docs/history/progress-through-v055.md. The Actions materialization commit contains the real source/data/PNG files; BUILD_COMMIT in its artifact is the exact APK source.

## DONE
- Native200 shared source map, seven nationwide scenarios plus two explicit crops; 87 sites, 591 national plots, 42 real seven-cell cities. Story fields preserved.
- Column-staggered source/axial/Android projection, source bounds/camera/culling, map revision and SaveCodec31 old-save rejection.
- CityArtCatalog/CityAtlas integrated in actual MapView at all LODs, army layer above cities; 30 real RGBA city/gate/port PNGs with pivots and alpha/SHA audits.
- Full typed odd-r checkpoint API/tests retained after recovering the 48 complete source files from the previously truncated transport.
- Local validation: native map contracts 1,230,589 assertions; legacy coordinate checkpoint 1,168,801; exact native Android projection 400,101 (353 near-view culling candidates); v055 formal footprint/move/entry/combat/AI/save 11,070; legacy UI projection 49,017, terrain connections 451, geometry 1,136,811. Counts include parameterized iterations, not distinct user workflows.
- Actual old/new march benchmarks documented for Guan-Luo, Jing-Xiang and Shu road; turns 4/4, 3/3, 8/7.

## IN PROGRESS
- Actions compilation, installed Native56Probe, screenshots, source/signature/APK identity. No success is claimed until the returned run/artifact is inspected.
- Nationwide reference review: current records distinguish calibrated anchors from estimated gate/port locations.

## TODO
- Complete manual per-cell terrain review, including noisy road/mountain/forest classification and small shoal/plank/dam features.
- Further port/ship route benchmarks, physical ARM-device performance and final quality acceptance.
- Reconcile historical all-core assertions with current rules. ContentIntegrationTest failure was reproduced on unmodified main too; full suite is not reported green.

## BLOCKER / limits
- Shell GitHub DNS and local Android SDK unavailable; authenticated connector plus Actions used. Main is never force-pushed.
- Original compressed transport lacked its tail. Only the full-SHA-verified first 48 sections were applied; missing original generator/calibration was not fabricated. New evidence describes its uncertainty explicitly.
- This is a real native-map/art build checkpoint, not certification of perfect geographic fidelity or completed Android runtime validation.
''')
    p=ROOT/'README.md';text=p.read_text().replace('地形来自高精图逐格采样，仍有明确ESTIMATED项','地形为恢复的原生格数字化数据，采样生成器尾部缺失且分类噪声尚待复核，仍有明确ESTIMATED项');p.write_text(text)
    p=ROOT/'core/src/main/resources/maps/national-map-v056.properties';text=p.read_text().replace('# Native 200x200 odd-q source cells digitized from private MAP_SAN11.','# Native 200x200 odd-q source cells; recovered digitization, evidence and limits in data/map/reference-v056.');p.write_text(text)

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--base',default=BASE);parser.add_argument('--already-recovered',action='store_true');a=parser.parse_args()
    if not a.already_recovered:recover(a.base)
    migrate();art();docs();evidence(a.base);manifest()
    print('Materialized production code, shared map, nine scenarios and 30 PNG assets; Android results remain independent.')
if __name__=='__main__':main()
