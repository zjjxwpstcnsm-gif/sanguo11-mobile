package game.sanguo.core;

import java.util.*;
import java.security.MessageDigest;

/** Run unchanged before/after extracting the query validators, including arrival and return. */
public final class DiplomacyParityProbe {
    public static World fixture(){
        World w=new World(22,16);
        w.cities.add(new World.City(10,"甲",new Hex(5,5),0));
        w.cities.add(new World.City(20,"乙",new Hex(18,5),1));
        for(int i=0;i<4;i++)w.officers.add(new World.Officer(i,"将"+i,0,10,70+i,80+i,90+i,85,85));
        w.officers.add(new World.Officer(20,"敌",1,20,80,80,80,80,80));
        w.city(10).gold=30000;w.city(10).food=200000;w.city(20).food=200000;
        w.strategy.initializeOffices();w.strategy.setFactionRelation(0,1,30);return w;
    }
    private static String hash(World w)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(SaveCodec.encode(w)));}
    public static void main(String[] args)throws Exception{
        for(int type=0;type<4;type++)for(int n=0;n<11;n++){
            World w=fixture();int city=10,officer=0,side=1,turns=6;
            Campaign.TreatyKind kind=type==1?Campaign.TreatyKind.CEASEFIRE:Campaign.TreatyKind.ALLIANCE;
            if(type==3)w.campaign.concludeTreaty(0,1,Campaign.TreatyKind.ALLIANCE,12);
            switch(n){case 1:city=-1;break;case 2:officer=-1;break;case 3:side=-1;break;
                case 4:w.actionPoints[0]=0;break;case 5:w.city(10).gold=0;break;
                case 6:turns=4;break;case 7:w.strategy.setFactionRelation(0,1,100);break;
                case 8:w.strategy.setFactionRelation(0,1,-50);break;case 9:kind=null;break;
                case 10:w.campaign.concludeTreaty(0,1,Campaign.TreatyKind.CEASEFIRE,3);break;}
            World.Result r=type==0?w.campaign.goodwill(city,officer,side):type==3?w.campaign.breakTreaty(city,officer,side):w.campaign.negotiate(city,officer,side,kind,turns);
            System.out.println(type+"/"+n+" "+r.ok+" "+r.message+" "+hash(w));
            if(r.ok)for(int turn=0;turn<10;turn++){
                r=w.nextTurn();System.out.println(type+"/"+n+"/turn"+turn+" "+r.ok+" "+hash(w));
            }
        }
    }
}
