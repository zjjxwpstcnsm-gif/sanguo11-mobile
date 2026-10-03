package game.sanguo.api;
/** Immutable transport draft. Equipment order is SPEAR,HALBERD,CROSSBOW,CAVALRY,
 * SWORD,RAM,SIEGE_TOWER,WOODEN_BEAST,CATAPULT (legacy length4 also accepted).
 * Ships are TOWER_SHIP,WARSHIP cargo; sea enables the existing route mode. */
public final class TransportCommand {
 public final StateToken expected;
 public final int sourceCityId,targetCityId,officerId,gold,food,troops;
 public final boolean sea,returnOfficers;
 private final int[] deputies,equipment,ships;
 public TransportCommand(StateToken expected,int source,int target,int officer,int[] deputies,int gold,int food,int troops,int[] equipment,boolean sea,boolean returning,int[] ships){
  this.expected=expected;sourceCityId=source;targetCityId=target;officerId=officer;this.deputies=copy(deputies);this.gold=gold;this.food=food;this.troops=troops;this.equipment=copy(equipment);this.sea=sea;returnOfficers=returning;this.ships=copy(ships);
 }
 private static int[] copy(int[] value){return value==null?null:value.clone();}
 public int[] deputies(){return copy(deputies);}public int[] equipment(){return copy(equipment);}public int[] ships(){return copy(ships);}
}
