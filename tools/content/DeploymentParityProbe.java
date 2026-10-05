package game.sanguo.core;

import java.util.*;
import java.security.MessageDigest;

/** Compile unchanged against the frozen inherited core and the current core; compare stdout exactly. */
public final class DeploymentParityProbe {
    private static World fixture(){
        World w=new World(22,16);w.cities.add(new World.City(10,"甲",new Hex(5,5),0));w.cities.add(new World.City(20,"乙",new Hex(18,5),1));
        for(int i=0;i<4;i++)w.officers.add(new World.Officer(i,"将"+i,0,10,70+i,80+i,90+i,85,85));
        w.officers.add(new World.Officer(20,"敌",1,20,80,80,80,80,80));World.City c=w.city(10);
        c.gold=30000;c.food=200000;c.troops=30000;Arrays.fill(c.equipment,12000);c.equipment[World.Weapon.SWORD.ordinal()]=0;
        for(int i=5;i<9;i++)c.equipment[i]=2;Arrays.fill(c.ships,2);w.strategy.initializeOffices();return w;
    }
    private static String hash(World w)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(SaveCodec.encode(w)));}
    public static void main(String[] args)throws Exception{
        for(World.Weapon weapon:World.Weapon.values())for(Army.Ship ship:Army.Ship.values()){
            World w=fixture();World.Result r=w.army.deploy(10,0,new int[]{1,2},weapon,ship,3000,9000,123);
            System.out.println(weapon+"/"+ship+" "+r.ok+" "+r.message+" "+hash(w));
        }
        for(int n=0;n<12;n++){
            World w=fixture();int city=10,leader=0,troops=3000,food=9000,gold=0;int[] deputies={1,2};World.Weapon weapon=World.Weapon.SPEAR;Army.Ship ship=Army.Ship.BOAT;
            switch(n){case 0:city=-1;leader=-1;gold=-1;break;case 1:leader=-1;gold=-1;break;case 2:w.actionPoints[0]=0;gold=-1;break;
                case 3:gold=-1;weapon=null;break;case 4:weapon=null;ship=null;break;case 5:ship=null;break;case 6:deputies=new int[]{0};break;
                case 7:troops=999;food=0;break;case 8:food=2999;break;case 9:w.city(10).equipment[0]=0;break;case 10:w.nextUnitId=10000000;break;
                case 11:for(Hex h:SiteFootprint.edge(w.city(10)))w.terrain[h.q][h.r]=World.Terrain.MOUNTAIN;break;}
            World.Result r=w.army.deploy(city,leader,deputies,weapon,ship,troops,food,gold);
            System.out.println("reject/"+n+" "+r.ok+" "+r.message+" "+hash(w));
        }
    }
}
