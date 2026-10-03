package game.sanguo.core;
import java.util.*;
import java.security.*;
/** Normal commands only: compare frozen complete R and current engine without sharing implementation. */
public final class PcProductionCompatibilityProbe {
 static World fixture(World.Weapon weapon,Army.Ship ship){
  World w=new World(24,20);w.cities.add(new World.City(10,"A",new Hex(4,4),0));w.cities.add(new World.City(20,"B",new Hex(18,14),1));
  for(int i=0;i<4;i++)w.officers.add(new World.Officer(i,"O"+i,0,10,80,80,80,90,80));w.officers.add(new World.Officer(20,"E",1,20,80,80,80,80,80));w.strategy.initializeOffices();w.city(10).gold=30000;w.strategy.setSeed(20261003L);
  Domestic.Kind kind=ship!=null?Domestic.Kind.SHIPYARD:Domestic.productionFacility(weapon);
  if(kind!=null){Hex h=w.domestic.buildSites(10).get(0);if(ship!=null){Hex water=h.neighbors().stream().filter(n->w.inside(n)&&w.cityAt(n)==null&&w.domestic.at(n)==null).findFirst().orElseThrow();w.terrain[water.q][water.r]=World.Terrain.WATER;}w.domestic.facilities.add(new Domestic.Facility(w.domestic.nextFacilityId++,10,kind,h,-1,0));}
  w.campaign.learned.put(0,EnumSet.allOf(Campaign.Tech.class));w.campaign.learned.get(0).remove(Campaign.Tech.WARSHIP);return w;
 }
 static String hash(World w)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(SaveCodec.encode(w)));}
 static void sample(World.Weapon weapon,Army.Ship ship,int mode)throws Exception{
  World w=fixture(weapon,ship);int actor=1,city=10;
  if(mode==1)w.actionPoints[0]=9;if(mode==2)w.city(10).gold=0;if(mode==3)w.officer(1).acted=true;if(mode==4)actor=999;if(mode==5)city=999;
  if(mode==6)w.domestic.facilities.clear();if(mode==7)for(var f:w.domestic.facilities)f.lastUseTurn=w.turn;
  if(mode==8)w.campaign.learned.clear();if(mode==9){if(ship!=null&&ship!=Army.Ship.BOAT)w.city(10).ships[ship.ordinal()-1]=100;else if(weapon!=null&&weapon!=World.Weapon.SWORD)w.city(10).equipment[weapon.ordinal()]=w.campaign.equipmentCap(w.city(10),weapon);}
  if(mode==10)w.officer(1).skillId=ship!=null?Skill.ZAOCHUAN.id:Army.siegeWeapon(weapon)?Skill.FAMING.id:weapon==World.Weapon.CAVALRY?Skill.FANZHI.id:Skill.NENGLI.id;
  var result=ship!=null?w.army.produce(city,actor,null,ship):w.produce(city,actor,weapon);String key=weapon+":"+ship+":"+mode;
  System.out.println(key+"|"+result.ok+"|"+result.message+"|"+hash(w)+"|"+w.strategy.getRandomState());
  if(result.ok)for(int n=0;n<4;n++){World restored=SaveCodec.decode(SaveCodec.encode(w));if(!w.nextTurn().ok||!restored.nextTurn().ok||!hash(w).equals(hash(restored)))throw new AssertionError("save replay "+key);System.out.println(key+":"+n+"|"+hash(w)+"|"+w.strategy.getRandomState());}
 }
 public static void main(String[] args)throws Exception{
  for(var weapon:World.Weapon.values())for(int mode=0;mode<=10;mode++)sample(weapon,null,mode);
  for(var ship:Army.Ship.values())for(int mode=0;mode<=10;mode++)sample(null,ship,mode);
 }
}
