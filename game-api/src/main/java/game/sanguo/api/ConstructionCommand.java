package game.sanguo.api;

/** Ordinary new construction; kind is the stable Domestic.Kind name, q/r are axial map coordinates. */
public final class ConstructionCommand {
    public final StateToken expected;
    public final int cityId,officerId,q,r;
    public final String kind;
    public ConstructionCommand(StateToken expected,int cityId,int officerId,String kind,int q,int r){
        this.expected=expected;this.cityId=cityId;this.officerId=officerId;this.kind=kind;this.q=q;this.r=r;
    }
}
