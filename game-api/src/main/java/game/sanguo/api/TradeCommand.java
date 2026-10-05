package game.sanguo.api;
/** BUY or SELL an explicit food amount. Invalid input is retained, never silently clamped. */
public final class TradeCommand {
 public final StateToken expected;
 public final String operation;
 public final int cityId,officerId,food;
 public TradeCommand(StateToken expected,String operation,int cityId,int officerId,int food){this.expected=expected;this.operation=operation;this.cityId=cityId;this.officerId=officerId;this.food=food;}
}
