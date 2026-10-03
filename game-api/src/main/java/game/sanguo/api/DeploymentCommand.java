package game.sanguo.api;

/** Enum names are stable wire codes. Invalid inputs are rejected by the rule host, not constructors. */
public final class DeploymentCommand {
    public final StateToken expected;
    public final int cityId,leaderId,troops,food,gold;
    public final String weapon,ship;
    private final int[] deputies;
    public DeploymentCommand(StateToken expected,int cityId,int leaderId,int[] deputies,String weapon,String ship,int troops,int food,int gold){
        this.expected=expected;this.cityId=cityId;this.leaderId=leaderId;this.deputies=deputies==null?null:deputies.clone();
        this.weapon=weapon;this.ship=ship;this.troops=troops;this.food=food;this.gold=gold;
    }
    public int[] deputies(){return deputies==null?null:deputies.clone();}
}
