package game.sanguo.api.bridge;

import java.util.Objects;

/** Detached schema-1 entity data; no rule-world or platform references. */
public final class BridgeEntity {
    public final String entityId, kind, name;
    public final int q, r, owner, troops, energy, officerId, gold, food, order;
    public BridgeEntity(String id,String kind,String name,int q,int r,int owner,int troops,int energy,int officerId,int gold,int food,int order){
        this.entityId=id;this.kind=kind;this.name=name;this.q=q;this.r=r;
        this.owner=owner;this.troops=troops;this.energy=energy;this.officerId=officerId;this.gold=gold;this.food=food;this.order=order;
    }
    @Override public boolean equals(Object other){
        if(!(other instanceof BridgeEntity))return false;BridgeEntity e=(BridgeEntity)other;
        return entityId.equals(e.entityId)&&kind.equals(e.kind)&&name.equals(e.name)&&q==e.q&&r==e.r
            &&owner==e.owner&&troops==e.troops&&energy==e.energy&&officerId==e.officerId&&gold==e.gold&&food==e.food&&order==e.order;
    }
    @Override public int hashCode(){return Objects.hash(entityId,kind,name,q,r,owner,troops,energy,officerId,gold,food,order);}
}
