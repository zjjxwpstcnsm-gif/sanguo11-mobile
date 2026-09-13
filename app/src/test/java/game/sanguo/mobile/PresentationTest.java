package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.*;

/** Dependency-free executable UI projection/camera regressions, also run by Gradle check. */
public final class PresentationTest {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    private static void near(float a,float b,String label){check(Math.abs(a-b)<.02f,label+": "+a+" vs "+b);}
    public static void main(String[] args)throws Exception {
        World w=ScenarioCatalog.load("regional-sandbox",2);
        byte[] unchanged=SaveCodec.encode(w);
        check(UiModels.officers(w,"周",2,-1,0).size()==1,"name and faction filter");
        check(UiModels.officers(w,"",2,300,4).get(0).name.equals("鲁肃"),"politics descending in city");
        check(UiModels.officers(w,"不存在",-1,-1,0).isEmpty(),"empty search");
        check(UiModels.cities(w,0).get(0).owner==2,"friendly city first");
        check(UiModels.tasks(w,0).isEmpty(),"no phantom task");
        check(Arrays.equals(unchanged,SaveCodec.encode(w)),"projections never mutate world");
        check(w.domestic.build(300,3001,Domestic.Kind.MARKET,w.domestic.buildSites(300).get(0)).ok,"construction fixture");
        check(w.domestic.transfer(300,310,3000).ok,"travel fixture");
        check(w.domestic.transport(300,310,3002,1000,3000,1000,new int[]{1000,0,0,0}).ok,"cargo fixture");
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
        // Resource changes are net snapshots, not invented monthly income calculations.
        before=SaveCodec.decode(SaveCodec.encode(w));w.city(300).gold-=123;check(UiModels.turnSummary(before,w).contains("金 -123"),"signed net delta");
        for(int width:new int[]{1920,2340,2400}) {
            MapCamera c=new MapCamera();c.resize(width,1080,1400,900,25,3);near(c.scale,c.minScale,"initial fit");
            c.zoom(10000,width/2f,540);near(c.scale,c.maxScale,"max zoom");
            c.pan(100000,100000);check(c.x-25*c.scale<=48.02&&c.y-25*c.scale<=48.02,"positive drag bounds");
            c.pan(-200000,-200000);check(c.x+(1400-25)*c.scale>=width-48.02&&c.y+(900-25)*c.scale>=1080-48.02,"negative drag bounds");
            c.zoom(0,width/2f,540);near(c.scale,c.minScale,"min zoom");
            c.focus(650,400);float cx=c.centerX(),cy=c.centerY(),ratio=c.scale/c.minScale;
            c.resize(width-300,1080,1400,900,25,3);near(c.centerX(),cx,"resize world center x");near(c.centerY(),cy,"resize world center y");
            c.restore(ratio,cx,cy);near(c.centerX(),cx,"restored camera center");
            // Pinch preserves the touched world point when not on a clamped boundary.
            c.focus(650,400);float fx=(width-300)/2f+30,fy=550;float wx=(fx-c.x)/c.scale,wy=(fy-c.y)/c.scale;c.zoom(c.scale*1.1f,fx,fy);near((fx-c.x)/c.scale,wx,"pinch focal x");near((fy-c.y)/c.scale,wy,"pinch focal y");
        }
        System.out.println("PASS: "+checks+" UI projection/camera assertions.");
    }
}
