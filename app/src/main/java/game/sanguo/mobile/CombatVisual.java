package game.sanguo.mobile;

import game.sanguo.core.*;

/** Bounded, deterministic presentation samples. No World, RNG, command or damage calculation. */
final class CombatVisual {
    static final float LAUNCH=.22f, HIT=.62f, FEEDBACK=.68f;
    static final int CAPACITY=64, FIRE_BUDGET=16, TEXT_BUDGET=8, MESH_COUNT=8;
    enum Style { NONE, MELEE, CHARGE, ARROW, STONE, FIRE, LIGHTNING, RECOVER, STATUS }
    static final class Particle {
        int mesh; float x,y,z,scale,yaw,pitch;
    }
    final Particle[] particles=new Particle[CAPACITY];
    int count;
    private int detail=2;
    void detail(int value){detail=Math.max(0,Math.min(2,value));}
    int rays(){return detail==0?3:detail==1?5:7;}
    CombatVisual(){for(int i=0;i<CAPACITY;i++)particles[i]=new Particle();}
    static float fraction(float f){return Float.isFinite(f)?Math.max(0,Math.min(1,f)):0;}
    static Style style(TurnJournal.Event e){
        if(e==null)return Style.NONE;
        if(e.kind==TurnJournal.Kind.RECOVER)return Style.RECOVER;
        if(e.kind==TurnJournal.Kind.MOVE||e.kind==TurnJournal.Kind.ENTER||e.kind==TurnJournal.Kind.DEPLOY)return Style.NONE;
        if(e.plot==War.Plot.LIGHTNING)return Style.LIGHTNING;
        if(e.plot==War.Plot.FIRE||e.infantryTactic==War.Tactic.FIRE_ARROW||e.equipmentTactic==Army.Tactic.FIRE_ARROW||e.equipmentTactic==Army.Tactic.FLAME)return Style.FIRE;
        if(e.equipmentTactic==Army.Tactic.STONE)return Style.STONE;
        if(e.sourceNaval&&e.equipmentTactic!=Army.Tactic.RAM)return Style.ARROW;
        if(e.kind==TurnJournal.Kind.PLOT)return Style.STATUS;
        if(e.kind==TurnJournal.Kind.CHANGE)return e.impacts.isEmpty()?Style.NONE:Style.STATUS;
        if(e.sourceType.contains("CATAPULT"))return Style.STONE;
        if(e.sourceType.equals("CAVALRY"))return Style.CHARGE;
        if(e.sourceKey.startsWith("c")||e.sourceType.contains("BOW")||e.sourceType.equals("ARROW_TOWER")||e.sourceType.equals("SIEGE_TOWER"))return Style.ARROW;
        return Style.MELEE;
    }
    static String feedback(TurnJournal.Event event,TurnJournal.Impact impact){
        if(impact.metric!=TurnJournal.Metric.TROOPS||!impact.entityKey.startsWith("u"))return impact.text;
        int shown=0;
        for(var hit:event.strikes)if(impact.entityKey.equals("u"+hit.targetId))shown+=Math.max(0,hit.beforeTroops-hit.afterTroops);
        int remaining=Math.max(0,-impact.amount-shown);
        return remaining==0?"":"−"+remaining+"兵";
    }
    static TurnJournal.Strike strike(TurnJournal.Event e,float fraction){
        return e==null||e.strikes.isEmpty()?null:e.strikes.get(Math.min(e.strikes.size()-1,(int)(fraction(fraction)*e.strikes.size())));
    }
    static float phase(TurnJournal.Event e,float fraction){
        float f=fraction(fraction);if(e==null||e.strikes.size()<2)return f;
        float scaled=f*e.strikes.size();return f==1?1:scaled-(int)scaled;
    }
    static Style strikeStyle(TurnJournal.Strike s){
        return s.naval?Style.ARROW:s.type.equals("CATAPULT")?Style.STONE:s.type.equals("CAVALRY")?Style.CHARGE:s.type.equals("CROSSBOW")||s.type.equals("SIEGE_TOWER")?Style.ARROW:Style.MELEE;
    }
    void sample(TurnJournal.Event e,float fraction,MapSceneSnapshot.Ground g){
        count=0;if(e==null||g==null)return;float f=phase(e,fraction);Style style=style(e);TurnJournal.Strike strike=strike(e,fraction);
        // A counter/support hit uses its own captured weapon; the original actor keeps
        // the typed tactic (e.g. a fire arrow must not silently become a plain bolt).
        if(strike!=null&&(strike.actorId!=e.actorId||e.plot==null&&e.infantryTactic==null&&e.equipmentTactic==null))style=strikeStyle(strike);
        if(style==Style.NONE)return;
        Hex target=strike!=null?strike.target:e.target!=null?e.target:!e.impacts.isEmpty()?e.impacts.get(0).hex:e.start;
        if(target==null||!g.valid(target))return;
        Hex start=strike!=null?strike.start:e.start!=null&&g.valid(e.start)?e.start:target;
        float ax=g.grid.x(start),az=g.grid.z(start),bx=g.grid.x(target),bz=g.grid.z(target);
        float ay=g.surface.at(start)+.35f,by=g.surface.at(target)+.3f;
        if(f>=LAUNCH&&f<HIT){
            float t=(f-LAUNCH)/(HIT-LAUNCH),yaw=(float)Math.atan2(bx-ax,bz-az);
            if(style==Style.ARROW||style==Style.STONE||style==Style.FIRE){
                int n=style==Style.ARROW?rays():style==Style.FIRE?Math.max(2,rays()-3):1;
                for(int i=0;i<n;i++){
                    float offset=(i-(n-1)*.5f)*.07f;
                    float x=ax+(bx-ax)*t+offset,z=az+(bz-az)*t+offset;
                    float arc=style==Style.STONE?1.8f:.55f;
                    float y=flightHeight(g,ax,ay,az,bx,by,bz,t,offset,arc);
                    float t2=Math.min(1,t+.01f),y2=flightHeight(g,ax,ay,az,bx,by,bz,t2,offset,arc);
                    add(style==Style.ARROW?0:style==Style.STONE?1:2,x,y,z,style==Style.ARROW?.85f:.7f,yaw);
                    particles[count-1].pitch=(float)Math.atan2(y2-y,Math.max(.001f,(t2-t)*(float)Math.hypot(bx-ax,bz-az)));
                    if(style==Style.FIRE&&detail>0)add(6,x-.07f*(bx-ax),y+.18f,z-.07f*(bz-az),.3f,0);
                }
            }else if(style==Style.LIGHTNING){
                for(int i=0;i<8;i++)add(4,bx+(i%2==0?.08f:-.08f),by+i*.28f,bz,.8f,0);
            }else if(style==Style.MELEE||style==Style.CHARGE){
                add(3,bx,by+.15f,bz,.5f+t*.7f,yaw+t*2);
                if(style==Style.CHARGE)for(int i=0;i<rays();i++){float u=t*(i+1)/rays(),x=ax+(bx-ax)*u,z=az+(bz-az)*u;add(6,x,g.surface.sample(x,z)+.18f,z,.2f+u*.2f,yaw);}
            }
        }
        if(f>=HIT&&f<1){
            float t=(f-HIT)/(1-HIT);
            // Only recorded changes produce hit bursts; a failed plot has no invented damage.
            if(strike!=null){
                if(strike.afterTroops<strike.beforeTroops){
                    burst(g,bx,bz,t,strike.naval?7:1,rays());
                    add(3,bx,by+.13f,bz,(1-t)*.8f,0);
                    if(detail>0)add(strike.naval?7:6,bx,by+t*.8f,bz,(1-t)*.8f,0);
                }
                return;
            }
            int targets=0;
            for(TurnJournal.StateChange delta:e.states){
                TurnJournal.State before=delta.before,after=delta.after;
                TurnJournal.State at=after!=null?after:before;if(at==null||!g.valid(at.hex))continue;
                boolean damage=before!=null&&(after==null||after.troops<before.troops||after.hp<before.hp);
                boolean recovery=before!=null&&after!=null&&after.energy>before.energy;
                boolean fire=after!=null&&after.key.startsWith("f");
                if(!damage&&!recovery&&!fire)continue;
                if(targets++>=6)break;
                float x=g.grid.x(at.hex),z=g.grid.z(at.hex),y=g.surface.at(at.hex)+.22f;
                burst(g,x,z,t,recovery?5:fire?2:1,Math.min(5,rays()));
            }
            if(style==Style.STATUS||style==Style.RECOVER)add(5,bx,by,bz,.4f+t*.7f,t*3);
        }
    }
    void fire(MapSceneSnapshot.FireState fire,MapSceneSnapshot.Ground g,long millis,boolean motion){
        float pulse=motion?(float)Math.sin(millis*.006+fire.hex.q*3+fire.hex.r)*.12f:0;
        add(2,g.grid.x(fire.hex),g.surface.at(fire.hex)+.25f,g.grid.z(fire.hex),.5f+pulse,0);
        if(detail>0)add(6,g.grid.x(fire.hex)+.09f,g.surface.at(fire.hex)+.85f+pulse,g.grid.z(fire.hex),.50f,0);
    }
    private static float flightHeight(MapSceneSnapshot.Ground g,float ax,float ay,float az,float bx,float by,float bz,float t,float offset,float arc){
        return Math.max(ay+(by-ay)*t+4*t*(1-t)*arc,g.surface.sample(ax+(bx-ax)*t+offset,az+(bz-az)*t+offset)+.36f);
    }
    private void burst(MapSceneSnapshot.Ground g,float x,float z,float t,int mesh,int n){
        for(int i=0;i<n;i++){double angle=i*Math.PI*2/n;float px=x+(float)Math.cos(angle)*t*.65f,pz=z+(float)Math.sin(angle)*t*.65f;
            add(mesh,px,g.surface.sample(px,pz)+.22f+(float)Math.sin(t*Math.PI)*.45f,pz,(1-t)*.35f,0);}
    }
    private void add(int mesh,float x,float y,float z,float scale,float yaw){
        if(count==CAPACITY)return;Particle p=particles[count++];p.mesh=mesh;p.x=x;p.y=y;p.z=z;p.scale=Math.max(.001f,scale);p.yaw=yaw;p.pitch=0;
    }
    /** Original CC0 opaque effect geometry; eight shared meshes, no runtime shader compiler. */
    static SceneMesh mesh(int kind){
        SceneMesh.Builder b=new SceneMesh.Builder();
        int color=kind==0?0xffd2c0a0:kind==1?0xffc8b59a:kind==2?0xffff9238:kind==3?0xffffdc98:kind==4?0xffcbe7ff:kind==6?0xffa29b90:kind==7?0xffa3d9e1:0xff82d5bc;
        if(kind==0){
            b.box(-.015f,-.22f,.03f,.44f,.035f,color);
            b.face(new float[]{0,0,.31f,-.07f,0,.15f,.07f,0,.15f,0,0,.31f},color);
        }else if(kind==3||kind==5){
            for(int i=0;i<12;i++){double a=i*Math.PI/6,c=(i+1)*Math.PI/6;float r=.5f,s=.43f;
                b.face(new float[]{(float)Math.cos(a)*r,0,(float)Math.sin(a)*r,(float)Math.cos(c)*r,0,(float)Math.sin(c)*r,(float)Math.cos(c)*s,0,(float)Math.sin(c)*s,(float)Math.cos(a)*s,0,(float)Math.sin(a)*s},color);}
        }else{
            float h=kind==2?.65f:kind==4?.38f:.22f,r=kind==4?.05f:.20f;
            for(int i=0;i<6;i++){double a=i*Math.PI/3,c=(i+1)*Math.PI/3;
                b.face(new float[]{0,h,0,(float)Math.cos(a)*r,0,(float)Math.sin(a)*r,0,-h*.3f,0,(float)Math.cos(c)*r,0,(float)Math.sin(c)*r},color);}
        }
        return b.mesh(0,0,1);
    }
}
