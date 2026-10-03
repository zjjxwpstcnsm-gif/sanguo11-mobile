package game.sanguo.mobile;

import game.sanguo.core.*;

/** Shared player fraction and deterministic visual clock only; no command/RNG access. */
final class UnitAnimation {
    String clip="idle";int frame;boolean naval;float scale=1;
    static int shownTroops(UnitVisual unit,TurnJournal.Event event,float fraction) {
        int troops=unit.troops;
        if(event==null)return troops;
        int active=Math.min(event.strikes.size()-1,(int)(CombatVisual.fraction(fraction)*event.strikes.size()));
        for(int i=0;i<event.strikes.size();i++) {
            TurnJournal.Strike hit=event.strikes.get(i);
            if(hit.targetId==unit.id&&(i<active||i==active&&CombatVisual.phase(event,fraction)>=CombatVisual.HIT))troops=hit.afterTroops;
        }
        return troops;
    }
    void sample(MapSceneSnapshot.Item item,MapSceneSnapshot.Ground ground,TurnJournal.Event event,float fraction,long time,int lod){
        UnitVisual u=item.unit;naval=u.naval;clip="idle";scale=1;
        float f=Float.isFinite(fraction)?Math.max(0,Math.min(1,fraction)):0;
        // Instance-specific phase; distant idle poses are static and off-screen poses unscheduled.
        frame=UnitLod.idleFrame(time,u.id,lod);
        if(event==null)return;
        TurnJournal.Strike strike=CombatVisual.strike(event,f);
        if(strike!=null){
            float phase=CombatVisual.phase(event,f);
            if(strike.actorId==u.id){clip=phase<CombatVisual.LAUNCH?"prepare":"attack";frame=Math.min(11,(int)(phase*11));}
            if(strike.targetId==u.id&&phase>=CombatVisual.HIT&&strike.afterTroops<strike.beforeTroops){
                float hit=(phase-CombatVisual.HIT)/(1-CombatVisual.HIT);clip=strike.afterTroops==0?"defeat":"hit";frame=Math.min(11,(int)(hit*11));if(strike.afterTroops==0)scale=1-hit*.8f;
            }
            return;
        }
        if(event.actorId==u.id){
            switch(event.kind){
                case MOVE:
                    clip=f<.06f?"turn":"walk";frame=(int)(f*Math.max(1,event.path.size()-1)*6)%12;
                    if(!event.path.isEmpty()){
                        float cursor=f*(event.path.size()-1);int index=Math.min(event.path.size()-1,(int)cursor);
                        Hex from=event.path.get(index),to=event.path.get(Math.min(index+1,event.path.size()-1));
                        float t=from.distance(to)>1?0:cursor-index;
                        Hex h=ground.grid.cell(ground.grid.x(from)+(ground.grid.x(to)-ground.grid.x(from))*t,ground.grid.z(from)+(ground.grid.z(to)-ground.grid.z(from))*t);
                        if(ground.valid(h)){int terrain=ground.terrain[h.r*ground.width+h.q];naval=terrain==World.Terrain.WATER.ordinal()||terrain==World.Terrain.SEA.ordinal();}
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
