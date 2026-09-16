package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.*;

/** Dependency-free executable UI projection/camera regressions, also run by Gradle check. */
public final class PresentationTest {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    private static void near(float a,float b,String label){check(Math.abs(a-b)<.02f,label+": "+a+" vs "+b);}
    private static void deploymentCaps()throws Exception {
        World w=ScenarioCatalog.load("regional-sandbox",0);World.City c=w.home();int leader=w.idle(c).get(0).id;
        c.troops=7000;c.food=8000;c.equipment[0]=4567;
        check(UiModels.deployTroopCap(w,c,leader,World.Weapon.SPEAR,Army.Ship.BOAT)==4567,"equipment limits actual slider maximum");
        c.food=1234;check(UiModels.deployTroopCap(w,c,leader,World.Weapon.SPEAR,Army.Ship.BOAT)==1234,"food limits troop maximum");
        c.food=8000;c.equipment[5]=1;
        check(UiModels.deployTroopCap(w,c,leader,World.Weapon.RAM,Army.Ship.BOAT)==Math.min(7000,w.government.commandLimit(leader)),"siege stock uses one piece, not troop count");
        c.equipment[5]=0;check(UiModels.deployTroopCap(w,c,leader,World.Weapon.RAM,Army.Ship.BOAT)==0,"missing siege piece blocks form");
        c.ships[0]=0;check(UiModels.deployTroopCap(w,c,leader,World.Weapon.SWORD,Army.Ship.TOWER_SHIP)==0,"missing ship blocks form");
        check(UiModels.deployTroopCap(w,c,leader,World.Weapon.SWORD,Army.Ship.BOAT)==Math.min(7000,w.government.commandLimit(leader)),"sword needs no inventory");
    }
    private static void readyQueueAndNotices()throws Exception {
        World w=new World(20,12,"我军","敌军");w.cities.add(new World.City(0,"本营",new Hex(2,2),0));w.cities.add(new World.City(1,"敌营",new Hex(17,9),1));
        for(int i=1;i<=4;i++){World.Officer o=new World.Officer(i,"将"+i,0,-1,80,80,80,80,80);o.unitId=i;w.officers.add(o);w.units.add(new World.Unit(i,0,i,World.Weapon.SPEAR,new Hex(3+i,4),4000,10000));}w.nextUnitId=5;
        w.unit(2).acted=true;w.unit(4).status=War.Status.CONFUSED;
        byte[] before=SaveCodec.encode(w);
        check(UiModels.readyUnits(w).size()==2,"only eligible direct armies in ready queue");
        check(UiModels.cycleReady(w,1,1).id==3,"next skips spent army");
        check(UiModels.cycleReady(w,3,1).id==1,"next wraps by stable ID");
        check(UiModels.cycleReady(w,1,-1).id==3,"previous wraps by stable ID");
        check(Arrays.equals(before,SaveCodec.encode(w)),"cycling and reasons preserve game/RNG");
        w.unit(1).acted=true;w.unit(3).acted=true;check(UiModels.cycleReady(w,1,1)==null,"empty ready queue is explicit");
        World normal=ScenarioCatalog.load("regional-sandbox",2);World low=SaveCodec.decode(SaveCodec.encode(normal));low.home().food=0;
        String report=UiModels.turnSummary(normal,low);check(report.indexOf("新增待处理")<report.indexOf("城池资源净变化"),"priority changes precede bulk resource deltas");
        check(UiModels.attention(low).stream().anyMatch(n->n.key.equals("food:"+low.home().id)),"food notice uses real city ID");
        check(UiModels.turnSummary(low,SaveCodec.decode(SaveCodec.encode(low))).contains("没有新增异常"),"persistent anomaly not repeated as new alert");
    }
    public static void main(String[] args)throws Exception {
        deploymentCaps();readyQueueAndNotices();
        for(int i=0;i<PortraitCatalog.NAMES.length;i++)check(PortraitCatalog.index(PortraitCatalog.NAMES[i])==i,"stable famous portrait mapping");
        check(PortraitCatalog.index("劉備")==1&&PortraitCatalog.index("趙雲")==6&&PortraitCatalog.index("自建武将")==-1,"traditional names and custom portrait fallback");
        check(PortraitCatalog.variant(1,"甲")==PortraitCatalog.variant(1,"甲")&&PortraitCatalog.variant(1,"甲")!=PortraitCatalog.variant(2,"甲"),"fallback identity stable across redraws and different IDs");
        World w=ScenarioCatalog.load("regional-sandbox",2);
        byte[] unchanged=SaveCodec.encode(w);
        check(UiModels.officers(w,"周",2,-1,0).size()==1,"name and faction filter");
        check(UiModels.officers(w,"",2,300,4).get(0).name.equals("鲁肃"),"politics descending in city");
        check(UiModels.officers(w,"不存在",-1,-1,0).isEmpty(),"empty search");
        check(UiModels.cities(w,0).get(0).owner==2,"friendly city first");
        check(UiModels.tasks(w,0).isEmpty(),"no phantom task");
        check(Arrays.equals(unchanged,SaveCodec.encode(w)),"projections never mutate world");
        personnelProjection();abilityProjection();marchProjection();viewport();
        check(UiModels.cities(w,0,"建业",2).size()==1,"city search and owner");
        check(UiModels.cities(w,0,"不存在",-1).isEmpty(),"empty city search");
        check(UiModels.factions(w,"孙权").equals(Arrays.asList(2)),"faction search preserves ID");
        check(w.domestic.build(300,3001,Domestic.Kind.MARKET,w.domestic.buildSites(300).get(0)).ok,"construction fixture");
        check(w.domestic.transfer(300,310,3000).ok,"travel fixture");
        check(w.domestic.transport(300,310,3002,1000,3000,1000,new int[]{1000,0,0,0}).ok,"cargo fixture");
        check(UiModels.tasks(w,0,"鲁肃").size()==1,"task name search");
        check(UiModels.tasks(w,0,"不存在").isEmpty(),"empty task search");
        List<UiModels.Task> tasks=UiModels.tasks(w,0);check(tasks.size()==3,"unified tasks");
        check(tasks.stream().map(t->t.id).distinct().count()==3,"stable IDs across task types");
        check(UiModels.tasks(w,1).size()==1&&UiModels.tasks(w,2).size()==1&&UiModels.tasks(w,3).size()==1,"type filters");
        check(UiModels.tasks(w,3).get(0).detail.contains("金 1000")&&UiModels.tasks(w,3).get(0).detail.contains("柴桑 → 建业"),"cargo and route visible");
        check(UiModels.officers(w,"孙权",2,300,0).isEmpty(),"travel excluded from stationed filter");
        check(UiModels.location(w,w.officer(3000)).equals("柴桑 → 建业"),"travel location");
        World before=SaveCodec.decode(SaveCodec.encode(w));w.nextTurn();String first=UiModels.turnSummary(before,w);
        check(first.contains("剩余 1 旬")&&first.contains(before.date())&&first.contains(w.date()),"summary dates and progress");
        before=SaveCodec.decode(SaveCodec.encode(w));w.nextTurn();String second=UiModels.turnSummary(before,w);
        check(second.contains("市场建设完成")&&second.contains("抵达建业")&&second.contains("入库：金 1000"),"summary completion and cargo arrival");
        check(second.contains("粮 2900")&&!second.contains("粮 2950"),"arrival report uses actual final grain after last travel ration");
        // Resource changes are net snapshots, not invented monthly income calculations.
        before=SaveCodec.decode(SaveCodec.encode(w));w.city(300).gold-=123;check(UiModels.turnSummary(before,w).contains("金 -123"),"signed net delta");
        for(int width:new int[]{1920,2340,2400}) {
            MapCamera c=new MapCamera();c.resize(width,1080,1400,900,25,3);near(c.scale,c.minScale,"initial fit");
            c.zoom(10000,width/2f,540);near(c.scale,c.maxScale,"max zoom");
            c.pan(100000,100000);check(c.x-25*c.scale<=48.02&&c.y-25*c.scale<=48.02,"positive drag bounds");
            c.pan(-200000,-200000);check(c.x+(1400-25)*c.scale>=width-48.02&&c.y+(900-25)*c.scale>=1080-48.02,"negative drag bounds");
            c.zoom(0,width/2f,540);near(c.scale,c.minScale,"min zoom");
            c.focus(650,400);float cx=c.centerX(),cy=c.centerY(),ratio=c.scale/c.minScale;
            float scaleBefore=c.scale;
            c.resize(width-300,1080,1400,900,25,3);near(c.scale,scaleBefore,"panel opening keeps world scale");near(c.centerX(),cx,"resize world center x");near(c.centerY(),cy,"resize world center y");
            c.restore(ratio,cx,cy);near(c.centerX(),cx,"restored camera center");
            // Pinch preserves the touched world point when not on a clamped boundary.
            c.focus(650,400);float fx=(width-300)/2f+30,fy=550;float wx=(fx-c.x)/c.scale,wy=(fy-c.y)/c.scale;c.zoom(c.scale*1.1f,fx,fy);near((fx-c.x)/c.scale,wx,"pinch focal x");near((fy-c.y)/c.scale,wy,"pinch focal y");
        }
        // Orientation and sheet changes must expose more map, not magnify the same patch.
        MapCamera rotated=new MapCamera();rotated.resize(1080,2000,10000,8000,25,3);rotated.focus(4500,3500);
        float stableScale=rotated.scale,stableX=rotated.centerX(),stableY=rotated.centerY();
        for(int[] size:new int[][]{{1080,1000},{2200,780},{1500,780},{2200,780},{1080,2000}}){
            rotated.resize(size[0],size[1],10000,8000,25,3);
            near(rotated.scale,stableScale,"orientation preserves readable zoom");near(rotated.centerX(),stableX,"orientation preserves world center x");near(rotated.centerY(),stableY,"orientation preserves world center y");
        }
        MapCamera restored=new MapCamera();restored.resize(1080,1000,10000,8000,25,3);restored.restoreScale(stableScale,stableX,stableY);
        near(restored.scale,stableScale,"saved absolute zoom independent of sheet size");near(restored.centerX(),stableX,"saved absolute camera x");near(restored.centerY(),stableY,"saved absolute camera y");
        System.out.println("PASS: "+checks+" UI projection/camera assertions.");
    }
    private static void viewport(){
        Random rng=new Random(803);
        for(int width:new int[]{960,1170,1200}){
            MapCamera c=new MapCamera();c.resize(width,540,8300,4820,25,1.5f);
            for(int n=0;n<25;n++){
                c.focus(rng.nextFloat()*8200,rng.nextFloat()*4700);c.zoom(c.minScale+(c.maxScale-c.minScale)*rng.nextFloat(),width/2f,270);
                int first=c.firstRow(128,50),last=c.lastRow(128,50),visited=0;
                for(int r=0;r<128;r++)for(int q=0;q<128;q++)if(c.visible(25*1.7320508f*(q+r*.5f),37.5f*r,25*c.scale))check(r>=first&&r<=last&&q>=c.firstColumn(r,128,50)&&q<=c.lastColumn(r,128,50),"viewport never omits visible hex");
                for(int r=first;r<=last;r++)visited+=Math.max(0,c.lastColumn(r,128,50)-c.firstColumn(r,128,50)+1);
                if(c.scale>1)check(visited<128*128/4,"zoomed traversal bounded by viewport");
                float scale=c.scale;c.centerOn(4100,2400);near(c.scale,scale,"navigator preserves zoom");
            }
        }
    }
    private static void abilityProjection()throws Exception {
        World w=new World(20,14,"学营","守营");
        w.cities.add(new World.City(10,"学宫",new Hex(2,2),0));w.cities.add(new World.City(20,"守城",new Hex(17,11),1));
        for(World.City c:w.cities){c.gold=30000;c.food=200000;c.troops=0;}
        w.officers.add(new World.Officer(0,"习武生",0,10,50,50,50,50,50));
        check(w.abilities.startResearch(10,"lead.low").ok,"PK research projection fixture");
        List<UiModels.Task> tasks=UiModels.tasks(w,4);
        check(tasks.size()==1&&tasks.get(0).abilityResearch!=null&&tasks.get(0).title.equals("PK研究统率+5低"),"real research task in filter");
        World before=SaveCodec.decode(SaveCodec.encode(w));w.nextTurn();String summary=UiModels.turnSummary(before,w);
        check(summary.contains("PK研究统率+5低 · 剩余8旬")&&!summary.contains("本旬没有建设"),"PK research progress in summary");
        for(int i=0;i<7;i++)w.nextTurn();before=SaveCodec.decode(SaveCodec.encode(w));w.nextTurn();
        check(UiModels.turnSummary(before,w).contains("PK研究统率+5低 · 已完成"),"research completion shown");
        check(w.abilities.train(10,0,"lead.low",true).ok,"PK training projection fixture");
        tasks=UiModels.tasks(w,4);check(tasks.size()==1&&tasks.get(0).abilityTraining!=null&&tasks.get(0).title.equals("PK培养统率+5低 · 习武生"),"real training task in filter");
        before=SaveCodec.decode(SaveCodec.encode(w));w.nextTurn();
        check(UiModels.turnSummary(before,w).contains("PK培养统率+5低 · 习武生 · 剩余2旬"),"training progress shown");
        w.nextTurn();before=SaveCodec.decode(SaveCodec.encode(w));w.nextTurn();
        check(UiModels.turnSummary(before,w).contains("PK培养统率+5低 · 习武生 · 已完成"),"training completion shown");
        check(w.abilities.startResearch(10,"buqu").ok&&w.abilities.train(10,0,"lead.low",true).ok,"parallel research and training");
        before=SaveCodec.decode(SaveCodec.encode(w));w.city(10).owner=1;w.checkVictory();summary=UiModels.turnSummary(before,w);
        check(summary.contains("PK研究不屈 · 已中止")&&summary.contains("PK培养统率+5低 · 习武生 · 已中止"),"loss is cancellation, never completion");
    }
    private static void marchProjection()throws Exception {
        World w=ScenarioCatalog.load("regional-sandbox",2);check(w.deploy(310,3003,World.Weapon.CROSSBOW,3000).ok,"march projection deployment");
        World.Unit u=w.unit(w.officer(3003).unitId);MarchOrders.Plan plan=w.marches.preview(u.id,w.city(300).hex);check(plan.valid()&&w.marches.execute(plan).ok&&u.march!=null,"long route for UI task");
        byte[] before=SaveCodec.encode(w);List<UiModels.Task> tasks=UiModels.tasks(w,6);check(tasks.size()==1&&tasks.get(0).marching==u&&tasks.get(0).location.equals(u.hex),"march task targets actual unit location");
        check(UiModels.tasks(w,0).size()==1&&UiModels.tasks(w,6,"柴桑").size()==1&&UiModels.tasks(w,3).isEmpty(),"march filter/name/global count without cargo phantom");
        check(UiModels.status(w,w.officer(3003)).contains("行军 → 柴桑"),"officer list shows actual march");
        check(Arrays.equals(before,SaveCodec.encode(w)),"route task reads are pure");World old=SaveCodec.decode(before);check(w.nextTurn().ok&&UiModels.turnSummary(old,w).contains("行军 · 甘宁"),"turn summary includes route progress");
        if(u.march!=null)check(w.marches.stop(u.id).ok,"stop fixture");check(UiModels.tasks(w,6).isEmpty(),"stopped route removed from task count");
    }
    private static void personnelProjection()throws Exception {
        World w=ScenarioCatalog.load("regional-sandbox",2);
        check(UiModels.governor(w,300).equals("未任命"),"no invented governor before appointment");
        check(w.strategy.search(300,3002).ok,"discover seeded talent");
        World.Officer talent=w.officer(910002);
        check(talent!=null&&UiModels.status(w,talent).equals("在野 · 待登用"),"unaffiliated talent is not an idle officer");
        check(UiModels.faction(w,talent).equals("在野"),"unaffiliated faction label");
        check(UiModels.officerCount(w,300)==3,"talent does not inflate stationed officer count");
        check(UiModels.officers(w,"",-2,-1,0).size()==1,"unaffiliated filter is separate from all forces");
        byte[] before=SaveCodec.encode(w);
        UiModels.officers(w,"",-1,-1,0);UiModels.status(w,talent);UiModels.governor(w,300);
        check(Arrays.equals(before,SaveCodec.encode(w)),"personnel projections do not consume random state");
        check(w.strategy.recruitOfficer(300,3000,talent.id).ok&&talent.owner==2,"seeded hire fixture");
        check(UiModels.officers(w,"",-2,-1,0).isEmpty()&&UiModels.officerCount(w,300)==4,"hire updates filters and stationed count");
        check(UiModels.status(w,talent).equals("本旬已行动"),"newly hired officer rests this turn");
        check(w.strategy.appointGovernor(300,3001,3001).ok&&UiModels.governor(w,300).equals("周瑜"),"actual appointed governor displayed");
        World restored=SaveCodec.decode(SaveCodec.encode(w));
        check(UiModels.governor(restored,300).equals("周瑜")&&restored.officer(3001).role==Strategy.Role.GOVERNOR,"governor projection survives v4 round trip");
        World deployed=ScenarioCatalog.load("regional-sandbox",2);
        check(deployed.deploy(310,3003,World.Weapon.CROSSBOW,3000).ok,"deployed officer fixture");
        World.Officer officer=deployed.officer(3003);World.Unit unit=deployed.unit(officer.unitId);
        officer.acted=false;unit.acted=true;
        check(UiModels.status(deployed,officer).equals("出征 · 已行动"),"deployed status follows army action flag");
        World assigned=ScenarioCatalog.load("regional-sandbox",2);
        check(assigned.strategy.beginAssignment(300,3002,"政务",2).ok,"long assignment fixture");
        check(UiModels.status(assigned,assigned.officer(3002)).equals("政务 · 剩2旬"),"long assignment visible");
    }
}
