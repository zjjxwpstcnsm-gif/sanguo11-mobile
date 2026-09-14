#!/usr/bin/env python3
"""Deterministic offline importer. Checksums prove bytes, never original-game fidelity."""
import argparse, collections, hashlib, html, json, pathlib, unicodedata
ROOT = pathlib.Path(__file__).resolve().parents[2]
DATA = ROOT / 'data/content'
OUT = ROOT / 'core/src/main/resources/content'
STAT = ['統率','武勇','智謀','政治','魅力']
APT = ['槍兵適性','戟兵適性','弩兵適性','騎兵適性','兵器適性','水軍適性']

def unique(pairs):
    result = {}
    for k,v in pairs:
        if k in result: raise ValueError('duplicate field: '+k)
        result[k] = v
    return result

def read(name): return json.loads((DATA/name).read_text(), object_pairs_hook=unique)
def sha(b): return hashlib.sha256(b).hexdigest()
def require(ok,msg):
    if not ok: raise ValueError(msg)
def ids(rows,key):
    values=[r[key] for r in rows]
    require(len(values)==len(set(values)), 'duplicate '+key)
    return set(values)
def number(v,lo,hi):
    require(str(v).strip()!='', 'missing numeric value')
    n=int(v);require(lo<=n<=hi, 'out of range: '+str(v));return n

def table(header,rows):
    lines=['\t'.join(header)]
    for row in rows:
        cells=['?' if x is None else str(x) for x in row]
        require(all('\t' not in x and '\n' not in x and '\r' not in x for x in cells),'invalid TSV cell')
        lines.append('\t'.join(cells))
    return ('\n'.join(lines)+'\n').encode()

def build():
    m=read('manifest.json');sources=ids(m['sources'],'id')
    require(m['format']==1 and m['revision']>0,'unknown manifest version')
    for s in m['sources']:require(s['url'].startswith('https://') and s['version'] and s['status'] and s['retrieved'],'missing source metadata')
    raw=read('officers-source.json');ids(raw['rows'],'sourceId');ids(raw['rows'],'projectId')
    require(raw['source'] in sources,'officer source absent')
    officers=[r for r in raw['rows'] if 0<=r['sourceId']<670]
    require({r['sourceId'] for r in officers}==set(range(670)),'officer count/ID coverage differs')
    skills=read('skills-source.json');items=read('items-source.json');sites=read('sites-source.json');scenarios=read('scenarios-source.json')
    for rows in [skills,items,sites,scenarios]:
        ids(rows,'id');ids(rows,'sourceId')
        for r in rows:require(r['source'] in sources,'invalid source FK')
    require(len(skills)==100 and len(items)==43,'catalog count differs')
    require(collections.Counter(s['kind'] for s in sites)=={'city':42,'gate':10,'port':35},'site count/types differ')
    ids(skills,'name');skill_names={r['name']:r['id'] for r in skills}
    require(len({(s['rawX'],s['rawY']) for s in sites})==len(sites),'overlapping source coordinates')
    for s in sites:
        number(s['rawX'],0,199);number(s['rawY'],0,199);number(s['durability'],1,100000)
        require(s['coordinateStatus'] in ['unknown','collected','cross-checked'],'unknown coordinate status')
    byid={o['projectId']:o for o in officers};bridge=read('legacy-bridge.json');ids(bridge,'projectId');ids(bridge,'sourceId')
    ids(read('cross-checks.json'),'projectId')
    checks={r['projectId']:r for r in read('cross-checks.json')}
    require(set(checks)<=set(byid),'cross-check FK')
    for b in bridge:
        r=byid[b['projectId']];require(r['sourceId']==b['sourceId'] and r['name']==b['expectedName'],'stable ID bridge changed')
    report={'target':m['target'],'revision':m['revision'],'originalInstallationVerified':False,'counts':{'officersCollected':len(officers),'officersExcluded':len(raw['rows'])-len(officers),'sites':len(sites),'skills':len(skills),'items':len(items),'scenarioMetadata':len(scenarios),'officialPlayableScenarios':0,'originalTerrainCells':0},'crossCheckedOfficerIds':sorted(checks),'uncertainRelations':[],'duplicateItemNames':{},'errors':[], 'gaps':['目标 1.1 原版安装数据哈希未知','全国逐格地形、道路、水系、岸线、开发地及连接关系未取得','关港原始坐标疑似错配，禁止投影进游戏；42城坐标仍待原版核验','全部历史/假想开局缺少完整状态；均不可选为官方剧本','特技/宝物/事件/生卒/关系未接入运行时；不覆盖 Agent 1 的状态及存档']}
    namecounts=collections.Counter(o['name'] for o in officers)
    report['sameNameOfficers']=[n for n,c in namecounts.items() if c>1]
    orows=[]
    for o in sorted(officers,key=lambda o:o['projectId']):
        v=o['values'];require(set(v)==set(raw['headers']),'unknown/missing raw officer field')
        stats=[number(v[k],0,100) for k in STAT]
        require(sum(stats)==number(v['總和'],0,500),'ability sum differs: '+o['name'])
        apt=''.join(unicodedata.normalize('NFKC',v[k]) for k in APT)
        require(len(apt)==6 and all(a in 'CBAS' for a in apt),'unknown aptitude')
        birth=number(v['出生年'],1,9999);death=number(v['死亡年'],birth,9999);appear=number(v['登場年'],birth,9999)
        skill=v['特技'];require(not skill or skill in skill_names,'invalid skill reference: '+skill)
        relation=[]
        for k in ['父親','母親','配偶','義兄']+[k for k in raw['headers'] if k.startswith('親近武將') or k.startswith('厭惡武將')]:
            if v[k]:
                relation.append(k+'='+v[k]);report['uncertainRelations'].append({'officerId':o['projectId'],'field':k,'raw':v[k],'targetId':None,'status':'unresolved-source-name'})
        state='collected'
        if o['projectId'] in checks:
            check=checks[o['projectId']];require(stats==check['stats'] and apt==check['aptitudes'],'cross-source difference: '+o['name']);state='cross-checked'
        orows.append([o['projectId'],o['sourceId'],o['name'],','.join(map(str,stats)),apt,birth,death,appear,skill_names.get(skill,'none'),state,'rlu-officers','；'.join(relation) or '原表空白',v['性格'],v['性別']])
    for item in items:
        number(item['value'],0,100)
        require(item['scenarioId'] is None,'unverified item scenario binding')
    for name,count in collections.Counter(r['name'] for r in items).items():
        if count>1:report['duplicateItemNames'][name]=[r['id'] for r in items if r['name']==name]
    for s in scenarios:
        require(not s['playable'] and s['status']=='incomplete','unverified scenario cannot be playable')
        number(s['year'],1,9999);number(s['month'],1,12);require(s['kind'] in ['historical','fictional'],'unknown scenario kind')
    generated={}
    generated[OUT/'officers.tsv']=table(['id','sourceId','name','stats','aptitudes','birth','death','appearance','skillId','status','source','relationsRaw','personality','gender'],orows)
    generated[OUT/'sites.tsv']=table(['id','name','kind','rawX','rawY','durability','coordinateStatus','source'],[[s[k] for k in ['id','name','kind','rawX','rawY','durability','coordinateStatus','source']] for s in sorted(sites,key=lambda s:s['id'])])
    generated[OUT/'skills.tsv']=table(['id','name','source'],[[s[k] for k in ['id','name','source']] for s in sorted(skills,key=lambda s:s['id'])])
    generated[OUT/'items.tsv']=table(['id','name','kind','value','holderRaw','locationRaw','scenarioId','source'],[[s['id'],s['name'],s['kind'],s['value'],s['rawHolder'] or '原表空白',s['rawLocation'] or '原表空白',None,s['source']] for s in sorted(items,key=lambda s:s['id'])])
    generated[OUT/'scenarios.tsv']=table(['id','name','year','month','kind','status','source'],[[s[k] for k in ['id','name','year','month','kind','status','source']] for s in scenarios])
    generated[OUT/'sources.tsv']=table(['id','url','version','status'],[[s[k] for k in ['id','url','version','status']] for s in m['sources']])
    generated[OUT/'manifest.tsv']=table(['format','revision','target','originalInstallationVerified'],[[1,m['revision'],m['target'],'false']])
    # Source coordinates deliberately stay in their own coordinate space, no modern geolocation/axial guesses.
    svg=['<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="1320" viewBox="0 0 1200 1320"><rect width="1200" height="1320" fill="#142d31"/><g fill="#efdfb8" font-family="sans-serif"><text x="40" y="42" font-size="24">全国据点坐标资料预览（非原版地形地图）</text><text x="40" y="76" font-size="17">原表 X/Y 原样绘制；42城待核验，45关港坐标隔离；未转换为六角格</text>']
    for s in sites:
        if s['kind']!='city':continue
        x=70+s['rawX']*5.3;y=110+s['rawY']*5.6
        svg.append(f'<circle cx="{x}" cy="{y}" r="4" fill="#8bd4b3"/><text x="{x+7}" y="{y+5}" font-size="13">{html.escape(s["name"])} ({s["rawX"]},{s["rawY"]})</text>')
    svg.append('</g></svg>');generated[ROOT/'docs/content/site-coordinate-preview.svg']=''.join(svg).encode()
    # Same original regional fixture, with explicit sourced base abilities/aptitudes. No invented historical opening.
    base=(ROOT/'core/src/main/resources/scenarios/regional-sandbox.properties').read_text()
    lines=[];diff=[];simple={}
    for l in base.splitlines():
        if l.startswith('id='):l='id=officer-reference-drill'
        elif l.startswith('name='):l='name=武将资料演练'
        elif l.startswith('source='):l='source=community-reference'
        elif l.startswith('revision='):l='revision=1'
        elif l.startswith('talents=') or l.startswith('talent.'):continue
        elif l.startswith('officer.'):
            key,value=l.split('=',1);c=value.split('|');oid=int(c[0]);o=byid[oid];values=[str(o['values'][k]) for k in STAT];simple[oid]=c[1]
            diff.append({'officerId':oid,'name':c[1],'oldStats':list(map(int,c[4:])),'newStats':list(map(int,values)),'sourceId':o['sourceId'],'aptitudes':next(r[4] for r in orows if r[0]==oid)})
            l=key+'='+'|'.join(c[:4]+values)
        lines.append(l)
    lines[0]='# Original regional battlefield with community-sourced base stats; NOT an official historical opening.'
    lines.extend(['reference=rlu-officers','aptitudes='+str(len(bridge))])
    for i,b in enumerate(bridge):
        o=byid[b['projectId']];apt=''.join(unicodedata.normalize('NFKC',o['values'][k]) for k in APT)
        lines.append('aptitude.'+str(i)+'='+str(b['projectId'])+'|'+'|'.join(str('CBAS'.index(a)) for a in apt))
    generated[ROOT/'core/src/main/resources/scenarios/officer-reference-drill.properties']=('\n'.join(lines)+'\n').encode()
    generated[OUT/'aliases.tsv']=table(['id','alias'],sorted(simple.items()))
    report['runtimeOfficerDiff']=diff
    report['sourceFileHashes']={p.name:sha(p.read_bytes()) for p in sorted(DATA.glob('*.json'))}
    generated[ROOT/'docs/content/verification-report.json']=(json.dumps(report,ensure_ascii=False,indent=2)+'\n').encode()
    index=['# resource SHA-256; format 1; generated offline']+[p.name+' '+sha(b) for p,b in sorted(generated.items()) if p.parent==OUT]
    generated[OUT/'index.txt']=('\n'.join(index)+'\n').encode()
    # Preserve old pack hashes and order byte-for-byte.
    path=ROOT/'core/src/main/resources/scenarios/index.txt'
    current=[l for l in path.read_text().splitlines() if not l.startswith('officer-reference-drill ')]
    generated[path]=('\n'.join(current)+'\nofficer-reference-drill '+sha(generated[ROOT/'core/src/main/resources/scenarios/officer-reference-drill.properties'])+'\n').encode()
    return generated,report

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');args=parser.parse_args()
    generated,report=build()
    for path,value in generated.items():
        if args.check:require(path.exists() and path.read_bytes()==value,'stale generated output: '+str(path.relative_to(ROOT)))
        else:path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(value)
    print(json.dumps({'mode':'check' if args.check else 'build','files':len(generated),'counts':report['counts'],'unresolvedRelations':len(report['uncertainRelations'])},ensure_ascii=False))
if __name__=='__main__':main()
