package game.sanguo.api;
/** Current ordinary administration: PATROL,TRAIN,RECRUIT,SEARCH,REWARD,APPOINT_GOVERNOR.
 * targets is empty for the first four, nonempty for batch REWARD, one for APPOINT_GOVERNOR. */
public final class CityActionCommand {
 public final StateToken expected;
 public final String operation;
 public final int cityId,officerId;
 private final int[] targets;
 public CityActionCommand(StateToken expected,String operation,int cityId,int officerId,int[] targets){this.expected=expected;this.operation=operation;this.cityId=cityId;this.officerId=officerId;this.targets=targets==null?null:targets.clone();}
 public int[] targets(){return targets==null?null:targets.clone();}
}
