#!/usr/bin/env python3
"""Playable authored historical reconstructions, NOT extracted official save data.
Explicit major rosters, rulers and territories. Unresolved people remain unaffiliated;
future people obey the source appearance dates. 207/250 are user-requested variants.
"""
from pathlib import Path
import hashlib,json,argparse
from build_mobile_scenarios import rows, cities, officers, aliases, SCENARIOS, ROOT
from national_geography import generate_geography
from strategic_sites import layout

# Names below use the pinned catalog spelling. One entry is one independently playable force.
ROSTERS={
'劉備':'劉備 關羽 張飛 簡雍 孫乾 糜竺 糜芳',
'曹操':'曹操 夏侯惇 夏侯淵 曹仁 曹洪 樂進 李典 荀彧 荀攸 程昱 郭嘉 典韋 許褚 于禁 滿寵',
'孫堅':'孫堅 孫策 孫權 黃蓋 程普 韓當 祖茂 朱治',
'孫策':'孫策 孫權 周瑜 黃蓋 程普 韓當 朱治 呂範 孫靜 周泰 蔣欽 陳武 凌操',
'孫權':'孫權 周瑜 魯肅 呂蒙 甘寧 黃蓋 程普 韓當 朱治 呂範 諸葛瑾 陸遜 周泰 蔣欽 陳武 凌統 徐盛 丁奉 孫尚香 大喬 小喬',
'袁紹':'袁紹 顏良 文醜 田豐 沮授 審配 逢紀 郭圖 許攸 袁譚 袁熙 袁尚 高幹 淳于瓊 張郃 高覽',
'袁術':'袁術 紀靈 張勳 橋蕤 閻象 楊弘 袁胤 韓胤 陳蘭 雷薄',
'董卓':'董卓 呂布 李儒 華雄 李傕 郭汜 張濟 樊稠 牛輔 胡軫 徐榮 董旻 李肅 貂蟬',
'呂布':'呂布 陳宮 高順 張遼 臧霸 侯成 宋憲 魏續 曹性 貂蟬',
'李傕':'李傕 郭汜 樊稠 張濟 賈詡',
'劉表':'劉表 蔡瑁 蒯良 蒯越 黃祖 劉琦 劉琮 文聘 王粲 韓嵩 伊籍',
'劉焉':'劉焉 劉璋 張任 嚴顏 黃權 劉璝 冷苞 鄧賢 吳懿 吳班 王累 張松',
'劉璋':'劉璋 張任 嚴顏 黃權 劉璝 冷苞 鄧賢 吳懿 吳班 王累 張松 法正 孟達 李恢 許靖',
'馬騰':'馬騰 馬超 馬岱 馬鐵 馬休 龐德',
'韓遂':'韓遂 成公英 閻行 程銀 侯選 李堪 張橫 梁興 楊秋 馬玩 成宜',
'公孫瓚':'公孫瓚 公孫越 公孫範 公孫續 田楷 嚴綱 單經 鄒丹',
'公孫度':'公孫度 公孫康 公孫恭',
'公孫康':'公孫康 公孫恭',
'孔融':'孔融 武安國 王修',
'陶謙':'陶謙 陳登 陳珪 曹豹 笮融',
'劉虞':'劉虞 魏攸',
'韓馥':'韓馥 潘鳳',
'張楊':'張楊 眭固 楊醜',
'劉岱':'劉岱 鮑信 王匡 橋瑁',
'孔伷':'孔伷',
'張魯':'張魯 張衛 楊松 楊柏 楊任 閻圃',
'張繡':'張繡 胡車兒 賈詡',
'張角':'張角 張寶 張梁 張燕 張曼成 波才 管亥 程遠志 鄧茂 韓忠 趙弘 孫仲',
'何進':'何進 盧植 皇甫嵩 朱雋 王允 袁紹 袁術 曹操 荀彧 荀攸 鮑信',
'丁原':'丁原 呂布 張遼',
'張燕':'張燕 眭固',
'劉繇':'劉繇 太史慈 張英 樊能 陳橫',
'王朗':'王朗 虞翻 周昕',
'嚴白虎':'嚴白虎 嚴輿',
'劉度':'劉度 劉賢 邢道榮',
'趙範':'趙範 陳應 鮑隆',
'金旋':'金旋 金禕',
'韓玄':'韓玄 黃忠 魏延',
'孟獲':'孟獲 祝融 孟優 帶來洞主 木鹿大王 兀突骨 朵思大王 阿會喃 董荼那 金環三結',
'司馬懿':'司馬懿 司馬師 司馬昭 司馬炎 鄧艾 鍾會 賈充 陳泰 郭淮 胡奮 王基 王濬 羊祜 杜預',
}
# key/title/year/month/[(ruler, cities)]. All layouts explicitly authored, not sold as exact PK tables.
CONFIGS=[
('huangjin-184','184 黄巾之乱 · 重建',184,1,[
('何進','洛陽 長安 宛 許昌 陳留 壽春 下邳 小沛 江陵 襄陽 新野 上庸'),('張角','鄴 南皮 平原 濮陽 汝南 北海'),('董卓','天水 安定'),('丁原','晉陽'),('韓遂','武威'),('劉焉','薊 北平 襄平'),('孫堅','長沙')]),
('coalition-190','190 讨伐董卓 · 重建',190,1,[
('劉備','平原'),('曹操','陳留'),('孫堅','長沙'),('袁紹','南皮'),('袁術','宛'),('董卓','洛陽 長安 上庸 天水 安定'),('公孫瓚','北平'),('劉虞','薊'),('公孫度','襄平'),('韓馥','鄴'),('張楊','晉陽'),('孔融','北海'),('陶謙','下邳 小沛'),('劉岱','濮陽'),('孔伷','汝南'),('劉表','襄陽 江陵 江夏'),('劉焉','成都 梓潼 江州 永安 漢中'),('馬騰','武威')]),
('warlords-194','194 群雄割据 · 重建',194,6,[
('劉備','下邳 小沛'),('曹操','陳留 許昌'),('孫策','廬江'),('袁紹','鄴 南皮 平原'),('袁術','壽春 汝南'),('呂布','濮陽'),('李傕','長安 洛陽 上庸'),('公孫瓚','北平 薊'),('公孫度','襄平'),('張楊','晉陽'),('孔融','北海'),('劉表','襄陽 江陵 江夏 新野'),('劉璋','成都 梓潼 江州 永安'),('張魯','漢中'),('馬騰','武威 天水 安定'),('張繡','宛'),('劉繇','建業'),('嚴白虎','吳'),('王朗','會稽')]),
('guandu-200','200 官渡之战 · 重建',200,1,[
('劉備','下邳 小沛'),('曹操','許昌 陳留 濮陽 北海 洛陽 長安 宛 汝南 壽春 上庸'),('孫策','建業 吳 會稽 廬江 柴桑'),('袁紹','鄴 南皮 平原 北平 薊 晉陽'),('公孫度','襄平'),('劉表','襄陽 江陵 江夏 新野 長沙 武陵 桂陽 零陵'),('劉璋','成都 梓潼 江州 永安 建寧 雲南'),('張魯','漢中'),('馬騰','武威 天水 安定')]),
('chibi-207','207 赤壁前夜 · 定制',207,9,[
('劉備','新野'),('曹操','許昌 陳留 濮陽 北海 洛陽 長安 宛 汝南 壽春 上庸 鄴 南皮 平原 北平 薊 晉陽 下邳 小沛'),('孫權','建業 吳 會稽 廬江 柴桑'),('公孫康','襄平'),('劉表','襄陽 江陵 江夏'),('劉璋','成都 梓潼 江州 永安 建寧 雲南'),('張魯','漢中'),('馬騰','武威 天水 安定'),('韓玄','長沙'),('金旋','武陵'),('趙範','桂陽'),('劉度','零陵')]),
('heroes-250','250 幻想群雄争霸 · 定制',250,1,[
('劉備','成都'),('曹操','許昌'),('孫權','建業 吳'),('袁紹','鄴 南皮'),('袁術','壽春'),('董卓','長安'),('呂布','下邳 小沛'),('劉表','襄陽 江夏'),('劉璋','梓潼 江州'),('馬騰','武威 天水'),('韓遂','安定'),('公孫瓚','北平 薊'),('公孫度','襄平'),('孔融','北海'),('陶謙','廬江'),('張魯','漢中'),('張繡','宛'),('張角','平原'),('何進','洛陽'),('張燕','晉陽'),('劉繇','柴桑'),('王朗','會稽'),('孟獲','建寧 雲南'),('司馬懿','上庸'),('劉度','零陵'),('趙範','桂陽'),('金旋','武陵'),('韓玄','長沙')])]

BY_NAME={}
for o in officers:BY_NAME.setdefault(o['name'],[]).append(o)
CITY_NAME={c['name']:c for c in cities}
# Resolve names strictly; never silently bind homonymous generals.
def person(name):
    found=BY_NAME.get(name,[])
    assert len(found)==1,(name,'missing/ambiguous',len(found))
    return found[0]

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');args=parser.parse_args()
    terrain,parcels=generate_geography(cities)
    extras=layout(terrain,{c['id']:(int(c['rawX']),int(c['rawY'])) for c in cities})
    site_data={int(c['id']):c for c in rows('sites.tsv')}
    entries=[];report=[]
    for key,title,year,month,forces in CONFIGS:
        fantasy=year==250
        owner={int(c['id']):-1 for c in cities};capital={};teams={};names={name:i for i,(name,_) in enumerate(forces)}
        for side,(leader,territory) in enumerate(forces):
            homes=[int(CITY_NAME[n]['id']) for n in territory.split()];capital[side]=homes[0]
            for city in homes:assert owner[city]==-1;owner[city]=side
            teams[side]=[leader]+[n for n in ROSTERS[leader].split() if n!=leader]
        # Year-specific transfers precede static affinity lists.
        transfers={}
        def to(leader,ns):
            if leader in names:
                for n in ns.split():transfers[n]=names[leader]
        if year==184:
            to('劉焉','劉備 關羽 張飛 簡雍 公孫瓚')
            to('丁原','呂布 張遼');to('何進','劉表 劉璋')
        if year==190:
            to('曹操','夏侯惇 夏侯淵 曹仁 曹洪 樂進 李典')
            to('董卓','張遼 賈詡 張繡');to('韓馥','張郃 沮授')
        if year==194:
            to('孫策','周瑜');to('劉備','糜竺 糜芳 孫乾 陳登 陳珪')
        if year==200:
            to('曹操','張遼 徐晃 賈詡 張繡 臧霸');to('袁紹','張郃 高覽 趙雲');to('孫策','太史慈 魯肅')
        if year==207:
            to('曹操','張遼 徐晃 賈詡 張郃 高覽 臧霸 司馬懿')
            to('劉備','趙雲 徐庶 周倉 關平')
        # Fantasy keeps every explicit ruler even if listed as somebody else's subordinate.
        leaders={n:i for i,(n,_) in enumerate(forces)}
        assignments={}
        for side,ns in teams.items():
            for name in ns:
                person(name)
                if name not in assignments:assignments[name]=side
        assignments.update(transfers);assignments.update(leaders)
        if year==190:
            for name in '荀彧 荀攸 程昱 郭嘉 典韋 許褚 滿寵'.split():assignments.pop(name,None)
        if year==194:
            for name in '郭嘉 許褚'.split():assignments.pop(name,None)
        selected=[o for o in officers if fantasy or int(o['death'])>=year]
        playable=[];counts={i:0 for i in names.values()}
        for o in selected:
            present=fantasy or int(o['appearance'])<=year
            side=assignments.get(o['name'],-1) if present else -1
            if side>=0:
                homes=[c for c,s in owner.items() if s==side];home=capital[side] if o['name'] in leaders else homes[counts[side]%len(homes)];counts[side]+=1
            else:
                known={'諸葛亮':'襄陽','龐統':'襄陽','徐庶':'新野','黃忠':'長沙','魏延':'長沙','司馬懿':'洛陽','姜維':'天水','趙雲':'薊','魯肅':'廬江','甘寧':'江夏','太史慈':'北海'}
                home=int(CITY_NAME[known[o['name']]]['id']) if o['name'] in known else 20000+int(o['sourceId'])%42
            playable.append((o,side,home))
        for n,side in leaders.items():assert any(o['name']==n and s==side for o,s,_ in playable),(key,'leader date invalid',n)
        lines=['# Authored reconstruction; not a verified official opening. See docs/NAVIGATION_DEFENSE_V0_32.md.',
               'format=1',f'id={key}',f'name={title}','source=community-reference','reference=rlu-officers','reference-details=1',f'reference-dates={0 if fantasy else 1}','natural-deaths=0','revision=1',f'year={year}',f'month={month}','coordinates=odd-r','width=200','height=200',f'factions={len(forces)}']
        lines += [f'faction.{i}={name}军' for i,(name,_) in enumerate(forces)]
        lines += [f'terrain.{y}={"".join(row)}' for y,row in enumerate(terrain)]
        lines += ['cities=87']
        for i,c in enumerate(cities):
            side=owner[int(c['id'])];troops=24000 if side>=0 else 0
            if c['name'] in ('鄴','洛陽','長安') and side>=0:troops=32000
            lines += [f'city.{i}={c["id"]}|{c["name"]}|{c["rawX"]}|{c["rawY"]}|{side}|{15000 if side>=0 else 1000}|{90000 if side>=0 else 6000}|{troops}|95|90|{c["durability"]}|20000|20000|20000|20000']
        for i,(sid,parent,x,y,kind) in enumerate(extras,42):
            c=site_data[sid];name=c['name']+('港' if kind=='PORT' else '' if c['name']=='劍閣' else '關');side=owner[parent]
            lines += [f'city.{i}={sid}|{name}|{x}|{y}|{side}|1500|15000|{3000 if side>=0 else 0}|90|80|{c["durability"]}|3000|3000|3000|3000']
        lines += ['site-kinds=45']+[f'site-kind.{i}={sid}|{kind}|{site_data[sid]["durability"]}' for i,(sid,parent,x,y,kind) in enumerate(extras)]
        plots=[(cid,x,y) for cid,points in parcels.items() for x,y in points]
        lines += [f'development-plots={len(plots)}']+[f'development-plot.{i}={cid}|{x}|{y}' for i,(cid,x,y) in enumerate(plots)]
        lines += ['arsenals=42']+[f'arsenal.{i}={c["id"]}|2|2|0|0|2|0' for i,c in enumerate(cities)]
        lines += [f'officers={len(playable)}']+[f'officer.{i}={o["id"]}|{aliases.get(o["id"],o["name"])}|{side}|{home}|{o["stats"].replace(",","|")}' for i,(o,side,home) in enumerate(playable)]
        lines += [f'aptitudes={len(playable)}']+[f'aptitude.{i}={o["id"]}|'+ '|'.join(str('CBAS'.index(c)) for c in o['aptitudes']) for i,(o,_,_) in enumerate(playable)]
        lines += [f'rulers={len(forces)}']+[f'ruler.{i}={i}|{person(n)["id"]}' for i,(n,_) in enumerate(forces)]
        treaties=[]
        if year in (184,190):
            excluded=names['張角' if year==184 else '董卓'];coalition=[i for i in names.values() if i!=excluded]
            treaties=[(a,b,36) for a in coalition for b in coalition if a<b]
        elif year==194:treaties=[(names['孫策'],names['袁術'],24)]
        elif year==200:treaties=[(names['劉備'],names['袁紹'],12)]
        elif year==207:treaties=[(names['劉備'],names['劉表'],24)]
        lines += [f'initial-treaties={len(treaties)}']+[f'initial-treaty.{i}={a}|{b}|ALLIANCE|{turns}' for i,(a,b,turns) in enumerate(treaties)]
        out='\n'.join(lines)+'\n';path=SCENARIOS/(key+'.properties')
        if args.check:assert path.read_text()==out,key
        else:path.write_text(out)
        entries.append(key+' '+hashlib.sha256(out.encode()).hexdigest())
        report.append({'id':key,'year':year,'factions':len(forces),'sites':87,'officers':len(playable),'serving':sum(s>=0 for _,s,_ in playable),'rosters':{n:[o['name'] for o,s,_ in playable if s==i] for i,(n,_) in enumerate(forces)}})
    index=SCENARIOS/'index.txt';ids={c[0] for c in CONFIGS}
    old=[l for l in index.read_text().splitlines() if l.split()[0] not in ids]
    out='\n'.join(entries+old)+'\n'
    if args.check:assert index.read_text()==out
    else:index.write_text(out)
    report_path=ROOT/'data/historical-openings.json'
    if not args.check:report_path.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print([(r['id'],r['factions'],r['officers'],r['serving']) for r in report])
if __name__=='__main__':main()
