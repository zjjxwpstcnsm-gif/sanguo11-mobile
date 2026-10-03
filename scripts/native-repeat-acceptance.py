#!/usr/bin/env python3
"""Evidence-only publisher. Does not execute or modify game code, assertions or timeouts."""
import collections, copy, hashlib, json, pathlib, re, subprocess
ROOT=pathlib.Path(__file__).resolve().parents[1]
BASE=ROOT/'docs/native-pc-visual'; OLD=BASE/'evidence/full-acceptance'; OUT=BASE/'evidence/repeat-20260926'
SOURCE='5f8997ebfef15f4680931d40532411a336506d0d'; APK='6ba2211d961567b2aa396ba19e3018c1dc44780fa4351a807db099fc643bd058'
PRIOR='702feccf5ba83d49ae9b6e95ef531f7df3df4f77'
def load(p): return json.loads(p.read_text())
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,v): p.parent.mkdir(parents=True,exist_ok=True); p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n')
HEAD=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
subprocess.run(['git','diff','--exit-code',SOURCE,'HEAD','--','app/src/main','core','game-api','game-runtime','data','unity','tools/3d','version.properties','app/build.gradle'],cwd=ROOT,check=True)
old=load(OLD/'matrix.json'); rows=copy.deepcopy(old['requirements']); original=load(OLD/'original213-history.json'); byid={r['id']:r for r in rows}
assert len(rows)==435 and len(original['requirements'])==213
for r in original['requirements']:
    assert byid[r['id']]['requirement']==r['requirement']
    assert byid[r['id']]['source']['file']==r['prompt'] and byid[r['id']]['source']['line']==r['line']
locs={'01_ROADMAP.md':[4,38,42,44,52],'02_BASELINE_AUDIT.md':[3,10,19,21,38,46,50,52,58],'08_SOURCES.md':[7,24,29,34,39,44,49,56,60],'prompts/R00_PROMPT.md':[26],'EXECUTE_PROMPT.txt':[68,78,139,146]}
added=[]
for f,ns in locs.items():
    p=OLD/'originals'/f
    for n in ns:
        code=('H' if f=='EXECUTE_PROMPT.txt' else 'YP' if f.startswith('prompts/') else 'Y'+f[:2])+f'.L{n:03d}'
        stage={68:'R12',78:'R12',139:'R11',146:'R13'}.get(n,'GLOBAL') if f=='EXECUTE_PROMPT.txt' else 'GLOBAL'
        r={'id':code,'stage':stage,'requirement':p.read_text().splitlines()[n-1],'source':{'file':f,'line':n,'sha256':sha(p)},'original_kind':'SUPPLEMENT','history':{'reason':'恢复此前被归为背景的规范段落，或补充整改交接中的精确验收条件；不降低原文。'},'current_status':'NOT_RUN','formal_implementation':copy.deepcopy(byid['X00.L026']['formal_implementation']),'normal_entry_call_chain':'See source-paths.json and original clause; cross-cutting contract, not a new game feature.'}
        if code=='YP.L026': r['source_aliases']=[{'file':f'prompts/R{i:02d}_PROMPT.md','line':26,'sha256':sha(OLD/'originals'/f'prompts/R{i:02d}_PROMPT.md')} for i in range(15)]
        added.append(r)
assert len(added)==28
rows+=added
summary=load(OLD/'summary.json'); gaps={s['stage']:s['gap'] for s in summary['stages']}
gaps.update({'R00':'两API独立正常冷启动全国预览FAIL；S01–S13逐项当前质量/完整环境门槛未闭合，不能把源码存在当完成。','R01':'本轮API29初始ready失败，切换0/前后台0；API35完成切换20/前后台1后background frame loop stopped断言失败。owner仍同步读材质、atlas和rig；晚到worker/故障恢复后半段NOT_REACHED。','R12':'原NativeR12两API均初始ready失败。独立全国预览FAIL。API29月7/月4权威与widget正确，原整屏仍190年1月上旬；UI draw95/Window frames89冻结而3D颜色改变。完整纯触控尾链NOT_REACHED。','R14':'月份到季节正式接入；API29三月份fixture到达且Surface颜色变化，但UI日期冻结。四季×近中远×三画质、阴影/雾/过绘、长稳和真机仍未闭合。','GLOBAL':'原文及历史全部保留。当前共享源码/HOST/安装证据只证明明示子范围，未执行的必要条件不自动升级。'})
changes={'R12.I05':'FAIL','R12.I06':'FAIL','R14.V04':'PARTIAL','R14.V05':'PARTIAL','H.L068':'FAIL','H.L078':'FAIL','H.L139':'PARTIAL','H.L146':'PARTIAL'}
positives={'R00.I05','R00.I06','R00.V03','R01.I07','R02.I07','X00.L026','X00.L030','X00.L034','X03.L041'}
assert {r['id'] for r in old['requirements'] if r['current_status']=='PASS'}==positives
specific={'R00.I01':'实时分支/main/PR、未提交状态及保护树已核对；S01–S13每项当前实现质量、安装和可复用/需修复对应仍未全部闭合。','R00.I02':'本轮容器JDK21以--release17执行原core；CI为JDK17/SDK35。容器无adb/KVM；ARM64和全部建模/性能工具实际运行未验证。','R00.I07':'PC手册07原参考已查看；洛阳原参考相机/版本缺字段，未伪造近似相机完整图组。','R12.I05':gaps['R12'],'R12.I06':gaps['R12'],'R14.V04':'当前原整屏UI可辨且未被世界调色染糊，但日期错误归R12；攻击范围后处理、多屏尺寸/方向未完整执行。','R14.V05':'API29三个真实安装月份fixture已到达并记录计数；四季连续长时间切换与逐旬解码/上传完整设备矩阵未完成。','H.L068':'真实触点在全国预览超时；势力确认、出征、移动、攻击、战报、下一旬、手动保存/读档NOT_REACHED。当前cold探针本身也不实现全部后半段。','H.L078':'API29独立整屏/Surface/录像交叉确认UI陈旧FAIL；控件实例ID和bounds、真实手动读档/跨月跨年完整链仍需采集。','H.L139':'类型化效果进入正常路径；CALM/EXTINGUISH实际事件未逐类产生并实拍，不能由已有fixture冒称全覆盖。','H.L146':'内部AtomicFile不能证明外部SAF provider原子替换；取消/撤销权限/断写/杀进程/重启端到端未执行。'}
raw=['audit/raw/input-integrity.json','audit/raw/nested-integrity.json','audit/raw/apk-independent-identity.json','audit/raw/reaudit-host-attempt2','audit/raw/core','audit/raw/authority-independent.json','audit/raw/media-review/video-validation.json','audit/raw/remote-snapshot-1','audit/raw/runtime/reaudit-api29-all-attempt2','audit/raw/runtime/reaudit-api35-all-attempt2','audit/raw/runtime/reaudit-api29-cold-attempt2','audit/raw/runtime/reaudit-api35-cold-attempt3','audit/raw/runtime/reaudit-api29-lifecycle-attempt2','audit/raw/runtime/reaudit-api35-lifecycle-attempt2','audit/raw/runtime/reaudit-api29-parity-attempt2','audit/raw/runtime/reaudit-api35-parity-attempt3']
source_paths={}
for r in rows:
    rid=r['id']; stage=r['stage']; prior=r['current_status']
    r['prior_review']={'head':PRIOR if r not in added else None,'status':prior,'gap':r.get('current_gap'),'full_record_ref':f'{PRIOR}:docs/native-pc-visual/evidence/full-acceptance/matrix.json#{rid}'}
    r['current_status']=changes.get(rid,prior); r['apk_source_sha']=SOURCE; r['apk_sha256']=APK; r['inspected_head']=HEAD
    r['requirement_types']=r.get('requirement_types') or ['架构','交付']
    r['current_gap']=specific.get(rid,gaps[stage]) if r['current_status']!='PASS' else '已按当前生产源码、原APK字节、精确原构建结果和本轮HOST日志复核此限定条目；不扩展为阶段通过。'
    r['necessary_conditions']=[x.strip() for x in r['requirement'].split('；') if x.strip()]
    r['unverified_conditions']=[] if r['current_status']=='PASS' else ['尚需完整闭合：'+x for x in r['necessary_conditions']]
    r['previous_evidence_paths']=r.get('evidence_paths',[]); r['evidence_paths']=raw
    r['validation']={'method':'本轮原文逐字/路径核验、原完整core及42调用矩阵、20个HOST门禁、隔离AVD原APK原断言原120s、完整存档字节、原PNG和逐段完整解码/内容抽样分层核对。','environments':['Linux local JDK21 --release17 (core)','CI JDK17','API29 x86_64 Pixel2 1080x1920','API35 x86_64 Pixel2 1080x1920'],'backend':'Filament1.56.0 OPENGL → GLES Translator → ANGLE → SwiftShader software','physical_arm64':'NOT_RUN','scope_limit':'共享证据仅支持日志所示子范围；没有为每条都执行独立完整设备/美术矩阵。'}
    r['evidence_dimensions']={'source':'正常入口与文件哈希复核，存在不等于完整满足','host':'20门禁exit0；原完整core两边exit1，42调用退出矩阵相同','installation':'两API安装原v112；另一次API35 adb初始化失败未运行应用','runtime':'cold/R12/lifecycle FAIL；parity只证明fixture范围','visual':'日期和岸线有实际失败；完整V1/V2/兵种/四季图组未闭合','performance':'PARTIAL诊断；无物理ARM64帧时/PSS/GPU/热稳定PASS','physical_device':'NOT_RUN'}
    for impl in r['formal_implementation']:
        paths=[impl['path']] if 'path' in impl else impl.get('paths',[])
        impl['current_file_sha256']={}
        for path in paths:
            p=ROOT/path; assert p.is_file(),path
            impl['current_file_sha256'][path]=sha(p); source_paths[path]={'sha256':sha(p),'lines':len(p.read_text().splitlines())}
    p=OLD/'originals'/r['source']['file']; assert sha(p)==r['source']['sha256']
    line=p.read_text().splitlines()[r['source']['line']-1]; assert r['requirement'] in line or re.sub(r'^\d+[.、]\s*','',line)==r['requirement'],rid
counts=dict(collections.Counter(r['current_status'] for r in rows))
assert len(rows)==len({r['id'] for r in rows})==463
assert counts=={'PASS':9,'FAIL':20,'PARTIAL':174,'NOT_RUN':256,'NOT_REACHED':4},counts
matrix={'schema':4,'audit':'repeat-20260926','all_requirements_satisfied':False,'counting':'213原始复合条目+此前222补充+24漏联规范段+4整改验收澄清=463行，不是独立功能数或完成率。复合原文不降低；R15–R18未来工作不在本轮执行。','apk_source_sha':SOURCE,'apk_sha256':APK,'inspected_head':HEAD,'previous_matrix':f'{PRIOR}:docs/native-pc-visual/evidence/full-acceptance/matrix.json','original_count':213,'prior_supplemental_count':222,'new_supplemental_count':28,'total':463,'counts':counts,'requirements':rows}
write(OUT/'matrix.json',matrix); write(OUT/'source-paths.json',source_paths)
coverage=load(OLD/'source-coverage.json'); newlinks={}
for r in added:
    for s in [r['source']]+r.get('source_aliases',[]): newlinks[(s['file'],s['line'])]=r['id']
for c in coverage['lines']:
    if (c['file'],c['line']) in newlinks:
        c['previous_classification']=c.get('classification'); c['classification']='normative_paragraph_recovered'; c['requirement_id']=newlinks[(c['file'],c['line'])]
coverage.update({'original213_verbatim_preserved':True,'prior_false_completeness_claim':'Zero-unmapped did not establish semantic completeness: normative paragraphs had been classified as context.','added_requirements':[{'id':r['id'],'source':r['source'],'aliases':r.get('source_aliases',[]),'requirement':r['requirement']} for r in added],'scope_note':'保留所有原始行和历史；重复共通段落用别名关联。原开发阶段必须修改生产源码的过程要求，不强迫当前审计制造生产diff，也不由当前审计追认旧阶段交付PASS。'})
write(OUT/'coverage.json',coverage)
summary={'total':463,'counts':counts,'stages':[],'global_counts':dict(collections.Counter(r['current_status'] for r in rows if r['stage']=='GLOBAL'))}
for i in range(15):
    stage=f'R{i:02d}'; c=dict(collections.Counter(r['current_status'] for r in rows if r['stage']==stage)); summary['stages'].append({'stage':stage,'status':'FAIL' if c.get('FAIL') else 'PARTIAL','counts':c,'gap':gaps[stage]})
write(OUT/'summary.json',summary)
md=['# R00–R14 本轮逐条矩阵','', '整体：否。原文/位置/历史/正常入口/文件哈希/设备/原始证据及限定范围见 matrix.json。','','| ID | 阶段 | 当前 | 来源 | 原文 | 缺口 |','|---|---|---|---|---|---|']
for r in rows: md.append('| '+' | '.join(str(x).replace('|','\\|') for x in [r['id'],r['stage'],r['current_status'],r['source']['file']+':'+str(r['source']['line']),r['requirement'],r['current_gap']])+' |')
(OUT/'MATRIX.md').write_text('\n'.join(md)+'\n')
for status in ['FAIL','PARTIAL','NOT_RUN','NOT_REACHED']:
    (OUT/(status+'.md')).write_text('# '+status+'\n\n'+'\n\n'.join(f"## {r['id']} · {r['stage']}\n\n{r['requirement']}\n\n{r['source']['file']}:{r['source']['line']}\n\n{r['current_gap']}" for r in rows if r['current_status']==status)+'\n')
manifest={'schema':1,'audit':'repeat-20260926','all_requirements_satisfied':False,'production_changed':False,'apk_source_sha':SOURCE,'apk_sha256':APK,'apk_bytes':37387038,'application_id':'game.sanguo.mobile.dev','version_code':112,'version_name':'0.112.0-native-first-asset-lod','abis':['arm64-v8a','armeabi-v7a','x86','x86_64'],'signer_cert_sha256':'8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24','inspected_head':HEAD,'remote_input_head':PRIOR,'main':'ac29b458325b52d6e302ca44270d16552de4ed7f','total':463,'counts':counts,'runtime_ci':{'strict_run':36222182595,'strict_attempt':2,'original_run':36213760665,'host_attempt':2,'api35_cold_attempt':3,'parity_run':36222424271,'api29_parity_attempt':2,'api35_parity_attempt':3},'artifacts':{'api29_cold':10902830047,'api35_cold_infra_only':10902695822,'api35_cold_retry':10901554856,'api29_all':10902399818,'api35_all':10901933906,'api29_lifecycle':10902656301,'api35_lifecycle':10903021642,'api29_parity':10902602820,'api35_parity':10903645682,'host':10901639309},'results':{'api29_cold':'FAIL pending285 submitted8','api35_cold':'FAIL pending324 submitted2','r12':'Both original tests FAIL initial ready; original timeouts/assertions preserved','date_api29':'FAIL: full PNG January while authority/snapshot/widget July or April; UI95/Window89 frozen; Surface changes','lifecycle_api29':'FAIL 0 switch / 0 background','lifecycle_api35':'FAIL 20 switch / 1 background; next HOME queued assertion','original_lifecycle_108325858061':'Historical final FAIL 20 switch /18 background, not in_progress or PASS','api29_parity':'Fresh PASS12 cases/341checks;50 full byte pairs independently equal','api35_parity':'Fresh PASS12 cases/255checks;50 full byte pairs independently equal','parity_scope':'Explicit fixture/API commands, not full touch, all events, all ports or physical visual verification','full_touch':'FAIL preview; subsequent steps NOT_REACHED','core':'input and candidate original full core exit1 logistics:75;42 invocation matrices equal,12 zero/30 nonzero each','host':'20 actual gates exit0, not green core','arm64':'NOT_RUN','memory':'No process found is missing data, never zero','video':'Every fetched original segment parsed and fully decoded; sampled content review is not full frame-by-frame acceptance','release':'Use actual publication stdout/stderr, not historical403 or GET success as current write outcome'},'raw_evidence_paths':raw,'missing_measurements':['physical ARM64 installation/rendering/GPU/PSS/thermal','full allocation/GC trace','all widget IDs/bounds for stale-date diagnosis','complete touch command/save/load chain','SAF provider faults','matched-camera V1/V2 and all unit/season visual matrices']}
write(OUT/'manifest.json',manifest)
cp=BASE/'evidence/R00-R14-remediation-checklist.json'; check=load(cp); current={r['id']:r for r in rows}
for r in check['requirements']: r['repeat_20260926']={'status':current[r['id']]['current_status'],'gap':current[r['id']]['current_gap'],'apk_source_sha':SOURCE,'apk_sha256':APK,'matrix':'repeat-20260926/matrix.json#'+r['id']}
check['latest_acceptance']={'matrix':'repeat-20260926/matrix.json','counts':counts,'total':463,'all_requirements_satisfied':False}; write(cp,check)
p=BASE/'evidence/R00-R14-REMEDIATION.json'; value=load(p); value['repeat_20260926']=manifest; value['latest_acceptance_manifest']='repeat-20260926/manifest.json'; write(p,value)
intro='\n\n## 2026-09-26 本轮独立复验：原v112，不是新生产版本\n\n**R00–R14是否全部满足原始开发要求：否。**\n\n463条：PASS9 / FAIL20 / PARTIAL174 / NOT_RUN256 / NOT_REACHED4。原213原文和历史不动，补24漏联规范段与4精确验收澄清。以 `evidence/repeat-20260926/` 为本轮结论；旧full-acceptance是702feccf历史。\n\nAPI29/API35全国预览与原NativeR12失败；API29月7/月4整屏仍1月，UI draw95/Window89冻结而3D变化。生命周期本轮为API29 0/0、API35 20/1后后台停帧断言失败；历史20/18不是20+20通过。两API新parity各12组合、50对完整存档字节一致，但只是fixture。纯触控尾链NOT_REACHED，ARM64 NOT_RUN。原core两边同一logistics:75失败且42调用矩阵相同；20HOST门禁不能替代core/设备/美术。\n\nAPK source `'+SOURCE+'` / SHA256 `'+APK+'`。生产、规则、资产不变，保留原APK与开发签名；未开始R15或合main。\n'
for rel in ['PROGRESS.md','DEFECTS.md','reports/R00-R14-CONTINUATION.md','handoffs/R00-R14-REMEDIATION.md','reports/R00-R14-P0-v112.md']:
    p=BASE/rel; s=p.read_text()
    if '## 2026-09-26 本轮独立复验：原v112，不是新生产版本' not in s: p.write_text(s+intro)
report=['# R00–R14 原始要求独立复验',intro,'## 阶段结果','','| 阶段 | 结论 | 具体差距 |','|---|---|---|']
report += [f"| {s['stage']} | {s['status']} | {s['gap']} |" for s in summary['stages']]
report += ['','## 执行和证据边界','','本轮实际重跑严格安装CI36222182595 attempt2、原CI36213760665 host attempt2及API35冷启动attempt3、兼容parity CI36222424271 API29 attempt2/API35 attempt3。必须按job started/completed时间识别实际重跑，不能把新attempt中的克隆旧job算新执行。原job108325858061最终失败，旧20/18日志保留。','','输入8b42af2a70c0fc066620e2ce79951aa31d8b923e与候选5f8997ebfef15f4680931d40532411a336506d0d在本地重新执行原完整core及全部42调用，均12 exit0/30 exit1，退出矩阵相同。没有新增core失败证据，没有删除断言或改AI冲绿。CI另有20HOST门禁通过。','','两API冷启动均在真实触点全国预览内实际执行首CPU/farLOD检查；API29 CPU177地形/211环境已交付，pending285/提交8；API35有效重试pending324/提交2。API35前一次adb root连接关闭发生在安装前，单列基础设施失败。原NativeR12两边均初始ready失败，原120s和原断言未改。','','API29月份fixture的208检查PASS仅覆盖内存/轮询。原完整PNG、独立Surface及录像显示顶部仍190年1月上旬，而authority/snapshot/widget为7月/4月，季节颜色已变。UI draw95及Window frames89冻结；Window PixelCopy无backing surface。控件实例ID/bounds及真实手动读档/跨月跨年全链仍缺，不能声称根因全部定位。','','本轮两APIparity分别341/255检查，各12组合，50对完整sg11字节逐一相同，另核对9组跨模式同检查点哈希。它是fixture/API命令，不覆盖所有真实事件和完整纯触控。触控链在预览失败后确认开局、选城出征、移动攻击、战报、下一旬及手动存读档NOT_REACHED；当前cold探针未实现全部尾链。','','## 源码、视觉、性能分开','','SceneWorkQueue/SceneAssetQueue有界、epoch取消和owner交接确实进入正式路径；doFrame在beginFrame前排空CPU结果，但GPU上传需beginFrame准入。overlayDraws不能证明3D已呈现，WAITING_FRAME也不等于绝对没像素。构造器仍同步读材质/解码atlas和rig，因此R01.I03/R06.I05继续FAIL。保护门控不能为了冲绿删掉。','','300个正式assets与APK逐字节相符；本轮保护校验通过，复制manifest破坏hash的负测试实际exit1。core/game-api/game-runtime/data/unity等保护生产树不变，APK四ABI无Unity Player，开发证书不变。源码位置及hash见source-paths，正常调用链和每条必要条件见matrix。','','独立查看PC手册07原参考、当前整屏及Surface：阶梯斜岸/方形水块与WaterVisualField矩形并集一致；V1/V2无完整同区域相机图组，城港关全LOD、各兵种动作、四季三画质仍缺。没有拿离线图/概念图/测试fit替代实际画面。原R10/R11/R13缺口不自动关闭。','','设备是API29/35 x86_64 Pixel2 1080×1920，Filament OPENGL经ANGLE/SwiftShader软件后端，不是ARM64/Adreno/Mali。墙钟、线程CPU、队列等待/背压、pending、beginFrame尝试/拒绝、提交与Surface分别记录；完整分配/GC profile、物理GPU时间/PSS/热稳定NOT_RUN。No process found不记零内存。','','原视频逐段ffprobe解析、ffmpeg -xerror完整解码和时间采样内容检查；视频可播放与功能/美术/性能通过分开，抽样不冒称逐帧全流程验证。历史坏片保留，不能由新可播放片段宣布修复。','','## 下一步顺序','','先沿ScenarioFactionPicker→MapHost→FilamentMapView.doFrame/CPU mailbox→beginFrame→loadVisible/syncObjects→Surface修全国预览，保留本轮原120s失败基线；不隐藏对象、不缩图、不换引擎。并行定位API29 Window backing/UI draw冻结；补控件身份/焦点/布局和整屏+Surface同步证据，不仅getText。随后核查HOME实际焦点/Activity门控/queued时序并修20+20生命周期，再跑晚到worker与异常资源。通过前置后执行完整纯触控尾链，再处理owner解码、R05岸线/V1/V2、R10/R11全类别、R13 SAF及编辑矩阵。ARM64和真性能单列。','','原始输入六份ZIP/JSON及嵌套包完整CRC/大小/hash校验；原213及旧435原文和历史保留。463行是规范段/复合条目，不是463个互不重叠功能。完整matrix/coverage/未完成列表/manifest与原始证据包一起交付，最终远端HEAD另行复读。Release按本轮实际stdout/stderr，不把GET成功当可写或伪造链接。pm clear仅用于隔离AVD，禁止清用户真实设备存档。']
(BASE/'reports/R00-R14-REPEAT-ACCEPTANCE.md').write_text('\n'.join(report)+'\n')
print(json.dumps(summary,ensure_ascii=False,indent=2))
