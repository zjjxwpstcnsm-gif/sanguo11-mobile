package game.sanguo.mobile;

import game.sanguo.core.*;

/** Shared player fraction and deterministic visual clock only; no command/RNG access. */
final class UnitAnimation {
    String clip="idle";int frame;boolean naval;float scale=1;
    void sample(MapSceneSnapshot.Item item,MapSceneSnapshot.Ground ground,TurnJournal.Event event,float fraction,long time,int lod){
        UnitVisual u=item.unit;naval=u.naval;clip="idle";scale=1;
        float f=Float.isFinite(fraction)?Math.max(0,Math.min(1,fraction)):0;
        // Instance-specific phase; distant idle poses are static and off-screen poses unscheduled.
        frame=lod==2?0:(int)Math.floorMod(time/(lod==0?83:167)+u.id*7L,12);
        if(event==null)return;
        if(event.actorId==u.id){
            switch(event.kind){
                case MOVE:
                    clip=f<.06f?"turn":"walk";frame=(int)(f*Math.max(1,event.path.size()-1)*6)%12;
                    if(!event.path.isEmpty()){
                        Hex h=event.path.get(Math.min(event.path.size()-1,Math.round(f*(event.path.size()-1))));
                        if(ground.valid(h)){int t=ground.terrain[h.r*ground.width+h.q];naval=t==World.Terrain.WATER.ordinal()||t==World.Terrain.SEA.ordinal();}
                    }break;
                case ATTACK:case TACTIC:case FACILITY_ATTACK:case FACILITY_COUNTER:clip=f<.25f?"prepare":"attack";frame=Math.min(11,(int)((f<.25f?f*4:(f-.25f)/.75f)*11));break;
                case ENTER:clip="enter";frame=(int)(f*11);scale=1-f*.9f;break;
                default:break;
            }
        }
        for(TurnJournal.Impact impact:event.impacts)if(f>=CombatVisual.HIT&&impact.hex.equals(item.hex)&&impact.loss){
            clip=event.removesUnit(u.id)?"defeat":"hit";float hit=(f-CombatVisual.HIT)/(1-CombatVisual.HIT);frame=Math.min(11,(int)(hit*11));
            if(clip.equals("defeat"))scale=1-hit*.8f;break;
        }
    }
}
