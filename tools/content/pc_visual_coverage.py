#!/usr/bin/env python3
"""Update explicit visual scope without treating conversion as PC acceptance."""
from pathlib import Path
import json,re

ROOT=Path(__file__).resolve().parents[2]
DOC=ROOT/'docs/pc-visual'
entries=[]
units=json.loads((DOC/'units-source.json').read_text()) if (DOC/'units-source.json').exists() else None
unit_resources=[2236,*[m['resource'] for m in units['models']],*[m['texture_resource'] for m in units['models']]] if units else []

def add(category,label,status,resources=(),binding=None,evidence=(),remaining=None):
    entries.append(dict(category=category,label=label,status=status,
        source_file='Media/san11pkres.bin' if resources else None,resource_ids=list(resources),
        source_hashes='inventory.json' if resources else None,format_conversion='FORMAT_NOTES.md' if resources else 'unresolved',
        runtime_binding=binding,evidence=list(evidence),remaining=remaining or 'PC运行对照、形状/材质/时序验收尚未完成'))

for name in ['地形','山体','悬崖','道路','栈道']:
    add('地图与环境',name,'inherited-source-unverified',[4791,4793,*range(4787,4791),*range(4800,4804)],'PcMap / TerrainSurface / SceneMesh / pc-ground.mat',['../pc-map/source.json','validation.json'],'已接入地格/高度/四季贴图；源法线、比例、UV密度和PC镜头对照待校准')
walls=json.loads((DOC/'cliff-walls-source.json').read_text())
add('地图与环境','源地图独立悬崖墙','source-integrated-unverified',[4805,*[m['id'] for m in walls['models']],*[t['id'] for t in walls['textures']]],'PcCliffWalls / FilamentMapView',['cliff-walls-source.json','validation.json'],'161原落点与154唯一边依源EXE连接柱/高度剪切接入，CPU和安装三处春冬/全国/生命周期通过；源初始化地表变形和PC对照待完成；堤防独立推进')
for name in ['森林','树木','灌木/地面簇','源地形19草簇']:
    add('地图与环境',name,'source-integrated-unverified',[4805,*range(4808,4834),*range(4840,4844)],'PcScenery / FilamentMapView',['scenery-source.json','validation.json'],'943条原落点、源近远模型和四季贴图接入；树木偏暗、状态变化和PC参考待验证')
if (DOC/'environment-source-working.json').exists():
    add('地图与环境','静态对象原绘画光照','source-integration-candidate',[4799,4806],
        'PcEnvironment → pc-scenery.mat / FilamentMapView static original scenery/sites/facilities',
        ['environment-source-working.json','shading-source-working.json','validation-v141-working.json'],
        '源顶点法线光照UV、两次MODULATE2X、RGBA透明/深度写入接入候选；源程序参数/状态核对通过。安装证据单列；雾/环境光、部队/地面材质、过滤/MIP/透明色彩空间、原排序/裁剪、比例和PC对照未完成')
    add('地图与环境','原静态轮廓演出','converted-runtime-pending',[4807],None,
        ['environment-source-working.json','shading-source-working.json'],
        '源轮廓贴图及77b008着色器已识别；APK仅封装原像素，轮廓pass/厚度/颜色/源裁剪与PC对照尚未接入')
dam=json.loads((DOC/'dams-source.json').read_text())
add('地图与环境','水坝','source-integrated-unverified',[4805], 'NaturalStructures / PcDamCatalog / PcDams / PcFacilities / FilamentMapView',['dams-source.json','validation.json'],'4条原落点、原模型/近远LOD/受损状态由真实军事实体驱动；8次正常攻击安装验证，击破无静态重复及存档复活；原溃坝/洪水动画、PC画面对照和ARM性能待完成')
for name in ['岩石','毒泉','瀑布','天空','雾','光照','阴影']:
    add('地图与环境',name,'partial-or-legacy-pending',[4792,4795,4799,4804,4805],None,['inventory.json','placements.json'],'地理/水位部分已接入；对应模型/源特效/环境配置未完整解码，禁止用旧近似美术验收')
for name in ['河流','湖泊','海面','岸线','水面动画']:
    add('地图与环境',name,'source-integration-candidate',[4793,4844],
        'PcMap coarse water → SceneMesh.sourceWater → pc-water.mat / FilamentMapView visual clock',
        ['water-source.json','validation-v147-working.json'],
        '原粗水格65536记录、完整四角透明度、4844两套64帧贴图接入v147；原时钟/深度写入源核对和全国网格检查通过。安装/视频另列；PC图像、MOD注册表覆盖、原逐格可见列表时钟、非可玩全地表与ARM仍待完成')
add('地图与环境','季节变化','source-integrated-unverified',[*range(4787,4791),*range(4800,4804),*range(4840,4844)],'snapshot.month → pcSeason / PcScenery.season',['validation.json'],'四季地图/植被接入；PC日历/运行颜色与环境变化待对照')
for name in ['城池','城墙','城门','关隘','港口']:
    sites=json.loads((DOC/'sites-source.json').read_text())
    add('城池与设施',name,'source-integrated-unverified',[4805,*[m['id'] for m in sites['models']],*[t['id'] for t in sites['textures']]],'PcSites / FilamentMapView',['sites-source.json','validation.json'],'42城/10关/35港源主体与城墙、近远LOD、HP和内政规模模型接入；原旗帜、关港区域冬季材质、透明/光照与PC对照仍待完成')

def enums(file,enum):
    text=(ROOT/'core/src/main/java/game/sanguo/core'/file).read_text()
    match=re.search(r'enum '+enum+r'\s*\{(.*?);',text,re.S)
    if match is None:raise ValueError(f'Missing enum {file}:{enum}')
    return re.findall(r'\b([A-Z][A-Z_0-9]*)\("([^"\n]+)"',match[1])

for file,enum,category in [('Domestic.java','Kind','内政建筑'),('War.java','StructureKind','军事设施'),('World.java','Weapon','兵种'),('Army.java','Ship','船只'),('War.java','Tactic','战法'),('Army.java','Tactic','水军及器械战法'),('War.java','Plot','计略')]:
    for key,label in enums(file,enum):
        if category in ('内政建筑','军事设施'):
            facilities=json.loads((DOC/'facilities-source.json').read_text())
            add(category,label,'source-integrated-unverified',[*[m['id'] for m in facilities['models']],*[t['id'] for t in facilities['textures']]],'FacilityState → PcFacilities / PcConstructibleWalls / FilamentMapView',['facility-bindings.json','facilities-source.json','validation.json'],'源主体、等级/季度/近远模型及HP/建设状态已接入；正常指令覆盖农场/阵/土垒/石墙，土垒石墙源连接状态由建成/HP驱动，其他种类安装证据、区域冬季、透明/光照、旗帜/燃烧/坍塌/洪水及PC对照待完成')
            if key=='CATAPULT_TOWER' and (DOC/'facility-rigs-source.json').exists():
                entries[-1]['resource_ids'].extend([2234,2235,2236])
                entries[-1]['runtime_binding']+=' / PcFacilityRigs actual FACILITY_ATTACK'
                entries[-1]['evidence'].extend(['facility-rigs-source.json','facility-rigs-validation-working.json'])
                entries[-1]['remaining']='原正常/受损六骨骼发射主体及73/74源曲线接入真实下一旬；正常/受损真实下一旬默认240秒128项验证通过。源投射物、旗帜、PC时序/比例/透明对照及完整连续时序视频对照待完成'
        elif category in ('兵种','船只') and units:
            add(category,label,'source-integrated-unverified',unit_resources,'UnitVisual → PcUnits original rig/curve → PcUnitFormation → FilamentMapView',['units-source.json','units-validation-working.json'],'14原地图模型、14独立原RGBA、75原FCVD曲线、源不透明/透明分组和编队数量已接入；CPU源校验通过，正常指令GPU逐兵种安装验证进行；动作语义、比例/光照/透明排序、旗帜及PC视频对照未通过')
        else:
            add(category,label,'legacy-pending',(),f'core {file}:{enum}.{key} → current mobile renderer',[],'正常玩法已有对应规则；原资源编号、动画、源事件效果、PC录像对照待完成')
catalog=json.loads((DOC/'facility-bindings.json').read_text())
for row in catalog['facilities']:
    if row['facility_id'] in (14,15,23,28,29,*range(40,48),49):
        add('源定义其他设施',row['source_name']+' #'+str(row['facility_id']),'converted-runtime-pending',[v['near']['id'] for v in row['model_variants']],None,['facility-bindings.json','facilities-source.json'],'整合版定义和源模型已核对/转换；当前core没有相应正常玩法实体，事件/静态场景绑定与PC对照待查，不增设近似物件冒充')
add('兵种','运输部队','source-integrated-unverified' if units else 'legacy-pending',unit_resources, 'Domestic.Mission → UnitVisual → PcUnits source supply/boat', ['units-source.json','units-validation-working.json'] if units else [],'原车队/走舸模型与源曲线已接入；货物/旗帜、正常指令安装证据及PC动作/比例对照待完成')
for name in ['人物','坐骑','武器','旗帜','编队','朝向','待机','移动','攻击','受击','撤退','溃灭']:
    bound=units and name not in ('旗帜','撤退')
    add('部队状态',name,'source-integrated-unverified' if bound else 'pending',unit_resources if bound else [],'PcUnits / PcUnitFormation / normal visual journal' if bound else None,['units-source.json','units-validation-working.json'] if units else [],'原WKMD骨骼/权重、FCVD全局帧多项式和RGBA已绑定；编队只接入默认及状态7落点，事件动作语义仍待PC运行确认，旗帜及撤退专用语义未完成，不能以程序化动作验收')
for name in ['等级','建造中','受损','燃烧','摧毁','归属旗帜']:
    add('建筑设施状态',name,'partial-or-legacy-pending',(),None,['sites-source.json','facilities-source.json','validation.json'],'源城墙/关港受损、城市规模、设施主体等级/建设/受损已接入；摧毁可由正常命令移除实体，原燃烧/坍塌/洪水/归属旗帜演出仍待完成')
add('地图与战斗动态','SEFF八类地图常驻效果（语义待PC对照）','source-integration-candidate',[124,4792,133,134,141,142,143,144,145,148],'normal PC map → original private visual VM → PcMapEffects source-order quads',['worker-source-working.json','effect-textures-source.json','validation-v149-working.json','validation-v150-working.json'],'126源落点、八原模板、33原RGBA已进入v150生产GPU候选；v149安装运算/传输296包通过，原RH相机与组合视图投影80966项核对。v150正常地图零面和管道中断失败保留；后续启动/读管道修复仍待安装验收。具体毒泉/瀑布等语义、PC像素/时序/MOD覆盖与ARM性能未确认')
for name in ['落石','毒泉','水流','瀑布','水坝破坏','洪水','火焰','烟雾','燃烧','爆炸','箭矢','投石','冲锋','碰撞','暴击','特殊效果']:
    add('地图与战斗动态',name,'source-runtime-unrecovered',[4792],None,['placements.json','effect-bindings-source.json','effect-curves-working.json','effect-uv-working.json','validation-v141-working.json'],'126条SEFF原实例表绑定、246个KSEF图层边界、347张追加RGBA与33共用图已记录；887条primitive2几何和2008条UV求值经源代码核对。Motion标量及矩阵表达式布局已解析，不代表控制器/时钟/发射器/材质/事件接入。源3D禁止混入旧代用效果；原动态效果仍0，PC对照视频未完成')
for name in ['计略高光文字','暴击高光文字','原字形/笔触/描边/发光','背景','人物/立绘','层次','入场','退场','持续时间','镜头运动','连续触发顺序','播放加速','跳过','暂停恢复','场景退出释放']:
    add('全屏演出',name,'source-runtime-unrecovered',(),None,['effect-bindings-source.json','validation-v140-working.json'],'源动态贴图/模板表及共用图32覆盖位置已确认；原实例播放/镜头/事件未接入。源3D已禁用通用CriticalFlash代用，旧/自定义地图兼容路径明确分离；不能用Toast/通用字放大/普通粒子替代')
for name in ['选择标记','范围提示','伤害文字','旗帜','图标','关联界面演出','触控可用']:
    add('关联界面',name,'legacy-pending',(),None,[],'原图集、位置与时序待恢复；移动触控与遮挡测试需随新素材复验')
for name in ['相机遮挡','透明排序','多效果叠加','长期移动/缩放/旋转','场景切换','内存增长','正常指令动态视频','全国地图及典型对象','PC同地点同季节同镜头对照','ARM真机']:
    add('综合验证',name,'pending',(),None,['validation.json'],'本阶段仅模拟器植被/季节/LOD/退出释放验证，不能冒充全范围或真机性能验收')

if (DOC/'ground-passes-source.json').exists():
    for row in entries:
        if row['category']=='地图与环境' and row['label'] in ('雾','光照'):
            row.update(status='source-integrated-unverified',resource_ids=[4793,4799,4800,4801,4802,4803],
                runtime_binding='PcEnvironment / FilamentMapView ground / pc-ground.mat',
                evidence=['ground-passes-source.json','validation-v157-working.json'],
                remaining='v157原地表SENV默认季节环境色、c26雾/c27淡出和未归一化原法线明暗已接入；秋冬原实测常量和生产Java逐位一致。其他对象/单位原雾、天气变体/1500ms过渡、原LOD、远处透明目标合成和PC完整画面对照仍待完成')
    add('地图与环境','原地表法线/明暗层','source-integrated-unverified',[4793,4800,4801,4802,4803],
        'FilamentMapView raw numeric1025normal / source RGBA paint / pc-ground.mat',
        ['ground-passes-source.json','validation-v157-working.json'],
        '真实源像素及数值法线接入；原三角形LOD、逐材质透明覆盖层/图集MIP、PC精确像素验收仍待完成')
    add('地图与环境','原地表背面描边','source-integrated-unverified',[4793,4807],
        'Shared terrain VB/IB / pc-ground-outline.mat / original draw24 c3=(1.3,1.3,1.3,1)',
        ['ground-passes-source.json','validation-v157-working.json'],
        '共享地形缓冲和原量化法线挤出、原RGBA及季节雾/淡出接入；各物体77b008轮廓、原LOD/全局透明排序和PC图像仍待完成')
    add('关联界面','原地表网格贴图','converted-runtime-pending',[4804],None,
        ['ground-passes-source.json','validation-v157-working.json'],
        '原4804 image1透明索引WFTX已独立解码及PC实际像素核对并封装；原网格UV/选择状态/正式绘制尚未绑定，不能作为已实现')

if (DOC/'presentations-source-working.json').exists():
    for row in entries:
        if row['category']=='全屏演出':
            row.update(status='source-integrated-unverified',resource_ids=[124,240,246,247,495,496,521],
                source_file='Media/san11pkres.bin',source_hashes='presentations-source-working.json',
                format_conversion='import_pc_presentations.py / original isolated source factory/controllers/final GPU writes',
                runtime_binding='immutable TurnJournal → PcPresentationPlan → MapHost/TurnPlayback → PcPresentations',
                evidence=['presentations-source-working.json','plot-presentations-source-working.json','presentation-calls-source-working.json','validation-v159-working.json'],
                remaining='当前关羽1001战法暴击152/115、成功妖术暴击126/121和落雷暴击127/122；原名称表与分派已纠正此前扰乱误绑定。正常命令/存档RNG主机72161及计略事实150项通过。默认750ms，用户接受500～1000ms。关羽连续15源帧/~733ms；单独妖术/落雷各21源帧、~746/751ms并PASS23；新候选三类严格色差/生命周期PASS284（最大/总采样误差0），旧分项PASS247；组合连续FAIL仍保留，旧重启候选关羽连续2帧FAIL；新增提交计时/源深度相机主机72378、关羽严格136、三类连续提交76项/~754～790ms通过，随机seek缺演出判断撤回，顺序decode确认原图层；当前恢复Window关羽连续35项/~820ms通过，新首预热视口修复安装129项/522396像素误差0、三类正常连续81项与真实逐样本视频通过；妖术估算1140ms仅采样容差，原镜头/MOD/远景/其他绑定仍待查。旧43/376项保留，但旧扰乱录像不作正确绑定验收。旧缓存色差FAIL11已通过真实视口变化重捕获修复；其他人物/计略、原镜头/编码混合/MOD/PC对照/ARM未完成')
        elif row['category']=='地图与战斗动态':
            row['remaining']=row['remaining'].replace('原动态效果仍0','八模板地图常驻原播放已接入，战斗动态事件绑定仍未完成')
    add('全屏演出','关羽正常突刺暴击首批原演出','source-integrated-unverified',[124,240,521],
        'TurnJournal.critical.officerId1001/name关羽 → selector152/template115 → normal command750ms prelude',
        ['presentations-source-working.json','validation-v159-working.json'],
        '分段暂停实际源GPU立绘/笔触/放射光截图已确认；年龄脸表变体/PC镜头、编码混合／PC时序／ARM未通过；新750ms正常连续15源帧/~733ms；新候选严格色差/生命周期整轮PASS284，旧FAIL11与分项PASS247保留历史；旧重启候选连续FAIL2源帧；新增源深度/提交计时关羽严格136、连续整轮76源提交检查通过，随机seek缺演出判断撤回，顺序解码确认立绘/笔触/光层；恢复Window关羽连续35项/~820ms通过，首灰白由预热视口修复，安装129/三类连续81通过，原镜头/MOD/其他人物/ARM仍待查')
    add('全屏演出','妖术暴击原书法演出','source-integrated-unverified',[124,246,495],
        'immutable successful critical PlotOutcome(SORCERY) → selector126/template121',
        ['presentations-source-working.json','validation-v159-working.json'],
        '原source7妖術/name8aecb4/592050原调用确认；移除错误CONFUSE绑定。正常妖术单项连续21源帧、入场主体退场PASS23，组合连续FAIL保留；反射事实不重复随机数，源镜头/完整动态对照/ARM仍未通过')

    add('全屏演出','落雷暴击原书法演出','source-integrated-unverified',[124,247,496],
        'immutable successful critical PlotOutcome(LIGHTNING) → selector127/template122',
        ['presentations-source-working.json','plot-presentations-source-working.json','validation-v159-working.json'],
        '原source8落雷/592ed0调用及原247模板、496书法转换接入；正常规则/完整RNG存档通过，单独安装连续21源帧、入场主体退场PASS23，组合连续FAIL保留；PC画面/时序/ARM未验收')

inventory=json.loads((DOC/'inventory.json').read_text())
main=next(a for a in inventory['archives'] if a['path'].lower()=='media/san11pkres.bin')
catalogs={fmt:[e['id'] for e in main['entries'] if e['format']==fmt] for fmt in ['WKMD0010','KSEF0131','TOD20053','FCVD0022','NUNO0220','PAC02_00','AIMG0001','WFTX0010']}
result=dict(schema=1,goal_status='active',complete=False,retail_baseline=None,
    definitions={'source-integrated-unverified':'真实源资源已接入安卓；尚未通过PC运行对照','source-integration-candidate':'已进入生产渲染代码和候选APK；安装与源画面对照另列','source-runtime-unrecovered':'已调查源资源，原播放未接入；源3D不使用旧代用效果','inherited-source-unverified':'继承v131数据接入，校准尚未完成','legacy-pending':'仍为旧移动素材/效果，禁止当作原版验收','pending':'未完成所需证据'},
    source_resource_catalogs=catalogs,source_catalog_status='仅格式分类，源效果/动作名称和MOD新增范围还须从资源引用及运行证据补全',entries=entries)
(DOC/'coverage.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
print(f'{len(entries)} explicit scope entries; goal active; no PC visual acceptance')
