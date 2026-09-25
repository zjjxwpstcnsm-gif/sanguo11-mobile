package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.*;

public final class NativeR14Test {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        World w=ScenarioCatalog.load("heroes-250",0);
        MapSceneSnapshot.Ground ground=new MapSceneSnapshot.Ground(w);
        for(int start=1;start<=12;start++)for(int turn:new int[]{0,2,3,8,9,35,36,72}){
            w.startMonth=start;w.turn=turn;
            byte[] before=SaveCodec.encode(w);
            MapSceneSnapshot a=new MapSceneSnapshot(ground,w,w.home().hex,-1);
            int expected=(start-1+turn/3)%12+1;
            check(a.month==expected,"immutable month follows authoritative date");
            check(SeasonStyle.fromDate(start,turn)==SeasonStyle.forMonth(expected),"season quarter mapping");
            check(w.date().contains(expected+"月"),"visible core calendar agreement");
            check(Arrays.equals(before,SaveCodec.encode(w)),"projection does not mutate full save/RNG");
            check(a.ground==ground&&ground.matches(w),"date keeps terrain/cache identity");
            if(turn%9==0){
                World restored=SaveCodec.decode(before);
                MapSceneSnapshot b=new MapSceneSnapshot(ground,restored,restored.home().hex,-1);
                check(b.month==a.month,"restored calendar agrees");
                check(Arrays.equals(before,SaveCodec.encode(restored)),"full save roundtrip unchanged");
            }
        }
        check(SeasonStyle.forMonth(1)==SeasonStyle.SPRING,"known manual January spring convention");
        check(SeasonStyle.forMonth(4)==SeasonStyle.SUMMER,"summer boundary");
        check(SeasonStyle.forMonth(7)==SeasonStyle.AUTUMN,"autumn boundary");
        check(SeasonStyle.forMonth(10)==SeasonStyle.WINTER,"winter boundary");
        check(SeasonStyle.fromDate(12,3)==SeasonStyle.SPRING,"year rollover");
        for(int invalid:new int[]{-1,0,13}){boolean rejected=false;try{SeasonStyle.forMonth(invalid);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"invalid profile month rejected");}
        World authority=ScenarioCatalog.load("coalition-190",5),reference=SaveCodec.decode(SaveCodec.encode(authority));
        authority.startMonth=1;reference.startMonth=1;authority.turn=8;reference.turn=8;
        for(int i=0;i<2;i++){
            new MapSceneSnapshot(ground,authority,authority.home().hex,-1);
            check(authority.nextTurn().ok&&reference.nextTurn().ok,"normal quarter-crossing turn");
            check(Arrays.equals(SaveCodec.encode(authority),SaveCodec.encode(reference)),"quarter turn state/RNG equality");
            check(new MapSceneSnapshot(ground,authority,authority.home().hex,-1).month==4,"calendar exact after turn");
        }
        System.out.println("PASS R14 season/date/save/authority checks="+checks);
    }
}
