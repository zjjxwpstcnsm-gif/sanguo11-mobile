package game.sanguo.api;

/** Immutable before/after fact from one committed authority boundary. No reward formula. */
public final class TechniquePointsChange {
    public final int owner,before,after,delta;
    public TechniquePointsChange(int owner,int before,int after){
        if(owner<0||before<0||after<0)throw new IllegalArgumentException("Invalid technique points fact");
        this.owner=owner;this.before=before;this.after=after;this.delta=after-before;
    }
}
