#!/usr/bin/env python3
"""One-shot checked source integration. The workflow commits the resulting Java files, then removes this script."""
from pathlib import Path
import re
CORE=Path('core/src/main/java/game/sanguo/core')
APP=Path('app/src/main/java/game/sanguo/mobile')
def replace(path,old,new,count=1):
    s=path.read_text()
    if s.count(old)!=count: raise RuntimeError(f'{path}: expected {count} anchors, got {s.count(old)}: {old[:100]}')
    path.write_text(s.replace(old,new))

def method_region(s,anchor):
    start=s.index(anchor); brace=s.index('{',start); depth=1;i=brace+1; string=None;escape=False
    while depth:
        ch=s[i]
        if string:
            if escape:escape=False
            elif ch=='\\':escape=True
            elif ch==string:string=None
        elif ch in ('"',"'"):string=ch
        elif ch=='{':depth+=1
        elif ch=='}':depth-=1
        i+=1
    return start,brace,i

p=CORE/'World.java'
replace(p,'public final List<String> log=new ArrayList<>();','public final List<String> log=new ArrayList<>();\n    public final BattleReports reports=new BattleReports(this);')
replace(p,'void visualAction(TurnJournal.Kind kind,int actor,Hex target,String label){actionLabel=label;','void visualAction(TurnJournal.Kind kind,int actor,Hex target,String label){reports.action(kind,actor,target,label);actionLabel=label;')
replace(p,'public void note(String text) { log.add(text);while(log.size()>40)log.remove(0); }','public void note(String text) { reports.note(text);log.add(text);while(log.size()>40)log.remove(0); }')
replace(p,'if(turnJournal!=null)turnJournal.checkpoint(r.message);return r;','if(turnJournal!=null)turnJournal.checkpoint(r.message);reports.clearAction();return r;')
replace(p,'Result fail(String text) { if(turnJournal!=null)turnJournal.cancel();return result(false,text); }','Result fail(String text) { if(turnJournal!=null)turnJournal.cancel();reports.clearAction();return result(false,text); }')
replace(p,'List<Integer> sides=new ArrayList<>();for(int offset=1;offset<factions.length;offset++)','reports.beginTurn();try {\n        List<Integer> sides=new ArrayList<>();for(int offset=1;offset<factions.length;offset++)')
replace(p,'progress.accept(new TurnProgress(player,total,total,"结算完成",true));return success(date()+" · 行动力恢复");','progress.accept(new TurnProgress(player,total,total,"结算完成",true));return success(date()+" · 行动力恢复");\n        } finally { reports.endTurn(); }')
replace(p,'active=player;progress.accept(new TurnProgress(player,completed+1,total,"恢复行动与自动行军"));','reports.nextPlayer();active=player;progress.accept(new TurnProgress(player,completed+1,total,"恢复行动与自动行军"));')
replace(p,'turn++;contests.tick();','reports.globalPhase();turn++;contests.tick();')
s=p.read_text();start,brace,end=method_region(s,'private void settleGlobalTurn(');part=s[start:end]
part=re.sub(r'\b(contests|domestic|campaign|army|abilities|recruitment|envoys|strategy|war|cityDefense|government|treasures|events|life|diplomacy)\.tick\(\);',lambda m:m.group(0)+'reports.checkpoint("'+{'contests':'对局','domestic':'建设运输','campaign':'技巧研究','army':'军备与持续伤害','abilities':'能力研究','recruitment':'登用结果','envoys':'外交任务','strategy':'人员内政','war':'火场设施','cityDefense':'据点守备','government':'武将任职','treasures':'宝物发现','events':'世界事件','life':'武将生涯','diplomacy':'外交变化'}[m.group(1)]+'结算");',part)
part=part.replace('if(u.troops<=0)removeUnit(u);','if(u.troops<=0)removeUnit(u);reports.checkpoint("部队兵粮消耗");')
part=part.replace('int consumption=cityFoodUse(c)-domestic.arrivalFoodCredit(c);','int reportFoodBefore=c.food;\n            int consumption=cityFoodUse(c)-domestic.arrivalFoodCredit(c);')
old='c.gold+=Math.min(Math.max(0,campaign.goldCap(c)-c.gold),domestic.goldIncome(c.id,turn));c.food+=Math.min(Math.max(0,campaign.foodCap(c)-c.food),domestic.foodIncome(c.id,turn));'
assert part.count(old)==1
part=part.replace(old,'int reportFoodUse=reportFoodBefore-c.food;\n            int goldIncome=Math.min(Math.max(0,campaign.goldCap(c)-c.gold),domestic.goldIncome(c.id,turn));int foodIncome=Math.min(Math.max(0,campaign.foodCap(c)-c.food),domestic.foodIncome(c.id,turn));\n            c.gold+=goldIncome;c.food+=foodIncome;')
part=part.replace('c.defense+=cityDefense.recovery(c);','c.defense+=cityDefense.recovery(c);\n            reports.note(c.name+"本旬收支：金收入+"+goldIncome+"，粮收入+"+foodIncome+"，驻军实际粮耗"+reportFoodUse);')
s=s[:start]+part+s[end:];p.write_text(s)
# Prime at authoritative public command boundaries, including the first headless command.
changed=[]
for file in CORE.glob('*.java'):
    if file.name=='BattleReports.java':continue
    s=file.read_text();prefix='reports.prepare();' if file.name=='World.java' else 'w.reports.prepare();'
    pattern=r'(public\s+(?:synchronized\s+)?(?:World\.)?Result\s+\w+\s*\([^{}]*\)\s*\{)'
    if file.name!='World.java':pattern=r'(public\s+(?:synchronized\s+)?World\.Result\s+\w+\s*\([^{}]*\)\s*\{)'
    s,n=re.subn(pattern,lambda m:m.group(0)+prefix,s)
    s,nf=re.subn(r'if\(w\.turnJournal!=null\)w\.turnJournal\.facility\(([^;]+)\);',lambda m:'w.reports.facility('+m.group(1)+');'+m.group(0),s)
    if n or nf:file.write_text(s);changed.append((file.name,n,nf))
print('Integrated command boundaries:',changed)
p=CORE/'Army.java'
old='Hex exit=null;for(Hex h:c.hex.neighbors())if((w.fieldworks.landCost(h,weapon,c.owner)>0||c.kind==World.SiteKind.PORT&&water(h))&&w.unitAt(h)==null&&w.cityAt(h)==null&&w.domestic.at(h)==null&&w.war.at(h)==null&&w.war.fireAt(h)==null){exit=h;break;}'
replace(p,old,'Hex exit=deploymentExit(c,weapon);')
insert='''    /** Read-only exit selection shared by preview and the real deployment command. */
    public Hex deploymentExit(World.City c,World.Weapon weapon){
        if(c==null||weapon==null)return null;
        for(Hex h:c.hex.neighbors())if((w.fieldworks.landCost(h,weapon,c.owner)>0||c.kind==World.SiteKind.PORT&&water(h))&&w.unitAt(h)==null&&w.cityAt(h)==null&&w.domestic.at(h)==null&&w.war.at(h)==null&&w.war.fireAt(h)==null)return h;
        return null;
    }
    public World.Unit deploymentPreview(World.City c,int commander,int[] deputies,World.Weapon weapon,Ship ship,int troops,int food,int gold){
        if(w.officer(commander)==null||deputies==null||ship==null)return null;
        for(int id:deputies)if(w.officer(id)==null)return null;
        Hex exit=deploymentExit(c,weapon);if(exit==null)return null;
        World.Unit u=new World.Unit(-1,c.owner,commander,weapon,exit,troops,food);u.gold=gold;u.deputies=deputies.clone();u.ship=ship;u.energy=c.morale;return u;
    }
'''
replace(p,'    private boolean completed(int city,Domestic.Kind kind)',insert+'    private boolean completed(int city,Domestic.Kind kind)')
p=CORE/'SaveCodec.java'
replace(p,'VERSION=28, MAX_BYTES=4*1024*1024','VERSION=29, MAX_BYTES=32*1024*1024')
replace(p,'d.writeInt(w.log.size());for(String line:w.log)d.writeUTF(line);','d.writeInt(w.log.size());for(String line:w.log)d.writeUTF(line);\n        w.reports.write(d);')
replace(p,'if(d.available()!=0)throw new IOException("存档存在未知尾部数据");','if(version>=29)w.reports.read(d);else w.reports.rebase();\n        if(d.available()!=0)throw new IOException("存档存在未知尾部数据");')
p=APP/'MainActivity.java'
replace(p,'private void buildGameUi(Bundle state,boolean restored,boolean coldStart){','private void buildGameUi(Bundle state,boolean restored,boolean coldStart){\n        world.reports.prepare();')
replace(p,'territoryToggle=CompactButtons.create(this);territoryToggle.setText("势力");','Button reportsButton=button("战报",v->new BattleReportUi(this,world).show());reportsButton.setTag("reports.entry");reportsButton.setContentDescription("战报中心：近三个月全部势力交互结果");header.addView(reportsButton,new LinearLayout.LayoutParams(dp(52),dp(48)));\n        territoryToggle=CompactButtons.create(this);territoryToggle.setText("势力");')
s=p.read_text();start,brace,end=method_region(s,'private void showTurnReport(');s=s[:brace+1]+'new BattleReportUi(this,world).show(Math.max(0,world.turn-1));'+s[end-1:];p.write_text(s)
# Keep report capture linear in roster size even with many field units.
p=CORE/'BattleReports.java'
replace(p,'World.Unit u=w.unit(o.unitId);World.City c=w.city(o.cityId);','World.Unit u=o.unitId<0?null:w.unit(o.unitId);World.City c=o.cityId<0?null:w.city(o.cityId);')
replace(p,'append(eventTurn(),-1,c.related,c.kind,label+" · "+c.name,c.text,c.location);','append(eventTurn(),-1,c.related,classify(label)==Kind.OTHER?c.kind:classify(label),label+" · "+c.name,c.text,c.location);')
p=Path('version.properties');s=p.read_text();s=re.sub(r'(?m)^versionName=.*$','versionName=0.53.0',s);s=re.sub(r'(?m)^versionCode=.*$','versionCode=53',s);p.write_text(s)
notes='''## v0.53.0 · 战报中心与三页签出征

基于完整v0.52.0 `63a3d5e3f946145fb8b5e4a52b29d5d3cccaf140`。主界面常驻战报入口；按游戏旬保存当前旬和前8旬，不按日志条数挤出历史。结算结果、实体属性变化、敌我关联与事件地点从正式世界记录，独立于动画与视野，支持关键词/旬/类型/我方相关/主动/受影响筛选。存档v29追加有解压大小边界的压缩战报；旧存档无法补回未曾记录的历史。

出征改为武将编队、兵种钱粮、部队详情三个页签；同列表连续选主副将、一键交换主将。复用真实出征格和正式攻防、移动、射程、后勤计算；切页保留草稿，新出征清空。原地图、美术、既有数值规则不回退。自动结算旧旬的效果归旧旬，恢复后的玩家自动行军归新旬。

验证结果以本轮Actions日志为准，不把编译等同于真机验证。

'''
for p in [Path('README.md'),Path('progress.md')]:p.write_text(notes+p.read_text())
Path('docs/BATTLE_REPORTS_SORTIE_V0_53.md').write_text(notes+'战报分类对现有文字结果做展示归类；我方关联由行动源、目标与属性变更前后的势力ID判定，不以名字关键词推断归属。按事件冻结结果正文，部队消失或倒戈不改写旧记录。安全上限触发时明确报存档错误，不静默截断有效旬内的战报。\n')
Path(__file__).unlink()
print('v0.53 production integration complete; one-shot script removed.')
