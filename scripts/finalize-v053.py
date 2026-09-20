#!/usr/bin/env python3
from pathlib import Path

def change(path,old,new):
 p=Path(path);s=p.read_text()
 if s.count(old)!=1:raise RuntimeError(f'{path}: anchor count {s.count(old)}: {old[:90]}')
 p.write_text(s.replace(old,new))

p='core/src/main/java/game/sanguo/core/BattleReports.java'
change(p,'private long nextId=1;','private final List<Change> pending=new ArrayList<>();\n    private long nextId=1;')
change(p,'actionKind=null;actionName="";}','actionKind=null;actionName="";pending.clear();}')
change(p,'prepare();List<Change> changes=changes();long related=actionRelated;','prepare();List<Change> changes=takeChanges();long related=actionRelated;')
anchor='    private static long bit(int owner)'
code='''    /** Saves a parent command's deltas before a nested facility counter mutates its attacker. */
    static final class ActionContext {
        int actor;long related;Hex location;Kind kind;String name;List<Change> changes;
    }
    synchronized ActionContext beginCounter(War.Structure source,Hex target){
        prepare();ActionContext context=new ActionContext();context.actor=actionOwner;context.related=actionRelated;context.location=actionLocation;context.kind=actionKind;context.name=actionName;context.changes=takeChanges();
        facility(source,target,TurnJournal.Kind.FACILITY_COUNTER,source.kind.label+"反击");return context;
    }
    synchronized void finishCounter(ActionContext context,String result){
        try{note(result);}finally{actionOwner=context.actor;actionRelated=context.related;actionLocation=context.location;actionKind=context.kind;actionName=context.name;pending.clear();pending.addAll(context.changes);}
    }
    private List<Change> takeChanges(){List<Change> result=new ArrayList<>(pending);pending.clear();result.addAll(changes());return result;}
'''
change(p,anchor,code+anchor)
p='core/src/main/java/game/sanguo/core/Fieldworks.java'
change(p,'w.reports.facility(s,u.hex,TurnJournal.Kind.FACILITY_COUNTER,s.kind.label+"反击");if(w.turnJournal!=null)','BattleReports.ActionContext reportContext=w.reports.beginCounter(s,u.hex);if(w.turnJournal!=null)')
change(p,'if(w.turnJournal!=null)w.turnJournal.checkpoint(s.kind.label+"反击，损失"+(before-u.troops)+"兵");','w.reports.finishCounter(reportContext,s.kind.label+"反击，损失"+(before-u.troops)+"兵");\n        if(w.turnJournal!=null)w.turnJournal.checkpoint(s.kind.label+"反击，损失"+(before-u.troops)+"兵");')
change(p,'String report=s.kind.label+"射击敌军，损失"+(before-target.troops)+"兵";w.note(report);','String report=s.kind.label+"射击敌军，损失"+(before-target.troops)+"兵";w.note(report);w.reports.clearAction();')
p='core/src/main/java/game/sanguo/core/EnergyRules.java'
old='''        if(w.turnJournal!=null){
            War.Structure source=null;
            for(War.Structure s:w.war.structures)if(s.complete&&s.kind==War.StructureKind.MUSIC&&s.owner==u.owner&&s.hex.distance(u.hex)<=2){source=s;break;}
            if(source!=null)w.turnJournal.facility(source,u.hex,TurnJournal.Kind.RECOVER,"军乐台恢复");
            else w.turnJournal.mark(TurnJournal.Kind.RECOVER,u.id,u.hex,"奏乐恢复");
        }
        Change result=change(u,gain,reason);'''
new='''        War.Structure source=null;
        for(War.Structure s:w.war.structures)if(s.complete&&s.kind==War.StructureKind.MUSIC&&s.owner==u.owner&&s.hex.distance(u.hex)<=2){source=s;break;}
        if(source!=null){w.reports.facility(source,u.hex,TurnJournal.Kind.RECOVER,"军乐台恢复");if(w.turnJournal!=null)w.turnJournal.facility(source,u.hex,TurnJournal.Kind.RECOVER,"军乐台恢复");}
        else {w.reports.action(TurnJournal.Kind.RECOVER,u.id,u.hex,"奏乐恢复");if(w.turnJournal!=null)w.turnJournal.mark(TurnJournal.Kind.RECOVER,u.id,u.hex,"奏乐恢复");}
        Change result=change(u,gain,reason);
        w.reports.note((reason==Reason.MUSIC?"军乐台":"奏乐")+"：气力+"+result.actual);w.reports.clearAction();'''
change(p,old,new)
p='core/src/test/java/game/sanguo/core/Reports53Test.java'
change(p,'retention();ownership();roundTrip();turnOwnership();preview();','retention();ownership();roundTrip();turnOwnership();preview();facilities();')
s=Path(p).read_text();at=s.rfind('}')
code='''    private static void facilities(){
        World w=Personnel49Fixture.world();World.Unit own=w.unit(2);War.Structure camp=new War.Structure(100,1,War.StructureKind.CAMP,new Hex(7,7),1100);w.war.structures.add(camp);w.reports.rebase();
        World.Result attack=w.war.attackStructure(own.id,camp.hex);require(attack.ok,attack.message);
        require(w.reports.query(-1,0,BattleReports.Scope.INITIATED,null,"攻击阵").size()==1,"original attack keeps player as initiator despite counterattack");
        List<BattleReports.Entry> counters=w.reports.query(-1,0,BattleReports.Scope.RECEIVED,null,"阵反击");require(counters.size()==1&&counters.get(0).actor==1,"facility counter is independently attributed to enemy");
        w.war.structures.clear();own.energy=40;w.war.structures.add(new War.Structure(101,0,War.StructureKind.MUSIC,new Hex(6,6),800));w.reports.rebase();w.energy.settleTurn();
        require(own.energy==50,"actual music recovery performed");require(w.reports.query(-1,0,BattleReports.Scope.INITIATED,null,"军乐台：气力+10").size()==1,"actual music gain recorded once without animation journal");
        w.war.structures.clear();w.war.structures.add(new War.Structure(102,1,War.StructureKind.ARROW_TOWER,new Hex(8,7),700));w.reports.rebase();w.fieldworks.towers();
        require(w.reports.query(-1,0,BattleReports.Scope.RECEIVED,null,"箭楼射击").size()==1,"enemy arrow tower result remains incoming and ownership-aware");
    }
'''
Path(p).write_text(s[:at]+code+s[at:])
Path(__file__).unlink()
