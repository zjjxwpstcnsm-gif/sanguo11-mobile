package game.sanguo.api;
import java.util.*;
/** EQUIPMENT with a weapon name (including siege), or SHIP with a ship name. One actor. */
public final class ProductionCommand {
 public final StateToken expected;
 public final String operation,item;
 public final int cityId,officerId;
 private final int[] officers;
 public ProductionCommand(StateToken expected,String operation,int cityId,int officerId,String item){this(expected,operation,cityId,new int[]{officerId},item);}
 public ProductionCommand(StateToken expected,String operation,int cityId,int[] officers,String item){this.expected=expected;this.operation=operation;this.cityId=cityId;this.officers=officers==null?new int[0]:officers.clone();this.officerId=this.officers.length==0?-1:this.officers[0];this.item=item;}
 public int[] officers(){return officers.clone();}
}
