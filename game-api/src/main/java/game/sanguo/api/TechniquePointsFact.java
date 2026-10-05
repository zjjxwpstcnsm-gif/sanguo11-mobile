package game.sanguo.api;

import java.util.Objects;

/** One actual point change at a successful commit; no formula or mutable rule reference. */
public final class TechniquePointsFact {
    public final String id,parentId,cause,phase;
    /** Optional parent presentation checkpoint; blank for commits with no journal. */
    public final String presentationParentId;
    public final StateToken state;
    public final long sequence;
    public final int owner,before,after,delta,cityId,officerId;
    public TechniquePointsFact(String parentId,StateToken state,long sequence,int owner,int before,int after,
                               String cause,String phase,int cityId,int officerId){
        this(parentId,state,sequence,owner,before,after,cause,phase,cityId,officerId,"");
    }
    public TechniquePointsFact(String parentId,StateToken state,long sequence,int owner,int before,int after,
                               String cause,String phase,int cityId,int officerId,String presentationParentId){
        if(sequence<1||owner<0||before<0||after<0||before==after)throw new IllegalArgumentException("Invalid point fact");
        this.parentId=Objects.requireNonNull(parentId);this.state=Objects.requireNonNull(state);this.sequence=sequence;
        this.id=parentId+":technique:"+sequence;this.owner=owner;this.before=before;this.after=after;delta=after-before;
        this.cause=Objects.requireNonNull(cause);this.phase=Objects.requireNonNull(phase);this.cityId=cityId;this.officerId=officerId;
        this.presentationParentId=Objects.requireNonNull(presentationParentId);
    }
}
