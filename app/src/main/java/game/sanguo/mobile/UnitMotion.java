package game.sanguo.mobile;

import game.sanguo.core.*;

/** One mutable pose per renderer instance; never owns or executes a gameplay command. */
final class UnitMotion {
    float x,z,yaw;
    private Hex authoritative;

    void settle(Hex hex, GridWorldTransform grid) {
        authoritative=hex;x=grid.x(hex);z=grid.z(hex);
    }
    void sample(TurnJournal.Event event,float fraction,GridWorldTransform grid) {
        if(authoritative==null)return;
        // Every sample starts from the snapshot. Ending/replacing a clip cannot leave a stale pose.
        x=grid.x(authoritative);z=grid.z(authoritative);
        if(event==null)return;
        float f=Float.isFinite(fraction)?Math.max(0,Math.min(1,fraction)):0;
        if(event.kind==TurnJournal.Kind.MOVE&&!event.path.isEmpty()) {
            float cursor=f*(event.path.size()-1);
            int index=Math.min((int)cursor,event.path.size()-1);
            Hex from=event.path.get(index),to=event.path.get(Math.min(index+1,event.path.size()-1));
            if(from==null||to==null)return;
            float t=cursor-index;
            // Legacy forced displacement may only record endpoints: never draw a shortcut
            // through intervening cells whose legal route was not recorded by core.
            if(from.distance(to)>1){x=grid.x(from);z=grid.z(from);return;}
            float dx=grid.x(to)-grid.x(from),dz=grid.z(to)-grid.z(from);
            x=grid.x(from)+dx*t;z=grid.z(from)+dz*t;
            if(dx!=0||dz!=0)yaw=(float)Math.atan2(dx,dz);
        } else if((event.kind==TurnJournal.Kind.ATTACK||event.kind==TurnJournal.Kind.TACTIC||event.kind==TurnJournal.Kind.FACILITY_ATTACK||event.kind==TurnJournal.Kind.FACILITY_COUNTER)&&event.target!=null) {
            float dx=grid.x(event.target)-x,dz=grid.z(event.target)-z;
            if(dx!=0||dz!=0)yaw=(float)Math.atan2(dx,dz);
            if(CombatVisual.style(event)==CombatVisual.Style.CHARGE){
                float length=(float)Math.sqrt(dx*dx+dz*dz),pulse=f<CombatVisual.HIT?f/CombatVisual.HIT:(1-f)/(1-CombatVisual.HIT);
                if(length>0){float travel=Math.min(.65f,length*.35f)*Math.max(0,pulse);x+=dx/length*travel;z+=dz/length*travel;}
            }
        }
    }
}
