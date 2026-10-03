package game.sanguo.api;
/** EQUIPMENT with a weapon name (including siege), or SHIP with a ship name. One actor. */
public final class ProductionCommand {
 public final StateToken expected;
 public final String operation,item;
 public final int cityId,officerId;
 public ProductionCommand(StateToken expected,String operation,int cityId,int officerId,String item){this.expected=expected;this.operation=operation;this.cityId=cityId;this.officerId=officerId;this.item=item;}
}
